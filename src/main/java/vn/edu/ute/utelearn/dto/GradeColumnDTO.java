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
public class GradeColumnDTO {
    private Long lessonId;
    private String title;
    private String lessonType;
    private boolean isFinalExam;
    private BigDecimal maxScore;
    private BigDecimal passingScore;
    private Long assessmentId;
}
