package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Enrollment;
import vn.edu.ute.utelearn.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    Optional<Enrollment> findByUserAndCourse(User user, Course course);

    Optional<Enrollment> findByUser_IdAndCourse_Id(Long userId, Long courseId);
    
    @EntityGraph(attributePaths = {"course"})
    List<Enrollment> findByUserOrderByEnrolledAtDesc(User user);
    
    boolean existsByUserAndCourse(User user, Course course);

    boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course c JOIN FETCH e.user u LEFT JOIN FETCH c.createdBy cb WHERE e.certificateUrl LIKE CONCAT('%', :code, '%')")
    List<Enrollment> findByCertificateCode(@Param("code") String code);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course c JOIN FETCH e.user u LEFT JOIN FETCH c.createdBy cb WHERE c.id = :courseId AND u.id = :userId")
    Optional<Enrollment> findByCourseIdAndUserIdWithDetails(@Param("courseId") Long courseId, @Param("userId") Long userId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.user u WHERE e.course.id = :courseId ORDER BY u.fullName ASC")
    List<Enrollment> findByCourse_IdWithUser(@Param("courseId") Long courseId);
}
