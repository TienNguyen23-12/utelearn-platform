package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "quiz_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @CreationTimestamp
    @Column(name = "started_at")
    private Instant startedAt;

    @CreationTimestamp
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "is_passed")
    @Builder.Default
    private Boolean isPassed = false;

    @Column(length = 30)
    @Builder.Default
    private String status = "SUBMITTED";

    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String answers = "[]";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
