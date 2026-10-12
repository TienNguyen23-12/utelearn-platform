package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dto.LessonRequestDTO;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.service.LessonService;
import vn.edu.ute.utelearn.service.SectionService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final SectionService sectionService;

    @Override
    public List<Lesson> getLessonsBySectionId(Long sectionId) {
        return lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);
    }

    @Override
    public Lesson getLessonById(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học ID: " + lessonId));
    }

    @Override
    @Transactional
    public Lesson createLesson(Long sectionId, LessonRequestDTO request, Long instructorId) {
        Section section = sectionService.getSectionById(sectionId);

        if (!section.getCourse().getCreatedBy().getId().equals(instructorId)) {
            throw new RuntimeException("Bạn không có quyền thêm bài học vào khóa học này");
        }

        Lesson lesson = Lesson.builder()
                .section(section)
                .title(request.getTitle())
                .lessonType(request.getLessonType())
                .videoUrl(request.getVideoUrl())
                .documentContent(request.getDocumentContent())
                .assetUrl(request.getAssetUrl())
                .durationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0)
                .isFreePreview(request.getIsFreePreview() != null ? request.getIsFreePreview() : false)
                .isPublicForCohorts(request.getIsPublicForCohorts() != null ? request.getIsPublicForCohorts() : false)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 1)
                .build();

        return lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public Lesson updateLesson(Long lessonId, LessonRequestDTO request, Long instructorId) {
        Lesson lesson = getLessonById(lessonId);

        if (!lesson.getSection().getCourse().getCreatedBy().getId().equals(instructorId)) {
            throw new RuntimeException("Bạn không có quyền sửa bài học này");
        }

        lesson.setTitle(request.getTitle());
        lesson.setLessonType(request.getLessonType());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setDocumentContent(request.getDocumentContent());
        lesson.setAssetUrl(request.getAssetUrl());
        
        if (request.getDurationSeconds() != null) {
            lesson.setDurationSeconds(request.getDurationSeconds());
        }
        if (request.getIsFreePreview() != null) {
            lesson.setIsFreePreview(request.getIsFreePreview());
        }
        if (request.getIsPublicForCohorts() != null) {
            lesson.setIsPublicForCohorts(request.getIsPublicForCohorts());
        }
        if (request.getOrderIndex() != null) {
            lesson.setOrderIndex(request.getOrderIndex());
        }

        return lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public void deleteLesson(Long lessonId, Long instructorId) {
        Lesson lesson = getLessonById(lessonId);

        if (!lesson.getSection().getCourse().getCreatedBy().getId().equals(instructorId)) {
            throw new RuntimeException("Bạn không có quyền xóa bài học này");
        }

        lessonRepository.delete(lesson);
    }
}
