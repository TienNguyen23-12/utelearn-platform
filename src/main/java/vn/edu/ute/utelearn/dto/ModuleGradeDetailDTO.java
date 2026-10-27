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
public class ModuleGradeDetailDTO {
    private Long moduleId;
    private String moduleCode;
    private String moduleName;
    private String gradingPolicy;
    private boolean isEnrolled;
    private boolean isGradesPublished;
    private BigDecimal finalGrade;
    private Boolean isPassed;
    private int completedLessons;
    private int totalLessons;
    private int progressPercent;
    private String certificateUrl;
    private boolean hasReviewed;
}
