package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.service.RoleService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/roles")
@RequiredArgsConstructor
public class RoleApiController {

    private final RoleService roleService;

    @PostMapping("/{id}/delete")
    public ResponseEntity<Map<String, Object>> deleteRole(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            roleService.deleteRole(id);
            response.put("success", true);
            response.put("message", "Xóa vai trò thành công!");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", "Không thể xóa vai trò này vì đang được gán cho người dùng!");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
