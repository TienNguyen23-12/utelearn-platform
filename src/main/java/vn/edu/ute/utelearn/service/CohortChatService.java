package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.ChatMessageRequestDTO;
import vn.edu.ute.utelearn.dto.ChatMessageResponseDTO;
import vn.edu.ute.utelearn.dto.ChatNotificationDTO;
import vn.edu.ute.utelearn.entity.User;

import java.util.List;

public interface CohortChatService {
    boolean canUserAccessCohort(User user, Long cohortId);
    boolean canUserModerateCohort(User user, Long cohortId);
    List<ChatMessageResponseDTO> getRecentMessages(Long cohortId, User user);
    ChatMessageResponseDTO processAndSendMessage(Long cohortId, User user, ChatMessageRequestDTO request);
    void deleteMessage(Long cohortId, Long messageId, User user, String mode);
    List<ChatNotificationDTO> getRecentNotifications(User user);
}
