package vn.edu.ute.utelearn.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.util.SlugUtils;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final vn.edu.ute.utelearn.dao.CategoryRepository categoryRepository;
    private final vn.edu.ute.utelearn.dao.UserRepository userRepository;
    private final vn.edu.ute.utelearn.service.ModerationService moderationService;
    private final vn.edu.ute.utelearn.service.AuthService authService;

    @Override
    public Page<Course> getPublishedCourses(String keyword, String categorySlug, String level, Pageable pageable) {
        return getPublishedCourses(keyword, categorySlug, level, null, pageable);
    }

    @Override
    public Page<Course> getPublishedCourses(String keyword, String categorySlug, String level, Long cursor, Pageable pageable) {
        Specification<Course> spec = (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("category", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("createdBy", jakarta.persistence.criteria.JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();
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
            if (cursor != null) {
                predicates.add(cb.lessThan(root.get("id"), cursor));
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
    public Page<Course> getAllCourses(Pageable pageable) {
        return courseRepository.findAllCourses(pageable);
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

        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            String prefix = "CRS";
            long count = courseRepository.count();
            String year = String.valueOf(java.time.Year.now().getValue());
            String autoCode = String.format("%s-%s-%02d", prefix, year, count + 1);
            while (courseRepository.existsByCode(autoCode)) {
                count++;
                autoCode = String.format("%s-%s-%02d", prefix, year, count + 1);
            }
            request.setCode(autoCode);
        } else if (courseRepository.existsByCode(request.getCode().trim())) {
            throw new RuntimeException("Course code already exists");
        } else {
            request.setCode(request.getCode().trim());
        }

        String slug = generateSlug(request.getTitle());

        String thumbnail = request.getThumbnailUrl();
        if (thumbnail == null || thumbnail.trim().isEmpty()) {
            thumbnail = "/images/courses/default-cover.png";
        } else {
            thumbnail = thumbnail.trim();
        }

        Course course = Course.builder()
                .code(request.getCode())
                .title(request.getTitle())
                .slug(slug)
                .headline(request.getHeadline())
                .description(request.getDescription())
                .thumbnailUrl(thumbnail)
                .level(request.getLevel())
                .category(category)
                .createdBy(creator)
                .status("DRAFT")
                .objectives(parseObjectives(request.getObjectives()))
                .price(request.getPrice())
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

        String thumbnail = request.getThumbnailUrl();
        if (thumbnail == null || thumbnail.trim().isEmpty()) {
            thumbnail = "/images/courses/default-cover.png";
        } else {
            thumbnail = thumbnail.trim();
        }

        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            String cleanCode = request.getCode().trim();
            if (!cleanCode.equals(course.getCode()) && courseRepository.existsByCode(cleanCode)) {
                throw new RuntimeException("Course code already exists");
            }
            course.setCode(cleanCode);
        }
        course.setTitle(request.getTitle());
        course.setHeadline(request.getHeadline());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(thumbnail);
        course.setLevel(request.getLevel());
        course.setCategory(category);
        course.setObjectives(parseObjectives(request.getObjectives()));
        course.setPrice(request.getPrice());

        if ("PUBLISHED".equalsIgnoreCase(course.getStatus())) {
            course.setStatus("DRAFT");
            log.info("[CourseService] Chuyển khóa học id: {} về DRAFT do cập nhật thông tin khóa học", courseId);
        }

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

        vn.edu.ute.utelearn.entity.User currentUser = authService != null ? authService.getCurrentAuthenticatedUser().orElse(null) : null;
        if (moderationService != null) {
            moderationService.syncStatusFromExternalChange("COURSE", courseId, status, currentUser);
        }
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

    @Override
    @Transactional
    public void revertToDraftIfPublished(Course course) {
        if (course != null && "PUBLISHED".equalsIgnoreCase(course.getStatus())) {
            course.setStatus("DRAFT");
            courseRepository.save(course);
            log.info("[CourseService] Chuyển khóa học id: {} về DRAFT do có thay đổi nội dung", course.getId());
        }
    }

    @Override
    @Transactional
    public void revertToDraftIfPublished(Long courseId) {
        if (courseId != null) {
            courseRepository.findById(courseId).ifPresent(this::revertToDraftIfPublished);
        }
    }

    private String generateSlug(String title) {
        String slug = SlugUtils.toSlug(title);
        return slug + "-" + (System.currentTimeMillis() % 10000);
    }
}
