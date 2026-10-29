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
            throw new IllegalArgumentException("Äá»‹a chá»‰ email khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng!");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Äá»‹a chá»‰ email '" + normalizedEmail + "' Ä‘Ã£ Ä‘Æ°á»£c Ä‘Äƒng kÃ½ tÃ i khoáº£n!");
        }

        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        registrationOtpRepository.deleteByEmail(normalizedEmail);

        RegistrationOtp registrationOtp = RegistrationOtp.builder()
            .email(normalizedEmail)
            .otp(otp)
            .expiryDate(Instant.now().plus(5, java.time.temporal.ChronoUnit.MINUTES))
            .attemptCount(0)
            .build();
        registrationOtpRepository.save(registrationOtp);

        emailService.sendRegistrationOtpEmail(normalizedEmail, otp);
        log.info("ÄÃ£ gá»­i mÃ£ OTP xÃ¡c thá»±c Ä‘Äƒng kÃ½ tÃ i khoáº£n tá»›i email {}", normalizedEmail);
    }

    @Override
    @Transactional
    public User register(RegisterRequestDTO requestDTO) {
        if (!requestDTO.isPasswordMatching()) {
            throw new IllegalArgumentException("Máº­t kháº©u xÃ¡c nháº­n khÃ´ng khá»›p!");
        }

        if (!ValidationUtils.isStrongPassword(requestDTO.getPassword())) {
            throw new IllegalArgumentException("Máº­t kháº©u pháº£i cÃ³ Ã­t nháº¥t 8 kÃ½ tá»±, bao gá»“m chá»¯ hoa, chá»¯ thÆ°á»ng, chá»¯ sá»‘ vÃ  kÃ½ tá»± Ä‘áº·c biá»‡t!");
        }

        String email = requestDTO.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Äá»‹a chá»‰ email '" + email + "' Ä‘Ã£ Ä‘Æ°á»£c Ä‘Äƒng kÃ½ tÃ i khoáº£n!");
        }

        String inputOtp = requestDTO.getOtp() != null ? requestDTO.getOtp().trim() : "";
        if (inputOtp.isBlank()) {
            throw new IllegalArgumentException("Vui lÃ²ng nháº­p mÃ£ OTP xÃ¡c thá»±c!");
        }

        RegistrationOtp storedOtp = registrationOtpRepository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y mÃ£ OTP cho email nÃ y. Vui lÃ²ng nháº¥n gá»­i láº¡i mÃ£!"));

        if (storedOtp.isExpired()) {
            registrationOtpRepository.delete(storedOtp);
            throw new IllegalArgumentException("MÃ£ OTP Ä‘Ã£ háº¿t háº¡n. Vui lÃ²ng nháº­n mÃ£ má»›i!");
        }

        if (!storedOtp.getOtp().equals(inputOtp)) {
            int attempts = (storedOtp.getAttemptCount() != null ? storedOtp.getAttemptCount() : 0) + 1;
            storedOtp.setAttemptCount(attempts);
            if (attempts >= 5) {
                registrationOtpRepository.delete(storedOtp);
                throw new IllegalArgumentException("Báº¡n Ä‘Ã£ nháº­p sai mÃ£ OTP quÃ¡ 5 láº§n. MÃ£ OTP Ä‘Ã£ bá»‹ há»§y, vui lÃ²ng gá»­i láº¡i mÃ£ má»›i!");
            }
            registrationOtpRepository.save(storedOtp);
            throw new IllegalArgumentException("MÃ£ OTP khÃ´ng chÃ­nh xÃ¡c. Báº¡n cÃ²n " + (5 - attempts) + " láº§n thá»­!");
        }

        registrationOtpRepository.delete(storedOtp);

        String username;
        if (requestDTO.getUsername() != null && !requestDTO.getUsername().isBlank()) {
            username = requestDTO.getUsername().trim();
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("TÃªn Ä‘Äƒng nháº­p '" + username + "' Ä‘Ã£ tá»“n táº¡i trÃªn há»‡ thá»‘ng!");
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
                    .name("Há»c viÃªn")
                    .description("TÃ i khoáº£n há»c viÃªn tá»± Ä‘Äƒng kÃ½")
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
        log.info("ÄÄƒng kÃ½ tÃ i khoáº£n ngÆ°á»i dÃ¹ng thÃ nh cÃ´ng: email={}, username={}, role=STUDENT", email, savedUser.getUsername());
        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO requestDTO, HttpServletResponse response) {
        String loginId = requestDTO.getUsernameOrEmail().trim();

        User user = userRepository.findByUsernameOrEmail(loginId, loginId)
            .orElseThrow(() -> new BadCredentialsException("TÃªn Ä‘Äƒng nháº­p hoáº·c máº­t kháº©u khÃ´ng chÃ­nh xÃ¡c!"));

        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("TÃªn Ä‘Äƒng nháº­p hoáº·c máº­t kháº©u khÃ´ng chÃ­nh xÃ¡c!");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new DisabledException("TÃ i khoáº£n cá»§a báº¡n Ä‘Ã£ bá»‹ khÃ³a hoáº·c táº¡m ngÆ°ng hoáº¡t Ä‘á»™ng!");
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

        log.info("NgÆ°á»i dÃ¹ng Ä‘Äƒng nháº­p thÃ nh cÃ´ng: username={}, roles={}", user.getUsername(), roleCodes);

        return AuthResponseDTO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresInMs(properties.getJwt().getAccessTokenExpirationMs())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .roles(roleCodes)
            .permissions(permissionCodes)
            .build();
    }

    @Override
    public void logout(HttpServletResponse response) {
        tokenProvider.clearTokenCookie(response);
        SecurityContextHolder.clearContext();
        log.info("NgÆ°á»i dÃ¹ng Ä‘Ã£ Ä‘Äƒng xuáº¥t vÃ  xÃ³a Token Cookie.");
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

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || Boolean.FALSE.equals(userOpt.get().getIsActive())) {
            SecurityContextHolder.clearContext();
            return Optional.empty();
        }
        return userOpt;
    }

    @Override
    @Transactional
    public void processForgotPassword(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y tÃ i khoáº£n nÃ o vá»›i email nÃ y!"));

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
        
        String emailContent = "<h3>Äáº·t láº¡i máº­t kháº©u UTELearn</h3>"
            + "<p>Xin chÃ o " + user.getFullName() + ",</p>"
            + "<p>Báº¡n Ä‘Ã£ yÃªu cáº§u Ä‘áº·t láº¡i máº­t kháº©u. Vui lÃ²ng click vÃ o link bÃªn dÆ°á»›i Ä‘á»ƒ Ä‘áº·t láº¡i máº­t kháº©u cá»§a báº¡n. Link nÃ y sáº½ háº¿t háº¡n sau 15 phÃºt.</p>"
            + "<p><a href=\"" + resetUrl + "\">Nháº¥n vÃ o Ä‘Ã¢y Ä‘á»ƒ Ä‘áº·t láº¡i máº­t kháº©u</a></p>"
            + "<br><p>Náº¿u báº¡n khÃ´ng yÃªu cáº§u, vui lÃ²ng bá» qua email nÃ y.</p>";

        log.info("[AuthService] Link dat lai mat khau cho {}: {}", user.getEmail(), resetUrl);
        emailService.sendHtmlEmail(user.getEmail(), "YÃªu cáº§u Ä‘áº·t láº¡i máº­t kháº©u UTELearn", emailContent);
    }

    @Override
    @Transactional
    public void processResetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Link Ä‘áº·t láº¡i máº­t kháº©u khÃ´ng há»£p lá»‡ hoáº·c Ä‘Ã£ háº¿t háº¡n!"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Link Ä‘áº·t láº¡i máº­t kháº©u Ä‘Ã£ háº¿t háº¡n!");
        }

        if (!ValidationUtils.isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("Máº­t kháº©u má»›i pháº£i cÃ³ Ã­t nháº¥t 8 kÃ½ tá»±, bao gá»“m chá»¯ hoa, chá»¯ thÆ°á»ng, chá»¯ sá»‘ vÃ  kÃ½ tá»± Ä‘áº·c biá»‡t!");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
        log.info("NgÆ°á»i dÃ¹ng {} Ä‘Ã£ Ä‘áº·t láº¡i máº­t kháº©u thÃ nh cÃ´ng.", user.getUsername());
    }
}
