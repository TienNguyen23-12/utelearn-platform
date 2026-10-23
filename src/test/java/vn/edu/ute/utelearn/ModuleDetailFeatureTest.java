package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import vn.edu.ute.utelearn.controller.CourseController;
import vn.edu.ute.utelearn.controller.HomeController;
import vn.edu.ute.utelearn.controller.api.CourseApiController;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.LessonService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import vn.edu.ute.utelearn.controller.instructor.InstructorCohortController;

@ExtendWith(MockitoExtension.class)
public class ModuleDetailFeatureTest {

    private MockMvc courseMvc;
    private MockMvc homeMvc;
    private MockMvc apiMvc;
    private MockMvc instructorCohortMvc;

    @Mock
    private AuthService authService;

    @Mock
    private CourseService courseService;

    @Mock
    private CohortService cohortService;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonService lessonService;

    @Mock
    private vn.edu.ute.utelearn.service.SectionService sectionService;

    @Mock
    private vn.edu.ute.utelearn.dao.SectionRepository sectionRepository;

    @InjectMocks
    private CourseController courseController;

    @InjectMocks
    private HomeController homeController;

    @InjectMocks
    private CourseApiController courseApiController;

    @InjectMocks
    private InstructorCohortController instructorCohortController;

    private Course sampleCourse;
    private Cohort sampleCohort;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver courseResolver = new InternalResourceViewResolver();
        courseResolver.setPrefix("/templates/");
        courseResolver.setSuffix(".html");

        InternalResourceViewResolver homeResolver = new InternalResourceViewResolver();
        homeResolver.setPrefix("/templates/");
        homeResolver.setSuffix(".html");

        courseMvc = MockMvcBuilders.standaloneSetup(courseController).setViewResolvers(courseResolver).build();
        homeMvc = MockMvcBuilders.standaloneSetup(homeController).setViewResolvers(homeResolver).build();
        apiMvc = MockMvcBuilders.standaloneSetup(courseApiController).build();
        instructorCohortMvc = MockMvcBuilders.standaloneSetup(instructorCohortController).build();

        sampleCourse = Course.builder()
                .id(1L)
                .title("Lập trình Web Cơ bản")
                .slug("lap-trinh-web-co-ban")
                .code("CS-WEB-01")
                .price(BigDecimal.valueOf(699000))
                .build();

        sampleCohort = Cohort.builder()
                .id(1L)
                .name("Module 1: Nền tảng Web với HTML5 & CSS3")
                .code("MOD-WEB-01")
                .price(BigDecimal.valueOf(250000))
                .course(sampleCourse)
                .description("Mô tả module")
                .objectives("[\"Mục tiêu 1\", \"Mục tiêu 2\"]")
                .build();
    }

    @Test
    void testShowCourseDetailReturnsViewWithModules() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findByCourseIdAndStatusInOrdered(anyLong(), anyList()))
                .thenReturn(List.of(sampleCohort));

        courseMvc.perform(get("/courses/lap-trinh-web-co-ban"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/detail"))
                .andExpect(model().attributeExists("availableCohorts", "courseSlug"));
    }

    @Test
    void testShowCourseDetailThymeleafRendering() throws Exception {
        org.thymeleaf.templateresolver.ClassLoaderTemplateResolver templateResolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        org.thymeleaf.spring6.SpringTemplateEngine templateEngine = new org.thymeleaf.spring6.SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        org.thymeleaf.spring6.view.ThymeleafViewResolver viewResolver = new org.thymeleaf.spring6.view.ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        MockMvc mvcWithThymeleaf = MockMvcBuilders.standaloneSetup(courseController)
                .setViewResolvers(viewResolver)
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findByCourseIdAndStatusInOrdered(anyLong(), anyList()))
                .thenReturn(List.of(sampleCohort));
        when(cohortRepository.findByCourseIdOrdered(anyLong()))
                .thenReturn(List.of(sampleCohort));

        mvcWithThymeleaf.perform(get("/courses/lap-trinh-web-co-ban"))
                .andExpect(status().isOk());
    }

    @Test
    void testShowModuleDetailReturnsModuleDetailView() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(sampleCohort));
        when(cohortScheduleRepository.findByCohortIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findSectionsForCohort(1L)).thenReturn(Collections.emptyList());

        courseMvc.perform(get("/courses/lap-trinh-web-co-ban/modules/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/module-detail"))
                .andExpect(model().attributeExists("course", "module", "moduleLessons", "upcomingSchedules"));
    }

    @Test
    void testCohortRedirectToModuleDetail() throws Exception {
        when(cohortService.getCohortById(1L)).thenReturn(sampleCohort);

        homeMvc.perform(get("/cohorts/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/lap-trinh-web-co-ban/modules/1"));
    }

    @Test
    void testCourseApiReturnsModulesWithObjectives() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(sectionService.getSectionsByCourseId(1L)).thenReturn(Collections.emptyList());
        when(cohortRepository.findByCourseIdOrdered(1L)).thenReturn(List.of(sampleCohort));

        apiMvc.perform(get("/api/courses/lap-trinh-web-co-ban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules[0].name").value("Module 1: Nền tảng Web với HTML5 & CSS3"))
                .andExpect(jsonPath("$.modules[0].objectives").value("[\"Mục tiêu 1\", \"Mục tiêu 2\"]"))
                .andExpect(jsonPath("$.modules[0].description").value("Mô tả module"));
    }

    @Test
    void testShowCourseContentRedirectsToEnrolledModuleLearn() throws Exception {
        User user = User.builder().id(10L).fullName("Test Student").email("test@utelearn.edu.vn").build();
        vn.edu.ute.utelearn.entity.CohortMember cm = vn.edu.ute.utelearn.entity.CohortMember.builder()
                .id(101L).cohort(sampleCohort).user(user).build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(user));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortMemberRepository.findByCourseIdAndUserIdWithCohort(1L, 10L)).thenReturn(List.of(cm));

        courseMvc.perform(get("/courses/lap-trinh-web-co-ban/learn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/lap-trinh-web-co-ban/modules/1/learn"));
    }

    @Test
    void testShowModuleLearnReturnsModuleLearnView() throws Exception {
        User user = User.builder().id(10L).fullName("Test Student").email("test@utelearn.edu.vn").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(user));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(1L, 10L)).thenReturn(true);
        when(cohortScheduleRepository.findByCohortIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());
        when(cohortMemberRepository.findByCourseIdAndUserIdWithCohort(1L, 10L)).thenReturn(Collections.emptyList());

        courseMvc.perform(get("/courses/lap-trinh-web-co-ban/modules/1/learn"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/module-learn"))
                .andExpect(model().attributeExists("currentUser", "course", "module", "moduleLessons", "lessonDtos"));
    }

    @Test
    void testModuleLearnApiReturnsModuleData() throws Exception {
        User user = User.builder().id(10L).fullName("Test Student").email("test@utelearn.edu.vn").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(user));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(1L, 10L)).thenReturn(true);
        when(cohortScheduleRepository.findByCohortIdWithDetails(1L)).thenReturn(Collections.emptyList());

        apiMvc.perform(get("/api/courses/lap-trinh-web-co-ban/modules/1/learn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module.code").value("MOD-WEB-01"))
                .andExpect(jsonPath("$.course.title").value("Lập trình Web Cơ bản"));
    }

    @Test
    void testShowMyCoursesReturnsEnrolledCoursesAndModules() throws Exception {
        User user = User.builder().id(10L).fullName("Test Student").email("test@utelearn.edu.vn").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(user));
        when(cohortService.getUserEnrollmentSummary(user)).thenReturn(
                vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO.builder()
                        .enrolledCourses(List.of(vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO.EnrolledCourseItem.builder()
                                .course(sampleCourse)
                                .build()))
                        .enrolledModules(Collections.emptyList())
                        .build()
        );

        courseMvc.perform(get("/courses/my-courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/my-courses"))
                .andExpect(model().attributeExists("enrolledCourses", "enrolledModules"));
    }

    @Test
    void testEnrollmentSummaryExcludesModulesWhenFullCourseEnrolled() {
        vn.edu.ute.utelearn.service.impl.CohortServiceImpl cohortServiceImpl =
                new vn.edu.ute.utelearn.service.impl.CohortServiceImpl(
                        cohortRepository,
                        cohortScheduleRepository,
                        null,
                        null,
                        cohortMemberRepository,
                        null,
                        null,
                        null
                );

        User user = User.builder().id(10L).build();
        Cohort cohort1 = Cohort.builder().id(1L).course(sampleCourse).code("MOD-01").build();
        Cohort cohort2 = Cohort.builder().id(2L).course(sampleCourse).code("MOD-02").build();

        vn.edu.ute.utelearn.entity.CohortMember cm1 = vn.edu.ute.utelearn.entity.CohortMember.builder()
                .id(101L).cohort(cohort1).user(user).build();
        vn.edu.ute.utelearn.entity.CohortMember cm2 = vn.edu.ute.utelearn.entity.CohortMember.builder()
                .id(102L).cohort(cohort2).user(user).build();

        when(cohortMemberRepository.findByUserOrderByEnrolledAtDesc(user)).thenReturn(List.of(cm1, cm2));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(cohort1, cohort2));

        vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO summary = cohortServiceImpl.getUserEnrollmentSummary(user);

        org.junit.jupiter.api.Assertions.assertEquals(1, summary.getEnrolledCourses().size());
        org.junit.jupiter.api.Assertions.assertEquals(0, summary.getEnrolledModules().size());
    }

    @Test
    void testRedirectToCreateModule() throws Exception {
        courseMvc.perform(get("/courses/1/modules/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/courses/1/modules/create"));
    }

    @Test
    void testRedirectToCohortCreate() throws Exception {
        homeMvc.perform(get("/cohorts/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/cohorts/create"));
    }

    @Test
    void testRedirectToEditModule() throws Exception {
        Course course = Course.builder().id(3L).slug("course-slug").build();
        Cohort cohort = Cohort.builder().id(1L).course(course).build();
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(cohort));

        courseMvc.perform(get("/courses/1/modules/1/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/courses/3/modules/1/edit"));
    }

    @Test
    void testRedirectToCohortEdit() throws Exception {
        Course course = Course.builder().id(3L).build();
        Cohort cohort = Cohort.builder().id(1L).course(course).build();
        when(cohortService.getCohortById(1L)).thenReturn(cohort);

        homeMvc.perform(get("/cohorts/1/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/courses/3/modules/1/edit"));
    }

    @Test
    void testDeleteCohortSuccessWhenNoEnrolledMembers() {
        vn.edu.ute.utelearn.dao.SectionRepository mockSecRepo = org.mockito.Mockito.mock(vn.edu.ute.utelearn.dao.SectionRepository.class);
        org.springframework.jdbc.core.JdbcTemplate mockJdbc = org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
        vn.edu.ute.utelearn.dao.LessonRepository mockLessonRepo = org.mockito.Mockito.mock(vn.edu.ute.utelearn.dao.LessonRepository.class);

        vn.edu.ute.utelearn.service.impl.CohortServiceImpl cohortServiceImpl =
                new vn.edu.ute.utelearn.service.impl.CohortServiceImpl(
                        cohortRepository,
                        cohortScheduleRepository,
                        null,
                        mockLessonRepo,
                        cohortMemberRepository,
                        mockSecRepo,
                        null,
                        mockJdbc
                );

        User instructor = User.builder().id(2L).roles(Collections.emptySet()).build();
        Cohort cohort = Cohort.builder().id(15L).createdBy(instructor).build();

        when(cohortRepository.findById(15L)).thenReturn(Optional.of(cohort));
        when(cohortMemberRepository.countByCohort_Id(15L)).thenReturn(0L);
        when(mockSecRepo.findByCohortIdOrderByOrderIndexAsc(15L)).thenReturn(Collections.emptyList());

        cohortServiceImpl.deleteCohort(15L, instructor);

        org.mockito.Mockito.verify(cohortRepository).delete(cohort);
        org.mockito.Mockito.verify(cohortScheduleRepository).deleteByCohortId(15L);
    }

    @Test
    void testInstructorCohortDeleteSuccess() throws Exception {
        User instructor = User.builder().id(2L).build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(instructor));

        instructorCohortMvc.perform(post("/instructor/cohorts/15/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/cohorts"))
                .andExpect(flash().attribute("successMessage", "Xóa Module thành công!"));

        org.mockito.Mockito.verify(cohortService).deleteCohort(15L, instructor);
    }

    @Test
    void testInstructorCohortDeleteFailureWhenEnrolled() throws Exception {
        User instructor = User.builder().id(2L).build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(instructor));
        org.mockito.Mockito.doThrow(new RuntimeException("Không thể xóa Module đã có 1 học viên đăng ký!"))
                .when(cohortService).deleteCohort(15L, instructor);

        instructorCohortMvc.perform(post("/instructor/cohorts/15/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/cohorts"))
                .andExpect(flash().attribute("errorMessage", "Không thể xóa Module: Không thể xóa Module đã có 1 học viên đăng ký!"));
    }

    @Test
    void testShowCreateCohortForm() throws Exception {
        User instructor = User.builder().id(2L).build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(instructor));
        when(courseService.getCoursesByInstructor(org.mockito.ArgumentMatchers.eq(2L), any())).thenReturn(org.springframework.data.domain.Page.empty());

        instructorCohortMvc.perform(get("/instructor/cohorts/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("instructor/cohorts/form"))
                .andExpect(model().attributeExists("cohortDto", "courses"));
    }

    @Test
    void testShowEditCohortFormWithParsedObjectives() throws Exception {
        User instructor = User.builder().id(2L).build();
        Cohort cohort = Cohort.builder()
                .id(15L)
                .code("MOD-15")
                .name("Module Test")
                .objectives("[\"Mục tiêu 1\", \"Mục tiêu 2\"]")
                .description("Mô tả test")
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(instructor));
        when(cohortService.getCohortByIdAndInstructor(15L, 2L)).thenReturn(cohort);
        when(courseService.getCoursesByInstructor(org.mockito.ArgumentMatchers.eq(2L), any())).thenReturn(org.springframework.data.domain.Page.empty());

        instructorCohortMvc.perform(get("/instructor/cohorts/15/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("instructor/cohorts/form"))
                .andExpect(model().attributeExists("cohortDto", "cohortId", "cohort"))
                .andExpect(result -> {
                    vn.edu.ute.utelearn.dto.CohortRequestDTO dto = (vn.edu.ute.utelearn.dto.CohortRequestDTO) result.getModelAndView().getModel().get("cohortDto");
                    org.junit.jupiter.api.Assertions.assertEquals("Mục tiêu 1\nMục tiêu 2", dto.getObjectives());
                    org.junit.jupiter.api.Assertions.assertEquals("Mô tả test", dto.getDescription());
                });
    }

    @Test
    void testShowStandaloneModuleDetailSuccess() throws Exception {
        Cohort standaloneCohort = Cohort.builder()
                .id(19L)
                .code("MOD-STANDALONE-19")
                .name("Chuyên đề Độc lập")
                .course(null)
                .price(BigDecimal.valueOf(150000))
                .build();

        when(cohortRepository.findById(19L)).thenReturn(Optional.of(standaloneCohort));
        when(cohortScheduleRepository.findByCohortIdWithDetails(19L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findSectionsForCohort(19L)).thenReturn(Collections.emptyList());

        courseMvc.perform(get("/courses/standalone/modules/19"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/module-detail"))
                .andExpect(model().attributeExists("course", "module", "moduleLessons"));
    }

    @Test
    void testShowCourseDetailExcludesFullCourseCohortFromModules() throws Exception {
        Cohort allCohort = Cohort.builder()
                .id(99L)
                .code("CS-JAVA-01-ALL")
                .name("Toàn bộ khóa học: Kiến Trúc Java Backend & Spring Boot Doanh Nghiệp")
                .course(sampleCourse)
                .price(BigDecimal.valueOf(990000))
                .status("IN_PROGRESS")
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findByCourseIdAndStatusInOrdered(anyLong(), anyList()))
                .thenReturn(List.of(sampleCohort, allCohort));
        when(cohortRepository.findByCourseIdOrdered(anyLong()))
                .thenReturn(List.of(sampleCohort, allCohort));

        courseMvc.perform(get("/courses/lap-trinh-web-co-ban"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/detail"))
                .andExpect(model().attribute("totalModulesCount", 1))
                .andExpect(result -> {
                    List<?> modules = (List<?>) result.getModelAndView().getModel().get("courseModules");
                    org.junit.jupiter.api.Assertions.assertEquals(1, modules.size());
                    List<?> available = (List<?>) result.getModelAndView().getModel().get("availableCohorts");
                    org.junit.jupiter.api.Assertions.assertEquals(1, available.size());
                });
    }
}

