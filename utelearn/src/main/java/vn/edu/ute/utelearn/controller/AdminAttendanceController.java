package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AttendanceService;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.Optional;

@Controller
@RequestMapping("/admin/attendance")
@RequiredArgsConstructor
public class AdminAttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    @GetMapping
    public String getAllAttendancesPage(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
        if (!isAdmin) {
            return "redirect:/home?error=admin_only";
        }
        
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("attendances", attendanceService.getAllAttendances());
        return "admin/attendance/index";
    }
}
