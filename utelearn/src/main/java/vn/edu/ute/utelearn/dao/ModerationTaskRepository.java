package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.ModerationTask;

import java.util.List;

@Repository
public interface ModerationTaskRepository extends JpaRepository<ModerationTask, Long> {
    
    @EntityGraph(attributePaths = {"author"})
    List<ModerationTask> findByStatusOrderByCreatedAtAsc(String status);
    
    @EntityGraph(attributePaths = {"author", "escalatedBy"})
    List<ModerationTask> findByStatusAndEscalationLevelGreaterThanOrderByCreatedAtAsc(String status, Integer escalationLevel);
    
    @EntityGraph(attributePaths = {"author", "cohort", "assignedTo", "escalatedBy"})
    java.util.Optional<ModerationTask> findById(Long id);
}
