package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.DashboardService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AuthService authService;
    private final DashboardService dashboardService;

    @GetMapping("/")
    public String index() {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            boolean hasStaffAccess = SecurityUtils.isAdmin(user) || SecurityUtils.hasAnyPermission(user, "USER_MANAGE", "ROLE_MANAGE", "CONTENT_APPROVE", "COURSE_CREATE", "ORDER_MANAGE", "ASSET_MANAGE");
            return hasStaffAccess ? "redirect:/dashboard" : "redirect:/home";
        }
        return "redirect:/home";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model, @RequestParam(required = false, defaultValue = "all") String filter) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login?error=unauthorized";
        }

        User user = currentUserOpt.get();
        boolean hasStaffAccess = SecurityUtils.isAdmin(user) || SecurityUtils.hasAnyPermission(user, "USER_MANAGE", "ROLE_MANAGE", "CONTENT_APPROVE", "COURSE_CREATE", "ORDER_MANAGE", "ASSET_MANAGE");
        if (!hasStaffAccess) {
            return "redirect:/home?error=access_denied";
        }

        Map<String, Object> stats = dashboardService.getDashboardStatistics(filter);
        model.addAttribute("currentUser", user);
        model.addAllAttributes(stats);

        return "dashboard/index";
    }
}
