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

    Order createCourseOrder(User user, String courseSlug, String paymentMethod);

    Order createCourseOrder(User user, String courseSlug, List<Long> selectedCohortIds, String paymentMethod);

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

    Order payCourseWithWallet(User user, String courseSlug);

    Order payCourseWithWallet(User user, String courseSlug, List<Long> selectedCohortIds);

    BigDecimal getUserBalance(User user);
}
