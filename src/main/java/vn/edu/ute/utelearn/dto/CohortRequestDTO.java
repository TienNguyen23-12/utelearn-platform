package vn.edu.ute.utelearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class CohortRequestDTO {

    private String code;

    @NotBlank(message = "Cohort name is required")
    private String name;

    @NotNull(message = "Course ID is required")
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

    private java.util.List<CohortScheduleRequestDTO> schedules = new java.util.ArrayList<>();
}
