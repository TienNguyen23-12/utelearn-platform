package vn.edu.ute.utelearn.controller.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.dto.AuthResponseDTO;
import vn.edu.ute.utelearn.dto.LoginRequestDTO;
import vn.edu.ute.utelearn.dto.RegisterRequestDTO;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.dto.ForgotPasswordRequestDTO;
import vn.edu.ute.utelearn.dto.ResetPasswordRequestDTO;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO requestDTO, BindingResult bindingResult, HttpServletResponse response) {
        Map<String, Object> res = new HashMap<>();
        if (bindingResult.hasErrors()) {
            res.put("success", false);
            res.put("message", "Dữ liệu nhập không hợp lệ");
            return ResponseEntity.badRequest().body(res);
        }

        try {
            AuthResponseDTO authResponse = authService.login(requestDTO, response);
            boolean isAdmin = authResponse.getRoles() != null &&
                    (authResponse.getRoles().contains("ADMIN") || authResponse.getRoles().contains("ROLE_ADMIN"));
            
            res.put("success", true);
            res.put("message", "Đăng nhập thành công!");
            res.put("redirectUrl", isAdmin ? "/dashboard" : "/home");
            res.put("data", authResponse);
            return ResponseEntity.ok(res);
        } catch (Exception ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequestDTO requestDTO, BindingResult bindingResult) {
        Map<String, Object> res = new HashMap<>();
        if (!requestDTO.isPasswordMatching()) {
            res.put("success", false);
            res.put("message", "Mật khẩu xác nhận không khớp!");
            return ResponseEntity.badRequest().body(res);
        }
        if (bindingResult.hasErrors()) {
            res.put("success", false);
            res.put("message", "Vui lòng nhập đầy đủ thông tin hợp lệ.");
            return ResponseEntity.badRequest().body(res);
        }

        try {
            authService.register(requestDTO);
            res.put("success", true);
            res.put("message", "Đăng ký thành công! Vui lòng đăng nhập.");
            res.put("redirectUrl", "/login?registered=true");
            return ResponseEntity.ok(res);
        } catch (Exception ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        Map<String, Object> res = new HashMap<>();
        authService.logout(response);
        res.put("success", true);
        res.put("message", "Đăng xuất thành công");
        res.put("redirectUrl", "/login?logout=true");
        return ResponseEntity.ok(res);
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO requestDTO, BindingResult bindingResult) {
        Map<String, Object> res = new HashMap<>();
        if (bindingResult.hasErrors()) {
            res.put("success", false);
            res.put("message", "Email không hợp lệ.");
            return ResponseEntity.badRequest().body(res);
        }
        try {
            authService.processForgotPassword(requestDTO.getEmail());
            res.put("success", true);
            res.put("message", "Link đặt lại mật khẩu đã được gửi đến email của bạn.");
            return ResponseEntity.ok(res);
        } catch (Exception ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO requestDTO, BindingResult bindingResult) {
        Map<String, Object> res = new HashMap<>();
        if (!requestDTO.isPasswordMatching()) {
            res.put("success", false);
            res.put("message", "Mật khẩu xác nhận không khớp!");
            return ResponseEntity.badRequest().body(res);
        }
        if (bindingResult.hasErrors()) {
            res.put("success", false);
            res.put("message", "Vui lòng kiểm tra lại thông tin.");
            return ResponseEntity.badRequest().body(res);
        }
        try {
            authService.processResetPassword(requestDTO.getToken(), requestDTO.getNewPassword());
            res.put("success", true);
            res.put("message", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
            res.put("redirectUrl", "/login");
            return ResponseEntity.ok(res);
        } catch (Exception ex) {
            res.put("success", false);
            res.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
}
