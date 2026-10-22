package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.orderCode = :orderCode")
    Optional<Order> findByOrderCode(@Param("orderCode") String orderCode);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.user = :user ORDER BY o.createdAt DESC")
    List<Order> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user ORDER BY o.createdAt DESC")
    List<Order> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status IN ('COMPLETED', 'SUCCESS')")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status IN ('COMPLETED', 'SUCCESS') AND o.createdAt BETWEEN :start AND :end")
    BigDecimal calculateTotalRevenueBetween(@Param("start") Instant start, @Param("end") Instant end);
}

