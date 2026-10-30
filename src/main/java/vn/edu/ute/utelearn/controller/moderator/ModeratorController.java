package vn.edu.ute.utelearn.controller.moderator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.InstructorAssetRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.ModerationTask;
import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.security.RbacHelper;
import vn.edu.ute.utelearn.service.AttendanceService;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.LessonService;
import vn.edu.ute.utelearn.service.ModerationService;
import vn.edu.ute.utelearn.service.SectionService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Slf4j
@Controller
@RequestMapping("/moderator")
@RequiredArgsConstructor
public class ModeratorController {

    private final ModerationTaskRepository taskRepository;
    private final CourseService courseService;
    private final AttendanceService attendanceService;
    private final AuthService authService;
    private final CohortRepository cohortRepository;
    private final SectionService sectionService;
    private final LessonService lessonService;
    private final UserRepository userRepository;
    private final RbacHelper rbacHelper;
    private final InstructorAssetRepository assetRepository;
    private final ModerationService moderationService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.dao.LessonRepository lessonRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.dao.AssignmentRepository assignmentRepository;

    @GetMapping("/queue")
    @PreAuthorize("@rbac.hasPermission('CONTENT_APPROVE')")
    public String viewQueue(
            @RequestParam(required = false, defaultValue = "PENDING") String tab,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();
        boolean isAdmin = rbacHelper.hasRole("ADMIN") || (currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode())));

        ModeratorAttendance currentAttendance = attendanceService.getCurrentAttendance(currentUser);
        boolean isCheckedIn = currentAttendance != null && Boolean.TRUE.equals(currentAttendance.getIsActive());

        if (!isAdmin && !isCheckedIn) {
            log.warn("[ModeratorController] Moderator {} chua check-in, chuyen huong toi /moderator/attendance", currentUser.getEmail());
            redirectAttributes.addFlashAttribute("error", "Vui lòng điểm danh (Check-in) ca trực trước khi truy cập hàng đợi kiểm duyệt.");
            return "redirect:/moderator/attendance";
        }

        List<ModerationTask> tasks;
        if ("HISTORY".equalsIgnoreCase(tab)) {
            tasks = taskRepository.findByStatusInOrderByCreatedAtDesc(List.of("APPROVED", "REJECTED", "ESCALATED"));
        } else {
            tasks = taskRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        }

        Map<Long, String> itemTitles = new HashMap<>();
        for (ModerationTask t : tasks) {
            itemTitles.put(t.getId(), moderationService.resolveItemTitle(t.getItemType(), t.getItemId()));
        }

        model.addAttribute("tasks", tasks);
        model.addAttribute("itemTitles", itemTitles);
        model.addAttribute("tab", tab != null ? tab.toUpperCase() : "PENDING");
        model.addAttribute("pendingCount", taskRepository.countByStatus("PENDING"));
        model.addAttribute("historyCount", taskRepository.countByStatusIn(List.of("APPROVED", "REJECTED", "ESCALATED")));
        model.addAttribute("currentAttendance", currentAttendance);
        model.addAttribute("isCheckedIn", isCheckedIn);
        return "moderator/queue";
    }

    @GetMapping({"/review", "/review/"})
    public String reviewIndex() {
        return "redirect:/moderator/queue";
    }

    @GetMapping("/review/{taskId}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @PreAuthorize("@rbac.hasPermission('CONTENT_APPROVE')")
    public String reviewTask(@PathVariable Long taskId, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();
        boolean isAdmin = rbacHelper.hasRole("ADMIN") || (currentUser.getRoles() != null && currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode())));

        ModeratorAttendance currentAttendance = attendanceService.getCurrentAttendance(currentUser);
        boolean isCheckedIn = currentAttendance != null && Boolean.TRUE.equals(currentAttendance.getIsActive());

        if (!isAdmin && !isCheckedIn) {
            log.warn("[ModeratorController] Moderator {} chua check-in khi xem task {}", currentUser.getEmail(), taskId);
            redirectAttributes.addFlashAttribute("error", "Vui lòng điểm danh (Check-in) ca trực trước khi thực hiện kiểm duyệt.");
            return "redirect:/moderator/attendance";
        }

        ModerationTask task = taskRepository.findById(taskId).orElseThrow();
        boolean isSelfAuthored = task.getAuthor() != null && task.getAuthor().getId().equals(currentUser.getId());

        model.addAttribute("task", task);
        model.addAttribute("isSelfAuthored", isSelfAuthored);
        model.addAttribute("currentAttendance", currentAttendance);

        List<User> activeModerators = Collections.emptyList();
        if (isAdmin) {
            activeModerators = userRepository.findActiveModerators().stream()
                    .filter(m -> task.getAuthor() == null || !task.getAuthor().getId().equals(m.getId()))
                    .toList();
        }
        model.addAttribute("moderators", activeModerators);

        if ("COURSE".equals(task.getItemType())) {
            Course course = null;
            try {
                course = courseService.getCourseById(task.getItemId());
            } catch (Exception e) {
                log.warn("[ModeratorController] Không tìm thấy khóa học id {} của task {}", task.getItemId(), taskId);
            }
            if (course == null) {
                redirectAttributes.addFlashAttribute("error", "Khóa học này đã bị xóa hoặc không còn tồn tại trên hệ thống.");
                return "redirect:/moderator/queue";
            }
            model.addAttribute("course", course);
            List<Section> sections = sectionService.getSectionsByCourseId(course.getId());
            model.addAttribute("sections", sections);
            Map<Long, List<Lesson>> lessonsMap = new HashMap<>();
            Map<Long, Long> sectionDraftCountMap = new HashMap<>();
            long totalDraftLessons = 0;
            for (Section s : sections) {
                List<Lesson> secLessons = lessonService.getLessonsBySectionId(s.getId());
                lessonsMap.put(s.getId(), secLessons);
                long draftsInSec = (secLessons != null) ? secLessons.stream().filter(l -> !l.isPublished()).count() : 0;
                sectionDraftCountMap.put(s.getId(), draftsInSec);
                totalDraftLessons += draftsInSec;
            }
            model.addAttribute("lessonsMap", lessonsMap);
            model.addAttribute("sectionDraftCountMap", sectionDraftCountMap);
            model.addAttribute("totalDraftLessons", totalDraftLessons);
            populateAssetStatusMap(model, lessonsMap);
            return "moderator/review_course";
        } else if ("COHORT".equals(task.getItemType())) {
            Cohort cohort = cohortRepository.findById(task.getItemId()).orElse(null);
            if (cohort == null) {
                redirectAttributes.addFlashAttribute("error", "Module này đã bị xóa hoặc không còn tồn tại trên hệ thống.");
                return "redirect:/moderator/queue";
            }
            model.addAttribute("cohort", cohort);
            if (cohort.getCourse() != null) {
                Course course = courseService.getCourseById(cohort.getCourse().getId());
                model.addAttribute("course", course);
            }
            List<Section> sections = sectionService.getSectionsByCohortId(cohort.getId());
            model.addAttribute("sections", sections);
            Map<Long, List<Lesson>> lessonsMap = new HashMap<>();
            Map<Long, Long> sectionDraftCountMap = new HashMap<>();
            long totalDraftLessons = 0;
            for (Section s : sections) {
                List<Lesson> secLessons = lessonService.getLessonsBySectionId(s.getId());
                lessonsMap.put(s.getId(), secLessons);
                long draftsInSec = (secLessons != null) ? secLessons.stream().filter(l -> !l.isPublished()).count() : 0;
                sectionDraftCountMap.put(s.getId(), draftsInSec);
                totalDraftLessons += draftsInSec;
            }
            model.addAttribute("lessonsMap", lessonsMap);
            model.addAttribute("sectionDraftCountMap", sectionDraftCountMap);
            model.addAttribute("totalDraftLessons", totalDraftLessons);
            populateAssetStatusMap(model, lessonsMap);
            return "moderator/review_course";
        }

        if ("ASSET".equals(task.getItemType())) {
            InstructorAsset asset = assetRepository.findById(task.getItemId()).orElseThrow();
            model.addAttribute("asset", asset);
            return "moderator/review_course";
        }

        return "redirect:/moderator/queue";
    }

    @GetMapping("/attendance")
    public String getAttendancePage(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentAttendance", attendanceService.getCurrentAttendance(currentUser));
        model.addAttribute("attendanceHistory", attendanceService.getAttendanceHistory(currentUser));
        return "moderator/attendance";
    }

    @PostMapping("/attendance/check-in")
    public String checkIn(RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        try {
            attendanceService.checkIn(currentUserOpt.get());
            redirectAttributes.addFlashAttribute("success", "Điểm danh (Check-in) thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/attendance";
    }

    @PostMapping("/attendance/check-out")
    public String checkOut(RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        try {
            attendanceService.checkOut(currentUserOpt.get());
            redirectAttributes.addFlashAttribute("success", "Kết thúc ca trực (Check-out) thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/attendance";
    }

    @PostMapping("/attendance/check-in-ot")
    public String checkInOvertime(RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        try {
            attendanceService.checkInOvertime(currentUserOpt.get());
            redirectAttributes.addFlashAttribute("success", "Bắt đầu ca làm việc ngoài giờ (Check-in OT) thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/attendance";
    }

    @PostMapping("/attendance/check-out-ot")
    public String checkOutOvertime(RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        try {
            attendanceService.checkOutOvertime(currentUserOpt.get());
            redirectAttributes.addFlashAttribute("success", "Kết thúc ca làm việc ngoài giờ (Check-out OT) thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/moderator/attendance";
    }

    private void populateAssetStatusMap(Model model, Map<Long, List<Lesson>> lessonsMap) {
        if (assetRepository == null || lessonsMap == null) {
            return;
        }
        Map<String, String> assetStatusMap = new HashMap<>();
        Set<String> urls = new HashSet<>();
        for (List<Lesson> lList : lessonsMap.values()) {
            if (lList != null) {
                for (Lesson l : lList) {
                    if (l.getAssetUrl() != null && !l.getAssetUrl().isBlank()) urls.add(l.getAssetUrl().trim());
                    if (l.getVideoUrl() != null && !l.getVideoUrl().isBlank()) urls.add(l.getVideoUrl().trim());
                }
            }
        }
        if (!urls.isEmpty()) {
            List<InstructorAsset> assets = assetRepository.findByCloudinaryUrlIn(urls);
            for (InstructorAsset a : assets) {
                if (a.getCloudinaryUrl() != null && a.getStatus() != null) {
                    assetStatusMap.put(a.getCloudinaryUrl(), a.getStatus());
                }
            }
        }
        model.addAttribute("assetStatusMap", assetStatusMap);
    }
}