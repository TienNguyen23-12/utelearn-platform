package vn.edu.ute.utelearn.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Notification;
import vn.edu.ute.utelearn.entity.NotificationCategory;
import vn.edu.ute.utelearn.entity.NotificationType;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"sender"})
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    Page<Notification> findByRecipientIdAndCategoryOrderByCreatedAtDesc(Long recipientId, NotificationCategory category, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    Page<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDesc(Long recipientId, Boolean isRead, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    Page<Notification> findByRecipientIdAndCategoryAndIsReadOrderByCreatedAtDesc(Long recipientId, NotificationCategory category, Boolean isRead, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    List<Notification> findTop10ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.recipient.id = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId, @Param("readAt") Instant readAt);

    boolean existsByRecipientIdAndTypeAndReferenceTypeAndReferenceId(Long recipientId, NotificationType type, String referenceType, Long referenceId);
}
