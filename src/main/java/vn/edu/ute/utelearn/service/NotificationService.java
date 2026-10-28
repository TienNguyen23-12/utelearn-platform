package vn.edu.ute.utelearn.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.ute.utelearn.dto.NotificationDTO;
import vn.edu.ute.utelearn.dto.NotificationEvent;

import java.util.List;

public interface NotificationService {

    NotificationDTO sendNotification(NotificationEvent event);

    void sendNotificationToUsers(List<Long> recipientIds, NotificationEvent templateEvent);

    Page<NotificationDTO> getUserNotifications(Long userId, String category, Boolean unreadOnly, Pageable pageable);

    List<NotificationDTO> getRecentNotifications(Long userId);

    long getUnreadCount(Long userId);

    NotificationDTO markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);
}
