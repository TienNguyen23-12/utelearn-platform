package vn.edu.ute.utelearn;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.LessonCompletionRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dao.SectionRepository;
import vn.edu.ute.utelearn.dto.CohortRequestDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.impl.CohortServiceImpl;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CohortAutoCodeFeatureTest {

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private LessonCompletionRepository lessonCompletionRepository;

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CohortServiceImpl cohortService;

    @Test
    void testCreateCohortWithoutCode_WithCourse_AutoGeneratesCode() {
        User instructor = User.builder().id(10L).fullName("Instructor A").build();
        Course course = Course.builder().id(1L).code("CRS-JAVA").createdBy(instructor).build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(cohortRepository.countByCourseId(1L)).thenReturn(0L);
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CohortRequestDTO request = new CohortRequestDTO();
        request.setName("Lop Java K1");
        request.setCourseId(1L);
        request.setPrice(BigDecimal.valueOf(200000));
        request.setCode("");

        Cohort saved = cohortService.createCohort(request, instructor);

        String expectedCode = String.format("CRS-JAVA-%s-01", Year.now().getValue());
        Assertions.assertEquals(expectedCode, saved.getCode());
    }

    @Test
    void testCreateCohortWithoutCode_Independent_AutoGeneratesCode() {
        User instructor = User.builder().id(10L).fullName("Instructor A").build();

        when(cohortRepository.count()).thenReturn(0L);
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CohortRequestDTO request = new CohortRequestDTO();
        request.setName("Module doc lap");
        request.setPrice(BigDecimal.valueOf(150000));
        request.setCode(null);

        Cohort saved = cohortService.createCohort(request, instructor);

        String expectedCode = String.format("MOD-%s-01", Year.now().getValue());
        Assertions.assertEquals(expectedCode, saved.getCode());
    }

    @Test
    void testCreateCohortWithExistingAutoCode_IncrementsSequence() {
        User instructor = User.builder().id(10L).fullName("Instructor A").build();
        String year = String.valueOf(Year.now().getValue());
        String code1 = String.format("MOD-%s-01", year);
        String code2 = String.format("MOD-%s-02", year);

        when(cohortRepository.count()).thenReturn(0L);
        when(cohortRepository.existsByCode(code1)).thenReturn(true);
        when(cohortRepository.existsByCode(code2)).thenReturn(false);
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CohortRequestDTO request = new CohortRequestDTO();
        request.setName("Module tiep theo");
        request.setCode("");

        Cohort saved = cohortService.createCohort(request, instructor);

        Assertions.assertEquals(code2, saved.getCode());
    }

    @Test
    void testCreateCohortWithCustomCode_RetainsCustomCode() {
        User instructor = User.builder().id(10L).fullName("Instructor A").build();

        when(cohortRepository.existsByCode("MOD-CUSTOM-99")).thenReturn(false);
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CohortRequestDTO request = new CohortRequestDTO();
        request.setName("Module rieng");
        request.setCode("MOD-CUSTOM-99");

        Cohort saved = cohortService.createCohort(request, instructor);

        Assertions.assertEquals("MOD-CUSTOM-99", saved.getCode());
    }

    @Test
    void testCreateCohortWithDuplicateCustomCode_ThrowsException() {
        User instructor = User.builder().id(10L).fullName("Instructor A").build();

        when(cohortRepository.existsByCode("DUPLICATE-MOD-CODE")).thenReturn(true);

        CohortRequestDTO request = new CohortRequestDTO();
        request.setName("Module bi trung");
        request.setCode("DUPLICATE-MOD-CODE");

        Assertions.assertThrows(RuntimeException.class, () -> cohortService.createCohort(request, instructor));
    }

    @Test
    void testUpdateCohortWithDuplicateCode_ThrowsException() {
        User instructor = User.builder().id(10L).fullName("Instructor A").build();
        Cohort existing = Cohort.builder()
                .id(50L)
                .code("ORIGINAL-MOD-CODE")
                .createdBy(instructor)
                .build();

        when(cohortRepository.findByIdAndInstructorId(50L, 10L)).thenReturn(Optional.of(existing));
        when(cohortRepository.existsByCode("ANOTHER-MOD-CODE")).thenReturn(true);

        CohortRequestDTO request = new CohortRequestDTO();
        request.setName("Updated Module Name");
        request.setCode("ANOTHER-MOD-CODE");

        Assertions.assertThrows(RuntimeException.class, () -> cohortService.updateCohort(50L, request, instructor));
    }

    @Test
    void testUpdateCohortKeepingSameCode_Success() {
        User instructor = User.builder().id(10L).fullName("Instructor A").build();
        Cohort existing = Cohort.builder()
                .id(50L)
                .code("ORIGINAL-MOD-CODE")
                .name("Old Name")
                .createdBy(instructor)
                .build();

        when(cohortRepository.findByIdAndInstructorId(50L, 10L)).thenReturn(Optional.of(existing));
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CohortRequestDTO request = new CohortRequestDTO();
        request.setName("Updated Module Name");
        request.setCode("ORIGINAL-MOD-CODE");

        Cohort updated = cohortService.updateCohort(50L, request, instructor);

        Assertions.assertEquals("ORIGINAL-MOD-CODE", updated.getCode());
        Assertions.assertEquals("Updated Module Name", updated.getName());
    }
}

