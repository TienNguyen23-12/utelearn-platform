package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.edu.ute.utelearn.dto.NotificationEvent;
import vn.edu.ute.utelearn.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("[NotificationEventListener] Tiếp nhận sự kiện thông báo: {} cho recipientId: {}", event.type(), event.recipientId());
        try {
            notificationService.sendNotification(event);
        } catch (Exception e) {
            log.error("[NotificationEventListener] Lỗi khi xử lý sự kiện thông báo: {}", e.getMessage(), e);
        }
    }
}
