package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.dao.EnrollmentRepository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Enrollment;
import vn.edu.ute.utelearn.entity.User;
import java.util.Optional;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final AuthService authService;
    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;

    @GetMapping
    public String showCourseList(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        currentUserOpt.ifPresent(user -> model.addAttribute("currentUser", user));
        return "courses/index";
    }

    @GetMapping("/my-courses")
    public String showMyCourses(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();
        model.addAttribute("currentUser", user);
        model.addAttribute("myCourses", enrollmentRepository.findByUserOrderByEnrolledAtDesc(user));
        return "courses/my-courses";
    }

    @GetMapping("/{slug}")
    public String showCourseDetail(@PathVariable String slug, Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        
        Course course;
        try {
            course = courseService.getCourseBySlug(slug);
        } catch (Exception e) {
            return "redirect:/courses";
        }
        
        boolean isEnrolled = false;
        boolean hasAccessRole = false;
        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            model.addAttribute("currentUser", user);
            isEnrolled = enrollmentRepository.existsByUserAndCourse(user, course);
            hasAccessRole = user.getRoles().stream().anyMatch(r -> 
                r.getCode().equals("ADMIN") || 
                r.getCode().equals("MODERATOR") || 
                r.getCode().equals("INSTRUCTOR"));
        }
        
        model.addAttribute("courseSlug", slug);
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("hasAccessRole", hasAccessRole);
        
        return "courses/detail";
    }

    @PostMapping("/{slug}/enroll")
    public String enrollCourse(@PathVariable String slug, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        Course course;
        try {
            course = courseService.getCourseBySlug(slug);
        } catch (Exception e) {
            return "redirect:/courses";
        }

        User user = currentUserOpt.get();
        if (!enrollmentRepository.existsByUserAndCourse(user, course)) {
            Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .build();
            enrollmentRepository.save(enrollment);
            redirectAttributes.addFlashAttribute("success", "Tham gia khóa học thành công!");
        }

        return "redirect:/courses/" + slug + "/learn";
    }

    @GetMapping("/{slug}/learn")
    public String showCourseContent(@PathVariable String slug, Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        Course course;
        try {
            course = courseService.getCourseBySlug(slug);
        } catch (Exception e) {
            return "redirect:/courses";
        }

        User user = currentUserOpt.get();
        boolean isEnrolled = enrollmentRepository.existsByUserAndCourse(user, course);
        boolean hasAccessRole = user.getRoles().stream().anyMatch(r -> 
            r.getCode().equals("ADMIN") || 
            r.getCode().equals("MODERATOR") || 
            r.getCode().equals("INSTRUCTOR"));
        
        if (!isEnrolled && !hasAccessRole) {
            return "redirect:/courses/" + slug;
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("courseSlug", slug);
        return "courses/learn";
    }
}
