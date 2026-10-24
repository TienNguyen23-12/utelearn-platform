package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.CartSummaryDTO;
import vn.edu.ute.utelearn.entity.CartItem;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;

import java.util.List;

public interface CartService {

    List<CartItem> getCartItems(User user);

    long getCartItemCount(User user);

    CartItem addToCart(User user, String itemType, Long itemId);

    void removeFromCart(User user, Long cartItemId);

    void clearCart(User user);

    CartSummaryDTO getCartSummary(User user, List<Long> selectedItemIds, String discountCode);

    Order checkoutCart(User user, List<Long> selectedItemIds, String discountCode, String paymentMethod, String note);
}
