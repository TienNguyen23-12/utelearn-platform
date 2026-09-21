package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CloudinaryService;
import vn.edu.ute.utelearn.service.InstructorAssetService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/instructor/assets")
@RequiredArgsConstructor
public class InstructorAssetController {

    private final InstructorAssetService assetService;
    private final AuthService authService;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    public String listAssets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        User currentUser = authService.getCurrentAuthenticatedUser()
                .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

        Page<InstructorAsset> assets = assetService.getAssetsByInstructor(
                currentUser, keyword, type, PageRequest.of(page, 12)
        );

        model.addAttribute("assets", assets);
        model.addAttribute("folders", assetService.getFoldersByInstructor(currentUser));
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        model.addAttribute("currentUser", currentUser);
        
        return "instructor/assets/index";
    }

    @GetMapping("/cloudinary-signature")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSignature(
            @RequestParam String folderPath,
            @RequestParam String assetType,
            @RequestParam String originalFilename
    ) {
        try {
            authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            String resourceType = "auto";
            if ("VIDEO".equalsIgnoreCase(assetType)) resourceType = "video";
            else if ("DOCUMENT".equalsIgnoreCase(assetType) || "SLIDE".equalsIgnoreCase(assetType)) resourceType = "raw";

            Map<String, Object> signatureData = cloudinaryService.generateSignature(folderPath, resourceType, originalFilename);
            return ResponseEntity.ok(signatureData);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
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
            @RequestParam("fileSizeBytes") Long fileSizeBytes
    ) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser()
                    .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

            InstructorAsset asset = assetService.saveAssetMetadata(currentUser, name, assetType, folderPath, visibility, sharedEmails, cloudinaryUrl, fileSizeBytes);
            
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
        return "redirect:/instructor/assets";
    }
}
