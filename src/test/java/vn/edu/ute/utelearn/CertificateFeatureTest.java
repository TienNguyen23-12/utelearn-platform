package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import vn.edu.ute.utelearn.controller.CertificateController;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dto.CertificateViewDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.impl.CertificateServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CertificateFeatureTest {

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private vn.edu.ute.utelearn.dao.EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CertificateServiceImpl certificateService;

    private CertificateController certificateController;

    private User sampleUser;
    private Cohort sampleCohortWithScore;
    private Cohort sampleCohortWithoutScore;
    private CohortMember sampleMemberWithScore;
    private CohortMember sampleMemberWithoutScore;

    @BeforeEach
    void setUp() {
        certificateController = new CertificateController(certificateService);

        sampleUser = User.builder()
                .id(101L)
                .fullName("Nguyễn Văn A")
                .email("student_a@ute.edu.vn")
                .build();

        Course course = Course.builder()
                .id(1L)
                .code("CS101")
                .title("Lập trình Web chuyên sâu")
                .build();

        sampleCohortWithScore = Cohort.builder()
                .id(13L)
                .code("WEB-2026-K1")
                .name("Lập trình Web - K26")
                .course(course)
                .gradingPolicy("WEIGHTED_GRADING")
                .isGradesPublished(true)
                .build();

        sampleCohortWithoutScore = Cohort.builder()
                .id(14L)
                .code("WEB-PROG-K1")
                .name("Nhập môn Lập trình Web (Tự học)")
                .course(course)
                .gradingPolicy("PROGRESS_BASED")
                .isGradesPublished(true)
                .build();

        sampleMemberWithScore = CohortMember.builder()
                .id(1L)
                .user(sampleUser)
                .cohort(sampleCohortWithScore)
                .finalGrade(new BigDecimal("9.20"))
                .isPassed(true)
                .gradedAt(Instant.now())
                .rating(5)
                .certificateUrl("/certificates/verify?code=UTE-CERT-WEB-2026-K1-101")
                .build();

        sampleMemberWithoutScore = CohortMember.builder()
                .id(2L)
                .user(sampleUser)
                .cohort(sampleCohortWithoutScore)
                .finalGrade(null)
                .isPassed(true)
                .gradedAt(Instant.now())
                .rating(4)
                .certificateUrl("/certificates/verify?code=UTE-CERT-WEB-PROG-K1-101")
                .build();
    }

    @Test
    void testGetCertificateByCode_WithScore_ReturnsCorrectDTO() {
        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-WEB-2026-K1-101"))
                .thenReturn(List.of(sampleMemberWithScore));

        Optional<CertificateViewDTO> certOpt = certificateService.getCertificateByCode("UTE-CERT-WEB-2026-K1-101");

        assertTrue(certOpt.isPresent());
        CertificateViewDTO cert = certOpt.get();
        assertEquals("UTE-CERT-WEB-2026-K1-101", cert.getCertCode());
        assertEquals("Nguyễn Văn A", cert.getStudentName());
        assertEquals("WEB-2026-K1", cert.getCourseCode());
        assertTrue(cert.isHasScore());
        assertEquals("WITH_SCORE", cert.getTemplateType());
        assertEquals("/images/certificates/UTELearn_Cert_WithScore.png", cert.getTemplateImageUrl());
        assertEquals("A+", cert.getGradeClassification());
        assertTrue(cert.isValid());
    }

    @Test
    void testGetCertificateByCode_WithoutScore_ReturnsWithoutScoreDTO() {
        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-WEB-PROG-K1-101"))
                .thenReturn(List.of(sampleMemberWithoutScore));

        Optional<CertificateViewDTO> certOpt = certificateService.getCertificateByCode("UTE-CERT-WEB-PROG-K1-101");

        assertTrue(certOpt.isPresent());
        CertificateViewDTO cert = certOpt.get();
        assertEquals("UTE-CERT-WEB-PROG-K1-101", cert.getCertCode());
        assertEquals("WEB-PROG-K1", cert.getCourseCode());
        assertFalse(cert.isHasScore());
        assertEquals("WITHOUT_SCORE", cert.getTemplateType());
        assertEquals("/images/certificates/UTELearn_Cert_WithoutScore.png", cert.getTemplateImageUrl());
        assertEquals("HOÀN THÀNH", cert.getGradeClassification());
        assertTrue(cert.isValid());
    }

    @Test
    void testGetCertificateByCode_NotFound_ReturnsEmpty() {
        when(cohortMemberRepository.findByCertificateCode("NON-EXISTENT"))
                .thenReturn(Collections.emptyList());

        Optional<CertificateViewDTO> certOpt = certificateService.getCertificateByCode("NON-EXISTENT");

        assertTrue(certOpt.isEmpty());
    }

    @Test
    void testGetCertificateByCode_GradesNotPublished_ReturnsInvalid() {
        sampleCohortWithScore.setIsGradesPublished(false);
        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-WEB-2026-K1-101"))
                .thenReturn(List.of(sampleMemberWithScore));

        Optional<CertificateViewDTO> certOpt = certificateService.getCertificateByCode("UTE-CERT-WEB-2026-K1-101");

        assertTrue(certOpt.isPresent());
        CertificateViewDTO cert = certOpt.get();
        assertFalse(cert.isPublished());
        assertFalse(cert.isValid());
    }

    @Test
    void testController_Verify_NoCode_ShowsSearchForm() {
        Model model = new ConcurrentModel();
        String view = certificateController.verifyCertificate(null, model);

        assertEquals("certificates/verify", view);
        assertEquals(false, model.getAttribute("hasSearched"));
    }

    @Test
    void testController_Verify_ValidCode_PopulatesModel() {
        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-WEB-2026-K1-101"))
                .thenReturn(List.of(sampleMemberWithScore));

        Model model = new ConcurrentModel();
        String view = certificateController.verifyCertificate("UTE-CERT-WEB-2026-K1-101", model);

        assertEquals("certificates/verify", view);
        assertEquals(true, model.getAttribute("hasSearched"));
        assertEquals(true, model.getAttribute("isValid"));
        assertNotNull(model.getAttribute("cert"));
    }

    @Test
    void testController_ApiVerify_Success() {
        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-WEB-2026-K1-101"))
                .thenReturn(List.of(sampleMemberWithScore));

        ResponseEntity<?> response = certificateController.apiVerifyCertificate("UTE-CERT-WEB-2026-K1-101");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("success"));
        assertEquals(true, body.get("isValid"));
    }

    @Test
    void testGetCertificateByCode_NotReviewed_RequiresReviewAndNotValid() {
        sampleMemberWithScore.setRating(null);
        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-WEB-2026-K1-101"))
                .thenReturn(List.of(sampleMemberWithScore));

        Optional<CertificateViewDTO> certOpt = certificateService.getCertificateByCode("UTE-CERT-WEB-2026-K1-101");

        assertTrue(certOpt.isPresent());
        CertificateViewDTO cert = certOpt.get();
        assertFalse(cert.isValid());
        assertFalse(cert.isHasReviewed());
        assertTrue(cert.isRequiresReview());
    }

    @Test
    void testSubmitCourseReview_Success() {
        sampleMemberWithScore.setRating(null);
        when(cohortMemberRepository.findByCohort_IdAndUser_Id(13L, 101L))
                .thenReturn(Optional.of(sampleMemberWithScore));
        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-WEB-2026-K1-101"))
                .thenReturn(List.of(sampleMemberWithScore));

        CertificateViewDTO result = certificateService.submitCourseReview(101L, 13L, null, 5, "Khóa học rất hữu ích");

        assertNotNull(result);
        assertEquals(5, sampleMemberWithScore.getRating());
        assertEquals("Khóa học rất hữu ích", sampleMemberWithScore.getReviewComment());
        assertNotNull(sampleMemberWithScore.getReviewedAt());
        assertTrue(result.isValid());
        assertTrue(result.isHasReviewed());
    }

    @Test
    void testSubmitCourseReview_InvalidRating_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            certificateService.submitCourseReview(101L, 13L, null, 0, "Không hợp lệ");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            certificateService.submitCourseReview(101L, 13L, null, 6, "Quá số sao");
        });
    }

    @Test
    void testController_SubmitReview_Success() {
        when(cohortMemberRepository.findByCohort_IdAndUser_Id(13L, 101L))
                .thenReturn(Optional.of(sampleMemberWithScore));
        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-WEB-2026-K1-101"))
                .thenReturn(List.of(sampleMemberWithScore));

        vn.edu.ute.utelearn.dto.CourseReviewRequestDTO req = vn.edu.ute.utelearn.dto.CourseReviewRequestDTO.builder()
                .cohortId(13L)
                .rating(5)
                .comment("Tuyệt vời")
                .build();

        vn.edu.ute.utelearn.service.AuthService mockAuth = org.mockito.Mockito.mock(vn.edu.ute.utelearn.service.AuthService.class);
        when(mockAuth.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));
        org.springframework.test.util.ReflectionTestUtils.setField(certificateController, "authService", mockAuth);

        ResponseEntity<?> response = certificateController.submitReview(req);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("success"));
    }

    @Test
    void testCalculateClassification_MatchesGradingScaleTable() {
        assertEquals("A+", certificateService.calculateClassification(true, BigDecimal.valueOf(10.0)));
        assertEquals("A+", certificateService.calculateClassification(true, BigDecimal.valueOf(9.5)));
        assertEquals("A+", certificateService.calculateClassification(true, BigDecimal.valueOf(9.0)));
        assertEquals("A", certificateService.calculateClassification(true, BigDecimal.valueOf(8.9)));
        assertEquals("A", certificateService.calculateClassification(true, BigDecimal.valueOf(8.75)));
        assertEquals("A", certificateService.calculateClassification(true, BigDecimal.valueOf(8.5)));
        assertEquals("B+", certificateService.calculateClassification(true, BigDecimal.valueOf(8.4)));
        assertEquals("B+", certificateService.calculateClassification(true, BigDecimal.valueOf(8.0)));
        assertEquals("B", certificateService.calculateClassification(true, BigDecimal.valueOf(7.9)));
        assertEquals("B", certificateService.calculateClassification(true, BigDecimal.valueOf(7.0)));
        assertEquals("C+", certificateService.calculateClassification(true, BigDecimal.valueOf(6.9)));
        assertEquals("C+", certificateService.calculateClassification(true, BigDecimal.valueOf(6.5)));
        assertEquals("C", certificateService.calculateClassification(true, BigDecimal.valueOf(6.4)));
        assertEquals("C", certificateService.calculateClassification(true, BigDecimal.valueOf(5.5)));
        assertEquals("D+", certificateService.calculateClassification(true, BigDecimal.valueOf(5.4)));
        assertEquals("D+", certificateService.calculateClassification(true, BigDecimal.valueOf(5.0)));
        assertEquals("D", certificateService.calculateClassification(true, BigDecimal.valueOf(4.9)));
        assertEquals("D", certificateService.calculateClassification(true, BigDecimal.valueOf(4.0)));
        assertEquals("F", certificateService.calculateClassification(true, BigDecimal.valueOf(3.9)));
        assertEquals("F", certificateService.calculateClassification(true, BigDecimal.valueOf(0.0)));
        assertEquals("HOÀN THÀNH", certificateService.calculateClassification(false, BigDecimal.valueOf(9.5)));
        assertEquals("HOÀN THÀNH", certificateService.calculateClassification(true, null));
    }

    @Test
    void testVerifyCourseCertificate_Success() {
        Course course = Course.builder().id(1L).code("CS101").title("Lập trình Web chuyên sâu").build();
        vn.edu.ute.utelearn.entity.Enrollment sampleEnrollment = vn.edu.ute.utelearn.entity.Enrollment.builder()
                .id(99L)
                .user(sampleUser)
                .course(course)
                .isCompleted(true)
                .averageGrade(new BigDecimal("9.50"))
                .rating(5)
                .certificateUrl("/certificates/verify?code=UTE-CERT-CS101-101")
                .completedAt(Instant.now())
                .build();

        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-CS101-101")).thenReturn(Collections.emptyList());
        when(enrollmentRepository.findByCertificateCode("UTE-CERT-CS101-101")).thenReturn(List.of(sampleEnrollment));

        Model model = new ConcurrentModel();
        String view = certificateController.verifyCertificate("UTE-CERT-CS101-101", model);

        assertEquals("certificates/verify", view);
        assertTrue((Boolean) model.getAttribute("hasSearched"));
        assertTrue((Boolean) model.getAttribute("isValid"));
        assertFalse((Boolean) model.getAttribute("requiresReview"));

        CertificateViewDTO cert = (CertificateViewDTO) model.getAttribute("cert");
        assertNotNull(cert);
        assertTrue(cert.isCourseCertificate());
        assertEquals("UTE-CERT-CS101-101", cert.getCertCode());
        assertEquals("CS101", cert.getCourseCode());
        assertEquals("Lập trình Web chuyên sâu", cert.getCourseName());
        assertEquals(new BigDecimal("9.50"), cert.getFinalGrade());
        assertEquals("A+", cert.getGradeClassification());
    }

    @Test
    void testSubmitCourseReview_ForCourseCertificate_Success() {
        Course course = Course.builder().id(1L).code("CS101").title("Lập trình Web chuyên sâu").build();
        vn.edu.ute.utelearn.entity.Enrollment sampleEnrollment = vn.edu.ute.utelearn.entity.Enrollment.builder()
                .id(99L)
                .user(sampleUser)
                .course(course)
                .isCompleted(true)
                .averageGrade(new BigDecimal("9.50"))
                .rating(null)
                .certificateUrl("/certificates/verify?code=UTE-CERT-CS101-101")
                .completedAt(Instant.now())
                .build();

        when(cohortMemberRepository.findByCertificateCode("UTE-CERT-CS101-101")).thenReturn(Collections.emptyList());
        when(enrollmentRepository.findByCertificateCode("UTE-CERT-CS101-101")).thenReturn(List.of(sampleEnrollment));
        when(enrollmentRepository.save(any(vn.edu.ute.utelearn.entity.Enrollment.class))).thenReturn(sampleEnrollment);

        vn.edu.ute.utelearn.dto.CourseReviewRequestDTO req = vn.edu.ute.utelearn.dto.CourseReviewRequestDTO.builder()
                .courseId(1L)
                .certCode("UTE-CERT-CS101-101")
                .rating(5)
                .comment("Khóa học rất chất lượng")
                .build();

        vn.edu.ute.utelearn.service.AuthService mockAuth = org.mockito.Mockito.mock(vn.edu.ute.utelearn.service.AuthService.class);
        when(mockAuth.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));
        org.springframework.test.util.ReflectionTestUtils.setField(certificateController, "authService", mockAuth);

        ResponseEntity<?> response = certificateController.submitReview(req);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(true, body.get("success"));
    }
}
