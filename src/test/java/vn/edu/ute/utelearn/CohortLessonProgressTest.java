package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.ute.utelearn.controller.api.CohortApiController;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.LessonCompletionRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dao.SectionRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.LessonCompletion;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.service.impl.CohortServiceImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class CohortLessonProgressTest {

    private MockMvc mockMvc;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private LessonCompletionRepository lessonCompletionRepository;

    @Mock
    private AuthService authService;

    @Mock
    private CohortService cohortService;

    @InjectMocks
    private CohortApiController cohortApiController;

    private CohortServiceImpl cohortServiceImpl;

    private User sampleUser;
    private Cohort sampleCohort;
    private Lesson sampleLesson;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cohortApiController).build();

        cohortServiceImpl = new CohortServiceImpl(
                cohortRepository,
                cohortScheduleRepository,
                null,
                lessonRepository,
                cohortMemberRepository,
                sectionRepository,
                lessonCompletionRepository,
                null
        );

        sampleUser = User.builder().id(100L).username("student1").email("student1@ute.edu.vn").build();
        sampleCohort = Cohort.builder().id(1L).code("MOD-01").build();
        sampleLesson = Lesson.builder().id(10L).title("Lesson 1").build();
    }

    @Test
    void testScheduleLockedStatus() {
        CohortSchedule lockedSchedule = CohortSchedule.builder()
                .unlockAt(Instant.now().plus(2, ChronoUnit.DAYS))
                .build();
        assertTrue(lockedSchedule.isLockedNow());

        CohortSchedule unlockedSchedule = CohortSchedule.builder()
                .unlockAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build();
        assertFalse(unlockedSchedule.isLockedNow());
    }

    @Test
    void testToggleLessonCompletionWhenNotCompleted() {
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(sampleCohort));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(sampleLesson));
        when(lessonCompletionRepository.existsByCohort_IdAndLesson_IdAndUser_Id(1L, 10L, 100L))
                .thenReturn(false);
        when(cohortScheduleRepository.findLessonIdsByCohortId(1L)).thenReturn(List.of(10L));
        when(lessonCompletionRepository.countByCohort_IdAndUser_Id(1L, 100L)).thenReturn(1L);

        CohortMember member = CohortMember.builder().id(50L).cohort(sampleCohort).user(sampleUser).isPassed(false).build();
        when(cohortMemberRepository.findByCohort_IdAndUser_Id(1L, 100L)).thenReturn(Optional.of(member));

        Map<String, Object> result = cohortServiceImpl.toggleLessonCompletion(1L, 10L, sampleUser);

        assertTrue((Boolean) result.get("success"));
        assertTrue((Boolean) result.get("completed"));
        assertEquals(100, result.get("progressPercent"));
        assertTrue((Boolean) result.get("isPassed"));
        verify(lessonCompletionRepository).save(any(LessonCompletion.class));
        verify(cohortMemberRepository).save(argThat(m -> Boolean.TRUE.equals(m.getIsPassed())));
    }

    @Test
    void testToggleLessonCompletionWhenAlreadyCompleted() {
        when(cohortRepository.findById(1L)).thenReturn(Optional.of(sampleCohort));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(sampleLesson));
        when(lessonCompletionRepository.existsByCohort_IdAndLesson_IdAndUser_Id(1L, 10L, 100L))
                .thenReturn(true);
        when(cohortScheduleRepository.findLessonIdsByCohortId(1L)).thenReturn(List.of(10L));
        when(lessonCompletionRepository.countByCohort_IdAndUser_Id(1L, 100L)).thenReturn(0L);

        CohortMember member = CohortMember.builder().id(50L).cohort(sampleCohort).user(sampleUser).isPassed(true).build();
        when(cohortMemberRepository.findByCohort_IdAndUser_Id(1L, 100L)).thenReturn(Optional.of(member));

        Map<String, Object> result = cohortServiceImpl.toggleLessonCompletion(1L, 10L, sampleUser);

        assertTrue((Boolean) result.get("success"));
        assertFalse((Boolean) result.get("completed"));
        assertEquals(0, result.get("progressPercent"));
        assertFalse((Boolean) result.get("isPassed"));
        verify(lessonCompletionRepository).deleteByCohortIdAndLessonIdAndUserId(1L, 10L, 100L);
    }

    @Test
    void testGetCohortProgressCalculation() {
        when(lessonCompletionRepository.findCompletedLessonIds(1L, 100L)).thenReturn(List.of(10L));
        when(cohortScheduleRepository.findLessonIdsByCohortId(1L)).thenReturn(List.of(10L, 20L));

        Map<String, Object> progress = cohortServiceImpl.getCohortProgress(1L, 100L);

        assertEquals(1, progress.get("completedCount"));
        assertEquals(2, progress.get("totalCount"));
        assertEquals(50, progress.get("progressPercent"));
    }

    @Test
    void testToggleCompleteApiEndpoint() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));
        when(cohortService.toggleLessonCompletion(1L, 10L, sampleUser)).thenReturn(Map.of(
                "success", true,
                "completed", true,
                "completedCount", 1L,
                "totalCount", 2,
                "progressPercent", 50,
                "isPassed", false
        ));

        mockMvc.perform(post("/api/cohorts/1/lessons/10/toggle-complete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.progressPercent").value(50))
                .andExpect(jsonPath("$.completedCount").value(1));
    }

    @Test
    void testGetCohortProgressApiEndpoint() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));
        when(cohortService.getCohortProgress(1L, 100L)).thenReturn(Map.of(
                "completedCount", 2,
                "totalCount", 2,
                "progressPercent", 100,
                "completedLessonIds", List.of(10L, 20L)
        ));

        mockMvc.perform(get("/api/cohorts/1/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(2))
                .andExpect(jsonPath("$.progressPercent").value(100))
                .andExpect(jsonPath("$.completedLessonIds[0]").value(10));
    }

    @Test
    void testCreateStandaloneCohortWithoutCourse() {
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vn.edu.ute.utelearn.dto.CohortRequestDTO dto = new vn.edu.ute.utelearn.dto.CohortRequestDTO();
        dto.setName("Module Độc Lập");
        dto.setCourseId(null);

        Cohort created = cohortServiceImpl.createCohort(dto, sampleUser);
        assertNotNull(created);
        assertNull(created.getCourse());
        assertEquals("Module Độc Lập", created.getName());
        assertEquals("DRAFT", created.getModerationStatus());
    }

    @Test
    void testUpdateStandaloneCohortWithoutCourse() {
        when(cohortRepository.findByIdAndInstructorId(1L, 100L)).thenReturn(Optional.of(sampleCohort));
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vn.edu.ute.utelearn.dto.CohortRequestDTO dto = new vn.edu.ute.utelearn.dto.CohortRequestDTO();
        dto.setName("Module Độc Lập Updated");
        dto.setCode("MOD-01");
        dto.setCourseId(null);

        Cohort updated = cohortServiceImpl.updateCohort(1L, dto, sampleUser);
        assertNotNull(updated);
        assertNull(updated.getCourse());
        assertEquals("Module Độc Lập Updated", updated.getName());
    }

    @Test
    void testUpdateStandaloneCohort_RevertsToDraftWhenApproved() {
        Cohort approvedCohort = Cohort.builder()
                .id(2L)
                .code("MOD-APPROVED")
                .name("Module Approved")
                .moderationStatus("APPROVED")
                .build();

        when(cohortRepository.findByIdAndInstructorId(2L, 100L)).thenReturn(Optional.of(approvedCohort));
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vn.edu.ute.utelearn.dto.CohortRequestDTO dto = new vn.edu.ute.utelearn.dto.CohortRequestDTO();
        dto.setName("Module Approved Renamed");
        dto.setCode("MOD-APPROVED");
        dto.setCourseId(null);

        Cohort updated = cohortServiceImpl.updateCohort(2L, dto, sampleUser);
        assertNotNull(updated);
        assertEquals("DRAFT", updated.getModerationStatus());
    }

    @Test
    void testRevertToDraftIfApproved_UpdatesStatusWhenApproved() {
        Cohort approvedCohort = Cohort.builder()
                .id(3L)
                .code("MOD-APPROVED-2")
                .moderationStatus("APPROVED")
                .build();

        when(cohortRepository.findById(3L)).thenReturn(Optional.of(approvedCohort));

        cohortServiceImpl.revertToDraftIfApproved(3L);

        assertEquals("DRAFT", approvedCohort.getModerationStatus());
        verify(cohortRepository).save(approvedCohort);
    }

    @Test
    void testGetPublicCohorts_ReturnsOkWithCohortList() throws Exception {
        Cohort cohort = Cohort.builder()
                .id(1L)
                .code("MOD-01")
                .name("Module Java Web")
                .status("ENROLLING")
                .visibility("PUBLIC")
                .price(java.math.BigDecimal.valueOf(150000))
                .build();

        org.springframework.data.domain.Page<Cohort> page = new org.springframework.data.domain.PageImpl<>(
                List.of(cohort),
                org.springframework.data.domain.PageRequest.of(0, 8),
                1
        );

        when(cohortService.searchPublicCohorts(any(), any(), any())).thenReturn(page);
        when(cohortMemberRepository.countByCohort_Id(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/cohorts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Module Java Web"))
                .andExpect(jsonPath("$.content[0].currentEnrolled").value(5))
                .andExpect(jsonPath("$.totalItems").value(1));
    }
}
