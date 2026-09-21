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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    public Page<Course> getPublishedCourses(String keyword, String categorySlug, String level, Pageable pageable) {
        Specification<Course> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Only fetch published courses
            predicates.add(cb.equal(root.get("status"), "APPROVED"));
            
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
}
