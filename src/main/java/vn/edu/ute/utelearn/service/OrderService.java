package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.OrderResponseDTO;
import vn.edu.ute.utelearn.dto.SepayWebhookDTO;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createCohortOrder(User user, Long cohortId, String paymentMethod);

    Order createCohortOrder(User user, Long cohortId, String discountCode, String paymentMethod);

    Order createCourseOrder(User user, String courseSlug, String paymentMethod);

    Order createCourseOrder(User user, String courseSlug, List<Long> selectedCohortIds, String paymentMethod);

    Order createCourseOrder(User user, String courseSlug, List<Long> selectedCohortIds, String discountCode, String paymentMethod);

    Order createFreeCohortOrder(User user, Long cohortId);

    Order createFreeCourseOrder(User user, String courseSlug);

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findById(Long id);

    List<Order> getOrdersByUser(User user);

    List<Order> getAllOrders();

    List<Order> getOrdersByStatus(String status);

    Order fulfillOrder(String orderCode, String transactionId, String paymentMethod);

    Order manualConfirmPayment(Long orderId, String adminUsername);

    Order cancelOrder(Long orderId, String reason);

    boolean processSepayWebhook(SepayWebhookDTO payload, String authorizationHeader);

    String generateVietQrUrl(String orderCode, BigDecimal amount);

    OrderResponseDTO toResponseDTO(Order order);

    Order createDepositOrder(User user, BigDecimal amount);

    Order payWithWallet(User user, Long cohortId);

    Order payWithWallet(User user, Long cohortId, String discountCode);

    Order payCourseWithWallet(User user, String courseSlug);

    Order payCourseWithWallet(User user, String courseSlug, List<Long> selectedCohortIds);

    Order payCourseWithWallet(User user, String courseSlug, List<Long> selectedCohortIds, String discountCode);

    BigDecimal getUserBalance(User user);

    Order createOfflineCohortOrder(User user, Long cohortId, String note);

    Order createOfflineCohortOrder(User user, Long cohortId, String discountCode, String note);

    Order createOfflineCourseOrder(User user, String courseSlug, List<Long> selectedCohortIds, String note);

    Order createOfflineCourseOrder(User user, String courseSlug, List<Long> selectedCohortIds, String discountCode, String note);

    Order createCashierOrder(String studentIdentifier, String productType, Long productId, List<Long> cohortIds, String paymentMethod, boolean paidImmediately, String note, String cashierUsername);

    Order createCashierMultiOrder(String studentIdentifier, java.util.List<Long> courseIds, java.util.List<Long> cohortIds, String paymentMethod, boolean paidImmediately, String note, String cashierUsername);

    Order createCashierMultiOrder(String studentIdentifier, java.util.List<Long> courseIds, java.util.List<Long> cohortIds, String discountCode, String paymentMethod, boolean paidImmediately, String note, String cashierUsername);

    java.util.Map<String, Object> getAvailableProductsForStudent(String studentIdentifier);

    Order cashierConfirmPayment(Long orderId, String cashierUsername, String paymentMethod, String note);

    Order createMultiItemOrder(User user, List<vn.edu.ute.utelearn.dto.OrderItemDTO> items, String discountCode, BigDecimal discountAmount, String paymentMethod, String note);

    void sendOfflinePaymentInstructionEmail(Order order);

    void sendPaymentReceiptEmail(Order order);
}
