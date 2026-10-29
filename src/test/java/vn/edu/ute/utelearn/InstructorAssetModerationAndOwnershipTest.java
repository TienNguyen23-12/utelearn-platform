package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import vn.edu.ute.utelearn.dao.InstructorAssetRepository;
import vn.edu.ute.utelearn.dao.ModerationTaskRepository;
import vn.edu.ute.utelearn.dao.ModeratorAttendanceRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.CloudinaryService;
import vn.edu.ute.utelearn.service.NotificationService;
import vn.edu.ute.utelearn.service.impl.InstructorAssetServiceImpl;
import vn.edu.ute.utelearn.service.impl.ModerationServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstructorAssetModerationAndOwnershipTest {

    @Mock
    private InstructorAssetRepository assetRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ModerationTaskRepository moderationTaskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModeratorAttendanceRepository moderatorAttendanceRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InstructorAssetServiceImpl assetService;

    @InjectMocks
    private ModerationServiceImpl moderationService;

    private User instructorA;
    private User instructorB;
    private User adminUser;
    private User moderatorUser;
    private InstructorAsset sampleAsset;

    @BeforeEach
    void setUp() {
        Role instructorRole = Role.builder().id(2L).code("INSTRUCTOR").name("Giảng viên").build();
        Role adminRole = Role.builder().id(1L).code("ADMIN").name("Quản trị viên").build();
        Role modRole = Role.builder().id(3L).code("MODERATOR").name("Kiểm duyệt viên").build();

        instructorA = User.builder().id(10L).fullName("Instructor A").email("inst_a@utelearn.edu.vn").roles(Set.of(instructorRole)).build();
        instructorB = User.builder().id(20L).fullName("Instructor B").email("inst_b@utelearn.edu.vn").roles(Set.of(instructorRole)).build();
        adminUser = User.builder().id(1L).fullName("Admin").email("admin@utelearn.edu.vn").roles(Set.of(adminRole)).build();
        moderatorUser = User.builder().id(30L).fullName("Moderator").email("mod@utelearn.edu.vn").roles(Set.of(modRole)).build();

        sampleAsset = InstructorAsset.builder()
                .id(100L)
                .name("Tai lieu Lap trinh Java")
                .instructor(instructorA)
                .assetType("DOCUMENT")
                .folderPath("/Java/")
                .visibility("PRIVATE")
                .status("PENDING_MODERATOR")
                .cloudinaryUrl("https://res.cloudinary.com/demo/sample.pdf")
                .build();
    }

    @Test
    void testSaveAssetMetadata_InstructorGetsPendingModerator() {
        when(assetRepository.save(any(InstructorAsset.class))).thenAnswer(i -> i.getArgument(0));

        InstructorAsset saved = assetService.saveAssetMetadata(instructorA, "File Test", "VIDEO", "/Videos/", "PUBLIC", null, "https://url", 1024L);

        assertNotNull(saved);
        assertEquals("PENDING_MODERATOR", saved.getStatus());
        assertEquals("PUBLIC", saved.getVisibility());
    }

    @Test
    void testSaveAssetMetadata_AdminGetsApproved() {
        when(assetRepository.save(any(InstructorAsset.class))).thenAnswer(i -> i.getArgument(0));

        InstructorAsset saved = assetService.saveAssetMetadata(adminUser, "Admin Guide", "DOCUMENT", "/Admin/", "PUBLIC", null, "https://url", 2048L);

        assertNotNull(saved);
        assertEquals("APPROVED", saved.getStatus());
    }

    @Test
    void testUpdateAssetMetadata_OwnerCanUpdate() {
        when(assetRepository.findById(100L)).thenReturn(Optional.of(sampleAsset));
        when(assetRepository.save(any(InstructorAsset.class))).thenAnswer(i -> i.getArgument(0));

        InstructorAsset updated = assetService.updateAssetMetadata(100L, instructorA, "Ten moi", "/NewFolder/", "PUBLIC", "test@utelearn.edu.vn");

        assertEquals("Ten moi", updated.getName());
        assertEquals("/NewFolder/", updated.getFolderPath());
        assertEquals("PUBLIC", updated.getVisibility());
        assertEquals("test@utelearn.edu.vn", updated.getSharedEmails());
    }

    @Test
    void testUpdateAssetMetadata_NonOwnerThrowsException() {
        when(assetRepository.findById(100L)).thenReturn(Optional.of(sampleAsset));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                assetService.updateAssetMetadata(100L, instructorB, "Ten moi", "/NewFolder/", "PUBLIC", null)
        );
        assertTrue(ex.getMessage().contains("không có quyền chỉnh sửa"));
    }

    @Test
    void testUpdateAssetMetadata_AdminCanUpdate() {
        when(assetRepository.findById(100L)).thenReturn(Optional.of(sampleAsset));
        when(assetRepository.save(any(InstructorAsset.class))).thenAnswer(i -> i.getArgument(0));

        InstructorAsset updated = assetService.updateAssetMetadata(100L, adminUser, "Admin Updated", "/AdminFolder/", "PUBLIC", null);

        assertEquals("Admin Updated", updated.getName());
    }

    @Test
    void testCreateAssetModerationTask_SetsPendingModeratorAndCreatesTask() {
        when(assetRepository.findById(100L)).thenReturn(Optional.of(sampleAsset));
        when(userRepository.findById(10L)).thenReturn(Optional.of(instructorA));
        when(moderationTaskRepository.save(any(ModerationTask.class))).thenAnswer(i -> {
            ModerationTask t = i.getArgument(0);
            t.setId(500L);
            return t;
        });

        ModerationTask task = moderationService.createAssetModerationTask(100L, 10L);

        assertNotNull(task);
        assertEquals("ASSET", task.getItemType());
        assertEquals(100L, task.getItemId());
        assertEquals("PENDING", task.getStatus());
        assertEquals("PENDING_MODERATOR", sampleAsset.getStatus());
        verify(assetRepository).save(sampleAsset);
    }

    @Test
    void testApproveAssetTask_UpdatesAssetStatusToApproved() {
        ModerationTask task = ModerationTask.builder()
                .id(500L)
                .itemType("ASSET")
                .itemId(100L)
                .author(instructorA)
                .status("PENDING")
                .build();

        ModeratorAttendance attendance = ModeratorAttendance.builder()
                .moderator(moderatorUser)
                .workDate(LocalDate.now())
                .isActive(true)
                .completedTasksCount(0)
                .build();

        when(moderationTaskRepository.findById(500L)).thenReturn(Optional.of(task));
        when(userRepository.findById(30L)).thenReturn(Optional.of(moderatorUser));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderatorUser, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(assetRepository.findById(100L)).thenReturn(Optional.of(sampleAsset));

        moderationService.approveTask(500L, 30L, "File hop le");

        assertEquals("APPROVED", task.getStatus());
        assertEquals("APPROVED", sampleAsset.getStatus());
        verify(assetRepository).save(sampleAsset);
    }

    @Test
    void testRejectAssetTask_UpdatesAssetStatusToRejected() {
        ModerationTask task = ModerationTask.builder()
                .id(500L)
                .itemType("ASSET")
                .itemId(100L)
                .author(instructorA)
                .status("PENDING")
                .build();

        ModeratorAttendance attendance = ModeratorAttendance.builder()
                .moderator(moderatorUser)
                .workDate(LocalDate.now())
                .isActive(true)
                .completedTasksCount(0)
                .build();

        when(moderationTaskRepository.findById(500L)).thenReturn(Optional.of(task));
        when(userRepository.findById(30L)).thenReturn(Optional.of(moderatorUser));
        when(moderatorAttendanceRepository.findByModeratorAndWorkDate(moderatorUser, LocalDate.now()))
                .thenReturn(Optional.of(attendance));
        when(assetRepository.findById(100L)).thenReturn(Optional.of(sampleAsset));

        moderationService.rejectTask(500L, 30L, "File loi dinh dang");

        assertEquals("REJECTED", task.getStatus());
        assertEquals("REJECTED", sampleAsset.getStatus());
        verify(assetRepository).save(sampleAsset);
    }

    @Test
    void testGetAssets_MineAndSharedScope() {
        InstructorAsset myAsset = InstructorAsset.builder().id(1L).instructor(instructorA).name("My Asset").build();
        InstructorAsset sharedAsset = InstructorAsset.builder().id(2L).instructor(instructorB).name("Shared Asset").status("APPROVED").visibility("PUBLIC").build();

        when(assetRepository.findMyAssets(eq(instructorA), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(myAsset)));
        when(assetRepository.findSharedAssets(eq(instructorA), eq(true), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sharedAsset)));

        Page<InstructorAsset> myPage = assetService.getAssets(instructorA, null, null, "MINE", PageRequest.of(0, 10));
        assertEquals(1, myPage.getTotalElements());
        assertEquals("My Asset", myPage.getContent().get(0).getName());

        Page<InstructorAsset> sharedPage = assetService.getAssets(instructorA, null, null, "SHARED", PageRequest.of(0, 10));
        assertEquals(1, sharedPage.getTotalElements());
        assertEquals("Shared Asset", sharedPage.getContent().get(0).getName());
    }
}
