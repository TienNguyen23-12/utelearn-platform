package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.CohortMember;

import java.util.Optional;

@Repository
public interface CohortMemberRepository extends JpaRepository<CohortMember, Long> {
    boolean existsByCohort_Course_IdAndUser_Id(Long courseId, Long userId);

    boolean existsByCohort_IdAndUser_Id(Long cohortId, Long userId);

    long countByCohort_Id(Long cohortId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "cohort"})
    java.util.List<CohortMember> findByCohort_IdOrderByEnrolledAtAsc(Long cohortId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "cohort"})
    java.util.List<CohortMember> findByCohort_Id(Long cohortId);

    java.util.List<CohortMember> findByCohort_Course_IdAndUser_Id(Long courseId, Long userId);

    Optional<CohortMember> findByCohort_IdAndUser_Id(Long cohortId, Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT cm FROM CohortMember cm JOIN FETCH cm.cohort c LEFT JOIN FETCH cm.lastLesson WHERE c.course.id = :courseId AND cm.user.id = :userId")
    java.util.List<CohortMember> findByCourseIdAndUserIdWithCohort(
            @org.springframework.data.repository.query.Param("courseId") Long courseId,
            @org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT cm FROM CohortMember cm JOIN FETCH cm.cohort c LEFT JOIN FETCH cm.lastLesson WHERE c.course.id = :courseId AND cm.user.id = :userId ORDER BY cm.lastAccessedAt DESC NULLS LAST, cm.enrolledAt DESC")
    java.util.List<CohortMember> findByCourseIdAndUserIdWithCohortOrderByLastAccessed(
            @org.springframework.data.repository.query.Param("courseId") Long courseId,
            @org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT cm FROM CohortMember cm JOIN FETCH cm.cohort c LEFT JOIN FETCH c.createdBy LEFT JOIN FETCH c.course cr LEFT JOIN FETCH cr.createdBy LEFT JOIN FETCH cr.category LEFT JOIN FETCH cm.lastLesson WHERE cm.user = :user ORDER BY cm.enrolledAt DESC")
    java.util.List<CohortMember> findByUserOrderByEnrolledAtDesc(
            @org.springframework.data.repository.query.Param("user") vn.edu.ute.utelearn.entity.User user);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT cm.user.id) FROM CohortMember cm WHERE cm.cohort.course.id = :courseId")
    long countDistinctUsersByCourseId(@org.springframework.data.repository.query.Param("courseId") Long courseId);

    @org.springframework.data.jpa.repository.Query("SELECT cm FROM CohortMember cm JOIN FETCH cm.cohort c JOIN FETCH cm.user u LEFT JOIN FETCH c.course cr LEFT JOIN FETCH cr.createdBy cb WHERE cm.certificateUrl LIKE CONCAT('%', :code, '%')")
    java.util.List<CohortMember> findByCertificateCode(@org.springframework.data.repository.query.Param("code") String code);

    @org.springframework.data.jpa.repository.Query("SELECT cm FROM CohortMember cm JOIN FETCH cm.user u JOIN FETCH cm.cohort c WHERE c.course.id = :courseId ORDER BY u.fullName ASC")
    java.util.List<CohortMember> findByCourseIdWithUserAndCohort(@org.springframework.data.repository.query.Param("courseId") Long courseId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(cm.rating) FROM CohortMember cm WHERE cm.cohort.id = :cohortId AND cm.rating IS NOT NULL")
    Double getAverageRatingByCohortId(@org.springframework.data.repository.query.Param("cohortId") Long cohortId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(cm.id) FROM CohortMember cm WHERE cm.cohort.id = :cohortId AND cm.rating IS NOT NULL")
    long countRatingsByCohortId(@org.springframework.data.repository.query.Param("cohortId") Long cohortId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(cm.rating) FROM CohortMember cm WHERE cm.cohort.id = :cohortId AND cm.rating IS NOT NULL AND cm.reviewedAt >= :since")
    Double getAverageRatingByCohortIdSince(
            @org.springframework.data.repository.query.Param("cohortId") Long cohortId,
            @org.springframework.data.repository.query.Param("since") java.time.Instant since);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(cm.id) FROM CohortMember cm WHERE cm.cohort.id = :cohortId AND cm.rating IS NOT NULL AND cm.reviewedAt >= :since")
    long countRatingsByCohortIdSince(
            @org.springframework.data.repository.query.Param("cohortId") Long cohortId,
            @org.springframework.data.repository.query.Param("since") java.time.Instant since);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(cm.id) FROM CohortMember cm WHERE cm.cohort.id = :cohortId AND cm.enrolledAt >= :since")
    long countEnrolledByCohortIdSince(
            @org.springframework.data.repository.query.Param("cohortId") Long cohortId,
            @org.springframework.data.repository.query.Param("since") java.time.Instant since);

    @org.springframework.data.jpa.repository.Query("SELECT cm.cohort.id, COUNT(cm.id), AVG(cm.rating), COUNT(cm.rating) FROM CohortMember cm WHERE cm.cohort.id IN :cohortIds GROUP BY cm.cohort.id")
    java.util.List<Object[]> findAggregateMetricsByCohortIds(
            @org.springframework.data.repository.query.Param("cohortIds") java.util.Collection<Long> cohortIds);

    @org.springframework.data.jpa.repository.Query("SELECT cm.cohort.id, COUNT(cm.id) FROM CohortMember cm WHERE cm.cohort.id IN :cohortIds AND cm.enrolledAt >= :since GROUP BY cm.cohort.id")
    java.util.List<Object[]> findEnrolledCountsSinceByCohortIds(
            @org.springframework.data.repository.query.Param("cohortIds") java.util.Collection<Long> cohortIds,
            @org.springframework.data.repository.query.Param("since") java.time.Instant since);

    @org.springframework.data.jpa.repository.Query("SELECT cm.cohort.id, AVG(cm.rating), COUNT(cm.id) FROM CohortMember cm WHERE cm.cohort.id IN :cohortIds AND cm.rating IS NOT NULL AND cm.reviewedAt >= :since GROUP BY cm.cohort.id")
    java.util.List<Object[]> findRatingsSinceByCohortIds(
            @org.springframework.data.repository.query.Param("cohortIds") java.util.Collection<Long> cohortIds,
            @org.springframework.data.repository.query.Param("since") java.time.Instant since);

    @org.springframework.data.jpa.repository.Query("SELECT cm FROM CohortMember cm LEFT JOIN FETCH cm.user u WHERE cm.cohort.id = :cohortId AND cm.rating IS NOT NULL ORDER BY cm.reviewedAt DESC NULLS LAST, cm.id DESC")
    java.util.List<CohortMember> findReviewsByCohortIdWithUser(@org.springframework.data.repository.query.Param("cohortId") Long cohortId);

    @org.springframework.data.jpa.repository.Query("SELECT cm FROM CohortMember cm JOIN FETCH cm.cohort c LEFT JOIN FETCH c.createdBy LEFT JOIN FETCH c.course cr LEFT JOIN FETCH cr.createdBy WHERE cm.roleInCohort = 'STUDENT' OR cm.roleInCohort IS NULL")
    java.util.List<CohortMember> findAllStudentEnrollmentsWithCohortAndInstructor();
}
