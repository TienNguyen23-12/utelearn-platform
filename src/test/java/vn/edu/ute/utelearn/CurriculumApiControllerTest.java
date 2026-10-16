package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.ute.utelearn.controller.api.InstructorCurriculumApiController;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.InstructorAssetService;
import vn.edu.ute.utelearn.service.LessonService;
import vn.edu.ute.utelearn.service.SectionService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CurriculumApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SectionService sectionService;

    @Mock
    private LessonService lessonService;

    @Mock
    private AuthService authService;

    @Mock
    private InstructorAssetService assetService;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @InjectMocks
    private InstructorCurriculumApiController curriculumApiController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(curriculumApiController).build();
    }

    @Test
    void testGetFullCurriculumSuccess() throws Exception {
        Section section = Section.builder().id(10L).title("Chương 1").build();
        Lesson lesson = Lesson.builder().id(100L).title("Bài 1.1").lessonType("VIDEO").isFreePreview(false).build();
        Cohort cohort = Cohort.builder().id(5L).code("MOD-01").name("Module 1").build();
        CohortSchedule schedule = CohortSchedule.builder()
                .id(1L)
                .cohort(cohort)
                .lesson(lesson)
                .unlockAt(Instant.now())
                .deadlineAt(Instant.now().plusSeconds(3600))
                .showScoreType("IMMEDIATELY")
                .build();

        when(sectionService.getSectionsByCourseId(1L)).thenReturn(List.of(section));
        when(lessonService.getLessonsBySectionId(10L)).thenReturn(List.of(lesson));
        when(cohortScheduleRepository.findByLessonId(100L)).thenReturn(List.of(schedule));
        when(cohortScheduleRepository.findByCohortIdAndLessonId(5L, 100L)).thenReturn(Optional.of(schedule));

        mockMvc.perform(get("/api/instructor/courses/1/full-curriculum").param("cohortId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Chương 1"))
                .andExpect(jsonPath("$[0].lessons[0].id").value(100))
                .andExpect(jsonPath("$[0].lessons[0].title").value("Bài 1.1"))
                .andExpect(jsonPath("$[0].lessons[0].cohortId").value(5))
                .andExpect(jsonPath("$[0].lessons[0].cohortCode").value("MOD-01"))
                .andExpect(jsonPath("$[0].lessons[0].showScoreType").value("IMMEDIATELY"));
    }

    @Test
    void testGetLessonsSuccess() throws Exception {
        Lesson lesson = Lesson.builder().id(100L).title("Bài 1.1").lessonType("VIDEO").isFreePreview(true).build();
        Cohort cohort = Cohort.builder().id(5L).code("MOD-01").name("Module 1").build();
        CohortSchedule schedule = CohortSchedule.builder().id(1L).cohort(cohort).lesson(lesson).build();

        when(lessonService.getLessonsBySectionId(10L)).thenReturn(List.of(lesson));
        when(cohortScheduleRepository.findByLessonId(100L)).thenReturn(List.of(schedule));

        mockMvc.perform(get("/api/instructor/courses/sections/10/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].title").value("Bài 1.1"))
                .andExpect(jsonPath("$[0].isFreePreview").value(true))
                .andExpect(jsonPath("$[0].cohortCode").value("MOD-01"));
    }

    @Test
    void testCourseControllerRedirectToCurriculum() {
        vn.edu.ute.utelearn.controller.CourseController courseController = new vn.edu.ute.utelearn.controller.CourseController(
                authService, null, null, null, null, null, null
        );
        String redirect = courseController.redirectToCurriculum(12L);
        org.junit.jupiter.api.Assertions.assertEquals("redirect:/instructor/courses/12/curriculum", redirect);
    }
}
