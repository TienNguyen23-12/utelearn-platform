package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDTO {
    private Long id;
    private Long lessonId;
    private String lessonStatus;
    private String title;
    private Integer durationMinutes;
    private BigDecimal passingScore;
    private Integer maxAttempts;
    private Boolean shuffleQuestions;
    private Boolean allowReview;
    private Integer totalQuestions;
    private BigDecimal totalPoints;
    @Builder.Default
    private List<QuestionDTO> questions = new ArrayList<>();
    private Integer userAttemptsCount;
    private BigDecimal userHighestScore;
    private Boolean userPassed;
    private Boolean canAttempt;
    private Boolean isReviewer;
}
