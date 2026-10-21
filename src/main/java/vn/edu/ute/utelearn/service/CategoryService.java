package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.CategoryRequestDTO;
import vn.edu.ute.utelearn.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> getRootCategories();
    List<Category> getAllCategories();
    Category getCategoryById(Long id);
    Category createCategory(CategoryRequestDTO dto);
    Category updateCategory(Long id, CategoryRequestDTO dto);
    void deleteCategory(Long id);
    long countCoursesInCategory(Long categoryId);
}
