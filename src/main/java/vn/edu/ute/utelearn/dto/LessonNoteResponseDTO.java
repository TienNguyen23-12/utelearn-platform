package vn.edu.ute.utelearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonNoteResponseDTO {
    private Long id;
    private Long lessonId;
    private Long cohortId;
    private Integer timestampSeconds;
    private String formattedTime;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
}
