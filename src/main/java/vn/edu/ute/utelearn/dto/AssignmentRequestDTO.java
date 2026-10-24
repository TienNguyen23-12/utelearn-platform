package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRequestDTO {
    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    private String title;

    private String instruction;

    private String attachmentUrl;

    @NotNull(message = "Thang điểm không được để trống")
    @DecimalMin(value = "0.0", message = "Thang điểm tối thiểu là 0.0")
    @Builder.Default
    private BigDecimal maxScore = new BigDecimal("10.0");

    @NotNull(message = "Điểm sàn đạt không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm sàn đạt tối thiểu là 0.0")
    @Builder.Default
    private BigDecimal passingScore = new BigDecimal("5.0");

    @Builder.Default
    private Boolean allowLateSubmission = true;

    @Builder.Default
    private Boolean allowFileSubmission = true;

    @Builder.Default
    private Boolean allowTextSubmission = true;

    @Builder.Default
    private String allowedFileTypes = "pdf,doc,docx,zip,rar,txt";

    @Builder.Default
    private Integer maxFileSizeMb = 20;
}
