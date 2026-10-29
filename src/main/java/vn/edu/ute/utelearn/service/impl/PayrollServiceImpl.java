package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository;
import vn.edu.ute.utelearn.dao.PayrollConfigRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.PayrollItemDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.PayrollConfig;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.PayrollService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollServiceImpl implements PayrollService {

    private static final ZoneId HCM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PayrollConfigRepository payrollConfigRepository;
    private final UserRepository userRepository;
    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final ModeratorAttendanceRepository moderatorAttendanceRepository;
    private final ModerationTaskRepository moderationTaskRepository;

    @Override
    public PayrollConfig getGlobalConfig() {
        return payrollConfigRepository.findFirstByIsGlobalTrue()
                .orElseGet(() -> {
                    log.info("[PayrollService] Khởi tạo cấu hình lương mặc định toàn hệ thống");
                    return payrollConfigRepository.save(PayrollConfig.createDefaultGlobalConfig());
                });
    }

    @Override
    public PayrollConfig updateGlobalConfig(
            BigDecimal instructorSharePercent,
            BigDecimal instructorRatingBonusPercent,
            BigDecimal instructorRatingThreshold,
            BigDecimal moderatorMinuteRate,
            BigDecimal moderatorOtMultiplier,
            BigDecimal moderatorTaskBonus,
            BigDecimal moderatorSlaPenalty,
            BigDecimal fixedAllowance) {

        PayrollConfig global = getGlobalConfig();
        global.setInstructorSharePercent(clampPercent(instructorSharePercent, new BigDecimal("70.00")));
        global.setInstructorRatingBonusPercent(clampPercent(instructorRatingBonusPercent, new BigDecimal("5.00")));
        global.setInstructorRatingThreshold(clampThreshold(instructorRatingThreshold, new BigDecimal("4.50")));
        global.setModeratorMinuteRate(sanitizeNonNegative(moderatorMinuteRate, new BigDecimal("500")));
        global.setModeratorOtMultiplier(sanitizeNonNegative(moderatorOtMultiplier, new BigDecimal("1.50")));
        global.setModeratorTaskBonus(sanitizeNonNegative(moderatorTaskBonus, new BigDecimal("10000")));
        global.setModeratorSlaPenalty(sanitizeNonNegative(moderatorSlaPenalty, new BigDecimal("15000")));
        global.setFixedAllowance(sanitizeNonNegative(fixedAllowance, BigDecimal.ZERO));

        PayrollConfig saved = payrollConfigRepository.save(global);
        log.info("[PayrollService] Đã cập nhật cấu hình lương mặc định toàn hệ thống: share={}%, minuteRate={}",
                saved.getInstructorSharePercent(), saved.getModeratorMinuteRate());
        return saved;
    }

    @Override
    public PayrollConfig saveUserOverrideConfig(
            Long userId,
            BigDecimal instructorSharePercent,
            BigDecimal instructorRatingBonusPercent,
            BigDecimal instructorRatingThreshold,
            BigDecimal moderatorMinuteRate,
            BigDecimal moderatorOtMultiplier,
            BigDecimal moderatorTaskBonus,
            BigDecimal moderatorSlaPenalty,
            BigDecimal fixedAllowance,
            String note) {

        if (userId == null) {
            throw new IllegalArgumentException("Không xác định được người dùng cần cấu hình lương.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        PayrollConfig userConfig = payrollConfigRepository.findByUser_Id(userId)
                .orElseGet(() -> PayrollConfig.builder()
                        .user(user)
                        .isGlobal(false)
                        .build());

        userConfig.setInstructorSharePercent(instructorSharePercent != null ? clampPercent(instructorSharePercent, null) : null);
        userConfig.setInstructorRatingBonusPercent(instructorRatingBonusPercent != null ? clampPercent(instructorRatingBonusPercent, null) : null);
        userConfig.setInstructorRatingThreshold(instructorRatingThreshold != null ? clampThreshold(instructorRatingThreshold, null) : null);
        userConfig.setModeratorMinuteRate(moderatorMinuteRate != null ? sanitizeNonNegative(moderatorMinuteRate, null) : null);
        userConfig.setModeratorOtMultiplier(moderatorOtMultiplier != null ? sanitizeNonNegative(moderatorOtMultiplier, null) : null);
        userConfig.setModeratorTaskBonus(moderatorTaskBonus != null ? sanitizeNonNegative(moderatorTaskBonus, null) : null);
        userConfig.setModeratorSlaPenalty(moderatorSlaPenalty != null ? sanitizeNonNegative(moderatorSlaPenalty, null) : null);
        userConfig.setFixedAllowance(fixedAllowance != null ? sanitizeNonNegative(fixedAllowance, BigDecimal.ZERO) : null);
        userConfig.setNote(note != null ? note.trim() : null);

        PayrollConfig saved = payrollConfigRepository.save(userConfig);
        log.info("[PayrollService] Đã lưu cấu hình lương cá nhân cho user {} (id={})", user.getUsername(), userId);
        return saved;
    }

    @Override
    public void resetUserConfigToDefault(Long userId) {
        if (userId == null) {
            return;
        }
        payrollConfigRepository.findByUser_Id(userId).ifPresent(cfg -> {
            payrollConfigRepository.delete(cfg);
            log.info("[PayrollService] Đã khôi phục cấu hình lương mặc định cho userId: {}", userId);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollItemDTO> calculateInstructorPayroll(String period) {
        PayrollConfig global = payrollConfigRepository.findFirstByIsGlobalTrue()
                .orElseGet(PayrollConfig::createDefaultGlobalConfig);
        Map<Long, PayrollConfig> customConfigs = loadCustomConfigMap();
        YearMonth targetMonth = parseYearMonth(period);

        List<User> allUsers = userRepository.findAll();
        Map<Long, User> instructorMap = new LinkedHashMap<>();
        for (User u : allUsers) {
            if (SecurityUtils.isInstructor(u)) {
                instructorMap.put(u.getId(), u);
            }
        }

        List<CohortMember> allEnrollments = cohortMemberRepository.findAllStudentEnrollmentsWithCohortAndInstructor();
        Map<Long, Set<Long>> instructorCohortIds = new HashMap<>();
        for (Cohort c : cohortRepository.findAll()) {
            if (c == null || c.isFullCourseCohort()) {
                continue;
            }
            User owner = resolveCohortInstructor(c);
            if (owner != null && owner.getId() != null) {
                instructorMap.putIfAbsent(owner.getId(), owner);
                instructorCohortIds.computeIfAbsent(owner.getId(), k -> new HashSet<>()).add(c.getId());
            }
        }
        Map<Long, Long> instructorStudentCount = new HashMap<>();
        Map<Long, BigDecimal> instructorGrossRevenue = new HashMap<>();
        Map<Long, List<Integer>> instructorRatings = new HashMap<>();

        for (CohortMember cm : allEnrollments) {
            Cohort cohort = cm.getCohort();
            if (cohort == null || cohort.isFullCourseCohort()) {
                continue;
            }
            User owner = resolveCohortInstructor(cohort);
            if (owner == null || owner.getId() == null) {
                continue;
            }
            Long instructorId = owner.getId();
            instructorMap.putIfAbsent(instructorId, owner);
            instructorCohortIds.computeIfAbsent(instructorId, k -> new HashSet<>()).add(cohort.getId());

            if (cm.getRating() != null && cm.getRating() >= 1 && cm.getRating() <= 5) {
                instructorRatings.computeIfAbsent(instructorId, k -> new ArrayList<>()).add(cm.getRating());
            }

            if (!isInstantInPeriod(cm.getEnrolledAt(), targetMonth)) {
                continue;
            }

            instructorStudentCount.merge(instructorId, 1L, Long::sum);
            BigDecimal unitPrice = resolveCohortPrice(cohort);
            instructorGrossRevenue.merge(instructorId, unitPrice, BigDecimal::add);
        }

        List<PayrollItemDTO> results = new ArrayList<>();
        for (User instructor : instructorMap.values()) {
            Long uid = instructor.getId();
            PayrollConfig custom = customConfigs.get(uid);
            PayrollItemDTO item = buildInstructorPayrollItem(
                    instructor,
                    global,
                    custom,
                    instructorCohortIds.getOrDefault(uid, Set.of()).size(),
                    instructorStudentCount.getOrDefault(uid, 0L),
                    instructorGrossRevenue.getOrDefault(uid, BigDecimal.ZERO),
                    instructorRatings.getOrDefault(uid, List.of()));
            results.add(item);
        }

        results.sort(Comparator.comparing(PayrollItemDTO::getTotalNetPay).reversed()
                .thenComparing(PayrollItemDTO::getUserId));
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollItemDTO> calculateModeratorPayroll(String period) {
        PayrollConfig global = payrollConfigRepository.findFirstByIsGlobalTrue()
                .orElseGet(PayrollConfig::createDefaultGlobalConfig);
        Map<Long, PayrollConfig> customConfigs = loadCustomConfigMap();
        YearMonth targetMonth = parseYearMonth(period);

        List<User> allUsers = userRepository.findAll();
        Map<Long, User> moderatorMap = new LinkedHashMap<>();
        for (User u : allUsers) {
            if (SecurityUtils.isModerator(u)) {
                moderatorMap.put(u.getId(), u);
            }
        }

        List<ModeratorAttendance> allAttendances = moderatorAttendanceRepository.findAllByOrderByWorkDateDesc();
        Map<Long, Integer> workDaysMap = new HashMap<>();
        Map<Long, Integer> regularMinMap = new HashMap<>();
        Map<Long, Integer> otMinMap = new HashMap<>();
        Map<Long, Long> attendanceCompletedTasksMap = new HashMap<>();

        for (ModeratorAttendance att : allAttendances) {
            User mod = att.getModerator();
            if (mod == null || mod.getId() == null) {
                continue;
            }
            Long modId = mod.getId();
            moderatorMap.putIfAbsent(modId, mod);

            if (!isLocalDateInPeriod(att.getWorkDate(), targetMonth)) {
                continue;
            }

            workDaysMap.merge(modId, 1, Integer::sum);
            int otMin = att.getOtWorkloadMinutes() != null ? Math.max(0, att.getOtWorkloadMinutes()) : 0;
            int totalMin = att.getTotalWorkloadMinutes() != null ? Math.max(0, att.getTotalWorkloadMinutes()) : 0;
            int regMin = Math.max(0, totalMin - otMin);

            regularMinMap.merge(modId, regMin, Integer::sum);
            otMinMap.merge(modId, otMin, Integer::sum);

            long completed = att.getCompletedTasksCount() != null ? Math.max(0, att.getCompletedTasksCount()) : 0;
            attendanceCompletedTasksMap.merge(modId, completed, Long::sum);
        }

        List<ModerationTask> allTasks = moderationTaskRepository.findAllWithAssignees();
        Map<Long, Long> taskCompletedMap = new HashMap<>();
        Map<Long, Long> taskEscalatedMap = new HashMap<>();

        for (ModerationTask task : allTasks) {
            Instant taskTime = task.getAssignedAt() != null ? task.getAssignedAt() : task.getCreatedAt();
            if (!isInstantInPeriod(taskTime, targetMonth)) {
                continue;
            }
            if (task.getAssignedTo() != null && task.getAssignedTo().getId() != null) {
                Long assignedId = task.getAssignedTo().getId();
                if ("APPROVED".equalsIgnoreCase(task.getStatus()) || "REJECTED".equalsIgnoreCase(task.getStatus())) {
                    taskCompletedMap.merge(assignedId, 1L, Long::sum);
                }
            }
            if (task.getEscalatedBy() != null && task.getEscalatedBy().getId() != null) {
                taskEscalatedMap.merge(task.getEscalatedBy().getId(), 1L, Long::sum);
            }
        }

        List<PayrollItemDTO> results = new ArrayList<>();
        for (User moderator : moderatorMap.values()) {
            Long uid = moderator.getId();
            PayrollConfig custom = customConfigs.get(uid);
            long completedTasks = Math.max(
                    attendanceCompletedTasksMap.getOrDefault(uid, 0L),
                    taskCompletedMap.getOrDefault(uid, 0L));
            long escalatedTasks = taskEscalatedMap.getOrDefault(uid, 0L);

            PayrollItemDTO item = buildModeratorPayrollItem(
                    moderator,
                    global,
                    custom,
                    workDaysMap.getOrDefault(uid, 0),
                    regularMinMap.getOrDefault(uid, 0),
                    otMinMap.getOrDefault(uid, 0),
                    completedTasks,
                    escalatedTasks);
            results.add(item);
        }

        results.sort(Comparator.comparing(PayrollItemDTO::getTotalNetPay).reversed()
                .thenComparing(PayrollItemDTO::getUserId));
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PayrollItemDTO> getInstructorPayrollForUser(Long userId, String period) {
        if (userId == null) {
            return Optional.empty();
        }
        return calculateInstructorPayroll(period).stream()
                .filter(item -> userId.equals(item.getUserId()))
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PayrollItemDTO> getModeratorPayrollForUser(Long userId, String period) {
        if (userId == null) {
            return Optional.empty();
        }
        return calculateModeratorPayroll(period).stream()
                .filter(item -> userId.equals(item.getUserId()))
                .findFirst();
    }

    private PayrollItemDTO buildInstructorPayrollItem(
            User instructor,
            PayrollConfig global,
            PayrollConfig custom,
            int cohortCount,
            long studentCount,
            BigDecimal grossRevenue,
            List<Integer> ratings) {

        BigDecimal effShare = resolveValue(custom != null ? custom.getInstructorSharePercent() : null,
                global.getInstructorSharePercent(), new BigDecimal("70.00"));
        BigDecimal effBonusPct = resolveValue(custom != null ? custom.getInstructorRatingBonusPercent() : null,
                global.getInstructorRatingBonusPercent(), new BigDecimal("5.00"));
        BigDecimal effThreshold = resolveValue(custom != null ? custom.getInstructorRatingThreshold() : null,
                global.getInstructorRatingThreshold(), new BigDecimal("4.50"));
        BigDecimal effAllowance = resolveValue(custom != null ? custom.getFixedAllowance() : null,
                global.getFixedAllowance(), BigDecimal.ZERO);

        Double avgRating = null;
        if (!ratings.isEmpty()) {
            double sum = ratings.stream().mapToInt(Integer::intValue).sum();
            avgRating = Math.round((sum / ratings.size()) * 100.0) / 100.0;
        }

        boolean qualifiedBonus = avgRating != null
                && !ratings.isEmpty()
                && BigDecimal.valueOf(avgRating).compareTo(effThreshold) >= 0;

        BigDecimal baseCommission = grossRevenue.multiply(effShare)
                .divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP);
        BigDecimal ratingBonus = qualifiedBonus
                ? grossRevenue.multiply(effBonusPct).divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal netPay = baseCommission.add(ratingBonus).add(effAllowance).max(BigDecimal.ZERO);

        return PayrollItemDTO.builder()
                .userId(instructor.getId())
                .username(instructor.getUsername())
                .fullName(instructor.getFullName())
                .email(instructor.getEmail())
                .avatarUrl(instructor.getAvatarUrl())
                .roleType("INSTRUCTOR")
                .hasCustomConfig(hasInstructorOverride(custom))
                .customNote(custom != null ? custom.getNote() : null)
                .effectiveSharePercent(effShare)
                .customSharePercent(custom != null ? custom.getInstructorSharePercent() : null)
                .effectiveRatingBonusPercent(effBonusPct)
                .customRatingBonusPercent(custom != null ? custom.getInstructorRatingBonusPercent() : null)
                .effectiveRatingThreshold(effThreshold)
                .customRatingThreshold(custom != null ? custom.getInstructorRatingThreshold() : null)
                .effectiveFixedAllowance(effAllowance)
                .customFixedAllowance(custom != null ? custom.getFixedAllowance() : null)
                .cohortCount(cohortCount)
                .studentCount(studentCount)
                .grossRevenue(grossRevenue)
                .averageRating(avgRating)
                .ratingCount(ratings.size())
                .qualifiedForRatingBonus(qualifiedBonus)
                .baseCommissionAmount(baseCommission)
                .ratingBonusAmount(ratingBonus)
                .totalNetPay(netPay)
                .build();
    }

    private PayrollItemDTO buildModeratorPayrollItem(
            User moderator,
            PayrollConfig global,
            PayrollConfig custom,
            int workDays,
            int regularMinutes,
            int otMinutes,
            long completedTasks,
            long escalatedTasks) {

        BigDecimal effMinuteRate = resolveValue(custom != null ? custom.getModeratorMinuteRate() : null,
                global.getModeratorMinuteRate(), new BigDecimal("500"));
        BigDecimal effOtMultiplier = resolveValue(custom != null ? custom.getModeratorOtMultiplier() : null,
                global.getModeratorOtMultiplier(), new BigDecimal("1.50"));
        BigDecimal effTaskBonus = resolveValue(custom != null ? custom.getModeratorTaskBonus() : null,
                global.getModeratorTaskBonus(), new BigDecimal("10000"));
        BigDecimal effSlaPenalty = resolveValue(custom != null ? custom.getModeratorSlaPenalty() : null,
                global.getModeratorSlaPenalty(), new BigDecimal("15000"));
        BigDecimal effAllowance = resolveValue(custom != null ? custom.getFixedAllowance() : null,
                global.getFixedAllowance(), BigDecimal.ZERO);

        BigDecimal regularPay = BigDecimal.valueOf(regularMinutes)
                .multiply(effMinuteRate)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal otPay = BigDecimal.valueOf(otMinutes)
                .multiply(effMinuteRate)
                .multiply(effOtMultiplier)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal taskBonusPay = BigDecimal.valueOf(completedTasks)
                .multiply(effTaskBonus)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal slaPenalty = BigDecimal.valueOf(escalatedTasks)
                .multiply(effSlaPenalty)
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal netPay = regularPay.add(otPay).add(taskBonusPay).add(effAllowance)
                .subtract(slaPenalty)
                .max(BigDecimal.ZERO);

        return PayrollItemDTO.builder()
                .userId(moderator.getId())
                .username(moderator.getUsername())
                .fullName(moderator.getFullName())
                .email(moderator.getEmail())
                .avatarUrl(moderator.getAvatarUrl())
                .roleType("MODERATOR")
                .hasCustomConfig(hasModeratorOverride(custom))
                .customNote(custom != null ? custom.getNote() : null)
                .effectiveMinuteRate(effMinuteRate)
                .customMinuteRate(custom != null ? custom.getModeratorMinuteRate() : null)
                .effectiveOtMultiplier(effOtMultiplier)
                .customOtMultiplier(custom != null ? custom.getModeratorOtMultiplier() : null)
                .effectiveTaskBonus(effTaskBonus)
                .customTaskBonus(custom != null ? custom.getModeratorTaskBonus() : null)
                .effectiveSlaPenalty(effSlaPenalty)
                .customSlaPenalty(custom != null ? custom.getModeratorSlaPenalty() : null)
                .effectiveFixedAllowance(effAllowance)
                .customFixedAllowance(custom != null ? custom.getFixedAllowance() : null)
                .workDaysCount(workDays)
                .regularMinutes(regularMinutes)
                .otMinutes(otMinutes)
                .completedTasksCount(completedTasks)
                .escalatedTasksCount(escalatedTasks)
                .regularShiftPay(regularPay)
                .otShiftPay(otPay)
                .taskBonusPay(taskBonusPay)
                .slaPenaltyAmount(slaPenalty)
                .totalNetPay(netPay)
                .build();
    }

    private Map<Long, PayrollConfig> loadCustomConfigMap() {
        return payrollConfigRepository.findByIsGlobalFalse().stream()
                .filter(c -> c.getUser() != null && c.getUser().getId() != null)
                .collect(Collectors.toMap(c -> c.getUser().getId(), c -> c, (a, b) -> b));
    }

    private boolean hasInstructorOverride(PayrollConfig custom) {
        if (custom == null) {
            return false;
        }
        return custom.getInstructorSharePercent() != null
                || custom.getInstructorRatingBonusPercent() != null
                || custom.getInstructorRatingThreshold() != null
                || (custom.getFixedAllowance() != null && custom.getFixedAllowance().compareTo(BigDecimal.ZERO) > 0);
    }

    private boolean hasModeratorOverride(PayrollConfig custom) {
        if (custom == null) {
            return false;
        }
        return custom.getModeratorMinuteRate() != null
                || custom.getModeratorOtMultiplier() != null
                || custom.getModeratorTaskBonus() != null
                || custom.getModeratorSlaPenalty() != null
                || (custom.getFixedAllowance() != null && custom.getFixedAllowance().compareTo(BigDecimal.ZERO) > 0);
    }

    private User resolveCohortInstructor(Cohort cohort) {
        if (cohort.getCreatedBy() != null) {
            return cohort.getCreatedBy();
        }
        if (cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null) {
            return cohort.getCourse().getCreatedBy();
        }
        return null;
    }

    private BigDecimal resolveCohortPrice(Cohort cohort) {
        if (cohort.getPrice() != null && cohort.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return cohort.getPrice();
        }
        if (cohort.getCourse() != null && cohort.getCourse().getPrice() != null && cohort.getCourse().getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return cohort.getCourse().getPrice();
        }
        return BigDecimal.ZERO;
    }

    private YearMonth parseYearMonth(String period) {
        if (period == null || period.isBlank() || "ALL".equalsIgnoreCase(period.trim())) {
            return null;
        }
        try {
            return YearMonth.parse(period.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isInstantInPeriod(Instant instant, YearMonth targetMonth) {
        if (targetMonth == null) {
            return true;
        }
        if (instant == null) {
            return false;
        }
        YearMonth ym = YearMonth.from(instant.atZone(HCM_ZONE));
        return targetMonth.equals(ym);
    }

    private boolean isLocalDateInPeriod(LocalDate date, YearMonth targetMonth) {
        if (targetMonth == null) {
            return true;
        }
        if (date == null) {
            return false;
        }
        return targetMonth.equals(YearMonth.from(date));
    }

    private BigDecimal resolveValue(BigDecimal customVal, BigDecimal globalVal, BigDecimal fallback) {
        if (customVal != null) {
            return customVal;
        }
        if (globalVal != null) {
            return globalVal;
        }
        return fallback;
    }

    private BigDecimal clampPercent(BigDecimal val, BigDecimal fallback) {
        if (val == null) {
            return fallback;
        }
        return val.max(BigDecimal.ZERO).min(ONE_HUNDRED);
    }

    private BigDecimal clampThreshold(BigDecimal val, BigDecimal fallback) {
        if (val == null) {
            return fallback;
        }
        return val.max(BigDecimal.ONE).min(new BigDecimal("5.00"));
    }

    private BigDecimal sanitizeNonNegative(BigDecimal val, BigDecimal fallback) {
        if (val == null) {
            return fallback;
        }
        return val.max(BigDecimal.ZERO);
    }
}
