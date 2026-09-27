package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.ModerationTask;

@Repository
public interface ModerationTaskRepository extends JpaRepository<ModerationTask, Long> {
    long countByStatus(String status);
    long countByStatusAndCreatedAtBetween(String status, java.time.Instant start, java.time.Instant end);
}
