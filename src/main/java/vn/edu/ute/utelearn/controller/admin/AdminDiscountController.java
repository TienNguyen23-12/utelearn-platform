package vn.edu.ute.utelearn.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dto.DiscountDTO;
import vn.edu.ute.utelearn.entity.Discount;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.DiscountService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/admin/discounts")
@PreAuthorize("hasAnyAuthority('ORDER_MANAGE', 'ADMIN', 'ROLE_ADMIN', 'FINANCE', 'ROLE_FINANCE') or @rbac.hasPermission('ORDER_MANAGE')")
@RequiredArgsConstructor
public class AdminDiscountController {

    private final DiscountService discountService;
    private final AuthService authService;

    @GetMapping
    public String listDiscounts(Model model) {
        List<Discount> discounts = discountService.getAllDiscounts();
        model.addAttribute("discounts", discounts);
        model.addAttribute("activePage", "admin_discounts");
        model.addAttribute("totalDiscounts", discounts.size());
        model.addAttribute("activeCount", discounts.stream().filter(Discount::isActive).count());
        return "admin/discounts/index";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("discount", DiscountDTO.builder().active(true).discountType("PERCENTAGE").build());
        model.addAttribute("isEdit", false);
        model.addAttribute("activePage", "admin_discounts");
        return "admin/discounts/form";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Discount> discountOpt = discountService.getDiscountById(id);
        if (discountOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy mã giảm giá.");
            return "redirect:/admin/discounts";
        }
        Discount d = discountOpt.get();
        DiscountDTO dto = DiscountDTO.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .discountType(d.getDiscountType())
                .discountValue(d.getDiscountValue())
                .maxDiscountAmount(d.getMaxDiscountAmount())
                .minOrderAmount(d.getMinOrderAmount())
                .usageLimit(d.getUsageLimit())
                .usedCount(d.getUsedCount())
                .active(d.isActive())
                .startDate(d.getStartDate())
                .endDate(d.getEndDate())
                .build();

        model.addAttribute("discount", dto);
        model.addAttribute("isEdit", true);
        model.addAttribute("activePage", "admin_discounts");
        return "admin/discounts/form";
    }

    @PostMapping("/save")
    public String saveDiscount(@ModelAttribute DiscountDTO dto, RedirectAttributes redirectAttributes) {
        try {
            Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
            String createdBy = currentUserOpt.map(User::getUsername).orElse("ADMIN");

            if (dto.getId() != null) {
                discountService.updateDiscount(dto.getId(), dto);
                redirectAttributes.addFlashAttribute("success", "Cập nhật mã giảm giá thành công!");
            } else {
                discountService.createDiscount(dto, createdBy);
                redirectAttributes.addFlashAttribute("success", "Tạo mới mã giảm giá '" + dto.getCode().toUpperCase() + "' thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (dto.getId() != null) {
                return "redirect:/admin/discounts/" + dto.getId() + "/edit";
            }
            return "redirect:/admin/discounts/new";
        }
        return "redirect:/admin/discounts";
    }

    @PostMapping("/{id}/toggle")
    public String toggleDiscount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean active = discountService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", active ? "Đã kích hoạt mã giảm giá." : "Đã tạm dừng mã giảm giá.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/discounts";
    }

    @PostMapping("/{id}/delete")
    public String deleteDiscount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            discountService.deleteDiscount(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa mã giảm giá thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/discounts";
    }
}
