package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository;
import vn.edu.ute.utelearn.dao.PayrollConfigRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.PayrollItemDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.PayrollConfig;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.impl.PayrollServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollFeatureTest {

    @Mock
    private PayrollConfigRepository payrollConfigRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CohortRepository cohortRepository;
    @Mock
    private CohortMemberRepository cohortMemberRepository;
    @Mock
    private ModeratorAttendanceRepository moderatorAttendanceRepository;
    @Mock
    private ModerationTaskRepository moderationTaskRepository;

    @InjectMocks
    private PayrollServiceImpl payrollService;

    private User instructorDefault;
    private User instructorCustom;
    private User moderatorDefault;
    private User moderatorCustom;
    private PayrollConfig globalConfig;

    @BeforeEach
    void setUp() {
        Role instRole = Role.builder().id(1L).code("INSTRUCTOR").name("Instructor").build();
        Role modRole = Role.builder().id(2L).code("MODERATOR").name("Moderator").build();

        instructorDefault = User.builder().id(10L).username("inst1").fullName("Giảng viên Mặc định").roles(Set.of(instRole)).build();
        instructorCustom = User.builder().id(11L).username("inst2").fullName("Giảng viên VIP").roles(Set.of(instRole)).build();

        moderatorDefault = User.builder().id(20L).username("mod1").fullName("Moderator Mặc định").roles(Set.of(modRole)).build();
        moderatorCustom = User.builder().id(21L).username("mod2").fullName("Moderator Trưởng nhóm").roles(Set.of(modRole)).build();

        globalConfig = PayrollConfig.createDefaultGlobalConfig();
    }

    @Test
    void testCalculateInstructorPayroll_DefaultAndCustomOverride() {
        PayrollConfig customInstConfig = PayrollConfig.builder()
                .user(instructorCustom)
                .isGlobal(false)
                .instructorSharePercent(new BigDecimal("80.00"))
                .fixedAllowance(new BigDecimal("500000"))
                .note("Giảng viên cấp cao")
                .build();

        Cohort c1 = Cohort.builder().id(100L).code("MOD-01").name("Module 1").createdBy(instructorDefault).price(new BigDecimal("1000000")).build();
        Cohort c2 = Cohort.builder().id(101L).code("MOD-02").name("Module 2").createdBy(instructorCustom).price(new BigDecimal("1000000")).build();

        CohortMember cm1 = CohortMember.builder().id(1L).cohort(c1).roleInCohort("STUDENT").enrolledAt(Instant.now()).rating(5).build();
        CohortMember cm2 = CohortMember.builder().id(2L).cohort(c2).roleInCohort("STUDENT").enrolledAt(Instant.now()).rating(4).build();

        when(payrollConfigRepository.findFirstByIsGlobalTrue()).thenReturn(Optional.of(globalConfig));
        when(payrollConfigRepository.findByIsGlobalFalse()).thenReturn(List.of(customInstConfig));
        when(userRepository.findAll()).thenReturn(List.of(instructorDefault, instructorCustom));
        when(cohortRepository.findAll()).thenReturn(List.of(c1, c2));
        when(cohortMemberRepository.findAllStudentEnrollmentsWithCohortAndInstructor()).thenReturn(List.of(cm1, cm2));

        List<PayrollItemDTO> items = payrollService.calculateInstructorPayroll("ALL");
        assertEquals(2, items.size());

        PayrollItemDTO vipItem = items.stream().filter(i -> i.getUserId().equals(11L)).findFirst().orElseThrow();
        assertTrue(vipItem.isHasCustomConfig());
        assertEquals(new BigDecimal("80.00"), vipItem.getEffectiveSharePercent());
        assertEquals(new BigDecimal("800000"), vipItem.getBaseCommissionAmount());
        assertEquals(new BigDecimal("0"), vipItem.getRatingBonusAmount());
        assertEquals(new BigDecimal("1300000"), vipItem.getTotalNetPay());

        PayrollItemDTO defaultItem = items.stream().filter(i -> i.getUserId().equals(10L)).findFirst().orElseThrow();
        assertFalse(defaultItem.isHasCustomConfig());
        assertEquals(new BigDecimal("70.00"), defaultItem.getEffectiveSharePercent());
        assertEquals(new BigDecimal("700000"), defaultItem.getBaseCommissionAmount());
        assertEquals(new BigDecimal("50000"), defaultItem.getRatingBonusAmount());
        assertEquals(new BigDecimal("750000"), defaultItem.getTotalNetPay());
    }

    @Test
    void testCalculateModeratorPayroll_DefaultAndCustomOverride() {
        PayrollConfig customModConfig = PayrollConfig.builder()
                .user(moderatorCustom)
                .isGlobal(false)
                .moderatorMinuteRate(new BigDecimal("1000"))
                .moderatorTaskBonus(new BigDecimal("20000"))
                .build();

        ModeratorAttendance att1 = ModeratorAttendance.builder()
                .id(1L)
                .moderator(moderatorDefault)
                .workDate(LocalDate.now())
                .totalWorkloadMinutes(300)
                .otWorkloadMinutes(60)
                .completedTasksCount(5)
                .build();

        ModeratorAttendance att2 = ModeratorAttendance.builder()
                .id(2L)
                .moderator(moderatorCustom)
                .workDate(LocalDate.now())
                .totalWorkloadMinutes(200)
                .otWorkloadMinutes(0)
                .completedTasksCount(5)
                .build();

        when(payrollConfigRepository.findFirstByIsGlobalTrue()).thenReturn(Optional.of(globalConfig));
        when(payrollConfigRepository.findByIsGlobalFalse()).thenReturn(List.of(customModConfig));
        when(userRepository.findAll()).thenReturn(List.of(moderatorDefault, moderatorCustom));
        when(moderatorAttendanceRepository.findAllByOrderByWorkDateDesc()).thenReturn(List.of(att1, att2));
        when(moderationTaskRepository.findAllWithAssignees()).thenReturn(List.of());

        List<PayrollItemDTO> items = payrollService.calculateModeratorPayroll("ALL");
        assertEquals(2, items.size());

        PayrollItemDTO customItem = items.stream().filter(i -> i.getUserId().equals(21L)).findFirst().orElseThrow();
        assertTrue(customItem.isHasCustomConfig());
        assertEquals(200, customItem.getRegularMinutes());
        assertEquals(new BigDecimal("200000"), customItem.getRegularShiftPay());
        assertEquals(new BigDecimal("100000"), customItem.getTaskBonusPay());
        assertEquals(new BigDecimal("300000"), customItem.getTotalNetPay());

        PayrollItemDTO defItem = items.stream().filter(i -> i.getUserId().equals(20L)).findFirst().orElseThrow();
        assertFalse(defItem.isHasCustomConfig());
        assertEquals(240, defItem.getRegularMinutes());
        assertEquals(60, defItem.getOtMinutes());
        assertEquals(new BigDecimal("120000"), defItem.getRegularShiftPay());
        assertEquals(new BigDecimal("45000"), defItem.getOtShiftPay());
        assertEquals(new BigDecimal("50000"), defItem.getTaskBonusPay());
        assertEquals(new BigDecimal("215000"), defItem.getTotalNetPay());
    }
}

