package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository;
import vn.edu.ute.utelearn.dao.SectionRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.LessonRequestDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.SectionService;
import vn.edu.ute.utelearn.service.impl.LessonServiceImpl;
import vn.edu.ute.utelearn.service.impl.ModerationServiceImpl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LessonStatusFeatureTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private SectionService sectionService;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private vn.edu.ute.utelearn.service.CloudinaryService cloudinaryService;

    @InjectMocks
    private LessonServiceImpl lessonServiceImpl;

    @Mock
    private ModerationTaskRepository moderationTaskRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ModeratorAttendanceRepository moderatorAttendanceRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private ModerationServiceImpl moderationService;

    private Section section;
    private Cohort standaloneCohort;
    private User instructor;
    private User moderator;

    @BeforeEach
    void setUp() {
        instructor = User.builder().id(99L).email("teacher@ute.edu.vn").build();
        Role modRole = Role.builder().id(2L).code("MODERATOR").build();
        moderator = User.builder().id(88L).email("mod@ute.edu.vn").roles(Set.of(modRole)).build();

        standaloneCohort = Cohort.builder()
                .id(20L)
                .name("Module độc lập")
                .course(null)
                .moderationStatus("APPROVED")
                .createdBy(instructor)
                .build();
        section = Section.builder()
                .id(1L)
                .title("Chương 1")
                .cohort(standaloneCohort)
                .course(null)
                .build();
    }

    @Test
    void testLessonDefaultStatusIsDraft() {
        Lesson newLesson = Lesson.builder().title("Bài 1").build();
        assertEquals("DRAFT", newLesson.getStatus());
        assertFalse(newLesson.isPublished());
    }

    @Test
    void testBackwardCompatibilityForOldLessonsWithNullStatus() {
        Lesson oldLesson = new Lesson();
        oldLesson.setTitle("Bài cũ");
        oldLesson.setStatus(null);
        assertEquals("PUBLISHED", oldLesson.getStatus());
        assertTrue(oldLesson.isPublished());

        oldLesson.setStatus("   ");
        assertEquals("PUBLISHED", oldLesson.getStatus());
        assertTrue(oldLesson.isPublished());
    }

    @Test
    void testCreateLessonDefaultsToDraftAndRevertsCohortToDraft() {
        when(userRepository.findById(99L)).thenReturn(Optional.of(instructor));
        when(sectionService.getSectionById(1L)).thenReturn(section);
        when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());
        when(cohortRepository.findById(20L)).thenReturn(Optional.of(standaloneCohort));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        LessonRequestDTO dto = new LessonRequestDTO();
        dto.setTitle("Bài học mới");
        dto.setLessonType("VIDEO");
        dto.setVideoUrl("https://example.com/video.mp4");
        dto.setCohortId(20L);

        Lesson created = lessonServiceImpl.createLesson(1L, dto, instructor.getId());

        assertNotNull(created);
        assertEquals("DRAFT", created.getStatus());
        assertFalse(created.isPublished());
        assertEquals("DRAFT", standaloneCohort.getModerationStatus());
        verify(cohortRepository, atLeastOnce()).save(standaloneCohort);
    }

    @Test
    void testUpdateLessonAlwaysForcesDraft() {
        when(userRepository.findById(99L)).thenReturn(Optional.of(instructor));
        Lesson existing = Lesson.builder()
                .id(100L)
                .title("Bài học")
                .section(section)
                .status("PUBLISHED")
                .build();

        when(lessonRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(cohortRepository.findById(20L)).thenReturn(Optional.of(standaloneCohort));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LessonRequestDTO dto = new LessonRequestDTO();
        dto.setTitle("Bài học cập nhật nội dung");
        dto.setLessonType("VIDEO");
        dto.setVideoUrl("https://example.com/video.mp4");
        dto.setStatus("PUBLISHED");
        dto.setCohortId(20L);

        Lesson updated = lessonServiceImpl.updateLesson(100L, dto, instructor.getId());

        assertNotNull(updated);
        assertEquals("DRAFT", updated.getStatus());
        assertFalse(updated.isPublished());
        assertEquals("DRAFT", standaloneCohort.getModerationStatus());
        verify(cohortRepository, atLeastOnce()).save(standaloneCohort);
    }

    @Test
    void testModeratorApprovalPublishesAllLessons() {
        ModeratorAttendance attendance = ModeratorAttendance.builder()
                .moderator(moderator)
                .workDate(LocalDate.now())
                .isActive(true)
                .completedTasksCount(2)
                .build();

        ModerationTask task = ModerationTask.builder()
                .id(50L)
                .itemType("COHORT")
                .itemId(20L)
                .author(instructor)
                .status("PENDING")
                .build();

        Lesson draftLesson = Lesson.builder().id(101L).title("Bài 1").status("DRAFT").build();

        when(moderationTaskRepository.findById(50L)).thenReturn(Optional.of(task));
        when(userRepository.findById(88L)).thenReturn(Optional.of(moderator));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderator, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(cohortRepository.findById(20L)).thenReturn(Optional.of(standaloneCohort));
        when(cohortScheduleRepository.findByCohortIdWithDetails(20L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findByCohortIdOrderByOrderIndexAsc(20L)).thenReturn(List.of(section));
        when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(draftLesson));

        moderationService.approveTask(50L, 88L, "Duyệt tốt");

        assertEquals("APPROVED", standaloneCohort.getModerationStatus());
        assertEquals("PUBLISHED", draftLesson.getStatus());
        assertTrue(draftLesson.isPublished());
        verify(lessonRepository, atLeastOnce()).saveAll(any());
    }

    @Test
    void testCreateLessonInPublishedCourseRevertsCourseToDraft() {
        when(userRepository.findById(99L)).thenReturn(Optional.of(instructor));
        Course publishedCourse = Course.builder()
                .id(10L)
                .title("Khóa học Java")
                .status("PUBLISHED")
                .createdBy(instructor)
                .build();
        Section courseSection = Section.builder()
                .id(2L)
                .course(publishedCourse)
                .build();

        when(sectionService.getSectionById(2L)).thenReturn(courseSection);
        when(lessonRepository.findBySectionIdOrderByOrderIndexAsc(2L)).thenReturn(Collections.emptyList());
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LessonRequestDTO dto = new LessonRequestDTO();
        dto.setTitle("Quiz kiểm tra trắc nghiệm");
        dto.setLessonType("QUIZ");

        Lesson created = lessonServiceImpl.createLesson(2L, dto, instructor.getId());

        assertNotNull(created);
        assertEquals("DRAFT", created.getStatus());
        assertEquals("DRAFT", publishedCourse.getStatus());
        verify(courseRepository, atLeastOnce()).save(publishedCourse);
    }
}
