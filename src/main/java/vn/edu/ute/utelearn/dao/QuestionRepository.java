package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Question;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuizIdOrderByOrderIndexAscCreatedAtAsc(Long quizId);
    List<Question> findByQuizIdOrderByCreatedAtAsc(Long quizId);
    List<Question> findByQuizIdOrderByIdAsc(Long quizId);
    long countByQuizId(Long quizId);
    void deleteByQuizId(Long quizId);
}
