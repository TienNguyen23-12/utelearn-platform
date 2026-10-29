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

    @Override
    @Transactional(readOnly = true)
    public Page<InstructorAsset> getAssetsByInstructor(User instructor, String keyword, String type, Pageable pageable) {
        boolean isAdmin = SecurityUtils.isAdmin(instructor);
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasType = type != null && !type.trim().isEmpty() && !type.equalsIgnoreCase("ALL");
        String cleanKeyword = hasKeyword ? keyword.trim() : null;
        String cleanType = hasType ? type.trim() : null;

        if (isAdmin) {
            if (hasKeyword && hasType) {
                return assetRepository.findByNameContainingIgnoreCaseAndAssetTypeOrderByCreatedAtDesc(cleanKeyword, cleanType, pageable);
            }
            if (hasKeyword) {
                return assetRepository.findByNameContainingIgnoreCaseOrderByCreatedAtDesc(cleanKeyword, pageable);
            }
            if (hasType) {
                return assetRepository.findByAssetTypeOrderByCreatedAtDesc(cleanType, pageable);
            }
            return assetRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        if (hasKeyword && hasType) {
            return assetRepository.findByInstructorAndNameContainingIgnoreCaseAndAssetTypeOrderByCreatedAtDesc(instructor, cleanKeyword, cleanType, pageable);
        }
        if (hasKeyword) {
            return assetRepository.findByInstructorAndNameContainingIgnoreCaseOrderByCreatedAtDesc(instructor, cleanKeyword, pageable);
        }
        if (hasType) {
            return assetRepository.findByInstructorAndAssetTypeOrderByCreatedAtDesc(instructor, cleanType, pageable);
        }
        return assetRepository.findByInstructorOrderByCreatedAtDesc(instructor, pageable);
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
        String safeName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Tài nguyên số";
        String safeVisibility = (visibility != null && !visibility.trim().isEmpty()) ? visibility.trim() : "PRIVATE";
        String safeFolder = (folderPath != null && !folderPath.trim().isEmpty()) ? folderPath.trim() : "/";
        Long safeSize = fileSizeBytes != null ? fileSizeBytes : 0L;

        InstructorAsset asset = InstructorAsset.builder()
                .instructor(instructor)
                .name(safeName)
                .assetType(assetType)
                .cloudinaryUrl(cloudinaryUrl)
                .fileSizeBytes(safeSize)
                .folderPath(safeFolder)
                .visibility(safeVisibility)
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
}
