package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CategoryRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dto.CategoryRequestDTO;
import vn.edu.ute.utelearn.entity.Category;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.service.CategoryService;
import vn.edu.ute.utelearn.util.ValidationUtils;

import java.text.Normalizer;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    @Override
    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục ID: " + id));
    }

    @Override
    @Transactional
    public Category createCategory(CategoryRequestDTO dto) {
        log.info("[CategoryService] Bắt đầu tạo mới danh mục: {}", dto.getName());

        String slug = dto.getSlug();
        if (ValidationUtils.isNullOrEmpty(slug)) {
            slug = generateSlug(dto.getName());
        } else {
            slug = sanitizeSlug(slug);
        }

        String baseSlug = slug;
        int suffix = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        Category parent = null;
        if (dto.getParentId() != null) {
            parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại ID: " + dto.getParentId()));
        }

        Category category = Category.builder()
                .name(dto.getName().trim())
                .slug(slug)
                .parent(parent)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("[CategoryService] Tạo mới danh mục thành công, ID: {}, slug: {}", saved.getId(), saved.getSlug());
        return saved;
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, CategoryRequestDTO dto) {
        log.info("[CategoryService] Bắt đầu cập nhật danh mục ID: {}", id);
        Category category = getCategoryById(id);

        String slug = dto.getSlug();
        if (ValidationUtils.isNullOrEmpty(slug)) {
            slug = generateSlug(dto.getName());
        } else {
            slug = sanitizeSlug(slug);
        }

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new IllegalArgumentException("Đường dẫn tĩnh '" + slug + "' đã tồn tại trên hệ thống");
        }

        Category parent = null;
        if (dto.getParentId() != null) {
            if (dto.getParentId().equals(id)) {
                throw new IllegalArgumentException("Danh mục cha không thể là chính danh mục hiện tại");
            }
            if (isDescendant(id, dto.getParentId())) {
                throw new IllegalArgumentException("Không thể chọn danh mục con làm danh mục cha");
            }
            parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Danh mục cha không tồn tại ID: " + dto.getParentId()));
        }

        category.setName(dto.getName().trim());
        category.setSlug(slug);
        category.setParent(parent);

        Category updated = categoryRepository.save(category);
        log.info("[CategoryService] Cập nhật danh mục thành công ID: {}", updated.getId());
        return updated;
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        log.info("[CategoryService] Bắt đầu xóa danh mục ID: {}", id);
        Category category = getCategoryById(id);

        List<Course> courses = courseRepository.findByCategoryId(id);
        for (Course course : courses) {
            course.setCategory(null);
            courseRepository.save(course);
        }

        List<Category> subCategories = categoryRepository.findByParentId(id);
        for (Category sub : subCategories) {
            sub.setParent(category.getParent());
            categoryRepository.save(sub);
        }

        categoryRepository.delete(category);
        log.info("[CategoryService] Đã xóa danh mục ID: {}", id);
    }

    @Override
    public long countCoursesInCategory(Long categoryId) {
        return courseRepository.countByCategoryId(categoryId);
    }

    private boolean isDescendant(Long ancestorId, Long targetId) {
        Category current = categoryRepository.findById(targetId).orElse(null);
        while (current != null && current.getParent() != null) {
            if (current.getParent().getId().equals(ancestorId)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private String generateSlug(String name) {
        if (name == null) return "";
        String slug = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (slug.isBlank()) {
            slug = "cat-" + System.currentTimeMillis();
        }
        return slug;
    }

    private String sanitizeSlug(String rawSlug) {
        return Normalizer.normalize(rawSlug, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
