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
import vn.edu.ute.utelearn.dto.OrderResponseDTO;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.OrderService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Slf4j
@Controller
@Transactional(readOnly = true)
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;

    @GetMapping
    public String showOrderHistory(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();

        List<Order> allOrders = orderService.getOrdersByUser(user);
        List<Order> filteredOrders = allOrders.stream()
                .filter(o -> matchesStatus(o, status))
                .filter(o -> matchesKeyword(o, keyword))
                .filter(o -> matchesDate(o, fromDate, toDate))
                .filter(o -> matchesAmount(o, minAmount, maxAmount))
                .collect(Collectors.toList());

        List<OrderResponseDTO> orderDTOs = filteredOrders.stream()
                .map(orderService::toResponseDTO)
                .collect(Collectors.toList());

        long completedCount = allOrders.stream().filter(o -> "SUCCESS".equalsIgnoreCase(o.getStatus()) || "COMPLETED".equalsIgnoreCase(o.getStatus())).count();
        long pendingCount = allOrders.stream().filter(o -> "PENDING".equalsIgnoreCase(o.getStatus())).count();
        long failedCount = allOrders.stream().filter(o -> "CANCELLED".equalsIgnoreCase(o.getStatus()) || "FAILED".equalsIgnoreCase(o.getStatus())).count();

        model.addAttribute("orders", orderDTOs);
        model.addAttribute("totalOrders", allOrders.size());
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("filteredCount", orderDTOs.size());
        model.addAttribute("currentUser", user);
        model.addAttribute("userBalance", orderService.getUserBalance(user));
        model.addAttribute("formattedUserBalance", user.getFormattedBalance());

        model.addAttribute("statusFilter", status);
        model.addAttribute("keywordFilter", keyword != null ? keyword : "");
        model.addAttribute("fromDateFilter", fromDate != null ? fromDate : "");
        model.addAttribute("toDateFilter", toDate != null ? toDate : "");
        model.addAttribute("minAmountFilter", minAmount != null ? minAmount.toString() : "");
        model.addAttribute("maxAmountFilter", maxAmount != null ? maxAmount.toString() : "");

        return "orders/index";
    }

    private boolean matchesStatus(Order order, String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return true;
        }
        String s = order.getStatus() != null ? order.getStatus().toUpperCase() : "";
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return "SUCCESS".equals(s) || "COMPLETED".equals(s);
        }
        if ("PENDING".equalsIgnoreCase(status)) {
            return "PENDING".equals(s);
        }
        if ("FAILED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            return "CANCELLED".equals(s) || "FAILED".equals(s);
        }
        return s.equalsIgnoreCase(status.trim());
    }

    private boolean matchesKeyword(Order order, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String q = keyword.trim().toLowerCase();
        boolean codeMatches = order.getOrderCode() != null && order.getOrderCode().toLowerCase().contains(q);
        boolean itemsMatches = order.getItems() != null && order.getItems().toLowerCase().contains(q);
        boolean txnMatches = order.getTransactionId() != null && order.getTransactionId().toLowerCase().contains(q);
        return codeMatches || itemsMatches || txnMatches;
    }

    private boolean matchesDate(Order order, String fromDate, String toDate) {
        if (order.getCreatedAt() == null) {
            return true;
        }
        LocalDate orderDate = order.getCreatedAt()
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toLocalDate();

        if (fromDate != null && !fromDate.isBlank()) {
            try {
                LocalDate from = LocalDate.parse(fromDate.trim());
                if (orderDate.isBefore(from)) {
                    return false;
                }
            } catch (Exception ignored) {
            }
        }

        if (toDate != null && !toDate.isBlank()) {
            try {
                LocalDate to = LocalDate.parse(toDate.trim());
                if (orderDate.isAfter(to)) {
                    return false;
                }
            } catch (Exception ignored) {
            }
        }

        return true;
    }

    private boolean matchesAmount(Order order, BigDecimal minAmount, BigDecimal maxAmount) {
        if (order.getTotalAmount() == null) {
            return true;
        }
        if (minAmount != null && order.getTotalAmount().compareTo(minAmount) < 0) {
            return false;
        }
        if (maxAmount != null && order.getTotalAmount().compareTo(maxAmount) > 0) {
            return false;
        }
        return true;
    }

    @GetMapping("/{orderCode}")
    public String showOrderDetail(@PathVariable String orderCode, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();

        Order order = orderService.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng: " + orderCode);
            return "redirect:/orders";
        }

        boolean isOwner = order.getUser() != null && order.getUser().getId().equals(user.getId());
        boolean isAdmin = SecurityUtils.isAdminOrFinance(user);
        if (!isOwner && !isAdmin) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập đơn hàng này.");
            return "redirect:/orders";
        }

        OrderResponseDTO dto = orderService.toResponseDTO(order);
        model.addAttribute("order", order);
        model.addAttribute("orderDTO", dto);
        model.addAttribute("currentUser", user);

        return "orders/detail";
    }

    @PostMapping("/{orderCode}/cancel")
    @Transactional
    public String cancelOrder(@PathVariable String orderCode, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();

        Order order = orderService.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng.");
            return "redirect:/orders";
        }

        if (order.getUser() == null || !user.getId().equals(order.getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thao tác trên đơn hàng này.");
            return "redirect:/orders";
        }

        try {
            log.info("[OrderController] Người dùng {} yêu cầu hủy đơn hàng: {}", user.getUsername(), orderCode);
            orderService.cancelOrder(order.getId(), "Người dùng tự hủy trên giao diện");
            log.info("[OrderController] Đã hủy thành công đơn hàng: {}", orderCode);
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng " + orderCode + " thành công.");
        } catch (Exception e) {
            log.error("[OrderController] Lỗi khi hủy đơn hàng {}: {}", orderCode, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/orders";
    }
}
