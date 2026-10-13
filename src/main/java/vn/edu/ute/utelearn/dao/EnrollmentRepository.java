package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Enrollment;
import vn.edu.ute.utelearn.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    Optional<Enrollment> findByUserAndCourse(User user, Course course);
    
    @EntityGraph(attributePaths = {"course"})
    List<Enrollment> findByUserOrderByEnrolledAtDesc(User user);
    
    boolean existsByUserAndCourse(User user, Course course);
}
