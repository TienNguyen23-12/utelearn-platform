package vn.edu.ute.utelearn.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.ute.utelearn.entity.Course;

public interface CourseService {
    Page<Course> getPublishedCourses(String keyword, String categorySlug, String level, Pageable pageable);
    Course getCourseBySlug(String slug);
}
