package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.ModerationService;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class ModerationServiceImpl implements ModerationService {

    private final ModerationTaskRepository moderationTaskRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    public ModerationTask createCourseModerationTask(Long courseId, Long authorId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Basic implementation: assign to a specific moderator if least-loaded logic is complex
        // For simplicity right now, it goes into the queue unassigned, or assigned to a default moderator.
        // The SRS says Least-Loaded Dispatcher. We will leave assignedTo as null so it goes to the pool,
        // and moderators can "claim" it.

        course.setStatus("PENDING_MODERATOR");
        courseRepository.save(course);

        ModerationTask task = ModerationTask.builder()
                .itemType("COURSE")
                .itemId(courseId)
                .author(author)
                .status("PENDING")
                .assignedAt(Instant.now())
                .escalationLevel(1)
                .build();

        return moderationTaskRepository.save(task);
    }

    @Override
    public void approveTask(Long taskId, Long moderatorId, String feedback) {
        ModerationTask task = getTask(taskId);
        task.setStatus("APPROVED");
        task.setFeedback(feedback);
        moderationTaskRepository.save(task);

        if ("COURSE".equals(task.getItemType())) {
            Course course = courseRepository.findById(task.getItemId()).orElseThrow();
            course.setStatus("APPROVED"); // Course is now approved
            courseRepository.save(course);
        }
    }

    @Override
    public void rejectTask(Long taskId, Long moderatorId, String feedback) {
        ModerationTask task = getTask(taskId);
        task.setStatus("REJECTED");
        task.setFeedback(feedback);
        moderationTaskRepository.save(task);

        if ("COURSE".equals(task.getItemType())) {
            Course course = courseRepository.findById(task.getItemId()).orElseThrow();
            course.setStatus("REJECTED"); // Goes back to instructor as rejected
            courseRepository.save(course);
        }
    }

    @Override
    public void escalateTask(Long taskId, Long moderatorId, String reason) {
        ModerationTask task = getTask(taskId);
        task.setStatus("ESCALATED");
        task.setEscalationLevel(2); // Escalated to Admin
        User escalater = userRepository.findById(moderatorId).orElseThrow();
        task.setEscalatedBy(escalater);
        task.setEscalationReason(reason);
        moderationTaskRepository.save(task);
        
        if ("COURSE".equals(task.getItemType())) {
            Course course = courseRepository.findById(task.getItemId()).orElseThrow();
            course.setStatus("PENDING_ADMIN");
            courseRepository.save(course);
        }
    }

    private ModerationTask getTask(Long taskId) {
        return moderationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }
}
