package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class CohortRequestDTO {

    @Size(max = 50, message = "Mã module tối đa 50 ký tự")
    private String code;

    @NotBlank(message = "Cohort name is required")
    private String name;

    private Long courseId;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime enrollmentStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime enrollmentEnd;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime studyStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime studyEnd;

    private Integer maxCapacity = 50;

    private BigDecimal price = BigDecimal.ZERO;

    private String status = "UPCOMING";
    private String visibility = "PUBLIC";

    private String description;

    private String objectives;

    private String announcementTitle;

    private String announcementContent;

    private String thumbnailUrl;

    private String gradingPolicy = "PROGRESS_BASED";
    private Integer processWeight = 50;
    private Integer finalWeight = 50;
    private Long finalLessonId;
    private BigDecimal minPassingGrade = new BigDecimal("5.00");
    private BigDecimal minFinalExamScore = new BigDecimal("3.00");

    private java.util.List<CohortScheduleRequestDTO> schedules = new java.util.ArrayList<>();
}
