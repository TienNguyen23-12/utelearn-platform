package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.ChatMessageRequestDTO;
import vn.edu.ute.utelearn.dto.ChatMessageResponseDTO;
import vn.edu.ute.utelearn.dto.ChatNotificationDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CohortChatService;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CohortChatController {

    private final CohortChatService cohortChatService;
    private final AuthService authService;
    private final UserRepository userRepository;

    @MessageMapping("/cohort/{cohortId}/send")
    public void handleChatMessage(
            @DestinationVariable Long cohortId,
            @Payload ChatMessageRequestDTO messageRequest,
            Principal principal
    ) {
        User user = null;
        if (principal != null) {
            user = userRepository.findByUsernameOrEmail(principal.getName(), principal.getName()).orElse(null);
        }
        if (user == null) {
            user = authService.getCurrentAuthenticatedUser().orElse(null);
        }
        if (user == null) {
            return;
        }

        cohortChatService.processAndSendMessage(cohortId, user, messageRequest);
    }

    @GetMapping("/api/cohorts/{cohortId}/messages")
    @ResponseBody
    public ResponseEntity<List<ChatMessageResponseDTO>> getCohortMessages(@PathVariable Long cohortId) {
        User user = authService.getCurrentAuthenticatedUser().orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!cohortChatService.canUserAccessCohort(user, cohortId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(cohortChatService.getRecentMessages(cohortId, user));
    }

    @PostMapping("/api/cohorts/{cohortId}/messages")
    @ResponseBody
    public ResponseEntity<ChatMessageResponseDTO> sendCohortMessage(
            @PathVariable Long cohortId,
            @RequestBody ChatMessageRequestDTO request
    ) {
        User user = authService.getCurrentAuthenticatedUser().orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!cohortChatService.canUserAccessCohort(user, cohortId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ChatMessageResponseDTO response = cohortChatService.processAndSendMessage(cohortId, user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/cohorts/notifications/recent")
    @ResponseBody
    public ResponseEntity<List<ChatNotificationDTO>> getRecentNotifications() {
        User user = authService.getCurrentAuthenticatedUser().orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(cohortChatService.getRecentNotifications(user));
    }
}
