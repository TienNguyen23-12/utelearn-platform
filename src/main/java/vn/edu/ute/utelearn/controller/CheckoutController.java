package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Slf4j
@Controller
@Transactional
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CourseRepository courseRepository;
    private final AuthService authService;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final LessonRepository lessonRepository;

    @GetMapping("/{cohortId}")
    public String showCheckoutPage(@PathVariable Long cohortId, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        if (cohort == null) {
            redirectAttributes.addFlashAttribute("error", "Module không tồn tại.");
            return "redirect:/courses";
        }

        User user = currentUserOpt.get();
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã ghi danh module này.");
            return (cohort.getCourse() != null) ? "redirect:/courses/" + cohort.getCourse().getSlug() : "redirect:/cohorts";
        }

        if (cohort.getPrice() == null || cohort.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return (cohort.getCourse() != null) ? "redirect:/courses/" + cohort.getCourse().getSlug() : "redirect:/cohorts";
        }

        Course course = cohort.getCourse() != null ? courseRepository.findById(cohort.getCourse().getId()).orElse(null) : null;

        log.info("[CheckoutController] Hiển thị trang checkout cho module id: {}, user: {}", cohortId, user.getEmail());

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
            redirectAttributes.addFlashAttribute("error", "Module không tồn tại.");
            return "redirect:/courses";
        }

        Course course = cohort.getCourse() != null ? courseRepository.findById(cohort.getCourse().getId()).orElse(null) : null;

        User user = currentUserOpt.get();
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã ghi danh module này.");
            return course != null ? "redirect:/courses/" + course.getSlug() : "redirect:/cohorts";
        }

        if (List.of("ENROLLING", "UPCOMING").contains(cohort.getStatus())) {
            if (cohort.getCurrentEnrolled() >= cohort.getMaxCapacity()) {
                redirectAttributes.addFlashAttribute("error", "Module đã đủ số lượng học viên.");
                return course != null ? "redirect:/courses/" + course.getSlug() : "redirect:/cohorts";
            }

            CohortMember cohortMember = CohortMember.builder()
                    .user(user)
                    .cohort(cohort)
                    .build();
            cohortMemberRepository.save(cohortMember);

            cohort.setCurrentEnrolled((int) cohortMemberRepository.countByCohort_Id(cohort.getId()));
            cohortRepository.save(cohort);

            log.info("[CheckoutController] Thanh toán thành công module id: {}, user: {}", cohortId, user.getEmail());
            redirectAttributes.addFlashAttribute("success", "Thanh toán thành công! Bạn đã tham gia module.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Module này hiện không mở đăng ký.");
        }

        if (course != null) {
            return "redirect:/courses/" + course.getSlug() + "/modules/" + cohort.getId() + "/learn";
        }
        return "redirect:/cohorts/" + cohort.getId() + "/learn";
    }

    private boolean isFullCourseCohort(Cohort c) {
        if (c == null) {
            return false;
        }
        boolean isCodeAll = c.getCode() != null && c.getCode().endsWith("-ALL");
        boolean isNameAll = c.getName() != null && c.getName().toLowerCase().startsWith("toàn bộ khóa học");
        return isCodeAll || isNameAll;
    }

    private List<Cohort> getOrCreateAvailableCohorts(Course course) {
        List<Cohort> cohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING", "IN_PROGRESS"));
        List<Cohort> realModules = cohorts.stream()
                .filter(c -> !isFullCourseCohort(c))
                .collect(java.util.stream.Collectors.toList());
        if (!realModules.isEmpty()) {
            return realModules;
        }
        if (!cohorts.isEmpty()) {
            return cohorts;
        }
        Cohort defaultCohort = Cohort.builder()
                .course(course)
                .name("Toàn bộ khóa học: " + course.getTitle())
                .code(course.getCode() + "-ALL")
                .price(course.getPrice() != null ? course.getPrice() : java.math.BigDecimal.ZERO)
                .status("ENROLLING")
                .visibility("PRIVATE")
                .moderationStatus("APPROVED")
                .maxCapacity(1000)
                .currentEnrolled(0)
                .enrollmentStart(Instant.now())
                .studyStart(Instant.now())
                .build();
        defaultCohort = cohortRepository.save(defaultCohort);
        List<Lesson> lessons = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(course.getId());
        for (Lesson l : lessons) {
            CohortSchedule cs = CohortSchedule.builder()
                    .cohort(defaultCohort)
                    .lesson(l)
                    .unlockAt(Instant.now())
                    .build();
            cohortScheduleRepository.save(cs);
        }
        return List.of(defaultCohort);
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
        List<Cohort> availableCohorts = getOrCreateAvailableCohorts(course);

        List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        List<CohortMember> realUserMemberships = userMemberships.stream()
                .filter(cm -> !isFullCourseCohort(cm.getCohort()))
                .collect(java.util.stream.Collectors.toList());
        java.util.Set<Long> enrolledCohortIds = realUserMemberships.stream().map(cm -> cm.getCohort().getId()).collect(java.util.stream.Collectors.toSet());

        List<Cohort> unenrolledCohorts = availableCohorts.stream()
                .filter(c -> !enrolledCohortIds.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (unenrolledCohorts.isEmpty()) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã tham gia tất cả các module của khóa học này.");
            Long firstModId = availableCohorts.get(0).getId();
            return "redirect:/courses/" + slug + "/modules/" + firstModId + "/learn";
        }

        java.math.BigDecimal sumOfCohorts = unenrolledCohorts.stream()
                .map(Cohort::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal coursePrice = course.getPrice() != null ? course.getPrice() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalPaidAmount = realUserMemberships.stream()
                .map(cm -> cm.getCohort() != null && cm.getCohort().getPrice() != null ? cm.getCohort().getPrice() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        boolean isUpgrade = !realUserMemberships.isEmpty();
        java.math.BigDecimal totalPrice;

        if (isUpgrade) {
            java.math.BigDecimal diff = coursePrice.subtract(totalPaidAmount);
            totalPrice = diff.compareTo(java.math.BigDecimal.ZERO) > 0 ? diff : java.math.BigDecimal.ZERO;
            if (sumOfCohorts.compareTo(java.math.BigDecimal.ZERO) > 0 && sumOfCohorts.compareTo(totalPrice) < 0) {
                totalPrice = sumOfCohorts;
            }
        } else {
            totalPrice = (coursePrice.compareTo(java.math.BigDecimal.ZERO) > 0) ? coursePrice : sumOfCohorts;
        }

        java.math.BigDecimal discountAmount = (sumOfCohorts.compareTo(totalPrice) > 0)
                ? sumOfCohorts.subtract(totalPrice)
                : java.math.BigDecimal.ZERO;

        log.info("[CheckoutController] Checkout trọn gói course: {}, user: {}, isUpgrade: {}, totalPaid: {}, total: {}, discount: {}",
                slug, user.getEmail(), isUpgrade, totalPaidAmount, totalPrice, discountAmount);

        model.addAttribute("isFullCourse", true);
        model.addAttribute("isUpgrade", isUpgrade);
        model.addAttribute("course", course);
        model.addAttribute("unenrolledCohorts", unenrolledCohorts);
        model.addAttribute("coursePrice", coursePrice);
        model.addAttribute("totalPaidAmount", totalPaidAmount);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("sumOfCohorts", sumOfCohorts);
        model.addAttribute("discountAmount", discountAmount);
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
        List<Cohort> availableCohorts = getOrCreateAvailableCohorts(course);

        List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        List<CohortMember> realUserMemberships = userMemberships.stream()
                .filter(cm -> !isFullCourseCohort(cm.getCohort()))
                .collect(java.util.stream.Collectors.toList());
        java.util.Set<Long> enrolledCohortIds = realUserMemberships.stream().map(cm -> cm.getCohort().getId()).collect(java.util.stream.Collectors.toSet());

        List<Cohort> unenrolledCohorts = availableCohorts.stream()
                .filter(c -> !enrolledCohortIds.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (unenrolledCohorts.isEmpty()) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã tham gia tất cả các module của khóa học này.");
            Long firstModId = availableCohorts.get(0).getId();
            return "redirect:/courses/" + slug + "/modules/" + firstModId + "/learn";
        }

        int enrolledCount = 0;
        for (Cohort c : unenrolledCohorts) {
            if (c.getCurrentEnrolled() < c.getMaxCapacity()) {
                CohortMember member = CohortMember.builder()
                        .user(user)
                        .cohort(c)
                        .build();
                cohortMemberRepository.save(member);
                c.setCurrentEnrolled((int) cohortMemberRepository.countByCohort_Id(c.getId()));
                cohortRepository.save(c);
                enrolledCount++;
            }
        }

        cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId()).stream()
                .filter(this::isFullCourseCohort)
                .findFirst()
                .ifPresent(allCohort -> {
                    if (!cohortMemberRepository.existsByCohort_IdAndUser_Id(allCohort.getId(), user.getId())) {
                        cohortMemberRepository.save(CohortMember.builder().user(user).cohort(allCohort).build());
                        allCohort.setCurrentEnrolled((int) cohortMemberRepository.countByCohort_Id(allCohort.getId()));
                        cohortRepository.save(allCohort);
                    }
                });

        log.info("[CheckoutController] Hoàn tất thanh toán trọn gói course: {}, user: {}, số module đã đăng ký: {}",
                slug, user.getEmail(), enrolledCount);

        boolean isUpgrade = !realUserMemberships.isEmpty();
        if (enrolledCount > 0) {
            if (isUpgrade) {
                redirectAttributes.addFlashAttribute("success", "Nâng cấp trọn gói khóa học thành công! Bạn đã mở khóa toàn bộ " + enrolledCount + " module còn lại.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Thanh toán trọn gói thành công! Bạn đã tham gia " + enrolledCount + " module của khóa học.");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Các module đều đã đủ số lượng học viên.");
        }

        Long targetId = unenrolledCohorts.get(0).getId();
        return "redirect:/courses/" + slug + "/modules/" + targetId + "/learn";
    }
}
