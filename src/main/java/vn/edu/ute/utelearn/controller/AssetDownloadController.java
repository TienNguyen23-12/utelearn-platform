package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.InstructorAssetService;

import java.io.InputStream;
import java.net.URL;

@Controller
@RequiredArgsConstructor
public class AssetDownloadController {

    private final InstructorAssetService assetService;
    private final AuthService authService;

    @GetMapping("/assets/{id}/download")
    public ResponseEntity<?> downloadAsset(@PathVariable Long id) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Vui long dang nhap de tai nguyen"));

            InstructorAsset asset = assetService.getAssetById(id);

            // Kiem tra quyen
            boolean hasPermission = false;

            if (currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) )) {
                hasPermission = true;
            } else if (asset.getInstructor().getId().equals(currentUser.getId())) {
                hasPermission = true;
            } else if ("COHORT_PUBLIC".equals(asset.getVisibility())) {
                hasPermission = true;
            } else if ("DEPARTMENT_SHARED".equals(asset.getVisibility()) && 
                     currentUser.getRoles().stream().anyMatch(r -> r.getName().equals("INSTRUCTOR"))) {
                hasPermission = true;
            } else if (asset.getSharedEmails() != null && !asset.getSharedEmails().trim().isEmpty()) {
                String[] emails = asset.getSharedEmails().split(",");
                for (String email : emails) {
                    if (email.trim().equalsIgnoreCase(currentUser.getEmail())) {
                        hasPermission = true;
                        break;
                    }
                }
            }

            if (!hasPermission) {
                return ResponseEntity.status(403).body("Ban khong co quyen xem hoac tai tai nguyen nay.");
            }

            URL url = new URL(asset.getCloudinaryUrl());
            InputStream inputStream = url.openStream();
            InputStreamResource resource = new InputStreamResource(inputStream);

            String contentType = "application/octet-stream";
            if ("VIDEO".equals(asset.getAssetType())) contentType = "video/mp4";
            else if ("DOCUMENT".equals(asset.getAssetType())) contentType = "application/pdf";
            
            String filename = asset.getName();
            if ("DOCUMENT".equals(asset.getAssetType()) && !filename.toLowerCase().endsWith(".pdf") && !filename.toLowerCase().endsWith(".doc") && !filename.toLowerCase().endsWith(".docx")) {
                filename += ".pdf";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Loi tai tai nguyen: " + e.getMessage());
        }
    }
}
