package vn.edu.ute.utelearn.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.dto.LessonNoteRequestDTO;
import vn.edu.ute.utelearn.dto.LessonNoteResponseDTO;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.LessonNoteService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class LessonNoteApiController {

    private final LessonNoteService lessonNoteService;
    private final AuthService authService;

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<?> getNotesForLesson(@PathVariable Long lessonId) {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để xem ghi chú cá nhân"));
        }
        try {
            List<LessonNoteResponseDTO> notes = lessonNoteService.getNotesForLesson(lessonId, currentUser.get().getId());
            return ResponseEntity.ok(notes);
        } catch (Exception e) {
            log.error("[LessonNoteApiController] Lỗi lấy danh sách ghi chú bài học {}: {}", lessonId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createNote(@Valid @RequestBody LessonNoteRequestDTO request) {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để lưu ghi chú cá nhân"));
        }
        try {
            LessonNoteResponseDTO created = lessonNoteService.createNote(request, currentUser.get().getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("[LessonNoteApiController] Lỗi tạo ghi chú: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Đã xảy ra lỗi khi tạo ghi chú"));
        }
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<?> updateNote(
            @PathVariable Long noteId,
            @Valid @RequestBody LessonNoteRequestDTO request) {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để cập nhật ghi chú"));
        }
        try {
            LessonNoteResponseDTO updated = lessonNoteService.updateNote(noteId, request, currentUser.get().getId());
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("[LessonNoteApiController] Lỗi cập nhật ghi chú {}: {}", noteId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Đã xảy ra lỗi khi cập nhật ghi chú"));
        }
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<?> deleteNote(@PathVariable Long noteId) {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để xóa ghi chú"));
        }
        try {
            lessonNoteService.deleteNote(noteId, currentUser.get().getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã xóa ghi chú thành công"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("[LessonNoteApiController] Lỗi xóa ghi chú {}: {}", noteId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Đã xảy ra lỗi khi xóa ghi chú"));
        }
    }
}
