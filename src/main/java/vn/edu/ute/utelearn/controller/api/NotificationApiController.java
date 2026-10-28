package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.dto.NotificationDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để xem thông báo"));
        }
        try {
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
            Page<NotificationDTO> result = notificationService.getUserNotifications(
                    currentUser.get().getId(), category, unreadOnly, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[NotificationApiController] Lỗi lấy danh sách thông báo: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentNotifications() {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để xem thông báo"));
        }
        try {
            List<NotificationDTO> recent = notificationService.getRecentNotifications(currentUser.get().getId());
            return ResponseEntity.ok(recent);
        } catch (Exception e) {
            log.error("[NotificationApiController] Lỗi lấy thông báo gần đây: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.ok(Map.of("unreadCount", 0));
        }
        try {
            long count = notificationService.getUnreadCount(currentUser.get().getId());
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            log.error("[NotificationApiController] Lỗi lấy số lượng chưa đọc: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("unreadCount", 0));
        }
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập"));
        }
        try {
            NotificationDTO updated = notificationService.markAsRead(id, currentUser.get().getId());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("[NotificationApiController] Lỗi đánh dấu đã đọc thông báo {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead() {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập"));
        }
        try {
            notificationService.markAllAsRead(currentUser.get().getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("[NotificationApiController] Lỗi đánh dấu tất cả đã đọc: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập"));
        }
        try {
            notificationService.deleteNotification(id, currentUser.get().getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("[NotificationApiController] Lỗi xóa thông báo {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
