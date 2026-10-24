package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class QuestionRequestDTO {
    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    @NotBlank(message = "Loại câu hỏi không được để trống")
    private String questionType;

    @NotNull(message = "Điểm số không được để trống")
    @Builder.Default
    private BigDecimal points = new BigDecimal("1.0");

    private String explanation;

    private Integer orderIndex;

    @Builder.Default
    private List<QuestionOptionDTO> options = new ArrayList<>();
}
