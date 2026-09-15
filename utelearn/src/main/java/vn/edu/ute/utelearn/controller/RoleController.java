package vn.edu.ute.utelearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dto.RoleRequestDTO;
import vn.edu.ute.utelearn.entity.Permission;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.service.RoleService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public String listRoles(Model model) {
        model.addAttribute("roles", roleService.getAllRoles());
        return "admin/roles/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("roleForm")) {
            model.addAttribute("roleForm", new RoleRequestDTO());
        }
        model.addAttribute("modulesMap", roleService.getPermissionsGroupedByModule());
        model.addAttribute("isEdit", false);
        return "admin/roles/form";
    }

    @PostMapping("/create")
    public String processCreateRole(
        @Valid @ModelAttribute("roleForm") RoleRequestDTO roleForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modulesMap", roleService.getPermissionsGroupedByModule());
            model.addAttribute("isEdit", false);
            return "admin/roles/form";
        }

        try {
            roleService.createRole(roleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Khởi tạo vai trò mới '" + roleForm.getCode() + "' thành công!");
            return "redirect:/admin/roles";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("modulesMap", roleService.getPermissionsGroupedByModule());
            model.addAttribute("isEdit", false);
            return "admin/roles/form";
        } catch (Exception ex) {
            log.error("Lỗi khi tạo vai trò: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi trong quá trình tạo vai trò!");
            model.addAttribute("modulesMap", roleService.getPermissionsGroupedByModule());
            model.addAttribute("isEdit", false);
            return "admin/roles/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Role role = roleService.getRoleById(id);

        List<Long> currentPermissionIds = role.getPermissions().stream()
            .map(Permission::getId)
            .collect(Collectors.toList());

        RoleRequestDTO dto = RoleRequestDTO.builder()
            .id(role.getId())
            .code(role.getCode())
            .name(role.getName())
            .description(role.getDescription())
            .permissionIds(currentPermissionIds)
            .build();

        model.addAttribute("roleForm", dto);
        model.addAttribute("modulesMap", roleService.getPermissionsGroupedByModule());
        model.addAttribute("isEdit", true);
        model.addAttribute("roleCode", role.getCode());
        return "admin/roles/form";
    }

    @PostMapping("/{id}/edit")
    public String processUpdateRole(
        @PathVariable Long id,
        @Valid @ModelAttribute("roleForm") RoleRequestDTO roleForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modulesMap", roleService.getPermissionsGroupedByModule());
            model.addAttribute("isEdit", true);
            model.addAttribute("roleCode", roleForm.getCode());
            return "admin/roles/form";
        }

        try {
            roleService.updateRole(id, roleForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật vai trò '" + roleForm.getCode() + "' thành công!");
            return "redirect:/admin/roles";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("modulesMap", roleService.getPermissionsGroupedByModule());
            model.addAttribute("isEdit", true);
            model.addAttribute("roleCode", roleForm.getCode());
            return "admin/roles/form";
        } catch (Exception ex) {
            log.error("Lỗi khi cập nhật vai trò: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi trong quá trình cập nhật vai trò!");
            model.addAttribute("modulesMap", roleService.getPermissionsGroupedByModule());
            model.addAttribute("isEdit", true);
            model.addAttribute("roleCode", roleForm.getCode());
            return "admin/roles/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteRole(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roleService.deleteRole(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa vai trò thành công!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Lỗi khi xóa vai trò: {}", ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa vai trò này vì đang được gán cho người dùng!");
        }
        return "redirect:/admin/roles";
    }
}
