package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendOtpRequestDTO {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng (ví dụ: user@utelearn.edu.vn)")
    private String email;
}
