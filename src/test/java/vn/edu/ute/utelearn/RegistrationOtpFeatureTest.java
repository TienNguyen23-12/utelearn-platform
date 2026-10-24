package vn.edu.ute.utelearn;

import org.junit.jupiter.api.Test;
import vn.edu.ute.utelearn.dto.RegisterRequestDTO;
import vn.edu.ute.utelearn.dto.SendOtpRequestDTO;
import vn.edu.ute.utelearn.entity.RegistrationOtp;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class RegistrationOtpFeatureTest {

    @Test
    void testRegistrationOtp_NotExpired() {
        RegistrationOtp otp = RegistrationOtp.builder()
                .id(1L)
                .email("student@utelearn.edu.vn")
                .otp("123456")
                .expiryDate(Instant.now().plusSeconds(300))
                .build();

        assertFalse(otp.isExpired());
        assertEquals("student@utelearn.edu.vn", otp.getEmail());
        assertEquals("123456", otp.getOtp());
    }

    @Test
    void testRegistrationOtp_Expired() {
        RegistrationOtp otp = RegistrationOtp.builder()
                .id(2L)
                .email("student@utelearn.edu.vn")
                .otp("654321")
                .expiryDate(Instant.now().minusSeconds(5))
                .build();

        assertTrue(otp.isExpired());
    }

    @Test
    void testRegisterRequestDTO_PasswordMatchingAndOtp() {
        RegisterRequestDTO dto = RegisterRequestDTO.builder()
                .email("test@utelearn.edu.vn")
                .password("Password123!")
                .confirmPassword("Password123!")
                .otp("123456")
                .build();

        assertTrue(dto.isPasswordMatching());
        assertEquals("123456", dto.getOtp());

        RegisterRequestDTO mismatchDto = RegisterRequestDTO.builder()
                .email("test@utelearn.edu.vn")
                .password("Password123!")
                .confirmPassword("Different123!")
                .otp("123456")
                .build();

        assertFalse(mismatchDto.isPasswordMatching());
    }

    @Test
    void testSendOtpRequestDTO_GetterSetter() {
        SendOtpRequestDTO dto = SendOtpRequestDTO.builder()
                .email("test@utelearn.edu.vn")
                .build();

        assertEquals("test@utelearn.edu.vn", dto.getEmail());
    }
}
