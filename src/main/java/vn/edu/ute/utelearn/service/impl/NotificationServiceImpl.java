package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.NotificationRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.NotificationDTO;
import vn.edu.ute.utelearn.dto.NotificationEvent;
import vn.edu.ute.utelearn.entity.Notification;
import vn.edu.ute.utelearn.entity.NotificationCategory;
import vn.edu.ute.utelearn.entity.NotificationType;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.NotificationService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDTO sendNotification(NotificationEvent event) {
        log.info("[NotificationService] Bắt đầu gửi thông báo cho recipientId: {}, loại: {}", event.recipientId(), event.type());
        if (event.recipientId() == null) {
            log.warn("[NotificationService] Bỏ qua gửi thông báo do recipientId rỗng");
            return null;
        }

        User recipient = userRepository.findById(event.recipientId()).orElse(null);
        if (recipient == null) {
            log.warn("[NotificationService] Không tìm thấy người dùng nhận thông báo với ID: {}", event.recipientId());
            return null;
        }

        User sender = null;
        if (event.senderId() != null) {
            sender = userRepository.findById(event.senderId()).orElse(null);
        }

        NotificationCategory category = event.category() != null ? event.category() : NotificationCategory.ACADEMIC;
        Notification notification = Notification.builder()
                .recipient(recipient)
                .sender(sender)
                .title(event.title())
                .content(event.content())
                .type(event.type())
                .category(category)
                .actionUrl(event.actionUrl())
                .referenceId(event.referenceId())
                .referenceType(event.referenceType())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.saveAndFlush(notification);
        NotificationDTO dto = mapToDTO(saved);

        sendRealtimeNotification(recipient.getId(), dto);

        log.info("[NotificationService] Đã lưu và phát thông báo ID {} cho recipientId: {}", saved.getId(), recipient.getId());
        return dto;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotificationToUsers(List<Long> recipientIds, NotificationEvent templateEvent) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        log.info("[NotificationService] Gửi thông báo hàng loạt cho {} người nhận", recipientIds.size());
        for (Long recipientId : recipientIds) {
            if (recipientId != null) {
                NotificationEvent singleEvent = NotificationEvent.builder()
                        .recipientId(recipientId)
                        .senderId(templateEvent.senderId())
                        .title(templateEvent.title())
                        .content(templateEvent.content())
                        .type(templateEvent.type())
                        .category(templateEvent.category())
                        .actionUrl(templateEvent.actionUrl())
                        .referenceId(templateEvent.referenceId())
                        .referenceType(templateEvent.referenceType())
                        .build();
                sendNotification(singleEvent);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(Long userId, String categoryStr, Boolean unreadOnly, Pageable pageable) {
        log.info("[NotificationService] Lấy danh sách thông báo cho userId: {}, category: {}, unreadOnly: {}", userId, categoryStr, unreadOnly);
        NotificationCategory category = parseCategory(categoryStr);

        Page<Notification> page;
        if (category != null && Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByRecipientIdAndCategoryAndIsReadOrderByCreatedAtDesc(userId, category, false, pageable);
        } else if (category != null) {
            page = notificationRepository.findByRecipientIdAndCategoryOrderByCreatedAtDesc(userId, category, pageable);
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            page = notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable);
        } else {
            page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        }

        return page.map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getRecentNotifications(Long userId) {
        log.info("[NotificationService] Lấy 10 thông báo gần nhất cho userId: {}", userId);
        List<Notification> list = notificationRepository.findTop10ByRecipientIdOrderByCreatedAtDesc(userId);
        List<NotificationDTO> dtos = new ArrayList<>();
        for (Notification n : list) {
            dtos.add(mapToDTO(n));
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationDTO markAsRead(Long notificationId, Long userId) {
        log.info("[NotificationService] Đánh dấu đã đọc thông báo ID: {} cho userId: {}", notificationId, userId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thao tác trên thông báo này");
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return mapToDTO(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("[NotificationService] Đánh dấu tất cả thông báo đã đọc cho userId: {}", userId);
        notificationRepository.markAllAsReadByRecipientId(userId, Instant.now());
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        log.info("[NotificationService] Xóa thông báo ID: {} cho userId: {}", notificationId, userId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa thông báo này");
        }

        notificationRepository.delete(notification);
    }

    private void sendRealtimeNotification(Long recipientId, NotificationDTO dto) {
        try {
            simpMessagingTemplate.convertAndSend("/topic/notifications/" + recipientId, dto);
        } catch (Exception e) {
            log.error("[NotificationService] Lỗi đẩy thông báo real-time qua WebSocket cho user {}: {}", recipientId, e.getMessage());
        }
    }

    private NotificationCategory parseCategory(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty() || "ALL".equalsIgnoreCase(categoryStr.trim())) {
            return null;
        }
        try {
            return NotificationCategory.valueOf(categoryStr.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private NotificationDTO mapToDTO(Notification n) {
        if (n == null) {
            return null;
        }

        String senderName = n.getSender() != null ? n.getSender().getFullName() : "Hệ thống UTELearn";
        String senderAvatar = n.getSender() != null ? n.getSender().getAvatarUrl() : null;

        Instant createdAt = n.getCreatedAt() != null ? n.getCreatedAt() : Instant.now();
        String createdAtFormatted = DATE_TIME_FORMATTER.format(createdAt);
        String timeAgoFormatted = calculateTimeAgo(createdAt);
        String iconClass = resolveIconClass(n.getType());
        String badgeClass = resolveBadgeClass(n.getCategory());

        return NotificationDTO.builder()
                .id(n.getId())
                .recipientId(n.getRecipient().getId())
                .senderId(n.getSender() != null ? n.getSender().getId() : null)
                .senderName(senderName)
                .senderAvatar(senderAvatar)
                .title(n.getTitle())
                .content(n.getContent())
                .type(n.getType() != null ? n.getType().name() : null)
                .category(n.getCategory() != null ? n.getCategory().name() : "ACADEMIC")
                .actionUrl(n.getActionUrl())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .isRead(Boolean.TRUE.equals(n.getIsRead()))
                .readAt(n.getReadAt())
                .createdAt(createdAt)
                .createdAtFormatted(createdAtFormatted)
                .timeAgoFormatted(timeAgoFormatted)
                .iconClass(iconClass)
                .badgeClass(badgeClass)
                .build();
    }

    private String calculateTimeAgo(Instant instant) {
        if (instant == null) {
            return "";
        }
        Duration duration = Duration.between(instant, Instant.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return "Vừa xong";
        }
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " phút trước";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " giờ trước";
        }
        long days = duration.toDays();
        if (days < 7) {
            return days + " ngày trước";
        }
        return DATE_FORMATTER.format(instant);
    }

    private String resolveIconClass(NotificationType type) {
        if (type == null) {
            return "bi-bell-fill text-primary";
        }
        return switch (type) {
            case ASSIGNMENT_GRADED -> "bi-award-fill text-warning";
            case QUIZ_RESULT -> "bi-check2-circle text-info";
            case ASSIGNMENT_DEADLINE_REMINDER -> "bi-alarm-fill text-danger";
            case LESSON_UNLOCKED -> "bi-unlock-fill text-success";
            case NEW_ASSIGNMENT -> "bi-journal-check text-primary";
            case CERTIFICATE_ISSUED -> "bi-patch-check-fill text-success";
            case COHORT_ANNOUNCEMENT -> "bi-megaphone-fill text-warning";
            case COHORT_CHAT -> "bi-chat-dots-fill text-primary";
            case SUBMISSION_RECEIVED -> "bi-inbox-fill text-info";
            case PAYMENT_SUCCESS -> "bi-wallet2 text-success";
            case MODERATION_SUBMITTED -> "bi-shield-exclamation text-warning";
            case MODERATION_APPROVED -> "bi-patch-check-fill text-success";
            case MODERATION_REJECTED -> "bi-x-circle-fill text-danger";
            case SYSTEM_ALERT -> "bi-info-circle-fill text-secondary";
        };
    }

    private String resolveBadgeClass(NotificationCategory category) {
        if (category == null) {
            return "bg-primary-subtle text-primary border border-primary-subtle";
        }
        return switch (category) {
            case ACADEMIC -> "bg-primary-subtle text-primary border border-primary-subtle";
            case COHORT -> "bg-info-subtle text-info border border-info-subtle";
            case TRANSACTION -> "bg-success-subtle text-success border border-success-subtle";
            case SYSTEM -> "bg-secondary-subtle text-secondary border border-secondary-subtle";
        };
    }
}
