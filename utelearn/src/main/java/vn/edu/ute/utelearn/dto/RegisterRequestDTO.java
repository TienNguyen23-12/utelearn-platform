package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.Email;
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
public class RegisterRequestDTO {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng (ví dụ: user@utelearn.edu.vn)")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu")
    private String confirmPassword;

    private String username;

    private String fullName;

    private String phoneNumber;

    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
