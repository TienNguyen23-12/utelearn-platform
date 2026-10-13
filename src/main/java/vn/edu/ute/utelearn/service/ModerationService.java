package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.entity.ModerationTask;

public interface ModerationService {
    ModerationTask createCourseModerationTask(Long courseId, Long authorId);
    void approveTask(Long taskId, Long moderatorId, String feedback);
    void rejectTask(Long taskId, Long moderatorId, String feedback);
    void escalateTask(Long taskId, Long moderatorId, String reason);
}
