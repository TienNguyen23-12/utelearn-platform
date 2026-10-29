package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.PayrollItemDTO;
import vn.edu.ute.utelearn.entity.PayrollConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayrollService {

    PayrollConfig getGlobalConfig();

    PayrollConfig updateGlobalConfig(
            BigDecimal instructorSharePercent,
            BigDecimal instructorRatingBonusPercent,
            BigDecimal instructorRatingThreshold,
            BigDecimal moderatorMinuteRate,
            BigDecimal moderatorOtMultiplier,
            BigDecimal moderatorTaskBonus,
            BigDecimal moderatorSlaPenalty,
            BigDecimal fixedAllowance);

    PayrollConfig saveUserOverrideConfig(
            Long userId,
            BigDecimal instructorSharePercent,
            BigDecimal instructorRatingBonusPercent,
            BigDecimal instructorRatingThreshold,
            BigDecimal moderatorMinuteRate,
            BigDecimal moderatorOtMultiplier,
            BigDecimal moderatorTaskBonus,
            BigDecimal moderatorSlaPenalty,
            BigDecimal fixedAllowance,
            String note);

    void resetUserConfigToDefault(Long userId);

    List<PayrollItemDTO> calculateInstructorPayroll(String period);

    List<PayrollItemDTO> calculateModeratorPayroll(String period);

    Optional<PayrollItemDTO> getInstructorPayrollForUser(Long userId, String period);

    Optional<PayrollItemDTO> getModeratorPayrollForUser(Long userId, String period);
}

