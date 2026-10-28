package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long recipientId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String title;
    private String content;
    private String type;
    private String category;
    private String actionUrl;
    private Long referenceId;
    private String referenceType;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;
    private String createdAtFormatted;
    private String timeAgoFormatted;
    private String badgeClass;
    private String iconClass;
}
