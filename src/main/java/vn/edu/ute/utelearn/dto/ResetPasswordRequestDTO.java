package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.ute.utelearn.util.ValidationUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDTO {
    
    private String token;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
        regexp = ValidationUtils.PASSWORD_REGEX,
        message = "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt"
    )
    private String newPassword;

    @NotBlank(message = "Mật khẩu xác nhận không được để trống")
    private String confirmPassword;

    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
