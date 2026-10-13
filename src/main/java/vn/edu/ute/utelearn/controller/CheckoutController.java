package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CourseRepository courseRepository;
    private final AuthService authService;

    @GetMapping("/{cohortId}")
    public String showCheckoutPage(@PathVariable Long cohortId, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        if (cohort == null) {
            redirectAttributes.addFlashAttribute("error", "Lớp học không tồn tại.");
            return "redirect:/courses";
        }

        User user = currentUserOpt.get();
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã ghi danh lớp học này.");
            return "redirect:/courses/" + cohort.getCourse().getSlug();
        }

        if (cohort.getPrice() == null || cohort.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return "redirect:/courses/" + cohort.getCourse().getSlug();
        }

        Course course = courseRepository.findById(cohort.getCourse().getId()).orElse(null);

        model.addAttribute("isFullCourse", false);
        model.addAttribute("cohort", cohort);
        model.addAttribute("course", course);
        model.addAttribute("totalPrice", cohort.getPrice());
        model.addAttribute("currentUser", user);
        return "checkout/index";
    }

    @PostMapping("/{cohortId}/process")
    public String processPayment(@PathVariable Long cohortId, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        if (cohort == null) {
            redirectAttributes.addFlashAttribute("error", "Lớp học không tồn tại.");
            return "redirect:/courses";
        }

        Course course = courseRepository.findById(cohort.getCourse().getId()).orElse(null);
        if (course == null) {
            redirectAttributes.addFlashAttribute("error", "Lớp học không hợp lệ.");
            return "redirect:/courses";
        }

        User user = currentUserOpt.get();
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã ghi danh lớp học này.");
            return "redirect:/courses/" + course.getSlug();
        }

        if (List.of("ENROLLING", "UPCOMING").contains(cohort.getStatus())) {
            if (cohort.getCurrentEnrolled() >= cohort.getMaxCapacity()) {
                redirectAttributes.addFlashAttribute("error", "Lớp học đã đủ số lượng học viên.");
                return "redirect:/courses/" + course.getSlug();
            }

            CohortMember cohortMember = CohortMember.builder()
                    .user(user)
                    .cohort(cohort)
                    .build();
            cohortMemberRepository.save(cohortMember);

            cohort.setCurrentEnrolled(cohort.getCurrentEnrolled() + 1);
            cohortRepository.save(cohort);

            redirectAttributes.addFlashAttribute("success", "Thanh toán thành công! Bạn đã tham gia lớp học.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Lớp học này hiện không mở đăng ký.");
        }

        return "redirect:/courses/" + course.getSlug() + "/learn";
    }

    @GetMapping("/course/{slug}")
    public String showCourseCheckoutPage(@PathVariable String slug, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        Course course = courseRepository.findBySlug(slug).orElse(null);
        if (course == null) {
            redirectAttributes.addFlashAttribute("error", "Khóa học không tồn tại.");
            return "redirect:/courses";
        }

        User user = currentUserOpt.get();
        List<Cohort> availableCohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING"));
        if (availableCohorts.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Khóa học chưa mở đợt học nào.");
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

        model.addAttribute("isFullCourse", true);
        model.addAttribute("course", course);
        model.addAttribute("unenrolledCohorts", unenrolledCohorts);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("currentUser", user);
        return "checkout/index";
    }

    @PostMapping("/course/{slug}/process")
    public String processCoursePayment(@PathVariable String slug, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        Course course = courseRepository.findBySlug(slug).orElse(null);
        if (course == null) {
            redirectAttributes.addFlashAttribute("error", "Khóa học không tồn tại.");
            return "redirect:/courses";
        }

        User user = currentUserOpt.get();
        List<Cohort> availableCohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING"));

        List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        java.util.Set<Long> enrolledCohortIds = userMemberships.stream().map(cm -> cm.getCohort().getId()).collect(java.util.stream.Collectors.toSet());

        List<Cohort> unenrolledCohorts = availableCohorts.stream()
                .filter(c -> !enrolledCohortIds.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (unenrolledCohorts.isEmpty()) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã tham gia tất cả các đợt học của khóa học này.");
            return "redirect:/courses/" + slug + "/learn";
        }

        int enrolledCount = 0;
        for (Cohort c : unenrolledCohorts) {
            if (c.getCurrentEnrolled() < c.getMaxCapacity()) {
                CohortMember member = CohortMember.builder()
                        .user(user)
                        .cohort(c)
                        .build();
                cohortMemberRepository.save(member);
                c.setCurrentEnrolled(c.getCurrentEnrolled() + 1);
                cohortRepository.save(c);
                enrolledCount++;
            }
        }

        if (enrolledCount > 0) {
            redirectAttributes.addFlashAttribute("success", "Thanh toán trọn gói thành công! Bạn đã tham gia " + enrolledCount + " đợt học của khóa học.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Các đợt học đều đã đủ số lượng học viên.");
        }

        return "redirect:/courses/" + slug + "/learn";
    }
}
