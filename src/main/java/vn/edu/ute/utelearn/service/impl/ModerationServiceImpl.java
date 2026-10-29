package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.NotificationCategory;
import vn.edu.ute.utelearn.dto.NotificationEvent;
import vn.edu.ute.utelearn.entity.NotificationType;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.NotificationService;
import vn.edu.ute.utelearn.service.ModerationService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ModerationServiceImpl implements ModerationService {

    private final ModerationTaskRepository moderationTaskRepository;
    private final CourseRepository courseRepository;
    private final CohortRepository cohortRepository;
    private final UserRepository userRepository;
    private final ModeratorAttendanceRepository moderatorAttendanceRepository;
    private final vn.edu.ute.utelearn.dao.LessonRepository lessonRepository;
    private final vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;
    private final vn.edu.ute.utelearn.dao.SectionRepository sectionRepository;
    private final NotificationService notificationService;
    private final vn.edu.ute.utelearn.dao.InstructorAssetRepository assetRepository;

    @Override
    public ModerationTask createCourseModerationTask(Long courseId, Long authorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        course.setStatus("PENDING_MODERATOR");
        courseRepository.save(course);

        Instant now = Instant.now();
        ModerationTask task = ModerationTask.builder()
                .itemType("COURSE")
                .itemId(courseId)
                .author(author)
                .status("PENDING")
                .assignedAt(now)
                .deadlineAt(now.plus(48, ChronoUnit.HOURS))
                .estimatedReviewMinutes(60)
                .escalationLevel(1)
                .build();

        ModerationTask savedTask = moderationTaskRepository.save(task);
        notifyModeratorsOnSubmission(savedTask, author, "khóa học", course.getTitle(), "COURSE_MODERATION");
        log.info("[ModerationService] Khoi tao nhiem vu kiem duyet id: {}, courseId: {}, authorId: {}, deadlineAt: {}",
                savedTask.getId(), courseId, authorId, savedTask.getDeadlineAt());
        return savedTask;
    }

    @Override
    public ModerationTask createCohortModerationTask(Long cohortId, Long authorId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Cohort not found"));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        cohort.setModerationStatus("PENDING_MODERATOR");
        cohortRepository.save(cohort);

        Instant now = Instant.now();
        ModerationTask task = ModerationTask.builder()
                .itemType("COHORT")
                .itemId(cohortId)
                .author(author)
                .cohort(cohort)
                .status("PENDING")
                .assignedAt(now)
                .deadlineAt(now.plus(48, ChronoUnit.HOURS))
                .estimatedReviewMinutes(30)
                .escalationLevel(1)
                .build();

        ModerationTask savedTask = moderationTaskRepository.save(task);
        notifyModeratorsOnSubmission(savedTask, author, "module", cohort.getName(), "COHORT_MODERATION");
        log.info("[ModerationService] Khoi tao nhiem vu kiem duyet module id: {}, cohortId: {}, authorId: {}, deadlineAt: {}",
                savedTask.getId(), cohortId, authorId, savedTask.getDeadlineAt());
        return savedTask;
    }

    @Override
    public ModerationTask createAssetModerationTask(Long assetId, Long authorId) {
        vn.edu.ute.utelearn.entity.InstructorAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        asset.setStatus("PENDING_MODERATOR");
        assetRepository.save(asset);

        Instant now = Instant.now();
        ModerationTask task = ModerationTask.builder()
                .itemType("ASSET")
                .itemId(assetId)
                .author(author)
                .status("PENDING")
                .assignedAt(now)
                .deadlineAt(now.plus(48, ChronoUnit.HOURS))
                .estimatedReviewMinutes(15)
                .escalationLevel(1)
                .build();

        ModerationTask savedTask = moderationTaskRepository.save(task);
        notifyModeratorsOnSubmission(savedTask, author, "tài nguyên số", asset.getName(), "ASSET_MODERATION");
        log.info("[ModerationService] Khoi tao nhiem vu kiem duyet tai nguyen id: {}, assetId: {}, authorId: {}, deadlineAt: {}",
                savedTask.getId(), assetId, authorId, savedTask.getDeadlineAt());
        return savedTask;
    }

    @Override
    public void approveTask(Long taskId, Long moderatorId, String feedback) {
        ModerationTask task = getTask(taskId);
        validateModeratorEligibility(task, moderatorId);

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setStatus("APPROVED");
        task.setFeedback(feedback);
        task.setAssignedTo(moderator);
        moderationTaskRepository.save(task);

        String itemLabel = "khóa học";
        String itemTitle = "";
        User author = task.getAuthor();
        if ("COURSE".equals(task.getItemType())) {
            Course course = courseRepository.findById(task.getItemId()).orElseThrow();
            course.setStatus("PUBLISHED");
            courseRepository.save(course);
            publishLessonsForCourse(course.getId());
            itemTitle = course.getTitle();
            if (author == null) {
                author = course.getCreatedBy();
            }
        } else if ("COHORT".equals(task.getItemType())) {
            Cohort cohort = cohortRepository.findById(task.getItemId()).orElseThrow();
            cohort.setModerationStatus("APPROVED");
            cohortRepository.save(cohort);
            publishLessonsForCohort(cohort.getId());
            itemLabel = "module";
            itemTitle = cohort.getName();
            if (author == null) {
                author = cohort.getCreatedBy();
            }
        } else if ("ASSET".equals(task.getItemType())) {
            vn.edu.ute.utelearn.entity.InstructorAsset asset = assetRepository.findById(task.getItemId()).orElseThrow();
            asset.setStatus("APPROVED");
            assetRepository.save(asset);
            itemLabel = "tài nguyên số";
            itemTitle = asset.getName();
            if (author == null) {
                author = asset.getInstructor();
            }
        }

        recordModeratorProductivity(moderator);
        notifyAuthorOnDecision(task, author, moderator, itemLabel, itemTitle, true, feedback);
        log.info("[ModerationService] Phe duyet thanh cong nhiem vu kiem duyet id: {}, moderatorId: {}", taskId, moderatorId);
    }

    @Override
    public void rejectTask(Long taskId, Long moderatorId, String feedback) {
        ModerationTask task = getTask(taskId);
        validateModeratorEligibility(task, moderatorId);

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setStatus("REJECTED");
        task.setFeedback(feedback);
        task.setAssignedTo(moderator);
        moderationTaskRepository.save(task);

        String itemLabel = "khóa học";
        String itemTitle = "";
        User author = task.getAuthor();
        if ("COURSE".equals(task.getItemType())) {
            Course course = courseRepository.findById(task.getItemId()).orElseThrow();
            course.setStatus("REJECTED");
            courseRepository.save(course);
            itemTitle = course.getTitle();
            if (author == null) {
                author = course.getCreatedBy();
            }
        } else if ("COHORT".equals(task.getItemType())) {
            Cohort cohort = cohortRepository.findById(task.getItemId()).orElseThrow();
            cohort.setModerationStatus("REJECTED");
            cohortRepository.save(cohort);
            itemLabel = "module";
            itemTitle = cohort.getName();
            if (author == null) {
                author = cohort.getCreatedBy();
            }
        } else if ("ASSET".equals(task.getItemType())) {
            vn.edu.ute.utelearn.entity.InstructorAsset asset = assetRepository.findById(task.getItemId()).orElseThrow();
            asset.setStatus("REJECTED");
            assetRepository.save(asset);
            itemLabel = "tài nguyên số";
            itemTitle = asset.getName();
            if (author == null) {
                author = asset.getInstructor();
            }
        }

        recordModeratorProductivity(moderator);
        notifyAuthorOnDecision(task, author, moderator, itemLabel, itemTitle, false, feedback);
        log.info("[ModerationService] Tu choi nhiem vu kiem duyet id: {}, moderatorId: {}", taskId, moderatorId);
    }

    @Override
    public void escalateTask(Long taskId, Long moderatorId, String reason) {
        ModerationTask task = getTask(taskId);
        validateModeratorEligibility(task, moderatorId);

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.setStatus("ESCALATED");
        task.setEscalationLevel(2);
        task.setEscalatedBy(moderator);
        task.setEscalationReason(reason);
        task.setAssignedTo(moderator);
        moderationTaskRepository.save(task);

        if ("COURSE".equals(task.getItemType())) {
            Course course = courseRepository.findById(task.getItemId()).orElseThrow();
            course.setStatus("PENDING_ADMIN");
            courseRepository.save(course);
        } else if ("COHORT".equals(task.getItemType())) {
            Cohort cohort = cohortRepository.findById(task.getItemId()).orElseThrow();
            cohort.setModerationStatus("PENDING_ADMIN");
            cohortRepository.save(cohort);
        } else if ("ASSET".equals(task.getItemType())) {
            vn.edu.ute.utelearn.entity.InstructorAsset asset = assetRepository.findById(task.getItemId()).orElseThrow();
            asset.setStatus("PENDING_ADMIN");
            assetRepository.save(asset);
        }

        recordModeratorProductivity(moderator);
        log.info("[ModerationService] Chuyen cap len Admin nhiem vu kiem duyet id: {}, moderatorId: {}", taskId, moderatorId);
    }

    @Override
    public void reassignTaskToModerator(Long taskId, Long adminId, Long targetModeratorId, String note) {
        ModerationTask task = getTask(taskId);
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        boolean isAdmin = SecurityUtils.isAdmin(admin);
        if (!isAdmin) {
            throw new IllegalStateException("Chỉ Quản trị viên (Admin) mới có quyền chuyển giao việc duyệt.");
        }

        User targetModerator = null;
        if (targetModeratorId != null) {
            targetModerator = userRepository.findById(targetModeratorId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kiểm duyệt viên được chỉ định."));
            if (task.getAuthor() != null && task.getAuthor().getId().equals(targetModerator.getId())) {
                throw new IllegalStateException("Không thể chỉ định tác giả của khóa học hoặc module làm người kiểm duyệt.");
            }
        }

        task.setStatus("PENDING");
        task.setEscalationLevel(1);
        task.setAssignedTo(targetModerator);

        String instruction = (note != null && !note.isBlank()) ? note.trim() : "Chuyển giao lại bởi Admin";
        task.setEscalationReason(instruction);
        moderationTaskRepository.save(task);

        if ("COURSE".equals(task.getItemType())) {
            Course course = courseRepository.findById(task.getItemId()).orElseThrow();
            course.setStatus("PENDING_MODERATOR");
            courseRepository.save(course);
        } else if ("COHORT".equals(task.getItemType())) {
            Cohort cohort = cohortRepository.findById(task.getItemId()).orElseThrow();
            cohort.setModerationStatus("PENDING_MODERATOR");
            cohortRepository.save(cohort);
        } else if ("ASSET".equals(task.getItemType())) {
            vn.edu.ute.utelearn.entity.InstructorAsset asset = assetRepository.findById(task.getItemId()).orElseThrow();
            asset.setStatus("PENDING_MODERATOR");
            assetRepository.save(asset);
        }

        log.info("[ModerationService] Admin {} chuyen giao task {} ve Moderator {}: {}",
                adminId, taskId, targetModerator != null ? targetModerator.getId() : "QUEUE", instruction);
    }

    private void validateModeratorEligibility(ModerationTask task, Long moderatorId) {
        if (task.getAuthor() != null && task.getAuthor().getId().equals(moderatorId)) {
            log.warn("[ModerationService] Chan tu kiem duyet: moderatorId {} la tac gia cua task {}", moderatorId, task.getId());
            throw new IllegalStateException("Kiểm duyệt viên không được phép tự kiểm duyệt khóa học do chính mình tạo ra.");
        }

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = SecurityUtils.isAdmin(moderator);

        if (isAdmin) {
            return;
        }

        ModeratorAttendance attendance = moderatorAttendanceRepository
                .findByModeratorAndWorkDate(moderator, LocalDate.now())
                .orElse(null);

        if (attendance == null || !Boolean.TRUE.equals(attendance.getIsActive())) {
            log.warn("[ModerationService] Moderator {} chua diem danh ca truc hoac ca truc da dong", moderatorId);
            throw new IllegalStateException("Kiểm duyệt viên chưa điểm danh ca trực hôm nay hoặc ca trực đã kết thúc.");
        }
    }

    private void recordModeratorProductivity(User moderator) {
        if (moderator == null) {
            return;
        }
        moderatorAttendanceRepository.findByModeratorAndWorkDate(moderator, LocalDate.now())
                .ifPresent(attendance -> {
                    int completed = attendance.getCompletedTasksCount() != null ? attendance.getCompletedTasksCount() : 0;
                    attendance.setCompletedTasksCount(completed + 1);
                    moderatorAttendanceRepository.save(attendance);
                    log.info("[ModerationService] Cap nhat nang suat ca truc cho moderator {}: completedTasksCount = {}",
                            moderator.getId(), attendance.getCompletedTasksCount());
                });
    }

    @Override
    public java.util.Optional<ModerationTask> getLatestTask(String itemType, Long itemId) {
        return moderationTaskRepository.findTopByItemTypeAndItemIdOrderByCreatedAtDesc(itemType, itemId);
    }

    @Override
    public void syncStatusFromExternalChange(String itemType, Long itemId, String newStatus, User actor) {
        java.util.List<ModerationTask> pendingTasks = moderationTaskRepository.findByItemTypeAndItemIdAndStatusIn(
                itemType, itemId, java.util.List.of("PENDING", "ESCALATED"));
        for (ModerationTask task : pendingTasks) {
            Boolean approved = null;
            if ("PUBLISHED".equalsIgnoreCase(newStatus) || "APPROVED".equalsIgnoreCase(newStatus)) {
                task.setStatus("APPROVED");
                task.setAssignedTo(actor);
                task.setFeedback("Được phê duyệt trực tiếp bởi " + (actor != null && actor.getFullName() != null ? actor.getFullName() : "Admin"));
                approved = true;
            } else if ("REJECTED".equalsIgnoreCase(newStatus)) {
                task.setStatus("REJECTED");
                task.setAssignedTo(actor);
                if (task.getFeedback() == null || task.getFeedback().isBlank()) {
                    task.setFeedback("Bị từ chối trực tiếp bởi " + (actor != null && actor.getFullName() != null ? actor.getFullName() : "Admin"));
                }
                approved = false;
            }
            moderationTaskRepository.save(task);
            if (approved != null && task.getAuthor() != null && task.getAuthor().getId() != null) {
                String itemLabel = "COHORT".equalsIgnoreCase(itemType) ? "module" : ("ASSET".equalsIgnoreCase(itemType) ? "tài nguyên số" : "khóa học");
                String itemTitle = resolveItemTitle(itemType, itemId);
                notifyAuthorOnDecision(task, task.getAuthor(), actor, itemLabel, itemTitle, approved, task.getFeedback());
            }
            log.info("[ModerationService] Dong bo trang thai task {} thanh {} tu thao tac quan tri", task.getId(), task.getStatus());
        }
    }

    @Override
    public String resolveItemTitle(String itemType, Long itemId) {
        if (itemId == null) {
            return "";
        }
        if ("COHORT".equalsIgnoreCase(itemType)) {
            return cohortRepository.findById(itemId).map(Cohort::getName).orElse("");
        }
        if ("ASSET".equalsIgnoreCase(itemType)) {
            return assetRepository.findById(itemId).map(vn.edu.ute.utelearn.entity.InstructorAsset::getName).orElse("");
        }
        return courseRepository.findById(itemId).map(Course::getTitle).orElse("");
    }

    private void notifyModeratorsOnSubmission(ModerationTask task, User author, String itemLabel, String itemTitle, String referenceType) {
        if (notificationService == null || task == null) {
            return;
        }
        try {
            Long authorId = author != null ? author.getId() : null;
            String authorName = (author != null && author.getFullName() != null && !author.getFullName().isBlank())
                    ? author.getFullName()
                    : "Giảng viên";
            List<Long> moderatorIds = userRepository.findActiveModerators().stream()
                    .map(User::getId)
                    .filter(id -> id != null && !id.equals(authorId))
                    .toList();
            if (moderatorIds.isEmpty()) {
                return;
            }
            String safeTitle = (itemTitle != null && !itemTitle.isBlank()) ? itemTitle : ("#" + task.getItemId());
            NotificationEvent event = NotificationEvent.builder()
                    .senderId(authorId)
                    .title("Yêu cầu kiểm duyệt " + itemLabel + " mới")
                    .content("Giảng viên " + authorName + " vừa gửi yêu cầu kiểm duyệt " + itemLabel + " \"" + safeTitle + "\".")
                    .type(NotificationType.MODERATION_SUBMITTED)
                    .category(NotificationCategory.SYSTEM)
                    .actionUrl("/moderator/review/" + task.getId())
                    .referenceId(task.getId())
                    .referenceType(referenceType)
                    .build();
            notificationService.sendNotificationToUsers(moderatorIds, event);
        } catch (Exception e) {
            log.warn("[ModerationService] Không thể gửi thông báo kiểm duyệt mới cho task {}: {}", task.getId(), e.getMessage());
        }
    }

    private void notifyAuthorOnDecision(ModerationTask task, User author, User moderator, String itemLabel, String itemTitle, boolean approved, String feedback) {
        if (notificationService == null || task == null || author == null || author.getId() == null) {
            return;
        }
        try {
            Long authorId = author.getId();
            Long moderatorId = moderator != null ? moderator.getId() : null;
            String safeTitle = (itemTitle != null && !itemTitle.isBlank()) ? itemTitle : ("#" + task.getItemId());
            String title = approved
                    ? ("Phê duyệt " + itemLabel + " thành công")
                    : ("Từ chối kiểm duyệt " + itemLabel);
            String baseContent = approved
                    ? ("Nội dung " + itemLabel + " \"" + safeTitle + "\" của bạn đã được phê duyệt và xuất bản.")
                    : ("Nội dung " + itemLabel + " \"" + safeTitle + "\" của bạn chưa đạt yêu cầu kiểm duyệt.");
            String cleanFeedback = (feedback != null && !feedback.isBlank()) ? feedback.trim() : null;
            String content = cleanFeedback != null
                    ? (baseContent + (approved ? " Phản hồi: " : " Lý do: ") + cleanFeedback)
                    : baseContent;
            String actionUrl = "ASSET".equalsIgnoreCase(task.getItemType())
                    ? "/instructor/assets"
                    : ("COHORT".equalsIgnoreCase(task.getItemType())
                            ? ("/instructor/cohorts/" + task.getItemId() + "/curriculum")
                            : ("/instructor/courses/" + task.getItemId() + "/curriculum"));
            String referenceType = "ASSET".equalsIgnoreCase(task.getItemType())
                    ? "ASSET_MODERATION"
                    : ("COHORT".equalsIgnoreCase(task.getItemType())
                            ? "COHORT_MODERATION"
                            : "COURSE_MODERATION");

            NotificationEvent event = NotificationEvent.builder()
                    .recipientId(authorId)
                    .senderId(moderatorId)
                    .title(title)
                    .content(content)
                    .type(approved ? NotificationType.MODERATION_APPROVED : NotificationType.MODERATION_REJECTED)
                    .category(NotificationCategory.SYSTEM)
                    .actionUrl(actionUrl)
                    .referenceId(task.getId())
                    .referenceType(referenceType)
                    .build();
            notificationService.sendNotification(event);
        } catch (Exception e) {
            log.warn("[ModerationService] Không thể gửi thông báo kết quả kiểm duyệt cho task {}: {}", task.getId(), e.getMessage());
        }
    }

    private void publishLessonsForCourse(Long courseId) {
        java.util.List<vn.edu.ute.utelearn.entity.Lesson> courseLessons = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(courseId);
        for (vn.edu.ute.utelearn.entity.Lesson l : courseLessons) {
            l.setStatus("PUBLISHED");
        }
        if (!courseLessons.isEmpty()) {
            lessonRepository.saveAll(courseLessons);
            log.info("[ModerationService] Đã xuất bản {} bài học cho khóa học id: {}", courseLessons.size(), courseId);
        }
    }

    private void publishLessonsForCohort(Long cohortId) {
        java.util.Set<vn.edu.ute.utelearn.entity.Lesson> lessonsToPublish = new java.util.HashSet<>();
        java.util.List<vn.edu.ute.utelearn.entity.CohortSchedule> schedules = cohortScheduleRepository.findByCohortIdWithDetails(cohortId);
        for (vn.edu.ute.utelearn.entity.CohortSchedule cs : schedules) {
            if (cs.getLesson() != null) {
                lessonsToPublish.add(cs.getLesson());
            }
        }
        java.util.List<vn.edu.ute.utelearn.entity.Section> cohortSections = sectionRepository.findByCohortIdOrderByOrderIndexAsc(cohortId);
        for (vn.edu.ute.utelearn.entity.Section sec : cohortSections) {
            lessonsToPublish.addAll(lessonRepository.findBySectionIdOrderByOrderIndexAsc(sec.getId()));
        }
        for (vn.edu.ute.utelearn.entity.Lesson l : lessonsToPublish) {
            l.setStatus("PUBLISHED");
        }
        if (!lessonsToPublish.isEmpty()) {
            lessonRepository.saveAll(lessonsToPublish);
            log.info("[ModerationService] Đã xuất bản {} bài học cho module id: {}", lessonsToPublish.size(), cohortId);
        }
    }

    private ModerationTask getTask(Long taskId) {
        return moderationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }
}
