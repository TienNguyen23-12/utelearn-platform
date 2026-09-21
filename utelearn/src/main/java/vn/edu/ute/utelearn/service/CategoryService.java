package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.entity.Category;
import java.util.List;

public interface CategoryService {
    List<Category> getRootCategories();
    List<Category> getAllCategories();
}
