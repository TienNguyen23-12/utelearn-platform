package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cohorts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cohort {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "enrollment_start", nullable = false)
    private Instant enrollmentStart;

    @Column(name = "enrollment_end", nullable = false)
    private Instant enrollmentEnd;

    @Column(name = "study_start", nullable = false)
    private Instant studyStart;

    @Column(name = "study_end", nullable = false)
    private Instant studyEnd;

    @Column(name = "max_capacity")
    @Builder.Default
    private Integer maxCapacity = 50;

    @Column(name = "current_enrolled")
    @Builder.Default
    private Integer currentEnrolled = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(length = 30)
    @Builder.Default
    private String status = "UPCOMING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
