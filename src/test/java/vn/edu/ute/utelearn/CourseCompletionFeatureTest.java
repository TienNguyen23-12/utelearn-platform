package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.*;
import vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.impl.CohortServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseCompletionFeatureTest {

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private LessonCompletionRepository lessonCompletionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private CohortServiceImpl cohortService;

    private User sampleUser;
    private Course sampleCourse;
    private Cohort module1;
    private Cohort module2;

    @BeforeEach
    void setUp() {
        cohortService = new CohortServiceImpl(
                cohortRepository,
                cohortScheduleRepository,
                courseRepository,
                lessonRepository,
                cohortMemberRepository,
                sectionRepository,
                lessonCompletionRepository,
                null
        );
        org.springframework.test.util.ReflectionTestUtils.setField(cohortService, "enrollmentRepository", enrollmentRepository);

        sampleUser = User.builder().id(10L).username("student_course").email("student_course@ute.edu.vn").build();
        sampleCourse = Course.builder().id(100L).code("CS200").title("Khóa học Lập trình Web Fullstack").build();

        module1 = Cohort.builder().id(1001L).course(sampleCourse).code("MOD-01").name("Module 1 Frontend").build();
        module2 = Cohort.builder().id(1002L).course(sampleCourse).code("MOD-02").name("Module 2 Backend").build();
    }

    @Test
    void testCheckAndSyncCourseCompletion_WhenAllModulesPassed() {
        CohortMember member1 = CohortMember.builder()
                .cohort(module1)
                .user(sampleUser)
                .isPassed(true)
                .finalGrade(new BigDecimal("8.00"))
                .build();

        CohortMember member2 = CohortMember.builder()
                .cohort(module2)
                .user(sampleUser)
                .isPassed(true)
                .finalGrade(new BigDecimal("9.00"))
                .build();

        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(module1, module2));
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(100L, 10L)).thenReturn(List.of(member1, member2));
        when(enrollmentRepository.findByUser_IdAndCourse_Id(10L, 100L)).thenReturn(Optional.empty());

        cohortService.checkAndSyncCourseCompletion(sampleCourse, sampleUser);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());

        Enrollment saved = captor.getValue();
        assertNotNull(saved);
        assertTrue(saved.getIsCompleted());
        assertEquals(new BigDecimal("8.50"), saved.getAverageGrade());
        assertEquals(100.0, saved.getProgress());
        assertNotNull(saved.getCertificateUrl());
        assertTrue(saved.getCertificateUrl().contains("UTE-CERT-CS200-10"));
        assertNotNull(saved.getCompletedAt());
    }

    @Test
    void testCheckAndSyncCourseCompletion_WhenNotAllModulesPassed() {
        CohortMember member1 = CohortMember.builder()
                .cohort(module1)
                .user(sampleUser)
                .isPassed(true)
                .finalGrade(new BigDecimal("8.00"))
                .build();

        CohortMember member2 = CohortMember.builder()
                .cohort(module2)
                .user(sampleUser)
                .isPassed(false)
                .finalGrade(null)
                .build();

        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(module1, module2));
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(100L, 10L)).thenReturn(List.of(member1, member2));
        when(enrollmentRepository.findByUser_IdAndCourse_Id(10L, 100L)).thenReturn(Optional.empty());

        cohortService.checkAndSyncCourseCompletion(sampleCourse, sampleUser);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());

        Enrollment saved = captor.getValue();
        assertNotNull(saved);
        assertFalse(saved.getIsCompleted());
        assertEquals(50.0, saved.getProgress());
    }

    @Test
    void testGetUserEnrollmentSummary_CourseCompletionDataEnriched() {
        CohortMember member1 = CohortMember.builder()
                .cohort(module1)
                .user(sampleUser)
                .isPassed(true)
                .finalGrade(new BigDecimal("9.00"))
                .enrolledAt(Instant.now())
                .build();

        CohortMember member2 = CohortMember.builder()
                .cohort(module2)
                .user(sampleUser)
                .isPassed(true)
                .finalGrade(new BigDecimal("10.00"))
                .enrolledAt(Instant.now())
                .build();

        Enrollment completedEnrollment = Enrollment.builder()
                .id(50L)
                .course(sampleCourse)
                .user(sampleUser)
                .isCompleted(true)
                .averageGrade(new BigDecimal("9.50"))
                .certificateUrl("/certificates/verify?code=UTE-CERT-CS200-10")
                .rating(5)
                .reviewComment("Tuyệt vời")
                .build();

        when(cohortMemberRepository.findByUserOrderByEnrolledAtDesc(sampleUser))
                .thenReturn(List.of(member1, member2));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(module1, module2));
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(100L, 10L))
                .thenReturn(List.of(member1, member2));
        when(enrollmentRepository.findByUser_IdAndCourse_Id(10L, 100L))
                .thenReturn(Optional.of(completedEnrollment));

        UserEnrollmentSummaryDTO summary = cohortService.getUserEnrollmentSummary(sampleUser);

        assertNotNull(summary);
        assertEquals(1, summary.getEnrolledCourses().size());
        UserEnrollmentSummaryDTO.EnrolledCourseItem item = summary.getEnrolledCourses().get(0);
        assertEquals(sampleCourse, item.getCourse());
        assertEquals(2, item.getTotalModules());
        assertEquals(2, item.getCompletedModulesCount());
        assertEquals(100, item.getProgressPercent());
        assertTrue(item.isCompleted());
        assertEquals(new BigDecimal("9.50"), item.getAverageGrade());
        assertEquals("/certificates/verify?code=UTE-CERT-CS200-10", item.getCertificateUrl());
        assertTrue(item.isHasReviewed());
        assertEquals(5, item.getRating());
    }
}
