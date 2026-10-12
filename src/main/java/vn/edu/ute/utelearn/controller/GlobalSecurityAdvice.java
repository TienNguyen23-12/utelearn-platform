package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.ute.utelearn.security.RbacHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GlobalSecurityAdvice: Tự động cung cấp danh sách vai trò và quyền hạn của người dùng
 * hiện tại vào tất cả các View Thymeleaf (dưới dạng List Java và chuỗi JSON)
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalSecurityAdvice {

    private final RbacHelper rbacHelper;
    private final vn.edu.ute.utelearn.service.AuthService authService;

    @ModelAttribute
    public void addSecurityAttributesToModel(Model model) {
        if (rbacHelper.isLoggedIn()) {
            List<String> userRoles = rbacHelper.getCurrentUserRoles();
            List<String> userPermissions = rbacHelper.getCurrentUserPermissions();

            model.addAttribute("currentUserRoles", userRoles);
            model.addAttribute("currentUserPermissions", userPermissions);
            model.addAttribute("currentUserRolesJson", toJsonArray(userRoles));
            model.addAttribute("currentUserPermissionsJson", toJsonArray(userPermissions));

            authService.getCurrentAuthenticatedUser().ifPresent(u -> model.addAttribute("currentUser", u));
        } else {
            model.addAttribute("currentUserRoles", List.of());
            model.addAttribute("currentUserPermissions", List.of());
            model.addAttribute("currentUserRolesJson", "[]");
            model.addAttribute("currentUserPermissionsJson", "[]");
        }
    }

    private String toJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        return items.stream()
            .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
            .collect(Collectors.joining(",", "[", "]"));
    }
}
