package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizRequestDTO {
    @NotBlank(message = "Tiêu đề bài kiểm tra không được để trống")
    private String title;

    @Builder.Default
    private Integer durationMinutes = 15;

    @Builder.Default
    private BigDecimal passingScore = new BigDecimal("5.0");

    @Builder.Default
    private Integer maxAttempts = 1;

    @Builder.Default
    private Boolean shuffleQuestions = true;

    @Builder.Default
    private Boolean allowReview = true;

    @Builder.Default
    private Boolean allowLateSubmission = true;
}
