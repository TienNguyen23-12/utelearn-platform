package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import vn.edu.ute.utelearn.controller.CourseController;
import vn.edu.ute.utelearn.controller.instructor.InstructorCourseController;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.EnrollmentRepository;
import vn.edu.ute.utelearn.dto.CourseGradebookDTO;
import vn.edu.ute.utelearn.dto.StudentCourseGradeDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Enrollment;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.GradebookService;
import vn.edu.ute.utelearn.service.impl.GradebookServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseGradebookFeatureTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private AuthService authService;

    @Mock
    private CourseService courseService;

    @Mock
    private GradebookService mockGradebookService;

    @Mock
    private vn.edu.ute.utelearn.service.CohortService cohortService;

    @Mock
    private vn.edu.ute.utelearn.dao.UserRepository userRepository;

    @InjectMocks
    private GradebookServiceImpl gradebookService;

    private User sampleInstructor;
    private User sampleStudent;
    private Course sampleCourse;
    private Cohort module1;
    private Cohort module2;
    private Enrollment sampleEnrollment;
    private CohortMember member1;
    private CohortMember member2;

    @BeforeEach
    void setUp() {
        sampleInstructor = User.builder()
                .id(1L)
                .fullName("TS. Nguyễn Văn Giảng")
                .email("instructor@ute.edu.vn")
                .build();

        sampleStudent = User.builder()
                .id(2L)
                .fullName("Trần Thị Học Viên")
                .email("student@ute.edu.vn")
                .build();

        sampleCourse = Course.builder()
                .id(100L)
                .title("Lập trình Web nâng cao")
                .slug("lap-trinh-web-nang-cao")
                .code("CRS-WEB-01")
                .createdBy(sampleInstructor)
                .build();

        module1 = Cohort.builder()
                .id(201L)
                .name("Module 1: Front-end")
                .code("MOD-01")
                .course(sampleCourse)
                .createdBy(sampleInstructor)
                .orderIndex(1)
                .build();

        module2 = Cohort.builder()
                .id(202L)
                .name("Module 2: Back-end")
                .code("MOD-02")
                .course(sampleCourse)
                .createdBy(sampleInstructor)
                .orderIndex(2)
                .build();

        sampleEnrollment = Enrollment.builder()
                .id(501L)
                .course(sampleCourse)
                .user(sampleStudent)
                .enrolledAt(Instant.now())
                .isCompleted(false)
                .averageGrade(null)
                .build();

        member1 = CohortMember.builder()
                .id(601L)
                .cohort(module1)
                .user(sampleStudent)
                .finalGrade(new BigDecimal("8.5"))
                .isPassed(true)
                .enrolledAt(Instant.now())
                .build();

        member2 = CohortMember.builder()
                .id(602L)
                .cohort(module2)
                .user(sampleStudent)
                .finalGrade(new BigDecimal("7.5"))
                .isPassed(true)
                .enrolledAt(Instant.now())
                .build();

        ReflectionTestUtils.setField(gradebookService, "userRepository", userRepository);
        ReflectionTestUtils.setField(gradebookService, "cohortService", cohortService);
        ReflectionTestUtils.setField(gradebookService, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(gradebookService, "enrollmentRepository", enrollmentRepository);
    }

    @Test
    void testGetCourseGradebook_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdOrdered(100L)).thenReturn(List.of(module1, module2));
        when(enrollmentRepository.findByCourse_IdWithUser(100L)).thenReturn(List.of(sampleEnrollment));
        when(cohortMemberRepository.findByCourseIdWithUserAndCohort(100L)).thenReturn(List.of(member1, member2));

        CourseGradebookDTO gradebook = gradebookService.getCourseGradebook(100L, 1L);

        assertNotNull(gradebook);
        assertNotNull(gradebook.getCourse());
        assertEquals(100L, gradebook.getCourse().getId());
        assertEquals("Lập trình Web nâng cao", gradebook.getCourse().getTitle());
        assertEquals(2, gradebook.getModules().size());
        assertEquals(1, gradebook.getStudents().size());

        var studentRow = gradebook.getStudents().get(0);
        assertEquals(2L, studentRow.getUserId());
        assertEquals(2, studentRow.getCompletedModulesCount());
        assertEquals(new BigDecimal("8.00"), studentRow.getCourseAverageGrade());
        assertTrue(studentRow.getIsCompleted());
        assertEquals(1, gradebook.getCompletedCount());
        assertEquals(100.0, gradebook.getPassRate());
    }

    @Test
    void testCalculateAndSyncCourseGradebook_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(sampleCourse));
        when(enrollmentRepository.findByCourse_IdWithUser(100L)).thenReturn(List.of(sampleEnrollment));
        when(cohortMemberRepository.findByCourseIdWithUserAndCohort(100L)).thenReturn(List.of(member1, member2));

        gradebookService.calculateAndSyncCourseGradebook(100L, 1L);

        verify(cohortService, times(1)).checkAndSyncCourseCompletion(sampleCourse, sampleStudent);
    }

    @Test
    void testGetStudentCourseGrade_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(sampleStudent));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdOrdered(100L)).thenReturn(List.of(module1, module2));
        when(enrollmentRepository.findByUser_IdAndCourse_Id(2L, 100L)).thenReturn(Optional.of(sampleEnrollment));
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(100L, 2L)).thenReturn(List.of(member1, member2));

        StudentCourseGradeDTO studentGrade = gradebookService.getStudentCourseGrade(100L, 2L);

        assertNotNull(studentGrade);
        assertNotNull(studentGrade.getCourse());
        assertEquals(100L, studentGrade.getCourse().getId());
        assertEquals("Lập trình Web nâng cao", studentGrade.getCourse().getTitle());
        assertEquals(2, studentGrade.getTotalModulesCount());
        assertEquals(2, studentGrade.getCompletedModulesCount());
        assertEquals(100, studentGrade.getProgressPercent());
        assertEquals(new BigDecimal("8.00"), studentGrade.getCourseAverageGrade());
        assertEquals(2, studentGrade.getModuleGrades().size());
        assertTrue(Boolean.TRUE.equals(studentGrade.getModuleGrades().get(0).getIsPassed()));
    }

    @Test
    void testExportCourseGradebookToExcel_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdOrdered(100L)).thenReturn(List.of(module1, module2));
        when(enrollmentRepository.findByCourse_IdWithUser(100L)).thenReturn(List.of(sampleEnrollment));
        when(cohortMemberRepository.findByCourseIdWithUserAndCohort(100L)).thenReturn(List.of(member1, member2));

        byte[] excelBytes = gradebookService.exportCourseGradebookToExcel(100L, 1L);

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }

    @Test
    void testInstructorCourseController_ShowGradebook() {
        InstructorCourseController controller = new InstructorCourseController(
                courseService, null, authService, null, null, null, null, null, null, null, null, null
        );
        ReflectionTestUtils.setField(controller, "gradebookService", mockGradebookService);

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleInstructor));
        CourseGradebookDTO mockGradebook = CourseGradebookDTO.builder()
                .course(sampleCourse)
                .modules(new ArrayList<>())
                .students(new ArrayList<>())
                .build();
        when(mockGradebookService.getCourseGradebook(100L, 1L)).thenReturn(mockGradebook);

        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.showCourseGradebook(100L, model, redirectAttributes);

        assertEquals("instructor/courses/gradebook", view);
        assertEquals(mockGradebook, model.getAttribute("gradebook"));
    }

    @Test
    void testInstructorCourseController_CalculateAndSync() {
        InstructorCourseController controller = new InstructorCourseController(
                courseService, null, authService, null, null, null, null, null, null, null, null, null
        );
        ReflectionTestUtils.setField(controller, "gradebookService", mockGradebookService);

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleInstructor));
        doNothing().when(mockGradebookService).calculateAndSyncCourseGradebook(100L, 1L);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String redirect = controller.calculateCourseGrades(100L, redirectAttributes);

        assertEquals("redirect:/instructor/courses/100/gradebook", redirect);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    void testInstructorCourseController_ExportExcel() {
        InstructorCourseController controller = new InstructorCourseController(
                courseService, null, authService, null, null, null, null, null, null, null, null, null
        );
        ReflectionTestUtils.setField(controller, "gradebookService", mockGradebookService);

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleInstructor));
        when(courseService.getCourseById(100L)).thenReturn(sampleCourse);
        when(mockGradebookService.exportCourseGradebookToExcel(100L, 1L)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.exportCourseGradebook(100L);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().length);
    }

    @Test
    void testCourseController_GetStudentCourseGradesJson_Success() {
        CourseController courseController = new CourseController(
                authService, courseService, null, cohortMemberRepository, cohortRepository, null, null, null
        );
        ReflectionTestUtils.setField(courseController, "gradebookService", mockGradebookService);

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleStudent));
        when(courseService.getCourseBySlug("lap-trinh-web-nang-cao")).thenReturn(sampleCourse);

        StudentCourseGradeDTO mockGrade = StudentCourseGradeDTO.builder()
                .course(sampleCourse)
                .totalModulesCount(2)
                .completedModulesCount(2)
                .progressPercent(100)
                .courseAverageGrade(new BigDecimal("8.00"))
                .isCompleted(true)
                .moduleGrades(Collections.emptyList())
                .build();
        when(mockGradebookService.getStudentCourseGrade(100L, 2L)).thenReturn(mockGrade);

        ResponseEntity<?> response = courseController.getStudentCourseGradesJson("lap-trinh-web-nang-cao");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockGrade, response.getBody());
    }

    @Test
    void testCourseController_GetStudentCourseGradesJson_Unauthorized() {
        CourseController courseController = new CourseController(
                authService, courseService, null, cohortMemberRepository, cohortRepository, null, null, null
        );
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());

        ResponseEntity<?> response = courseController.getStudentCourseGradesJson("lap-trinh-web-nang-cao");

        assertEquals(401, response.getStatusCode().value());
    }
}
