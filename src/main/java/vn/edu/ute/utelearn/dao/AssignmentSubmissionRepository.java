package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.AssignmentSubmission;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    Optional<AssignmentSubmission> findByAssignmentIdAndUserIdAndCohortId(Long assignmentId, Long userId, Long cohortId);
    Optional<AssignmentSubmission> findByAssignmentIdAndUserId(Long assignmentId, Long userId);
    List<AssignmentSubmission> findByAssignmentIdAndCohortIdOrderBySubmittedAtDesc(Long assignmentId, Long cohortId);
    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);
    long countByAssignmentIdAndCohortId(Long assignmentId, Long cohortId);
    long countByAssignmentIdAndCohortIdAndStatus(Long assignmentId, Long cohortId, String status);
}
