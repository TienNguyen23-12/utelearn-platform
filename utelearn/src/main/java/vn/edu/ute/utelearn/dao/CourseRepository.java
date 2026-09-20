package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Course;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category", "createdBy"})
    Optional<Course> findBySlug(String slug);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category"})
    org.springframework.data.domain.Page<Course> findByCreatedById(Long createdById, org.springframework.data.domain.Pageable pageable);
}
