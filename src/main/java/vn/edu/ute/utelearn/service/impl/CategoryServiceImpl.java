package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.ute.utelearn.dao.CategoryRepository;
import vn.edu.ute.utelearn.entity.Category;
import vn.edu.ute.utelearn.service.CategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}
