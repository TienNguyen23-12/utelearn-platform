package vn.edu.ute.utelearn.dto;

import lombok.Builder;
import vn.edu.ute.utelearn.entity.NotificationCategory;
import vn.edu.ute.utelearn.entity.NotificationType;

@Builder
public record NotificationEvent(
        Long recipientId,
        Long senderId,
        String title,
        String content,
        NotificationType type,
        NotificationCategory category,
        String actionUrl,
        Long referenceId,
        String referenceType
) {
}
