package vn.edu.ute.utelearn.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dto.CategoryRequestDTO;
import vn.edu.ute.utelearn.entity.Category;
import vn.edu.ute.utelearn.service.CategoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String listCategories(Model model) {
        log.info("[AdminCategoryController] Truy cập danh sách danh mục");
        List<Category> categories = categoryService.getAllCategories();
        long rootCount = categories.stream().filter(c -> c.getParent() == null).count();
        long subCount = categories.stream().filter(c -> c.getParent() != null).count();

        Map<Long, Long> courseCountMap = new HashMap<>();
        for (Category cat : categories) {
            courseCountMap.put(cat.getId(), categoryService.countCoursesInCategory(cat.getId()));
        }

        model.addAttribute("categories", categories);
        model.addAttribute("totalCount", categories.size());
        model.addAttribute("rootCount", rootCount);
        model.addAttribute("subCount", subCount);
        model.addAttribute("courseCountMap", courseCountMap);
        return "admin/categories/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new CategoryRequestDTO());
        }
        model.addAttribute("parentCategories", categoryService.getAllCategories());
        model.addAttribute("isEdit", false);
        return "admin/categories/form";
    }

    @PostMapping("/create")
    public String processCreateCategory(
            @Valid @ModelAttribute("categoryForm") CategoryRequestDTO categoryForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("parentCategories", categoryService.getAllCategories());
            model.addAttribute("isEdit", false);
            return "admin/categories/form";
        }

        try {
            Category created = categoryService.createCategory(categoryForm);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo mới danh mục '" + created.getName() + "' thành công!");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("parentCategories", categoryService.getAllCategories());
            model.addAttribute("isEdit", false);
            return "admin/categories/form";
        } catch (Exception ex) {
            log.error("[AdminCategoryController] Lỗi khi tạo danh mục: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra sự cố trong quá trình khởi tạo danh mục!");
            model.addAttribute("parentCategories", categoryService.getAllCategories());
            model.addAttribute("isEdit", false);
            return "admin/categories/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryService.getCategoryById(id);

            CategoryRequestDTO dto = CategoryRequestDTO.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .slug(category.getSlug())
                    .parentId(category.getParent() != null ? category.getParent().getId() : null)
                    .build();

            List<Category> availableParents = categoryService.getAllCategories().stream()
                    .filter(c -> !c.getId().equals(id))
                    .collect(Collectors.toList());

            model.addAttribute("categoryForm", dto);
            model.addAttribute("category", category);
            model.addAttribute("parentCategories", availableParents);
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/{id}/edit")
    public String processUpdateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoryForm") CategoryRequestDTO categoryForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            List<Category> availableParents = categoryService.getAllCategories().stream()
                    .filter(c -> !c.getId().equals(id))
                    .collect(Collectors.toList());
            model.addAttribute("parentCategories", availableParents);
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        }

        try {
            Category updated = categoryService.updateCategory(id, categoryForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục '" + updated.getName() + "' thành công!");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException ex) {
            List<Category> availableParents = categoryService.getAllCategories().stream()
                    .filter(c -> !c.getId().equals(id))
                    .collect(Collectors.toList());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("parentCategories", availableParents);
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        } catch (Exception ex) {
            log.error("[AdminCategoryController] Lỗi khi cập nhật danh mục ID {}: {}", id, ex.getMessage(), ex);
            List<Category> availableParents = categoryService.getAllCategories().stream()
                    .filter(c -> !c.getId().equals(id))
                    .collect(Collectors.toList());
            model.addAttribute("errorMessage", "Đã xảy ra sự cố trong quá trình lưu thông tin danh mục!");
            model.addAttribute("parentCategories", availableParents);
            model.addAttribute("isEdit", true);
            return "admin/categories/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String processDeleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục thành công!");
        } catch (Exception ex) {
            log.error("[AdminCategoryController] Lỗi khi xóa danh mục ID {}: {}", id, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa danh mục: " + ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
