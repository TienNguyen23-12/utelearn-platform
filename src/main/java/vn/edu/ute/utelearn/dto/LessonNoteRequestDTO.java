package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonNoteRequestDTO {
    private Long lessonId;
    private Long cohortId;
    private Integer timestampSeconds;

    @NotBlank(message = "Nội dung ghi chú không được để trống")
    private String content;
}
