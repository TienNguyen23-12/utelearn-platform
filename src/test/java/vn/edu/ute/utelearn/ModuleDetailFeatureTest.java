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
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
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

    @Mock
    private vn.edu.ute.utelearn.service.GradebookService gradebookService;

    @Mock
    private vn.edu.ute.utelearn.dao.EnrollmentRepository enrollmentRepository;

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

        org.springframework.test.util.ReflectionTestUtils.setField(courseController, "gradebookService", gradebookService);
        org.springframework.test.util.ReflectionTestUtils.setField(courseController, "enrollmentRepository", enrollmentRepository);
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

    @Test
    void testGeneralLearnRedirectsToLoginWhenAnonymous() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());

        homeMvc.perform(get("/learn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testGeneralLearnRedirectsToModuleWhenEnrolled() throws Exception {
        User user = User.builder().id(10L).fullName("Test Student").email("test@utelearn.edu.vn").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(user));

        vn.edu.ute.utelearn.entity.Lesson lesson = vn.edu.ute.utelearn.entity.Lesson.builder().id(55L).build();
        vn.edu.ute.utelearn.entity.CohortMember member = vn.edu.ute.utelearn.entity.CohortMember.builder()
                .id(1L)
                .user(user)
                .cohort(sampleCohort)
                .lastLesson(lesson)
                .build();

        when(cohortMemberRepository.findByUserOrderByEnrolledAtDesc(user)).thenReturn(List.of(member));

        homeMvc.perform(get("/learn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/lap-trinh-web-co-ban/modules/1/learn?lesson=55"));
    }

    @Test
    void testGeneralLearnRedirectsToMyCoursesWhenNoEnrollment() throws Exception {
        User user = User.builder().id(10L).fullName("Test Student").email("test@utelearn.edu.vn").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(user));
        when(cohortMemberRepository.findByUserOrderByEnrolledAtDesc(user)).thenReturn(Collections.emptyList());

        homeMvc.perform(get("/learn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/my-courses"));
    }

    @Test
    void testShowModuleDetailIncludesStudentGradeWhenEnrolled() throws Exception {
        User user = User.builder().id(10L).fullName("Test Student").email("test@utelearn.edu.vn").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(user));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(1L, 10L)).thenReturn(true);
        when(cohortScheduleRepository.findByCohortIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findSectionsForCohort(1L)).thenReturn(Collections.emptyList());

        vn.edu.ute.utelearn.dto.StudentGradeItemDTO studentGrade = vn.edu.ute.utelearn.dto.StudentGradeItemDTO.builder()
                .userId(10L)
                .fullName("Test Student")
                .finalGrade(BigDecimal.valueOf(8.5))
                .isPassed(true)
                .build();
        when(gradebookService.getStudentGrade(1L, 10L)).thenReturn(studentGrade);

        courseMvc.perform(get("/courses/lap-trinh-web-co-ban/modules/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/module-detail"))
                .andExpect(model().attributeExists("studentGrade"))
                .andExpect(model().attribute("studentGrade", studentGrade));
    }

    @Test
    void testModuleDetailThymeleafRenderingWithGradeAndReviewPrompt() throws Exception {
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
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(sampleCohort));
        when(cohortScheduleRepository.findByCohortIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findSectionsForCohort(1L)).thenReturn(Collections.emptyList());

        mvcWithThymeleaf.perform(get("/courses/lap-trinh-web-co-ban/modules/1"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpcomingSchedulesExcludesPassedDeadlines() throws Exception {
        vn.edu.ute.utelearn.entity.Lesson l1 = vn.edu.ute.utelearn.entity.Lesson.builder().id(101L).title("Bài tập quá hạn").build();
        vn.edu.ute.utelearn.entity.Lesson l2 = vn.edu.ute.utelearn.entity.Lesson.builder().id(102L).title("Bài tập sắp tới").build();

        vn.edu.ute.utelearn.entity.CohortSchedule pastSchedule = vn.edu.ute.utelearn.entity.CohortSchedule.builder()
                .id(1L).cohort(sampleCohort).lesson(l1)
                .unlockAt(java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.DAYS))
                .deadlineAt(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS))
                .build();

        vn.edu.ute.utelearn.entity.CohortSchedule futureSchedule = vn.edu.ute.utelearn.entity.CohortSchedule.builder()
                .id(2L).cohort(sampleCohort).lesson(l2)
                .unlockAt(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS))
                .deadlineAt(java.time.Instant.now().plus(2, java.time.temporal.ChronoUnit.DAYS))
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(sampleCohort));
        when(cohortScheduleRepository.findByCohortIdWithDetails(1L)).thenReturn(List.of(pastSchedule, futureSchedule));
        when(sectionRepository.findSectionsForCohort(1L)).thenReturn(Collections.emptyList());

        courseMvc.perform(get("/courses/lap-trinh-web-co-ban/modules/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("upcomingSchedules", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("upcomingSchedules", org.hamcrest.Matchers.contains(futureSchedule)));
    }

    @Test
    void testShowModuleDetailIncludesReviewAttributes() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(sampleCohort));
        when(cohortScheduleRepository.findByCohortIdWithDetails(1L)).thenReturn(Collections.emptyList());
        when(cohortMemberRepository.getAverageRatingByCohortId(1L)).thenReturn(4.5);
        when(cohortMemberRepository.countRatingsByCohortId(1L)).thenReturn(2L);
        when(cohortMemberRepository.findReviewsByCohortIdWithUser(1L)).thenReturn(Collections.emptyList());

        courseMvc.perform(get("/courses/lap-trinh-web-co-ban/modules/1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("moduleRating", "totalReviewsCount", "moduleReviews", "ratingBreakdown"))
                .andExpect(model().attribute("moduleRating", 4.5))
                .andExpect(model().attribute("totalReviewsCount", 2L));
    }

    @Test
    void testSubmitCohortReview_Success() throws Exception {
        User student = User.builder().id(99L).fullName("Sinh Vien Test").build();
        vn.edu.ute.utelearn.entity.CohortMember member = vn.edu.ute.utelearn.entity.CohortMember.builder()
                .id(10L)
                .user(student)
                .cohort(sampleCohort)
                .isPassed(false)
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(student));
        when(cohortMemberRepository.findByCohort_IdAndUser_Id(1L, 99L)).thenReturn(Optional.of(member));

        String jsonPayload = """
                {
                    "rating": 5,
                    "comment": "Môn học rất thực tế và hữu ích!"
                }
                """;

        courseMvc.perform(post("/courses/lap-trinh-web-co-ban/modules/1/review")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cảm ơn bạn đã gửi đánh giá cho môn học!"));
    }

    @Test
    void testSubmitCohortReview_Unauthenticated() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());

        String jsonPayload = """
                {
                    "rating": 5,
                    "comment": "Môn học tốt"
                }
                """;

        courseMvc.perform(post("/courses/lap-trinh-web-co-ban/modules/1/review")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSubmitCohortReview_NotEnrolled() throws Exception {
        User student = User.builder().id(99L).fullName("Sinh Vien Test").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(student));
        when(cohortMemberRepository.findByCohort_IdAndUser_Id(1L, 99L)).thenReturn(Optional.empty());

        String jsonPayload = """
                {
                    "rating": 5,
                    "comment": "Môn học tốt"
                }
                """;

        courseMvc.perform(post("/courses/lap-trinh-web-co-ban/modules/1/review")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    void testShowCourseDetailIncludesRatingBreakdown() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findByCourseIdAndStatusInOrdered(anyLong(), anyList()))
                .thenReturn(List.of(sampleCohort));
        when(cohortRepository.findByCourseIdOrdered(anyLong()))
                .thenReturn(List.of(sampleCohort));
        when(enrollmentRepository.countRatingsByCourseId(anyLong())).thenReturn(2L);
        when(enrollmentRepository.getAverageRatingByCourseId(anyLong())).thenReturn(4.5);

        vn.edu.ute.utelearn.entity.Enrollment r1 = vn.edu.ute.utelearn.entity.Enrollment.builder()
                .id(101L)
                .rating(5)
                .reviewComment("Khóa học rất hay!")
                .reviewedAt(java.time.Instant.now())
                .build();
        vn.edu.ute.utelearn.entity.Enrollment r2 = vn.edu.ute.utelearn.entity.Enrollment.builder()
                .id(102L)
                .rating(4)
                .reviewComment("Rất bổ ích!")
                .reviewedAt(java.time.Instant.now())
                .build();
        when(enrollmentRepository.findReviewsByCourseIdWithUser(anyLong()))
                .thenReturn(List.of(r1, r2));

        courseMvc.perform(get("/courses/lap-trinh-web-co-ban"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/detail"))
                .andExpect(model().attributeExists("courseRating", "totalReviewsCount", "courseReviews", "ratingBreakdown"));
    }

    @Test
    void testSubmitCourseReview_Success() throws Exception {
        User student = User.builder().id(99L).fullName("Sinh Vien Test").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(student));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);

        vn.edu.ute.utelearn.entity.Enrollment enrollment = vn.edu.ute.utelearn.entity.Enrollment.builder()
                .id(1L)
                .user(student)
                .course(sampleCourse)
                .isCompleted(true)
                .build();
        when(enrollmentRepository.findByUser_IdAndCourse_Id(99L, sampleCourse.getId()))
                .thenReturn(Optional.of(enrollment));

        String jsonPayload = """
                {
                    "rating": 5,
                    "comment": "Khóa học tuyệt vời!"
                }
                """;

        courseMvc.perform(post("/courses/lap-trinh-web-co-ban/review")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cảm ơn bạn đã gửi đánh giá cho khóa học!"));
    }

    @Test
    void testSubmitCourseReview_Unauthenticated() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());

        String jsonPayload = """
                {
                    "rating": 5,
                    "comment": "Khóa học tốt"
                }
                """;

        courseMvc.perform(post("/courses/lap-trinh-web-co-ban/review")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSubmitCourseReview_NotCompleted() throws Exception {
        User student = User.builder().id(99L).fullName("Sinh Vien Test").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(student));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);

        vn.edu.ute.utelearn.entity.Enrollment enrollment = vn.edu.ute.utelearn.entity.Enrollment.builder()
                .id(1L)
                .user(student)
                .course(sampleCourse)
                .isCompleted(false)
                .build();
        when(enrollmentRepository.findByUser_IdAndCourse_Id(99L, sampleCourse.getId()))
                .thenReturn(Optional.of(enrollment));
        when(cohortRepository.findByCourseIdOrdered(sampleCourse.getId()))
                .thenReturn(List.of(sampleCohort));
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(sampleCourse.getId(), 99L))
                .thenReturn(Collections.emptyList());

        String jsonPayload = """
                {
                    "rating": 5,
                    "comment": "Khóa học tốt"
                }
                """;

        courseMvc.perform(post("/courses/lap-trinh-web-co-ban/review")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Bạn cần hoàn thành toàn bộ khóa học để có thể gửi đánh giá"));
    }

    @Test
    void testSubmitCourseReview_NotEnrolled() throws Exception {
        User student = User.builder().id(99L).fullName("Sinh Vien Test").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(student));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(enrollmentRepository.findByUser_IdAndCourse_Id(99L, sampleCourse.getId()))
                .thenReturn(Optional.empty());
        when(cohortRepository.findByCourseIdOrdered(sampleCourse.getId()))
                .thenReturn(Collections.emptyList());

        String jsonPayload = """
                {
                    "rating": 5,
                    "comment": "Khóa học tốt"
                }
                """;

        courseMvc.perform(post("/courses/lap-trinh-web-co-ban/review")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReviewTimestampFormattedGmt7() {
        java.time.Instant utcInstant = java.time.Instant.parse("2026-10-28T20:00:00Z");
        vn.edu.ute.utelearn.entity.Enrollment enrollment = vn.edu.ute.utelearn.entity.Enrollment.builder()
                .reviewedAt(utcInstant)
                .build();
        vn.edu.ute.utelearn.entity.CohortMember member = vn.edu.ute.utelearn.entity.CohortMember.builder()
                .reviewedAt(utcInstant)
                .build();
        vn.edu.ute.utelearn.dto.StudentGradeItemDTO dto = new vn.edu.ute.utelearn.dto.StudentGradeItemDTO();
        dto.setReviewedAt(utcInstant);

        org.junit.jupiter.api.Assertions.assertEquals("29/10/2026 03:00", enrollment.getFormattedReviewedAt());
        org.junit.jupiter.api.Assertions.assertEquals("29/10/2026 03:00", member.getFormattedReviewedAt());
        org.junit.jupiter.api.Assertions.assertEquals("29/10/2026 03:00", dto.getFormattedReviewedAt());

        enrollment.setReviewedAt(null);


        enrollment.setReviewedAt(null);
        org.junit.jupiter.api.Assertions.assertEquals("", enrollment.getFormattedReviewedAt());
    }

    @Test
    void testGetFeaturedCohortsRankingFormula() {
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

        User standaloneCreator = User.builder().id(5L).fullName("TS. Nguyễn Văn A").build();
        Cohort newEmptyCohort = Cohort.builder()
                .id(100L)
                .name("Triết học Marx - Lenin")
                .status("UPCOMING")
                .maxCapacity(50)
                .createdBy(standaloneCreator)
                .createdAt(java.time.Instant.now())
                .build();

        Cohort popularRatedCohort = Cohort.builder()
                .id(50L)
                .name("Nhập môn Cơ sở dữ liệu")
                .status("ENROLLING")
                .maxCapacity(40)
                .course(sampleCourse)
                .enrollmentStart(java.time.Instant.now().minusSeconds(3600))
                .enrollmentEnd(java.time.Instant.now().plusSeconds(86400))
                .studyStart(java.time.Instant.now().plusSeconds(172800))
                .createdAt(java.time.Instant.now().minusSeconds(86400 * 10))
                .build();

        org.springframework.data.domain.Page<Cohort> mockPage =
                new org.springframework.data.domain.PageImpl<>(List.of(newEmptyCohort, popularRatedCohort));
        when(cohortRepository.searchPublicCohorts(org.mockito.ArgumentMatchers.eq(""), org.mockito.ArgumentMatchers.eq(""), any()))
                .thenReturn(mockPage);

        when(cohortMemberRepository.countByCohort_Id(100L)).thenReturn(0L);
        when(cohortMemberRepository.getAverageRatingByCohortId(100L)).thenReturn(null);
        when(cohortMemberRepository.countRatingsByCohortId(100L)).thenReturn(0L);

        when(cohortMemberRepository.countByCohort_Id(50L)).thenReturn(12L);
        when(cohortMemberRepository.getAverageRatingByCohortId(50L)).thenReturn(4.8);
        when(cohortMemberRepository.countRatingsByCohortId(50L)).thenReturn(5L);

        List<Cohort> featured = cohortServiceImpl.getFeaturedCohorts(4);

        org.junit.jupiter.api.Assertions.assertEquals(2, featured.size());
        org.junit.jupiter.api.Assertions.assertEquals(50L, featured.get(0).getId());
        org.junit.jupiter.api.Assertions.assertEquals(12L, featured.get(0).getEnrolledCount());
        org.junit.jupiter.api.Assertions.assertEquals(4.8, featured.get(0).getAverageRating());
        org.junit.jupiter.api.Assertions.assertEquals(5L, featured.get(0).getRatingCount());
        org.junit.jupiter.api.Assertions.assertTrue(featured.get(0).getFeaturedScore() > featured.get(1).getFeaturedScore());
        org.junit.jupiter.api.Assertions.assertEquals("TS. Nguyễn Văn A", featured.get(1).getInstructorDisplayName());
    }

    @Test
    void testGetFeaturedCohortsWithTimeframeWeekAndMonth() {
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

        User standaloneCreator = User.builder().id(5L).fullName("TS. Nguyễn Văn A").build();
        Cohort newEmptyCohort = Cohort.builder()
                .id(100L)
                .name("Triết học Marx - Lenin")
                .status("UPCOMING")
                .maxCapacity(50)
                .createdBy(standaloneCreator)
                .createdAt(java.time.Instant.now())
                .build();

        Cohort popularRatedCohort = Cohort.builder()
                .id(50L)
                .name("Nhập môn Cơ sở dữ liệu")
                .status("ENROLLING")
                .maxCapacity(40)
                .course(sampleCourse)
                .createdAt(java.time.Instant.now().minusSeconds(86400 * 2))
                .build();

        org.springframework.data.domain.Page<Cohort> mockPage =
                new org.springframework.data.domain.PageImpl<>(List.of(newEmptyCohort, popularRatedCohort));
        when(cohortRepository.searchPublicCohorts(org.mockito.ArgumentMatchers.eq(""), org.mockito.ArgumentMatchers.eq(""), any()))
                .thenReturn(mockPage);

        when(cohortMemberRepository.countByCohort_Id(100L)).thenReturn(0L);
        when(cohortMemberRepository.getAverageRatingByCohortId(100L)).thenReturn(null);
        when(cohortMemberRepository.countRatingsByCohortId(100L)).thenReturn(0L);

        when(cohortMemberRepository.countByCohort_Id(50L)).thenReturn(12L);
        when(cohortMemberRepository.getAverageRatingByCohortId(50L)).thenReturn(4.8);
        when(cohortMemberRepository.countRatingsByCohortId(50L)).thenReturn(5L);

        when(cohortMemberRepository.countEnrolledByCohortIdSince(eq(50L), any())).thenReturn(6L);
        when(cohortMemberRepository.getAverageRatingByCohortIdSince(eq(50L), any())).thenReturn(4.9);
        when(cohortMemberRepository.countRatingsByCohortIdSince(eq(50L), any())).thenReturn(3L);

        List<Cohort> featuredWeek = cohortServiceImpl.getFeaturedCohorts(4, "week");
        org.junit.jupiter.api.Assertions.assertEquals(2, featuredWeek.size());
        org.junit.jupiter.api.Assertions.assertEquals(50L, featuredWeek.get(0).getId());

        List<Cohort> featuredMonth = cohortServiceImpl.getFeaturedCohorts(4, "month");
        org.junit.jupiter.api.Assertions.assertEquals(2, featuredMonth.size());
        org.junit.jupiter.api.Assertions.assertEquals(50L, featuredMonth.get(0).getId());
    }

    @Test
    void testHomeControllerGetFeaturedCohortsApi() throws Exception {
        Cohort cohort1 = Cohort.builder()
                .id(1L)
                .name("Lập trình Web Cơ bản")
                .code("MOD-01")
                .price(BigDecimal.valueOf(200000))
                .course(sampleCourse)
                .build();
        cohort1.setEnrolledCount(15L);
        cohort1.setAverageRating(4.9);
        cohort1.setRatingCount(8L);

        when(cohortService.getFeaturedCohorts(4, "week")).thenReturn(List.of(cohort1));

        homeMvc.perform(get("/api/cohorts/featured").param("timeframe", "week").param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Lập trình Web Cơ bản"))
                .andExpect(jsonPath("$[0].enrolledCount").value(15))
                .andExpect(jsonPath("$[0].averageRating").value(4.9))
                .andExpect(jsonPath("$[0].ratingCount").value(8))
                .andExpect(jsonPath("$[0].formattedPrice").value("200.000 VNĐ"));
    }
}
