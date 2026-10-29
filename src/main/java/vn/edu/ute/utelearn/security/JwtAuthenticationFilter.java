package vn.edu.ute.utelearn.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String token = tokenProvider.resolveToken(request);
        boolean accountLocked = false;

        try {
            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsernameFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!userDetails.isEnabled()) {
                    accountLocked = true;
                } else {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    String username = (auth.getPrincipal() instanceof UserDetails ud) ? ud.getUsername() : auth.getName();
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (!userDetails.isEnabled()) {
                        accountLocked = true;
                    }
                }
            }
        } catch (DisabledException ex) {
            accountLocked = true;
        } catch (Exception ex) {
            log.debug("Không thể thiết lập xác thực người dùng trong Security Context: {}", ex.getMessage());
        }

        if (accountLocked) {
            kickOutLockedUser(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void kickOutLockedUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
            }
        }
        tokenProvider.clearTokenCookie(response);

        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/login") || uri.startsWith("/logout") || uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/"))) {
            return;
        }

        String accept = request.getHeader("Accept");
        boolean isApi = (uri != null && uri.startsWith("/api/")) ||
                        "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With")) ||
                        (accept != null && accept.contains("application/json"));

        if (isApi) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Tài khoản của bạn đã bị khóa hoặc tạm ngưng hoạt động!\",\"code\":\"ACCOUNT_LOCKED\"}");
        } else {
            response.sendRedirect("/login?error=account_locked");
        }
    }
}
