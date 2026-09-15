package vn.edu.ute.utelearn.service;

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
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UtelearnProperties properties;

    @Override
    @Transactional
    public User register(RegisterRequestDTO requestDTO) {

        if (!requestDTO.isPasswordMatching()) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }

        String email = requestDTO.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Địa chỉ email '" + email + "' đã được đăng ký tài khoản!");
        }

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
}
