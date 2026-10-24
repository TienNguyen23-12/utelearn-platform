package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dto.CartSummaryDTO;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CartService;
import vn.edu.ute.utelearn.service.OrderService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final AuthService authService;
    private final OrderService orderService;

    @GetMapping
    public String viewCart(
            @RequestParam(required = false) List<Long> selectedItemIds,
            @RequestParam(required = false) String discountCode,
            Model model) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();

        CartSummaryDTO summary = cartService.getCartSummary(user, selectedItemIds, discountCode);
        model.addAttribute("cart", summary);
        model.addAttribute("currentUser", user);
        model.addAttribute("activePage", "cart");
        model.addAttribute("userBalance", orderService.getUserBalance(user));
        return "cart/index";
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addToCartAjax(
            @RequestParam String itemType,
            @RequestParam Long itemId,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập để thêm vào giỏ hàng."));
        }
        User user = currentUserOpt.get();

        try {
            cartService.addToCart(user, itemType, itemId);
            long count = cartService.getCartItemCount(user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã thêm vào giỏ hàng thành công!",
                    "cartItemCount", count
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/add-redirect")
    public String addToCartAndRedirect(
            @RequestParam String itemType,
            @RequestParam Long itemId,
            @RequestParam(required = false, defaultValue = "false") boolean buyNow,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thêm vào giỏ hàng.");
            return "redirect:/login";
        }
        User user = currentUserOpt.get();

        try {
            cartService.addToCart(user, itemType, itemId);
            if (buyNow) {
                return "redirect:/cart";
            }
            redirectAttributes.addFlashAttribute("success", "Đã thêm vào giỏ hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        try {
            cartService.removeFromCart(currentUserOpt.get(), id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        cartService.clearCart(currentUserOpt.get());
        redirectAttributes.addFlashAttribute("success", "Đã xóa toàn bộ giỏ hàng.");
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(
            @RequestParam(required = false) List<Long> selectedItemIds,
            @RequestParam(required = false) String discountCode,
            @RequestParam(required = false, defaultValue = "VIETQR_SEPAY") String paymentMethod,
            @RequestParam(required = false) String note,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();

        try {
            Order order = cartService.checkoutCart(user, selectedItemIds, discountCode, paymentMethod, note);

            if ("SUCCESS".equalsIgnoreCase(order.getStatus())) {
                redirectAttributes.addFlashAttribute("success", "Thanh toán thành công! Bạn đã được ghi danh vào toàn bộ các khóa học/module đã chọn.");
                var dto = orderService.toResponseDTO(order);
                return "redirect:" + dto.getTargetLearnUrl();
            }

            if (order.isOffline()) {
                redirectAttributes.addFlashAttribute("success", "Đã tạo đơn hàng #" + order.getOrderCode() + "! Vui lòng mang mã đơn đến quầy thu ngân phòng A1-01 để thanh toán.");
                return "redirect:/orders/" + order.getOrderCode();
            }

            return "redirect:/checkout/payment/" + order.getOrderCode();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }
}
