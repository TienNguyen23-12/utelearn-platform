package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequestDTO {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 150, message = "Họ và tên phải từ 2 đến 150 ký tự")
    private String fullName;

    @Pattern(
        regexp = "^(84|0[3|5|7|8|9])[0-9]{8}$|^$",
        message = "Số điện thoại di động Việt Nam không đúng định dạng (VD: 0901234567)"
    )
    private String phoneNumber;

    private String avatarUrl;
}
