package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import vn.edu.ute.utelearn.dao.*;
import vn.edu.ute.utelearn.dto.NotificationDTO;
import vn.edu.ute.utelearn.dto.NotificationEvent;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.NotificationService;
import vn.edu.ute.utelearn.service.impl.NotificationEventListener;
import vn.edu.ute.utelearn.service.impl.NotificationScheduler;
import vn.edu.ute.utelearn.service.impl.NotificationServiceImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationFeatureTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @InjectMocks
    private NotificationScheduler notificationScheduler;

    private User student;
    private User instructor;

    @BeforeEach
    void setUp() {
        student = User.builder().id(100L).fullName("Nguyễn Văn Học Viên").email("student@utelearn.edu.vn").build();
        instructor = User.builder().id(200L).fullName("TS. Giảng Viên").email("teacher@utelearn.edu.vn").build();
    }

    @Test
    void testSendNotification_Success() {
        NotificationEvent event = NotificationEvent.builder()
                .recipientId(100L)
                .senderId(200L)
                .title("Công bố điểm bài tập")
                .content("Bạn đạt 9.5 điểm cho bài tập 1")
                .type(NotificationType.ASSIGNMENT_GRADED)
                .category(NotificationCategory.ACADEMIC)
                .actionUrl("/modules/10/learn?lessonId=5")
                .referenceId(55L)
                .referenceType("ASSIGNMENT_SUBMISSION")
                .build();

        Notification savedNotification = Notification.builder()
                .id(1L)
                .recipient(student)
                .sender(instructor)
                .title(event.title())
                .content(event.content())
                .type(event.type())
                .category(event.category())
                .actionUrl(event.actionUrl())
                .referenceId(event.referenceId())
                .referenceType(event.referenceType())
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findById(100L)).thenReturn(Optional.of(student));
        when(userRepository.findById(200L)).thenReturn(Optional.of(instructor));
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenReturn(savedNotification);

        NotificationDTO result = notificationService.sendNotification(event);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Công bố điểm bài tập", result.getTitle());
        assertEquals("ASSIGNMENT_GRADED", result.getType());
        assertEquals("ACADEMIC", result.getCategory());
        assertFalse(result.getIsRead());
        verify(simpMessagingTemplate, times(1)).convertAndSend(eq("/topic/notifications/100"), any(NotificationDTO.class));
    }

    @Test
    void testSendNotificationToUsers_Bulk() {
        NotificationEvent template = NotificationEvent.builder()
                .senderId(200L)
                .title("Thông báo lớp học")
                .content("Lớp học có bài giảng mới")
                .type(NotificationType.COHORT_ANNOUNCEMENT)
                .category(NotificationCategory.COHORT)
                .build();

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(student));
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(99L);
            return n;
        });

        notificationService.sendNotificationToUsers(List.of(100L, 101L, 102L), template);

        verify(notificationRepository, times(3)).saveAndFlush(any(Notification.class));
        verify(simpMessagingTemplate, times(3)).convertAndSend(anyString(), any(NotificationDTO.class));
    }

    @Test
    void testGetUserNotifications_WithCategoryFilter() {
        Notification n1 = Notification.builder()
                .id(1L)
                .recipient(student)
                .title("Điểm bài tập")
                .content("Nội dung")
                .type(NotificationType.ASSIGNMENT_GRADED)
                .category(NotificationCategory.ACADEMIC)
                .createdAt(Instant.now())
                .isRead(false)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(notificationRepository.findByRecipientIdAndCategoryOrderByCreatedAtDesc(100L, NotificationCategory.ACADEMIC, pageable))
                .thenReturn(new PageImpl<>(List.of(n1)));

        Page<NotificationDTO> page = notificationService.getUserNotifications(100L, "ACADEMIC", false, pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("Điểm bài tập", page.getContent().get(0).getTitle());
    }

    @Test
    void testGetUnreadCount() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(100L)).thenReturn(4L);

        long count = notificationService.getUnreadCount(100L);
        assertEquals(4L, count);
    }

    @Test
    void testMarkAsRead_Success() {
        Notification notif = Notification.builder()
                .id(10L)
                .recipient(student)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDTO result = notificationService.markAsRead(10L, 100L);

        assertTrue(result.getIsRead());
        assertNotNull(result.getReadAt());
    }

    @Test
    void testMarkAsRead_Unauthorized() {
        Notification notif = Notification.builder()
                .id(10L)
                .recipient(student)
                .isRead(false)
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notif));

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(10L, 999L));
    }

    @Test
    void testMarkAllAsRead() {
        notificationService.markAllAsRead(100L);
        verify(notificationRepository, times(1)).markAllAsReadByRecipientId(eq(100L), any(Instant.class));
    }

    @Test
    void testDeleteNotification_Success() {
        Notification notif = Notification.builder()
                .id(10L)
                .recipient(student)
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notif));

        notificationService.deleteNotification(10L, 100L);
        verify(notificationRepository, times(1)).delete(notif);
    }

    @Test
    void testNotificationScheduler_CheckUpcomingDeadlines() {
        Cohort cohort = Cohort.builder().id(50L).name("Khóa học Java Core").createdBy(instructor).build();
        Lesson lesson = Lesson.builder().id(20L).title("Bài tập 1: OOP").lessonType("ASSIGNMENT").build();
        Assignment assignment = Assignment.builder().id(30L).lesson(lesson).title("Bài tập 1: OOP").build();

        Instant deadlineIn23h30m = Instant.now().plus(23, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES);
        CohortSchedule schedule = CohortSchedule.builder()
                .id(1L)
                .cohort(cohort)
                .lesson(lesson)
                .deadlineAt(deadlineIn23h30m)
                .build();

        CohortMember member = CohortMember.builder().cohort(cohort).user(student).build();

        when(cohortScheduleRepository.findAll()).thenReturn(List.of(schedule));
        when(assignmentRepository.findByLessonId(20L)).thenReturn(Optional.of(assignment));
        when(cohortMemberRepository.findByCohort_Id(50L)).thenReturn(List.of(member));
        when(assignmentSubmissionRepository.findByAssignmentIdAndUserIdAndCohortId(30L, 100L, 50L)).thenReturn(Optional.empty());
        when(notificationRepository.existsByRecipientIdAndTypeAndReferenceTypeAndReferenceId(
                eq(100L), eq(NotificationType.ASSIGNMENT_DEADLINE_REMINDER), eq("ASSIGNMENT_DEADLINE_24H"), eq(30L)))
                .thenReturn(false);

        notificationScheduler.checkUpcomingDeadlines();

        verify(eventPublisher, times(1)).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void testNotificationScheduler_CheckImminentAndDueDeadlines() {
        Cohort cohort = Cohort.builder().id(50L).name("Khóa học Java Core").createdBy(instructor).build();
        Lesson lesson1 = Lesson.builder().id(21L).title("Bài tập gấp").lessonType("ASSIGNMENT").build();
        Lesson lesson2 = Lesson.builder().id(22L).title("Bài tập vừa đến hạn").lessonType("ASSIGNMENT").build();

        CohortSchedule schedule1h = CohortSchedule.builder()
                .id(2L).cohort(cohort).lesson(lesson1)
                .deadlineAt(Instant.now().plus(40, ChronoUnit.MINUTES))
                .build();
        CohortSchedule scheduleDue = CohortSchedule.builder()
                .id(3L).cohort(cohort).lesson(lesson2)
                .deadlineAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        CohortMember member = CohortMember.builder().cohort(cohort).user(student).build();

        when(cohortScheduleRepository.findAll()).thenReturn(List.of(schedule1h, scheduleDue));
        when(cohortMemberRepository.findByCohort_Id(50L)).thenReturn(List.of(member));
        when(assignmentRepository.findByLessonId(21L)).thenReturn(Optional.empty());
        when(assignmentRepository.findByLessonId(22L)).thenReturn(Optional.empty());

        when(notificationRepository.existsByRecipientIdAndTypeAndReferenceTypeAndReferenceId(
                eq(100L), eq(NotificationType.ASSIGNMENT_DEADLINE_REMINDER), eq("ASSIGNMENT_DEADLINE_1H"), eq(21L)))
                .thenReturn(false);
        when(notificationRepository.existsByRecipientIdAndTypeAndReferenceTypeAndReferenceId(
                eq(100L), eq(NotificationType.ASSIGNMENT_DEADLINE_REMINDER), eq("ASSIGNMENT_DEADLINE_DUE"), eq(22L)))
                .thenReturn(false);

        notificationScheduler.checkUpcomingDeadlines();

        verify(eventPublisher, times(2)).publishEvent(any(NotificationEvent.class));
    }

    @Test
    void testNotificationEventListener_DelegatesToService() {
        NotificationService mockService = mock(NotificationService.class);
        NotificationEventListener listener = new NotificationEventListener(mockService);

        NotificationEvent event = NotificationEvent.builder()
                .recipientId(100L)
                .title("Thông báo")
                .content("Nội dung")
                .type(NotificationType.LESSON_UNLOCKED)
                .build();

        listener.handleNotificationEvent(event);

        verify(mockService, times(1)).sendNotification(event);
    }
}
