package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.CohortSchedule;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CohortScheduleRepository extends JpaRepository<CohortSchedule, Long> {
    List<CohortSchedule> findByCohortId(Long cohortId);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"cohort"})
    List<CohortSchedule> findByLessonId(Long lessonId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"cohort"})
    Optional<CohortSchedule> findByCohortIdAndLessonId(Long cohortId, Long lessonId);
    
    @org.springframework.transaction.annotation.Transactional
    void deleteByCohortId(Long cohortId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByLessonId(Long lessonId);

    @Query("SELECT cs.lesson.id FROM CohortSchedule cs JOIN CohortMember cm ON cs.cohort.id = cm.cohort.id WHERE cm.user.id = :userId AND cs.cohort.course.id = :courseId")
    Set<Long> findUnlockedLessonIdsByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT cs.lesson.id FROM CohortSchedule cs WHERE cs.cohort.id = :cohortId")
    List<Long> findLessonIdsByCohortId(@Param("cohortId") Long cohortId);

    @Query("SELECT cs FROM CohortSchedule cs JOIN FETCH cs.lesson l JOIN FETCH cs.cohort c LEFT JOIN FETCH l.section s WHERE cs.cohort.id = :cohortId ORDER BY s.orderIndex ASC, s.id ASC, l.orderIndex ASC, l.id ASC")
    List<CohortSchedule> findByCohortIdWithDetails(@Param("cohortId") Long cohortId);

    @Query("SELECT cs FROM CohortSchedule cs JOIN FETCH cs.cohort WHERE cs.lesson.section.id = :sectionId")
    List<CohortSchedule> findBySectionId(@Param("sectionId") Long sectionId);
}
