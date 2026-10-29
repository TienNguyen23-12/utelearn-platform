package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.entity.ModerationTask;

public interface ModerationService {
    ModerationTask createCourseModerationTask(Long courseId, Long authorId);
    ModerationTask createCohortModerationTask(Long cohortId, Long authorId);
    ModerationTask createAssetModerationTask(Long assetId, Long authorId);
    void approveTask(Long taskId, Long moderatorId, String feedback);
    void rejectTask(Long taskId, Long moderatorId, String feedback);
    void escalateTask(Long taskId, Long moderatorId, String reason);
    void reassignTaskToModerator(Long taskId, Long adminId, Long targetModeratorId, String note);
    java.util.Optional<ModerationTask> getLatestTask(String itemType, Long itemId);
    void syncStatusFromExternalChange(String itemType, Long itemId, String newStatus, vn.edu.ute.utelearn.entity.User actor);
    String resolveItemTitle(String itemType, Long itemId);
    default void cleanupTasksForItem(String itemType, Long itemId) {}
}
