package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGradeItemDTO {
    private Long memberId;
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private Instant enrolledAt;

    private int completedLessonsCount;
    private int totalLessonsCount;
    private int progressPercent;

    private BigDecimal processGrade;
    private BigDecimal finalExamGrade;
    private BigDecimal finalGrade;

    private Boolean isPassed;
    private String certificateUrl;
    private Instant gradedAt;

    private Integer rating;
    private String reviewComment;
    private Instant reviewedAt;
    private boolean hasReviewed;

    private Map<Long, StudentColumnGradeDTO> columnGrades;
    private List<StudentColumnGradeDTO> processColumnGrades;
    private StudentColumnGradeDTO finalExamColumnGrade;

    public boolean isHasReviewed() {
        return (rating != null && rating >= 1 && rating <= 5) || hasReviewed;
    }

    public String getFormattedReviewedAt() {
        if (reviewedAt == null) {
            return "";
        }
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(reviewedAt);
    }

    public List<StudentColumnGradeDTO> getAllColumnGrades() {
        List<StudentColumnGradeDTO> list = new java.util.ArrayList<>();
        if (processColumnGrades != null) {
            list.addAll(processColumnGrades);
        }
        if (finalExamColumnGrade != null) {
            list.add(finalExamColumnGrade);
        }
        return list;
    }
}
