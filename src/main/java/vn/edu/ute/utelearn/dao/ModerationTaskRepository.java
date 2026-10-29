package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.ModerationTask;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModerationTaskRepository extends JpaRepository<ModerationTask, Long> {
    
    @EntityGraph(attributePaths = {"author", "escalatedBy", "cohort"})
    List<ModerationTask> findByStatusOrderByCreatedAtAsc(String status);
    
    @EntityGraph(attributePaths = {"author", "escalatedBy", "cohort"})
    List<ModerationTask> findByStatusAndEscalationLevelGreaterThanOrderByCreatedAtAsc(String status, Integer escalationLevel);

    @EntityGraph(attributePaths = {"author", "assignedTo", "escalatedBy", "cohort"})
    List<ModerationTask> findByStatusInOrderByCreatedAtDesc(Collection<String> statuses);
    
    @EntityGraph(attributePaths = {"author", "cohort", "assignedTo", "escalatedBy"})
    Optional<ModerationTask> findById(Long id);

    long countByStatus(String status);
    long countByStatusIn(Collection<String> statuses);
    long countByStatusAndCreatedAtBetween(String status, java.time.Instant start, java.time.Instant end);

    @EntityGraph(attributePaths = {"author", "assignedTo"})
    Optional<ModerationTask> findTopByItemTypeAndItemIdOrderByCreatedAtDesc(String itemType, Long itemId);

    List<ModerationTask> findByItemTypeAndItemIdAndStatusIn(String itemType, Long itemId, Collection<String> statuses);

    @EntityGraph(attributePaths = {"assignedTo", "escalatedBy"})
    @org.springframework.data.jpa.repository.Query("SELECT t FROM ModerationTask t")
    List<ModerationTask> findAllWithAssignees();
}
