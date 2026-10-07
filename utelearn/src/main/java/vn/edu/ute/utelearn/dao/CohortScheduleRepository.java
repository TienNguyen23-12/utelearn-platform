package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.CohortSchedule;

import java.util.List;
import java.util.Optional;

@Repository
public interface CohortScheduleRepository extends JpaRepository<CohortSchedule, Long> {
    List<CohortSchedule> findByCohortId(Long cohortId);
    
    Optional<CohortSchedule> findByCohortIdAndLessonId(Long cohortId, Long lessonId);
    
    void deleteByCohortId(Long cohortId);
}
