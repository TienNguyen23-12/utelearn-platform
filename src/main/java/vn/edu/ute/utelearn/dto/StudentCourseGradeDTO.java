package vn.edu.ute.utelearn.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonIgnore
    private Course course;
    private Long courseId;
    private String courseTitle;
    private String courseSlug;
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

    public BigDecimal getAverageGrade() {
        return courseAverageGrade;
    }

    public int getTotalModules() {
        return totalModulesCount;
    }

    @JsonProperty("completed")
    public boolean isCompletedGrade() {
        return Boolean.TRUE.equals(isCompleted);
    }

    public List<ModuleGradeDetailDTO> getModules() {
        return moduleGrades;
    }
}
