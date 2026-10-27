package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotificationDTO {
    private Long cohortId;
    private String cohortName;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String senderRole;
    private String message;
    private String actionUrl;
    private String createdAtFormatted;
}
