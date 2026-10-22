package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.dto.OrderResponseDTO;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.OrderService;

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

    @PostMapping("/checkout/initiate-cohort")
    public ResponseEntity<?> initiateCohortOrder(@RequestParam Long cohortId) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }

        try {
            User user = currentUserOpt.get();
            Order order = orderService.createCohortOrder(user, cohortId, "VIETQR_SEPAY");
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
                                                 @RequestParam(required = false) List<Long> cohortIds) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }

        try {
            User user = currentUserOpt.get();
            Order order = orderService.createCourseOrder(user, slug, cohortIds, "VIETQR_SEPAY");
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
    public ResponseEntity<?> payWalletCohort(@RequestParam Long cohortId) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }

        try {
            User user = currentUserOpt.get();
            Order order = orderService.payWithWallet(user, cohortId);
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            BigDecimal newBalance = orderService.getUserBalance(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "message", "Thanh toán bằng Ví UTELearn thành công!",
                    "targetLearnUrl", dto.getTargetLearnUrl(),
                    "newBalance", newBalance,
                    "formattedBalance", String.format("%,d VNĐ", newBalance.longValue())
            ));
        } catch (Exception ex) {
            log.warn("[CheckoutApiController] Lỗi thanh toán ví cohort: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/checkout/pay-wallet-course")
    public ResponseEntity<?> payWalletCourse(@RequestParam String slug,
                                             @RequestParam(required = false) List<Long> cohortIds) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }

        try {
            User user = currentUserOpt.get();
            Order order = orderService.payCourseWithWallet(user, slug, cohortIds);
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            BigDecimal newBalance = orderService.getUserBalance(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "message", "Thanh toán bằng Ví UTELearn thành công!",
                    "targetLearnUrl", dto.getTargetLearnUrl(),
                    "newBalance", newBalance,
                    "formattedBalance", String.format("%,d VNĐ", newBalance.longValue())
            ));
        } catch (Exception ex) {
            log.warn("[CheckoutApiController] Lỗi thanh toán ví course: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping("/wallet/deposit")
    public ResponseEntity<?> createDeposit(@RequestParam BigDecimal amount) {
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
                    "accountNo", properties.getPayment().getAccountNo(),
                    "accountName", properties.getPayment().getAccountName()
            ));
        } catch (Exception ex) {
            log.error("[CheckoutApiController] Lỗi nạp tiền ví: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
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
                "formattedBalance", String.format("%,d VNĐ", balance.longValue())
        ));
    }

    @PostMapping("/orders/{orderCode}/simulate")
    public ResponseEntity<?> simulateOrderPayment(@PathVariable String orderCode) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        try {
            Order order = orderService.fulfillOrder(orderCode, "DEMO_SIM_" + System.currentTimeMillis(), "VIETQR_TEST");
            OrderResponseDTO dto = orderService.toResponseDTO(order);
            BigDecimal currentBalance = orderService.getUserBalance(currentUserOpt.get());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", order.getOrderCode(),
                    "status", order.getStatus(),
                    "redirectUrl", dto.getTargetLearnUrl(),
                    "newBalance", currentBalance,
                    "formattedBalance", String.format("%,d VNĐ", currentBalance.longValue())
            ));
        } catch (Exception e) {
            log.error("[CheckoutApiController] Lỗi mô phỏng thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
