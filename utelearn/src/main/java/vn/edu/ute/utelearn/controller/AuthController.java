package vn.edu.ute.utelearn.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dto.AuthResponseDTO;
import vn.edu.ute.utelearn.dto.LoginRequestDTO;
import vn.edu.ute.utelearn.dto.RegisterRequestDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String showLoginPage(
        @RequestParam(value = "error", required = false) String error,
        @RequestParam(value = "logout", required = false) String logout,
        @RequestParam(value = "registered", required = false) String registered,
        Model model
    ) {
        if (authService.getCurrentAuthenticatedUser().isPresent()) {
            User user = authService.getCurrentAuthenticatedUser().get();
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
            return isAdmin ? "redirect:/dashboard" : "redirect:/home";
        }

        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginRequestDTO());
        }

        if (error != null) {
            if ("unauthorized".equals(error)) {
                model.addAttribute("errorMessage", "Vui lòng đăng nhập để tiếp tục!");
            } else if ("oauth2_email_missing".equals(error)) {
                model.addAttribute("errorMessage", "Tài khoản Google của bạn không cung cấp địa chỉ email!");
            } else {
                model.addAttribute("errorMessage", "Đăng nhập không thành công! Vui lòng thử lại.");
            }
        }

        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất khỏi hệ thống thành công.");
        }

        if (registered != null) {
            model.addAttribute("successMessage", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
        }

        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(
        @Valid @ModelAttribute("loginForm") LoginRequestDTO loginForm,
        BindingResult bindingResult,
        HttpServletResponse response,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            AuthResponseDTO authResponse = authService.login(loginForm, response);
            boolean isAdmin = authResponse.getRoles() != null &&
                (authResponse.getRoles().contains("ADMIN") || authResponse.getRoles().contains("ROLE_ADMIN"));
            if (isAdmin) {
                return "redirect:/dashboard";
            } else {
                return "redirect:/home";
            }
        } catch (BadCredentialsException ex) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác!");
            return "auth/login";
        } catch (DisabledException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/login";
        } catch (Exception ex) {
            log.error("Lỗi khi xử lý đăng nhập: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi hệ thống! Vui lòng thử lại sau.");
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (authService.getCurrentAuthenticatedUser().isPresent()) {
            User user = authService.getCurrentAuthenticatedUser().get();
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
            return isAdmin ? "redirect:/dashboard" : "redirect:/home";
        }

        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterRequestDTO());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(
        @Valid @ModelAttribute("registerForm") RegisterRequestDTO registerForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (!registerForm.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không trùng khớp!");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(registerForm);
            redirectAttributes.addAttribute("registered", "true");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        } catch (Exception ex) {
            log.error("Lỗi khi đăng ký tài khoản: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi trong quá trình tạo tài khoản. Vui lòng thử lại sau.");
            return "auth/register";
        }
    }

    @GetMapping("/logout")
    public String processLogout(HttpServletResponse response) {
        authService.logout(response);
        return "redirect:/login?logout=true";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new vn.edu.ute.utelearn.dto.ForgotPasswordRequestDTO());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
        @Valid @ModelAttribute("forgotPasswordForm") vn.edu.ute.utelearn.dto.ForgotPasswordRequestDTO requestDTO,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        try {
            authService.processForgotPassword(requestDTO.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Link đặt lại mật khẩu đã được gửi đến email của bạn.");
            return "redirect:/forgot-password";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/forgot-password";
        } catch (Exception ex) {
            log.error("Lỗi khi gửi email đặt lại mật khẩu: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi hệ thống! Vui lòng thử lại sau.");
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token, Model model) {
        if (!model.containsAttribute("resetPasswordForm")) {
            vn.edu.ute.utelearn.dto.ResetPasswordRequestDTO form = new vn.edu.ute.utelearn.dto.ResetPasswordRequestDTO();
            form.setToken(token);
            model.addAttribute("resetPasswordForm", form);
        }
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
        @Valid @ModelAttribute("resetPasswordForm") vn.edu.ute.utelearn.dto.ResetPasswordRequestDTO requestDTO,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (!requestDTO.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không trùng khớp!");
        }
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }
        try {
            authService.processResetPassword(requestDTO.getToken(), requestDTO.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/reset-password";
        } catch (Exception ex) {
            log.error("Lỗi khi đặt lại mật khẩu: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Đã xảy ra lỗi hệ thống! Vui lòng thử lại sau.");
            return "auth/reset-password";
        }
    }
}
