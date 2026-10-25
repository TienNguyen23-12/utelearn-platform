package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import vn.edu.ute.utelearn.controller.CheckoutController;
import vn.edu.ute.utelearn.controller.CourseController;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dto.CourseUpsellDTO;
import vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.impl.CohortServiceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CourseUpsellFeatureTest {

    private MockMvc checkoutMvc;
    private MockMvc courseMvc;

    @Mock
    private AuthService authService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private CohortService cohortService;
    @Mock
    private vn.edu.ute.utelearn.service.OrderService orderService;
    @Mock
    private vn.edu.ute.utelearn.config.UtelearnProperties properties;
    @Mock
    private vn.edu.ute.utelearn.dao.OrderRepository orderRepository;

    private User sampleUser;
    private Course sampleCourse;
    private Cohort cohort1;
    private Cohort cohort2;
    private Cohort cohort3;
    private Cohort cohortAll;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver1 = new InternalResourceViewResolver();
        viewResolver1.setPrefix("/templates/");
        viewResolver1.setSuffix(".html");

        InternalResourceViewResolver viewResolver2 = new InternalResourceViewResolver();
        viewResolver2.setPrefix("/templates/");
        viewResolver2.setSuffix(".html");

        CheckoutController checkoutController = new CheckoutController(
                cohortRepository,
                cohortMemberRepository,
                courseRepository,
                authService,
                cohortScheduleRepository,
                lessonRepository
                , orderService, properties, orderRepository
        );
        checkoutMvc = MockMvcBuilders.standaloneSetup(checkoutController)
                .setViewResolvers(viewResolver1)
                .build();

        CourseController courseController = new CourseController(
                authService,
                courseService,
                cohortService,
                cohortMemberRepository,
                cohortRepository,
                cohortScheduleRepository,
                lessonRepository,
                null
        );
        courseMvc = MockMvcBuilders.standaloneSetup(courseController)
                .setViewResolvers(viewResolver2)
                .build();

        sampleUser = User.builder()
                .id(1L)
                .email("student@utelearn.edu.vn")
                .fullName("Test Student")
                .build();

        sampleCourse = Course.builder()
                .id(100L)
                .title("Full Stack Web Development")
                .slug("full-stack-web")
                .code("FSW-01")
                .price(new BigDecimal("1000000"))
                .build();

        cohort1 = Cohort.builder()
                .id(11L)
                .name("Module 1: Frontend Basics")
                .code("FSW-MOD1")
                .course(sampleCourse)
                .price(new BigDecimal("300000"))
                .status("ENROLLING")
                .maxCapacity(50)
                .currentEnrolled(10)
                .build();

        cohort2 = Cohort.builder()
                .id(12L)
                .name("Module 2: Backend Spring Boot")
                .code("FSW-MOD2")
                .course(sampleCourse)
                .price(new BigDecimal("450000"))
                .status("ENROLLING")
                .maxCapacity(50)
                .currentEnrolled(10)
                .build();

        cohort3 = Cohort.builder()
                .id(13L)
                .name("Module 3: DevOps & Deployment")
                .code("FSW-MOD3")
                .course(sampleCourse)
                .price(new BigDecimal("450000"))
                .status("ENROLLING")
                .maxCapacity(50)
                .currentEnrolled(10)
                .build();

        cohortAll = Cohort.builder()
                .id(14L)
                .name("Toàn bộ khóa học: Full Stack Web Development")
                .code("FSW-ALL")
                .course(sampleCourse)
                .price(new BigDecimal("1000000"))
                .status("ENROLLING")
                .maxCapacity(1000)
                .currentEnrolled(10)
                .build();
    }

    @Test
    void testUpsellCalculationWhenUserEnrolledInOneModule() {
        CohortServiceImpl cohortServiceImpl = new CohortServiceImpl(
                cohortRepository,
                cohortScheduleRepository,
                courseRepository,
                lessonRepository,
                cohortMemberRepository,
                null,
                null,
                null
        );

        CohortMember cm1 = CohortMember.builder()
                .id(101L)
                .user(sampleUser)
                .cohort(cohort1)
                .build();

        when(cohortMemberRepository.findByUserOrderByEnrolledAtDesc(sampleUser)).thenReturn(List.of(cm1));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(cohort1, cohort2, cohort3));

        UserEnrollmentSummaryDTO summary = cohortServiceImpl.getUserEnrollmentSummary(sampleUser);

        assertNotNull(summary.getUpsellSuggestions());
        assertEquals(1, summary.getUpsellSuggestions().size());

        CourseUpsellDTO upsell = summary.getUpsellSuggestions().get(0);
        assertEquals(sampleCourse.getId(), upsell.getCourse().getId());
        assertEquals(3, upsell.getTotalModules());
        assertEquals(1, upsell.getEnrolledModulesCount());
        assertEquals(2, upsell.getRemainingModulesCount());

        assertEquals(new BigDecimal("1000000"), upsell.getCoursePrice());
        assertEquals(new BigDecimal("300000"), upsell.getTotalPaidAmount());
        assertEquals(new BigDecimal("700000"), upsell.getUpgradePrice());
        assertEquals(new BigDecimal("900000"), upsell.getRemainingModulesOriginalPrice());
        assertEquals(new BigDecimal("200000"), upsell.getSavingsAmount());
    }

    @Test
    void testUpsellExcludesSyntheticFullCourseCohort() {
        CohortServiceImpl cohortServiceImpl = new CohortServiceImpl(
                cohortRepository,
                cohortScheduleRepository,
                courseRepository,
                lessonRepository,
                cohortMemberRepository,
                null,
                null,
                null
        );

        CohortMember cm1 = CohortMember.builder()
                .id(101L)
                .user(sampleUser)
                .cohort(cohort1)
                .build();

        when(cohortMemberRepository.findByUserOrderByEnrolledAtDesc(sampleUser)).thenReturn(List.of(cm1));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(cohort1, cohort2, cohort3, cohortAll));

        UserEnrollmentSummaryDTO summary = cohortServiceImpl.getUserEnrollmentSummary(sampleUser);

        assertNotNull(summary.getUpsellSuggestions());
        assertEquals(1, summary.getUpsellSuggestions().size());

        CourseUpsellDTO upsell = summary.getUpsellSuggestions().get(0);
        assertEquals(3, upsell.getTotalModules());
        assertEquals(1, upsell.getEnrolledModulesCount());
        assertEquals(2, upsell.getRemainingModulesCount());
        assertFalse(upsell.getRemainingCohorts().stream().anyMatch(c -> c.getCode().endsWith("-ALL")));
        assertEquals(new BigDecimal("200000"), upsell.getSavingsAmount());
    }

    @Test
    void testNoUpsellWhenFullCourseEnrolled() {
        CohortServiceImpl cohortServiceImpl = new CohortServiceImpl(
                cohortRepository,
                cohortScheduleRepository,
                courseRepository,
                lessonRepository,
                cohortMemberRepository,
                null,
                null,
                null
        );

        CohortMember cm1 = CohortMember.builder().id(101L).user(sampleUser).cohort(cohort1).build();
        CohortMember cm2 = CohortMember.builder().id(102L).user(sampleUser).cohort(cohort2).build();
        CohortMember cm3 = CohortMember.builder().id(103L).user(sampleUser).cohort(cohort3).build();

        when(cohortMemberRepository.findByUserOrderByEnrolledAtDesc(sampleUser)).thenReturn(List.of(cm1, cm2, cm3));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(cohort1, cohort2, cohort3));

        UserEnrollmentSummaryDTO summary = cohortServiceImpl.getUserEnrollmentSummary(sampleUser);

        assertEquals(1, summary.getEnrolledCourses().size());
        assertEquals(0, summary.getEnrolledModules().size());
        assertTrue(summary.getUpsellSuggestions().isEmpty());
    }

    @Test
    void testShowCourseCheckoutPageWithUpgradeDifferencePrice() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));
        when(courseRepository.findBySlug("full-stack-web")).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdAndStatusIn(100L, List.of("ENROLLING", "UPCOMING", "IN_PROGRESS")))
                .thenReturn(List.of(cohort1, cohort2, cohort3));

        CohortMember existingMember = CohortMember.builder()
                .id(101L)
                .user(sampleUser)
                .cohort(cohort1)
                .build();
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(100L, 1L))
                .thenReturn(List.of(existingMember));

        checkoutMvc.perform(get("/checkout/course/full-stack-web"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout/index"))
                .andExpect(model().attribute("isFullCourse", true))
                .andExpect(model().attribute("isUpgrade", true))
                .andExpect(model().attribute("coursePrice", new BigDecimal("1000000")))
                .andExpect(model().attribute("totalPaidAmount", new BigDecimal("300000")))
                .andExpect(model().attribute("totalPrice", new BigDecimal("700000")))
                .andExpect(model().attribute("discountAmount", new BigDecimal("200000")));
    }

    @Test
    void testProcessCoursePaymentUpgradesAndEnrollsRemainingCohorts() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));
        when(courseRepository.findBySlug("full-stack-web")).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdAndStatusIn(100L, List.of("ENROLLING", "UPCOMING", "IN_PROGRESS")))
                .thenReturn(List.of(cohort1, cohort2, cohort3));

        CohortMember existingMember = CohortMember.builder()
                .id(101L)
                .user(sampleUser)
                .cohort(cohort1)
                .build();
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(100L, 1L))
                .thenReturn(List.of(existingMember));
        when(cohortMemberRepository.countByCohort_Id(any())).thenReturn(11L);

        checkoutMvc.perform(post("/checkout/course/full-stack-web/process"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/full-stack-web/modules/12/learn"))
                .andExpect(flash().attribute("success", "Nâng cấp trọn gói khóa học thành công! Bạn đã mở khóa toàn bộ 2 module còn lại."));
    }

    @Test
    void testShowMyCoursesPassesUpsellSuggestionsToModel() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));

        CourseUpsellDTO upsellDTO = CourseUpsellDTO.builder()
                .course(sampleCourse)
                .totalModules(3)
                .enrolledModulesCount(1)
                .remainingModulesCount(2)
                .coursePrice(new BigDecimal("1000000"))
                .totalPaidAmount(new BigDecimal("300000"))
                .upgradePrice(new BigDecimal("700000"))
                .remainingModulesOriginalPrice(new BigDecimal("900000"))
                .savingsAmount(new BigDecimal("200000"))
                .remainingCohorts(List.of(cohort2, cohort3))
                .build();

        UserEnrollmentSummaryDTO summary = UserEnrollmentSummaryDTO.builder()
                .enrolledCourses(Collections.emptyList())
                .enrolledModules(List.of(CohortMember.builder().user(sampleUser).cohort(cohort1).build()))
                .upsellSuggestions(List.of(upsellDTO))
                .build();

        when(cohortService.getUserEnrollmentSummary(sampleUser)).thenReturn(summary);
        when(cohortMemberRepository.findByUserOrderByEnrolledAtDesc(sampleUser)).thenReturn(Collections.emptyList());

        courseMvc.perform(get("/courses/my-courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/my-courses"))
                .andExpect(model().attributeExists("enrolledCourses", "enrolledModules", "upsellSuggestions"))
                .andExpect(model().attribute("upsellSuggestions", List.of(upsellDTO)));
    }
}
