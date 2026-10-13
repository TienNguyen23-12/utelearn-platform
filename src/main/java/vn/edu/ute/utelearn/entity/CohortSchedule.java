package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "cohort_schedules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cohort_id", "lesson_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CohortSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "unlock_at", nullable = false)
    private Instant unlockAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "show_score_type", length = 30)
    @Builder.Default
    private String showScoreType = "IMMEDIATELY";

    @Column(name = "is_score_published")
    @Builder.Default
    private Boolean isScorePublished = true;

    @Column(name = "allow_review_answers")
    @Builder.Default
    private Boolean allowReviewAnswers = false;

    @Column(name = "review_available_at")
    private Instant reviewAvailableAt;
}
