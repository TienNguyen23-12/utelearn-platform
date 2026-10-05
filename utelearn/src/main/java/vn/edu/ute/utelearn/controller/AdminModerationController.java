package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.entity.ModerationTask;

import java.util.List;

@Controller
@RequestMapping("/admin/moderation")
@RequiredArgsConstructor
public class AdminModerationController {

    private final ModerationTaskRepository taskRepository;

    @GetMapping
    @PreAuthorize("@rbac.hasRole('ADMIN')")
    public String viewEscalatedQueue(Model model) {
        // Admin views ESCALATED tasks
        List<ModerationTask> tasks = taskRepository.findByStatusAndEscalationLevelGreaterThanOrderByCreatedAtAsc("ESCALATED", 1);
        model.addAttribute("tasks", tasks);
        return "admin/moderation_queue";
    }
}
