package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.dto.CertificateViewDTO;
import vn.edu.ute.utelearn.service.CertificateService;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.service.AuthService authService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.config.UtelearnProperties properties;

    @GetMapping("/verify")
    public String verifyCertificate(
            @RequestParam(name = "code", required = false) String code,
            Model model) {

        if (authService != null) {
            authService.getCurrentAuthenticatedUser().ifPresent(u -> model.addAttribute("currentUser", u));
        }

        String baseUrl = (properties != null && properties.getRender() != null && properties.getRender().getKeepAliveUrl() != null && !properties.getRender().getKeepAliveUrl().isBlank())
                ? properties.getRender().getKeepAliveUrl().replaceAll("/+$", "")
                : "https://utelearn-platform.onrender.com";
        model.addAttribute("baseUrl", baseUrl);

        if (code == null || code.isBlank()) {
            model.addAttribute("hasSearched", false);
            model.addAttribute("notFound", false);
            model.addAttribute("isValid", false);
            model.addAttribute("requiresReview", false);
            return "certificates/verify";
        }

        String searchCode = code.trim();
        log.info("[CertificateController] Yêu cầu xác thực chứng chỉ với mã: {}", searchCode);

        model.addAttribute("hasSearched", true);
        model.addAttribute("searchedCode", searchCode);

        Optional<CertificateViewDTO> certOpt = certificateService.getCertificateByCode(searchCode);
        if (certOpt.isPresent()) {
            CertificateViewDTO cert = certOpt.get();
            model.addAttribute("cert", cert);
            model.addAttribute("isValid", cert.isValid());
            model.addAttribute("requiresReview", cert.isRequiresReview());
            model.addAttribute("notFound", false);
        } else {
            model.addAttribute("cert", null);
            model.addAttribute("isValid", false);
            model.addAttribute("requiresReview", false);
            model.addAttribute("notFound", true);
        }

        return "certificates/verify";
    }

    @PostMapping(value = "/api/review", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> submitReview(@RequestBody vn.edu.ute.utelearn.dto.CourseReviewRequestDTO req) {
        if (req == null || req.getRating() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số sao đánh giá không được để trống"));
        }

        Long userId = null;
        if (authService != null) {
            Optional<vn.edu.ute.utelearn.entity.User> uOpt = authService.getCurrentAuthenticatedUser();
            if (uOpt.isPresent()) {
                userId = uOpt.get().getId();
            }
        }

        try {
            CertificateViewDTO cert = certificateService.submitCourseReview(
                    userId, req.getCohortId(), req.getCourseId(), req.getCertCode(), req.getRating(), req.getComment());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đánh giá khóa học thành công! Giấy chứng nhận của bạn đã được kích hoạt.",
                    "data", cert
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[CertificateController] Lỗi khi lưu đánh giá khóa học: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Lỗi hệ thống khi lưu đánh giá"));
        }
    }

    @GetMapping("/{code}")
    public String viewCertificateByPath(@PathVariable("code") String code) {
        if (code == null || code.isBlank()) {
            return "redirect:/certificates/verify";
        }
        return "redirect:/certificates/verify?code=" + code.trim();
    }

    @GetMapping(value = "/api/verify", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> apiVerifyCertificate(@RequestParam(name = "code", required = false) String code) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mã chứng chỉ không được để trống"));
        }

        Optional<CertificateViewDTO> certOpt = certificateService.getCertificateByCode(code.trim());
        if (certOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "isValid", false,
                    "message", "Không tìm thấy chứng chỉ hoặc chứng chỉ không tồn tại trong hệ thống"
            ));
        }

        CertificateViewDTO cert = certOpt.get();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "isValid", cert.isValid(),
                "data", cert
        ));
    }
}
