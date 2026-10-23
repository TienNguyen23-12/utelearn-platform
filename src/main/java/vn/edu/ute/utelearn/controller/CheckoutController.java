package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

import vn.edu.ute.utelearn.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.dao.OrderRepository;
import vn.edu.ute.utelearn.entity.Order;

@Slf4j
@Controller
@Transactional
@RequestMapping("/checkout")
public class CheckoutController {

    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CourseRepository courseRepository;
    private final AuthService authService;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final LessonRepository lessonRepository;
    private final OrderService orderService;
    private final UtelearnProperties properties;
    private final OrderRepository orderRepository;

    public CheckoutController(CohortRepository cohortRepository,
                              CohortMemberRepository cohortMemberRepository,
                              CourseRepository courseRepository,
                              AuthService authService,
                              CohortScheduleRepository cohortScheduleRepository,
                              LessonRepository lessonRepository) {
        this(cohortRepository, cohortMemberRepository, courseRepository, authService, cohortScheduleRepository, lessonRepository, null, null, null);
    }

    @Autowired
    public CheckoutController(CohortRepository cohortRepository,
                              CohortMemberRepository cohortMemberRepository,
                              CourseRepository courseRepository,
                              AuthService authService,
                              CohortScheduleRepository cohortScheduleRepository,
                              LessonRepository lessonRepository,
                              OrderService orderService,
                              UtelearnProperties properties,
                              OrderRepository orderRepository) {
        this.cohortRepository = cohortRepository;
        this.cohortMemberRepository = cohortMemberRepository;
        this.courseRepository = courseRepository;
        this.authService = authService;
        this.cohortScheduleRepository = cohortScheduleRepository;
        this.lessonRepository = lessonRepository;
        this.orderService = orderService;
        this.properties = properties;
        this.orderRepository = orderRepository;
    }

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

        java.math.BigDecimal userBalance = orderService != null ? orderService.getUserBalance(user) : java.math.BigDecimal.ZERO;
        model.addAttribute("isFullCourse", false);
        model.addAttribute("cohort", cohort);
        model.addAttribute("course", course);
        model.addAttribute("totalPrice", cohort.getPrice());
        model.addAttribute("currentUser", user);
        model.addAttribute("userBalance", userBalance);
        model.addAttribute("formattedUserBalance", String.format("%,d VNĐ", userBalance.longValue()));
        return "checkout/index";
    }

    @PostMapping("/{cohortId}/process")
    public String processPayment(@PathVariable Long cohortId, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        if (properties != null && properties.getPayment() != null && !properties.getPayment().isSimulationEnabled()) {
            redirectAttributes.addFlashAttribute("error", "Chức năng thanh toán thử nghiệm đã bị vô hiệu hóa.");
            return "redirect:/checkout/" + cohortId;
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

            if (orderService != null) {
                try {
                    Order order = orderService.createCohortOrder(user, cohortId, "DEMO_SIMULATION");
                    orderService.fulfillOrder(order.getOrderCode(), "DEMO_PAY_" + System.currentTimeMillis(), "DEMO_SIMULATION");
                } catch (Exception ex) {
                    log.warn("[CheckoutController] Không tạo được order record cho mock payment: {}", ex.getMessage());
                }
            }

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

    @PostMapping("/{cohortId}/initiate")
    public String initiateCohortPayment(@PathVariable Long cohortId, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();
        try {
            Order order = orderService.createCohortOrder(user, cohortId, "VIETQR_SEPAY");
            if ("SUCCESS".equalsIgnoreCase(order.getStatus())) {
                redirectAttributes.addFlashAttribute("success", "Đăng ký module thành công!");
                Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
                if (cohort != null && cohort.getCourse() != null) {
                    return "redirect:/courses/" + cohort.getCourse().getSlug() + "/modules/" + cohort.getId() + "/learn";
                }
                return "redirect:/cohorts/" + cohortId + "/learn";
            }
            return "redirect:/checkout/payment/" + order.getOrderCode();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout/" + cohortId;
        }
    }

    @PostMapping("/{cohortId}/offline")
    public String initiateCohortOfflinePayment(@PathVariable Long cohortId, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();
        try {
            Order order = orderService.createOfflineCohortOrder(user, cohortId, "Học viên đăng ký thanh toán tại quầy");
            redirectAttributes.addFlashAttribute("success", "Đã tạo đơn hàng #" + order.getOrderCode() + "! Hướng dẫn thanh toán đã được gửi tới email của bạn. Vui lòng mang mã đơn đến quầy thu ngân.");
            return "redirect:/orders/" + order.getOrderCode();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout/" + cohortId;
        }
    }


    private List<Cohort> getOrCreateAvailableCohorts(Course course) {
        List<Cohort> cohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING", "IN_PROGRESS"));
        List<Cohort> realModules = cohorts.stream()
                .filter(c -> !(c != null && c.isFullCourseCohort()))
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
                .filter(cm -> !(cm.getCohort() != null && cm.getCohort().isFullCourseCohort()))
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

        java.math.BigDecimal userBalance = orderService != null ? orderService.getUserBalance(user) : java.math.BigDecimal.ZERO;
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
        model.addAttribute("userBalance", userBalance);
        model.addAttribute("formattedUserBalance", String.format("%,d VNĐ", userBalance.longValue()));
        return "checkout/index";
    }

    @PostMapping("/course/{slug}/initiate")
    public String initiateCoursePayment(@PathVariable String slug,
                                        @RequestParam(required = false) List<Long> cohortIds,
                                        RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();
        try {
            Order order = orderService.createCourseOrder(user, slug, cohortIds, "VIETQR_SEPAY");
            if ("SUCCESS".equalsIgnoreCase(order.getStatus())) {
                redirectAttributes.addFlashAttribute("success", "Đăng ký khóa học thành công!");
                return "redirect:/courses/" + slug;
            }
            return "redirect:/checkout/payment/" + order.getOrderCode();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout/course/" + slug;
        }
    }

    @PostMapping("/course/{slug}/offline")
    public String initiateCourseOfflinePayment(@PathVariable String slug,
                                               @RequestParam(required = false) List<Long> cohortIds,
                                               RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();
        try {
            Order order = orderService.createOfflineCourseOrder(user, slug, cohortIds, "Học viên đăng ký thanh toán trọn gói tại quầy");
            redirectAttributes.addFlashAttribute("success", "Đã tạo đơn hàng #" + order.getOrderCode() + "! Hướng dẫn thanh toán đã được gửi tới email của bạn. Vui lòng mang mã đơn đến quầy thu ngân.");
            return "redirect:/orders/" + order.getOrderCode();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout/course/" + slug;
        }
    }

    @PostMapping("/course/{slug}/process")
    public String processCoursePayment(@PathVariable String slug,
                                       @RequestParam(required = false) List<Long> cohortIds,
                                       RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        if (properties != null && properties.getPayment() != null && !properties.getPayment().isSimulationEnabled()) {
            redirectAttributes.addFlashAttribute("error", "Chức năng thanh toán thử nghiệm đã bị vô hiệu hóa.");
            return "redirect:/checkout/course/" + slug;
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
                .filter(cm -> !(cm.getCohort() != null && cm.getCohort().isFullCourseCohort()))
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

        List<Cohort> targetCohorts = unenrolledCohorts;
        if (cohortIds != null && !cohortIds.isEmpty()) {
            java.util.Set<Long> selectedSet = new java.util.HashSet<>(cohortIds);
            targetCohorts = unenrolledCohorts.stream()
                    .filter(c -> selectedSet.contains(c.getId()))
                    .collect(java.util.stream.Collectors.toList());
            if (targetCohorts.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất 1 module để thanh toán.");
                return "redirect:/checkout/course/" + slug;
            }
        }

        int enrolledCount = 0;
        for (Cohort c : targetCohorts) {
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

        List<Cohort> realModules = availableCohorts.stream()
                .filter(c -> !(c != null && c.isFullCourseCohort()))
                .collect(java.util.stream.Collectors.toList());
        boolean allRealEnrolled = !realModules.isEmpty() && realModules.stream()
                .allMatch(m -> cohortMemberRepository.existsByCohort_IdAndUser_Id(m.getId(), user.getId()));

        if (allRealEnrolled) {
            cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId()).stream()
                    .filter(c -> c != null && c.isFullCourseCohort())
                    .findFirst()
                    .ifPresent(allCohort -> {
                        if (!cohortMemberRepository.existsByCohort_IdAndUser_Id(allCohort.getId(), user.getId())) {
                            cohortMemberRepository.save(CohortMember.builder().user(user).cohort(allCohort).build());
                            allCohort.setCurrentEnrolled((int) cohortMemberRepository.countByCohort_Id(allCohort.getId()));
                            cohortRepository.save(allCohort);
                        }
                    });
        }

        if (orderService != null) {
            try {
                Order order = orderService.createCourseOrder(user, slug, cohortIds, "DEMO_SIMULATION");
                orderService.fulfillOrder(order.getOrderCode(), "DEMO_PAY_" + System.currentTimeMillis(), "DEMO_SIMULATION");
            } catch (Exception ex) {
                log.warn("[CheckoutController] Không tạo được order record cho mock payment: {}", ex.getMessage());
            }
        }

        log.info("[CheckoutController] Hoàn tất thanh toán course: {}, user: {}, số module đã đăng ký: {}",
                slug, user.getEmail(), enrolledCount);

        boolean isUpgrade = !realUserMemberships.isEmpty();
        boolean isAllRemaining = targetCohorts.size() == unenrolledCohorts.size();
        if (enrolledCount > 0) {
            if (isUpgrade) {
                if (isAllRemaining) {
                    redirectAttributes.addFlashAttribute("success", "Nâng cấp trọn gói khóa học thành công! Bạn đã mở khóa toàn bộ " + enrolledCount + " module còn lại.");
                } else {
                    redirectAttributes.addFlashAttribute("success", "Thanh toán thành công! Bạn đã mở khóa " + enrolledCount + " module đã chọn.");
                }
            } else {
                redirectAttributes.addFlashAttribute("success", "Thanh toán trọn gói thành công! Bạn đã tham gia " + enrolledCount + " module của khóa học.");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Các module đều đã đủ số lượng học viên.");
        }

        Long targetId = targetCohorts.get(0).getId();
        return "redirect:/courses/" + slug + "/modules/" + targetId + "/learn";
    }

    @GetMapping("/payment/{orderCode}")
    public String showPaymentPage(@PathVariable String orderCode, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();

        Order order = orderService.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng không tồn tại.");
            return "redirect:/courses";
        }

        boolean isOwner = order.getUser() != null && order.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) || "FINANCE".equalsIgnoreCase(r.getCode()));
        if (!isOwner && !isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập đơn hàng này.");
            return "redirect:/home";
        }

        if ("SUCCESS".equalsIgnoreCase(order.getStatus())) {
            redirectAttributes.addFlashAttribute("success", "Đơn hàng đã được thanh toán thành công!");
            var dto = orderService.toResponseDTO(order);
            return "redirect:" + dto.getTargetLearnUrl();
        }

        String qrUrl = orderService.generateVietQrUrl(order.getOrderCode(), order.getTotalAmount());
        var dto = orderService.toResponseDTO(order);

        model.addAttribute("order", order);
        model.addAttribute("orderDTO", dto);
        model.addAttribute("qrUrl", qrUrl);
        model.addAttribute("bankId", properties.getPayment().getBankId());
        model.addAttribute("accountNo", properties.getPayment().getAccountNo());
        model.addAttribute("accountName", properties.getPayment().getAccountName());
        model.addAttribute("currentUser", user);

        return "checkout/payment";
    }

    @PostMapping("/payment/{orderCode}/simulate")
    public String simulatePayment(@PathVariable String orderCode, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        if (properties != null && properties.getPayment() != null && !properties.getPayment().isSimulationEnabled()) {
            redirectAttributes.addFlashAttribute("error", "Chức năng mô phỏng thanh toán đã bị vô hiệu hóa.");
            return "redirect:/checkout/payment/" + orderCode;
        }

        User currentUser = currentUserOpt.get();
        Order existingOrder = orderService.findByOrderCode(orderCode).orElse(null);
        if (existingOrder == null) {
            redirectAttributes.addFlashAttribute("error", "Đơn hàng không tồn tại.");
            return "redirect:/orders";
        }

        boolean isOwner = existingOrder.getUser() != null && existingOrder.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) || "FINANCE".equalsIgnoreCase(r.getCode()));

        if (!isOwner && !isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thao tác trên đơn hàng này.");
            return "redirect:/orders";
        }

        try {
            Order order = orderService.fulfillOrder(orderCode, "DEMO_SIM_" + System.currentTimeMillis(), "VIETQR_TEST");
            redirectAttributes.addFlashAttribute("success", "Mô phỏng thanh toán VietQR thành công! Đã kích hoạt quyền học.");
            var dto = orderService.toResponseDTO(order);
            return "redirect:" + dto.getTargetLearnUrl();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout/payment/" + orderCode;
        }
    }
}

