package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.LessonNoteRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.LessonNoteRequestDTO;
import vn.edu.ute.utelearn.dto.LessonNoteResponseDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.LessonNote;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.LessonNoteService;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonNoteServiceImpl implements LessonNoteService {

    private final LessonNoteRepository lessonNoteRepository;
    private final LessonRepository lessonRepository;
    private final CohortRepository cohortRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LessonNoteResponseDTO> getNotesForLesson(Long lessonId, Long userId) {
        if (lessonId == null || userId == null) {
            return Collections.emptyList();
        }
        log.info("[LessonNoteService] Lấy danh sách ghi chú bài học {} cho người dùng {}", lessonId, userId);
        List<LessonNote> notes = lessonNoteRepository.findByLessonIdAndUserIdOrderByTimeline(lessonId, userId);
        return notes.stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional
    public LessonNoteResponseDTO createNote(LessonNoteRequestDTO request, Long userId) {
        if (request == null || request.getLessonId() == null || userId == null) {
            throw new IllegalArgumentException("Dữ liệu tạo ghi chú không hợp lệ");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung ghi chú không được để trống");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học"));

        Cohort cohort = null;
        if (request.getCohortId() != null) {
            cohort = cohortRepository.findById(request.getCohortId()).orElse(null);
        }

        Integer timestamp = normalizeTimestamp(request.getTimestampSeconds());

        LessonNote note = LessonNote.builder()
                .user(user)
                .lesson(lesson)
                .cohort(cohort)
                .timestampSeconds(timestamp)
                .content(request.getContent().trim())
                .build();

        LessonNote saved = lessonNoteRepository.save(note);
        log.info("[LessonNoteService] Đã tạo ghi chú {} cho bài học {} (timestamp: {}s)", saved.getId(), lesson.getId(), timestamp);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public LessonNoteResponseDTO updateNote(Long noteId, LessonNoteRequestDTO request, Long userId) {
        if (noteId == null || request == null || userId == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật ghi chú không hợp lệ");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung ghi chú không được để trống");
        }

        LessonNote note = lessonNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghi chú"));

        if (!note.getUser().getId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền chỉnh sửa ghi chú này");
        }

        note.setContent(request.getContent().trim());
        if (request.getTimestampSeconds() != null) {
            note.setTimestampSeconds(normalizeTimestamp(request.getTimestampSeconds()));
        }

        LessonNote saved = lessonNoteRepository.save(note);
        log.info("[LessonNoteService] Đã cập nhật ghi chú {} cho người dùng {}", noteId, userId);
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        if (noteId == null || userId == null) {
            throw new IllegalArgumentException("Dữ liệu xóa ghi chú không hợp lệ");
        }

        LessonNote note = lessonNoteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ghi chú"));

        if (!note.getUser().getId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền xóa ghi chú này");
        }

        lessonNoteRepository.delete(note);
        log.info("[LessonNoteService] Đã xóa ghi chú {} của người dùng {}", noteId, userId);
    }

    private Integer normalizeTimestamp(Integer timestampSeconds) {
        if (timestampSeconds == null || timestampSeconds < 0) {
            return null;
        }
        return timestampSeconds;
    }

    private String formatTimestamp(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds < 0) {
            return null;
        }
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private LessonNoteResponseDTO mapToDTO(LessonNote note) {
        return LessonNoteResponseDTO.builder()
                .id(note.getId())
                .lessonId(note.getLesson().getId())
                .cohortId(note.getCohort() != null ? note.getCohort().getId() : null)
                .timestampSeconds(note.getTimestampSeconds())
                .formattedTime(formatTimestamp(note.getTimestampSeconds()))
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
