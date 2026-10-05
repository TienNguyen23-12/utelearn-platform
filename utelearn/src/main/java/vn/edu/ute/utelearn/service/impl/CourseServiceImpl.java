package vn.edu.ute.utelearn.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.service.CourseService;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final vn.edu.ute.utelearn.dao.CategoryRepository categoryRepository;
    private final vn.edu.ute.utelearn.dao.UserRepository userRepository;

    @Override
    public Page<Course> getPublishedCourses(String keyword, String categorySlug, String level, Pageable pageable) {
        Specification<Course> spec = (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("category", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("createdBy", jakarta.persistence.criteria.JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
            
            // Only fetch published courses
            predicates.add(cb.equal(root.get("status"), "PUBLISHED"));
            
            if (!vn.edu.ute.utelearn.util.ValidationUtils.isNullOrEmpty(keyword)) {
                String safeKeyword = vn.edu.ute.utelearn.util.ValidationUtils.stripHtmlTags(keyword).toLowerCase();
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + safeKeyword + "%"));
            }
            if (!vn.edu.ute.utelearn.util.ValidationUtils.isNullOrEmpty(categorySlug)) {
                predicates.add(cb.equal(root.join("category").get("slug"), categorySlug));
            }
            if (!vn.edu.ute.utelearn.util.ValidationUtils.isNullOrEmpty(level) && !level.equals("ALL_LEVELS")) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return courseRepository.findAll(spec, pageable);
    }

    @Override
    public Course getCourseBySlug(String slug) {
        return courseRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học với đường dẫn: " + slug));
    }

    @Override
    public Page<Course> getCoursesByInstructor(Long instructorId, Pageable pageable) {
        return courseRepository.findByCreatedById(instructorId, pageable);
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học ID: " + id));
    }

    @Override
    @Transactional
    public Course createCourse(vn.edu.ute.utelearn.dto.CourseRequestDTO request, Long instructorId) {
        vn.edu.ute.utelearn.entity.Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
        
        vn.edu.ute.utelearn.entity.User creator = userRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        String slug = generateSlug(request.getTitle());

        Course course = Course.builder()
                .code(request.getCode())
                .title(request.getTitle())
                .slug(slug)
                .headline(request.getHeadline())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .level(request.getLevel())
                .category(category)
                .createdBy(creator)
                .status("DRAFT")
                .objectives(parseObjectives(request.getObjectives()))
                .build();

        return courseRepository.save(course);
    }

    @Override
    @Transactional
    public Course updateCourse(Long courseId, vn.edu.ute.utelearn.dto.CourseRequestDTO request, Long instructorId) {
        Course course = getCourseById(courseId);
        
        if (!course.getCreatedBy().getId().equals(instructorId)) {
            throw new RuntimeException("Bạn không có quyền sửa khóa học này");
        }

        vn.edu.ute.utelearn.entity.Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        course.setCode(request.getCode());
        course.setTitle(request.getTitle());
        // course.setSlug(generateSlug(request.getTitle())); // Cập nhật slug hay không tùy logic, thường không nên đổi slug
        course.setHeadline(request.getHeadline());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setLevel(request.getLevel());
        course.setCategory(category);
        course.setObjectives(parseObjectives(request.getObjectives()));

        return courseRepository.save(course);
    }

    private String parseObjectives(String objectivesStr) {
        if (objectivesStr == null || objectivesStr.trim().isEmpty()) {
            return "[]";
        }
        try {
            List<String> list = Arrays.stream(objectivesStr.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            return new ObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    @Transactional
    public void updateCourseStatus(Long courseId, String status) {
        Course course = getCourseById(courseId);
        course.setStatus(status);
        courseRepository.save(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        Course course = getCourseById(id);
        courseRepository.delete(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id, Long instructorId) {
        Course course = getCourseById(id);
        
        if (!course.getCreatedBy().getId().equals(instructorId)) {
            throw new RuntimeException("Bạn không có quyền xóa khóa học này");
        }

        courseRepository.delete(course);
    }

    private String generateSlug(String title) {
        if (title == null) return "";
        String slug = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return slug + "-" + System.currentTimeMillis() % 10000;
    }
}
