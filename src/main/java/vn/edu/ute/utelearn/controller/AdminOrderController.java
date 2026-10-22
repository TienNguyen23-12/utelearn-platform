package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dao.OrderRepository;
import vn.edu.ute.utelearn.dto.OrderResponseDTO;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.OrderService;

import java.math.BigDecimal;
import java.util.List;
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

    @GetMapping
    public String listOrders(
            @RequestParam(required = false, defaultValue = "ALL") String status,
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

        if (keyword != null && !keyword.isBlank()) {
            String q = keyword.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(o -> (o.getOrderCode() != null && o.getOrderCode().toLowerCase().contains(q))
                            || (o.getUser() != null && o.getUser().getFullName() != null && o.getUser().getFullName().toLowerCase().contains(q))
                            || (o.getUser() != null && o.getUser().getEmail() != null && o.getUser().getEmail().toLowerCase().contains(q))
                            || (o.getTransactionId() != null && o.getTransactionId().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        List<OrderResponseDTO> orderDTOs = filtered.stream()
                .map(orderService::toResponseDTO)
                .collect(Collectors.toList());

        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        long successCount = allOrders.stream().filter(o -> "SUCCESS".equalsIgnoreCase(o.getStatus()) || "COMPLETED".equalsIgnoreCase(o.getStatus())).count();
        long pendingCount = allOrders.stream().filter(o -> "PENDING".equalsIgnoreCase(o.getStatus())).count();
        long cancelledCount = allOrders.stream().filter(o -> "CANCELLED".equalsIgnoreCase(o.getStatus())).count();

        model.addAttribute("orders", orderDTOs);
        model.addAttribute("currentStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalOrders", allOrders.size());
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("successCount", successCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("currentUser", user);

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
            redirectAttributes.addFlashAttribute("success", "Đã xác nhận thanh toán thành công cho đơn hàng " + order.getOrderCode() + "! Học viên đã được tự động kích hoạt quyền học.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
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
}
