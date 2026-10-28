package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dto.CertificateViewDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.CertificateService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CohortMemberRepository cohortMemberRepository;
    private final vn.edu.ute.utelearn.dao.EnrollmentRepository enrollmentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Override
    @Transactional(readOnly = true)
    public Optional<CertificateViewDTO> getCertificateByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String searchCode = extractCleanCode(code);
        log.info("[CertificateService] Tra cứu chứng chỉ với mã: {}", searchCode);

        List<CohortMember> members = cohortMemberRepository.findByCertificateCode(searchCode);
        if (members.isEmpty()) {
            if (enrollmentRepository != null) {
                List<vn.edu.ute.utelearn.entity.Enrollment> enrollments = enrollmentRepository.findByCertificateCode(searchCode);
                if (!enrollments.isEmpty()) {
                    vn.edu.ute.utelearn.entity.Enrollment enrollment = enrollments.get(0);
                    Course course = enrollment.getCourse();
                    User student = enrollment.getUser();

                    boolean isPassed = Boolean.TRUE.equals(enrollment.getIsCompleted());
                    boolean isPublished = true;
                    boolean hasReviewed = enrollment.hasReviewed();
                    boolean isValid = isPassed && hasReviewed;
                    boolean requiresReview = isPassed && !hasReviewed;

                    String rawCertUrl = enrollment.getCertificateUrl();
                    String displayCertCode = extractCleanCode(rawCertUrl != null ? rawCertUrl : searchCode);

                    boolean hasScore = enrollment.getAverageGrade() != null && enrollment.getAverageGrade().compareTo(BigDecimal.ZERO) > 0;
                    String templateType = hasScore ? "WITH_SCORE" : "WITHOUT_SCORE";
                    String templateImageUrl = hasScore
                            ? "/images/certificates/UTELearn_Cert_WithScore.png"
                            : "/images/certificates/UTELearn_Cert_WithoutScore.png";

                    String classification = calculateClassification(hasScore, enrollment.getAverageGrade());
                    String issuedDate = formatIssuedDate(enrollment.getCompletedAt(), enrollment.getEnrolledAt());

                    String courseDisplayName = course != null && course.getTitle() != null ? course.getTitle() : "Khóa học UTELearn";
                    String courseCode = course != null && course.getCode() != null ? course.getCode() : "UTE-CRS";
                    String instructorName = (course != null && course.getCreatedBy() != null && course.getCreatedBy().getFullName() != null)
                            ? course.getCreatedBy().getFullName() : "Ban Đào tạo UTELearn";
                    String verifyUrl = "/certificates/verify?code=" + displayCertCode;

                    CertificateViewDTO dto = CertificateViewDTO.builder()
                            .certCode(displayCertCode)
                            .studentName(student != null && student.getFullName() != null ? student.getFullName() : "Học viên UTELearn")
                            .studentEmail(student != null ? student.getEmail() : "")
                            .courseName(courseDisplayName)
                            .courseCode(courseCode)
                            .cohortCode(courseCode)
                            .cohortName("Trọn gói khóa học")
                            .finalGrade(enrollment.getAverageGrade())
                            .gradeClassification(classification)
                            .hasScore(hasScore)
                            .issuedDateFormatted(issuedDate)
                            .templateType(templateType)
                            .templateImageUrl(templateImageUrl)
                            .isPublished(isPublished)
                            .isValid(isValid)
                            .verifyUrl(verifyUrl)
                            .instructorName(instructorName)
                            .cohortId(null)
                            .courseId(course != null ? course.getId() : null)
                            .isCourseCertificate(true)
                            .hasReviewed(hasReviewed)
                            .requiresReview(requiresReview)
                            .rating(enrollment.getRating())
                            .reviewComment(enrollment.getReviewComment())
                            .reviewedDateFormatted(enrollment.getReviewedAt() != null ? DATE_FORMATTER.format(enrollment.getReviewedAt()) : null)
                            .build();

                    log.info("[CertificateService] Tra cứu thành công chứng chỉ khóa học: {}, hợp lệ: {}, đã đánh giá: {}", displayCertCode, isValid, hasReviewed);
                    return Optional.of(dto);
                }
            }

            log.warn("[CertificateService] Không tìm thấy chứng chỉ cho mã: {}", searchCode);
            return Optional.empty();
        }

        CohortMember member = members.get(0);
        Cohort cohort = member.getCohort();
        User student = member.getUser();
        Course course = (cohort != null) ? cohort.getCourse() : null;

        boolean isPassed = Boolean.TRUE.equals(member.getIsPassed());
        boolean isPublished = (cohort != null && Boolean.TRUE.equals(cohort.getIsGradesPublished()));
        boolean hasReviewed = member.hasReviewed();
        boolean isValid = isPassed && isPublished && hasReviewed;
        boolean requiresReview = isPassed && isPublished && !hasReviewed;

        String rawCertUrl = member.getCertificateUrl();
        String displayCertCode = extractCleanCode(rawCertUrl != null ? rawCertUrl : searchCode);

        String policy = (cohort != null && cohort.getGradingPolicy() != null)
                ? cohort.getGradingPolicy()
                : "PROGRESS_BASED";

        boolean hasScore = !"PROGRESS_BASED".equalsIgnoreCase(policy) && member.getFinalGrade() != null;
        String templateType = hasScore ? "WITH_SCORE" : "WITHOUT_SCORE";
        String templateImageUrl = hasScore
                ? "/images/certificates/UTELearn_Cert_WithScore.png"
                : "/images/certificates/UTELearn_Cert_WithoutScore.png";

        String classification = calculateClassification(hasScore, member.getFinalGrade());
        String issuedDate = formatIssuedDate(member.getGradedAt(), member.getEnrolledAt());

        String courseDisplayName = resolveCourseDisplayName(cohort, course);
        String courseCode = resolveCourseCode(cohort, course);
        String instructorName = resolveInstructorName(cohort, course);
        String verifyUrl = "/certificates/verify?code=" + displayCertCode;

        CertificateViewDTO dto = CertificateViewDTO.builder()
                .certCode(displayCertCode)
                .studentName(student != null && student.getFullName() != null ? student.getFullName() : "Học viên UTELearn")
                .studentEmail(student != null ? student.getEmail() : "")
                .courseName(courseDisplayName)
                .courseCode(courseCode)
                .cohortCode(cohort != null ? cohort.getCode() : "")
                .cohortName(cohort != null ? cohort.getName() : "")
                .finalGrade(member.getFinalGrade())
                .gradeClassification(classification)
                .hasScore(hasScore)
                .issuedDateFormatted(issuedDate)
                .templateType(templateType)
                .templateImageUrl(templateImageUrl)
                .isPublished(isPublished)
                .isValid(isValid)
                .verifyUrl(verifyUrl)
                .instructorName(instructorName)
                .cohortId(cohort != null ? cohort.getId() : null)
                .hasReviewed(hasReviewed)
                .requiresReview(requiresReview)
                .rating(member.getRating())
                .reviewComment(member.getReviewComment())
                .reviewedDateFormatted(member.getReviewedAt() != null ? DATE_FORMATTER.format(member.getReviewedAt()) : null)
                .build();

        log.info("[CertificateService] Tra cứu thành công chứng chỉ: {}, hợp lệ: {}, đã đánh giá: {}", displayCertCode, isValid, hasReviewed);
        return Optional.of(dto);
    }

    @Override
    @Transactional
    public CertificateViewDTO submitCourseReview(Long userId, Long cohortId, String certCode, int rating, String comment) {
        return submitCourseReview(userId, cohortId, null, certCode, rating, comment);
    }

    @Override
    @Transactional
    public CertificateViewDTO submitCourseReview(Long userId, Long cohortId, Long courseId, String certCode, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Số sao đánh giá phải từ 1 đến 5 sao");
        }

        CohortMember member = null;
        if (cohortId != null && userId != null) {
            member = cohortMemberRepository.findByCohort_IdAndUser_Id(cohortId, userId).orElse(null);
        }

        if (member == null && certCode != null && !certCode.isBlank()) {
            String clean = extractCleanCode(certCode);
            List<CohortMember> list = cohortMemberRepository.findByCertificateCode(clean);
            if (!list.isEmpty()) {
                CohortMember found = list.get(0);
                if (userId == null || (found.getUser() != null && found.getUser().getId().equals(userId))) {
                    member = found;
                }
            }
        }

        if (member == null && cohortId != null) {
            List<CohortMember> members = cohortMemberRepository.findByCohort_Id(cohortId);
            if (userId != null) {
                member = members.stream()
                        .filter(m -> m.getUser() != null && m.getUser().getId().equals(userId))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (member != null) {
            if (!Boolean.TRUE.equals(member.getIsPassed())) {
                throw new IllegalStateException("Học viên chưa hoàn thành đạt yêu cầu môn học để đánh giá nhận chứng chỉ");
            }

            member.setRating(rating);
            if (comment != null && !comment.isBlank()) {
                member.setReviewComment(comment.trim());
            }
            member.setReviewedAt(Instant.now());

            if (member.getCertificateUrl() == null || member.getCertificateUrl().isBlank()) {
                Cohort c = member.getCohort();
                String code = "UTE-CERT-" + (c != null && c.getCode() != null ? c.getCode() : "MOD") + "-" + member.getUser().getId();
                member.setCertificateUrl("/certificates/verify?code=" + code);
            }

            cohortMemberRepository.save(member);
            log.info("[CertificateService] Học viên {} đã đánh giá {} sao cho đợt học {}", 
                    member.getUser() != null ? member.getUser().getId() : null, rating, member.getCohort() != null ? member.getCohort().getId() : null);

            String cleanCode = extractCleanCode(member.getCertificateUrl());
            return getCertificateByCode(cleanCode).orElseThrow(() -> new RuntimeException("Lỗi truy xuất chứng chỉ sau khi đánh giá"));
        }

        if (enrollmentRepository != null) {
            vn.edu.ute.utelearn.entity.Enrollment enrollment = null;
            if (courseId != null && userId != null) {
                enrollment = enrollmentRepository.findByUser_IdAndCourse_Id(userId, courseId).orElse(null);
            }
            if (enrollment == null && certCode != null && !certCode.isBlank()) {
                String clean = extractCleanCode(certCode);
                List<vn.edu.ute.utelearn.entity.Enrollment> list = enrollmentRepository.findByCertificateCode(clean);
                if (!list.isEmpty()) {
                    vn.edu.ute.utelearn.entity.Enrollment found = list.get(0);
                    if (userId == null || (found.getUser() != null && found.getUser().getId().equals(userId))) {
                        enrollment = found;
                    }
                }
            }

            if (enrollment != null) {
                if (!Boolean.TRUE.equals(enrollment.getIsCompleted())) {
                    throw new IllegalStateException("Học viên chưa hoàn thành đạt yêu cầu tất cả các module của khóa học để đánh giá nhận chứng chỉ");
                }

                enrollment.setRating(rating);
                if (comment != null && !comment.isBlank()) {
                    enrollment.setReviewComment(comment.trim());
                }
                enrollment.setReviewedAt(Instant.now());

                if (enrollment.getCertificateUrl() == null || enrollment.getCertificateUrl().isBlank()) {
                    Course c = enrollment.getCourse();
                    String code = "UTE-CERT-" + (c != null && c.getCode() != null ? c.getCode() : "CRS") + "-" + enrollment.getUser().getId();
                    enrollment.setCertificateUrl("/certificates/verify?code=" + code);
                }

                enrollmentRepository.save(enrollment);
                log.info("[CertificateService] Học viên {} đã đánh giá {} sao cho khóa học {}", 
                        enrollment.getUser() != null ? enrollment.getUser().getId() : null, rating, enrollment.getCourse() != null ? enrollment.getCourse().getId() : null);

                String cleanCode = extractCleanCode(enrollment.getCertificateUrl());
                return getCertificateByCode(cleanCode).orElseThrow(() -> new RuntimeException("Lỗi truy xuất chứng chỉ khóa học sau khi đánh giá"));
            }
        }

        throw new IllegalArgumentException("Không tìm thấy thông tin ghi danh học viên cho đợt học, khóa học hoặc mã chứng chỉ này");
    }

    private String extractCleanCode(String input) {
        if (input == null) {
            return "";
        }
        String clean = input.trim();
        int queryIdx = clean.indexOf("code=");
        if (queryIdx >= 0) {
            clean = clean.substring(queryIdx + 5);
        }
        int ampIdx = clean.indexOf("&");
        if (ampIdx >= 0) {
            clean = clean.substring(0, ampIdx);
        }
        return clean.trim();
    }

    public String calculateClassification(boolean hasScore, BigDecimal grade) {
        if (!hasScore || grade == null) {
            return "HOÀN THÀNH";
        }
        if (grade.compareTo(BigDecimal.valueOf(9.0)) >= 0) {
            return "A+";
        }
        if (grade.compareTo(BigDecimal.valueOf(8.5)) >= 0) {
            return "A";
        }
        if (grade.compareTo(BigDecimal.valueOf(8.0)) >= 0) {
            return "B+";
        }
        if (grade.compareTo(BigDecimal.valueOf(7.0)) >= 0) {
            return "B";
        }
        if (grade.compareTo(BigDecimal.valueOf(6.5)) >= 0) {
            return "C+";
        }
        if (grade.compareTo(BigDecimal.valueOf(5.5)) >= 0) {
            return "C";
        }
        if (grade.compareTo(BigDecimal.valueOf(5.0)) >= 0) {
            return "D+";
        }
        if (grade.compareTo(BigDecimal.valueOf(4.0)) >= 0) {
            return "D";
        }
        return "F";
    }

    private String formatIssuedDate(Instant gradedAt, Instant enrolledAt) {
        Instant dateToUse = (gradedAt != null) ? gradedAt : (enrolledAt != null ? enrolledAt : Instant.now());
        return DATE_FORMATTER.format(dateToUse);
    }

    private String resolveCourseDisplayName(Cohort cohort, Course course) {
        if (cohort != null && cohort.getName() != null && !cohort.getName().isBlank()) {
            return cohort.getName();
        }
        if (course != null && course.getTitle() != null && !course.getTitle().isBlank()) {
            return course.getTitle();
        }
        return "Khóa học UTELearn";
    }

    private String resolveCourseCode(Cohort cohort, Course course) {
        if (cohort != null && cohort.getCode() != null && !cohort.getCode().isBlank()) {
            return cohort.getCode();
        }
        if (course != null && course.getCode() != null && !course.getCode().isBlank()) {
            return course.getCode();
        }
        return "UTE-CS";
    }

    private String resolveInstructorName(Cohort cohort, Course course) {
        if (cohort != null && cohort.getCreatedBy() != null && cohort.getCreatedBy().getFullName() != null) {
            return cohort.getCreatedBy().getFullName();
        }
        if (course != null && course.getCreatedBy() != null && course.getCreatedBy().getFullName() != null) {
            return course.getCreatedBy().getFullName();
        }
        return "Ban Đào tạo UTELearn";
    }
}
