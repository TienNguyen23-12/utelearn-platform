package vn.edu.ute.utelearn.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.ute.utelearn.entity.Course;

public interface CourseService {
    Page<Course> getPublishedCourses(String keyword, String categorySlug, String level, Pageable pageable);
    Course getCourseBySlug(String slug);
    Page<Course> getCoursesByInstructor(Long instructorId, Pageable pageable);
    Course createCourse(vn.edu.ute.utelearn.dto.CourseRequestDTO request, Long instructorId);
    Course updateCourse(Long courseId, vn.edu.ute.utelearn.dto.CourseRequestDTO request, Long instructorId);
    void updateCourseStatus(Long courseId, String status);
    void deleteCourse(Long id);
    void deleteCourse(Long id, Long instructorId);
    Course getCourseById(Long id);
}
