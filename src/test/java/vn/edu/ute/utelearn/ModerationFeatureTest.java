package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.impl.ModerationServiceImpl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ModerationFeatureTest {

    @Mock
    private ModerationTaskRepository moderationTaskRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private vn.edu.ute.utelearn.dao.CohortRepository cohortRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModeratorAttendanceRepository moderatorAttendanceRepository;

    @Mock
    private vn.edu.ute.utelearn.dao.LessonRepository lessonRepository;

    @Mock
    private vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private vn.edu.ute.utelearn.dao.SectionRepository sectionRepository;

    @Mock
    private vn.edu.ute.utelearn.service.NotificationService notificationService;

    @Mock
    private vn.edu.ute.utelearn.dao.InstructorAssetRepository assetRepository;

    @InjectMocks
    private ModerationServiceImpl moderationService;

    private User authorUser;
    private User moderatorUser;
    private User adminUser;
    private Course course;
    private ModerationTask task;
    private ModeratorAttendance attendance;

    @BeforeEach
    void setUp() {
        Role modRole = Role.builder().id(2L).code("MODERATOR").build();
        Role adminRole = Role.builder().id(1L).code("ADMIN").build();

        authorUser = User.builder().id(10L).fullName("Author Instructor").build();
        moderatorUser = User.builder().id(20L).fullName("Test Moderator").roles(Set.of(modRole)).build();
        adminUser = User.builder().id(30L).fullName("Super Admin").roles(Set.of(adminRole)).build();

        course = Course.builder().id(100L).title("Spring Boot Masterclass").status("DRAFT").build();

        task = ModerationTask.builder()
                .id(1L)
                .itemType("COURSE")
                .itemId(100L)
                .author(authorUser)
                .status("PENDING")
                .createdAt(Instant.now())
                .deadlineAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .build();

        attendance = ModeratorAttendance.builder()
                .id(50L)
                .moderator(moderatorUser)
                .workDate(LocalDate.now())
                .isActive(true)
                .completedTasksCount(2)
                .build();
    }

    @Test
    void testCreateCourseModerationTask_SetsSlaDeadlineAndAssignedAt() {
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(userRepository.findById(10L)).thenReturn(Optional.of(authorUser));
        when(moderationTaskRepository.save(any(ModerationTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModerationTask result = moderationService.createCourseModerationTask(100L, 10L);

        assertNotNull(result);
        assertEquals("PENDING_MODERATOR", course.getStatus());
        assertEquals("PENDING", result.getStatus());
        assertNotNull(result.getAssignedAt());
        assertNotNull(result.getDeadlineAt());
        assertTrue(result.getDeadlineAt().isAfter(Instant.now().plus(47, ChronoUnit.HOURS)));
        verify(courseRepository).save(course);
        verify(moderationTaskRepository).save(any(ModerationTask.class));
    }

    @Test
    void testApproveTask_AntiSelfModeration_ThrowsException() {
        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                moderationService.approveTask(1L, 10L, "Approve by author"));

        assertTrue(ex.getMessage().contains("không được phép tự kiểm duyệt"));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void testApproveTask_WithoutActiveAttendance_ThrowsException() {
        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(20L)).thenReturn(Optional.of(moderatorUser));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderatorUser, LocalDate.now()))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                moderationService.approveTask(1L, 20L, "Good course"));

        assertTrue(ex.getMessage().contains("chưa điểm danh ca trực"));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void testApproveTask_AdminBypassesAttendanceCheck_AndApproves() {
        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(30L)).thenReturn(Optional.of(adminUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        moderationService.approveTask(1L, 30L, "Approved by Admin");

        assertEquals("APPROVED", task.getStatus());
        assertEquals("PUBLISHED", course.getStatus());
        assertEquals(adminUser, task.getAssignedTo());
        verify(moderationTaskRepository).save(task);
        verify(courseRepository).save(course);
    }

    @Test
    void testApproveTask_Success_PublishesCourseAndIncrementsAttendanceStats() {
        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(20L)).thenReturn(Optional.of(moderatorUser));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderatorUser, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        moderationService.approveTask(1L, 20L, "Dat tieu chuan giang day");

        assertEquals("APPROVED", task.getStatus());
        assertEquals("PUBLISHED", course.getStatus());
        assertEquals(moderatorUser, task.getAssignedTo());
        assertEquals(3, attendance.getCompletedTasksCount());
        verify(moderatorAttendanceRepository).save(attendance);
        verify(courseRepository).save(course);
    }

    @Test
    void testRejectTask_AntiSelfModeration_ThrowsException() {
        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                moderationService.rejectTask(1L, 10L, "Tu choi khoa hoc"));

        assertTrue(ex.getMessage().contains("không được phép tự kiểm duyệt"));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void testRejectTask_Success_RejectsCourseAndIncrementsAttendanceStats() {
        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(20L)).thenReturn(Optional.of(moderatorUser));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderatorUser, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        moderationService.rejectTask(1L, 20L, "Chua dat tieu chuan video");

        assertEquals("REJECTED", task.getStatus());
        assertEquals("REJECTED", course.getStatus());
        assertEquals(moderatorUser, task.getAssignedTo());
        assertEquals(3, attendance.getCompletedTasksCount());
        verify(moderatorAttendanceRepository).save(attendance);
        verify(courseRepository).save(course);
    }

    @Test
    void testEscalateTask_Success_EscalatesToAdminAndUpdatesCourseStatus() {
        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(20L)).thenReturn(Optional.of(moderatorUser));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderatorUser, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        moderationService.escalateTask(1L, 20L, "Can kiem tra them ban quyen");

        assertEquals("ESCALATED", task.getStatus());
        assertEquals(2, task.getEscalationLevel());
        assertEquals(moderatorUser, task.getEscalatedBy());
        assertEquals("PENDING_ADMIN", course.getStatus());
        assertEquals(3, attendance.getCompletedTasksCount());
        verify(moderatorAttendanceRepository).save(attendance);
        verify(courseRepository).save(course);
    }

    @Test
    void testCreateCohortModerationTask_SetsSlaDeadlineAndAssignedAt() {
        vn.edu.ute.utelearn.entity.Cohort cohort = vn.edu.ute.utelearn.entity.Cohort.builder()
                .id(200L)
                .name("React Mastery Module")
                .moderationStatus("DRAFT")
                .build();

        when(cohortRepository.findById(200L)).thenReturn(Optional.of(cohort));
        when(userRepository.findById(10L)).thenReturn(Optional.of(authorUser));
        when(moderationTaskRepository.save(any(ModerationTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ModerationTask result = moderationService.createCohortModerationTask(200L, 10L);

        assertNotNull(result);
        assertEquals("PENDING_MODERATOR", cohort.getModerationStatus());
        assertEquals("PENDING", result.getStatus());
        assertEquals("COHORT", result.getItemType());
        assertNotNull(result.getAssignedAt());
        assertNotNull(result.getDeadlineAt());
        verify(cohortRepository).save(cohort);
        verify(moderationTaskRepository).save(any(ModerationTask.class));
    }

    @Test
    void testApproveCohortTask_Success_ApprovesCohortAndIncrementsAttendanceStats() {
        vn.edu.ute.utelearn.entity.Cohort cohort = vn.edu.ute.utelearn.entity.Cohort.builder()
                .id(200L)
                .name("React Mastery Module")
                .moderationStatus("PENDING_MODERATOR")
                .build();
        ModerationTask cohortTask = ModerationTask.builder()
                .id(2L)
                .itemType("COHORT")
                .itemId(200L)
                .author(authorUser)
                .status("PENDING")
                .build();

        when(moderationTaskRepository.findById(2L)).thenReturn(Optional.of(cohortTask));
        when(userRepository.findById(20L)).thenReturn(Optional.of(moderatorUser));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderatorUser, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(cohortRepository.findById(200L)).thenReturn(Optional.of(cohort));

        moderationService.approveTask(2L, 20L, "Module rat tot");

        assertEquals("APPROVED", cohortTask.getStatus());
        assertEquals("APPROVED", cohort.getModerationStatus());
        assertEquals(moderatorUser, cohortTask.getAssignedTo());
        assertEquals(3, attendance.getCompletedTasksCount());
        verify(moderatorAttendanceRepository).save(attendance);
        verify(cohortRepository).save(cohort);
    }

    @Test
    void testRejectCohortTask_Success_RejectsCohortAndIncrementsAttendanceStats() {
        vn.edu.ute.utelearn.entity.Cohort cohort = vn.edu.ute.utelearn.entity.Cohort.builder()
                .id(200L)
                .name("React Mastery Module")
                .moderationStatus("PENDING_MODERATOR")
                .build();
        ModerationTask cohortTask = ModerationTask.builder()
                .id(2L)
                .itemType("COHORT")
                .itemId(200L)
                .author(authorUser)
                .status("PENDING")
                .build();

        when(moderationTaskRepository.findById(2L)).thenReturn(Optional.of(cohortTask));
        when(userRepository.findById(20L)).thenReturn(Optional.of(moderatorUser));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderatorUser, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(cohortRepository.findById(200L)).thenReturn(Optional.of(cohort));

        moderationService.rejectTask(2L, 20L, "Thieu tai lieu tham khao");

        assertEquals("REJECTED", cohortTask.getStatus());
        assertEquals("REJECTED", cohort.getModerationStatus());
        assertEquals(moderatorUser, cohortTask.getAssignedTo());
        assertEquals(3, attendance.getCompletedTasksCount());
        verify(moderatorAttendanceRepository).save(attendance);
        verify(cohortRepository).save(cohort);
    }

    @Test
    void testSyncStatusFromExternalChange_Published_UpdatesPendingTasksToApproved() {
        ModerationTask pendingTask = ModerationTask.builder()
                .id(5L)
                .itemType("COURSE")
                .itemId(100L)
                .status("PENDING")
                .build();

        when(moderationTaskRepository.findByItemTypeAndItemIdAndStatusIn(eq("COURSE"), eq(100L), any()))
                .thenReturn(java.util.List.of(pendingTask));

        moderationService.syncStatusFromExternalChange("COURSE", 100L, "PUBLISHED", adminUser);

        assertEquals("APPROVED", pendingTask.getStatus());
        assertEquals(adminUser, pendingTask.getAssignedTo());
        assertTrue(pendingTask.getFeedback().contains("Admin"));
        verify(moderationTaskRepository).save(pendingTask);
    }

    @Test
    void testGetLatestTask_ReturnsLatestTask() {
        when(moderationTaskRepository.findTopByItemTypeAndItemIdOrderByCreatedAtDesc("COURSE", 100L))
                .thenReturn(Optional.of(task));

        Optional<ModerationTask> result = moderationService.getLatestTask("COURSE", 100L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void testReassignTaskToModerator_ByAdmin_Success() {
        task.setStatus("ESCALATED");
        task.setEscalationLevel(2);

        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(30L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(20L)).thenReturn(Optional.of(moderatorUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        moderationService.reassignTaskToModerator(1L, 30L, 20L, "Kiem tra ky phan bai tap");

        assertEquals("PENDING", task.getStatus());
        assertEquals(1, task.getEscalationLevel());
        assertEquals(moderatorUser, task.getAssignedTo());
        assertEquals("Kiem tra ky phan bai tap", task.getEscalationReason());
        assertEquals("PENDING_MODERATOR", course.getStatus());
        verify(moderationTaskRepository).save(task);
        verify(courseRepository).save(course);
    }

    @Test
    void testReassignTaskToModerator_ToGeneralQueue_Success() {
        task.setStatus("ESCALATED");
        task.setEscalationLevel(2);

        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(30L)).thenReturn(Optional.of(adminUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        moderationService.reassignTaskToModerator(1L, 30L, null, "");

        assertEquals("PENDING", task.getStatus());
        assertEquals(1, task.getEscalationLevel());
        assertNull(task.getAssignedTo());
        assertEquals("Chuyển giao lại bởi Admin", task.getEscalationReason());
        assertEquals("PENDING_MODERATOR", course.getStatus());
        verify(moderationTaskRepository).save(task);
        verify(courseRepository).save(course);
    }

    @Test
    void testReassignTaskToModerator_NonAdmin_ThrowsException() {
        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(20L)).thenReturn(Optional.of(moderatorUser));

        assertThrows(IllegalStateException.class, () ->
                moderationService.reassignTaskToModerator(1L, 20L, 20L, "Note")
        );
    }

    @Test
    void testReassignTaskToModerator_AssignToAuthor_ThrowsException() {
        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(30L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(10L)).thenReturn(Optional.of(authorUser));

        assertThrows(IllegalStateException.class, () ->
                moderationService.reassignTaskToModerator(1L, 30L, 10L, "Note")
        );
    }

    @Test
    void testModerationNotifications_SentToModeratorsAndInstructor() {
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(userRepository.findById(10L)).thenReturn(Optional.of(authorUser));
        when(userRepository.findActiveModerators()).thenReturn(java.util.List.of(moderatorUser));
        when(moderationTaskRepository.save(any(ModerationTask.class))).thenAnswer(invocation -> {
            ModerationTask saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        moderationService.createCourseModerationTask(100L, 10L);

        verify(notificationService).sendNotificationToUsers(
                eq(java.util.List.of(20L)),
                argThat(ev -> ev.type() == vn.edu.ute.utelearn.entity.NotificationType.MODERATION_SUBMITTED
                        && "/moderator/review/99".equals(ev.actionUrl()))
        );

        when(moderationTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(userRepository.findById(20L)).thenReturn(Optional.of(moderatorUser));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderatorUser, LocalDate.now()))
                .thenReturn(Optional.of(attendance));

        moderationService.approveTask(1L, 20L, "Noi dung chuan");

        verify(notificationService).sendNotification(argThat(ev ->
                Long.valueOf(10L).equals(ev.recipientId())
                        && ev.type() == vn.edu.ute.utelearn.entity.NotificationType.MODERATION_APPROVED
                        && ev.content().contains("Spring Boot Masterclass")
                        && ev.content().contains("Noi dung chuan")
        ));

        task.setStatus("PENDING");
        moderationService.rejectTask(1L, 20L, "Can bo sung video");

        verify(notificationService).sendNotification(argThat(ev ->
                Long.valueOf(10L).equals(ev.recipientId())
                        && ev.type() == vn.edu.ute.utelearn.entity.NotificationType.MODERATION_REJECTED
                        && ev.content().contains("Can bo sung video")
        ));
    }
}
