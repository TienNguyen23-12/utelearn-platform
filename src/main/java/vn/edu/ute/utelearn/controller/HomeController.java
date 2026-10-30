package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
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
    private final vn.edu.ute.utelearn.dao.CohortMemberRepository cohortMemberRepository;

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
        } else if ("instructor_only".equals(error)) {
            model.addAttribute("adminOnlyMessage",
                "Khu vực này chỉ dành riêng cho Giảng viên (INSTRUCTOR). Bạn đã được chuyển hướng an toàn về Trang Học tập & Khóa học!");
        } else if ("moderator_only".equals(error)) {
            model.addAttribute("adminOnlyMessage",
                "Khu vực này chỉ dành riêng cho Kiểm duyệt viên (MODERATOR). Bạn đã được chuyển hướng an toàn về Trang Học tập & Khóa học!");
        } else if ("access_denied".equals(error)) {
            model.addAttribute("adminOnlyMessage",
                "Bạn không có quyền truy cập vào khu vực này. Bạn đã được chuyển hướng an toàn về Trang Học tập & Khóa học!");
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        try {
            categories = jdbcTemplate.queryForList("SELECT id, name, slug FROM categories ORDER BY id ASC");
        } catch (Exception e) {
            log.warn("Chưa đọc được categories từ DB: {}", e.getMessage());
        }

        model.addAttribute("categories", categories);

        List<vn.edu.ute.utelearn.entity.Cohort> featuredCohorts = cohortService.getFeaturedCohorts(4);
        if (featuredCohorts == null || featuredCohorts.isEmpty()) {
            org.springframework.data.domain.Page<vn.edu.ute.utelearn.entity.Cohort> latestCohortsPage =
                    cohortService.searchPublicCohorts(null, null, org.springframework.data.domain.PageRequest.of(0, 4, org.springframework.data.domain.Sort.by("id").descending()));
            featuredCohorts = latestCohortsPage != null ? latestCohortsPage.getContent() : java.util.Collections.emptyList();
        }
        model.addAttribute("latestCohorts", featuredCohorts);

        return "home";
    }

    @GetMapping("/api/cohorts/featured")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFeaturedCohortsApi(
            @RequestParam(name = "timeframe", defaultValue = "all") String timeframe,
            @RequestParam(name = "limit", defaultValue = "4") int limit) {
        log.info("[HomeController] API lấy danh sách module nổi bật (timeframe: {}, limit: {})", timeframe, limit);
        List<vn.edu.ute.utelearn.entity.Cohort> featuredCohorts = cohortService.getFeaturedCohorts(limit, timeframe);
        if (featuredCohorts == null || featuredCohorts.isEmpty()) {
            org.springframework.data.domain.Page<vn.edu.ute.utelearn.entity.Cohort> latestCohortsPage =
                    cohortService.searchPublicCohorts(
                            null, null,
                            org.springframework.data.domain.PageRequest.of(0, limit, org.springframework.data.domain.Sort.by("id").descending())
                    );
            featuredCohorts = latestCohortsPage != null ? latestCohortsPage.getContent() : java.util.Collections.emptyList();
        }
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.US);
        symbols.setGroupingSeparator('.');
        java.text.DecimalFormat currencyFormatter = new java.text.DecimalFormat("#,###", symbols);
        List<Map<String, Object>> result = new ArrayList<>();
        for (vn.edu.ute.utelearn.entity.Cohort cohort : featuredCohorts) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", cohort.getId());
            item.put("name", cohort.getName());
            item.put("code", cohort.getCode());
            String thumb = (cohort.getThumbnailUrl() != null && !cohort.getThumbnailUrl().isBlank())
                    ? cohort.getThumbnailUrl()
                    : (cohort.getCourse() != null && cohort.getCourse().getThumbnailUrl() != null && !cohort.getCourse().getThumbnailUrl().isBlank())
                    ? cohort.getCourse().getThumbnailUrl()
                    : "/images/courses/default-cover.png";
            item.put("thumbnailUrl", thumb);
            item.put("courseTitle", cohort.getCourse() != null ? cohort.getCourse().getTitle() : "Module độc lập");
            item.put("courseSlug", (cohort.getCourse() != null && cohort.getCourse().getSlug() != null) ? cohort.getCourse().getSlug() : "standalone");
            item.put("isStandalone", cohort.getCourse() == null);
            item.put("categoryName", (cohort.getCourse() != null && cohort.getCourse().getCategory() != null) ? cohort.getCourse().getCategory().getName() : "Chuyên đề");
            item.put("instructorDisplayName", cohort.getInstructorDisplayName());
            long enrolled = (cohort.getEnrolledCount() != null ? cohort.getEnrolledCount() : (cohort.getCurrentEnrolled() != null ? cohort.getCurrentEnrolled().longValue() : 0L));
            item.put("enrolledCount", enrolled);
            item.put("averageRating", cohort.getAverageRating());
            item.put("ratingCount", cohort.getRatingCount() != null ? cohort.getRatingCount() : 0L);
            java.math.BigDecimal price = cohort.getPrice();
            item.put("price", price);
            if (price != null && price.compareTo(java.math.BigDecimal.ZERO) > 0) {
                item.put("formattedPrice", currencyFormatter.format(price) + " VNĐ");
            } else {
                item.put("formattedPrice", "Miễn phí");
            }
            result.add(item);
        }
        return ResponseEntity.ok(result);
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

    @GetMapping("/cohorts/create")
    public String redirectToCohortCreate() {
        return "redirect:/instructor/cohorts/create";
    }

    @GetMapping("/cohorts/{id}/edit")
    public String redirectToCohortEdit(@PathVariable Long id) {
        try {
            vn.edu.ute.utelearn.entity.Cohort cohort = cohortService.getCohortById(id);
            if (cohort != null && cohort.getCourse() != null) {
                return "redirect:/instructor/courses/" + cohort.getCourse().getId() + "/modules/" + id + "/edit";
            } else if (cohort != null) {
                return "redirect:/instructor/cohorts/" + id + "/edit";
            }
        } catch (Exception e) {
            log.warn("Không tìm thấy cohort id: {}", id);
        }
        return "redirect:/cohorts";
    }

    @GetMapping({"/cohorts/{id}", "/cohort/{id}", "/modules/{id}", "/module/{id}"})
    public String showCohortDetailRedirect(@PathVariable Long id) {
        try {
            vn.edu.ute.utelearn.entity.Cohort cohort = cohortService.getCohortById(id);
            if (cohort != null) {
                String courseKey = (cohort.getCourse() != null && cohort.getCourse().getSlug() != null && !cohort.getCourse().getSlug().isBlank())
                        ? cohort.getCourse().getSlug()
                        : "standalone";
                return "redirect:/courses/" + courseKey + "/modules/" + id;
            }
        } catch (Exception e) {
            log.warn("Không tìm thấy cohort id: {}", id);
        }
        return "redirect:/cohorts";
    }

    @GetMapping({"/cohorts/{id}/learn", "/cohort/{id}/learn", "/modules/{id}/learn", "/module/{id}/learn"})
    public String showCohortLearnRedirect(@PathVariable Long id) {
        try {
            vn.edu.ute.utelearn.entity.Cohort cohort = cohortService.getCohortById(id);
            if (cohort != null) {
                String courseKey = (cohort.getCourse() != null && cohort.getCourse().getSlug() != null && !cohort.getCourse().getSlug().isBlank())
                        ? cohort.getCourse().getSlug()
                        : "standalone";
                return "redirect:/courses/" + courseKey + "/modules/" + id + "/learn";
            }
        } catch (Exception e) {
            log.warn("Không tìm thấy cohort id: {}", id);
        }
        return "redirect:/cohorts";
    }

    @GetMapping("/learn")
    public String showGeneralLearnRedirect() {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();
        List<vn.edu.ute.utelearn.entity.CohortMember> memberships = cohortMemberRepository.findByUserOrderByEnrolledAtDesc(user);
        if (!memberships.isEmpty()) {
            vn.edu.ute.utelearn.entity.CohortMember latest = memberships.get(0);
            vn.edu.ute.utelearn.entity.Cohort cohort = latest.getCohort();
            if (cohort != null) {
                String courseKey = (cohort.getCourse() != null && cohort.getCourse().getSlug() != null && !cohort.getCourse().getSlug().isBlank())
                        ? cohort.getCourse().getSlug()
                        : "standalone";
                Long lastLessonId = latest.getLastLesson() != null ? latest.getLastLesson().getId() : null;
                if (lastLessonId != null) {
                    return "redirect:/courses/" + courseKey + "/modules/" + cohort.getId() + "/learn?lesson=" + lastLessonId;
                }
                return "redirect:/courses/" + courseKey + "/modules/" + cohort.getId() + "/learn";
            }
        }
        return "redirect:/courses/my-courses";
    }
}
