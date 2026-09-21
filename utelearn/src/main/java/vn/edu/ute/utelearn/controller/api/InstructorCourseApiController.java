package vn.edu.ute.utelearn.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ute.utelearn.dto.CourseRequestDTO;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor/courses")
@RequiredArgsConstructor
public class InstructorCourseApiController {

    private final CourseService courseService;
    private final AuthService authService;

    @PostMapping
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> createCourse(@Valid @RequestBody CourseRequestDTO request) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow(() -> new RuntimeException("Unauthorized"));
            Course course = courseService.createCourse(request, currentUser.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo khóa học thành công");
            response.put("courseId", course.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequestDTO request) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow(() -> new RuntimeException("Unauthorized"));
            Course course = courseService.updateCourse(id, request, currentUser.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật khóa học thành công");
            response.put("courseId", course.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@rbac.hasRole('ADMIN') or @rbac.hasPermission('CONTENT_APPROVE')")
    public ResponseEntity<?> updateCourseStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String newStatus = body.get("status");
            if (newStatus == null || newStatus.isEmpty()) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ");
            }
            courseService.updateCourseStatus(id, newStatus);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@rbac.hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            courseService.deleteCourse(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa khóa học thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
