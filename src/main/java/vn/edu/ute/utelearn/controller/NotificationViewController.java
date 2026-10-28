package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.ute.utelearn.dto.NotificationDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.NotificationService;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationViewController {

    private final NotificationService notificationService;
    private final AuthService authService;

    @GetMapping
    public String showNotificationCenter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Model model
    ) {
        log.info("[NotificationViewController] Mở trang trung tâm thông báo, page: {}, category: {}, unreadOnly: {}", page, category, unreadOnly);
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        User currentUser = currentUserOpt.get();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<NotificationDTO> notificationPage = notificationService.getUserNotifications(
                currentUser.getId(), category, unreadOnly, pageable);
        long unreadCount = notificationService.getUnreadCount(currentUser.getId());

        model.addAttribute("notifications", notificationPage.getContent());
        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("currentPage", notificationPage.getNumber());
        model.addAttribute("totalPages", notificationPage.getTotalPages());
        model.addAttribute("totalElements", notificationPage.getTotalElements());
        model.addAttribute("selectedCategory", category != null ? category.toUpperCase() : "ALL");
        model.addAttribute("unreadOnly", unreadOnly);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("currentUser", currentUser);

        return "notifications/index";
    }
}
