package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import vn.edu.ute.utelearn.controller.api.InstructorAssignmentApiController;
import vn.edu.ute.utelearn.controller.instructor.InstructorAssignmentController;
import vn.edu.ute.utelearn.dao.AssignmentRepository;
import vn.edu.ute.utelearn.dao.AssignmentSubmissionRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dto.AssignmentDTO;
import vn.edu.ute.utelearn.dto.AssignmentSubmissionDTO;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.AssignmentService;
import vn.edu.ute.utelearn.service.AuthService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstructorAssignmentGradingFeatureTest {

    @Mock
    private AssignmentService assignmentService;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private InstructorAssignmentController instructorAssignmentController;

    @InjectMocks
    private InstructorAssignmentApiController instructorAssignmentApiController;

    private User instructor;
    private User student;
    private Assignment assignment;
    private Lesson lesson;
    private Section section;
    private Cohort cohort;

    @BeforeEach
    void setUp() {
        Role instructorRole = Role.builder().id(2L).name("Giảng viên").code("INSTRUCTOR").build();
        Role studentRole = Role.builder().id(3L).name("Học viên").code("STUDENT").build();

        instructor = User.builder()
                .id(10L)
                .fullName("Thầy Giảng Viên")
                .email("instructor@ute.edu.vn")
                .roles(Set.of(instructorRole))
                .build();

        student = User.builder()
                .id(20L)
                .fullName("Nguyễn Sinh Viên")
                .email("student@ute.edu.vn")
                .roles(Set.of(studentRole))
                .build();

        cohort = Cohort.builder()
                .id(100L)
                .name("Lớp Lập trình Web - K26")
                .code("WEB-K26")
                .createdBy(instructor)
                .build();

        section = Section.builder()
                .id(200L)
                .title("Chương 1: Tổng quan")
                .cohort(cohort)
                .build();

        lesson = Lesson.builder()
                .id(300L)
                .title("Bài tập 1: Xây dựng UI")
                .lessonType("ASSIGNMENT")
                .section(section)
                .build();

        assignment = Assignment.builder()
                .id(400L)
                .lesson(lesson)
                .title("Bài tập 1: Xây dựng UI")
                .instruction("Thực hiện nộp bài file PDF hoặc text")
                .maxScore(new BigDecimal("10.0"))
                .passingScore(new BigDecimal("5.0"))
                .build();
    }

    @Test
    void testShowGradingPage_ReturnsGradingViewWithCorrectData() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(instructor));
        when(assignmentRepository.findById(400L)).thenReturn(Optional.of(assignment));
        when(cohortRepository.findById(100L)).thenReturn(Optional.of(cohort));

        AssignmentDTO dto = AssignmentDTO.builder()
                .id(400L)
                .lessonId(300L)
                .title("Bài tập 1: Xây dựng UI")
                .maxScore(new BigDecimal("10.0"))
                .passingScore(new BigDecimal("5.0"))
                .build();
        when(assignmentService.getAssignmentById(400L, 100L, 10L)).thenReturn(dto);

        AssignmentSubmissionDTO subPending = AssignmentSubmissionDTO.builder()
                .id(1001L)
                .assignmentId(400L)
                .userId(20L)
                .studentName("Nguyễn Sinh Viên")
                .studentEmail("student@ute.edu.vn")
                .status("SUBMITTED")
                .fileUrl("https://cloudinary.com/test.pdf")
                .fileName("baitap.pdf")
                .fileSizeBytes(1024L)
                .submittedAt(Instant.now())
                .isLate(false)
                .build();

        AssignmentSubmissionDTO subGraded = AssignmentSubmissionDTO.builder()
                .id(1002L)
                .assignmentId(400L)
                .userId(21L)
                .studentName("Trần Sinh Viên")
                .studentEmail("tran@ute.edu.vn")
                .status("GRADED")
                .score(new BigDecimal("9.0"))
                .isPassed(true)
                .isLate(false)
                .build();

        when(assignmentService.getSubmissionsForInstructor(400L, 100L, 10L)).thenReturn(List.of(subPending, subGraded));

        Model model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = instructorAssignmentController.showGradingPage(400L, 100L, null, model, redirectAttributes);

        assertEquals("instructor/assignments/grading", view);
        assertEquals(assignment, model.getAttribute("assignment"));
        assertEquals(dto, model.getAttribute("assignmentDto"));
        assertEquals(2L, model.getAttribute("totalSubmissions"));
        assertEquals(1L, model.getAttribute("gradedCount"));
        assertEquals(1L, model.getAttribute("pendingCount"));
        assertEquals(1001L, model.getAttribute("selectedSubmissionId"));
    }

    @Test
    void testShowGradingByLesson_RedirectsToAssignmentPage() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(instructor));

        AssignmentDTO dto = AssignmentDTO.builder()
                .id(400L)
                .lessonId(300L)
                .title("Bài tập 1: Xây dựng UI")
                .build();
        when(assignmentService.getOrCreateAssignmentForLesson(300L, 10L)).thenReturn(dto);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String redirect = instructorAssignmentController.showGradingByLesson(300L, 100L, redirectAttributes);

        assertEquals("redirect:/instructor/assignments/400?cohortId=100", redirect);
    }

    @Test
    void testGetSubmissionFile_UnauthorizedUser_ReturnsForbidden() {
        User otherUser = User.builder()
                .id(999L)
                .fullName("Người lạ")
                .email("other@gmail.com")
                .roles(Set.of(Role.builder().id(9L).name("Khách").code("GUEST").build()))
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(otherUser));

        AssignmentSubmission sub = AssignmentSubmission.builder()
                .id(5001L)
                .user(student)
                .fileUrl("https://cloudinary.com/secret.pdf")
                .fileName("secret.pdf")
                .build();
        when(assignmentSubmissionRepository.findById(5001L)).thenReturn(Optional.of(sub));

        ResponseEntity<?> response = instructorAssignmentApiController.getSubmissionFile(5001L, false);

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void testGetSubmissionFile_NoFileAttached_ReturnsNotFound() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(instructor));

        AssignmentSubmission sub = AssignmentSubmission.builder()
                .id(5002L)
                .user(student)
                .fileUrl(null)
                .build();
        when(assignmentSubmissionRepository.findById(5002L)).thenReturn(Optional.of(sub));

        ResponseEntity<?> response = instructorAssignmentApiController.getSubmissionFile(5002L, false);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void testGradeSubmission_ScoreBelowPassing_ReturnsFailedStatus() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(instructor));

        vn.edu.ute.utelearn.dto.AssignmentGradeDTO gradeDTO = vn.edu.ute.utelearn.dto.AssignmentGradeDTO.builder()
                .score(new BigDecimal("4.0"))
                .feedback("Chưa đạt yêu cầu tối thiểu")
                .build();

        AssignmentSubmissionDTO expectedDTO = AssignmentSubmissionDTO.builder()
                .id(5003L)
                .score(new BigDecimal("4.0"))
                .status("GRADED")
                .isPassed(false)
                .build();

        when(assignmentService.gradeSubmission(eq(5003L), any(vn.edu.ute.utelearn.dto.AssignmentGradeDTO.class), eq(instructor.getId())))
                .thenReturn(expectedDTO);

        ResponseEntity<?> response = instructorAssignmentApiController.gradeSubmission(5003L, gradeDTO);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof AssignmentSubmissionDTO);
        AssignmentSubmissionDTO result = (AssignmentSubmissionDTO) response.getBody();
        assertEquals(new BigDecimal("4.0"), result.getScore());
        assertFalse(result.getIsPassed());
    }
}

