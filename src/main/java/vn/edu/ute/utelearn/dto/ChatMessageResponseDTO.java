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
public class ChatMessageResponseDTO {
    private Long id;
    private Long cohortId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String senderRole;
    private String message;
    private String messageType;
    private Instant createdAt;
    private String createdAtFormatted;
}
