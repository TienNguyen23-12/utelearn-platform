package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.Course;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEnrollmentSummaryDTO {
    private List<EnrolledCourseItem> enrolledCourses;
    private List<CohortMember> enrolledModules;
    private List<CourseUpsellDTO> upsellSuggestions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnrolledCourseItem {
        private Course course;
        private Instant enrolledAt;
        private int totalModules;
        private int enrolledModulesCount;
        private int completedModulesCount;
        private int progressPercent;
        private Boolean isCompleted;
        private BigDecimal averageGrade;
        private String certificateUrl;
        private boolean hasReviewed;
        private Integer rating;
        private String reviewComment;

        public Boolean getIsCompleted() {
            return isCompleted;
        }

        public boolean isCompleted() {
            return Boolean.TRUE.equals(isCompleted);
        }

        public boolean isHasReviewed() {
            return hasReviewed;
        }

        public boolean getHasReviewed() {
            return hasReviewed;
        }
    }
}
