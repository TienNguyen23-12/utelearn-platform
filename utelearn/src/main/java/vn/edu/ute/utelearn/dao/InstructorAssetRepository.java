package vn.edu.ute.utelearn.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;

import java.util.Optional;

@Repository
public interface InstructorAssetRepository extends JpaRepository<InstructorAsset, Long> {
    Page<InstructorAsset> findByInstructorOrderByCreatedAtDesc(User instructor, Pageable pageable);
    Page<InstructorAsset> findByInstructorAndAssetTypeOrderByCreatedAtDesc(User instructor, String assetType, Pageable pageable);
    Page<InstructorAsset> findByInstructorAndNameContainingIgnoreCaseOrderByCreatedAtDesc(User instructor, String name, Pageable pageable);
    
    Page<InstructorAsset> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<InstructorAsset> findByAssetTypeOrderByCreatedAtDesc(String assetType, Pageable pageable);
    Page<InstructorAsset> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name, Pageable pageable);
    
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT a.folderPath FROM InstructorAsset a")
    java.util.List<String> findAllDistinctFolderPaths();
    
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT a.folderPath FROM InstructorAsset a WHERE a.instructor = :instructor")
    java.util.List<String> findDistinctFolderPathsByInstructor(@org.springframework.data.repository.query.Param("instructor") User instructor);

    Optional<InstructorAsset> findByIdAndInstructor(Long id, User instructor);
}
