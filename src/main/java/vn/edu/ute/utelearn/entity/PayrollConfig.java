package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payroll_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Builder.Default
    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = false;

    @Column(name = "instructor_share_percent", precision = 5, scale = 2)
    private BigDecimal instructorSharePercent;

    @Column(name = "instructor_rating_bonus_percent", precision = 5, scale = 2)
    private BigDecimal instructorRatingBonusPercent;

    @Column(name = "instructor_rating_threshold", precision = 4, scale = 2)
    private BigDecimal instructorRatingThreshold;

    @Column(name = "moderator_minute_rate", precision = 12, scale = 2)
    private BigDecimal moderatorMinuteRate;

    @Column(name = "moderator_ot_multiplier", precision = 5, scale = 2)
    private BigDecimal moderatorOtMultiplier;

    @Column(name = "moderator_task_bonus", precision = 12, scale = 2)
    private BigDecimal moderatorTaskBonus;

    @Column(name = "moderator_sla_penalty", precision = 12, scale = 2)
    private BigDecimal moderatorSlaPenalty;

    @Builder.Default
    @Column(name = "fixed_allowance", precision = 12, scale = 2)
    private BigDecimal fixedAllowance = BigDecimal.ZERO;

    @Column(name = "note", length = 500)
    private String note;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public static PayrollConfig createDefaultGlobalConfig() {
        return PayrollConfig.builder()
                .isGlobal(true)
                .instructorSharePercent(new BigDecimal("70.00"))
                .instructorRatingBonusPercent(new BigDecimal("5.00"))
                .instructorRatingThreshold(new BigDecimal("4.50"))
                .moderatorMinuteRate(new BigDecimal("500"))
                .moderatorOtMultiplier(new BigDecimal("1.50"))
                .moderatorTaskBonus(new BigDecimal("10000"))
                .moderatorSlaPenalty(new BigDecimal("15000"))
                .fixedAllowance(BigDecimal.ZERO)
                .note("Cấu hình mặc định toàn hệ thống")
                .build();
    }
}

