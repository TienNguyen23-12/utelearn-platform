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
public class AssignmentDTO {
    private Long id;
    private Long lessonId;
    private String lessonStatus;
    private String title;
    private String instruction;
    private String attachmentUrl;
    private BigDecimal maxScore;
    private BigDecimal passingScore;
    private Boolean allowFileSubmission;
    private Boolean allowTextSubmission;
    private String allowedFileTypes;
    private Integer maxFileSizeMb;
    private Long totalSubmissions;
    private Long gradedSubmissions;
    private AssignmentSubmissionDTO currentSubmission;
    private String unlockAt;
    private String deadlineAt;
    private String deadlineAtDisplay;
    private Boolean isLocked;
    private Boolean isPassed;
    private Boolean isReviewer;
    private Boolean allowLateSubmission;
    private Boolean isPastDeadline;
    private Boolean requiresGrading;
}
