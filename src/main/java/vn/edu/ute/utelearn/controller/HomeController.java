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
    private final vn.edu.ute.utelearn.service.CohortService cohortService;

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

        model.addAttribute("categories", categories);

        org.springframework.data.domain.Page<vn.edu.ute.utelearn.entity.Cohort> latestCohortsPage = cohortService.searchPublicCohorts(null, null, org.springframework.data.domain.PageRequest.of(0, 4, org.springframework.data.domain.Sort.by("id").descending()));
        model.addAttribute("latestCohorts", latestCohortsPage.getContent());

        return "home";
    }

    @GetMapping("/cohorts")
    public String showCohortsPage(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        currentUserOpt.ifPresent(user -> model.addAttribute("currentUser", user));
        
        List<Map<String, Object>> categories = new ArrayList<>();
        try {
            categories = jdbcTemplate.queryForList("SELECT id, name, slug FROM categories ORDER BY id ASC");
        } catch (Exception e) {
            log.warn("Chưa đọc được categories từ DB: {}", e.getMessage());
        }
        model.addAttribute("categories", categories);

        return "cohorts/index";
    }
}
