package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dto.PayrollItemDTO;
import vn.edu.ute.utelearn.entity.PayrollConfig;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.PayrollService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PayrollController {

    private static final ZoneId HCM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PayrollService payrollService;
    private final AuthService authService;

    @GetMapping("/admin/payroll")
    public String showAdminPayroll(
            @RequestParam(required = false, defaultValue = "ALL") String period,
            @RequestParam(required = false, defaultValue = "INSTRUCTOR") String tab,
            Model model) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();
        if (!SecurityUtils.isAdminOrFinance(currentUser)) {
            return "redirect:/home?error=access_denied";
        }

        String normalizedPeriod = (period == null || period.isBlank()) ? "ALL" : period.trim();
        String normalizedTab = "MODERATOR".equalsIgnoreCase(tab) ? "MODERATOR" : "INSTRUCTOR";

        PayrollConfig globalConfig = payrollService.getGlobalConfig();
        List<PayrollItemDTO> instructorPayrolls = payrollService.calculateInstructorPayroll(normalizedPeriod);
        List<PayrollItemDTO> moderatorPayrolls = payrollService.calculateModeratorPayroll(normalizedPeriod);

        BigDecimal totalGrossRevenue = instructorPayrolls.stream()
                .map(PayrollItemDTO::getGrossRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInstructorPayout = instructorPayrolls.stream()
                .map(PayrollItemDTO::getTotalNetPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalModeratorPayout = moderatorPayrolls.stream()
                .map(PayrollItemDTO::getTotalNetPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPayrollBudget = totalInstructorPayout.add(totalModeratorPayout);

        long customConfigCount = instructorPayrolls.stream().filter(PayrollItemDTO::isHasCustomConfig).count()
                + moderatorPayrolls.stream().filter(PayrollItemDTO::isHasCustomConfig).count();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("globalConfig", globalConfig);
        model.addAttribute("instructorPayrolls", instructorPayrolls);
        model.addAttribute("moderatorPayrolls", moderatorPayrolls);
        model.addAttribute("totalGrossRevenue", totalGrossRevenue);
        model.addAttribute("totalInstructorPayout", totalInstructorPayout);
        model.addAttribute("totalModeratorPayout", totalModeratorPayout);
        model.addAttribute("totalPayrollBudget", totalPayrollBudget);
        model.addAttribute("customConfigCount", customConfigCount);
        model.addAttribute("currentPeriod", normalizedPeriod);
        model.addAttribute("activeTab", normalizedTab);
        model.addAttribute("availableMonths", buildRecentMonths(6));

        return "admin/payroll/index";
    }

    @PostMapping("/admin/payroll/global-config")
    public String updateGlobalConfig(
            @RequestParam BigDecimal instructorSharePercent,
            @RequestParam BigDecimal instructorRatingBonusPercent,
            @RequestParam BigDecimal instructorRatingThreshold,
            @RequestParam BigDecimal moderatorMinuteRate,
            @RequestParam BigDecimal moderatorOtMultiplier,
            @RequestParam BigDecimal moderatorTaskBonus,
            @RequestParam BigDecimal moderatorSlaPenalty,
            @RequestParam(required = false, defaultValue = "0") BigDecimal fixedAllowance,
            @RequestParam(required = false, defaultValue = "ALL") String period,
            @RequestParam(required = false, defaultValue = "INSTRUCTOR") String tab,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty() || !SecurityUtils.isAdminOrFinance(currentUserOpt.get())) {
            return "redirect:/login";
        }

        try {
            payrollService.updateGlobalConfig(
                    instructorSharePercent,
                    instructorRatingBonusPercent,
                    instructorRatingThreshold,
                    moderatorMinuteRate,
                    moderatorOtMultiplier,
                    moderatorTaskBonus,
                    moderatorSlaPenalty,
                    fixedAllowance);
            log.info("[PayrollController] Admin {} đã cập nhật cấu hình công thức lương mặc định",
                    currentUserOpt.get().getUsername());
            redirectAttributes.addFlashAttribute("success", "Đã lưu cấu hình công thức & tỷ lệ lương mặc định cho toàn hệ thống!");
        } catch (Exception e) {
            log.error("[PayrollController] Lỗi khi lưu cấu hình lương mặc định: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Không thể lưu cấu hình: " + e.getMessage());
        }

        return "redirect:/admin/payroll?period=" + period + "&tab=" + tab;
    }

    @PostMapping("/admin/payroll/user-config")
    public String saveUserConfig(
            @RequestParam Long userId,
            @RequestParam(required = false) BigDecimal instructorSharePercent,
            @RequestParam(required = false) BigDecimal instructorRatingBonusPercent,
            @RequestParam(required = false) BigDecimal instructorRatingThreshold,
            @RequestParam(required = false) BigDecimal moderatorMinuteRate,
            @RequestParam(required = false) BigDecimal moderatorOtMultiplier,
            @RequestParam(required = false) BigDecimal moderatorTaskBonus,
            @RequestParam(required = false) BigDecimal moderatorSlaPenalty,
            @RequestParam(required = false) BigDecimal fixedAllowance,
            @RequestParam(required = false) String note,
            @RequestParam(required = false, defaultValue = "ALL") String period,
            @RequestParam(required = false, defaultValue = "INSTRUCTOR") String tab,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty() || !SecurityUtils.isAdminOrFinance(currentUserOpt.get())) {
            return "redirect:/login";
        }

        try {
            payrollService.saveUserOverrideConfig(
                    userId,
                    instructorSharePercent,
                    instructorRatingBonusPercent,
                    instructorRatingThreshold,
                    moderatorMinuteRate,
                    moderatorOtMultiplier,
                    moderatorTaskBonus,
                    moderatorSlaPenalty,
                    fixedAllowance,
                    note);
            log.info("[PayrollController] Admin {} đã cập nhật cấu hình lương cá nhân cho userId={}",
                    currentUserOpt.get().getUsername(), userId);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật mức tính lương cá nhân thành công!");
        } catch (Exception e) {
            log.error("[PayrollController] Lỗi khi cập nhật lương cá nhân cho userId {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật: " + e.getMessage());
        }

        return "redirect:/admin/payroll?period=" + period + "&tab=" + tab;
    }

    @PostMapping("/admin/payroll/user-config/{userId}/reset")
    public String resetUserConfig(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "ALL") String period,
            @RequestParam(required = false, defaultValue = "INSTRUCTOR") String tab,
            RedirectAttributes redirectAttributes) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty() || !SecurityUtils.isAdminOrFinance(currentUserOpt.get())) {
            return "redirect:/login";
        }

        try {
            payrollService.resetUserConfigToDefault(userId);
            log.info("[PayrollController] Admin {} đã khôi phục mức lương mặc định cho userId={}",
                    currentUserOpt.get().getUsername(), userId);
            redirectAttributes.addFlashAttribute("success", "Đã khôi phục về công thức & tỷ lệ mặc định của hệ thống!");
        } catch (Exception e) {
            log.error("[PayrollController] Lỗi khi khôi phục cấu hình mặc định cho userId {}: {}", userId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Không thể khôi phục: " + e.getMessage());
        }

        return "redirect:/admin/payroll?period=" + period + "&tab=" + tab;
    }

    @GetMapping("/payroll/my-income")
    public String showMyIncome(
            @RequestParam(required = false, defaultValue = "ALL") String period,
            Model model) {

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();
        boolean isInstructor = SecurityUtils.isInstructor(currentUser);
        boolean isModerator = SecurityUtils.isModerator(currentUser);
        boolean isAdmin = SecurityUtils.isAdmin(currentUser);

        if (!isInstructor && !isModerator && !isAdmin) {
            return "redirect:/home";
        }

        String normalizedPeriod = (period == null || period.isBlank()) ? "ALL" : period.trim();
        PayrollConfig globalConfig = payrollService.getGlobalConfig();

        Optional<PayrollItemDTO> instructorIncome = (isInstructor || isAdmin)
                ? payrollService.getInstructorPayrollForUser(currentUser.getId(), normalizedPeriod)
                : Optional.empty();
        Optional<PayrollItemDTO> moderatorIncome = (isModerator || isAdmin)
                ? payrollService.getModeratorPayrollForUser(currentUser.getId(), normalizedPeriod)
                : Optional.empty();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("globalConfig", globalConfig);
        model.addAttribute("instructorIncome", instructorIncome.orElse(null));
        model.addAttribute("moderatorIncome", moderatorIncome.orElse(null));
        model.addAttribute("currentPeriod", normalizedPeriod);
        model.addAttribute("availableMonths", buildRecentMonths(6));

        return "payroll/my-income";
    }

    private List<String> buildRecentMonths(int count) {
        List<String> months = new ArrayList<>();
        YearMonth current = YearMonth.now(HCM_ZONE);
        for (int i = 0; i < count; i++) {
            months.add(current.minusMonths(i).toString());
        }
        return months;
    }
}

