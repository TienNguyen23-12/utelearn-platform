package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.ute.utelearn.controller.AssetDownloadController;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.entity.Role;
import vn.edu.ute.utelearn.entity.User;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AssetDownloadControllerTest {

    private AssetDownloadController downloadController;
    private User instructor;
    private User student;

    @BeforeEach
    void setUp() {
        downloadController = new AssetDownloadController(null, null);

        Role instRole = Role.builder().id(2L).code("INSTRUCTOR").name("Giảng viên").build();
        Role studentRole = Role.builder().id(3L).code("STUDENT").name("Học viên").build();

        instructor = User.builder().id(10L).fullName("Instructor A").email("inst@utelearn.edu.vn").roles(Set.of(instRole)).build();
        student = User.builder().id(20L).fullName("Student B").email("student@utelearn.edu.vn").roles(Set.of(studentRole)).build();
    }

    @Test
    void testResolveContentTypeAndInline() {
        String docxType = ReflectionTestUtils.invokeMethod(downloadController, "resolveContentType", "BaoCao.docx", "DOCUMENT");
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxType);

        boolean docxInline = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(downloadController, "isInlineDisplayable", "BaoCao.docx", "DOCUMENT"));
        assertFalse(docxInline);

        String pdfType = ReflectionTestUtils.invokeMethod(downloadController, "resolveContentType", "TaiLieu.pdf", "DOCUMENT");
        assertEquals("application/pdf", pdfType);

        boolean pdfInline = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(downloadController, "isInlineDisplayable", "TaiLieu.pdf", "DOCUMENT"));
        assertTrue(pdfInline);

        String mp4Type = ReflectionTestUtils.invokeMethod(downloadController, "resolveContentType", "Video.mp4", "VIDEO");
        assertEquals("video/mp4", mp4Type);

        boolean mp4Inline = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(downloadController, "isInlineDisplayable", "Video.mp4", "VIDEO"));
        assertTrue(mp4Inline);
    }

    @Test
    void testCheckAccessPermission_OwnerHasPermission() {
        InstructorAsset asset = InstructorAsset.builder()
                .id(26L)
                .name("BaoCao.docx")
                .instructor(instructor)
                .visibility("PRIVATE")
                .build();

        boolean hasPerm = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(downloadController, "checkAccessPermission", instructor, asset));
        assertTrue(hasPerm);
    }

    @Test
    void testCheckAccessPermission_StrangerPrivateDenied() {
        InstructorAsset asset = InstructorAsset.builder()
                .id(26L)
                .name("BaoCao.docx")
                .instructor(instructor)
                .visibility("PRIVATE")
                .build();

        boolean hasPerm = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(downloadController, "checkAccessPermission", student, asset));
        assertFalse(hasPerm);
    }

    @Test
    void testCheckAccessPermission_PublicAllowed() {
        InstructorAsset asset = InstructorAsset.builder()
                .id(26L)
                .name("BaoCao.docx")
                .instructor(instructor)
                .visibility("PUBLIC")
                .build();

        boolean hasPerm = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(downloadController, "checkAccessPermission", student, asset));
        assertTrue(hasPerm);
    }
}
