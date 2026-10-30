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
import vn.edu.ute.utelearn.service.CohortChatService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import vn.edu.ute.utelearn.dao.CohortMessageRepository;
import java.time.ZoneId;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.service.CloudinaryService;

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
    private final CohortChatService cohortChatService;
    private final CohortMessageRepository cohortMessageRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CloudinaryService cloudinaryService;

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

        boolean isAdmin = vn.edu.ute.utelearn.util.SecurityUtils.isAdmin(currentUser);
        Page<Cohort> cohorts;
        if (isAdmin) {
            cohorts = cohortService.searchAllCohorts(safeKeyword, safeStatus, PageRequest.of(page, size));
        } else if ((safeKeyword != null && !safeKeyword.isBlank()) || (safeStatus != null && !safeStatus.isBlank())) {
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
        Page<Course> courses = courseService.getCoursesByInstructor(currentUser.getId(), PageRequest.of(0, 100));
        model.addAttribute("courses", courses.getContent());
        return "instructor/cohorts/form";
    }

    @PostMapping("/create")
    public String createCohort(
            @Valid @ModelAttribute("cohortDto") CohortRequestDTO cohortDto,
            BindingResult result,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
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
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                try {
                    if (cloudinaryService != null) {
                        Map<String, Object> uploadResult = cloudinaryService.uploadFile(thumbnailFile, "cohorts", "image");
                        String uploadedUrl = (String) uploadResult.get("secure_url");
                        if (uploadedUrl == null || uploadedUrl.isBlank()) {
                            uploadedUrl = (String) uploadResult.get("url");
                        }
                        if (uploadedUrl != null && !uploadedUrl.isBlank()) {
                            cohortDto.setThumbnailUrl(uploadedUrl);
                        }
                    }
                } catch (Exception e) {
                    log.error("[InstructorCohortController] Lỗi khi upload ảnh bìa module: {}", e.getMessage());
                }
            }
            cohortService.createCohort(cohortDto, currentUser);
            log.info("[InstructorCohortController] Tạo lớp học mới: {}, user: {}", cohortDto.getName(), currentUser.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Lớp học đã được tạo thành công!");
            return "redirect:/instructor/cohorts";
        } catch (Exception e) {
            log.warn("[InstructorCohortController] Lỗi khi tạo lớp học: {}", e.getMessage());
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

            dto.setGradingPolicy(cohort.getGradingPolicy() != null ? cohort.getGradingPolicy() : "PROGRESS_BASED");
            dto.setProcessWeight(cohort.getProcessWeight() != null ? cohort.getProcessWeight() : 50);
            dto.setFinalWeight(cohort.getFinalWeight() != null ? cohort.getFinalWeight() : 50);
            dto.setFinalLessonId(cohort.getFinalLesson() != null ? cohort.getFinalLesson().getId() : null);
            dto.setMinPassingGrade(cohort.getMinPassingGrade() != null ? cohort.getMinPassingGrade() : new BigDecimal("5.00"));
            dto.setMinFinalExamScore(cohort.getMinFinalExamScore() != null ? cohort.getMinFinalExamScore() : new BigDecimal("3.00"));

            model.addAttribute("cohortDto", dto);
            model.addAttribute("cohortId", id);
            model.addAttribute("cohort", cohort);

            Page<Course> courses = courseService.getCoursesByInstructor(currentUser.getId(), PageRequest.of(0, 100));
            model.addAttribute("courses", courses.getContent());
            model.addAttribute("cohortSchedules", cohortScheduleRepository.findByCohortIdWithDetails(id));

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
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
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
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                try {
                    if (cloudinaryService != null) {
                        Map<String, Object> uploadResult = cloudinaryService.uploadFile(thumbnailFile, "cohorts", "image");
                        String uploadedUrl = (String) uploadResult.get("secure_url");
                        if (uploadedUrl == null || uploadedUrl.isBlank()) {
                            uploadedUrl = (String) uploadResult.get("url");
                        }
                        if (uploadedUrl != null && !uploadedUrl.isBlank()) {
                            cohortDto.setThumbnailUrl(uploadedUrl);
                        }
                    }
                } catch (Exception e) {
                    log.error("[InstructorCohortController] Lỗi khi upload ảnh bìa module: {}", e.getMessage());
                }
            }
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
    public String manageSchedule(@PathVariable Long id) {
        return "redirect:/instructor/cohorts/" + id + "/curriculum";
    }

    @PostMapping("/{id}/schedule")
    public String updateSchedule(
            @PathVariable Long id,
            @RequestParam(required = false) java.util.List<Long> scheduleIds,
            @RequestParam(required = false) java.util.List<String> unlockAts,
            @RequestParam(required = false) java.util.List<String> deadlineAts,
            @RequestParam(required = false) java.util.List<String> showScoreTypes,
            @RequestParam(required = false) java.util.List<Boolean> allowReviewAnswers,
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
                        if (allowReviewAnswers != null && idx < allowReviewAnswers.size() && allowReviewAnswers.get(idx) != null) {
                            cs.setAllowReviewAnswers(allowReviewAnswers.get(idx));
                        }
                        cohortScheduleRepository.save(cs);
                    }
                });
            }
        }

        cohortService.revertToDraftIfApproved(cohort);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lộ trình module thành công!");
        return "redirect:/instructor/cohorts/" + id + "/curriculum";
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
        return "redirect:/instructor/cohorts/" + id + "/curriculum";
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

    private List<Cohort> getSortedInstructorCohorts(Long instructorId) {
        List<Cohort> cohorts = cohortRepository.findByInstructorId(instructorId, PageRequest.of(0, 50)).getContent();
        if (cohorts == null || cohorts.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> cohortIds = cohorts.stream().map(Cohort::getId).toList();
        Map<Long, Instant> lastActivityMap = new HashMap<>();
        List<Object[]> results = cohortMessageRepository.findLatestMessageTimesByCohortIds(cohortIds);
        for (Object[] row : results) {
            lastActivityMap.put((Long) row[0], (Instant) row[1]);
        }
        List<Cohort> sorted = new ArrayList<>(cohorts);
        sorted.sort((a, b) -> {
            Instant tA = lastActivityMap.getOrDefault(a.getId(), a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH);
            Instant tB = lastActivityMap.getOrDefault(b.getId(), b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH);
            return tB.compareTo(tA);
        });
        return sorted;
    }

    @GetMapping("/chat")
    public String showInstructorCohortsChat() {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        List<Cohort> cohorts = getSortedInstructorCohorts(currentUser.getId());
        if (cohorts != null && !cohorts.isEmpty()) {
            return "redirect:/instructor/cohorts/" + cohorts.get(0).getId() + "/chat";
        }
        return "redirect:/instructor/cohorts";
    }

    @GetMapping("/{id}/chat")
    public String showCohortChat(
            @PathVariable Long id,
            Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        if (!cohortChatService.canUserAccessCohort(currentUser, id)) {
            return "redirect:/instructor/cohorts";
        }

        Cohort currentCohort = cohortRepository.findById(id).orElse(null);
        if (currentCohort == null) {
            return "redirect:/instructor/cohorts";
        }

        List<Cohort> instructorCohorts = getSortedInstructorCohorts(currentUser.getId());

        model.addAttribute("currentCohort", currentCohort);
        model.addAttribute("cohorts", instructorCohorts);
        model.addAttribute("currentUser", currentUser);
        return "instructor/cohorts/chat";
    }

    @GetMapping("/{id}/delete")
    public String deleteCohortGetRedirect() {
        return "redirect:/instructor/cohorts";
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String submitCohortForReview(@PathVariable Long id,
                                        @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                        RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        Cohort cohort = cohortService.getCohortByIdAndInstructor(id, currentUser.getId());
        if (!cohort.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/cohorts";
        }

        moderationService.createCohortModerationTask(id, currentUser.getId());
        log.info("[InstructorCohortController] Gửi duyệt module id: {}, user: {}", id, currentUser.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Module đã được gửi duyệt thành công. Vui lòng chờ Moderator kiểm duyệt.");
        if (redirectUrl != null && (redirectUrl.startsWith("/instructor/cohorts/") || redirectUrl.startsWith("/instructor/courses/"))) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/instructor/cohorts";
    }
}
