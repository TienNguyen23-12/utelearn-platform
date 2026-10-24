package vn.edu.ute.utelearn.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dto.AssignmentDTO;
import vn.edu.ute.utelearn.dto.AssignmentGradeDTO;
import vn.edu.ute.utelearn.dto.AssignmentRequestDTO;
import vn.edu.ute.utelearn.dto.AssignmentSubmissionDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AssignmentService;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/instructor/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN', 'MODERATOR', 'ROLE_MODERATOR')")
public class InstructorAssignmentApiController {

    private final AssignmentService assignmentService;
    private final AuthService authService;

    @GetMapping("/by-lesson/{lessonId}")
    public ResponseEntity<?> getAssignmentByLesson(@PathVariable Long lessonId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            AssignmentDTO dto = assignmentService.getOrCreateAssignmentForLesson(lessonId, currentUser.getId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("[InstructorAssignmentApiController] Lỗi lấy Assignment cho bài học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{assignmentId}")
    public ResponseEntity<?> updateAssignment(@PathVariable Long assignmentId, @Valid @RequestBody AssignmentRequestDTO request) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            AssignmentDTO updated = assignmentService.updateAssignment(assignmentId, request, currentUser.getId());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("[InstructorAssignmentApiController] Lỗi cập nhật Assignment {}: {}", assignmentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<?> getSubmissions(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) Long cohortId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            List<AssignmentSubmissionDTO> list = assignmentService.getSubmissionsForInstructor(assignmentId, cohortId, currentUser.getId());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("[InstructorAssignmentApiController] Lỗi lấy danh sách bài nộp của Assignment {}: {}", assignmentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<?> gradeSubmission(@PathVariable Long submissionId, @Valid @RequestBody AssignmentGradeDTO request) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            AssignmentSubmissionDTO graded = assignmentService.gradeSubmission(submissionId, request, currentUser.getId());
            return ResponseEntity.ok(graded);
        } catch (Exception e) {
            log.error("[InstructorAssignmentApiController] Lỗi chấm bài nộp {}: {}", submissionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping(value = "/upload-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAttachment(@RequestParam("file") MultipartFile file) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            String url = assignmentService.uploadAssignmentAttachment(file, currentUser.getId());
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("[InstructorAssignmentApiController] Lỗi tải tài liệu đề bài: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
