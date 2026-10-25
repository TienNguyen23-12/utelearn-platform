package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.DashboardService;

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
            boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
            return isAdmin ? "redirect:/dashboard" : "redirect:/home";
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
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
        if (!isAdmin) {
            return "redirect:/home?error=admin_only";
        }

        Map<String, Object> stats = dashboardService.getDashboardStatistics(filter);
        model.addAttribute("currentUser", user);
        model.addAllAttributes(stats);

        return "dashboard/index";
    }
}
