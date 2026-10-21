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
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dao.SectionRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.service.CourseService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
public class CourseEditPermissionFeatureTest {

    private MockMvc mockMvc;

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
    private SectionRepository sectionRepository;

    @Mock
    private vn.edu.ute.utelearn.security.RbacHelper rbacHelper;

    @InjectMocks
    private CourseController courseController;

    private Course sampleCourse;
    private Cohort sampleCohort;
    private User courseOwner;
    private User moderator;
    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(courseController).setViewResolvers(resolver).build();

        courseOwner = User.builder()
                .id(10L)
                .email("owner@ute.edu.vn")
                .fullName("Giảng viên Tác giả")
                .roles(Set.of(Role.builder().code("INSTRUCTOR").name("Giảng viên").build()))
                .build();

        moderator = User.builder()
                .id(20L)
                .email("moderator@ute.edu.vn")
                .fullName("Kiểm duyệt viên")
                .roles(Set.of(Role.builder().code("MODERATOR").name("Kiểm duyệt viên").build()))
                .build();

        admin = User.builder()
                .id(30L)
                .email("admin@ute.edu.vn")
                .fullName("Quản trị viên")
                .roles(Set.of(Role.builder().code("ADMIN").name("Quản trị viên").build()))
                .build();

        student = User.builder()
                .id(40L)
                .email("student@ute.edu.vn")
                .fullName("Học viên")
                .roles(Set.of(Role.builder().code("STUDENT").name("Học viên").build()))
                .build();

        sampleCourse = Course.builder()
                .id(1L)
                .title("Lập trình Web Cơ bản")
                .slug("lap-trinh-web-co-ban")
                .code("CS-WEB-01")
                .price(BigDecimal.valueOf(699000))
                .createdBy(courseOwner)
                .build();

        sampleCohort = Cohort.builder()
                .id(5L)
                .name("Module HTML5 CSS3")
                .code("MOD-WEB-01")
                .price(BigDecimal.valueOf(250000))
                .course(sampleCourse)
                .createdBy(courseOwner)
                .build();
    }

    @Test
    void testModeratorViewingCourseHasAccessRoleButCannotEdit() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(moderator));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findByCourseIdAndStatusInOrdered(anyLong(), anyList())).thenReturn(List.of(sampleCohort));
        when(cohortRepository.findByCourseIdOrdered(1L)).thenReturn(List.of(sampleCohort));
        when(lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/courses/lap-trinh-web-co-ban"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/detail"))
                .andExpect(model().attribute("hasAccessRole", true))
                .andExpect(model().attribute("canEdit", false));
    }

    @Test
    void testOwnerViewingCourseHasAccessRoleAndCanEdit() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(courseOwner));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findByCourseIdAndStatusInOrdered(anyLong(), anyList())).thenReturn(List.of(sampleCohort));
        when(cohortRepository.findByCourseIdOrdered(1L)).thenReturn(List.of(sampleCohort));
        when(lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/courses/lap-trinh-web-co-ban"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/detail"))
                .andExpect(model().attribute("hasAccessRole", true))
                .andExpect(model().attribute("canEdit", true));
    }

    @Test
    void testAdminViewingCourseHasAccessRoleAndCanEdit() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(admin));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortRepository.findByCourseIdAndStatusInOrdered(anyLong(), anyList())).thenReturn(List.of(sampleCohort));
        when(cohortRepository.findByCourseIdOrdered(1L)).thenReturn(List.of(sampleCohort));
        when(lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/courses/lap-trinh-web-co-ban"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/detail"))
                .andExpect(model().attribute("hasAccessRole", true))
                .andExpect(model().attribute("canEdit", true));
    }

    @Test
    void testModeratorViewingModuleHasAccessRoleButCannotEdit() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(moderator));
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(sampleCohort));
        when(cohortScheduleRepository.findByCohortIdWithDetails(5L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findSectionsForCohort(5L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());
        when(lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/courses/lap-trinh-web-co-ban/modules/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/module-detail"))
                .andExpect(model().attribute("hasAccessRole", true))
                .andExpect(model().attribute("canEdit", false));
    }

    @Test
    void testOwnerViewingModuleHasAccessRoleAndCanEdit() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(courseOwner));
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(sampleCohort));
        when(cohortScheduleRepository.findByCohortIdWithDetails(5L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findSectionsForCohort(5L)).thenReturn(Collections.emptyList());
        when(sectionRepository.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());
        when(lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/courses/lap-trinh-web-co-ban/modules/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/module-detail"))
                .andExpect(model().attribute("hasAccessRole", true))
                .andExpect(model().attribute("canEdit", true));
    }

    @Test
    void testModeratorAccessModuleLearnWithoutEnrollment_Success() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(moderator));
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(sampleCohort));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(5L, 20L)).thenReturn(false);
        when(cohortScheduleRepository.findByCohortIdWithDetails(5L)).thenReturn(Collections.emptyList());
        when(lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/courses/lap-trinh-web-co-ban/modules/5/learn"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/module-learn"))
                .andExpect(model().attribute("hasAccessRole", true))
                .andExpect(model().attribute("canEdit", false));
    }

    @Test
    void testStudentAccessModuleLearnWithoutEnrollment_RedirectsWithError() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(student));
        when(cohortRepository.findById(5L)).thenReturn(Optional.of(sampleCohort));
        when(courseService.getCourseBySlug("lap-trinh-web-co-ban")).thenReturn(sampleCourse);
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(5L, 40L)).thenReturn(false);

        mockMvc.perform(get("/courses/lap-trinh-web-co-ban/modules/5/learn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/lap-trinh-web-co-ban/modules/5"))
                .andExpect(flash().attribute("error", "Bạn cần đăng ký module này để vào phòng học."));
    }
}
