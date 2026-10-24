package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dto.AssignmentDTO;
import vn.edu.ute.utelearn.dto.AssignmentSubmissionDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AssignmentService;
import vn.edu.ute.utelearn.service.AuthService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentApiController {

    private final AssignmentService assignmentService;
    private final AuthService authService;

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<?> getAssignmentForLesson(
            @PathVariable Long lessonId,
            @RequestParam(required = false) Long cohortId) {
        try {
            Long userId = authService.getCurrentAuthenticatedUser().map(User::getId).orElse(null);
            AssignmentDTO dto = assignmentService.getAssignmentByLessonId(lessonId, cohortId, userId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("[AssignmentApiController] Lỗi lấy thông tin Assignment bài học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping(value = "/{assignmentId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitAssignment(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) String submissionText,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            User user = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để nộp bài tập"));
            AssignmentSubmissionDTO result = assignmentService.submitAssignment(assignmentId, cohortId, submissionText, file, user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[AssignmentApiController] Lỗi nộp bài tập Assignment {}: {}", assignmentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{assignmentId}/submissions/my")
    public ResponseEntity<?> deleteMySubmission(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) Long cohortId) {
        try {
            User user = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để thực hiện"));
            assignmentService.deleteStudentSubmission(assignmentId, cohortId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Đã xóa bài nộp thành công"));
        } catch (Exception e) {
            log.error("[AssignmentApiController] Lỗi xóa bài nộp Assignment {}: {}", assignmentId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
