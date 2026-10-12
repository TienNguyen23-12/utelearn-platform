package vn.edu.ute.utelearn.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.RoleRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        if (email == null) {
            log.error("Google OAuth2 User không cung cấp email");
            response.sendRedirect("/login?error=oauth2_email_missing");
            return;
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            boolean needsUpdate = false;
            if (picture != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty())) {
                user.setAvatarUrl(picture);
                needsUpdate = true;
            }
            if (!"GOOGLE".equals(user.getAuthProvider())) {
                user.setAuthProvider("GOOGLE");
                needsUpdate = true;
            }
            if (needsUpdate) {
                userRepository.save(user);
            }
        } else {
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
            String username = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsername(username)) {
                username = baseUsername + counter++;
            }

            Role studentRole = roleRepository.findByCode("STUDENT")
                .orElseGet(() -> roleRepository.save(
                    Role.builder()
                        .code("STUDENT")
                        .name("Học viên")
                        .description("Tài khoản tự động tạo qua Google")
                        .build()
                ));

            user = User.builder()
                .username(username)
                .email(email)
                .fullName(name != null ? name : username)
                .avatarUrl(picture)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .authProvider("GOOGLE")
                .isActive(true)
                .roles(Set.of(studentRole))
                .build();

            user = userRepository.save(user);
            log.info("Tự động tạo tài khoản mới từ Google OAuth2: username={}", username);
        }

        List<String> roleCodes = user.getRoles().stream()
            .map(Role::getCode)
            .collect(Collectors.toList());

        String accessToken = tokenProvider.generateAccessToken(user.getUsername(), user.getEmail(), roleCodes);

        tokenProvider.setTokenCookie(response, accessToken, true);

        boolean isAdmin = roleCodes.contains("ADMIN") || roleCodes.contains("ROLE_ADMIN");
        String targetUrl = isAdmin ? "/dashboard?loginSuccess=true" : "/home?loginSuccess=true";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
