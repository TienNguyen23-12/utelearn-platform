package vn.edu.ute.utelearn.controller.instructor;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CloudinaryService;
import vn.edu.ute.utelearn.service.InstructorAssetService;
import vn.edu.ute.utelearn.service.ModerationService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping({"/assets", "/instructor/assets"})
@PreAuthorize("hasAnyAuthority('ASSET_MANAGE', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('ASSET_MANAGE')")
@RequiredArgsConstructor
public class InstructorAssetController {

    private final InstructorAssetService assetService;
    private final AuthService authService;
    private final CloudinaryService cloudinaryService;
    private final ModerationService moderationService;

    @GetMapping
    public String listAssets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "ALL") String scope,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

        Page<InstructorAsset> assets = assetService.getAssets(
                currentUser, keyword, type, scope, PageRequest.of(page, 12)
        );

        model.addAttribute("assets", assets);
        model.addAttribute("folders", assetService.getFoldersByInstructor(currentUser));
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        model.addAttribute("scope", scope);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", SecurityUtils.isAdmin(currentUser));
        
        return "instructor/assets/index";
    }

    @GetMapping("/cloudinary-signature")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSignature(
            @RequestParam String folderPath,
            @RequestParam String assetType,
            @RequestParam String originalFilename
    ) {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String resourceType = "auto";
            if ("VIDEO".equalsIgnoreCase(assetType)) {
                resourceType = "video";
            } else if ("DOCUMENT".equalsIgnoreCase(assetType) || "SLIDE".equalsIgnoreCase(assetType)) {
                resourceType = "raw";
            }

            Map<String, Object> signatureData = cloudinaryService.generateSignature(folderPath, resourceType, originalFilename);
            return ResponseEntity.ok(signatureData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveAssetMetadata(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam("assetType") String assetType,
            @RequestParam(value = "folderPath", defaultValue = "/") String folderPath,
            @RequestParam(value = "visibility", defaultValue = "PRIVATE") String visibility,
            @RequestParam(value = "sharedEmails", required = false) String sharedEmails,
            @RequestParam("cloudinaryUrl") String cloudinaryUrl,
            @RequestParam(value = "fileSizeBytes", defaultValue = "0") Long fileSizeBytes
    ) {
        Optional<User> currentUser = authService.getCurrentAuthenticatedUser();
        if (currentUser.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Chưa đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        try {
            InstructorAsset asset = assetService.saveAssetMetadata(currentUser.get(), name, assetType, folderPath, visibility, sharedEmails, cloudinaryUrl, fileSizeBytes);
            
            if (!SecurityUtils.isAdmin(currentUser.get())) {
                try {
                    moderationService.createAssetModerationTask(asset.getId(), currentUser.get().getId());
                } catch (Exception ignored) {}
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("asset", asset);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/edit")
    public String editAsset(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "folderPath", defaultValue = "/") String folderPath,
            @RequestParam(value = "visibility", defaultValue = "PRIVATE") String visibility,
            @RequestParam(value = "sharedEmails", required = false) String sharedEmails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));
            assetService.updateAssetMetadata(id, currentUser, name, folderPath, visibility, sharedEmails);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin tài nguyên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/{id}/submit-review")
    public String submitAssetForReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));
            InstructorAsset asset = assetService.getAssetById(id);
            boolean isOwner = asset.getInstructor() != null && asset.getInstructor().getId().equals(currentUser.getId());
            boolean isAdmin = SecurityUtils.isAdmin(currentUser);
            if (!isOwner && !isAdmin) {
                throw new RuntimeException("Bạn không có quyền gửi duyệt tài nguyên này");
            }
            moderationService.createAssetModerationTask(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi tài nguyên cho Moderator kiểm duyệt!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/{id}/delete")
    public String deleteAsset(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));
                    
            assetService.deleteAsset(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài nguyên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/assets";
    }
}
