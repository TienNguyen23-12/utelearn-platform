package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.ModerationService;

import java.util.Map;

@RestController
@RequestMapping("/api/moderator/tasks")
@RequiredArgsConstructor
public class ModeratorApiController {

    private final ModerationService moderationService;
    private final AuthService authService;

    @PostMapping("/{taskId}/approve")
    @PreAuthorize("@rbac.hasRole('MODERATOR') or @rbac.hasRole('ADMIN')")
    public ResponseEntity<?> approveTask(@PathVariable Long taskId, @RequestBody Map<String, String> payload) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        moderationService.approveTask(taskId, currentUser.getId(), payload.get("feedback"));
        return ResponseEntity.ok(Map.of("message", "Đã duyệt thành công"));
    }

    @PostMapping("/{taskId}/reject")
    @PreAuthorize("@rbac.hasRole('MODERATOR') or @rbac.hasRole('ADMIN')")
    public ResponseEntity<?> rejectTask(@PathVariable Long taskId, @RequestBody Map<String, String> payload) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        moderationService.rejectTask(taskId, currentUser.getId(), payload.get("feedback"));
        return ResponseEntity.ok(Map.of("message", "Đã từ chối thành công"));
    }

    @PostMapping("/{taskId}/escalate")
    @PreAuthorize("@rbac.hasRole('MODERATOR') or @rbac.hasRole('ADMIN')")
    public ResponseEntity<?> escalateTask(@PathVariable Long taskId, @RequestBody Map<String, String> payload) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        moderationService.escalateTask(taskId, currentUser.getId(), payload.get("reason"));
        return ResponseEntity.ok(Map.of("message", "Đã chuyển cấp thành công"));
    }
}
