package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseStudentGradeDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private Instant enrolledAt;
    private int completedModulesCount;
    private int totalModulesCount;
    private int progressPercent;
    private BigDecimal courseAverageGrade;
    private Boolean isCompleted;
    private Instant completedAt;
    private String certificateUrl;
    private boolean hasReviewed;
    private Integer rating;
    private String reviewComment;
    private Map<Long, ModuleGradeDetailDTO> moduleGrades;
}
