package vn.edu.ute.utelearn.service.impl;

import vn.edu.ute.utelearn.service.AuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.dao.RoleRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.AuthResponseDTO;
import vn.edu.ute.utelearn.dto.LoginRequestDTO;
import vn.edu.ute.utelearn.dto.RegisterRequestDTO;
import vn.edu.ute.utelearn.entity.Permission;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.security.JwtTokenProvider;
import vn.edu.ute.utelearn.dao.PasswordResetTokenRepository;
import vn.edu.ute.utelearn.dao.RegistrationOtpRepository;
import vn.edu.ute.utelearn.entity.PasswordResetToken;
import vn.edu.ute.utelearn.entity.RegistrationOtp;
import vn.edu.ute.utelearn.service.EmailService;
import vn.edu.ute.utelearn.util.ValidationUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RegistrationOtpRepository registrationOtpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UtelearnProperties properties;

    @Override
    @Transactional
    public void sendRegistrationOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ email không được để trống!");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Địa chỉ email '" + normalizedEmail + "' đã được đăng ký tài khoản!");
        }

        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        registrationOtpRepository.deleteByEmail(normalizedEmail);

        RegistrationOtp registrationOtp = RegistrationOtp.builder()
            .email(normalizedEmail)
            .otp(otp)
            .expiryDate(Instant.now().plus(5, java.time.temporal.ChronoUnit.MINUTES))
            .build();
        registrationOtpRepository.save(registrationOtp);

        emailService.sendRegistrationOtpEmail(normalizedEmail, otp);
        log.info("Đã gửi mã OTP xác thực đăng ký tài khoản tới email {}", normalizedEmail);
    }

    @Override
    @Transactional
    public User register(RegisterRequestDTO requestDTO) {

        if (!requestDTO.isPasswordMatching()) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        if (!ValidationUtils.isStrongPassword(requestDTO.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt!");
        }

        String email = requestDTO.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Địa chỉ email '" + email + "' đã được đăng ký tài khoản!");
        }

        String inputOtp = requestDTO.getOtp() != null ? requestDTO.getOtp().trim() : "";
        if (inputOtp.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã OTP xác thực!");
        }

        RegistrationOtp storedOtp = registrationOtpRepository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã OTP cho email này. Vui lòng nhấn gửi lại mã!"));

        if (storedOtp.isExpired()) {
            registrationOtpRepository.delete(storedOtp);
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng nhận mã mới!");
        }

        if (!storedOtp.getOtp().equals(inputOtp)) {
            throw new IllegalArgumentException("Mã OTP không chính xác. Vui lòng kiểm tra lại!");
        }

        registrationOtpRepository.delete(storedOtp);

        String username;
        if (requestDTO.getUsername() != null && !requestDTO.getUsername().isBlank()) {
            username = requestDTO.getUsername().trim();
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Tên đăng nhập '" + username + "' đã tồn tại trên hệ thống!");
            }
        } else {
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
            if (baseUsername.isEmpty()) {
                baseUsername = "user";
            }
            username = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsername(username)) {
                username = baseUsername + counter++;
            }
        }

        String fullName = (requestDTO.getFullName() != null && !requestDTO.getFullName().isBlank())
            ? requestDTO.getFullName().trim()
            : username;

        String phoneNumber = (requestDTO.getPhoneNumber() != null && !requestDTO.getPhoneNumber().isBlank())
            ? requestDTO.getPhoneNumber().trim()
            : null;

        Role defaultRole = roleRepository.findByCode("STUDENT")
            .orElseGet(() -> roleRepository.save(
                Role.builder()
                    .code("STUDENT")
                    .name("Học viên")
                    .description("Tài khoản học viên tự đăng ký")
                    .build()
            ));

        User newUser = User.builder()
            .username(username)
            .email(email)
            .fullName(fullName)
            .phoneNumber(phoneNumber)
            .passwordHash(passwordEncoder.encode(requestDTO.getPassword()))
            .isActive(true)
            .roles(Set.of(defaultRole))
            .build();

        User savedUser = userRepository.save(newUser);
        log.info("Đăng ký tài khoản người dùng thành công: email={}, username={}, role=STUDENT", email, savedUser.getUsername());
        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO requestDTO, HttpServletResponse response) {
        String loginId = requestDTO.getUsernameOrEmail().trim();

        User user = userRepository.findByUsernameOrEmail(loginId, loginId)
            .orElseThrow(() -> new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác!"));

        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác!");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new DisabledException("Tài khoản của bạn đã bị khóa hoặc tạm ngưng hoạt động!");
        }

        List<String> roleCodes = user.getRoles().stream()
            .map(Role::getCode)
            .collect(Collectors.toList());

        List<String> permissionCodes = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(Permission::getCode)
            .distinct()
            .collect(Collectors.toList());

        String accessToken = tokenProvider.generateAccessToken(user.getUsername(), user.getEmail(), roleCodes, permissionCodes);
        String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

        tokenProvider.setTokenCookie(response, accessToken, requestDTO.isRememberMe());

        log.info("Người dùng đăng nhập thành công: username={}, roles={}", user.getUsername(), roleCodes);

        return AuthResponseDTO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresInMs(properties.getJwt().getAccessTokenExpirationMs())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .roles(roleCodes)
            .build();
    }

    @Override
    public void logout(HttpServletResponse response) {
        tokenProvider.clearTokenCookie(response);
        SecurityContextHolder.clearContext();
        log.info("Người dùng đã đăng xuất và xóa Token Cookie.");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }

        String username;
        if (auth.getPrincipal() instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            username = auth.getName();
        }

        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public void processForgotPassword(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản nào với email này!"));

        String token = java.util.UUID.randomUUID().toString();
        passwordResetTokenRepository.deleteByUser(user);

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token(token)
            .user(user)
            .expiryDate(Instant.now().plus(15, java.time.temporal.ChronoUnit.MINUTES))
            .build();
            
        passwordResetTokenRepository.save(resetToken);

        String baseUrl;
        try {
            baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        } catch (Exception e) {
            baseUrl = "https://utelearn-platform.onrender.com";
        }
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        
        String emailContent = "<h3>Đặt lại mật khẩu UTELearn</h3>"
            + "<p>Xin chào " + user.getFullName() + ",</p>"
            + "<p>Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng click vào link bên dưới để đặt lại mật khẩu của bạn. Link này sẽ hết hạn sau 15 phút.</p>"
            + "<p><a href=\"" + resetUrl + "\">Nhấn vào đây để đặt lại mật khẩu</a></p>"
            + "<br><p>Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>";

        emailService.sendHtmlEmail(user.getEmail(), "Yêu cầu đặt lại mật khẩu UTELearn", emailContent);
    }

    @Override
    @Transactional
    public void processResetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn!"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Link đặt lại mật khẩu đã hết hạn!");
        }

        if (!ValidationUtils.isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt!");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        log.info("Người dùng {} đã đặt lại mật khẩu thành công.", user.getUsername());
    }
}
