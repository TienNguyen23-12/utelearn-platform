package vn.edu.ute.utelearn.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorAssetRepository extends JpaRepository<InstructorAsset, Long> {
    @EntityGraph(attributePaths = {"instructor"})
    Page<InstructorAsset> findByInstructorOrderByCreatedAtDesc(User instructor, Pageable pageable);

    @EntityGraph(attributePaths = {"instructor"})
    Page<InstructorAsset> findByInstructorAndAssetTypeOrderByCreatedAtDesc(User instructor, String assetType, Pageable pageable);

    @EntityGraph(attributePaths = {"instructor"})
    Page<InstructorAsset> findByInstructorAndNameContainingIgnoreCaseOrderByCreatedAtDesc(User instructor, String name, Pageable pageable);

    @EntityGraph(attributePaths = {"instructor"})
    Page<InstructorAsset> findByInstructorAndNameContainingIgnoreCaseAndAssetTypeOrderByCreatedAtDesc(User instructor, String name, String assetType, Pageable pageable);
    
    @EntityGraph(attributePaths = {"instructor"})
    Page<InstructorAsset> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"instructor"})
    Page<InstructorAsset> findByAssetTypeOrderByCreatedAtDesc(String assetType, Pageable pageable);

    @EntityGraph(attributePaths = {"instructor"})
    Page<InstructorAsset> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name, Pageable pageable);

    @EntityGraph(attributePaths = {"instructor"})
    Page<InstructorAsset> findByNameContainingIgnoreCaseAndAssetTypeOrderByCreatedAtDesc(String name, String assetType, Pageable pageable);
    
    @Query("SELECT DISTINCT a.folderPath FROM InstructorAsset a")
    List<String> findAllDistinctFolderPaths();
    
    @Query("SELECT DISTINCT a.folderPath FROM InstructorAsset a WHERE a.instructor = :instructor")
    List<String> findDistinctFolderPathsByInstructor(@Param("instructor") User instructor);

    Optional<InstructorAsset> findByIdAndInstructor(Long id, User instructor);
}
