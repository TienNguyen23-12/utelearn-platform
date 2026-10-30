package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.InstructorAssetRepository;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.CloudinaryService;
import vn.edu.ute.utelearn.service.InstructorAssetService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorAssetServiceImpl implements InstructorAssetService {

    private final InstructorAssetRepository assetRepository;
    private final CloudinaryService cloudinaryService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.service.ModerationService moderationService;

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorAsset> getAssetsByInstructor(User instructor, String keyword, String type, Pageable pageable) {
        return getAssets(instructor, keyword, type, "ALL", pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorAsset> getAssets(User instructor, String keyword, String type, String scope, Pageable pageable) {
        boolean isAdmin = SecurityUtils.isAdmin(instructor);
        boolean isInstructor = SecurityUtils.isInstructor(instructor) || (instructor.getRoles() != null && instructor.getRoles().stream().anyMatch(r ->
                "INSTRUCTOR".equalsIgnoreCase(r.getCode()) || "ROLE_INSTRUCTOR".equalsIgnoreCase(r.getCode()) || "Giảng viên".equalsIgnoreCase(r.getName())));

        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : "";
        String cleanType = (type != null && !type.trim().isEmpty() && !type.equalsIgnoreCase("ALL")) ? type.trim() : "";
        String emailPattern = (instructor != null && instructor.getEmail() != null && !instructor.getEmail().trim().isEmpty())
                ? "%" + instructor.getEmail().trim() + "%"
                : "";

        if ("MINE".equalsIgnoreCase(scope)) {
            return assetRepository.findMyAssets(instructor, cleanKeyword, cleanType, pageable);
        }

        if ("SHARED".equalsIgnoreCase(scope)) {
            return assetRepository.findSharedAssets(instructor, isInstructor, emailPattern, cleanKeyword, cleanType, pageable);
        }

        return assetRepository.findAccessibleAssets(instructor, isAdmin, isInstructor, emailPattern, cleanKeyword, cleanType, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getFoldersByInstructor(User instructor) {
        boolean isAdmin = SecurityUtils.isAdmin(instructor);
        if (isAdmin) {
            return assetRepository.findAllDistinctFolderPaths();
        }
        return assetRepository.findDistinctFolderPathsByInstructor(instructor);
    }

    @Override
    @Transactional
    public InstructorAsset saveAssetMetadata(User instructor, String name, String assetType, String folderPath, String visibility, String sharedEmails, String cloudinaryUrl, Long fileSizeBytes) {
        boolean isAdmin = SecurityUtils.isAdmin(instructor);
        String safeName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Tài nguyên số";
        String safeVisibility = (visibility != null && !visibility.trim().isEmpty()) ? visibility.trim() : "PRIVATE";
        String safeFolder = (folderPath != null && !folderPath.trim().isEmpty()) ? folderPath.trim() : "/";
        Long safeSize = fileSizeBytes != null ? fileSizeBytes : 0L;
        String initialStatus = isAdmin ? "APPROVED" : "PENDING_MODERATOR";

        InstructorAsset asset = InstructorAsset.builder()
                .instructor(instructor)
                .name(safeName)
                .assetType(assetType)
                .cloudinaryUrl(cloudinaryUrl)
                .fileSizeBytes(safeSize)
                .folderPath(safeFolder)
                .visibility(safeVisibility)
                .sharedEmails(sharedEmails)
                .status(initialStatus)
                .build();
        return assetRepository.save(asset);
    }

    @Override
    @Transactional
    public InstructorAsset updateAssetMetadata(Long assetId, User user, String name, String folderPath, String visibility, String sharedEmails) {
        InstructorAsset asset = getAssetById(assetId);
        boolean isOwner = asset.getInstructor() != null && asset.getInstructor().getId().equals(user.getId());
        boolean isAdmin = SecurityUtils.isAdmin(user);
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa tài nguyên này");
        }

        if (name != null && !name.trim().isEmpty()) {
            asset.setName(name.trim());
        }
        if (folderPath != null && !folderPath.trim().isEmpty()) {
            asset.setFolderPath(folderPath.trim());
        }
        if (visibility != null && !visibility.trim().isEmpty()) {
            asset.setVisibility(visibility.trim());
        }
        asset.setSharedEmails(sharedEmails);

        return assetRepository.save(asset);
    }

    @Override
    public InstructorAsset getAssetById(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Tài nguyên không tồn tại"));
    }

    @Override
    @Transactional
    public void deleteAsset(Long assetId, User instructor) throws IOException {
        InstructorAsset asset = getAssetByIdAndInstructor(assetId, instructor);
        
        try {
            String url = asset.getCloudinaryUrl();
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex != -1) {
                String path = url.substring(uploadIndex + 8);
                
                if (path.matches("^v\\d+/.*")) {
                    path = path.replaceFirst("^v\\d+/", "");
                }
                String resourceType = "auto";
                if ("VIDEO".equalsIgnoreCase(asset.getAssetType())) {
                    resourceType = "video";
                } else if ("DOCUMENT".equalsIgnoreCase(asset.getAssetType()) || "SLIDE".equalsIgnoreCase(asset.getAssetType())) {
                    resourceType = "raw";
                }
                
                String publicId = path;
                if (!"raw".equals(resourceType)) {
                    int dotIndex = path.lastIndexOf('.');
                    if (dotIndex != -1) {
                        publicId = path.substring(0, dotIndex);
                    }
                }
                
                publicId = URLDecoder.decode(publicId, StandardCharsets.UTF_8);
                cloudinaryService.deleteFile(publicId, resourceType);
            }
        } catch (Exception e) {
            log.error("Failed to delete file from cloudinary, but proceeding to delete record", e);
        }

        assetRepository.delete(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorAsset getAssetByIdAndInstructor(Long assetId, User instructor) {
        boolean isAdmin = SecurityUtils.isAdmin(instructor);
        if (isAdmin) {
            return assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài nguyên"));
        }
        return assetRepository.findByIdAndInstructor(assetId, instructor)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài nguyên hoặc không có quyền"));
    }

    @Override
    @Transactional
    public InstructorAsset uploadAndRegisterAsset(org.springframework.web.multipart.MultipartFile file, User user) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Tệp tin tải lên không được để trống");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "Tai_lieu_" + System.currentTimeMillis();
        } else {
            originalFilename = originalFilename.trim();
        }

        List<InstructorAsset> existing = assetRepository.findByInstructorAndNameAndFileSizeBytes(user, originalFilename, file.getSize());
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }

        String assetType = resolveAssetType(originalFilename);
        String resourceType = resolveResourceType(assetType);

        java.util.Map<String, Object> uploadResult;
        try {
            uploadResult = cloudinaryService.uploadFile(file, "/assets", resourceType);
        } catch (Exception e) {
            log.error("[InstructorAssetService] Lỗi upload tệp lên Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tải tệp lên máy chủ: " + e.getMessage());
        }

        String cloudinaryUrl = (String) uploadResult.get("secure_url");
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            cloudinaryUrl = (String) uploadResult.get("url");
        }

        InstructorAsset asset = saveAssetMetadata(
                user,
                originalFilename,
                assetType,
                "/",
                "COHORT_PUBLIC",
                null,
                cloudinaryUrl,
                file.getSize()
        );

        if (moderationService != null && !SecurityUtils.isAdmin(user)) {
            try {
                moderationService.createAssetModerationTask(asset.getId(), user.getId());
            } catch (Exception ignored) {}
        }

        return asset;
    }

    private String resolveAssetType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov") || lower.endsWith(".mkv") || lower.endsWith(".webm")) {
            return "VIDEO";
        }
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.endsWith(".pps") || lower.endsWith(".ppsx")) {
            return "SLIDE";
        }
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".svg")) {
            return "IMAGE";
        }
        return "DOCUMENT";
    }

    private String resolveResourceType(String assetType) {
        if ("VIDEO".equalsIgnoreCase(assetType)) {
            return "video";
        }
        if ("IMAGE".equalsIgnoreCase(assetType)) {
            return "image";
        }
        return "raw";
    }
}
