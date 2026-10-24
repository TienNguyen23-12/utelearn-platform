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
public class QuestionReviewDTO {
    private Long questionId;
    private String content;
    private String questionType;
    private BigDecimal points;
    private BigDecimal earnedPoints;
    private Boolean isCorrect;
    private String explanation;
    @Builder.Default
    private List<QuestionOptionDTO> options = new ArrayList<>();
    @Builder.Default
    private List<String> studentSelectedOptionIds = new ArrayList<>();
    private String studentTextAnswer;
}
