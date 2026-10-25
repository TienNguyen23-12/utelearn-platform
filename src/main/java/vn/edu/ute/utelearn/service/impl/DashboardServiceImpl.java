package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository;
import vn.edu.ute.utelearn.dao.OrderRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.service.DashboardService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final OrderRepository orderRepository;
    private final ModerationTaskRepository moderationTaskRepository;
    private final ModeratorAttendanceRepository moderatorAttendanceRepository;

    @Override
    public Map<String, Object> getDashboardStatistics(String filter) {
        Instant start = null;
        Instant end = null;
        LocalDate startD = null;
        LocalDate endD = null;
        ZonedDateTime now = ZonedDateTime.now();

        if ("day".equals(filter)) {
            start = now.toLocalDate().atStartOfDay(now.getZone()).toInstant();
            end = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone()).minusNanos(1).toInstant();
            startD = now.toLocalDate();
            endD = now.toLocalDate();
        } else if ("week".equals(filter)) {
            LocalDate startOfWeek = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            start = startOfWeek.atStartOfDay(now.getZone()).toInstant();
            end = startOfWeek.plusDays(7).atStartOfDay(now.getZone()).minusNanos(1).toInstant();
            startD = startOfWeek;
            endD = startOfWeek.plusDays(6);
        } else if ("month".equals(filter)) {
            LocalDate startOfMonth = now.toLocalDate().withDayOfMonth(1);
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
        long escalatedModerationTasks;
        long totalAttendances;

        List<String> studentRoles = Arrays.asList("STUDENT", "LEARNER", "USER");

        if (start != null && end != null) {
            totalRevenue = orderRepository.calculateTotalRevenueBetween(start, end);
            totalUsers = userRepository.count();
            totalStudents = userRepository.countByRolesCodeInAndCreatedAtBetween(studentRoles, start, end);
            totalCourses = courseRepository.countByCreatedAtBetween(start, end);
            pendingModerationTasks = moderationTaskRepository.countByStatusAndCreatedAtBetween("PENDING", start, end);
            escalatedModerationTasks = moderationTaskRepository.countByStatusAndCreatedAtBetween("ESCALATED", start, end);
            totalAttendances = moderatorAttendanceRepository.countByWorkDateBetween(startD, endD);
        } else {
            totalRevenue = orderRepository.calculateTotalRevenue();
            totalUsers = userRepository.count();
            totalStudents = userRepository.countByRolesCodeIn(studentRoles);
            totalCourses = courseRepository.count();
            pendingModerationTasks = moderationTaskRepository.countByStatus("PENDING");
            escalatedModerationTasks = moderationTaskRepository.countByStatus("ESCALATED");
            totalAttendances = moderatorAttendanceRepository.count();
        }

        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalStudents", totalStudents);
        stats.put("totalCourses", totalCourses);
        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingModerationTasks", pendingModerationTasks);
        stats.put("escalatedModerationTasks", escalatedModerationTasks);
        stats.put("totalAttendances", totalAttendances);
        stats.put("currentFilter", filter);

        return stats;
    }
}
