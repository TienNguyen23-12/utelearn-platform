package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentColumnGradeDTO {
    private Long lessonId;
    private String lessonTitle;
    private String lessonType;
    private boolean isFinalExam;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String status;
    private Long submissionId;
    private String feedback;
    private Instant submittedAt;
    private Instant gradedAt;
}
