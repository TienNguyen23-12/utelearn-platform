package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseUpsellDTO {
    private Course course;
    private int totalModules;
    private int enrolledModulesCount;
    private int remainingModulesCount;
    private BigDecimal coursePrice;
    private BigDecimal totalPaidAmount;
    private BigDecimal upgradePrice;
    private BigDecimal remainingModulesOriginalPrice;
    private BigDecimal savingsAmount;
    private List<Cohort> remainingCohorts;
}
