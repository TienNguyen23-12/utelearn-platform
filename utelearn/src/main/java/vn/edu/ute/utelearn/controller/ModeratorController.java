package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AttendanceService;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/moderator")
@RequiredArgsConstructor
public class ModeratorController {

    private final ModerationTaskRepository taskRepository;
    private final CourseService courseService;
    private final AttendanceService attendanceService;
    private final AuthService authService;

    @GetMapping("/queue")
    @PreAuthorize("@rbac.hasRole('MODERATOR') or @rbac.hasRole('ADMIN')")
    public String viewQueue(Model model) {
        // Find all pending tasks
        List<ModerationTask> tasks = taskRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        model.addAttribute("tasks", tasks);
        return "moderator/queue";
    }

    @GetMapping("/review/{taskId}")
    @PreAuthorize("@rbac.hasRole('MODERATOR') or @rbac.hasRole('ADMIN')")
    public String reviewTask(@PathVariable Long taskId, Model model) {
        ModerationTask task = taskRepository.findById(taskId).orElseThrow();
        model.addAttribute("task", task);
        
        if ("COURSE".equals(task.getItemType())) {
            model.addAttribute("course", courseService.getCourseById(task.getItemId()));
            return "moderator/review_course";
        }
        
        return "redirect:/moderator/queue";
    }

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
