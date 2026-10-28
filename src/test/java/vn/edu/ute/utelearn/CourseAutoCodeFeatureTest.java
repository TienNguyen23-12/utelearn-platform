package vn.edu.ute.utelearn;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.CategoryRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.CourseRequestDTO;
import vn.edu.ute.utelearn.entity.Category;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.impl.CourseServiceImpl;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseAutoCodeFeatureTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    @Test
    void testCreateCourseWithoutCode_AutoGeneratesCode() {
        Category category = Category.builder().id(1L).name("Lập trình Web").build();
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(courseRepository.count()).thenReturn(0L);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseRequestDTO request = new CourseRequestDTO();
        request.setTitle("Lập trình Web nâng cao");
        request.setCategoryId(1L);
        request.setPrice(BigDecimal.valueOf(500000));

        Course saved = courseService.createCourse(request, 2L);

        String expectedCode = String.format("CRS-%s-01", Year.now().getValue());
        Assertions.assertEquals(expectedCode, saved.getCode());
    }

    @Test
    void testCreateCourseWithExistingAutoCode_IncrementsSequence() {
        Category category = Category.builder().id(1L).name("Lập trình Web").build();
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();

        String year = String.valueOf(Year.now().getValue());
        String code1 = String.format("CRS-%s-01", year);
        String code2 = String.format("CRS-%s-02", year);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(courseRepository.count()).thenReturn(0L);
        when(courseRepository.existsByCode(code1)).thenReturn(true);
        when(courseRepository.existsByCode(code2)).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseRequestDTO request = new CourseRequestDTO();
        request.setTitle("Lập trình Web nâng cao");
        request.setCategoryId(1L);
        request.setCode("");

        Course saved = courseService.createCourse(request, 2L);

        Assertions.assertEquals(code2, saved.getCode());
    }

    @Test
    void testCreateCourseWithCustomCode_RetainsCustomCode() {
        Category category = Category.builder().id(1L).name("Lập trình Web").build();
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(courseRepository.existsByCode("CS-CUSTOM")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseRequestDTO request = new CourseRequestDTO();
        request.setTitle("Lập trình Spring");
        request.setCategoryId(1L);
        request.setCode("CS-CUSTOM");

        Course saved = courseService.createCourse(request, 2L);

        Assertions.assertEquals("CS-CUSTOM", saved.getCode());
    }

    @Test
    void testCreateCourseWithDuplicateCustomCode_ThrowsException() {
        Category category = Category.builder().id(1L).name("Lập trình Web").build();
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(courseRepository.existsByCode("DUPLICATE-CODE")).thenReturn(true);

        CourseRequestDTO request = new CourseRequestDTO();
        request.setTitle("Lập trình Spring");
        request.setCategoryId(1L);
        request.setCode("DUPLICATE-CODE");

        Assertions.assertThrows(RuntimeException.class, () -> courseService.createCourse(request, 2L));
    }

    @Test
    void testUpdateCourseWithDuplicateCode_ThrowsException() {
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();
        Course existing = Course.builder()
                .id(10L)
                .code("ORIGINAL-CODE")
                .createdBy(instructor)
                .build();

        Category category = Category.builder().id(1L).name("Lập trình Web").build();

        when(courseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(courseRepository.existsByCode("ANOTHER-CODE")).thenReturn(true);

        CourseRequestDTO request = new CourseRequestDTO();
        request.setTitle("Updated Title");
        request.setCategoryId(1L);
        request.setCode("ANOTHER-CODE");

        Assertions.assertThrows(RuntimeException.class, () -> courseService.updateCourse(10L, request, 2L));
    }

    @Test
    void testUpdateCourseKeepingSameCode_Success() {
        User instructor = User.builder().id(2L).fullName("Nguyen Van A").build();
        Course existing = Course.builder()
                .id(10L)
                .code("ORIGINAL-CODE")
                .createdBy(instructor)
                .build();

        Category category = Category.builder().id(1L).name("Lập trình Web").build();

        when(courseRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseRequestDTO request = new CourseRequestDTO();
        request.setTitle("Updated Title");
        request.setCategoryId(1L);
        request.setCode("ORIGINAL-CODE");

        Course updated = courseService.updateCourse(10L, request, 2L);

        Assertions.assertEquals("ORIGINAL-CODE", updated.getCode());
        Assertions.assertEquals("Updated Title", updated.getTitle());
    }
}
