package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dao.InstructorAssetRepository;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.CloudinaryService;
import vn.edu.ute.utelearn.service.InstructorAssetService;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstructorAssetServiceImpl implements InstructorAssetService {

    private final InstructorAssetRepository assetRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorAsset> getAssetsByInstructor(User instructor, String keyword, String type, Pageable pageable) {
        boolean isAdmin = instructor.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) );
                
        if (isAdmin) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                return assetRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(keyword.trim(), pageable);
            } else if (type != null && !type.trim().isEmpty() && !type.equals("ALL")) {
                return assetRepository.findByAssetTypeOrderByCreatedAtDesc(type, pageable);
            }
            return assetRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            if (keyword != null && !keyword.trim().isEmpty()) {
                return assetRepository.findByInstructorAndNameContainingIgnoreCaseOrderByCreatedAtDesc(instructor, keyword.trim(), pageable);
            } else if (type != null && !type.trim().isEmpty() && !type.equals("ALL")) {
                return assetRepository.findByInstructorAndAssetTypeOrderByCreatedAtDesc(instructor, type, pageable);
            }
            return assetRepository.findByInstructorOrderByCreatedAtDesc(instructor, pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<String> getFoldersByInstructor(User instructor) {
        boolean isAdmin = instructor.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) );
        if (isAdmin) {
            return assetRepository.findAllDistinctFolderPaths();
        }
        return assetRepository.findDistinctFolderPathsByInstructor(instructor);
    }

    @Override
    public InstructorAsset saveAssetMetadata(User instructor, String name, String assetType, String folderPath, String visibility, String sharedEmails, String cloudinaryUrl, Long fileSizeBytes) {
        InstructorAsset asset = InstructorAsset.builder()
                .instructor(instructor)
                .name(name)
                .assetType(assetType)
                .cloudinaryUrl(cloudinaryUrl)
                .fileSizeBytes(fileSizeBytes)
                .folderPath(folderPath)
                .visibility(visibility != null ? visibility : "PRIVATE")
                .sharedEmails(sharedEmails)
                .build();
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
        
        // Extract public_id from cloudinary_url
        // Example url: https://res.cloudinary.com/demo/image/upload/v1312461204/sample.jpg
        // Public ID is usually 'sample' or 'folder/sample'
        try {
            String url = asset.getCloudinaryUrl();
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex != -1) {
                String path = url.substring(uploadIndex + 8);
                // Remove version like /v1234567/
                if (path.matches("^v\\d+/.*")) {
                    path = path.replaceFirst("^v\\d+/", "");
                }
                String resourceType = "auto";
                if ("VIDEO".equalsIgnoreCase(asset.getAssetType())) resourceType = "video";
                else if ("DOCUMENT".equalsIgnoreCase(asset.getAssetType()) || "SLIDE".equalsIgnoreCase(asset.getAssetType())) resourceType = "raw";
                
                String publicId = path;
                if (!"raw".equals(resourceType)) {
                    // Remove extension for non-raw files (image/video)
                    int dotIndex = path.lastIndexOf('.');
                    if (dotIndex != -1) {
                        publicId = path.substring(0, dotIndex);
                    }
                }
                
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
        boolean isAdmin = instructor.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getCode()) );
        if (isAdmin) {
            return assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài nguyên"));
        }
        return assetRepository.findByIdAndInstructor(assetId, instructor)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài nguyên hoặc không có quyền"));
    }
}
