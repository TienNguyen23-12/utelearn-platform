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

    @org.springframework.data.jpa.repository.Query("SELECT cm FROM CohortMember cm JOIN FETCH cm.cohort c JOIN FETCH c.course cr LEFT JOIN FETCH cr.createdBy LEFT JOIN FETCH cr.category LEFT JOIN FETCH cm.lastLesson WHERE cm.user = :user ORDER BY cm.enrolledAt DESC")
    java.util.List<CohortMember> findByUserOrderByEnrolledAtDesc(
            @org.springframework.data.repository.query.Param("user") vn.edu.ute.utelearn.entity.User user);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT cm.user.id) FROM CohortMember cm WHERE cm.cohort.course.id = :courseId")
    long countDistinctUsersByCourseId(@org.springframework.data.repository.query.Param("courseId") Long courseId);
}
