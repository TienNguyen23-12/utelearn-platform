package vn.edu.ute.utelearn;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.CategoryRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.CohortRequestDTO;
import vn.edu.ute.utelearn.dto.CourseRequestDTO;
import vn.edu.ute.utelearn.entity.Category;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.impl.CohortServiceImpl;
import vn.edu.ute.utelearn.service.impl.CourseServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultCoverFeatureTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    @InjectMocks
    private CohortServiceImpl cohortService;

    @Test
    void testCreateCourseWithEmptyThumbnailUsesDefaultCover() {
        Category category = Category.builder().id(1L).name("Lập trình Web").build();
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseRequestDTO request = new CourseRequestDTO();
        request.setTitle("Lập trình Spring Boot");
        request.setCode("SPB101");
        request.setCategoryId(1L);
        request.setThumbnailUrl("   ");
        request.setPrice(BigDecimal.valueOf(500000));

        Course saved = courseService.createCourse(request, 2L);

        Assertions.assertEquals("/images/courses/default-cover.png", saved.getThumbnailUrl());
        Assertions.assertEquals("/images/courses/default-cover.png", saved.getEffectiveThumbnailUrl());
    }

    @Test
    void testCreateCourseWithCustomThumbnailKeepsCustom() {
        Category category = Category.builder().id(1L).name("Lập trình Web").build();
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseRequestDTO request = new CourseRequestDTO();
        request.setTitle("Lập trình Spring Boot");
        request.setCode("SPB101");
        request.setCategoryId(1L);
        request.setThumbnailUrl("https://example.com/custom.png");
        request.setPrice(BigDecimal.valueOf(500000));

        Course saved = courseService.createCourse(request, 2L);

        Assertions.assertEquals("https://example.com/custom.png", saved.getThumbnailUrl());
        Assertions.assertEquals("https://example.com/custom.png", saved.getEffectiveThumbnailUrl());
    }

    @Test
    void testCreateCohortWithEmptyThumbnailInheritsCourseThumbnail() {
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();
        Course course = Course.builder()
                .id(10L)
                .code("SPB101")
                .createdBy(instructor)
                .thumbnailUrl("https://example.com/course-cover.png")
                .build();

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CohortRequestDTO dto = new CohortRequestDTO();
        dto.setCourseId(10L);
        dto.setName("Module 1");
        dto.setCode("MOD-01");
        dto.setThumbnailUrl("");

        Cohort saved = cohortService.createCohort(dto, instructor);

        Assertions.assertEquals("https://example.com/course-cover.png", saved.getThumbnailUrl());
        Assertions.assertEquals("https://example.com/course-cover.png", saved.getEffectiveThumbnailUrl());
        Assertions.assertEquals("APPROVED", saved.getModerationStatus());
    }

    @Test
    void testCohortEffectiveThumbnailFallbackToDefaultWhenCourseHasNoThumbnail() {
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();
        Course course = Course.builder()
                .id(10L)
                .code("SPB101")
                .createdBy(instructor)
                .thumbnailUrl("")
                .build();

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(cohortRepository.save(any(Cohort.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CohortRequestDTO dto = new CohortRequestDTO();
        dto.setCourseId(10L);
        dto.setName("Module 1");
        dto.setCode("MOD-01");
        dto.setThumbnailUrl(null);

        Cohort saved = cohortService.createCohort(dto, instructor);

        Assertions.assertEquals("/images/courses/default-cover.png", saved.getThumbnailUrl());
        Assertions.assertEquals("/images/courses/default-cover.png", saved.getEffectiveThumbnailUrl());
    }
}
