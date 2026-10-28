package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import vn.edu.ute.utelearn.service.impl.LessonNoteServiceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LessonNoteServiceTest {

    @Mock
    private LessonNoteRepository lessonNoteRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LessonNoteServiceImpl lessonNoteService;

    private User user;
    private Lesson lesson;
    private Cohort cohort;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("student1").email("student1@utelearn.edu.vn").build();
        lesson = Lesson.builder().id(10L).title("Bài giảng Video mẫu").lessonType("VIDEO").build();
        cohort = Cohort.builder().id(100L).name("Lớp K23").build();
    }

    @Test
    void testGetNotesForLesson() {
        LessonNote note1 = LessonNote.builder()
                .id(1L)
                .user(user)
                .lesson(lesson)
                .timestampSeconds(75)
                .content("Ghi chú tại 01:15")
                .createdAt(Instant.now())
                .build();

        LessonNote note2 = LessonNote.builder()
                .id(2L)
                .user(user)
                .lesson(lesson)
                .timestampSeconds(3665)
                .content("Ghi chú tại 01:01:05")
                .createdAt(Instant.now())
                .build();

        when(lessonNoteRepository.findByLessonIdAndUserIdOrderByTimeline(10L, 1L)).thenReturn(List.of(note1, note2));

        List<LessonNoteResponseDTO> dtos = lessonNoteService.getNotesForLesson(10L, 1L);

        assertEquals(2, dtos.size());
        assertEquals("01:15", dtos.get(0).getFormattedTime());
        assertEquals("01:01:05", dtos.get(1).getFormattedTime());
    }

    @Test
    void testCreateNoteWithTimestamp() {
        LessonNoteRequestDTO request = LessonNoteRequestDTO.builder()
                .lessonId(10L)
                .cohortId(100L)
                .timestampSeconds(125)
                .content("Giải thích cấu trúc Spring Boot")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(cohortRepository.findById(100L)).thenReturn(Optional.of(cohort));

        when(lessonNoteRepository.save(any(LessonNote.class))).thenAnswer(invocation -> {
            LessonNote n = invocation.getArgument(0);
            n.setId(99L);
            n.setCreatedAt(Instant.now());
            return n;
        });

        LessonNoteResponseDTO result = lessonNoteService.createNote(request, 1L);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals(125, result.getTimestampSeconds());
        assertEquals("02:05", result.getFormattedTime());
        assertEquals("Giải thích cấu trúc Spring Boot", result.getContent());
    }

    @Test
    void testUpdateNoteUnauthorizedUser() {
        User anotherUser = User.builder().id(2L).username("student2").build();
        LessonNote existingNote = LessonNote.builder()
                .id(50L)
                .user(anotherUser)
                .lesson(lesson)
                .content("Ghi chú của người khác")
                .build();

        when(lessonNoteRepository.findById(50L)).thenReturn(Optional.of(existingNote));

        LessonNoteRequestDTO updateRequest = LessonNoteRequestDTO.builder()
                .lessonId(10L)
                .content("Nội dung sửa đổi trái phép")
                .build();

        assertThrows(SecurityException.class, () -> lessonNoteService.updateNote(50L, updateRequest, 1L));
    }

    @Test
    void testDeleteNoteSuccessfully() {
        LessonNote existingNote = LessonNote.builder()
                .id(50L)
                .user(user)
                .lesson(lesson)
                .content("Ghi chú cần xóa")
                .build();

        when(lessonNoteRepository.findById(50L)).thenReturn(Optional.of(existingNote));

        assertDoesNotThrow(() -> lessonNoteService.deleteNote(50L, 1L));
        verify(lessonNoteRepository, times(1)).delete(existingNote);
    }
}
