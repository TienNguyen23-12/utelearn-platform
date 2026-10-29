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

    @Query(value = "SELECT a FROM InstructorAsset a LEFT JOIN FETCH a.instructor WHERE " +
           "(:isAdmin = true OR a.instructor = :user OR (a.status = 'APPROVED' AND (a.visibility = 'PUBLIC' OR a.visibility = 'COHORT_PUBLIC' OR (:isInstructor = true AND a.visibility = 'DEPARTMENT_SHARED') OR (:userEmailPattern <> '' AND a.sharedEmails LIKE :userEmailPattern)))) " +
           "AND (:keyword = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type = '' OR a.assetType = :type) " +
           "ORDER BY a.createdAt DESC",
           countQuery = "SELECT count(a) FROM InstructorAsset a WHERE " +
           "(:isAdmin = true OR a.instructor = :user OR (a.status = 'APPROVED' AND (a.visibility = 'PUBLIC' OR a.visibility = 'COHORT_PUBLIC' OR (:isInstructor = true AND a.visibility = 'DEPARTMENT_SHARED') OR (:userEmailPattern <> '' AND a.sharedEmails LIKE :userEmailPattern)))) " +
           "AND (:keyword = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type = '' OR a.assetType = :type)")
    Page<InstructorAsset> findAccessibleAssets(
            @Param("user") User user,
            @Param("isAdmin") boolean isAdmin,
            @Param("isInstructor") boolean isInstructor,
            @Param("userEmailPattern") String userEmailPattern,
            @Param("keyword") String keyword,
            @Param("type") String type,
            Pageable pageable);

    @Query(value = "SELECT a FROM InstructorAsset a LEFT JOIN FETCH a.instructor WHERE " +
           "a.instructor = :user " +
           "AND (:keyword = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type = '' OR a.assetType = :type) " +
           "ORDER BY a.createdAt DESC",
           countQuery = "SELECT count(a) FROM InstructorAsset a WHERE " +
           "a.instructor = :user " +
           "AND (:keyword = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type = '' OR a.assetType = :type)")
    Page<InstructorAsset> findMyAssets(
            @Param("user") User user,
            @Param("keyword") String keyword,
            @Param("type") String type,
            Pageable pageable);

    @Query(value = "SELECT a FROM InstructorAsset a LEFT JOIN FETCH a.instructor WHERE " +
           "a.instructor <> :user AND a.status = 'APPROVED' AND (a.visibility = 'PUBLIC' OR a.visibility = 'COHORT_PUBLIC' OR (:isInstructor = true AND a.visibility = 'DEPARTMENT_SHARED') OR (:userEmailPattern <> '' AND a.sharedEmails LIKE :userEmailPattern)) " +
           "AND (:keyword = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type = '' OR a.assetType = :type) " +
           "ORDER BY a.createdAt DESC",
           countQuery = "SELECT count(a) FROM InstructorAsset a WHERE " +
           "a.instructor <> :user AND a.status = 'APPROVED' AND (a.visibility = 'PUBLIC' OR a.visibility = 'COHORT_PUBLIC' OR (:isInstructor = true AND a.visibility = 'DEPARTMENT_SHARED') OR (:userEmailPattern <> '' AND a.sharedEmails LIKE :userEmailPattern)) " +
           "AND (:keyword = '' OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type = '' OR a.assetType = :type)")
    Page<InstructorAsset> findSharedAssets(
            @Param("user") User user,
            @Param("isInstructor") boolean isInstructor,
            @Param("userEmailPattern") String userEmailPattern,
            @Param("keyword") String keyword,
            @Param("type") String type,
            Pageable pageable);
}
