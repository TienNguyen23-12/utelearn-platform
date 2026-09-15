package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    @NotBlank(message = "Vui lòng nhập tên đăng nhập hoặc email")
    private String usernameOrEmail;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;

    @Builder.Default
    private boolean rememberMe = false;
}
