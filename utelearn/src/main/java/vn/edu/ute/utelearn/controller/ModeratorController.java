package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;

import java.util.List;

@Controller
@RequestMapping("/moderator")
@RequiredArgsConstructor
public class ModeratorController {

    private final ModerationTaskRepository taskRepository;
    private final CourseService courseService;
    private final AuthService authService;

    @GetMapping("/queue")
    @PreAuthorize("@rbac.hasRole('MODERATOR') or @rbac.hasRole('ADMIN')")
    public String viewQueue(Model model) {
        // Find all pending tasks
        List<ModerationTask> tasks = taskRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        model.addAttribute("tasks", tasks);
        return "moderator/queue";
    }

    @GetMapping("/review/{taskId}")
    @PreAuthorize("@rbac.hasRole('MODERATOR') or @rbac.hasRole('ADMIN')")
    public String reviewTask(@PathVariable Long taskId, Model model) {
        ModerationTask task = taskRepository.findById(taskId).orElseThrow();
        model.addAttribute("task", task);
        
        if ("COURSE".equals(task.getItemType())) {
            model.addAttribute("course", courseService.getCourseById(task.getItemId()));
            return "moderator/review_course";
        }
        
        return "redirect:/moderator/queue";
    }
}
