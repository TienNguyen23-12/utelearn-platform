package vn.edu.ute.utelearn.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.OrderRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.OrderResponseDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.OrderService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Controller
@Transactional(readOnly = true)
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'FINANCE', 'ROLE_FINANCE')")
public class AdminOrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CohortRepository cohortRepository;

    @GetMapping
    public String listOrders(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "ALL") String method,
            @RequestParam(required = false) String keyword,
            Model model) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();

        List<Order> allOrders = orderService.getAllOrders();

        List<Order> filtered = allOrders;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            filtered = filtered.stream()
                    .filter(o -> status.equalsIgnoreCase(o.getStatus()))
                    .collect(Collectors.toList());
        }

        if (method != null && !method.isBlank() && !"ALL".equalsIgnoreCase(method)) {
            if ("OFFLINE".equalsIgnoreCase(method)) {
                filtered = filtered.stream()
                        .filter(Order::isOffline)
                        .collect(Collectors.toList());
            } else if ("ONLINE".equalsIgnoreCase(method)) {
                filtered = filtered.stream()
                        .filter(o -> o.getPaymentMethod() != null && o.getPaymentMethod().toUpperCase().contains("VIETQR"))
                        .collect(Collectors.toList());
            } else if ("WALLET".equalsIgnoreCase(method)) {
                filtered = filtered.stream()
                        .filter(o -> o.getPaymentMethod() != null && o.getPaymentMethod().toUpperCase().contains("WALLET"))
                        .collect(Collectors.toList());
            }
        }

        if (keyword != null && !keyword.isBlank()) {
            String q = keyword.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(o -> (o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(q))
                            || (o.getUser() != null && o.getUser().getFullName() != null && o.getUser().getFullName().toLowerCase().contains(q))
                            || (o.getUser() != null && o.getUser().getEmail() != null && o.getUser().getEmail().toLowerCase().contains(q))
                            || (o.getTransactionId() != null && o.getTransactionId().toLowerCase().contains(q))
                            || (o.getConfirmedBy() != null && o.getConfirmedBy().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        List<OrderResponseDTO> orderDTOs = filtered.stream()
                .map(orderService::toResponseDTO)
                .collect(Collectors.toList());

        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        BigDecimal offlineRevenue = orderRepository.calculateOfflineRevenue();
        if (offlineRevenue == null) {
            offlineRevenue = BigDecimal.ZERO;
        }

        BigDecimal onlineRevenue = orderRepository.calculateOnlineRevenue();
        if (onlineRevenue == null) {
            onlineRevenue = BigDecimal.ZERO;
        }

        long successCount = allOrders.stream().filter(o -> "SUCCESS".equalsIgnoreCase(o.getStatus()) || "COMPLETED".equalsIgnoreCase(o.getStatus())).count();
        long pendingCount = allOrders.stream().filter(o -> "PENDING".equalsIgnoreCase(o.getStatus())).count();
        long cancelledCount = allOrders.stream().filter(o -> "CANCELLED".equalsIgnoreCase(o.getStatus())).count();
        long pendingOfflineCount = orderRepository.countPendingOfflineOrders();

        List<Course> availableCourses = courseRepository.findAll();
        List<Cohort> availableCohorts = cohortRepository.findAll();

        model.addAttribute("orders", orderDTOs);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentMethod", method);
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalOrders", allOrders.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("offlineRevenue", offlineRevenue);
        model.addAttribute("onlineRevenue", onlineRevenue);
        model.addAttribute("successCount", successCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("pendingOfflineCount", pendingOfflineCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("currentUser", user);
        model.addAttribute("courses", availableCourses);
        model.addAttribute("cohorts", availableCohorts);

        return "admin/orders/index";
    }

    @PostMapping("/{id}/confirm")
    @Transactional
    public String confirmPayment(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        String username = currentUserOpt.map(User::getUsername).orElse("ADMIN");

        try {
            Order order = orderService.manualConfirmPayment(id, username);
            redirectAttributes.addFlashAttribute("success", "Đã xác nhận thanh toán thành công cho đơn hàng " + order.getOrderCode() + "! Học viên đã được tự động kích hoạt quyền học và nhận email biên nhận.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/confirm-cashier")
    @Transactional
    public String confirmCashierPayment(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "OFFLINE_CASH") String cashierPaymentMethod,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        String cashierUsername = currentUserOpt.map(User::getUsername).orElse("CASHIER");

        try {
            Order order = orderService.cashierConfirmPayment(id, cashierUsername, cashierPaymentMethod, note);
            redirectAttributes.addFlashAttribute("success", "Thu tiền tại quầy thành công cho đơn " + order.getOrderCode() + "! Mã phiếu thu: " + order.getTransactionId() + ". Hệ thống đã gửi Email Biên nhận thanh toán điện tử cho học viên.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xác nhận thu ngân: " + e.getMessage());
        }

        return "redirect:/admin/orders";
    }

    @PostMapping("/create-offline")
    @Transactional
    public String createOfflineOrder(
            @RequestParam String studentIdentifier,
            @RequestParam String productType,
            @RequestParam Long productId,
            @RequestParam(required = false) List<Long> cohortIds,
            @RequestParam(required = false, defaultValue = "OFFLINE_CASH") String paymentMethod,
            @RequestParam(required = false, defaultValue = "false") boolean paidImmediately,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        String cashierUsername = currentUserOpt.map(User::getUsername).orElse("CASHIER");

        try {
            Order order = orderService.createCashierOrder(
                    studentIdentifier,
                    productType,
                    productId,
                    cohortIds,
                    paymentMethod,
                    paidImmediately,
                    note,
                    cashierUsername
            );

            if (paidImmediately) {
                redirectAttributes.addFlashAttribute("success", "Đã tạo đơn và thu tiền thành công cho đơn " + order.getOrderCode() + "! Mã phiếu thu: " + order.getTransactionId() + ". Học viên đã được kích hoạt khóa học và nhận email biên nhận.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Đã tạo đơn chờ thanh toán offline " + order.getOrderCode() + "! Hệ thống đã gửi email hướng dẫn nộp tiền cho học viên.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo đơn tại quầy: " + e.getMessage());
        }

        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public String cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Quản trị viên hủy đơn") String reason,
            RedirectAttributes redirectAttributes) {

        try {
            Order order = orderService.cancelOrder(id, reason);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng " + order.getOrderCode() + " thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/orders";
    }

    @GetMapping("/api/search-users")
    @ResponseBody
    public ResponseEntity<?> searchUsers(@RequestParam(required = false) String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<User> users = userRepository.searchUsers(query.trim(), PageRequest.of(0, 10));
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("fullName", u.getFullName() != null ? u.getFullName() : u.getUsername());
            map.put("email", u.getEmail());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/product-info")
    @ResponseBody
    public ResponseEntity<?> getProductInfo(
            @RequestParam String type,
            @RequestParam Long id) {
        if ("COHORT".equalsIgnoreCase(type)) {
            Optional<Cohort> opt = cohortRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Cohort c = opt.get();
            BigDecimal price = c.getPrice() != null ? c.getPrice() : BigDecimal.ZERO;
            return ResponseEntity.ok(Map.of(
                    "id", c.getId(),
                    "name", c.getName(),
                    "code", c.getCode() != null ? c.getCode() : "",
                    "price", price,
                    "formattedPrice", String.format("%,d VNĐ", price.longValue())
            ));
        }

        Optional<Course> opt = courseRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Course c = opt.get();
        BigDecimal price = c.getPrice() != null ? c.getPrice() : BigDecimal.ZERO;
        return ResponseEntity.ok(Map.of(
                "id", c.getId(),
                "name", c.getTitle(),
                "slug", c.getSlug(),
                "price", price,
                "formattedPrice", String.format("%,d VNĐ", price.longValue())
        ));
    }
}
