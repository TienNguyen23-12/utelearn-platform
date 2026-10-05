package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.CategoryService;
import vn.edu.ute.utelearn.service.SectionService;
import vn.edu.ute.utelearn.service.ModerationService;
import vn.edu.ute.utelearn.dto.CourseRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

@Controller
@RequestMapping("/instructor/courses")
@RequiredArgsConstructor
public class InstructorCourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final AuthService authService;
    private final SectionService sectionService;
    private final ModerationService moderationService;

    @GetMapping
    public String listCourses(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow(() -> new RuntimeException("Unauthorized"));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage = courseService.getCoursesByInstructor(currentUser.getId(), pageable);
        
        model.addAttribute("courses", coursePage.getContent());
        model.addAttribute("currentPage", coursePage.getNumber());
        model.addAttribute("totalPages", coursePage.getTotalPages());
        model.addAttribute("totalItems", coursePage.getTotalElements());
        return "instructor/courses/index";
    }

    @GetMapping("/create")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String showCreateForm(Model model) {
        model.addAttribute("categories", categoryService.getRootCategories());
        model.addAttribute("isEdit", false);
        return "instructor/courses/form";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String showEditForm(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        
        String objectivesText = "";
        try {
            List<String> objs = new ObjectMapper().readValue(course.getObjectives(), new TypeReference<List<String>>(){});
            objectivesText = String.join("\n", objs);
        } catch (Exception e) {}

        model.addAttribute("course", course);
        model.addAttribute("courseObjectives", objectivesText);
        model.addAttribute("categories", categoryService.getRootCategories());
        model.addAttribute("isEdit", true);
        return "instructor/courses/form";
    }
    @PostMapping("/create")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String createCourse(@Valid @ModelAttribute("course") CourseRequestDTO request, 
                               BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getRootCategories());
            model.addAttribute("isEdit", false);
            return "instructor/courses/form";
        }
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        courseService.createCourse(request, currentUser.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Tạo khóa học thành công");
        return "redirect:/instructor/courses";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String updateCourse(@PathVariable Long id, @Valid @ModelAttribute("course") CourseRequestDTO request, 
                               BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getRootCategories());
            model.addAttribute("isEdit", true);
            return "instructor/courses/form";
        }
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        courseService.updateCourse(id, request, currentUser.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khóa học thành công");
        return "redirect:/instructor/courses";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        courseService.deleteCourse(id, currentUser.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Xóa khóa học thành công");
        return "redirect:/instructor/courses";
    }

    @GetMapping("/{id}/curriculum")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String showCurriculum(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses"; // Or show 403
        }

        model.addAttribute("course", course);
        model.addAttribute("sections", sectionService.getSectionsByCourseId(id));
        return "instructor/courses/curriculum";
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String submitForReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        // Check if course belongs to user
        Course course = courseService.getCourseById(id);
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }
        
        moderationService.createCourseModerationTask(id, currentUser.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Khóa học đã được gửi duyệt thành công. Vui lòng chờ Moderator kiểm duyệt.");
        return "redirect:/instructor/courses";
    }
}
