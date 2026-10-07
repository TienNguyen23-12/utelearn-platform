package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.CohortMember;

import java.util.Optional;

@Repository
public interface CohortMemberRepository extends JpaRepository<CohortMember, Long> {
    boolean existsByCohort_Course_IdAndUser_Id(Long courseId, Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT cm FROM CohortMember cm JOIN FETCH cm.cohort c JOIN FETCH c.course cr LEFT JOIN FETCH cr.createdBy LEFT JOIN FETCH cr.category WHERE cm.user = :user ORDER BY cm.enrolledAt DESC")
    java.util.List<CohortMember> findByUserOrderByEnrolledAtDesc(
            @org.springframework.data.repository.query.Param("user") vn.edu.ute.utelearn.entity.User user);
}
