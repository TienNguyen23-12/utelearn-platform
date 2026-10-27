package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.QuizSubmission;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
    List<QuizSubmission> findByQuizIdAndUserIdAndCohortIdOrderBySubmittedAtDesc(Long quizId, Long userId, Long cohortId);
    List<QuizSubmission> findByQuizIdAndUserIdOrderBySubmittedAtDesc(Long quizId, Long userId);
    long countByQuizIdAndUserIdAndCohortId(Long quizId, Long userId, Long cohortId);
    Optional<QuizSubmission> findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(Long quizId, Long userId, Long cohortId);
    Optional<QuizSubmission> findTopByQuizIdAndUserIdOrderByScoreDesc(Long quizId, Long userId);
    List<QuizSubmission> findByQuizIdOrderBySubmittedAtDesc(Long quizId);
}
