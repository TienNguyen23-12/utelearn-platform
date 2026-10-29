package vn.edu.ute.utelearn.controller.api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.dto.OrderResponseDTO;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.entity.Discount;
import vn.edu.ute.utelearn.service.DiscountService;
import vn.edu.ute.utelearn.service.OrderService;
import vn.edu.ute.utelearn.util.CurrencyUtils;
import vn.edu.ute.utelearn.util.SecurityUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CheckoutApiController {
    private final OrderService orderService;
    private final AuthService authService;
    private final UtelearnProperties properties;
    private final DiscountService discountService;
    @PostMapping("/checkout/initiate-cohort")
    public ResponseEntity<?> initiateCohortOrder(@RequestParam Long cohortId,
                                                 @RequestParam(required = false) String discountCode) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        try {
            User user = currentUserOpt.get();
            Order order = orderService.createCohortOrder(user, cohortId, discountCode, "VIETQR_SEPAY");
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            String qrUrl = orderService.generateVietQrUrl(order.getOrderCode(), order.getTotalAmount());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "totalAmount", order.getTotalAmount(),
                    "formattedTotalAmount", order.getFormattedTotalAmount(),
                    "qrUrl", qrUrl,
                    "bankId", properties.getPayment().getBankId(),
                    "accountNo", properties.getPayment().getAccountNo(),
                    "accountName", properties.getPayment().getAccountName(),
                    "targetLearnUrl", dto.getTargetLearnUrl()
            ));
        } catch (Exception ex) {
            log.error("[CheckoutApiController] Lỗi khởi tạo đơn cohort: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
    @PostMapping("/checkout/initiate-course")
    public ResponseEntity<?> initiateCourseOrder(@RequestParam String slug,
                                                 @RequestParam(required = false) List<Long> cohortIds,
                                                 @RequestParam(required = false) String discountCode) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        try {
            User user = currentUserOpt.get();
            Order order = orderService.createCourseOrder(user, slug, cohortIds, discountCode, "VIETQR_SEPAY");
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            String qrUrl = orderService.generateVietQrUrl(order.getOrderCode(), order.getTotalAmount());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "totalAmount", order.getTotalAmount(),
                    "formattedTotalAmount", order.getFormattedTotalAmount(),
                    "qrUrl", qrUrl,
                    "bankId", properties.getPayment().getBankId(),
                    "accountNo", properties.getPayment().getAccountNo(),
                    "accountName", properties.getPayment().getAccountName(),
                    "targetLearnUrl", dto.getTargetLearnUrl()
            ));
        } catch (Exception ex) {
            log.error("[CheckoutApiController] Lỗi khởi tạo đơn trọn gói: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
    @PostMapping("/checkout/pay-wallet-cohort")
    public ResponseEntity<?> payWalletCohort(@RequestParam Long cohortId,
                                             @RequestParam(required = false) String discountCode) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        try {
            User user = currentUserOpt.get();
            Order order = orderService.payWithWallet(user, cohortId, discountCode);
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            BigDecimal newBalance = orderService.getUserBalance(user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "message", "Thanh toán bằng Ví UTELearn thành công!",
                    "targetLearnUrl", dto.getTargetLearnUrl(),
                    "newBalance", newBalance,
                    "formattedBalance", CurrencyUtils.formatVnd(newBalance)
            ));
        } catch (Exception ex) {
            log.warn("[CheckoutApiController] Lỗi thanh toán ví cohort: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
    @PostMapping("/checkout/pay-wallet-course")
    public ResponseEntity<?> payWalletCourse(@RequestParam String slug,
                                             @RequestParam(required = false) List<Long> cohortIds,
                                             @RequestParam(required = false) String discountCode) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        try {
            User user = currentUserOpt.get();
            Order order = orderService.payCourseWithWallet(user, slug, cohortIds, discountCode);
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            BigDecimal newBalance = orderService.getUserBalance(user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "message", "Thanh toán bằng Ví UTELearn thành công!",
                    "targetLearnUrl", dto.getTargetLearnUrl(),
                    "newBalance", newBalance,
                    "formattedBalance", CurrencyUtils.formatVnd(newBalance)
            ));
        } catch (Exception ex) {
            log.warn("[CheckoutApiController] Lỗi thanh toán ví course: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
    @PostMapping(value = "/wallet/deposit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createDepositJson(@RequestBody Map<String, Object> body) {
        BigDecimal amount = null;
        if (body != null && body.get("amount") != null) {
            try {
                amount = new BigDecimal(body.get("amount").toString());
            } catch (Exception ignored) {
            }
        }
        return processDeposit(amount);
    }

    @PostMapping("/wallet/deposit")
    public ResponseEntity<?> createDeposit(@RequestParam(required = false) BigDecimal amount) {
        return processDeposit(amount);
    }

    private ResponseEntity<?> processDeposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.valueOf(10000)) < 0) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập số tiền nạp tối thiểu là 10.000 VNĐ"));
        }
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        try {
            User user = currentUserOpt.get();
            Order order = orderService.createDepositOrder(user, amount);
            String qrUrl = orderService.generateVietQrUrl(order.getOrderCode(), order.getTotalAmount());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "amount", order.getTotalAmount(),
                    "formattedAmount", order.getFormattedTotalAmount(),
                    "qrUrl", qrUrl,
                    "bankId", properties.getPayment().getBankId(),
                    "bankName", properties.getPayment().getBankId(),
                    "accountNo", properties.getPayment().getAccountNo(),
                    "accountName", properties.getPayment().getAccountName()
            ));
        } catch (Exception ex) {
            log.error("[CheckoutApiController] Lỗi nạp tiền ví: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
    @GetMapping("/discounts/validate")
    public ResponseEntity<?> validateDiscount(@RequestParam String code, @RequestParam BigDecimal amount) {
        if (code == null || code.isBlank() || amount == null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Vui lòng cung cấp mã và số tiền."));
        }
        Optional<Discount> opt = discountService.findValidDiscountByCode(code.trim(), amount);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "Mã giảm giá không hợp lệ, đã hết hạn, hết lượt dùng hoặc chưa đạt giá trị đơn tối thiểu."
            ));
        }
        Discount d = opt.get();
        BigDecimal discountVal = d.calculateDiscount(amount);
        BigDecimal finalAmount = amount.subtract(discountVal).max(BigDecimal.ZERO);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "code", d.getCode(),
                "name", d.getName(),
                "discountType", d.getDiscountType(),
                "discountAmount", discountVal,
                "formattedDiscountAmount", CurrencyUtils.formatVnd(discountVal),
                "finalAmount", finalAmount,
                "formattedFinalAmount", CurrencyUtils.formatVnd(finalAmount)
        ));
    }
    @GetMapping("/wallet/balance")
    public ResponseEntity<?> getWalletBalance() {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        User user = currentUserOpt.get();
        BigDecimal balance = orderService.getUserBalance(user);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "balance", balance,
                "formattedBalance", CurrencyUtils.formatVnd(balance)
        ));
    }
    @PostMapping("/orders/{orderCode}/simulate")
    public ResponseEntity<?> simulateOrderPayment(@PathVariable String orderCode) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        if (!properties.getPayment().isSimulationEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Chức năng mô phỏng thanh toán đã bị vô hiệu hóa."));
        }
        User currentUser = currentUserOpt.get();
        Optional<Order> orderOpt = orderService.findByOrderCode(orderCode);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Không tìm thấy đơn hàng."));
        }
        Order existingOrder = orderOpt.get();
        boolean isOwner = existingOrder.getUser() != null && existingOrder.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = SecurityUtils.isAdminOrFinance(currentUser);
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Bạn không có quyền thao tác trên đơn hàng này."));
        }
        try {
            Order order = orderService.fulfillOrder(orderCode, "DEMO_SIM_" + System.currentTimeMillis(), "VIETQR_TEST");
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            BigDecimal currentBalance = orderService.getUserBalance(currentUser);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "status", order.getStatus(),
                    "redirectUrl", dto.getTargetLearnUrl(),
                    "newBalance", currentBalance,
                    "formattedBalance", CurrencyUtils.formatVnd(currentBalance)
            ));
        } catch (Exception e) {
            log.error("[CheckoutApiController] Lỗi mô phỏng thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    @PostMapping("/checkout/offline-cohort")
    public ResponseEntity<?> initiateOfflineCohort(@RequestParam Long cohortId,
                                                   @RequestParam(required = false) String discountCode,
                                                   @RequestParam(required = false) String note) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        try {
            User user = currentUserOpt.get();
            Order order = orderService.createOfflineCohortOrder(user, cohortId, discountCode, note != null ? note : "Đặt đơn thanh toán tại quầy");
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "totalAmount", order.getTotalAmount(),
                    "formattedTotalAmount", order.getFormattedTotalAmount(),
                    "expiresAt", dto.getFormattedExpiresAt(),
                    "orderDetailUrl", "/orders/" + order.getOrderCode(),
                    "message", "Đặt đơn thành công! Hướng dẫn thanh toán đã được gửi tới email của bạn."
            ));
        } catch (Exception e) {
            log.error("[CheckoutApiController] Lỗi đặt đơn offline cohort: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    @PostMapping("/checkout/offline-course")
    public ResponseEntity<?> initiateOfflineCourse(@RequestParam String slug,
                                                   @RequestParam(required = false) List<Long> cohortIds,
                                                   @RequestParam(required = false) String discountCode,
                                                   @RequestParam(required = false) String note) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        try {
            User user = currentUserOpt.get();
            Order order = orderService.createOfflineCourseOrder(user, slug, cohortIds, discountCode, note != null ? note : "Đặt đơn thanh toán tại quầy");
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "totalAmount", order.getTotalAmount(),
                    "formattedTotalAmount", order.getFormattedTotalAmount(),
                    "expiresAt", dto.getFormattedExpiresAt(),
                    "orderDetailUrl", "/orders/" + order.getOrderCode(),
                    "message", "Đặt đơn thành công! Hướng dẫn thanh toán đã được gửi tới email của bạn."
            ));
        } catch (Exception e) {
            log.error("[CheckoutApiController] Lỗi đặt đơn offline course: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
