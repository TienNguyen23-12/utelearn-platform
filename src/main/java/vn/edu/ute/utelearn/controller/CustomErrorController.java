package vn.edu.ute.utelearn.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CustomErrorController implements ErrorController {

    private final AuthService authService;

    @RequestMapping(value = "/error", produces = "text/html")
    public String handleErrorHtml(HttpServletRequest request, Model model) {
        int statusCode = resolveStatusCode(request);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (requestUri == null) {
            requestUri = request.getRequestURI();
        }

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        currentUserOpt.ifPresent(user -> model.addAttribute("currentUser", user));

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", resolveTitle(statusCode));
        model.addAttribute("errorMessage", resolveMessage(statusCode));
        model.addAttribute("requestUri", requestUri);

        return "error";
    }

    @RequestMapping("/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleErrorJson(HttpServletRequest request) {
        int statusCode = resolveStatusCode(request);
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("status", statusCode);
        body.put("error", resolveTitle(statusCode));
        body.put("message", resolveMessage(statusCode));

        HttpStatus status;
        try {
            status = HttpStatus.valueOf(statusCode);
        } catch (IllegalArgumentException ex) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(status).body(body);
    }

    private int resolveStatusCode(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        try {
            return Integer.parseInt(status.toString());
        } catch (NumberFormatException ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
    }

    private String resolveTitle(int statusCode) {
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "404 - Không tìm thấy trang";
        }
        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            return "403 - Quyền truy cập bị từ chối";
        }
        if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            return "401 - Phiên đăng nhập chưa xác thực";
        }
        if (statusCode == HttpStatus.BAD_REQUEST.value()) {
            return "400 - Yêu cầu không hợp lệ";
        }
        return statusCode + " - Sự cố hệ thống";
    }

    private String resolveMessage(int statusCode) {
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            return "Trang hoặc tài nguyên bạn yêu cầu hiện không tồn tại hoặc đã được thay đổi địa chỉ.";
        }
        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            return "Tài khoản của bạn không có đủ thẩm quyền để truy cập vào phân hệ này.";
        }
        if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            return "Vui lòng đăng nhập vào tài khoản UTELearn của bạn để tiếp tục thao tác.";
        }
        if (statusCode == HttpStatus.BAD_REQUEST.value()) {
            return "Dữ liệu được gửi lên máy chủ không đúng định dạng quy định.";
        }
        return "Máy chủ gặp sự cố ngoài dự kiến trong quá trình xử lý yêu cầu. Vui lòng thử lại sau.";
    }
}
