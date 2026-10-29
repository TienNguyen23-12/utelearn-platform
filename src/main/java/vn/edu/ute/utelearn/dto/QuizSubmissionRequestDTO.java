package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmissionRequestDTO {
    @NotNull(message = "Cohort ID không được để trống")
    private Long cohortId;

    private Long submissionId;

    @Builder.Default
    private List<StudentAnswerDTO> answers = new ArrayList<>();
}
