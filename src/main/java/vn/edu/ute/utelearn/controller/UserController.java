package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dto.UserRoleAssignmentDTO;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.RoleService;
import vn.edu.ute.utelearn.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users/index";
    }

    @GetMapping("/{id}/edit")
    public String showEditRolesForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);

        List<Long> currentRoleIds = user.getRoles().stream()
            .map(Role::getId)
            .collect(Collectors.toList());

        UserRoleAssignmentDTO dto = UserRoleAssignmentDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phoneNumber(user.getPhoneNumber())
            .isActive(user.getIsActive())
            .roleIds(currentRoleIds)
            .build();

        model.addAttribute("userForm", dto);
        model.addAttribute("allRoles", roleService.getAllRoles());
        return "admin/users/form";
    }

    @PostMapping("/{id}/edit")
    public String processUpdateUserRoles(
        @PathVariable Long id,
        @ModelAttribute("userForm") UserRoleAssignmentDTO userForm,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        try {
            userService.updateUserRoles(id, userForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phân quyền cho tài khoản '" + userForm.getUsername() + "' thành công!");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("allRoles", roleService.getAllRoles());
            return "admin/users/form";
        } catch (Exception ex) {
            log.error("Lỗi khi cập nhật vai trò người dùng: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi trong quá trình cập nhật phân quyền!");
            model.addAttribute("allRoles", roleService.getAllRoles());
            return "admin/users/form";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái tài khoản thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }
}
