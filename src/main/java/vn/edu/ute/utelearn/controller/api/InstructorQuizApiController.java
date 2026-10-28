package vn.edu.ute.utelearn.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dto.*;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.QuizService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/instructor/quizzes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN', 'MODERATOR', 'ROLE_MODERATOR')")
public class InstructorQuizApiController {

    private final QuizService quizService;
    private final AuthService authService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] excelBytes = quizService.exportQuestionTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment().filename("Mau_Cau_Hoi_Quiz_UTELearn.xlsx", StandardCharsets.UTF_8).build());
            return ResponseEntity.ok().headers(headers).body(excelBytes);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi khi tạo file mẫu Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/{quizId}/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importQuestionsFromExcel(@PathVariable Long quizId, @RequestParam("file") MultipartFile file) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            QuestionImportResultDTO result = quizService.importQuestionsFromExcel(quizId, file, currentUser.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi khi nhập câu hỏi từ Excel cho Quiz {}: {}", quizId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{quizId}/export")
    public ResponseEntity<byte[]> exportQuestions(@PathVariable Long quizId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            byte[] excelBytes = quizService.exportQuizQuestions(quizId, currentUser.getId());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment().filename("Bo_Cau_Hoi_Quiz_" + quizId + ".xlsx", StandardCharsets.UTF_8).build());
            return ResponseEntity.ok().headers(headers).body(excelBytes);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi khi xuất bộ câu hỏi Quiz {}: {}", quizId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{quizId}/questions/reorder")
    public ResponseEntity<?> reorderQuestions(@PathVariable Long quizId, @RequestBody List<Long> questionIds) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            quizService.reorderQuestions(quizId, questionIds, currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "Cập nhật thứ tự câu hỏi thành công"));
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi khi đổi thứ tự câu hỏi Quiz {}: {}", quizId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/by-lesson/{lessonId}")
    public ResponseEntity<?> getQuizByLesson(@PathVariable Long lessonId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            QuizDTO quiz = quizService.getOrCreateQuizForLesson(lessonId, currentUser.getId());
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi lấy Quiz cho bài học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{quizId}")
    public ResponseEntity<?> updateQuiz(@PathVariable Long quizId, @Valid @RequestBody QuizRequestDTO request) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            QuizDTO updated = quizService.updateQuiz(quizId, request, currentUser.getId());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi cập nhật Quiz {}: {}", quizId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{quizId}/questions")
    public ResponseEntity<?> getQuestions(@PathVariable Long quizId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            List<QuestionDTO> questions = quizService.getQuestionsByQuizId(quizId, true, currentUser.getId());
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi lấy danh sách câu hỏi Quiz {}: {}", quizId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{quizId}/questions")
    public ResponseEntity<?> createQuestion(@PathVariable Long quizId, @Valid @RequestBody QuestionRequestDTO request) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            QuestionDTO created = quizService.createQuestion(quizId, request, currentUser.getId());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi tạo câu hỏi cho Quiz {}: {}", quizId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/questions/{questionId}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long questionId, @Valid @RequestBody QuestionRequestDTO request) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            QuestionDTO updated = quizService.updateQuestion(questionId, request, currentUser.getId());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi cập nhật câu hỏi {}: {}", questionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long questionId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            quizService.deleteQuestion(questionId, currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "Đã xóa câu hỏi thành công"));
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi xóa câu hỏi {}: {}", questionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{quizId}/submissions")
    public ResponseEntity<?> getSubmissions(
            @PathVariable Long quizId,
            @RequestParam(required = false) Long cohortId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            List<QuizResultDTO> list = quizService.getSubmissionsForInstructor(quizId, cohortId, currentUser.getId());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi lấy danh sách bài làm Quiz {}: {}", quizId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<?> getSubmissionDetail(@PathVariable Long submissionId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            QuizResultDTO detail = quizService.getSubmissionDetail(submissionId, currentUser.getId());
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("[InstructorQuizApiController] Lỗi lấy chi tiết bài làm Quiz {}: {}", submissionId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
