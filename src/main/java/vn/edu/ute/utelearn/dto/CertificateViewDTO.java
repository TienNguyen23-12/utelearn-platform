package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateViewDTO {
    private String certCode;
    private String studentName;
    private String studentEmail;
    private String courseName;
    private String courseCode;
    private String cohortCode;
    private String cohortName;
    private BigDecimal finalGrade;
    private String gradeClassification;
    private boolean hasScore;
    private String issuedDateFormatted;
    private String templateType;
    private String templateImageUrl;
    private boolean isPublished;
    private boolean isValid;
    private String verifyUrl;
    private String instructorName;
    private Long cohortId;
    private Long courseId;
    private boolean isCourseCertificate;
    private boolean hasReviewed;
    private boolean requiresReview;
    private Integer rating;
    private String reviewComment;
    private String reviewedDateFormatted;
}
