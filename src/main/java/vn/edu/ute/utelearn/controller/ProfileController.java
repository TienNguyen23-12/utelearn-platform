package vn.edu.ute.utelearn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.service.CloudinaryService;
import java.util.Map;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dto.ChangePasswordRequestDTO;
import vn.edu.ute.utelearn.dto.ProfileUpdateRequestDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.util.ValidationUtils;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final PasswordEncoder passwordEncoder;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CloudinaryService cloudinaryService;

    @GetMapping
    public String showProfile(
        @RequestParam(value = "updated", required = false) String updated,
        @RequestParam(value = "passwordChanged", required = false) String passwordChanged,
        Model model
    ) {
        User user = authService.getCurrentAuthenticatedUser().orElse(null);
        if (user == null) {
            return "redirect:/login?error=unauthorized";
        }

        model.addAttribute("user", user);

        if (!model.containsAttribute("profileForm")) {
            ProfileUpdateRequestDTO profileForm = ProfileUpdateRequestDTO.builder()
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .build();
            model.addAttribute("profileForm", profileForm);
        }

        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ChangePasswordRequestDTO());
        }

        if (updated != null) {
            model.addAttribute("profileSuccessMessage", "Cập nhật thông tin hồ sơ thành công!");
        }

        if (passwordChanged != null) {
            model.addAttribute("passwordSuccessMessage", "Đổi mật khẩu tài khoản thành công!");
        }

        model.addAttribute("myCourses", cohortMemberRepository.findByUserOrderByEnrolledAtDesc(user));

        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile(
        @Valid @ModelAttribute("profileForm") ProfileUpdateRequestDTO profileForm,
        BindingResult bindingResult,
        @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        User user = authService.getCurrentAuthenticatedUser().orElse(null);
        if (user == null) {
            return "redirect:/login?error=unauthorized";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("passwordForm", new ChangePasswordRequestDTO());
            return "profile/index";
        }

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                if (cloudinaryService != null) {
                    Map<String, Object> uploadResult = cloudinaryService.uploadFile(avatarFile, "avatars", "image");
                    String uploadedUrl = (String) uploadResult.get("secure_url");
                    if (uploadedUrl == null || uploadedUrl.isBlank()) {
                        uploadedUrl = (String) uploadResult.get("url");
                    }
                    if (uploadedUrl != null && !uploadedUrl.isBlank()) {
                        user.setAvatarUrl(uploadedUrl);
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi khi tải ảnh đại diện lên Cloudinary: {}", e.getMessage());
            }
        } else if (profileForm.getAvatarUrl() != null && !profileForm.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(profileForm.getAvatarUrl().trim());
        }

        user.setFullName(profileForm.getFullName().trim());
        user.setPhoneNumber(profileForm.getPhoneNumber() != null && !profileForm.getPhoneNumber().isBlank() 
            ? profileForm.getPhoneNumber().trim() 
            : null);

        userRepository.save(user);
        log.info("Người dùng '{}' cập nhật thông tin hồ sơ thành công", user.getUsername());

        redirectAttributes.addAttribute("updated", "true");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(
        @Valid @ModelAttribute("passwordForm") ChangePasswordRequestDTO passwordForm,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        User user = authService.getCurrentAuthenticatedUser().orElse(null);
        if (user == null) {
            return "redirect:/login?error=unauthorized";
        }

        if ("GOOGLE".equals(user.getAuthProvider())) {
            redirectAttributes.addFlashAttribute("error", "Tài khoản đăng nhập bằng Google không thể đổi mật khẩu.");
            return "redirect:/profile";
        }

        if (!passwordEncoder.matches(passwordForm.getCurrentPassword(), user.getPasswordHash())) {
            bindingResult.rejectValue("currentPassword", "error.currentPassword", "Mật khẩu hiện tại không chính xác!");
        }

        if (!passwordForm.isPasswordMatching()) {
            bindingResult.rejectValue("confirmNewPassword", "error.confirmNewPassword", "Mật khẩu mới xác nhận không khớp!");
        }

        if (passwordForm.getNewPassword() != null && !ValidationUtils.isStrongPassword(passwordForm.getNewPassword())) {
            bindingResult.rejectValue("newPassword", "error.newPassword", "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt!");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            ProfileUpdateRequestDTO profileForm = ProfileUpdateRequestDTO.builder()
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .build();
            model.addAttribute("profileForm", profileForm);
            return "profile/index";
        }

        user.setPasswordHash(passwordEncoder.encode(passwordForm.getNewPassword()));
        userRepository.save(user);
        log.info("Người dùng '{}' đổi mật khẩu tài khoản thành công", user.getUsername());

        redirectAttributes.addAttribute("passwordChanged", "true");
        return "redirect:/profile";
    }
}
