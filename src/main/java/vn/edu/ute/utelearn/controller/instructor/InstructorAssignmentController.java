package vn.edu.ute.utelearn.controller.instructor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.dao.AssignmentRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dto.AssignmentDTO;
import vn.edu.ute.utelearn.dto.AssignmentSubmissionDTO;
import vn.edu.ute.utelearn.entity.Assignment;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AssignmentService;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/instructor/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN', 'MODERATOR', 'ROLE_MODERATOR')")
public class InstructorAssignmentController {

    private final AssignmentService assignmentService;
    private final AssignmentRepository assignmentRepository;
    private final CohortRepository cohortRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final AuthService authService;

    @GetMapping("/{assignmentId}")
    public String showGradingPage(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) Long submissionId,
            Model model,
            RedirectAttributes redirectAttributes) {
        log.info("[InstructorAssignmentController] Truy cập trang chấm bài tập ID: {}, cohortId: {}, submissionId: {}", assignmentId, cohortId, submissionId);

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();

        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài tập với ID: " + assignmentId);
            return "redirect:/instructor/cohorts";
        }

        Lesson lesson = assignment.getLesson();
        Course course = null;
        Cohort cohort = null;
        List<Cohort> availableCohorts = Collections.emptyList();

        if (cohortId != null) {
            cohort = cohortRepository.findById(cohortId).orElse(null);
            if (cohort != null && cohort.getCourse() != null) {
                course = cohort.getCourse();
            }
        }

        if (cohort == null && lesson != null && lesson.getSection() != null) {
            Section section = lesson.getSection();
            if (section.getCohort() != null) {
                cohort = section.getCohort();
                if (cohort.getCourse() != null) {
                    course = cohort.getCourse();
                }
            } else if (section.getCourse() != null) {
                course = section.getCourse();
                availableCohorts = cohortRepository.findByCourseIdOrdered(course.getId());
                if (!availableCohorts.isEmpty()) {
                    cohort = availableCohorts.get(0);
                }
            }
        }

        if (course != null && availableCohorts.isEmpty()) {
            availableCohorts = cohortRepository.findByCourseIdOrdered(course.getId());
        }

        Long filterCohortId = (cohort != null) ? cohort.getId() : null;
        AssignmentDTO assignmentDto = assignmentService.getAssignmentById(assignmentId, filterCohortId, currentUser.getId());
        List<AssignmentSubmissionDTO> submissions = assignmentService.getSubmissionsForInstructor(assignmentId, filterCohortId, currentUser.getId());

        long totalSubmissions = submissions.size();
        long gradedCount = submissions.stream()
                .filter(s -> "GRADED".equalsIgnoreCase(s.getStatus()) || "COMPLETED".equalsIgnoreCase(s.getStatus()))
                .count();
        long pendingCount = totalSubmissions - gradedCount;
        long passedCount = submissions.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsPassed()))
                .count();
        long lateCount = submissions.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsLate()))
                .count();

        Long targetSubmissionId = submissionId;
        if (targetSubmissionId == null && !submissions.isEmpty()) {
            Optional<AssignmentSubmissionDTO> firstPending = submissions.stream()
                    .filter(s -> !"GRADED".equalsIgnoreCase(s.getStatus()) && !"COMPLETED".equalsIgnoreCase(s.getStatus()))
                    .findFirst();
            targetSubmissionId = firstPending.isPresent() ? firstPending.get().getId() : submissions.get(0).getId();
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("assignment", assignment);
        model.addAttribute("assignmentDto", assignmentDto);
        model.addAttribute("lesson", lesson);
        model.addAttribute("course", course);
        model.addAttribute("cohort", cohort);
        model.addAttribute("availableCohorts", availableCohorts);
        model.addAttribute("submissions", submissions);
        model.addAttribute("selectedSubmissionId", targetSubmissionId);
        model.addAttribute("totalSubmissions", totalSubmissions);
        model.addAttribute("gradedCount", gradedCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("passedCount", passedCount);
        model.addAttribute("lateCount", lateCount);

        return "instructor/assignments/grading";
    }

    @GetMapping("/by-lesson/{lessonId}")
    public String showGradingByLesson(
            @PathVariable Long lessonId,
            @RequestParam(required = false) Long cohortId,
            RedirectAttributes redirectAttributes) {
        log.info("[InstructorAssignmentController] Truy cập chấm bài theo lessonId: {}, cohortId: {}", lessonId, cohortId);
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();

        try {
            AssignmentDTO assignment = assignmentService.getOrCreateAssignmentForLesson(lessonId, currentUser.getId());
            String redirectUrl = "/instructor/assignments/" + assignment.getId() + (cohortId != null ? "?cohortId=" + cohortId : "");
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            log.error("[InstructorAssignmentController] Lỗi tìm hoặc khởi tạo assignment cho bài học {}: {}", lessonId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/instructor/cohorts";
        }
    }

    @GetMapping("/cohorts/{cohortId}/assignments/{assignmentId}")
    public String showCohortAssignment(
            @PathVariable Long cohortId,
            @PathVariable Long assignmentId) {
        return "redirect:/instructor/assignments/" + assignmentId + "?cohortId=" + cohortId;
    }
}

