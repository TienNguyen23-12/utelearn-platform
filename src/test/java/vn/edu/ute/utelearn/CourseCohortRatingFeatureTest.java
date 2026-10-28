package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import vn.edu.ute.utelearn.controller.api.CohortApiController;
import vn.edu.ute.utelearn.controller.api.CourseApiController;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.EnrollmentRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CategoryService;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.service.CourseService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseCohortRatingFeatureTest {

    @Mock
    private CourseService courseService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private vn.edu.ute.utelearn.service.SectionService sectionService;

    @Mock
    private vn.edu.ute.utelearn.service.LessonService lessonService;

    @Mock
    private AuthService authService;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CohortService cohortService;

    @InjectMocks
    private CourseApiController courseApiController;

    @InjectMocks
    private CohortApiController cohortApiController;

    private Course sampleCourse;
    private Cohort sampleCohort;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(courseApiController, "enrollmentRepository", enrollmentRepository);

        User instructor = User.builder().id(10L).fullName("TS. Nguyen Van B").build();
        sampleCourse = Course.builder()
                .id(1L)
                .code("CRS-01")
                .title("Lap trinh Java Spring")
                .slug("lap-trinh-java-spring")
                .createdBy(instructor)
                .price(BigDecimal.valueOf(500000))
                .build();

        sampleCohort = Cohort.builder()
                .id(100L)
                .code("MOD-01")
                .name("Module Spring Boot")
                .course(sampleCourse)
                .createdBy(instructor)
                .price(BigDecimal.valueOf(200000))
                .build();
    }

    @Test
    void testGetCoursesIncludesRatingAndCountFromEnrollmentOnly() {
        when(courseService.getPublishedCourses(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(sampleCourse)));
        when(cohortRepository.countByCourseId(1L)).thenReturn(2L);
        when(cohortMemberRepository.countDistinctUsersByCourseId(1L)).thenReturn(10L);
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(1L)).thenReturn(Collections.emptyList());

        when(enrollmentRepository.getAverageRatingByCourseId(1L)).thenReturn(4.8);
        when(enrollmentRepository.countRatingsByCourseId(1L)).thenReturn(3L);

        ResponseEntity<Map<String, Object>> response = courseApiController.getCourses(null, null, null, 0, 12);
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertEquals(1, content.size());

        Map<String, Object> item = content.get(0);
        assertEquals(4.8, item.get("rating"));
        assertEquals(3L, item.get("ratingCount"));
    }

    @Test
    void testGetPublicCohortsIncludesRatingAndCount() {
        when(cohortService.searchPublicCohorts(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(sampleCohort)));
        when(cohortMemberRepository.countByCohort_Id(100L)).thenReturn(15L);
        when(cohortMemberRepository.getAverageRatingByCohortId(100L)).thenReturn(4.7);
        when(cohortMemberRepository.countRatingsByCohortId(100L)).thenReturn(8L);

        ResponseEntity<Map<String, Object>> response = cohortApiController.getPublicCohorts(null, null, 0, 8);
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertEquals(1, content.size());

        Map<String, Object> item = content.get(0);
        assertEquals(4.7, item.get("rating"));
        assertEquals(8L, item.get("ratingCount"));
    }

    @Test
    void testGetCoursesWhenUnratedReturnsNullRatingAndZeroCount() {
        when(courseService.getPublishedCourses(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(sampleCourse)));
        when(cohortRepository.countByCourseId(1L)).thenReturn(0L);
        when(cohortMemberRepository.countDistinctUsersByCourseId(1L)).thenReturn(0L);
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(1L)).thenReturn(Collections.emptyList());

        when(enrollmentRepository.getAverageRatingByCourseId(1L)).thenReturn(null);
        when(enrollmentRepository.countRatingsByCourseId(1L)).thenReturn(0L);

        ResponseEntity<Map<String, Object>> response = courseApiController.getCourses(null, null, null, 0, 12);
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        Map<String, Object> item = content.get(0);
        assertNull(item.get("rating"));
        assertEquals(0L, item.get("ratingCount"));
    }
}
