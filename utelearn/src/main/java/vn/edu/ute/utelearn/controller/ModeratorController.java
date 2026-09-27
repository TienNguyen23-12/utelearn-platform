package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AttendanceService;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.Optional;

@Controller
@RequestMapping("/moderator")
@RequiredArgsConstructor
public class ModeratorController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    @GetMapping("/attendance")
    public String getAttendancePage(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentAttendance", attendanceService.getCurrentAttendance(currentUser));
        model.addAttribute("attendanceHistory", attendanceService.getAttendanceHistory(currentUser));
        return "moderator/attendance";
    }

    @PostMapping("/attendance/check-in")
    public String checkIn(RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        try {
            attendanceService.checkIn(currentUserOpt.get());
            redirectAttributes.addFlashAttribute("success", "Điểm danh (Check-in) thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/attendance";
    }

    @PostMapping("/attendance/check-out")
    public String checkOut(RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        try {
            attendanceService.checkOut(currentUserOpt.get());
            redirectAttributes.addFlashAttribute("success", "Kết thúc ca trực (Check-out) thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/attendance";
    }
}
