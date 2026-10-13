package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            userService.toggleUserStatus(id);
            User user = userService.getUserById(id);
            response.put("success", true);
            response.put("isActive", user.getIsActive());
            response.put("message", "Thay đổi trạng thái tài khoản thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
