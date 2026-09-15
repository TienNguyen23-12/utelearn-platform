package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AuthService authService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/home")
    public String showHomePage(
        @RequestParam(value = "error", required = false) String error,
        Model model
    ) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        currentUserOpt.ifPresent(user -> model.addAttribute("currentUser", user));

        if ("admin_only".equals(error)) {
            model.addAttribute("adminOnlyMessage", 
                "Khu vực Quản trị hệ thống (/dashboard, /admin) chỉ dành riêng cho Quản trị viên (ADMIN). Bạn đã được chuyển hướng an toàn về Trang Học tập & Khóa học!");
        }

        // Lấy danh mục từ cơ sở dữ liệu (nếu bảng tồn tại)
        List<Map<String, Object>> categories = new ArrayList<>();
        try {
            categories = jdbcTemplate.queryForList("SELECT id, name, slug FROM categories ORDER BY id ASC");
        } catch (Exception e) {
            log.warn("Chưa đọc được categories từ DB: {}", e.getMessage());
        }

        // Dữ liệu khóa học mẫu theo đợt (Cohorts)
        List<Map<String, Object>> cohortCourses = List.of(
            Map.of(
                "title", "Lập trình Backend chuyên sâu với Java & Spring Boot 3",
                "category", "Lập trình Web & Backend",
                "instructor", "TS. Nguyễn Văn A (Khoa CNTT)",
                "cohortName", "Đợt K24 - Kỳ Thu",
                "duration", "12 Tuần",
                "lessons", 45,
                "rating", "4.9",
                "badgeColor", "primary",
                "status", "Đang mở đăng ký",
                "image", "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&auto=format&fit=crop&q=80"
            ),
            Map.of(
                "title", "Lập trình Fullstack React, Next.js 14 & UI/UX Design System",
                "category", "Lập trình Web & Backend",
                "instructor", "ThS. Trần Thị B",
                "cohortName", "Đợt K24 - Khóa Cấp tốc",
                "duration", "10 Tuần",
                "lessons", 38,
                "rating", "4.8",
                "badgeColor", "info",
                "status", "Đang diễn ra",
                "image", "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600&auto=format&fit=crop&q=80"
            ),
            Map.of(
                "title", "Cấu trúc Dữ liệu & Giải thuật Nâng cao (Chấm Online Judge)",
                "category", "Luyện thi Thuật toán & OJ",
                "instructor", "Ban Huấn luyện Olympic HCMUTE",
                "cohortName", "Đợt K24 - Tuyển chọn",
                "duration", "8 Tuần",
                "lessons", 60,
                "rating", "5.0",
                "badgeColor", "warning",
                "status", "Thi đấu trực tuyến",
                "image", "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80"
            ),
            Map.of(
                "title", "Khoa học Dữ liệu & Ứng dụng Học máy Thực chiến (Machine Learning)",
                "category", "Khoa học Dữ liệu & AI",
                "instructor", "TS. Lê Hoàng C",
                "cohortName", "Đợt K24 - AI Specialist",
                "duration", "14 Tuần",
                "lessons", 52,
                "rating", "4.9",
                "badgeColor", "success",
                "status", "Đang mở đăng ký",
                "image", "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=600&auto=format&fit=crop&q=80"
            )
        );

        model.addAttribute("categories", categories);
        model.addAttribute("cohortCourses", cohortCourses);

        return "home";
    }
}
