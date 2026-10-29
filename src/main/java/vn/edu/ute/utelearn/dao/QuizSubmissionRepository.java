package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.QuizSubmission;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
    List<QuizSubmission> findByQuizIdAndUserIdAndCohortIdOrderBySubmittedAtDesc(Long quizId, Long userId, Long cohortId);
    List<QuizSubmission> findByQuizIdAndUserIdOrderBySubmittedAtDesc(Long quizId, Long userId);
    long countByQuizIdAndUserIdAndCohortId(Long quizId, Long userId, Long cohortId);

    @Query("SELECT s FROM QuizSubmission s WHERE s.quiz.id = :quizId AND s.user.id = :userId AND s.cohort.id = :cohortId AND s.score IS NOT NULL ORDER BY s.score DESC, s.submittedAt DESC LIMIT 1")
    Optional<QuizSubmission> findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(@Param("quizId") Long quizId, @Param("userId") Long userId, @Param("cohortId") Long cohortId);

    @Query("SELECT s FROM QuizSubmission s WHERE s.quiz.id = :quizId AND s.user.id = :userId AND s.score IS NOT NULL ORDER BY s.score DESC, s.submittedAt DESC LIMIT 1")
    Optional<QuizSubmission> findTopByQuizIdAndUserIdOrderByScoreDesc(@Param("quizId") Long quizId, @Param("userId") Long userId);

    List<QuizSubmission> findByQuizIdOrderBySubmittedAtDesc(Long quizId);
    List<QuizSubmission> findByQuizIdAndCohortIdOrderBySubmittedAtDesc(Long quizId, Long cohortId);
    List<QuizSubmission> findByQuizIdAndUserIdAndCohortIdAndStatusOrderByStartedAtDesc(Long quizId, Long userId, Long cohortId, String status);
}
