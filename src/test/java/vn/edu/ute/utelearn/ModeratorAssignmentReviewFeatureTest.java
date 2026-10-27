package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import vn.edu.ute.utelearn.controller.api.AssignmentApiController;
import vn.edu.ute.utelearn.dao.AssignmentRepository;
import vn.edu.ute.utelearn.dao.AssignmentSubmissionRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.AssignmentDTO;
import vn.edu.ute.utelearn.entity.Assignment;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AssignmentService;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CloudinaryService;
import vn.edu.ute.utelearn.service.impl.AssignmentServiceImpl;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ModeratorAssignmentReviewFeatureTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private vn.edu.ute.utelearn.dao.LessonCompletionRepository lessonCompletionRepository;

    @Mock
    private vn.edu.ute.utelearn.dao.CohortRepository cohortRepository;

    @Mock
    private vn.edu.ute.utelearn.dao.CohortMemberRepository cohortMemberRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private AssignmentApiController assignmentApiController;

    private User moderatorUser;
    private Lesson sampleLesson;
    private Assignment sampleAssignment;

    @BeforeEach
    void setUp() {
        assignmentApiController = new AssignmentApiController(assignmentService, authService);

        Role modRole = Role.builder().id(3L).code("MODERATOR").name("Moderator").build();
        moderatorUser = User.builder()
                .id(99L)
                .username("mod_tran")
                .fullName("Trần Thị Kiểm Duyệt")
                .email("mod@utelearn.edu.vn")
                .roles(Set.of(modRole))
                .build();

        sampleLesson = Lesson.builder()
                .id(105L)
                .title("Bài tập lớn: Thiết kế hệ thống LMS")
                .lessonType("ASSIGNMENT")
                .build();

        sampleAssignment = Assignment.builder()
                .id(12L)
                .lesson(sampleLesson)
                .title("Bài tập lớn: Thiết kế hệ thống LMS")
                .instruction("Xây dựng sơ đồ ERD và viết mã nguồn")
                .maxScore(new BigDecimal("10.0"))
                .passingScore(new BigDecimal("5.0"))
                .allowLateSubmission(true)
                .allowFileSubmission(true)
                .allowTextSubmission(true)
                .allowedFileTypes("pdf,zip,rar")
                .maxFileSizeMb(25)
                .build();
    }

    @Test
    void testSecurityUtilsModeratorIdentification() {
        assertTrue(SecurityUtils.isModerator(moderatorUser));

        User rolePrefixUser = User.builder()
                .roles(Set.of(Role.builder().code("ROLE_MODERATOR").build()))
                .build();
        assertTrue(SecurityUtils.isModerator(rolePrefixUser));

        User nonModUser = User.builder()
                .roles(Set.of(Role.builder().code("STUDENT").name("Học viên").build()))
                .build();
        assertFalse(SecurityUtils.isModerator(nonModUser));
    }

    @Test
    void testModeratorGetsAssignmentWithReviewerFlag() {
        when(assignmentRepository.findByLessonId(105L)).thenReturn(Optional.of(sampleAssignment));
        when(userRepository.findById(99L)).thenReturn(Optional.of(moderatorUser));

        AssignmentDTO dto = assignmentService.getAssignmentByLessonId(105L, null, 99L);

        assertNotNull(dto);
        assertEquals("Bài tập lớn: Thiết kế hệ thống LMS", dto.getTitle());
        assertTrue(dto.getIsReviewer());
        assertEquals(new BigDecimal("10.0"), dto.getMaxScore());
        assertEquals("pdf,zip,rar", dto.getAllowedFileTypes());
    }

    @Test
    void testModeratorAutoCreatesDefaultAssignmentWhenMissing() {
        when(assignmentRepository.findByLessonId(105L)).thenReturn(Optional.empty());
        when(lessonRepository.findById(105L)).thenReturn(Optional.of(sampleLesson));
        when(userRepository.findById(99L)).thenReturn(Optional.of(moderatorUser));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment a = invocation.getArgument(0);
            a.setId(999L);
            return a;
        });

        AssignmentDTO dto = assignmentService.getAssignmentByLessonId(105L, null, 99L);

        assertNotNull(dto);
        assertEquals(105L, dto.getLessonId());
        assertTrue(dto.getIsReviewer());
        assertNotNull(dto.getMaxScore());
        verify(assignmentRepository, times(1)).save(any(Assignment.class));
    }

    @Test
    void testAssignmentApiControllerReturnsOkForModerator() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(moderatorUser));
        when(assignmentRepository.findByLessonId(105L)).thenReturn(Optional.of(sampleAssignment));
        when(userRepository.findById(99L)).thenReturn(Optional.of(moderatorUser));

        ResponseEntity<?> response = assignmentApiController.getAssignmentForLesson(105L, null);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof AssignmentDTO);
        AssignmentDTO body = (AssignmentDTO) response.getBody();
        assertTrue(body.getIsReviewer());
        assertEquals("Bài tập lớn: Thiết kế hệ thống LMS", body.getTitle());
    }
}
