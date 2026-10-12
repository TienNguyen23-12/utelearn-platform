package vn.edu.ute.utelearn.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class LessonRequestDTO {
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Loại bài học không được để trống")
    private String lessonType; 

    private String videoUrl;
    
    private Integer durationSeconds;

    private String documentContent;

    private Boolean isFreePreview = false;

    private Integer orderIndex;

    private String assetUrl;

    private Boolean isPublicForCohorts = false;
}
