package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.entity.LessonCompletion;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonCompletionRepository extends JpaRepository<LessonCompletion, Long> {

    List<LessonCompletion> findByCohort_IdAndUser_Id(Long cohortId, Long userId);

    @Query("SELECT lc.lesson.id FROM LessonCompletion lc WHERE lc.cohort.id = :cohortId AND lc.user.id = :userId")
    List<Long> findCompletedLessonIds(@Param("cohortId") Long cohortId, @Param("userId") Long userId);

    boolean existsByCohort_IdAndLesson_IdAndUser_Id(Long cohortId, Long lessonId, Long userId);

    Optional<LessonCompletion> findByCohort_IdAndLesson_IdAndUser_Id(Long cohortId, Long lessonId, Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM LessonCompletion lc WHERE lc.cohort.id = :cohortId AND lc.lesson.id = :lessonId AND lc.user.id = :userId")
    void deleteByCohortIdAndLessonIdAndUserId(@Param("cohortId") Long cohortId, @Param("lessonId") Long lessonId, @Param("userId") Long userId);

    long countByCohort_IdAndUser_Id(Long cohortId, Long userId);

    @Modifying
    @Transactional
    void deleteByCohort_Id(Long cohortId);

    @Modifying
    @Transactional
    void deleteByLesson_Id(Long lessonId);
}
