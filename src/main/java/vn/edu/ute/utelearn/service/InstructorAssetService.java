package vn.edu.ute.utelearn.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;

import java.io.IOException;
import java.util.List;

public interface InstructorAssetService {
    Page<InstructorAsset> getAssetsByInstructor(User instructor, String keyword, String type, Pageable pageable);
    Page<InstructorAsset> getAssets(User instructor, String keyword, String type, String scope, Pageable pageable);
    List<String> getFoldersByInstructor(User instructor);
    InstructorAsset saveAssetMetadata(User instructor, String name, String assetType, String folderPath, String visibility, String sharedEmails, String cloudinaryUrl, Long fileSizeBytes);
    InstructorAsset updateAssetMetadata(Long assetId, User user, String name, String folderPath, String visibility, String sharedEmails);
    void deleteAsset(Long assetId, User instructor) throws IOException;
    InstructorAsset getAssetByIdAndInstructor(Long assetId, User instructor);
    InstructorAsset getAssetById(Long assetId);
}
