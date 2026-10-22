package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String items = "[]";

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public String getFormattedTotalAmount() {
        if (totalAmount == null) {
            return "0 VNĐ";
        }
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(totalAmount) + " VNĐ";
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "";
        }
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(createdAt);
    }

    public String getStatusBadgeClass() {
        if ("SUCCESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
            return "bg-success";
        }
        if ("PENDING".equalsIgnoreCase(status)) {
            return "bg-warning text-dark";
        }
        if ("CANCELLED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
            return "bg-danger";
        }
        return "bg-secondary";
    }

    public String getStatusDisplayName() {
        if ("SUCCESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
            return "Thành công";
        }
        if ("PENDING".equalsIgnoreCase(status)) {
            return "Chờ thanh toán";
        }
        if ("CANCELLED".equalsIgnoreCase(status)) {
            return "Đã hủy";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "Thất bại";
        }
        return status != null ? status : "Không xác định";
    }
}
