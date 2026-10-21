package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.ute.utelearn.controller.AdminCategoryController;
import vn.edu.ute.utelearn.dao.CategoryRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dto.CategoryRequestDTO;
import vn.edu.ute.utelearn.entity.Category;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.service.CategoryService;
import vn.edu.ute.utelearn.service.impl.CategoryServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminCategoryFeatureTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Mock
    private CategoryService mockCategoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminCategoryController controller = new AdminCategoryController(mockCategoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testCreateCategoryAutoGeneratesSlug() {
        CategoryRequestDTO dto = CategoryRequestDTO.builder()
                .name("Lập trình Web")
                .slug(null)
                .build();

        when(categoryRepository.existsBySlug("lap-trinh-web")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        Category result = categoryService.createCategory(dto);

        assertNotNull(result);
        assertEquals("Lập trình Web", result.getName());
        assertEquals("lap-trinh-web", result.getSlug());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void testCreateCategoryDuplicateSlugAppendsSuffix() {
        CategoryRequestDTO dto = CategoryRequestDTO.builder()
                .name("Lập trình Web")
                .slug("lap-trinh-web")
                .build();

        when(categoryRepository.existsBySlug("lap-trinh-web")).thenReturn(true);
        when(categoryRepository.existsBySlug("lap-trinh-web-1")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category c = invocation.getArgument(0);
            c.setId(2L);
            return c;
        });

        Category result = categoryService.createCategory(dto);

        assertNotNull(result);
        assertEquals("lap-trinh-web-1", result.getSlug());
    }

    @Test
    void testUpdateCategorySuccess() {
        Category existing = Category.builder()
                .id(1L)
                .name("Cũ")
                .slug("cu")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsBySlugAndIdNot("moi", 1L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryRequestDTO dto = CategoryRequestDTO.builder()
                .name("Mới")
                .slug("moi")
                .build();

        Category updated = categoryService.updateCategory(1L, dto);

        assertEquals("Mới", updated.getName());
        assertEquals("moi", updated.getSlug());
    }

    @Test
    void testUpdateCategorySelfParentThrowsException() {
        Category existing = Category.builder()
                .id(1L)
                .name("Test")
                .slug("test")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        CategoryRequestDTO dto = CategoryRequestDTO.builder()
                .name("Test")
                .slug("test")
                .parentId(1L)
                .build();

        assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(1L, dto));
    }

    @Test
    void testDeleteCategoryDisassociatesCoursesAndSubCategories() {
        Category parent = Category.builder().id(10L).name("Cha").slug("cha").build();
        Category child = Category.builder().id(20L).name("Con").slug("con").parent(parent).build();

        Course course = Course.builder().id(100L).title("Khóa học").category(parent).build();

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(courseRepository.findByCategoryId(10L)).thenReturn(new ArrayList<>(List.of(course)));
        when(categoryRepository.findByParentId(10L)).thenReturn(new ArrayList<>(List.of(child)));

        categoryService.deleteCategory(10L);

        assertNull(course.getCategory());
        assertNull(child.getParent());
        verify(courseRepository).save(course);
        verify(categoryRepository).save(child);
        verify(categoryRepository).delete(parent);
    }

    @Test
    void testControllerListCategories() throws Exception {
        Category cat = Category.builder().id(1L).name("Web").slug("web").build();
        when(mockCategoryService.getAllCategories()).thenReturn(List.of(cat));
        when(mockCategoryService.countCoursesInCategory(1L)).thenReturn(5L);

        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/categories/index"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("courseCountMap"));
    }

    @Test
    void testControllerCreateCategoryPostSuccess() throws Exception {
        Category cat = Category.builder().id(1L).name("AI").slug("ai").build();
        when(mockCategoryService.createCategory(any(CategoryRequestDTO.class))).thenReturn(cat);

        mockMvc.perform(post("/admin/categories/create")
                        .param("name", "Trí tuệ nhân tạo")
                        .param("slug", "tri-tue-nhan-tao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testControllerDeleteCategoryPostSuccess() throws Exception {
        doNothing().when(mockCategoryService).deleteCategory(1L);

        mockMvc.perform(post("/admin/categories/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
