package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AuthService authService;
    private final vn.edu.ute.utelearn.dao.UserRepository userRepository;
    private final vn.edu.ute.utelearn.dao.RoleRepository roleRepository;
    private final vn.edu.ute.utelearn.dao.PermissionRepository permissionRepository;

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
    public String showDashboard(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login?error=unauthorized";
        }

        User user = currentUserOpt.get();
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
        if (!isAdmin) {
            return "redirect:/home?error=admin_only";
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalRoles", roleRepository.count());
        model.addAttribute("totalPermissions", permissionRepository.count());
        model.addAttribute("recentUsers", userRepository.findAll());
        return "dashboard/index";
    }
}
