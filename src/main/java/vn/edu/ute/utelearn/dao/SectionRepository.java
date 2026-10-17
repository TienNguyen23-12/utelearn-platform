package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Section;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    @EntityGraph(attributePaths = {"cohort"})
    List<Section> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    @EntityGraph(attributePaths = {"cohort"})
    List<Section> findByCohortIdOrderByOrderIndexAsc(Long cohortId);

    @EntityGraph(attributePaths = {"cohort"})
    @Query("SELECT DISTINCT s FROM Section s LEFT JOIN s.cohort c WHERE c.id = :cohortId OR s.id IN (SELECT cs.lesson.section.id FROM CohortSchedule cs WHERE cs.cohort.id = :cohortId) ORDER BY s.orderIndex ASC, s.id ASC")
    List<Section> findSectionsForCohort(@Param("cohortId") Long cohortId);
}
