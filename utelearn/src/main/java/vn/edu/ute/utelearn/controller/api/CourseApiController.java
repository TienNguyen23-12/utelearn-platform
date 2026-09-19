package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.entity.Category;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.service.CategoryService;
import vn.edu.ute.utelearn.service.CourseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseApiController {

    private final CourseService courseService;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCourses(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage = courseService.getPublishedCourses(q, category, level, pageable);
        
        List<Map<String, Object>> courses = coursePage.getContent().stream().map(course -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", course.getId());
            map.put("title", course.getTitle());
            map.put("slug", course.getSlug());
            map.put("headline", course.getHeadline());
            map.put("thumbnailUrl", course.getThumbnailUrl());
            map.put("level", course.getLevel());
            map.put("categoryName", course.getCategory() != null ? course.getCategory().getName() : null);
            map.put("categorySlug", course.getCategory() != null ? course.getCategory().getSlug() : null);
            map.put("instructorName", course.getCreatedBy() != null ? course.getCreatedBy().getFullName() : "Giảng viên");
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", courses);
        response.put("currentPage", coursePage.getNumber());
        response.put("totalItems", coursePage.getTotalElements());
        response.put("totalPages", coursePage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        List<Category> categories = categoryService.getRootCategories();
        List<Map<String, Object>> result = categories.stream().map(cat -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cat.getId());
            map.put("name", cat.getName());
            map.put("slug", cat.getSlug());
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Map<String, Object>> getCourseDetail(@PathVariable String slug) {
        try {
            Course course = courseService.getCourseBySlug(slug);
            
            Map<String, Object> map = new HashMap<>();
            map.put("id", course.getId());
            map.put("title", course.getTitle());
            map.put("slug", course.getSlug());
            map.put("headline", course.getHeadline());
            map.put("description", course.getDescription());
            map.put("thumbnailUrl", course.getThumbnailUrl());
            map.put("level", course.getLevel());
            map.put("objectives", course.getObjectives());
            map.put("requirements", course.getRequirements());
            map.put("categoryName", course.getCategory() != null ? course.getCategory().getName() : null);
            map.put("categorySlug", course.getCategory() != null ? course.getCategory().getSlug() : null);
            map.put("instructorName", course.getCreatedBy() != null ? course.getCreatedBy().getFullName() : "Giảng viên");
            map.put("instructorAvatar", course.getCreatedBy() != null ? course.getCreatedBy().getAvatarUrl() : null);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
