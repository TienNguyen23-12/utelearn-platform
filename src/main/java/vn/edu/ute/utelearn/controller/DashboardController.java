package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.Optional;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AuthService authService;
    private final vn.edu.ute.utelearn.dao.UserRepository userRepository;
    private final vn.edu.ute.utelearn.dao.RoleRepository roleRepository;
    private final vn.edu.ute.utelearn.dao.PermissionRepository permissionRepository;
    private final vn.edu.ute.utelearn.dao.CourseRepository courseRepository;
    private final vn.edu.ute.utelearn.dao.OrderRepository orderRepository;
    private final vn.edu.ute.utelearn.dao.ModerationTaskRepository moderationTaskRepository;
    private final vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository moderatorAttendanceRepository;

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
    public String showDashboard(Model model, @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "all") String filter) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login?error=unauthorized";
        }

        User user = currentUserOpt.get();
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()));
        if (!isAdmin) {
            return "redirect:/home?error=admin_only";
        }

        // Calculate time ranges
        java.time.Instant start = null;
        java.time.Instant end = null;
        java.time.LocalDate startD = null;
        java.time.LocalDate endD = null;
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();

        if ("day".equals(filter)) {
            start = now.toLocalDate().atStartOfDay(now.getZone()).toInstant();
            end = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone()).minusNanos(1).toInstant();
            startD = now.toLocalDate();
            endD = now.toLocalDate();
        } else if ("week".equals(filter)) {
            java.time.LocalDate startOfWeek = now.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            start = startOfWeek.atStartOfDay(now.getZone()).toInstant();
            end = startOfWeek.plusDays(7).atStartOfDay(now.getZone()).minusNanos(1).toInstant();
            startD = startOfWeek;
            endD = startOfWeek.plusDays(6);
        } else if ("month".equals(filter)) {
            java.time.LocalDate startOfMonth = now.toLocalDate().withDayOfMonth(1);
            start = startOfMonth.atStartOfDay(now.getZone()).toInstant();
            end = startOfMonth.plusMonths(1).atStartOfDay(now.getZone()).minusNanos(1).toInstant();
            startD = startOfMonth;
            endD = startOfMonth.plusMonths(1).minusDays(1);
        }

        BigDecimal totalRevenue;
        long totalUsers;
        long totalStudents;
        long totalCourses;
        long pendingModerationTasks;
        long totalAttendances;

        java.util.List<String> studentRoles = java.util.Arrays.asList("STUDENT", "LEARNER", "USER");

        if (start != null && end != null) {
            totalRevenue = orderRepository.calculateTotalRevenueBetween(start, end);
            totalUsers = userRepository.count(); // Users usually aren't filtered by date for overall dashboard stats, or maybe they are? The user says "số học viên". I'll filter it.
            // Wait, let's keep total users as total all time if we don't have countByCreatedAtBetween, but I didn't add countByCreatedAtBetween to UserRepository. I'll just keep totalUsers as all time, but totalStudents filtered.
            totalStudents = userRepository.countByRolesCodeInAndCreatedAtBetween(studentRoles, start, end);
            totalCourses = courseRepository.countByCreatedAtBetween(start, end);
            pendingModerationTasks = moderationTaskRepository.countByStatusAndCreatedAtBetween("PENDING", start, end);
            totalAttendances = moderatorAttendanceRepository.countByWorkDateBetween(startD, endD);
        } else {
            totalRevenue = orderRepository.calculateTotalRevenue();
            totalUsers = userRepository.count();
            totalStudents = userRepository.countByRolesCodeIn(studentRoles);
            totalCourses = courseRepository.count();
            pendingModerationTasks = moderationTaskRepository.countByStatus("PENDING");
            totalAttendances = moderatorAttendanceRepository.count();
        }

        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalCourses", totalCourses);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pendingModerationTasks", pendingModerationTasks);
        model.addAttribute("totalAttendances", totalAttendances);
        model.addAttribute("currentFilter", filter);
        
        return "dashboard/index";
    }
}
