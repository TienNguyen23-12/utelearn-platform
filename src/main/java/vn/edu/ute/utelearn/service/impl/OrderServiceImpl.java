package vn.edu.ute.utelearn.service.impl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.EnrollmentRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dao.OrderRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.OrderItemDTO;
import vn.edu.ute.utelearn.dto.OrderResponseDTO;
import vn.edu.ute.utelearn.dto.SepayWebhookDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Enrollment;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.EmailService;
import vn.edu.ute.utelearn.service.OrderService;
import vn.edu.ute.utelearn.util.CurrencyUtils;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CourseRepository courseRepository;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final UtelearnProperties properties;
    private final EmailService emailService;
    private vn.edu.ute.utelearn.service.DiscountService discountService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setDiscountService(vn.edu.ute.utelearn.service.DiscountService discountService) {
        this.discountService = discountService;
    }
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern ORDER_CODE_PATTERN = Pattern.compile("(?i)(UTENAP[0-9A-Z]{4,12}|UTE[0-9A-Z]{4,12}|ORD[0-9A-Z]{4,12})");
    private String generateUniqueOrderCode() {
        for (int i = 0; i < 10; i++) {
            int randomNum = ThreadLocalRandom.current().nextInt(100000, 999999);
            String code = "UTE" + randomNum;
            if (orderRepository.findByOrderCode(code).isEmpty()) {
                return code;
            }
        }
        return "UTE" + System.currentTimeMillis() % 100000000;
    }
    private BigDecimal applyDiscountToOrder(Order order, BigDecimal baseAmount, String discountCode) {
        if (discountCode == null || discountCode.isBlank() || discountService == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return baseAmount != null ? baseAmount : BigDecimal.ZERO;
        }
        Optional<vn.edu.ute.utelearn.entity.Discount> opt = discountService.findValidDiscountByCode(discountCode.trim(), baseAmount);
        if (opt.isEmpty()) {
            return baseAmount;
        }
        vn.edu.ute.utelearn.entity.Discount d = opt.get();
        BigDecimal discountVal = d.calculateDiscount(baseAmount);
        BigDecimal finalAmount = baseAmount.subtract(discountVal).max(BigDecimal.ZERO);
        order.setDiscountCode(d.getCode());
        order.setDiscountAmount(discountVal);
        discountService.incrementUsage(d.getCode());
        return finalAmount;
    }
    private String generateUniqueDepositCode() {
        for (int i = 0; i < 10; i++) {
            int randomNum = ThreadLocalRandom.current().nextInt(100000, 999999);
            String code = "UTENAP" + randomNum;
            if (orderRepository.findByOrderCode(code).isEmpty()) {
                return code;
            }
        }
        return "UTENAP" + (System.currentTimeMillis() % 100000000);
    }
    private List<Cohort> getOrCreateAvailableCohorts(Course course) {
        List<Cohort> cohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING", "IN_PROGRESS"));
        List<Cohort> realModules = cohorts.stream()
                .filter(c -> !(c != null && c.isFullCourseCohort()))
                .collect(Collectors.toList());
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
                .price(course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO)
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
    @Override
    @Transactional
    public Order createCohortOrder(User user, Long cohortId, String paymentMethod) {
        return createCohortOrder(user, cohortId, null, paymentMethod);
    }
    @Override
    @Transactional
    public Order createCohortOrder(User user, Long cohortId, String discountCode, String paymentMethod) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new IllegalArgumentException("Module không tồn tại"));
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
            throw new IllegalStateException("Bạn đã ghi danh module này rồi.");
        }
        if (cohort.getMaxCapacity() != null && cohort.getMaxCapacity() > 0
                && cohort.getCurrentEnrolled() != null && cohort.getCurrentEnrolled() >= cohort.getMaxCapacity()) {
            throw new IllegalStateException("Module này đã đủ số lượng học viên tối đa.");
        }
        BigDecimal price = cohort.getPrice() != null ? cohort.getPrice() : BigDecimal.ZERO;
        String status = price.compareTo(BigDecimal.ZERO) <= 0 ? "SUCCESS" : "PENDING";
        String resolvedPaymentMethod = price.compareTo(BigDecimal.ZERO) <= 0 ? "FREE"
                : (paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "VIETQR_SEPAY");
        OrderItemDTO item = OrderItemDTO.builder()
                .type("COHORT")
                .courseId(cohort.getCourse() != null ? cohort.getCourse().getId() : null)
                .courseSlug(cohort.getCourse() != null ? cohort.getCourse().getSlug() : null)
                .cohortId(cohort.getId())
                .cohortCode(cohort.getCode())
                .name(cohort.getName())
                .price(price)
                .isUpgrade(false)
                .cohortIds(List.of(cohort.getId()))
                .build();
        String itemsJson = serializeItems(List.of(item));
        Order order = Order.builder()
                .user(user)
                .orderCode(generateUniqueOrderCode())
                .totalAmount(price)
                .status(status)
                .paymentMethod(resolvedPaymentMethod)
                .items(itemsJson)
                .paidAt("SUCCESS".equals(status) ? Instant.now() : null)
                .build();
        BigDecimal finalPrice = applyDiscountToOrder(order, price, discountCode);
        order.setTotalAmount(finalPrice);
        if (finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus("SUCCESS");
            order.setPaidAt(Instant.now());
            status = "SUCCESS";
        }
        Order saved = orderRepository.save(order);
        if ("SUCCESS".equals(status)) {
            enrollCohortDirectly(user, cohort);
            sendPaymentReceiptEmail(saved);
        }
        return saved;
    }
    @Override
    @Transactional
    public Order createCourseOrder(User user, String courseSlug, String paymentMethod) {
        return createCourseOrder(user, courseSlug, null, paymentMethod);
    }
    @Override
    @Transactional
    public Order createCourseOrder(User user, String courseSlug, List<Long> selectedCohortIds, String paymentMethod) {
        return createCourseOrder(user, courseSlug, selectedCohortIds, null, paymentMethod);
    }
    @Override
    @Transactional
    public Order createCourseOrder(User user, String courseSlug, List<Long> selectedCohortIds, String discountCode, String paymentMethod) {
        Course course = courseRepository.findBySlug(courseSlug)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
        List<Cohort> availableCohorts = getOrCreateAvailableCohorts(course);
        List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        List<CohortMember> realUserMemberships = userMemberships.stream()
                .filter(cm -> !(cm.getCohort() != null && cm.getCohort().isFullCourseCohort()))
                .collect(Collectors.toList());
        Set<Long> enrolledCohortIds = realUserMemberships.stream()
                .map(cm -> cm.getCohort().getId())
                .collect(Collectors.toSet());
        List<Cohort> unenrolledCohorts = availableCohorts.stream()
                .filter(c -> !enrolledCohortIds.contains(c.getId()))
                .collect(Collectors.toList());
        if (unenrolledCohorts.isEmpty()) {
            throw new IllegalStateException("Bạn đã tham gia tất cả các module của khóa học này.");
        }
        List<Cohort> targetCohorts;
        if (selectedCohortIds != null && !selectedCohortIds.isEmpty()) {
            Set<Long> selectedSet = new HashSet<>(selectedCohortIds);
            targetCohorts = unenrolledCohorts.stream()
                    .filter(c -> selectedSet.contains(c.getId()))
                    .collect(Collectors.toList());
            if (targetCohorts.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ít nhất một module để thanh toán.");
            }
        } else {
            targetCohorts = unenrolledCohorts;
        }
        boolean isAllUnenrolledSelected = targetCohorts.size() == unenrolledCohorts.size();
        boolean isUpgrade = !realUserMemberships.isEmpty();
        BigDecimal sumOfTargetCohorts = targetCohorts.stream()
                .map(Cohort::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal coursePrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        BigDecimal totalPaidAmount = realUserMemberships.stream()
                .map(cm -> cm.getCohort() != null && cm.getCohort().getPrice() != null ? cm.getCohort().getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrice;
        if (isAllUnenrolledSelected) {
            if (isUpgrade) {
                BigDecimal diff = coursePrice.subtract(totalPaidAmount);
                totalPrice = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
                if (sumOfTargetCohorts.compareTo(BigDecimal.ZERO) > 0 && sumOfTargetCohorts.compareTo(totalPrice) < 0) {
                    totalPrice = sumOfTargetCohorts;
                }
            } else {
                totalPrice = (coursePrice.compareTo(BigDecimal.ZERO) > 0) ? coursePrice : sumOfTargetCohorts;
            }
        } else {
            totalPrice = sumOfTargetCohorts;
            if (isUpgrade) {
                BigDecimal diff = coursePrice.subtract(totalPaidAmount);
                if (diff.compareTo(BigDecimal.ZERO) > 0 && totalPrice.compareTo(diff) > 0) {
                    totalPrice = diff;
                }
            } else if (coursePrice.compareTo(BigDecimal.ZERO) > 0 && totalPrice.compareTo(coursePrice) > 0) {
                totalPrice = coursePrice;
            }
        }
        String status = totalPrice.compareTo(BigDecimal.ZERO) <= 0 ? "SUCCESS" : "PENDING";
        String resolvedPaymentMethod = totalPrice.compareTo(BigDecimal.ZERO) <= 0 ? "FREE"
                : (paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod : "VIETQR_SEPAY");
        List<Long> targetCohortIdsList = targetCohorts.stream().map(Cohort::getId).collect(Collectors.toList());
        String itemName = isAllUnenrolledSelected
                ? "Toàn bộ khóa học: " + course.getTitle()
                : (targetCohorts.size() == 1 ? targetCohorts.get(0).getName() : "Các module đã chọn (" + targetCohorts.size() + "): " + course.getTitle());
        OrderItemDTO item = OrderItemDTO.builder()
                .type("COURSE")
                .courseId(course.getId())
                .courseSlug(course.getSlug())
                .name(itemName)
                .price(totalPrice)
                .isUpgrade(isUpgrade && isAllUnenrolledSelected)
                .cohortIds(targetCohortIdsList)
                .build();
        String itemsJson = serializeItems(List.of(item));
        Order order = Order.builder()
                .user(user)
                .orderCode(generateUniqueOrderCode())
                .totalAmount(totalPrice)
                .status(status)
                .paymentMethod(resolvedPaymentMethod)
                .items(itemsJson)
                .paidAt("SUCCESS".equals(status) ? Instant.now() : null)
                .build();
        BigDecimal finalPrice = applyDiscountToOrder(order, totalPrice, discountCode);
        order.setTotalAmount(finalPrice);
        if (finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus("SUCCESS");
            order.setPaidAt(Instant.now());
            status = "SUCCESS";
        }
        Order saved = orderRepository.save(order);
        if ("SUCCESS".equals(status)) {
            enrollCourseDirectly(user, course, targetCohorts);
            sendPaymentReceiptEmail(saved);
        }
        return saved;
    }
    @Override
    @Transactional
    public Order createFreeCohortOrder(User user, Long cohortId) {
        return createCohortOrder(user, cohortId, "FREE");
    }
    @Override
    @Transactional
    public Order createFreeCourseOrder(User user, String courseSlug) {
        return createCourseOrder(user, courseSlug, "FREE");
    }
    private void enrollCohortDirectly(User user, Cohort cohort) {
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
            return;
        }
        CohortMember member = CohortMember.builder()
                .user(user)
                .cohort(cohort)
                .roleInCohort("STUDENT")
                .build();
        cohortMemberRepository.save(member);
        cohort.setCurrentEnrolled((int) cohortMemberRepository.countByCohort_Id(cohort.getId()));
        cohortRepository.save(cohort);
        if (cohort.getCourse() != null) {
            ensureCourseEnrollment(user, cohort.getCourse());
        }
    }
    private void enrollCourseDirectly(User user, Course course, List<Cohort> cohorts) {
        for (Cohort c : cohorts) {
            enrollCohortDirectly(user, c);
        }
        List<Cohort> realModules = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING", "IN_PROGRESS")).stream()
                .filter(c -> !(c != null && c.isFullCourseCohort()))
                .collect(Collectors.toList());
        boolean allRealEnrolled = !realModules.isEmpty() && realModules.stream()
                .allMatch(m -> cohortMemberRepository.existsByCohort_IdAndUser_Id(m.getId(), user.getId()));
        if (allRealEnrolled) {
            cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId()).stream()
                    .filter(c -> c != null && c.isFullCourseCohort())
                    .findFirst()
                    .ifPresent(allCohort -> {
                        if (!cohortMemberRepository.existsByCohort_IdAndUser_Id(allCohort.getId(), user.getId())) {
                            cohortMemberRepository.save(CohortMember.builder().user(user).cohort(allCohort).roleInCohort("STUDENT").build());
                            allCohort.setCurrentEnrolled((int) cohortMemberRepository.countByCohort_Id(allCohort.getId()));
                            cohortRepository.save(allCohort);
                        }
                    });
        }
        ensureCourseEnrollment(user, course);
    }
    private void ensureCourseEnrollment(User user, Course course) {
        if (!enrollmentRepository.existsByUserAndCourse(user, course)) {
            Enrollment enrollment = Enrollment.builder()
                    .user(user)
                    .course(course)
                    .progress(0.0)
                    .build();
            enrollmentRepository.save(enrollment);
        }
    }
    private String serializeItems(List<OrderItemDTO> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            log.error("[OrderService] Lỗi khi serialize order items: {}", e.getMessage());
            return "[]";
        }
    }
    private List<OrderItemDTO> deserializeItems(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank() || itemsJson.trim().equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<List<OrderItemDTO>>() {});
        } catch (Exception e) {
            log.error("[OrderService] Lỗi khi parse items JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    @Override
    public Optional<Order> findByOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            return Optional.empty();
        }
        return orderRepository.findByOrderCode(orderCode.trim());
    }
    @Override
    public Optional<Order> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return orderRepository.findById(id);
    }
    @Override
    public List<Order> getOrdersByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }
    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }
    @Override
    public List<Order> getOrdersByStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return getAllOrders();
        }
        return orderRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase().trim());
    }
    @Override
    @Transactional
    public Order fulfillOrder(String orderCode, String transactionId, String paymentMethod) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException("Mã đơn hàng không hợp lệ");
        }
        synchronized (orderCode.intern()) {
            Order order = orderRepository.findByOrderCode(orderCode.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại: " + orderCode));
            if ("SUCCESS".equalsIgnoreCase(order.getStatus()) || "COMPLETED".equalsIgnoreCase(order.getStatus())) {
                return order;
            }
            if (order.isExpired() && (paymentMethod == null || !paymentMethod.toUpperCase().startsWith("MANUAL"))) {
                order.setStatus("CANCELLED");
                order.setPaymentNote("Đơn hàng đã hết hạn hiệu lực.");
                orderRepository.save(order);
                throw new IllegalStateException("Đơn hàng đã quá hạn thanh toán.");
            }
            order.setStatus("SUCCESS");
            if (transactionId != null && !transactionId.isBlank()) {
                order.setTransactionId(transactionId);
            }
            if (paymentMethod != null && !paymentMethod.isBlank()) {
                order.setPaymentMethod(paymentMethod);
            }
            order.setPaidAt(Instant.now());
            List<OrderItemDTO> items = deserializeItems(order.getItems());
            for (OrderItemDTO item : items) {
                if ("WALLET_DEPOSIT".equalsIgnoreCase(item.getType())) {
                    User user = order.getUser();
                    if (user != null) {
                        User latestUser = userRepository.findById(user.getId()).orElse(user);
                        BigDecimal currentBalance = latestUser.getBalance() != null ? latestUser.getBalance() : BigDecimal.ZERO;
                        latestUser.setBalance(currentBalance.add(order.getTotalAmount()));
                        userRepository.save(latestUser);
                    }
                } else if ("COHORT".equalsIgnoreCase(item.getType())) {
                    if (item.getCohortId() != null) {
                        cohortRepository.findById(item.getCohortId()).ifPresent(c -> enrollCohortDirectly(order.getUser(), c));
                    }
                } else if ("COURSE".equalsIgnoreCase(item.getType())) {
                    if (item.getCohortIds() != null && !item.getCohortIds().isEmpty()) {
                        List<Cohort> cohortsToEnroll = cohortRepository.findAllById(item.getCohortIds());
                        if (item.getCourseId() != null) {
                            courseRepository.findById(item.getCourseId())
                                    .ifPresent(course -> enrollCourseDirectly(order.getUser(), course, cohortsToEnroll));
                        } else {
                            cohortsToEnroll.forEach(c -> enrollCohortDirectly(order.getUser(), c));
                        }
                    }
                }
            }
            Order saved = orderRepository.save(order);
            sendPaymentReceiptEmail(saved);
            return saved;
        }
    }
    @Override
    @Transactional
    public Order manualConfirmPayment(Long orderId, String adminUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        order.setConfirmedBy(adminUsername != null ? adminUsername : "ADMIN");
        orderRepository.save(order);
        String txId = order.isOffline() ? ("PT-" + order.getId()) : ("MANUAL_BY_" + (adminUsername != null ? adminUsername : "ADMIN"));
        Order fulfilled = fulfillOrder(order.getOrderCode(), txId, "MANUAL_ADMIN");
        return fulfilled;
    }
    @Override
    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        if ("SUCCESS".equalsIgnoreCase(order.getStatus()) || "COMPLETED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Không thể hủy đơn hàng đã hoàn tất thanh toán.");
        }
        order.setStatus("CANCELLED");
        if (reason != null && !reason.isBlank()) {
            order.setTransactionId("CANCELLED: " + reason);
        }
        return orderRepository.save(order);
    }
    @Override
    @Transactional
    public boolean processSepayWebhook(SepayWebhookDTO payload, String authorizationHeader) {
        if (payload == null) {
            return false;
        }
        String configuredKey = properties.getPayment().getSepayApiKey();
        if (configuredKey != null && !configuredKey.isBlank()) {
            if (authorizationHeader == null || !authorizationHeader.contains(configuredKey)) {
                log.warn("[OrderService] SePay webhook bị từ chối do API key không hợp lệ.");
                return false;
            }
        }
        String extractedCode = null;
        if (payload.getCode() != null && !payload.getCode().isBlank()) {
            extractedCode = payload.getCode().trim();
        }
        if (extractedCode == null && payload.getContent() != null) {
            Matcher matcher = ORDER_CODE_PATTERN.matcher(payload.getContent());
            if (matcher.find()) {
                extractedCode = matcher.group(1).toUpperCase();
            }
        }
        if (extractedCode == null && payload.getDescription() != null) {
            Matcher matcher = ORDER_CODE_PATTERN.matcher(payload.getDescription());
            if (matcher.find()) {
                extractedCode = matcher.group(1).toUpperCase();
            }
        }
        if (extractedCode == null) {
            log.warn("[OrderService] Không trích xuất được mã đơn hàng từ SePay content: {}", payload.getContent());
            return false;
        }
        Optional<Order> orderOpt = orderRepository.findByOrderCode(extractedCode);
        if (orderOpt.isEmpty()) {
            log.warn("[OrderService] Không tìm thấy đơn hàng tương ứng với mã: {}", extractedCode);
            return false;
        }
        Order order = orderOpt.get();
        if ("SUCCESS".equalsIgnoreCase(order.getStatus()) || "COMPLETED".equalsIgnoreCase(order.getStatus())) {
            return true;
        }
        if (order.isExpired()) {
            order.setStatus("CANCELLED");
            order.setPaymentNote("Chuyển khoản sau khi đơn hàng đã quá hạn 48h. Vui lòng liên hệ quầy thu ngân.");
            orderRepository.save(order);
            log.warn("[OrderService] Đơn hàng {} đã quá hạn thanh toán. Đã hủy đơn tự động.", order.getOrderCode());
            return false;
        }
        if (payload.getTransferAmount() != null && payload.getTransferAmount().compareTo(order.getTotalAmount()) < 0) {
            log.warn("[OrderService] Số tiền chuyển ({}) nhỏ hơn tổng tiền đơn hàng ({}) cho mã: {}",
                    payload.getTransferAmount(), order.getTotalAmount(), order.getOrderCode());
            return false;
        }
        String refCode = payload.getReferenceCode() != null && !payload.getReferenceCode().isBlank()
                ? payload.getReferenceCode()
                : String.valueOf(payload.getId());
        fulfillOrder(order.getOrderCode(), refCode, "VIETQR_SEPAY");
        log.info("[OrderService] SePay Webhook xử lý thành công đơn hàng {}", order.getOrderCode());
        return true;
    }
    @Override
    public String generateVietQrUrl(String orderCode, BigDecimal amount) {
        String bankId = properties.getPayment().getBankId();
        String accountNo = properties.getPayment().getAccountNo();
        String template = properties.getPayment().getTemplate();
        long roundedAmount = amount != null ? amount.longValue() : 0L;
        String encodedOrderCode = URLEncoder.encode(orderCode, StandardCharsets.UTF_8);
        return "https://qr.sepay.vn/img?acc=" + accountNo
                + "&bank=" + bankId
                + "&amount=" + roundedAmount
                + "&des=" + encodedOrderCode
                + "&template=" + template;
    }
    @Override
    public OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) {
            return null;
        }
        List<OrderItemDTO> items = deserializeItems(order.getItems());
        String primaryName = "Đơn hàng";
        String targetUrl = "/home";
        if (!items.isEmpty()) {
            OrderItemDTO first = items.get(0);
            primaryName = first.getName();
            if ("WALLET_DEPOSIT".equalsIgnoreCase(first.getType())) {
                targetUrl = "/profile";
            } else if ("COHORT".equalsIgnoreCase(first.getType()) && first.getCohortId() != null) {
                targetUrl = first.getCourseSlug() != null
                        ? "/courses/" + first.getCourseSlug() + "/modules/" + first.getCohortId() + "/learn"
                        : "/cohorts/" + first.getCohortId() + "/learn";
            } else if ("COURSE".equalsIgnoreCase(first.getType()) && first.getCourseSlug() != null) {
                targetUrl = "/courses/" + first.getCourseSlug();
            }
        }
        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .username(order.getUser() != null ? order.getUser().getUsername() : "")
                .userFullName(order.getUser() != null ? order.getUser().getFullName() : "")
                .userEmail(order.getUser() != null ? order.getUser().getEmail() : "")
                .totalAmount(order.getTotalAmount())
                .formattedTotalAmount(order.getFormattedTotalAmount())
                .discountCode(order.getDiscountCode())
                .discountAmount(order.getDiscountAmount())
                .formattedDiscountAmount(order.getFormattedDiscountAmount())
                .status(order.getStatus())
                .statusDisplayName(order.getStatusDisplayName())
                .statusBadgeClass(order.getStatusBadgeClass())
                .paymentMethod(order.getPaymentMethod())
                .paymentMethodDisplayName(order.getPaymentMethodDisplayName())
                .transactionId(order.getTransactionId())
                .confirmedBy(order.getConfirmedBy())
                .paymentNote(order.getPaymentNote())
                .createdAt(order.getFormattedCreatedAt())
                .paidAt(order.getPaidAt() != null ? order.getPaidAt().toString() : "")
                .expiresAt(order.getExpiresAt() != null ? order.getExpiresAt().toString() : "")
                .formattedExpiresAt(order.getFormattedExpiresAt())
                .offline(order.isOffline())
                .expired(order.isExpired())
                .items(items)
                .primaryItemName(primaryName)
                .targetLearnUrl(targetUrl)
                .build();
    }
    @Override
    @Transactional
    public Order createDepositOrder(User user, BigDecimal amount) {
        if (user == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ.");
        }
        if (amount == null || amount.compareTo(new BigDecimal("10000")) < 0) {
            throw new IllegalArgumentException("Số tiền nạp tối thiểu là 10.000 VNĐ.");
        }
        OrderItemDTO item = OrderItemDTO.builder()
                .type("WALLET_DEPOSIT")
                .name("Nạp tiền vào Ví UTELearn")
                .price(amount)
                .build();
        Order order = Order.builder()
                .orderCode(generateUniqueDepositCode())
                .user(user)
                .totalAmount(amount)
                .status("PENDING")
                .paymentMethod("VIETQR_SEPAY")
                .items(serializeItems(List.of(item)))
                .build();
        return orderRepository.save(order);
    }
    @Override
    @Transactional
    public Order payWithWallet(User user, Long cohortId) {
        return payWithWallet(user, cohortId, null);
    }
    @Override
    @Transactional
    public Order payWithWallet(User user, Long cohortId, String discountCode) {
        if (user == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ.");
        }
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new IllegalArgumentException("Module không tồn tại."));
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
            throw new IllegalStateException("Bạn đã ghi danh module này rồi.");
        }
        if (cohort.getMaxCapacity() != null && cohort.getMaxCapacity() > 0
                && cohort.getCurrentEnrolled() != null && cohort.getCurrentEnrolled() >= cohort.getMaxCapacity()) {
            throw new IllegalStateException("Module này đã đủ số lượng học viên tối đa.");
        }
        BigDecimal price = cohort.getPrice() != null ? cohort.getPrice() : BigDecimal.ZERO;
        User latestUser = userRepository.findById(user.getId()).orElse(user);
        BigDecimal currentBalance = latestUser.getBalance() != null ? latestUser.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(price) < 0) {
            throw new IllegalStateException("Số dư ví không đủ (" + latestUser.getFormattedBalance() + "). Vui lòng nạp thêm tiền.");
        }
        latestUser.setBalance(currentBalance.subtract(price));
        userRepository.save(latestUser);
        OrderItemDTO item = OrderItemDTO.builder()
                .type("COHORT")
                .courseId(cohort.getCourse() != null ? cohort.getCourse().getId() : null)
                .courseSlug(cohort.getCourse() != null ? cohort.getCourse().getSlug() : null)
                .cohortId(cohort.getId())
                .cohortCode(cohort.getCode())
                .name(cohort.getName())
                .price(price)
                .build();
        Order order = Order.builder()
                .orderCode(generateUniqueOrderCode())
                .user(latestUser)
                .totalAmount(price)
                .status("SUCCESS")
                .paymentMethod("WALLET")
                .transactionId("WALLET_PAY_" + System.currentTimeMillis())
                .paidAt(Instant.now())
                .items(serializeItems(List.of(item)))
                .build();
        Order saved = orderRepository.save(order);
        enrollCohortDirectly(latestUser, cohort);
        sendPaymentReceiptEmail(saved);
        return saved;
    }
    @Override
    @Transactional
    public Order payCourseWithWallet(User user, String courseSlug) {
        return payCourseWithWallet(user, courseSlug, null);
    }
    @Override
    @Transactional
    public Order payCourseWithWallet(User user, String courseSlug, List<Long> selectedCohortIds) {
        return payCourseWithWallet(user, courseSlug, selectedCohortIds, null);
    }
    @Override
    @Transactional
    public Order payCourseWithWallet(User user, String courseSlug, List<Long> selectedCohortIds, String discountCode) {
        if (user == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ.");
        }
        Course course = courseRepository.findBySlug(courseSlug)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại."));
        List<Cohort> availableCohorts = getOrCreateAvailableCohorts(course);
        List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        List<CohortMember> realUserMemberships = userMemberships.stream()
                .filter(cm -> !(cm.getCohort() != null && cm.getCohort().isFullCourseCohort()))
                .collect(Collectors.toList());
        Set<Long> enrolledCohortIds = realUserMemberships.stream()
                .map(cm -> cm.getCohort().getId())
                .collect(Collectors.toSet());
        List<Cohort> unenrolledCohorts = availableCohorts.stream()
                .filter(c -> !enrolledCohortIds.contains(c.getId()))
                .collect(Collectors.toList());
        if (unenrolledCohorts.isEmpty()) {
            throw new IllegalStateException("Bạn đã tham gia tất cả các module của khóa học này.");
        }
        List<Cohort> targetCohorts;
        if (selectedCohortIds != null && !selectedCohortIds.isEmpty()) {
            Set<Long> selectedSet = new HashSet<>(selectedCohortIds);
            targetCohorts = unenrolledCohorts.stream()
                    .filter(c -> selectedSet.contains(c.getId()))
                    .collect(Collectors.toList());
            if (targetCohorts.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ít nhất một module để thanh toán.");
            }
        } else {
            targetCohorts = unenrolledCohorts;
        }
        boolean isAllUnenrolledSelected = targetCohorts.size() == unenrolledCohorts.size();
        boolean isUpgrade = !realUserMemberships.isEmpty();
        BigDecimal sumOfTargetCohorts = targetCohorts.stream()
                .map(Cohort::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal coursePrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        BigDecimal totalPaidAmount = realUserMemberships.stream()
                .map(cm -> cm.getCohort() != null && cm.getCohort().getPrice() != null ? cm.getCohort().getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrice;
        if (isAllUnenrolledSelected) {
            if (isUpgrade) {
                BigDecimal diff = coursePrice.subtract(totalPaidAmount);
                totalPrice = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
                if (sumOfTargetCohorts.compareTo(BigDecimal.ZERO) > 0 && sumOfTargetCohorts.compareTo(totalPrice) < 0) {
                    totalPrice = sumOfTargetCohorts;
                }
            } else {
                totalPrice = (coursePrice.compareTo(BigDecimal.ZERO) > 0) ? coursePrice : sumOfTargetCohorts;
            }
        } else {
            totalPrice = sumOfTargetCohorts;
            if (isUpgrade) {
                BigDecimal diff = coursePrice.subtract(totalPaidAmount);
                if (diff.compareTo(BigDecimal.ZERO) > 0 && totalPrice.compareTo(diff) > 0) {
                    totalPrice = diff;
                }
            } else if (coursePrice.compareTo(BigDecimal.ZERO) > 0 && totalPrice.compareTo(coursePrice) > 0) {
                totalPrice = coursePrice;
            }
        }
        User latestUser = userRepository.findById(user.getId()).orElse(user);
        BigDecimal currentBalance = latestUser.getBalance() != null ? latestUser.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(totalPrice) < 0) {
            throw new IllegalStateException("Số dư ví không đủ (" + latestUser.getFormattedBalance() + "). Vui lòng nạp thêm tiền.");
        }
        latestUser.setBalance(currentBalance.subtract(totalPrice));
        userRepository.save(latestUser);
        List<Long> targetIds = targetCohorts.stream().map(Cohort::getId).collect(Collectors.toList());
        String itemName = isAllUnenrolledSelected
                ? course.getTitle()
                : (targetCohorts.size() == 1 ? targetCohorts.get(0).getName() : "Các module đã chọn (" + targetCohorts.size() + "): " + course.getTitle());
        OrderItemDTO item = OrderItemDTO.builder()
                .type("COURSE")
                .courseId(course.getId())
                .courseSlug(course.getSlug())
                .name(itemName)
                .price(totalPrice)
                .cohortIds(targetIds)
                .isUpgrade(isUpgrade && isAllUnenrolledSelected)
                .build();
        Order order = Order.builder()
                .orderCode(generateUniqueOrderCode())
                .user(latestUser)
                .totalAmount(totalPrice)
                .status("SUCCESS")
                .paymentMethod("WALLET")
                .transactionId("WALLET_PAY_" + System.currentTimeMillis())
                .paidAt(Instant.now())
                .items(serializeItems(List.of(item)))
                .build();
        Order saved = orderRepository.save(order);
        enrollCourseDirectly(latestUser, course, targetCohorts);
        sendPaymentReceiptEmail(saved);
        return saved;
    }
    @Override
    public BigDecimal getUserBalance(User user) {
        if (user == null || user.getId() == null) {
            return BigDecimal.ZERO;
        }
        return userRepository.findById(user.getId())
                .map(User::getBalance)
                .orElse(BigDecimal.ZERO);
    }
    @Override
    @Transactional
    public Order createOfflineCohortOrder(User user, Long cohortId, String note) {
        return createOfflineCohortOrder(user, cohortId, null, note);
    }
    @Override
    @Transactional
    public Order createOfflineCohortOrder(User user, Long cohortId, String discountCode, String note) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new IllegalArgumentException("Module không tồn tại"));
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
            throw new IllegalStateException("Bạn đã ghi danh module này rồi.");
        }
        if (cohort.getMaxCapacity() != null && cohort.getMaxCapacity() > 0
                && cohort.getCurrentEnrolled() != null && cohort.getCurrentEnrolled() >= cohort.getMaxCapacity()) {
            throw new IllegalStateException("Module này đã đủ số lượng học viên tối đa.");
        }
        BigDecimal price = cohort.getPrice() != null ? cohort.getPrice() : BigDecimal.ZERO;
        OrderItemDTO item = OrderItemDTO.builder()
                .type("COHORT")
                .courseId(cohort.getCourse() != null ? cohort.getCourse().getId() : null)
                .courseSlug(cohort.getCourse() != null ? cohort.getCourse().getSlug() : null)
                .cohortId(cohort.getId())
                .cohortCode(cohort.getCode())
                .name(cohort.getName())
                .price(price)
                .isUpgrade(false)
                .cohortIds(List.of(cohort.getId()))
                .build();
        Order order = Order.builder()
                .user(user)
                .orderCode(generateUniqueOrderCode())
                .totalAmount(price)
                .status("PENDING")
                .paymentMethod("OFFLINE_COUNTER")
                .paymentNote(note)
                .expiresAt(Instant.now().plus(48, java.time.temporal.ChronoUnit.HOURS))
                .items(serializeItems(List.of(item)))
                .build();
        Order saved = orderRepository.save(order);
        sendOfflinePaymentInstructionEmail(saved);
        return saved;
    }
    @Override
    @Transactional
    public Order createOfflineCourseOrder(User user, String courseSlug, List<Long> selectedCohortIds, String note) {
        return createOfflineCourseOrder(user, courseSlug, selectedCohortIds, null, note);
    }
    @Override
    @Transactional
    public Order createOfflineCourseOrder(User user, String courseSlug, List<Long> selectedCohortIds, String discountCode, String note) {
        Course course = courseRepository.findBySlug(courseSlug)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));
        List<Cohort> availableCohorts = getOrCreateAvailableCohorts(course);
        List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        List<CohortMember> realUserMemberships = userMemberships.stream()
                .filter(cm -> !(cm.getCohort() != null && cm.getCohort().isFullCourseCohort()))
                .collect(Collectors.toList());
        Set<Long> enrolledCohortIds = realUserMemberships.stream()
                .map(cm -> cm.getCohort().getId())
                .collect(Collectors.toSet());
        List<Cohort> unenrolledCohorts = availableCohorts.stream()
                .filter(c -> !enrolledCohortIds.contains(c.getId()))
                .collect(Collectors.toList());
        if (unenrolledCohorts.isEmpty()) {
            throw new IllegalStateException("Bạn đã tham gia tất cả các module của khóa học này.");
        }
        List<Cohort> targetCohorts;
        if (selectedCohortIds != null && !selectedCohortIds.isEmpty()) {
            Set<Long> selectedSet = new HashSet<>(selectedCohortIds);
            targetCohorts = unenrolledCohorts.stream()
                    .filter(c -> selectedSet.contains(c.getId()))
                    .collect(Collectors.toList());
            if (targetCohorts.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ít nhất một module để thanh toán.");
            }
        } else {
            targetCohorts = unenrolledCohorts;
        }
        boolean isAllUnenrolledSelected = targetCohorts.size() == unenrolledCohorts.size();
        boolean isUpgrade = !realUserMemberships.isEmpty();
        BigDecimal sumOfTargetCohorts = targetCohorts.stream()
                .map(Cohort::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal coursePrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        BigDecimal totalPaidAmount = realUserMemberships.stream()
                .map(cm -> cm.getCohort() != null && cm.getCohort().getPrice() != null ? cm.getCohort().getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrice;
        if (isAllUnenrolledSelected) {
            if (isUpgrade) {
                BigDecimal diff = coursePrice.subtract(totalPaidAmount);
                totalPrice = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
                if (sumOfTargetCohorts.compareTo(BigDecimal.ZERO) > 0 && sumOfTargetCohorts.compareTo(totalPrice) < 0) {
                    totalPrice = sumOfTargetCohorts;
                }
            } else {
                totalPrice = (coursePrice.compareTo(BigDecimal.ZERO) > 0) ? coursePrice : sumOfTargetCohorts;
            }
        } else {
            totalPrice = sumOfTargetCohorts;
            if (isUpgrade) {
                BigDecimal diff = coursePrice.subtract(totalPaidAmount);
                if (diff.compareTo(BigDecimal.ZERO) > 0 && totalPrice.compareTo(diff) > 0) {
                    totalPrice = diff;
                }
            } else if (coursePrice.compareTo(BigDecimal.ZERO) > 0 && totalPrice.compareTo(coursePrice) > 0) {
                totalPrice = coursePrice;
            }
        }
        List<Long> targetCohortIdsList = targetCohorts.stream().map(Cohort::getId).collect(Collectors.toList());
        String itemName = isAllUnenrolledSelected
                ? "Toàn bộ khóa học: " + course.getTitle()
                : (targetCohorts.size() == 1 ? targetCohorts.get(0).getName() : "Các module đã chọn (" + targetCohorts.size() + "): " + course.getTitle());
        OrderItemDTO item = OrderItemDTO.builder()
                .type("COURSE")
                .courseId(course.getId())
                .courseSlug(course.getSlug())
                .name(itemName)
                .price(totalPrice)
                .isUpgrade(isUpgrade && isAllUnenrolledSelected)
                .cohortIds(targetCohortIdsList)
                .build();
        Order order = Order.builder()
                .user(user)
                .orderCode(generateUniqueOrderCode())
                .totalAmount(totalPrice)
                .status("PENDING")
                .paymentMethod("OFFLINE_COUNTER")
                .paymentNote(note)
                .expiresAt(Instant.now().plus(48, java.time.temporal.ChronoUnit.HOURS))
                .items(serializeItems(List.of(item)))
                .build();
        Order saved = orderRepository.save(order);
        sendOfflinePaymentInstructionEmail(saved);
        return saved;
    }
    @Override
    @Transactional
    public Order createCashierOrder(String studentIdentifier, String productType, Long productId, List<Long> cohortIds, String paymentMethod, boolean paidImmediately, String note, String cashierUsername) {
        if (studentIdentifier == null || studentIdentifier.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp email hoặc tên đăng nhập của học viên.");
        }
        String ident = studentIdentifier.trim();
        User student = userRepository.findByEmail(ident)
                .or(() -> userRepository.findByUsername(ident))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học viên với email hoặc tài khoản: " + ident));
        String resolvedMethod = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod : "OFFLINE_CASH";
        if ("COHORT".equalsIgnoreCase(productType)) {
            Cohort cohort = cohortRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy module ID: " + productId));
            if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), student.getId())) {
                throw new IllegalStateException("Học viên này đã ghi danh vào module: " + cohort.getName());
            }
            if (cohort.getMaxCapacity() != null && cohort.getMaxCapacity() > 0
                    && cohort.getCurrentEnrolled() != null && cohort.getCurrentEnrolled() >= cohort.getMaxCapacity()) {
                throw new IllegalStateException("Module này đã đủ số lượng học viên tối đa.");
            }
            BigDecimal price = cohort.getPrice() != null ? cohort.getPrice() : BigDecimal.ZERO;
            OrderItemDTO item = OrderItemDTO.builder()
                    .type("COHORT")
                    .courseId(cohort.getCourse() != null ? cohort.getCourse().getId() : null)
                    .courseSlug(cohort.getCourse() != null ? cohort.getCourse().getSlug() : null)
                    .cohortId(cohort.getId())
                    .cohortCode(cohort.getCode())
                    .name(cohort.getName())
                    .price(price)
                    .isUpgrade(false)
                    .cohortIds(List.of(cohort.getId()))
                    .build();
            String status = paidImmediately ? "SUCCESS" : "PENDING";
            Order order = Order.builder()
                    .user(student)
                    .orderCode(generateUniqueOrderCode())
                    .totalAmount(price)
                    .status(status)
                    .paymentMethod(paidImmediately ? resolvedMethod : "OFFLINE_COUNTER")
                    .confirmedBy(cashierUsername != null ? cashierUsername : "CASHIER")
                    .paymentNote(note)
                    .paidAt(paidImmediately ? Instant.now() : null)
                    .expiresAt(Instant.now().plus(48, java.time.temporal.ChronoUnit.HOURS))
                    .items(serializeItems(List.of(item)))
                    .build();
            Order saved = orderRepository.save(order);
            if (paidImmediately) {
                saved.setTransactionId("PT-" + saved.getId());
                saved = orderRepository.save(saved);
                enrollCohortDirectly(student, cohort);
                sendPaymentReceiptEmail(saved);
            } else {
                sendOfflinePaymentInstructionEmail(saved);
            }
            return saved;
        }
        Course course = courseRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học ID: " + productId));
        List<Cohort> availableCohorts = getOrCreateAvailableCohorts(course);
        List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), student.getId());
        List<CohortMember> realUserMemberships = userMemberships.stream()
                .filter(cm -> !(cm.getCohort() != null && cm.getCohort().isFullCourseCohort()))
                .collect(Collectors.toList());
        Set<Long> enrolledCohortIds = realUserMemberships.stream()
                .map(cm -> cm.getCohort().getId())
                .collect(Collectors.toSet());
        List<Cohort> unenrolledCohorts = availableCohorts.stream()
                .filter(c -> !enrolledCohortIds.contains(c.getId()))
                .collect(Collectors.toList());
        if (unenrolledCohorts.isEmpty()) {
            throw new IllegalStateException("Học viên đã tham gia toàn bộ các module của khóa học này.");
        }
        List<Cohort> targetCohorts;
        if (cohortIds != null && !cohortIds.isEmpty()) {
            Set<Long> selectedSet = new HashSet<>(cohortIds);
            targetCohorts = unenrolledCohorts.stream()
                    .filter(c -> selectedSet.contains(c.getId()))
                    .collect(Collectors.toList());
            if (targetCohorts.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ít nhất một module để ghi danh.");
            }
        } else {
            targetCohorts = unenrolledCohorts;
        }
        boolean isAllUnenrolledSelected = targetCohorts.size() == unenrolledCohorts.size();
        boolean isUpgrade = !realUserMemberships.isEmpty();
        BigDecimal sumOfTargetCohorts = targetCohorts.stream()
                .map(Cohort::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal coursePrice = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        BigDecimal totalPaidAmount = realUserMemberships.stream()
                .map(cm -> cm.getCohort() != null && cm.getCohort().getPrice() != null ? cm.getCohort().getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPrice;
        if (isAllUnenrolledSelected) {
            if (isUpgrade) {
                BigDecimal diff = coursePrice.subtract(totalPaidAmount);
                totalPrice = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
                if (sumOfTargetCohorts.compareTo(BigDecimal.ZERO) > 0 && sumOfTargetCohorts.compareTo(totalPrice) < 0) {
                    totalPrice = sumOfTargetCohorts;
                }
            } else {
                totalPrice = (coursePrice.compareTo(BigDecimal.ZERO) > 0) ? coursePrice : sumOfTargetCohorts;
            }
        } else {
            totalPrice = sumOfTargetCohorts;
            if (isUpgrade) {
                BigDecimal diff = coursePrice.subtract(totalPaidAmount);
                if (diff.compareTo(BigDecimal.ZERO) > 0 && totalPrice.compareTo(diff) > 0) {
                    totalPrice = diff;
                }
            } else if (coursePrice.compareTo(BigDecimal.ZERO) > 0 && totalPrice.compareTo(coursePrice) > 0) {
                totalPrice = coursePrice;
            }
        }
        List<Long> targetCohortIdsList = targetCohorts.stream().map(Cohort::getId).collect(Collectors.toList());
        String itemName = isAllUnenrolledSelected
                ? "Toàn bộ khóa học: " + course.getTitle()
                : (targetCohorts.size() == 1 ? targetCohorts.get(0).getName() : "Các module đã chọn (" + targetCohorts.size() + "): " + course.getTitle());
        OrderItemDTO item = OrderItemDTO.builder()
                .type("COURSE")
                .courseId(course.getId())
                .courseSlug(course.getSlug())
                .name(itemName)
                .price(totalPrice)
                .isUpgrade(isUpgrade && isAllUnenrolledSelected)
                .cohortIds(targetCohortIdsList)
                .build();
        String status = paidImmediately ? "SUCCESS" : "PENDING";
        Order order = Order.builder()
                .user(student)
                .orderCode(generateUniqueOrderCode())
                .totalAmount(totalPrice)
                .status(status)
                .paymentMethod(paidImmediately ? resolvedMethod : "OFFLINE_COUNTER")
                .confirmedBy(cashierUsername != null ? cashierUsername : "CASHIER")
                .paymentNote(note)
                .paidAt(paidImmediately ? Instant.now() : null)
                .expiresAt(Instant.now().plus(48, java.time.temporal.ChronoUnit.HOURS))
                .items(serializeItems(List.of(item)))
                .build();
        Order saved = orderRepository.save(order);
        if (paidImmediately) {
            saved.setTransactionId("PT-" + saved.getId());
            saved = orderRepository.save(saved);
            enrollCourseDirectly(student, course, targetCohorts);
            sendPaymentReceiptEmail(saved);
        } else {
            sendOfflinePaymentInstructionEmail(saved);
        }
        return saved;
    }
    @Override
    public Map<String, Object> getAvailableProductsForStudent(String studentIdentifier) {
        if (studentIdentifier == null || studentIdentifier.isBlank()) {
            return Collections.emptyMap();
        }
        String ident = studentIdentifier.trim();
        Optional<User> studentOpt = userRepository.findByEmail(ident)
                .or(() -> userRepository.findByUsername(ident));
        if (studentOpt.isEmpty()) {
            return Collections.emptyMap();
        }
        User student = studentOpt.get();
        Set<Long> enrolledCohortIds = cohortMemberRepository.findByUserOrderByEnrolledAtDesc(student).stream()
                .map(cm -> cm.getCohort() != null ? cm.getCohort().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Order> userOrders = orderRepository.findByUserOrderByCreatedAtDesc(student);
        Set<Long> pendingCourseIds = new HashSet<>();
        Set<Long> pendingCohortIds = new HashSet<>();
        for (Order o : userOrders) {
            if ("PENDING".equalsIgnoreCase(o.getStatus()) && !o.isExpired()) {
                List<OrderItemDTO> items = deserializeItems(o.getItems());
                for (OrderItemDTO item : items) {
                    if ("COURSE".equalsIgnoreCase(item.getType())) {
                        if (item.getCourseId() != null) {
                            pendingCourseIds.add(item.getCourseId());
                        }
                        if (item.getCohortIds() != null) {
                            pendingCohortIds.addAll(item.getCohortIds());
                        }
                    } else if ("COHORT".equalsIgnoreCase(item.getType())) {
                        if (item.getCohortId() != null) {
                            pendingCohortIds.add(item.getCohortId());
                        }
                    }
                }
            }
        }
        List<Course> allCourses = courseRepository.findAll();
        List<Map<String, Object>> availableCourses = new ArrayList<>();
        for (Course course : allCourses) {
            if (pendingCourseIds.contains(course.getId())) {
                continue;
            }
            List<Cohort> cohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
            boolean allEnrolled = !cohorts.isEmpty() && cohorts.stream().allMatch(c -> enrolledCohortIds.contains(c.getId()));
            if (allEnrolled) {
                continue;
            }
            BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Map<String, Object> cMap = new HashMap<>();
            cMap.put("id", course.getId());
            cMap.put("title", course.getTitle());
            cMap.put("slug", course.getSlug());
            cMap.put("price", price);
            cMap.put("formattedPrice", CurrencyUtils.formatVnd(price));
            cMap.put("type", "COURSE");
            availableCourses.add(cMap);
        }
        List<Cohort> allCohorts = cohortRepository.findAll();
        List<Map<String, Object>> availableCohorts = new ArrayList<>();
        for (Cohort cohort : allCohorts) {
            if (cohort.isFullCourseCohort()) {
                continue;
            }
            if (enrolledCohortIds.contains(cohort.getId()) || pendingCohortIds.contains(cohort.getId())) {
                continue;
            }
            if (cohort.getMaxCapacity() != null && cohort.getMaxCapacity() > 0
                    && cohort.getCurrentEnrolled() != null && cohort.getCurrentEnrolled() >= cohort.getMaxCapacity()) {
                continue;
            }
            BigDecimal price = cohort.getPrice() != null ? cohort.getPrice() : BigDecimal.ZERO;
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Map<String, Object> mMap = new HashMap<>();
            mMap.put("id", cohort.getId());
            mMap.put("title", cohort.getName());
            mMap.put("code", cohort.getCode() != null ? cohort.getCode() : "");
            mMap.put("price", price);
            mMap.put("formattedPrice", CurrencyUtils.formatVnd(price));
            mMap.put("type", "COHORT");
            mMap.put("courseTitle", cohort.getCourse() != null ? cohort.getCourse().getTitle() : "");
            availableCohorts.add(mMap);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("student", Map.of(
                "id", student.getId(),
                "username", student.getUsername(),
                "fullName", student.getFullName() != null ? student.getFullName() : student.getUsername(),
                "email", student.getEmail()
        ));
        result.put("availableCourses", availableCourses);
        result.put("availableCohorts", availableCohorts);
        result.put("totalAvailable", availableCourses.size() + availableCohorts.size());
        return result;
    }
    @Override
    @Transactional
    public Order createCashierMultiOrder(String studentIdentifier, List<Long> courseIds, List<Long> cohortIds, String paymentMethod, boolean paidImmediately, String note, String cashierUsername) {
        return createCashierMultiOrder(studentIdentifier, courseIds, cohortIds, null, paymentMethod, paidImmediately, note, cashierUsername);
    }
    @Override
    @Transactional
    public Order createCashierMultiOrder(String studentIdentifier, List<Long> courseIds, List<Long> cohortIds, String discountCode, String paymentMethod, boolean paidImmediately, String note, String cashierUsername) {
        if (studentIdentifier == null || studentIdentifier.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp email hoặc tên đăng nhập của học viên.");
        }
        String ident = studentIdentifier.trim();
        User student = userRepository.findByEmail(ident)
                .or(() -> userRepository.findByUsername(ident))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học viên với email hoặc tài khoản: " + ident));
        boolean hasCourses = courseIds != null && !courseIds.isEmpty();
        boolean hasCohorts = cohortIds != null && !cohortIds.isEmpty();
        if (!hasCourses && !hasCohorts) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một khóa học hoặc module để tạo đơn.");
        }
        List<OrderItemDTO> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (hasCourses) {
            for (Long cId : courseIds) {
                Course course = courseRepository.findById(cId)
                        .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại ID: " + cId));
                List<Cohort> cohorts = getOrCreateAvailableCohorts(course);
                boolean allEnrolled = !cohorts.isEmpty() && cohorts.stream()
                        .allMatch(c -> cohortMemberRepository.existsByCohort_IdAndUser_Id(c.getId(), student.getId()));
                if (allEnrolled) {
                    throw new IllegalStateException("Học viên đã sở hữu toàn bộ khóa học: " + course.getTitle());
                }
                BigDecimal price = course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
                totalAmount = totalAmount.add(price);
                List<Long> cohortIdList = cohorts.stream().map(Cohort::getId).collect(Collectors.toList());
                items.add(OrderItemDTO.builder()
                        .type("COURSE")
                        .courseId(course.getId())
                        .courseSlug(course.getSlug())
                        .name("Khóa học: " + course.getTitle())
                        .price(price)
                        .isUpgrade(false)
                        .cohortIds(cohortIdList)
                        .build());
            }
        }
        if (hasCohorts) {
            for (Long mId : cohortIds) {
                Cohort cohort = cohortRepository.findById(mId)
                        .orElseThrow(() -> new IllegalArgumentException("Module không tồn tại ID: " + mId));
                if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), student.getId())) {
                    throw new IllegalStateException("Học viên đã ghi danh vào module: " + cohort.getName());
                }
                if (cohort.getMaxCapacity() != null && cohort.getMaxCapacity() > 0
                        && cohort.getCurrentEnrolled() != null && cohort.getCurrentEnrolled() >= cohort.getMaxCapacity()) {
                    throw new IllegalStateException("Module '" + cohort.getName() + "' đã đủ số lượng học viên tối đa.");
                }
                BigDecimal price = cohort.getPrice() != null ? cohort.getPrice() : BigDecimal.ZERO;
                totalAmount = totalAmount.add(price);
                items.add(OrderItemDTO.builder()
                        .type("COHORT")
                        .courseId(cohort.getCourse() != null ? cohort.getCourse().getId() : null)
                        .courseSlug(cohort.getCourse() != null ? cohort.getCourse().getSlug() : null)
                        .cohortId(cohort.getId())
                        .cohortCode(cohort.getCode())
                        .name("Module: " + cohort.getName())
                        .price(price)
                        .isUpgrade(false)
                        .cohortIds(List.of(cohort.getId()))
                        .build());
            }
        }
        String resolvedMethod = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod.trim().toUpperCase() : "OFFLINE_CASH";
        String status = paidImmediately ? "SUCCESS" : "PENDING";
        Instant now = Instant.now();
        Order order = Order.builder()
                .user(student)
                .orderCode(generateUniqueOrderCode())
                .totalAmount(totalAmount)
                .status(status)
                .paymentMethod(paidImmediately ? resolvedMethod : "OFFLINE_COUNTER")
                .confirmedBy(cashierUsername != null ? cashierUsername : "CASHIER")
                .paymentNote(note)
                .paidAt(paidImmediately ? now : null)
                .expiresAt(paidImmediately ? null : now.plus(48, java.time.temporal.ChronoUnit.HOURS))
                .items(serializeItems(items))
                .build();
        BigDecimal finalAmount = applyDiscountToOrder(order, totalAmount, discountCode);
        order.setTotalAmount(finalAmount);
        Order saved = orderRepository.save(order);
        if (paidImmediately) {
            saved.setTransactionId("PT-" + saved.getId());
            saved = orderRepository.save(saved);
            for (OrderItemDTO item : items) {
                if ("COHORT".equalsIgnoreCase(item.getType()) && item.getCohortId() != null) {
                    cohortRepository.findById(item.getCohortId()).ifPresent(c -> enrollCohortDirectly(student, c));
                } else if ("COURSE".equalsIgnoreCase(item.getType())) {
                    if (item.getCohortIds() != null && !item.getCohortIds().isEmpty()) {
                        List<Cohort> cohortsToEnroll = cohortRepository.findAllById(item.getCohortIds());
                        if (item.getCourseId() != null) {
                            courseRepository.findById(item.getCourseId())
                                    .ifPresent(course -> enrollCourseDirectly(student, course, cohortsToEnroll));
                        } else {
                            cohortsToEnroll.forEach(c -> enrollCohortDirectly(student, c));
                        }
                    }
                }
            }
            sendPaymentReceiptEmail(saved);
        } else {
            sendOfflinePaymentInstructionEmail(saved);
        }
        return saved;
    }
    @Override
    @Transactional
    public Order cashierConfirmPayment(Long orderId, String cashierUsername, String paymentMethod, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        if ("SUCCESS".equalsIgnoreCase(order.getStatus()) || "COMPLETED".equalsIgnoreCase(order.getStatus())) {
            return order;
        }
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng này đã bị hủy, không thể xác nhận thanh toán.");
        }
        String resolvedMethod = (paymentMethod != null && !paymentMethod.isBlank()) ? paymentMethod : "OFFLINE_CASH";
        String autoReceiptNumber = "PT-" + order.getId();
        order.setStatus("SUCCESS");
        order.setPaidAt(Instant.now());
        order.setConfirmedBy(cashierUsername != null ? cashierUsername : "CASHIER");
        order.setPaymentMethod(resolvedMethod);
        order.setTransactionId(autoReceiptNumber);
        if (note != null && !note.isBlank()) {
            order.setPaymentNote(note);
        }
        List<OrderItemDTO> items = deserializeItems(order.getItems());
        for (OrderItemDTO item : items) {
            if ("WALLET_DEPOSIT".equalsIgnoreCase(item.getType())) {
                User user = order.getUser();
                if (user != null) {
                    User latestUser = userRepository.findById(user.getId()).orElse(user);
                    BigDecimal currentBalance = latestUser.getBalance() != null ? latestUser.getBalance() : BigDecimal.ZERO;
                    latestUser.setBalance(currentBalance.add(order.getTotalAmount()));
                    userRepository.save(latestUser);
                }
            } else if ("COHORT".equalsIgnoreCase(item.getType())) {
                if (item.getCohortId() != null) {
                    cohortRepository.findById(item.getCohortId()).ifPresent(c -> enrollCohortDirectly(order.getUser(), c));
                }
            } else if ("COURSE".equalsIgnoreCase(item.getType())) {
                if (item.getCohortIds() != null && !item.getCohortIds().isEmpty()) {
                    List<Cohort> cohortsToEnroll = cohortRepository.findAllById(item.getCohortIds());
                    if (item.getCourseId() != null) {
                        courseRepository.findById(item.getCourseId())
                                .ifPresent(course -> enrollCourseDirectly(order.getUser(), course, cohortsToEnroll));
                    } else {
                        cohortsToEnroll.forEach(c -> enrollCohortDirectly(order.getUser(), c));
                    }
                }
            }
        }
        Order saved = orderRepository.save(order);
        sendPaymentReceiptEmail(saved);
        return saved;
    }
    @Override
    @Transactional
    public Order createMultiItemOrder(User user, List<OrderItemDTO> items, String discountCode, BigDecimal discountAmount, String paymentMethod, String note) {
        if (user == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải chứa ít nhất một sản phẩm.");
        }
        BigDecimal originalTotal = items.stream()
                .map(OrderItemDTO::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0)
                ? discountAmount
                : BigDecimal.ZERO;
        if (discount.compareTo(originalTotal) > 0) {
            discount = originalTotal;
        }
        BigDecimal finalTotal = originalTotal.subtract(discount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }
        String method = (paymentMethod != null && !paymentMethod.isBlank())
                ? paymentMethod.trim().toUpperCase()
                : "VIETQR_SEPAY";
        boolean isFree = finalTotal.compareTo(BigDecimal.ZERO) <= 0;
        if (isFree) {
            method = "FREE";
        }
        String status = isFree ? "SUCCESS" : "PENDING";
        Instant now = Instant.now();
        Instant paidAt = isFree ? now : null;
        Instant expiresAt = isFree ? null : now.plus(48, java.time.temporal.ChronoUnit.HOURS);
        String txId = null;
        User latestUser = userRepository.findById(user.getId()).orElse(user);
        if (!isFree && "WALLET".equalsIgnoreCase(method)) {
            BigDecimal currentBalance = latestUser.getBalance() != null ? latestUser.getBalance() : BigDecimal.ZERO;
            if (currentBalance.compareTo(finalTotal) < 0) {
                throw new IllegalStateException("Số dư ví không đủ (" + latestUser.getFormattedBalance() + "). Vui lòng nạp thêm tiền.");
            }
            latestUser.setBalance(currentBalance.subtract(finalTotal));
            userRepository.save(latestUser);
            status = "SUCCESS";
            paidAt = now;
            txId = "WALLET_PAY_" + System.currentTimeMillis();
            expiresAt = null;
        }
        Order order = Order.builder()
                .user(latestUser)
                .orderCode(generateUniqueOrderCode())
                .totalAmount(finalTotal)
                .discountCode(discountCode != null && !discountCode.isBlank() ? discountCode.trim().toUpperCase() : null)
                .discountAmount(discount)
                .status(status)
                .paymentMethod(method)
                .transactionId(txId)
                .paymentNote(note)
                .paidAt(paidAt)
                .expiresAt(expiresAt)
                .items(serializeItems(items))
                .build();
        Order saved = orderRepository.save(order);
        if ("SUCCESS".equalsIgnoreCase(status)) {
            for (OrderItemDTO item : items) {
                if ("COHORT".equalsIgnoreCase(item.getType()) && item.getCohortId() != null) {
                    cohortRepository.findById(item.getCohortId()).ifPresent(c -> enrollCohortDirectly(latestUser, c));
                } else if ("COURSE".equalsIgnoreCase(item.getType())) {
                    if (item.getCohortIds() != null && !item.getCohortIds().isEmpty()) {
                        List<Cohort> cohortsToEnroll = cohortRepository.findAllById(item.getCohortIds());
                        if (item.getCourseId() != null) {
                            courseRepository.findById(item.getCourseId())
                                    .ifPresent(course -> enrollCourseDirectly(latestUser, course, cohortsToEnroll));
                        } else {
                            cohortsToEnroll.forEach(c -> enrollCohortDirectly(latestUser, c));
                        }
                    }
                }
            }
            sendPaymentReceiptEmail(saved);
        } else if (saved.isOffline()) {
            sendOfflinePaymentInstructionEmail(saved);
        }
        return saved;
    }
    @Override
    public void sendOfflinePaymentInstructionEmail(Order order) {
        if (emailService != null) {
            emailService.sendOfflinePaymentInstructionEmail(order);
        }
    }
    @Override
    public void sendPaymentReceiptEmail(Order order) {
        if (emailService != null) {
            emailService.sendPaymentReceiptEmail(order);
        }
    }
}