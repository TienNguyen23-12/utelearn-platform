package vn.edu.ute.utelearn.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class CourseRequestDTO {
    @NotBlank(message = "Mã khóa học không được để trống")
    @Size(max = 50, message = "Mã khóa học tối đa 50 ký tự")
    private String code;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 500, message = "Mô tả ngắn tối đa 500 ký tự")
    private String headline;

    private String description;

    private String thumbnailUrl;

    private String level;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    private String objectives;
}
