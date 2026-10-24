package vn.edu.ute.utelearn.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.dto.QuizDTO;
import vn.edu.ute.utelearn.dto.QuizResultDTO;
import vn.edu.ute.utelearn.dto.QuizSubmissionRequestDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.QuizService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizApiController {

    private final QuizService quizService;
    private final AuthService authService;

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<?> getQuizForLesson(
            @PathVariable Long lessonId,
            @RequestParam(required = false) Long cohortId) {
        try {
            Long userId = authService.getCurrentAuthenticatedUser().map(User::getId).orElse(null);
            QuizDTO quiz = quizService.getQuizByLessonId(lessonId, cohortId, userId);
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            log.error("[QuizApiController] Lỗi lấy thông tin Quiz bài học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{quizId}/start")
    public ResponseEntity<?> startQuiz(
            @PathVariable Long quizId,
            @RequestParam Long cohortId) {
        try {
            User user = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để làm bài kiểm tra"));
            QuizDTO quiz = quizService.startQuiz(quizId, cohortId, user.getId());
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            log.error("[QuizApiController] Lỗi bắt đầu Quiz {}: {}", quizId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<?> submitQuiz(
            @PathVariable Long quizId,
            @Valid @RequestBody QuizSubmissionRequestDTO request) {
        try {
            User user = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để nộp bài kiểm tra"));
            QuizResultDTO result = quizService.submitQuiz(quizId, request, user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[QuizApiController] Lỗi nộp bài Quiz {}: {}", quizId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<?> getSubmissionDetail(@PathVariable Long submissionId) {
        try {
            User user = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập"));
            QuizResultDTO detail = quizService.getSubmissionDetail(submissionId, user.getId());
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("[QuizApiController] Lỗi xem chi tiết bài làm {}: {}", submissionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{quizId}/submissions")
    public ResponseEntity<?> getSubmissions(
            @PathVariable Long quizId,
            @RequestParam Long cohortId) {
        try {
            User user = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập"));
            List<QuizResultDTO> list = quizService.getSubmissionsForQuiz(quizId, cohortId, user.getId());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("[QuizApiController] Lỗi lấy danh sách bài làm Quiz {}: {}", quizId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
