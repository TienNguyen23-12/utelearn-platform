package vn.edu.ute.utelearn.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");

        if (uri.startsWith("/api/") || (accept != null && accept.contains("application/json"))) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền truy cập vào chức năng này!\"}");
        } else {
            response.sendRedirect("/home?error=admin_only");
        }
    }
}
