package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.ModerationService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/moderator/tasks")
@RequiredArgsConstructor
public class ModeratorApiController {

    private final ModerationService moderationService;
    private final AuthService authService;

    @PostMapping("/{taskId}/approve")
    @PreAuthorize("@rbac.hasPermission('CONTENT_APPROVE')")
    public ResponseEntity<?> approveTask(@PathVariable Long taskId, @RequestBody Map<String, String> payload) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            moderationService.approveTask(taskId, currentUser.getId(), payload.get("feedback"));
            return ResponseEntity.ok(Map.of("message", "Đã duyệt thành công"));
        } catch (IllegalStateException e) {
            log.warn("[ModeratorApiController] Thao tac approve bi tu choi do vi pham quy tac nghiep vu: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{taskId}/reject")
    @PreAuthorize("@rbac.hasPermission('CONTENT_APPROVE')")
    public ResponseEntity<?> rejectTask(@PathVariable Long taskId, @RequestBody Map<String, String> payload) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            moderationService.rejectTask(taskId, currentUser.getId(), payload.get("feedback"));
            return ResponseEntity.ok(Map.of("message", "Đã từ chối thành công"));
        } catch (IllegalStateException e) {
            log.warn("[ModeratorApiController] Thao tac reject bi tu choi do vi pham quy tac nghiep vu: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{taskId}/escalate")
    @PreAuthorize("@rbac.hasPermission('CONTENT_APPROVE')")
    public ResponseEntity<?> escalateTask(@PathVariable Long taskId, @RequestBody Map<String, String> payload) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            moderationService.escalateTask(taskId, currentUser.getId(), payload.get("reason"));
            return ResponseEntity.ok(Map.of("message", "Đã chuyển cấp thành công"));
        } catch (IllegalStateException e) {
            log.warn("[ModeratorApiController] Thao tac escalate bi tu choi do vi pham quy tac nghiep vu: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{taskId}/reassign")
    @PreAuthorize("@rbac.hasRole('ADMIN')")
    public ResponseEntity<?> reassignTask(@PathVariable Long taskId, @RequestBody Map<String, Object> payload) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            Long targetModeratorId = null;
            if (payload.get("targetModeratorId") != null && !payload.get("targetModeratorId").toString().isBlank()) {
                targetModeratorId = Long.valueOf(payload.get("targetModeratorId").toString());
            }
            String note = payload.get("note") != null ? payload.get("note").toString() : "";
            moderationService.reassignTaskToModerator(taskId, currentUser.getId(), targetModeratorId, note);
            return ResponseEntity.ok(Map.of("message", "Đã chuyển việc duyệt về cho Moderator thành công"));
        } catch (IllegalStateException e) {
            log.warn("[ModeratorApiController] Thao tac reassign bi tu choi do vi pham quy tac nghiep vu: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/feedback")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFeedback(@RequestParam String itemType, @RequestParam Long itemId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            return moderationService.getLatestTask(itemType, itemId)
                    .map(task -> {
                        boolean isAuthor = task.getAuthor() != null && task.getAuthor().getId().equals(currentUser.getId());
                        boolean canView = isAuthor || currentUser.getRoles().stream().anyMatch(r -> 
                                "ADMIN".equalsIgnoreCase(r.getCode()) || "MODERATOR".equalsIgnoreCase(r.getCode()));
                        if (!canView) {
                            return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xem thông tin này"));
                        }
                        Map<String, Object> data = new java.util.HashMap<>();
                        data.put("id", task.getId());
                        data.put("itemType", task.getItemType());
                        data.put("itemId", task.getItemId());
                        data.put("status", task.getStatus());
                        data.put("feedback", task.getFeedback());
                        data.put("moderatorName", task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : "Kiểm duyệt viên");
                        data.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().toString() : "");
                        return ResponseEntity.ok(data);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
