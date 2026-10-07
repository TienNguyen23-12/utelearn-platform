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
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.User;
import java.util.Optional;
import java.util.List;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final AuthService authService;
    private final CourseService courseService;
    private final CohortMemberRepository cohortMemberRepository;
    private final CohortRepository cohortRepository;

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
        model.addAttribute("myCourses", cohortMemberRepository.findByUserOrderByEnrolledAtDesc(user));
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
            isEnrolled = cohortMemberRepository.existsByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
            hasAccessRole = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN") ||
                    r.getCode().equals("MODERATOR") ||
                    r.getCode().equals("INSTRUCTOR"));
        }

        List<Cohort> availableCohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING"));
        List<java.util.Map<String, Object>> cohortList = availableCohorts.stream().map(c -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", c.getId());
            map.put("code", c.getCode());
            map.put("name", c.getName());
            map.put("currentEnrolled", c.getCurrentEnrolled());
            map.put("maxCapacity", c.getMaxCapacity());
            map.put("status", c.getStatus());
            map.put("price", c.getPrice());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        model.addAttribute("availableCohorts", cohortList);
        model.addAttribute("courseSlug", slug);
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("hasAccessRole", hasAccessRole);

        return "courses/detail";
    }

    @PostMapping("/{slug}/enroll/{cohortId}")
    public String enrollCourse(@PathVariable String slug, @PathVariable Long cohortId,
            RedirectAttributes redirectAttributes) {
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

        if (!cohortMemberRepository.existsByCohort_Course_IdAndUser_Id(course.getId(), user.getId())) {
            Optional<Cohort> cohortOpt = cohortRepository.findById(cohortId);

            if (cohortOpt.isPresent() && cohortOpt.get().getCourse().getId().equals(course.getId())) {
                Cohort activeCohort = cohortOpt.get();
                if (List.of("ENROLLING", "UPCOMING").contains(activeCohort.getStatus())) {
                    if (activeCohort.getCurrentEnrolled() >= activeCohort.getMaxCapacity()) {
                        redirectAttributes.addFlashAttribute("error", "Lớp học đã đủ số lượng học viên.");
                        return "redirect:/courses/" + slug;
                    }

                    if (activeCohort.getPrice() != null && activeCohort.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        return "redirect:/checkout/" + cohortId;
                    }

                    CohortMember cohortMember = CohortMember.builder()
                            .user(user)
                            .cohort(activeCohort)
                            .build();
                    cohortMemberRepository.save(cohortMember);

                    activeCohort.setCurrentEnrolled(activeCohort.getCurrentEnrolled() + 1);
                    cohortRepository.save(activeCohort);

                    redirectAttributes.addFlashAttribute("success", "Tham gia lớp học thành công!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Lớp học này hiện không mở đăng ký.");
                    return "redirect:/courses/" + slug;
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Lớp học không tồn tại hoặc không thuộc khóa học này.");
                return "redirect:/courses/" + slug;
            }
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
        boolean isEnrolled = cohortMemberRepository.existsByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        boolean hasAccessRole = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN") ||
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
