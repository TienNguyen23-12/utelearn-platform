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
    
    @Query(value = "SELECT c FROM Cohort c LEFT JOIN FETCH c.course cr LEFT JOIN FETCH c.createdBy WHERE (c.createdBy.id = :instructorId OR (c.createdBy IS NULL AND cr.createdBy.id = :instructorId)) AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%'))",
           countQuery = "SELECT count(c) FROM Cohort c LEFT JOIN c.course cr WHERE (c.createdBy.id = :instructorId OR (c.createdBy IS NULL AND cr.createdBy.id = :instructorId)) AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%'))")
    Page<Cohort> findByInstructorId(@Param("instructorId") Long instructorId, Pageable pageable);

    @Query(value = "SELECT c FROM Cohort c LEFT JOIN FETCH c.course cr LEFT JOIN FETCH c.createdBy WHERE (c.createdBy.id = :instructorId OR (c.createdBy IS NULL AND cr.createdBy.id = :instructorId)) " +
           "AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%')) " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = '') " +
           "AND (c.status = :status OR :status = '')",
           countQuery = "SELECT count(c) FROM Cohort c LEFT JOIN c.course cr WHERE (c.createdBy.id = :instructorId OR (c.createdBy IS NULL AND cr.createdBy.id = :instructorId)) " +
           "AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%')) " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = '') " +
           "AND (c.status = :status OR :status = '')")
    Page<Cohort> searchCohortsByInstructor(@Param("instructorId") Long instructorId, @Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    @Query(value = "SELECT c FROM Cohort c LEFT JOIN FETCH c.course cr LEFT JOIN FETCH cr.createdBy LEFT JOIN FETCH cr.category LEFT JOIN FETCH c.createdBy WHERE " +
           "(c.visibility = 'PUBLIC' OR c.visibility IS NULL) " +
           "AND (c.status = 'ENROLLING' OR c.status = 'UPCOMING' OR c.status = 'IN_PROGRESS') " +
           "AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%')) " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (cr.title IS NOT NULL AND LOWER(cr.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR :keyword = '') " +
           "AND (:categorySlug = '' OR (cr IS NOT NULL AND cr.category IS NOT NULL AND cr.category.slug = :categorySlug))",
           countQuery = "SELECT count(c) FROM Cohort c LEFT JOIN c.course cr LEFT JOIN cr.category cat WHERE " +
           "(c.visibility = 'PUBLIC' OR c.visibility IS NULL) " +
           "AND (c.status = 'ENROLLING' OR c.status = 'UPCOMING' OR c.status = 'IN_PROGRESS') " +
           "AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%')) " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (cr.title IS NOT NULL AND LOWER(cr.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR :keyword = '') " +
           "AND (:categorySlug = '' OR (cat IS NOT NULL AND cat.slug = :categorySlug))")
    Page<Cohort> searchPublicCohorts(@Param("keyword") String keyword, @Param("categorySlug") String categorySlug, Pageable pageable);

    @Query("SELECT c FROM Cohort c LEFT JOIN FETCH c.course cr LEFT JOIN FETCH c.createdBy WHERE (c.visibility = 'PUBLIC' OR c.createdBy.id = :instructorId OR (c.createdBy IS NULL AND cr.createdBy.id = :instructorId)) AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%'))")
    java.util.List<Cohort> findShareableCohorts(@Param("instructorId") Long instructorId);

    @Query("SELECT c FROM Cohort c LEFT JOIN FETCH c.course cr LEFT JOIN FETCH c.createdBy WHERE " +
           "(c.visibility = 'PUBLIC' OR c.createdBy.id = :instructorId OR (c.createdBy IS NULL AND cr.createdBy.id = :instructorId)) " +
           "AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%')) " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR :keyword = '') " +
           "ORDER BY c.createdAt DESC")
    java.util.List<Cohort> searchShareableCohorts(@Param("instructorId") Long instructorId, @Param("keyword") String keyword);

    @Query("SELECT c FROM Cohort c LEFT JOIN FETCH c.course cr LEFT JOIN FETCH c.createdBy WHERE (c.createdBy.id = :instructorId OR (c.createdBy IS NULL AND cr.createdBy.id = :instructorId)) AND c.id = :id")
    Optional<Cohort> findByIdAndInstructorId(@Param("id") Long id, @Param("instructorId") Long instructorId);
    
    boolean existsByCode(String code);

    long countByCourseId(Long courseId);

    java.util.Optional<Cohort> findFirstByCourseIdAndStatusIn(Long courseId, java.util.List<String> statuses);

    java.util.List<Cohort> findByCourseIdAndStatusIn(Long courseId, java.util.List<String> statuses);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"course", "createdBy"})
    java.util.Optional<Cohort> findById(Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"course", "createdBy"})
    java.util.List<Cohort> findByCourseIdOrderByCreatedAtAsc(Long courseId);

    @Query("SELECT c FROM Cohort c LEFT JOIN FETCH c.course cr LEFT JOIN FETCH c.createdBy WHERE c.course.id = :courseId " +
           "AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%')) " +
           "ORDER BY COALESCE(c.orderIndex, 9999) ASC, c.createdAt ASC")
    java.util.List<Cohort> findByCourseIdOrdered(@Param("courseId") Long courseId);

    @Query("SELECT c FROM Cohort c LEFT JOIN FETCH c.course cr LEFT JOIN FETCH c.createdBy WHERE c.course.id = :courseId AND c.status IN :statuses " +
           "AND (c.code NOT LIKE '%-ALL' AND (c.name IS NULL OR LOWER(c.name) NOT LIKE 'toàn bộ khóa học%')) " +
           "ORDER BY COALESCE(c.orderIndex, 9999) ASC, c.createdAt ASC")
    java.util.List<Cohort> findByCourseIdAndStatusInOrdered(@Param("courseId") Long courseId, @Param("statuses") java.util.List<String> statuses);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Cohort c SET c.status = 'ENROLLING' WHERE c.status = 'UPCOMING' AND c.enrollmentStart IS NOT NULL AND c.enrollmentStart <= :now AND (c.enrollmentEnd IS NULL OR c.enrollmentEnd > :now) AND (c.studyStart IS NULL OR c.studyStart > :now)")
    int autoPromoteUpcomingToEnrolling(@Param("now") java.time.Instant now);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Cohort c SET c.status = 'IN_PROGRESS' WHERE (c.status = 'ENROLLING' OR c.status = 'UPCOMING') AND c.studyStart IS NOT NULL AND c.studyStart <= :now AND (c.studyEnd IS NULL OR c.studyEnd > :now)")
    int autoPromoteToInProgress(@Param("now") java.time.Instant now);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Cohort c SET c.status = 'COMPLETED' WHERE c.status <> 'COMPLETED' AND c.studyEnd IS NOT NULL AND c.studyEnd <= :now")
    int autoPromoteToCompleted(@Param("now") java.time.Instant now);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Cohort c SET c.status = 'COMPLETED' WHERE c.status = 'ENROLLING' AND c.studyStart IS NULL AND c.enrollmentEnd IS NOT NULL AND c.enrollmentEnd <= :now")
    int autoCloseExpiredEnrollmentWithoutStudy(@Param("now") java.time.Instant now);
}

