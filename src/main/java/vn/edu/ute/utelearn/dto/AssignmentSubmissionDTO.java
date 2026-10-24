package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionDTO {
    private Long id;
    private Long assignmentId;
    private Long userId;
    private String studentName;
    private String studentEmail;
    private String studentAvatar;
    private Long cohortId;
    private String submissionText;
    private String fileUrl;
    private String fileName;
    private Long fileSizeBytes;
    private String status;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String feedback;
    private String gradedByName;
    private Instant gradedAt;
    private Instant submittedAt;
    private Boolean isPassed;
    private Boolean isLate;
    private Boolean requiresGrading;
}
