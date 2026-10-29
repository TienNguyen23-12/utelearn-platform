package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.InstructorAssetService;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class AssetDownloadController {

    private final InstructorAssetService assetService;
    private final AuthService authService;

    @GetMapping("/assets/{id}/download")
    public ResponseEntity<?> downloadAsset(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean forceDownload
    ) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Vui long dang nhap de tai nguyen"));

            InstructorAsset asset = assetService.getAssetById(id);

            boolean hasPermission = checkAccessPermission(currentUser, asset);
            if (!hasPermission) {
                return ResponseEntity.status(403).body("Ban khong co quyen xem hoac tai tai nguyen nay.");
            }

            URL url = new URL(asset.getCloudinaryUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);

            InputStream inputStream = conn.getInputStream();
            InputStreamResource resource = new InputStreamResource(inputStream);

            String filename = asset.getName();
            if (filename == null || filename.isBlank()) {
                filename = "asset_" + id;
            }

            String contentType = resolveContentType(filename, asset.getAssetType());
            boolean isInline = !forceDownload && isInlineDisplayable(filename, asset.getAssetType());

            ContentDisposition disposition = ContentDisposition.builder(isInline ? "inline" : "attachment")
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .contentType(MediaType.parseMediaType(contentType));

            long contentLength = conn.getContentLengthLong();
            if (contentLength > 0) {
                builder.contentLength(contentLength);
            } else if (asset.getFileSizeBytes() != null && asset.getFileSizeBytes() > 0) {
                builder.contentLength(asset.getFileSizeBytes());
            }

            return builder.body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Loi tai tai nguyen: " + e.getMessage());
        }
    }

    private boolean checkAccessPermission(User user, InstructorAsset asset) {
        if (user.getRoles().stream().anyMatch(r ->
                "ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_ADMIN".equalsIgnoreCase(r.getCode()) ||
                "MODERATOR".equalsIgnoreCase(r.getCode()) || "ROLE_MODERATOR".equalsIgnoreCase(r.getCode()))) {
            return true;
        }

        if (asset.getInstructor() != null && asset.getInstructor().getId().equals(user.getId())) {
            return true;
        }

        if ("COHORT_PUBLIC".equalsIgnoreCase(asset.getVisibility()) || "PUBLIC".equalsIgnoreCase(asset.getVisibility())) {
            return true;
        }

        if ("DEPARTMENT_SHARED".equalsIgnoreCase(asset.getVisibility()) &&
                user.getRoles().stream().anyMatch(r ->
                        "INSTRUCTOR".equalsIgnoreCase(r.getCode()) ||
                        "ROLE_INSTRUCTOR".equalsIgnoreCase(r.getCode()) ||
                        "Giảng viên".equalsIgnoreCase(r.getName()) ||
                        "INSTRUCTOR".equalsIgnoreCase(r.getName()))) {
            return true;
        }

        if (asset.getSharedEmails() != null && !asset.getSharedEmails().trim().isEmpty()) {
            String[] emails = asset.getSharedEmails().split(",");
            for (String email : emails) {
                if (email.trim().equalsIgnoreCase(user.getEmail())) {
                    return true;
                }
            }
        }

        return false;
    }

    private String resolveContentType(String filename, String assetType) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".txt")) return "text/plain; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".rar")) return "application/x-rar-compressed";

        if ("VIDEO".equalsIgnoreCase(assetType)) return "video/mp4";
        if ("DOCUMENT".equalsIgnoreCase(assetType)) return "application/pdf";

        return "application/octet-stream";
    }

    private boolean isInlineDisplayable(String filename, String assetType) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf") || lower.endsWith(".txt") || lower.endsWith(".png")
                || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".mp4") || lower.endsWith(".webm")) {
            return true;
        }
        return "VIDEO".equalsIgnoreCase(assetType);
    }
}
