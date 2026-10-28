package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.*;
import vn.edu.ute.utelearn.dto.NotificationEvent;
import vn.edu.ute.utelearn.entity.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final CohortScheduleRepository cohortScheduleRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 60000, initialDelay = 3000)
    @Transactional(readOnly = true)
    public void checkUpcomingDeadlines() {
        log.info("[NotificationScheduler] Bắt đầu quét các bài học sắp đến hạn nộp");
        Instant now = Instant.now();
        Instant maxAhead = now.plus(24, ChronoUnit.HOURS);
        Instant recentPast = now.minus(30, ChronoUnit.MINUTES);

        List<CohortSchedule> schedules = cohortScheduleRepository.findAll();
        for (CohortSchedule schedule : schedules) {
            Instant deadline = schedule.getDeadlineAt();
            if (deadline == null) {
                continue;
            }

            Lesson lesson = schedule.getLesson();
            Cohort cohort = schedule.getCohort();
            if (lesson == null || cohort == null) {
                continue;
            }

            String lessonType = lesson.getLessonType() != null ? lesson.getLessonType().toUpperCase() : "";
            boolean isGradedType = "ASSIGNMENT".equals(lessonType) || "QUIZ".equals(lessonType)
                    || "CODE_PRACTICE".equals(lessonType) || "EXAM".equals(lessonType);
            if (!isGradedType) {
                continue;
            }

            Assignment assignment = assignmentRepository.findByLessonId(lesson.getId()).orElse(null);
            Long referenceId = (assignment != null) ? assignment.getId() : lesson.getId();
            String lessonTitle = (assignment != null && assignment.getTitle() != null) ? assignment.getTitle() : lesson.getTitle();

            String windowKey;
            String hoursLabel;
            boolean isDueNow = false;

            if (deadline.isAfter(now) && !deadline.isAfter(maxAhead)) {
                Duration remaining = Duration.between(now, deadline);
                if (remaining.toMinutes() <= 60) {
                    windowKey = "ASSIGNMENT_DEADLINE_1H";
                    hoursLabel = "1 giờ";
                } else if (remaining.toHours() <= 6) {
                    windowKey = "ASSIGNMENT_DEADLINE_6H";
                    hoursLabel = "6 giờ";
                } else {
                    windowKey = "ASSIGNMENT_DEADLINE_24H";
                    hoursLabel = "24 giờ";
                }
            } else if (!deadline.isAfter(now) && !deadline.isBefore(recentPast)) {
                windowKey = "ASSIGNMENT_DEADLINE_DUE";
                hoursLabel = "ngay bây giờ";
                isDueNow = true;
            } else {
                continue;
            }

            List<CohortMember> members = cohortMemberRepository.findByCohort_Id(cohort.getId());
            for (CohortMember member : members) {
                User student = member.getUser();
                if (student == null) {
                    continue;
                }

                boolean alreadySubmitted = false;
                if (assignment != null) {
                    alreadySubmitted = assignmentSubmissionRepository
                            .findByAssignmentIdAndUserIdAndCohortId(assignment.getId(), student.getId(), cohort.getId())
                            .isPresent();
                }

                if (alreadySubmitted) {
                    continue;
                }

                boolean alreadyNotified = notificationRepository.existsByRecipientIdAndTypeAndReferenceTypeAndReferenceId(
                        student.getId(),
                        NotificationType.ASSIGNMENT_DEADLINE_REMINDER,
                        windowKey,
                        referenceId
                );

                if (alreadyNotified) {
                    continue;
                }

                String title = isDueNow ? "Hạn chót nộp bài đã đến" : "Nhắc nhở hạn chót nộp bài";
                String content = isDueNow
                        ? ("Bài tập '" + lessonTitle + "' trong lớp '" + cohort.getName() + "' đã đến hạn chót nộp bài. Vui lòng kiểm tra trạng thái nộp bài của bạn!")
                        : ("Bài tập '" + lessonTitle + "' trong lớp '" + cohort.getName() + "' sẽ hết hạn trong khoảng " + hoursLabel + ". Vui lòng hoàn thành và nộp bài đúng hạn!");

                NotificationEvent reminderEvent = NotificationEvent.builder()
                        .recipientId(student.getId())
                        .senderId(cohort.getCreatedBy() != null ? cohort.getCreatedBy().getId() : null)
                        .title(title)
                        .content(content)
                        .type(NotificationType.ASSIGNMENT_DEADLINE_REMINDER)
                        .category(NotificationCategory.ACADEMIC)
                        .actionUrl("/modules/" + cohort.getId() + "/learn?lessonId=" + lesson.getId())
                        .referenceId(referenceId)
                        .referenceType(windowKey)
                        .build();

                eventPublisher.publishEvent(reminderEvent);
                log.info("[NotificationScheduler] Đã phát sự kiện nhắc deadline ({}) bài tập {} cho học viên {}", hoursLabel, referenceId, student.getId());
            }
        }
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional(readOnly = true)
    public void checkUnlockedLessons() {
        log.info("[NotificationScheduler] Bắt đầu quét các bài học vừa mở khóa");
        Instant now = Instant.now();
        Instant windowStart = now.minus(16, ChronoUnit.MINUTES);

        List<CohortSchedule> schedules = cohortScheduleRepository.findAll();
        for (CohortSchedule schedule : schedules) {
            Instant unlockAt = schedule.getUnlockAt();
            if (unlockAt == null) {
                continue;
            }

            if (unlockAt.isAfter(windowStart) && !unlockAt.isAfter(now)) {
                Lesson lesson = schedule.getLesson();
                Cohort cohort = schedule.getCohort();
                if (lesson == null || cohort == null) {
                    continue;
                }

                List<CohortMember> members = cohortMemberRepository.findByCohort_Id(cohort.getId());
                for (CohortMember member : members) {
                    User student = member.getUser();
                    if (student == null) {
                        continue;
                    }

                    boolean alreadyNotified = notificationRepository.existsByRecipientIdAndTypeAndReferenceTypeAndReferenceId(
                            student.getId(),
                            NotificationType.LESSON_UNLOCKED,
                            "LESSON_UNLOCK",
                            lesson.getId()
                    );

                    if (alreadyNotified) {
                        continue;
                    }

                    NotificationEvent unlockEvent = NotificationEvent.builder()
                            .recipientId(student.getId())
                            .senderId(cohort.getCreatedBy() != null ? cohort.getCreatedBy().getId() : null)
                            .title("Bài học mới đã mở khóa")
                            .content("Bài học '" + lesson.getTitle() + "' trong đợt học '" + cohort.getName() + "' đã được mở khóa theo lịch trình. Hãy vào học ngay!")
                            .type(NotificationType.LESSON_UNLOCKED)
                            .category(NotificationCategory.ACADEMIC)
                            .actionUrl("/modules/" + cohort.getId() + "/learn?lessonId=" + lesson.getId())
                            .referenceId(lesson.getId())
                            .referenceType("LESSON_UNLOCK")
                            .build();

                    eventPublisher.publishEvent(unlockEvent);
                }
            }
        }
    }
}
