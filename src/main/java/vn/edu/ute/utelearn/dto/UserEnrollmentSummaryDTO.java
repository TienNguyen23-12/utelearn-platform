package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.Course;

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
    }
}
