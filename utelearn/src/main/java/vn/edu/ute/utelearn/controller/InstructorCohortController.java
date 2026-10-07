package vn.edu.ute.utelearn.controller;

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

@Controller
@RequestMapping("/instructor/cohorts")
@RequiredArgsConstructor
public class InstructorCohortController {

    private final CohortService cohortService;
    private final CourseService courseService;
    private final AuthService authService;

    @GetMapping
    public String listCohorts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        // Sanitize input
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
            dto.setCourseId(cohort.getCourse().getId());
            dto.setMaxCapacity(cohort.getMaxCapacity());
            dto.setPrice(cohort.getPrice());
            dto.setStatus(cohort.getStatus());

            if (cohort.getEnrollmentStart() != null)
                dto.setEnrollmentStart(LocalDateTime.ofInstant(cohort.getEnrollmentStart(), ZoneId.systemDefault()));
            if (cohort.getEnrollmentEnd() != null)
                dto.setEnrollmentEnd(LocalDateTime.ofInstant(cohort.getEnrollmentEnd(), ZoneId.systemDefault()));
            if (cohort.getStudyStart() != null)
                dto.setStudyStart(LocalDateTime.ofInstant(cohort.getStudyStart(), ZoneId.systemDefault()));
            if (cohort.getStudyEnd() != null)
                dto.setStudyEnd(LocalDateTime.ofInstant(cohort.getStudyEnd(), ZoneId.systemDefault()));

            model.addAttribute("cohortDto", dto);
            model.addAttribute("cohortId", id);

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
    public String manageSchedule(
            @PathVariable Long id,
            Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        Cohort cohort = cohortService.getCohortByIdAndInstructor(id, currentUser.getId());
        model.addAttribute("cohort", cohort);
        return "instructor/cohorts/schedule";
    }
}
