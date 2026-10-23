package vn.edu.ute.utelearn.controller.instructor;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dto.CohortRequestDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.service.CourseService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Controller
@RequestMapping("/instructor/cohorts")
@RequiredArgsConstructor
public class InstructorCohortController {

    private final CohortService cohortService;
    private final CourseService courseService;
    private final AuthService authService;
    private final vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;
    private final vn.edu.ute.utelearn.dao.CohortRepository cohortRepository;
    private final vn.edu.ute.utelearn.service.SectionService sectionService;
    private final vn.edu.ute.utelearn.service.ModerationService moderationService;

    @GetMapping
    public String listCohorts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        String safeKeyword = vn.edu.ute.utelearn.util.ValidationUtils.stripHtmlTags(keyword);
        String safeStatus = vn.edu.ute.utelearn.util.ValidationUtils.stripHtmlTags(status);

        Page<Cohort> cohorts;
        if ((safeKeyword != null && !safeKeyword.isBlank()) || (safeStatus != null && !safeStatus.isBlank())) {
            cohorts = cohortService.searchCohortsByInstructor(currentUser.getId(), safeKeyword, safeStatus,
                    PageRequest.of(page, size));
        } else {
            cohorts = cohortService.getCohortsByInstructor(currentUser.getId(), PageRequest.of(page, size));
        }

        model.addAttribute("cohorts", cohorts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", cohorts.getTotalPages());
        model.addAttribute("keyword", safeKeyword);
        model.addAttribute("status", safeStatus);

        return "instructor/cohorts/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        model.addAttribute("cohortDto", new CohortRequestDTO());
        // Load courses for instructor to select
        Page<Course> courses = courseService.getCoursesByInstructor(currentUser.getId(), PageRequest.of(0, 100));
        model.addAttribute("courses", courses.getContent());
        return "instructor/cohorts/form";
    }

    @PostMapping("/create")
    public String createCohort(
            @Valid @ModelAttribute("cohortDto") CohortRequestDTO cohortDto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        if (result.hasErrors()) {
            Page<Course> courses = courseService.getCoursesByInstructor(currentUser.getId(), PageRequest.of(0, 100));
            model.addAttribute("courses", courses.getContent());
            return "instructor/cohorts/form";
        }

        try {
            cohortService.createCohort(cohortDto, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Lớp học đã được tạo thành công!");
            return "redirect:/instructor/cohorts";
        } catch (Exception e) {
            result.rejectValue("code", "error.cohort", e.getMessage());
            Page<Course> courses = courseService.getCoursesByInstructor(currentUser.getId(), PageRequest.of(0, 100));
            model.addAttribute("courses", courses.getContent());
            return "instructor/cohorts/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable Long id,
            Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        try {
            Cohort cohort = cohortService.getCohortByIdAndInstructor(id, currentUser.getId());

            CohortRequestDTO dto = new CohortRequestDTO();
            dto.setCode(cohort.getCode());
            dto.setName(cohort.getName());
            dto.setCourseId(cohort.getCourse() != null ? cohort.getCourse().getId() : null);
            dto.setMaxCapacity(cohort.getMaxCapacity());
            dto.setPrice(cohort.getPrice());
            dto.setStatus(cohort.getStatus());
            dto.setVisibility(cohort.getVisibility());

            if (cohort.getEnrollmentStart() != null)
                dto.setEnrollmentStart(LocalDateTime.ofInstant(cohort.getEnrollmentStart(), ZoneId.systemDefault()));
            if (cohort.getEnrollmentEnd() != null)
                dto.setEnrollmentEnd(LocalDateTime.ofInstant(cohort.getEnrollmentEnd(), ZoneId.systemDefault()));
            if (cohort.getStudyStart() != null)
                dto.setStudyStart(LocalDateTime.ofInstant(cohort.getStudyStart(), ZoneId.systemDefault()));
            if (cohort.getStudyEnd() != null)
                dto.setStudyEnd(LocalDateTime.ofInstant(cohort.getStudyEnd(), ZoneId.systemDefault()));
            dto.setDescription(cohort.getDescription());
            if (cohort.getObjectives() != null && !cohort.getObjectives().isBlank()) {
                dto.setObjectives(String.join("\n", cohort.getParsedObjectives()));
            } else {
                dto.setObjectives("");
            }
            dto.setAnnouncementTitle(cohort.getAnnouncementTitle());
            dto.setAnnouncementContent(cohort.getAnnouncementContent());
            dto.setThumbnailUrl(cohort.getThumbnailUrl());

            model.addAttribute("cohortDto", dto);
            model.addAttribute("cohortId", id);
            model.addAttribute("cohort", cohort);

            Page<Course> courses = courseService.getCoursesByInstructor(currentUser.getId(), PageRequest.of(0, 100));
            model.addAttribute("courses", courses.getContent());

            return "instructor/cohorts/form";
        } catch (Exception e) {
            return "redirect:/instructor/cohorts";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateCohort(
            @PathVariable Long id,
            @Valid @ModelAttribute("cohortDto") CohortRequestDTO cohortDto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        if (result.hasErrors()) {
            model.addAttribute("cohortId", id);
            Page<Course> courses = courseService.getCoursesByInstructor(currentUser.getId(), PageRequest.of(0, 100));
            model.addAttribute("courses", courses.getContent());
            return "instructor/cohorts/form";
        }

        try {
            cohortService.updateCohort(id, cohortDto, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lớp học thành công!");
            return "redirect:/instructor/cohorts";
        } catch (Exception e) {
            result.rejectValue("code", "error.cohort", e.getMessage());
            model.addAttribute("cohortId", id);
            Page<Course> courses = courseService.getCoursesByInstructor(currentUser.getId(), PageRequest.of(0, 100));
            model.addAttribute("courses", courses.getContent());
            return "instructor/cohorts/form";
        }
    }

    @GetMapping("/{id}/schedule")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String manageSchedule(
            @PathVariable Long id,
            Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        Cohort cohort = cohortService.getCohortByIdAndInstructor(id, currentUser.getId());
        List<vn.edu.ute.utelearn.entity.CohortSchedule> schedules = cohortScheduleRepository.findByCohortIdWithDetails(id);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("cohort", cohort);
        model.addAttribute("schedules", schedules);
        return "instructor/cohorts/schedule";
    }

    @PostMapping("/{id}/schedule")
    public String updateSchedule(
            @PathVariable Long id,
            @RequestParam(required = false) java.util.List<Long> scheduleIds,
            @RequestParam(required = false) java.util.List<String> unlockAts,
            @RequestParam(required = false) java.util.List<String> deadlineAts,
            @RequestParam(required = false) java.util.List<String> showScoreTypes,
            RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        Cohort cohort = cohortService.getCohortByIdAndInstructor(id, currentUser.getId());

        if (scheduleIds != null && !scheduleIds.isEmpty()) {
            for (int i = 0; i < scheduleIds.size(); i++) {
                Long sId = scheduleIds.get(i);
                final int idx = i;
                cohortScheduleRepository.findById(sId).ifPresent(cs -> {
                    if (cs.getCohort().getId().equals(cohort.getId())) {
                        if (unlockAts != null && idx < unlockAts.size() && unlockAts.get(idx) != null && !unlockAts.get(idx).isBlank()) {
                            try {
                                cs.setUnlockAt(java.time.LocalDateTime.parse(unlockAts.get(idx)).atZone(ZoneId.systemDefault()).toInstant());
                            } catch (Exception ignored) {}
                        }
                        if (deadlineAts != null && idx < deadlineAts.size() && deadlineAts.get(idx) != null && !deadlineAts.get(idx).isBlank()) {
                            try {
                                cs.setDeadlineAt(java.time.LocalDateTime.parse(deadlineAts.get(idx)).atZone(ZoneId.systemDefault()).toInstant());
                            } catch (Exception ignored) {}
                        } else if (deadlineAts != null && idx < deadlineAts.size()) {
                            cs.setDeadlineAt(null);
                        }
                        if (showScoreTypes != null && idx < showScoreTypes.size() && showScoreTypes.get(idx) != null) {
                            cs.setShowScoreType(showScoreTypes.get(idx));
                        }
                        cohortScheduleRepository.save(cs);
                    }
                });
            }
        }

        cohortService.revertToDraftIfApproved(cohort);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lộ trình module thành công!");
        return "redirect:/instructor/cohorts/" + id + "/schedule";
    }

    @PostMapping("/{id}/schedule/{scheduleId}/delete")
    public String deleteScheduleItem(
            @PathVariable Long id,
            @PathVariable Long scheduleId,
            RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        Cohort cohort = cohortService.getCohortByIdAndInstructor(id, currentUser.getId());
        cohortScheduleRepository.findById(scheduleId).ifPresent(cs -> {
            if (cs.getCohort().getId().equals(cohort.getId())) {
                cohortScheduleRepository.delete(cs);
                cohortService.revertToDraftIfApproved(cohort);
            }
        });
        redirectAttributes.addFlashAttribute("successMessage", "Đã gỡ bài học khỏi module!");
        return "redirect:/instructor/cohorts/" + id + "/schedule";
    }

    @GetMapping("/{id}/curriculum")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String showCohortCurriculum(@PathVariable Long id, Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        Cohort cohort = cohortRepository.findById(id).orElseThrow(() -> new RuntimeException("Module not found"));
        boolean isOwner = (cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(currentUser.getId()))
                || currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));
        if (!isOwner) {
            return "redirect:/instructor/cohorts";
        }
        List<vn.edu.ute.utelearn.entity.Section> sections = sectionService.getSectionsByCohortId(id);
        model.addAttribute("course", cohort.getCourse());
        model.addAttribute("module", cohort);
        model.addAttribute("sections", sections);
        return "instructor/courses/module-curriculum";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    @Transactional
    public String deleteCohort(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        try {
            cohortService.deleteCohort(id, currentUser);
            log.info("[InstructorCohortController] Xóa module id: {} thành công bởi user: {}", id, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Xóa Module thành công!");
        } catch (Exception e) {
            log.error("[InstructorCohortController] Lỗi khi xóa module id {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa Module: " + e.getMessage());
        }
        return "redirect:/instructor/cohorts";
    }

    @GetMapping("/{id}/delete")
    public String deleteCohortGetRedirect() {
        return "redirect:/instructor/cohorts";
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String submitCohortForReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        Cohort cohort = cohortService.getCohortByIdAndInstructor(id, currentUser.getId());
        if (!cohort.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/cohorts";
        }

        moderationService.createCohortModerationTask(id, currentUser.getId());
        log.info("[InstructorCohortController] Gửi duyệt module id: {}, user: {}", id, currentUser.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Module đã được gửi duyệt thành công. Vui lòng chờ Moderator kiểm duyệt.");
        return "redirect:/instructor/cohorts";
    }
}
