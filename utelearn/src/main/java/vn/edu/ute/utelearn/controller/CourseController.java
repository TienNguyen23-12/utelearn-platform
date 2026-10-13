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
        java.util.Set<Long> enrolledCohortIds = new java.util.HashSet<>();
        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            model.addAttribute("currentUser", user);
            List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
            enrolledCohortIds = userMemberships.stream().map(cm -> cm.getCohort().getId()).collect(java.util.stream.Collectors.toSet());
            isEnrolled = !enrolledCohortIds.isEmpty();
            hasAccessRole = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN") ||
                    r.getCode().equals("MODERATOR") ||
                    r.getCode().equals("INSTRUCTOR"));
        }

        List<Cohort> availableCohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING"));
        final java.util.Set<Long> finalEnrolledCohortIds = enrolledCohortIds;
        List<java.util.Map<String, Object>> cohortList = availableCohorts.stream().map(c -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", c.getId());
            map.put("code", c.getCode());
            map.put("name", c.getName());
            map.put("currentEnrolled", c.getCurrentEnrolled());
            map.put("maxCapacity", c.getMaxCapacity());
            map.put("status", c.getStatus());
            map.put("price", c.getPrice());
            map.put("isEnrolled", finalEnrolledCohortIds.contains(c.getId()));
            return map;
        }).collect(java.util.stream.Collectors.toList());

        boolean isAllEnrolled = !availableCohorts.isEmpty() && availableCohorts.stream().allMatch(c -> finalEnrolledCohortIds.contains(c.getId()));
        java.math.BigDecimal fullCoursePrice = availableCohorts.stream()
                .map(Cohort::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal remainingCoursePrice = availableCohorts.stream()
                .filter(c -> !finalEnrolledCohortIds.contains(c.getId()))
                .map(Cohort::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        long unenrolledCount = availableCohorts.stream().filter(c -> !finalEnrolledCohortIds.contains(c.getId())).count();

        model.addAttribute("availableCohorts", cohortList);
        model.addAttribute("courseSlug", slug);
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("isAllEnrolled", isAllEnrolled);
        model.addAttribute("fullCoursePrice", fullCoursePrice);
        model.addAttribute("remainingCoursePrice", remainingCoursePrice);
        model.addAttribute("unenrolledCount", unenrolledCount);
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

        Optional<Cohort> cohortOpt = cohortRepository.findById(cohortId);
        if (cohortOpt.isEmpty() || !cohortOpt.get().getCourse().getId().equals(course.getId())) {
            redirectAttributes.addFlashAttribute("error", "Lớp học không tồn tại hoặc không thuộc khóa học này.");
            return "redirect:/courses/" + slug;
        }

        Cohort activeCohort = cohortOpt.get();
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(activeCohort.getId(), user.getId())) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã tham gia đợt học này rồi.");
            return "redirect:/courses/" + slug + "/learn";
        }

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

            redirectAttributes.addFlashAttribute("success", "Tham gia đợt học thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Lớp học này hiện không mở đăng ký.");
            return "redirect:/courses/" + slug;
        }

        return "redirect:/courses/" + slug + "/learn";
    }

    @PostMapping("/{slug}/enroll-all")
    public String enrollAll(@PathVariable String slug, RedirectAttributes redirectAttributes) {
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
        List<Cohort> availableCohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING"));

        if (availableCohorts.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Khóa học hiện chưa có đợt học nào mở đăng ký.");
            return "redirect:/courses/" + slug;
        }

        List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        java.util.Set<Long> enrolledCohortIds = userMemberships.stream().map(cm -> cm.getCohort().getId()).collect(java.util.stream.Collectors.toSet());

        List<Cohort> unenrolledCohorts = availableCohorts.stream()
                .filter(c -> !enrolledCohortIds.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (unenrolledCohorts.isEmpty()) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã tham gia tất cả các đợt học của khóa học này.");
            return "redirect:/courses/" + slug + "/learn";
        }

        java.math.BigDecimal totalPrice = unenrolledCohorts.stream()
                .map(Cohort::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        if (totalPrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return "redirect:/checkout/course/" + slug;
        }

        for (Cohort c : unenrolledCohorts) {
            if (c.getCurrentEnrolled() < c.getMaxCapacity()) {
                CohortMember member = CohortMember.builder()
                        .user(user)
                        .cohort(c)
                        .build();
                cohortMemberRepository.save(member);
                c.setCurrentEnrolled(c.getCurrentEnrolled() + 1);
                cohortRepository.save(c);
            }
        }

        redirectAttributes.addFlashAttribute("success", "Đã ghi danh toàn bộ các đợt học của khóa học!");
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
