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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.CategoryService;

@Controller
@RequestMapping("/instructor/courses")
@RequiredArgsConstructor
public class InstructorCourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final AuthService authService;

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
        model.addAttribute("course", course);
        model.addAttribute("categories", categoryService.getRootCategories());
        model.addAttribute("isEdit", true);
        return "instructor/courses/form";
    }
}
