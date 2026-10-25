package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDTO {
    private Long submissionId;
    private Long quizId;
    private String quizTitle;
    private BigDecimal score;
    private BigDecimal passingScore;
    private Boolean isPassed;
    private Integer totalQuestions;
    private Integer correctCount;
    private Instant startedAt;
    private Instant submittedAt;
    private Integer attemptNumber;
    private Integer maxAttempts;
    private Boolean canRetake;
    private Boolean allowReview;
    private Boolean isLate;
    @Builder.Default
    private List<QuestionReviewDTO> reviewQuestions = new ArrayList<>();
}
