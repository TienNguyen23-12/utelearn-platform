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
import vn.edu.ute.utelearn.controller.moderator.ModeratorController;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.security.RbacHelper;
import vn.edu.ute.utelearn.service.AttendanceService;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.LessonService;
import vn.edu.ute.utelearn.service.SectionService;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
public class ModeratorReviewPageRenderTest {

    private MockMvc mockMvc;

    @Mock
    private ModerationTaskRepository taskRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private AuthService authService;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private SectionService sectionService;

    @Mock
    private LessonService lessonService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RbacHelper rbacHelper;

    @InjectMocks
    private ModeratorController moderatorController;

    @BeforeEach
    void setup() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders
                .standaloneSetup(moderatorController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void testRenderTask3AsAdmin() throws Exception {
        User admin = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@ute.edu.vn")
                .fullName("Administrator")
                .build();

        Course course = Course.builder()
                .id(10L)
                .title("Sample Course")
                .slug("sample-course")
                .build();

        ModerationTask task = ModerationTask.builder()
                .id(3L)
                .itemType("COURSE")
                .itemId(10L)
                .status("PENDING")
                .author(User.builder().id(2L).fullName("Instructor").build())
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(admin));
        when(rbacHelper.hasRole("ADMIN")).thenReturn(true);
        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(userRepository.findActiveModerators()).thenReturn(Collections.emptyList());
        when(courseService.getCourseById(10L)).thenReturn(course);
        when(sectionService.getSectionsByCourseId(10L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/moderator/review/3"))
                .andExpect(status().isOk())
                .andExpect(view().name("moderator/review_course"))
                .andExpect(model().attributeExists("task"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("moderators"));
    }
}
