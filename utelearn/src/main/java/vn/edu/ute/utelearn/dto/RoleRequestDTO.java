package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequestDTO {

    private Long id;

    @NotBlank(message = "Mã vai trò không được để trống")
    @Pattern(
        regexp = "^[A-Z0-9_]{2,50}$",
        message = "Mã vai trò phải viết hoa, từ 2-50 ký tự, chỉ gồm chữ cái, số và dấu gạch dưới (VD: INSTRUCTOR, CONTENT_CREATOR)"
    )
    private String code;

    @NotBlank(message = "Tên vai trò không được để trống")
    @Size(min = 2, max = 100, message = "Tên vai trò từ 2 đến 100 ký tự")
    private String name;

    private String description;

    @Builder.Default
    private List<Long> permissionIds = new ArrayList<>();
}
