package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollItemDTO {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String roleType;
    private boolean hasCustomConfig;
    private String customNote;

    private BigDecimal effectiveSharePercent;
    private BigDecimal customSharePercent;
    private BigDecimal effectiveRatingBonusPercent;
    private BigDecimal customRatingBonusPercent;
    private BigDecimal effectiveRatingThreshold;
    private BigDecimal customRatingThreshold;

    private BigDecimal effectiveMinuteRate;
    private BigDecimal customMinuteRate;
    private BigDecimal effectiveOtMultiplier;
    private BigDecimal customOtMultiplier;
    private BigDecimal effectiveTaskBonus;
    private BigDecimal customTaskBonus;
    private BigDecimal effectiveSlaPenalty;
    private BigDecimal customSlaPenalty;

    private BigDecimal effectiveFixedAllowance;
    private BigDecimal customFixedAllowance;

    private int cohortCount;
    private long studentCount;
    @Builder.Default
    private BigDecimal grossRevenue = BigDecimal.ZERO;
    private Double averageRating;
    private long ratingCount;
    private boolean qualifiedForRatingBonus;
    @Builder.Default
    private BigDecimal baseCommissionAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal ratingBonusAmount = BigDecimal.ZERO;

    private int workDaysCount;
    private int regularMinutes;
    private int otMinutes;
    private long completedTasksCount;
    private long escalatedTasksCount;
    @Builder.Default
    private BigDecimal regularShiftPay = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal otShiftPay = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal taskBonusPay = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal slaPenaltyAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalNetPay = BigDecimal.ZERO;
}

