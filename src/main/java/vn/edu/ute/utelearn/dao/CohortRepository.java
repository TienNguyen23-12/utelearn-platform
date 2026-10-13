package vn.edu.ute.utelearn.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Cohort;

import java.util.Optional;

@Repository
public interface CohortRepository extends JpaRepository<Cohort, Long> {
    Page<Cohort> findByCourseId(Long courseId, Pageable pageable);
    
    @Query(value = "SELECT c FROM Cohort c JOIN FETCH c.course cr WHERE cr.createdBy.id = :instructorId",
           countQuery = "SELECT count(c) FROM Cohort c WHERE c.course.createdBy.id = :instructorId")
    Page<Cohort> findByInstructorId(@Param("instructorId") Long instructorId, Pageable pageable);

    @Query(value = "SELECT c FROM Cohort c JOIN FETCH c.course cr WHERE cr.createdBy.id = :instructorId " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = '') " +
           "AND (c.status = :status OR :status = '')",
           countQuery = "SELECT count(c) FROM Cohort c WHERE c.course.createdBy.id = :instructorId " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = '') " +
           "AND (c.status = :status OR :status = '')")
    Page<Cohort> searchCohortsByInstructor(@Param("instructorId") Long instructorId, @Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query(value = "SELECT c FROM Cohort c JOIN FETCH c.course cr LEFT JOIN FETCH cr.createdBy LEFT JOIN FETCH cr.category WHERE " +
           "(c.status = 'ENROLLING' OR c.status = 'UPCOMING') " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(cr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = '') " +
           "AND (cr.category.slug = :categorySlug OR :categorySlug = '')",
           countQuery = "SELECT count(c) FROM Cohort c JOIN c.course cr LEFT JOIN cr.createdBy LEFT JOIN cr.category WHERE " +
           "(c.status = 'ENROLLING' OR c.status = 'UPCOMING') " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(cr.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = '') " +
           "AND (cr.category.slug = :categorySlug OR :categorySlug = '')")
    Page<Cohort> searchPublicCohorts(@Param("keyword") String keyword, @Param("categorySlug") String categorySlug, Pageable pageable);

    @Query("SELECT c FROM Cohort c WHERE c.course.createdBy.id = :instructorId AND c.id = :id")
    Optional<Cohort> findByIdAndInstructorId(@Param("id") Long id, @Param("instructorId") Long instructorId);
    
    boolean existsByCode(String code);

    long countByCourseId(Long courseId);

    java.util.Optional<Cohort> findFirstByCourseIdAndStatusIn(Long courseId, java.util.List<String> statuses);

    java.util.List<Cohort> findByCourseIdAndStatusIn(Long courseId, java.util.List<String> statuses);
}
