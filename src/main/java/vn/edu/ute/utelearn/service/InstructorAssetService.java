package vn.edu.ute.utelearn.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;

import java.io.IOException;

public interface InstructorAssetService {
    Page<InstructorAsset> getAssetsByInstructor(User instructor, String keyword, String type, Pageable pageable);
    java.util.List<String> getFoldersByInstructor(User instructor);
    InstructorAsset saveAssetMetadata(User instructor, String name, String assetType, String folderPath, String visibility, String sharedEmails, String cloudinaryUrl, Long fileSizeBytes);
    void deleteAsset(Long assetId, User instructor) throws IOException;
    InstructorAsset getAssetByIdAndInstructor(Long assetId, User instructor);
    InstructorAsset getAssetById(Long assetId);
}
