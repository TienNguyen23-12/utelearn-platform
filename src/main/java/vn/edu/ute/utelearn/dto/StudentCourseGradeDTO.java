package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.utelearn.entity.Course;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseGradeDTO {
    private Course course;
    private Long userId;
    private String fullName;
    private String email;
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
    private List<ModuleGradeDetailDTO> moduleGrades;
}
