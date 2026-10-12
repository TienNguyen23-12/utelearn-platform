package vn.edu.ute.utelearn.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");

        if (uri.startsWith("/api/") || (accept != null && accept.contains("application/json"))) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"Yêu cầu chưa được xác thực hoặc phiên đăng nhập đã hết hạn!\"}");
        } else {
            response.sendRedirect("/login?error=unauthorized");
        }
    }
}
