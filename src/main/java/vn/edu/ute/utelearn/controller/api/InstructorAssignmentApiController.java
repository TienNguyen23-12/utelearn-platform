package vn.edu.ute.utelearn.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dao.AssignmentSubmissionRepository;
import vn.edu.ute.utelearn.dto.AssignmentDTO;
import vn.edu.ute.utelearn.dto.AssignmentGradeDTO;
import vn.edu.ute.utelearn.dto.AssignmentRequestDTO;
import vn.edu.ute.utelearn.dto.AssignmentSubmissionDTO;
import vn.edu.ute.utelearn.entity.AssignmentSubmission;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AssignmentService;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

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

    @GetMapping("/submissions/{submissionId}/file")
    public ResponseEntity<?> getSubmissionFile(
            @PathVariable Long submissionId,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Vui lòng đăng nhập để xem bài nộp"));

            AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp với ID: " + submissionId));

            boolean isStudentOwner = submission.getUser() != null && submission.getUser().getId().equals(currentUser.getId());
            boolean isStaff = SecurityUtils.isAdmin(currentUser) || SecurityUtils.isModerator(currentUser) || SecurityUtils.isInstructor(currentUser);

            if (!isStudentOwner && !isStaff) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xem tệp này"));
            }

            String fileUrl = submission.getFileUrl();
            if (fileUrl == null || fileUrl.isBlank()) {
                return ResponseEntity.status(404).body(Map.of("message", "Bài nộp không có tệp đính kèm"));
            }

            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);

            InputStream inputStream = conn.getInputStream();
            InputStreamResource resource = new InputStreamResource(inputStream);

            String filename = submission.getFileName();
            if (filename == null || filename.isBlank()) {
                filename = "submission_" + submissionId;
            }

            String contentType = resolveSubmissionContentType(filename);
            ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .contentType(MediaType.parseMediaType(contentType));

            long contentLength = conn.getContentLengthLong();
            if (contentLength > 0) {
                builder.contentLength(contentLength);
            } else if (submission.getFileSizeBytes() != null && submission.getFileSizeBytes() > 0) {
                builder.contentLength(submission.getFileSizeBytes());
            }

            return builder.body(resource);
        } catch (Exception e) {
            log.error("[InstructorAssignmentApiController] Lỗi khi tải hoặc xem trước tệp bài nộp {}: {}", submissionId, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi tải tệp: " + e.getMessage()));
        }
    }

    private String resolveSubmissionContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".txt") || lower.endsWith(".sql") || lower.endsWith(".java") || lower.endsWith(".py")
                || lower.endsWith(".cpp") || lower.endsWith(".c") || lower.endsWith(".h") || lower.endsWith(".cs")
                || lower.endsWith(".js") || lower.endsWith(".ts") || lower.endsWith(".html") || lower.endsWith(".css")
                || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".md") || lower.endsWith(".sh")) {
            return "text/plain; charset=UTF-8";
        }
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".rar")) return "application/x-rar-compressed";
        return "application/octet-stream";
    }
}
