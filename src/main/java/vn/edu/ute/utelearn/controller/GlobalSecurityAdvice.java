package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.ute.utelearn.security.RbacHelper;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalSecurityAdvice {

    private final RbacHelper rbacHelper;
    private final vn.edu.ute.utelearn.service.AuthService authService;
    private final vn.edu.ute.utelearn.dao.ModerationTaskRepository moderationTaskRepository;
    private final vn.edu.ute.utelearn.service.CartService cartService;

    @ModelAttribute
    public void addSecurityAttributesToModel(Model model) {
        if (rbacHelper.isLoggedIn()) {
            List<String> userRoles = rbacHelper.getCurrentUserRoles();
            List<String> userPermissions = rbacHelper.getCurrentUserPermissions();

            model.addAttribute("currentUserRoles", userRoles);
            model.addAttribute("currentUserPermissions", userPermissions);
            model.addAttribute("currentUserRolesJson", toJsonArray(userRoles));
            model.addAttribute("currentUserPermissionsJson", toJsonArray(userPermissions));

            authService.getCurrentAuthenticatedUser().ifPresent(u -> {
                model.addAttribute("currentUser", u);
                model.addAttribute("cartItemCount", cartService.getCartItemCount(u));
            });

            if (rbacHelper.hasRole("ADMIN")) {
                long escalatedCount = moderationTaskRepository.countByStatus("ESCALATED");
                model.addAttribute("escalatedTaskCount", escalatedCount);
            }
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
