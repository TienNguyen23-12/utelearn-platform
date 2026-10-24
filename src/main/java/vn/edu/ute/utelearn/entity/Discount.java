package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "discount_type", nullable = false, length = 20)
    @Builder.Default
    private String discountType = "PERCENTAGE";

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public boolean isValid(BigDecimal orderAmount) {
        if (!active) {
            return false;
        }
        Instant now = Instant.now();
        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }
        if (usageLimit != null && usedCount != null && usedCount >= usageLimit) {
            return false;
        }
        if (orderAmount != null && minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) {
            return false;
        }
        return true;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0 || !isValid(orderAmount)) {
            return BigDecimal.ZERO;
        }

        BigDecimal calculated;
        if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
            calculated = orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0 && calculated.compareTo(maxDiscountAmount) > 0) {
                calculated = maxDiscountAmount;
            }
        } else {
            calculated = discountValue != null ? discountValue : BigDecimal.ZERO;
        }

        if (calculated.compareTo(orderAmount) > 0) {
            calculated = orderAmount;
        }
        return calculated.compareTo(BigDecimal.ZERO) > 0 ? calculated : BigDecimal.ZERO;
    }

    public String getFormattedDiscountValue() {
        if (discountValue == null) {
            return "0";
        }
        if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
            return discountValue.stripTrailingZeros().toPlainString() + "%";
        }
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(discountValue) + " VNĐ";
    }

    public String getFormattedMinOrderAmount() {
        if (minOrderAmount == null || minOrderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Không giới hạn";
        }
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(minOrderAmount) + " VNĐ";
    }

    public String getFormattedMaxDiscountAmount() {
        if (maxDiscountAmount == null || maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Không giới hạn";
        }
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(maxDiscountAmount) + " VNĐ";
    }

    public String getFormattedStartDate() {
        if (startDate == null) {
            return "Không giới hạn";
        }
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(startDate);
    }

    public String getFormattedEndDate() {
        if (endDate == null) {
            return "Không thời hạn";
        }
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(endDate);
    }

    public boolean isExpired() {
        return endDate != null && Instant.now().isAfter(endDate);
    }
}
