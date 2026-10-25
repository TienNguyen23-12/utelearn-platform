package vn.edu.ute.utelearn.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.CohortMessage;

import java.util.List;

@Repository
public interface CohortMessageRepository extends JpaRepository<CohortMessage, Long> {

    @Query("SELECT m FROM CohortMessage m JOIN FETCH m.sender s WHERE m.cohort.id = :cohortId ORDER BY m.createdAt ASC")
    List<CohortMessage> findRecentMessagesByCohortId(@Param("cohortId") Long cohortId);

    @Query("SELECT m FROM CohortMessage m JOIN FETCH m.sender s WHERE m.cohort.id = :cohortId ORDER BY m.createdAt DESC")
    List<CohortMessage> findRecentMessagesByCohortIdDesc(@Param("cohortId") Long cohortId, Pageable pageable);

    @Query("SELECT m FROM CohortMessage m JOIN FETCH m.sender s JOIN FETCH m.cohort c WHERE m.cohort.id IN :cohortIds AND m.sender.id <> :userId ORDER BY m.createdAt DESC")
    List<CohortMessage> findRecentMessagesByCohortIdsAndNotSender(
            @Param("cohortIds") List<Long> cohortIds,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("SELECT m.cohort.id, MAX(m.createdAt) FROM CohortMessage m WHERE m.cohort.id IN :cohortIds GROUP BY m.cohort.id")
    List<Object[]> findLatestMessageTimesByCohortIds(@Param("cohortIds") List<Long> cohortIds);
}
