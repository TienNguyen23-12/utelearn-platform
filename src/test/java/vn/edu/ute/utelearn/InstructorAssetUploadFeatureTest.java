package vn.edu.ute.utelearn;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.controller.instructor.InstructorAssetController;
import vn.edu.ute.utelearn.dao.InstructorAssetRepository;
import vn.edu.ute.utelearn.dto.AuthResponseDTO;
import vn.edu.ute.utelearn.dto.LoginRequestDTO;
import vn.edu.ute.utelearn.dto.RegisterRequestDTO;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.InstructorAssetService;
import vn.edu.ute.utelearn.service.impl.CloudinaryServiceImpl;
import vn.edu.ute.utelearn.service.impl.InstructorAssetServiceImpl;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class InstructorAssetUploadFeatureTest {

    private Cloudinary cloudinary;
    private UtelearnProperties properties;
    private CloudinaryServiceImpl cloudinaryService;
    private StubAuthService stubAuthService;
    private StubInstructorAssetService stubAssetService;
    private InstructorAssetController controller;
    private StubAssetRepository stubRepo;
    private InstructorAssetServiceImpl assetServiceImpl;

    @BeforeEach
    void setUp() {
        properties = new UtelearnProperties();
        properties.getCloudinary().setCloudName("utelearn");
        properties.getCloudinary().setApiKey("test_key");
        properties.getCloudinary().setApiSecret("test_secret");
        properties.getCloudinary().setFolderBase("utelearn_assets");

        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "utelearn",
                "api_key", "test_key",
                "api_secret", "test_secret"
        ));

        cloudinaryService = new CloudinaryServiceImpl(cloudinary, properties);
        stubAuthService = new StubAuthService();
        stubAssetService = new StubInstructorAssetService();
        controller = new InstructorAssetController(stubAssetService, stubAuthService, cloudinaryService);

        stubRepo = new StubAssetRepository();
        assetServiceImpl = new InstructorAssetServiceImpl(stubRepo, cloudinaryService);
    }

    @Test
    void testGenerateSignature_Success() {
        Map<String, Object> result = cloudinaryService.generateSignature("Thư mục chung", "raw", "Bài giảng #1.pdf");

        assertNotNull(result);
        assertNotNull(result.get("signature"));
        assertFalse(result.get("signature").toString().isBlank());
        assertEquals("test_key", result.get("api_key"));
        assertEquals("utelearn", result.get("cloud_name"));
        assertNotNull(result.get("timestamp"));

        String publicId = (String) result.get("public_id");
        assertNotNull(publicId);
        assertTrue(publicId.startsWith("utelearn_assets/Thu_muc_chung/"));
        assertTrue(publicId.endsWith(".pdf"));
        assertFalse(publicId.contains(" "));
        assertFalse(publicId.contains("ư"));
        assertFalse(publicId.contains("#"));
    }

    @Test
    void testGenerateSignature_VideoAsset() {
        Map<String, Object> result = cloudinaryService.generateSignature("/courses/videos", "video", "lesson_01.mp4");

        assertNotNull(result);
        assertNotNull(result.get("signature"));

        String publicId = (String) result.get("public_id");
        assertNotNull(publicId);
        assertTrue(publicId.startsWith("utelearn_assets/courses/videos/lesson_01_"));
        assertFalse(publicId.endsWith(".mp4"));
    }

    @Test
    void testGetVideoDuration_BlankUrl() {
        assertNull(cloudinaryService.getVideoDuration(null));
        assertNull(cloudinaryService.getVideoDuration(""));
        assertNull(cloudinaryService.getVideoDuration("   "));
        assertNull(cloudinaryService.getVideoDuration("https://example.com/not-cloudinary"));
    }

    @Test
    void testController_GetSignature_Authorized() {
        stubAuthService.setCurrentUser(User.builder().id(1L).email("gv@utelearn.edu.vn").build());

        ResponseEntity<Map<String, Object>> response = controller.getSignature("Thư mục chung", "DOCUMENT", "slide.pdf");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("utelearn", response.getBody().get("cloud_name"));
        assertNotNull(response.getBody().get("signature"));
    }

    @Test
    void testController_GetSignature_Unauthorized() {
        stubAuthService.setCurrentUser(null);

        ResponseEntity<Map<String, Object>> response = controller.getSignature("Thư mục chung", "DOCUMENT", "slide.pdf");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testController_SaveAssetMetadata_Authorized() {
        stubAuthService.setCurrentUser(User.builder().id(1L).email("gv@utelearn.edu.vn").build());

        ResponseEntity<Map<String, Object>> response = controller.saveAssetMetadata(
                "Tài liệu môn Web.pdf",
                "DOCUMENT",
                "Thư mục chung",
                "PUBLIC",
                null,
                "https://res.cloudinary.com/utelearn/raw/upload/utelearn_assets/Thu_muc_chung/doc_123.pdf",
                2048576L
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertNotNull(response.getBody().get("asset"));

        InstructorAsset saved = (InstructorAsset) response.getBody().get("asset");
        assertEquals("Tài liệu môn Web.pdf", saved.getName());
        assertEquals("DOCUMENT", saved.getAssetType());
        assertEquals(2048576L, saved.getFileSizeBytes());
    }

    @Test
    void testController_SaveAssetMetadata_Unauthorized() {
        stubAuthService.setCurrentUser(null);

        ResponseEntity<Map<String, Object>> response = controller.saveAssetMetadata(
                "Tài liệu.pdf",
                "DOCUMENT",
                "/",
                "PRIVATE",
                null,
                "https://res.cloudinary.com/utelearn/raw/upload/file.pdf",
                100L
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("success"));
    }

    @Test
    void testService_SaveAssetMetadata_PersistsCorrectFields() {
        User instructor = User.builder().id(5L).email("gv5@utelearn.edu.vn").build();
        InstructorAsset saved = assetServiceImpl.saveAssetMetadata(
                instructor,
                "De_cuong_chi_tiet.pdf",
                "DOCUMENT",
                "/decuong",
                "PRIVATE",
                "sv@utelearn.edu.vn",
                "https://res.cloudinary.com/utelearn/raw/upload/file.pdf",
                512000L
        );

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        assertEquals("De_cuong_chi_tiet.pdf", saved.getName());
        assertEquals("DOCUMENT", saved.getAssetType());
        assertEquals("/decuong", saved.getFolderPath());
        assertEquals("PRIVATE", saved.getVisibility());
        assertEquals("sv@utelearn.edu.vn", saved.getSharedEmails());
        assertEquals(512000L, saved.getFileSizeBytes());
        assertEquals(1, stubRepo.savedAssets.size());
    }

    @Test
    void testService_GetAssetsByInstructor_AdminFiltersBothKeywordAndType() {
        User admin = User.builder().id(10L).roles(Set.of(Role.builder().code("ADMIN").build())).build();
        stubRepo.savedAssets.add(InstructorAsset.builder().id(1L).name("Asset1").build());

        Page<InstructorAsset> page = assetServiceImpl.getAssetsByInstructor(admin, "Asset", "DOCUMENT", PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }

    private static class StubAuthService implements AuthService {
        private User currentUser;

        public void setCurrentUser(User user) {
            this.currentUser = user;
        }

        @Override
        public Optional<User> getCurrentAuthenticatedUser() {
            return Optional.ofNullable(currentUser);
        }

        @Override public void sendRegistrationOtp(String email) {}
        @Override public User register(RegisterRequestDTO requestDTO) { return null; }
        @Override public AuthResponseDTO login(LoginRequestDTO requestDTO, HttpServletResponse response) { return null; }
        @Override public void logout(HttpServletResponse response) {}
        @Override public void processForgotPassword(String email) {}
        @Override public void processResetPassword(String token, String newPassword) {}
    }

    private static class StubInstructorAssetService implements InstructorAssetService {
        @Override
        public Page<InstructorAsset> getAssetsByInstructor(User instructor, String keyword, String type, Pageable pageable) {
            return Page.empty();
        }

        @Override
        public List<String> getFoldersByInstructor(User instructor) {
            return List.of("Thư mục chung");
        }

        @Override
        public InstructorAsset saveAssetMetadata(User instructor, String name, String assetType, String folderPath, String visibility, String sharedEmails, String cloudinaryUrl, Long fileSizeBytes) {
            return InstructorAsset.builder()
                    .id(100L)
                    .instructor(instructor)
                    .name(name)
                    .assetType(assetType)
                    .folderPath(folderPath)
                    .visibility(visibility)
                    .sharedEmails(sharedEmails)
                    .cloudinaryUrl(cloudinaryUrl)
                    .fileSizeBytes(fileSizeBytes)
                    .build();
        }

        @Override
        public void deleteAsset(Long assetId, User instructor) throws IOException {}

        @Override
        public InstructorAsset getAssetByIdAndInstructor(Long assetId, User instructor) {
            return null;
        }

        @Override
        public InstructorAsset getAssetById(Long assetId) {
            return null;
        }
    }

    private static class StubAssetRepository implements InstructorAssetRepository {
        List<InstructorAsset> savedAssets = new ArrayList<>();
        private long idGen = 1L;

        @Override
        public <S extends InstructorAsset> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen++);
            }
            savedAssets.add(entity);
            return entity;
        }

        @Override
        public Optional<InstructorAsset> findById(Long id) {
            return savedAssets.stream().filter(a -> a.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<InstructorAsset> findByIdAndInstructor(Long id, User instructor) {
            return savedAssets.stream().filter(a -> a.getId().equals(id) && a.getInstructor().equals(instructor)).findFirst();
        }

        @Override
        public Page<InstructorAsset> findByNameContainingIgnoreCaseAndAssetTypeOrderByCreatedAtDesc(String name, String assetType, Pageable pageable) {
            return new PageImpl<>(savedAssets);
        }

        @Override
        public Page<InstructorAsset> findByInstructorAndNameContainingIgnoreCaseAndAssetTypeOrderByCreatedAtDesc(User instructor, String name, String assetType, Pageable pageable) {
            return new PageImpl<>(savedAssets);
        }

        @Override public Page<InstructorAsset> findByInstructorOrderByCreatedAtDesc(User instructor, Pageable pageable) { return new PageImpl<>(savedAssets); }
        @Override public Page<InstructorAsset> findByInstructorAndNameContainingIgnoreCaseOrderByCreatedAtDesc(User instructor, String name, Pageable pageable) { return new PageImpl<>(savedAssets); }
        @Override public Page<InstructorAsset> findByInstructorAndAssetTypeOrderByCreatedAtDesc(User instructor, String assetType, Pageable pageable) { return new PageImpl<>(savedAssets); }
        @Override public Page<InstructorAsset> findAllByOrderByCreatedAtDesc(Pageable pageable) { return new PageImpl<>(savedAssets); }
        @Override public Page<InstructorAsset> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name, Pageable pageable) { return new PageImpl<>(savedAssets); }
        @Override public Page<InstructorAsset> findByAssetTypeOrderByCreatedAtDesc(String assetType, Pageable pageable) { return new PageImpl<>(savedAssets); }
        @Override public List<String> findDistinctFolderPathsByInstructor(User instructor) { return List.of(); }
        @Override public List<String> findAllDistinctFolderPaths() { return List.of(); }
        @Override public <S extends InstructorAsset> List<S> saveAll(Iterable<S> entities) { return List.of(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<InstructorAsset> findAll() { return savedAssets; }
        @Override public List<InstructorAsset> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return savedAssets.size(); }
        @Override public void deleteById(Long id) { savedAssets.removeIf(a -> a.getId().equals(id)); }
        @Override public void delete(InstructorAsset entity) { savedAssets.remove(entity); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends InstructorAsset> entities) {}
        @Override public void deleteAll() { savedAssets.clear(); }
        @Override public List<InstructorAsset> findAll(Sort sort) { return savedAssets; }
        @Override public Page<InstructorAsset> findAll(Pageable pageable) { return new PageImpl<>(savedAssets); }
        @Override public <S extends InstructorAsset> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends InstructorAsset> List<S> findAll(Example<S> example) { return List.of(); }
        @Override public <S extends InstructorAsset> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends InstructorAsset> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
        @Override public <S extends InstructorAsset> long count(Example<S> example) { return 0; }
        @Override public <S extends InstructorAsset> boolean exists(Example<S> example) { return false; }
        @Override public <S extends InstructorAsset, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public void flush() {}
        @Override public <S extends InstructorAsset> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends InstructorAsset> List<S> saveAllAndFlush(Iterable<S> entities) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<InstructorAsset> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public InstructorAsset getOne(Long id) { return findById(id).orElse(null); }
        @Override public InstructorAsset getById(Long id) { return findById(id).orElse(null); }
        @Override public InstructorAsset getReferenceById(Long id) { return findById(id).orElse(null); }
    }
}
