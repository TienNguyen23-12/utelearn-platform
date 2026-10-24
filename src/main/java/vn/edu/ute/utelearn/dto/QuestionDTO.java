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
public class QuestionDTO {
    private Long id;
    private Long quizId;
    private String content;
    private String questionType;
    private BigDecimal points;
    private String explanation;
    private Integer orderIndex;
    @Builder.Default
    private List<QuestionOptionDTO> options = new ArrayList<>();
}
