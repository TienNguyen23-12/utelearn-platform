package vn.edu.ute.utelearn.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class CohortScheduleRequestDTO {
    private Long lessonId;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime unlockAt;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime deadlineAt;
    
    private String showScoreType = "IMMEDIATELY";
    private Boolean allowReviewAnswers = false;
}
