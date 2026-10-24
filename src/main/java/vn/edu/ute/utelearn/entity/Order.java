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

    @Column(name = "discount_code", length = 50)
    private String discountCode;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

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

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "payment_note", length = 500)
    private String paymentNote;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public boolean isOffline() {
        return paymentMethod != null && paymentMethod.toUpperCase().contains("OFFLINE");
    }

    public boolean isExpired() {
        return "PENDING".equalsIgnoreCase(status) && expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public String getFormattedTotalAmount() {
        if (totalAmount == null) {
            return "0 VNĐ";
        }
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(totalAmount) + " VNĐ";
    }

    public String getFormattedDiscountAmount() {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "0 VNĐ";
        }
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(discountAmount) + " VNĐ";
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "";
        }
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(createdAt);
    }

    public String getFormattedExpiresAt() {
        if (expiresAt == null) {
            return "";
        }
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(expiresAt);
    }

    public String getStatusBadgeClass() {
        if (isExpired()) {
            return "bg-secondary";
        }
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
        if (isExpired()) {
            return "Đã quá hạn";
        }
        if ("SUCCESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
            return "Thành công";
        }
        if ("PENDING".equalsIgnoreCase(status)) {
            if (isOffline()) {
                return "Chờ thu tiền tại quầy";
            }
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

    public String getPaymentMethodDisplayName() {
        if (paymentMethod == null) {
            return "Chưa xác định";
        }
        switch (paymentMethod.toUpperCase()) {
            case "OFFLINE_COUNTER":
                return "Tại quầy thu ngân (Chờ thanh toán)";
            case "OFFLINE_CASH":
                return "Tiền mặt tại quầy";
            case "OFFLINE_BANK_TRANSFER":
                return "Chuyển khoản tại quầy";
            case "VIETQR_SEPAY":
            case "VIETQR":
                return "Chuyển khoản VietQR 24/7";
            case "WALLET":
                return "Ví điện tử UTELearn";
            case "FREE":
                return "Miễn phí";
            case "DEMO_SIMULATION":
            case "VIETQR_TEST":
                return "Mô phỏng thử nghiệm";
            default:
                return paymentMethod;
        }
    }
}
