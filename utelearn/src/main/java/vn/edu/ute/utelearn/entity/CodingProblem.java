package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "coding_problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingProblem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true)
    private Lesson lesson;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "statement_markdown", columnDefinition = "TEXT", nullable = false)
    private String statementMarkdown;

    @Column(name = "allowed_languages", length = 255)
    @Builder.Default
    private String allowedLanguages = "JAVA,PYTHON,CPP";

    @Column(name = "time_limit_ms")
    @Builder.Default
    private Integer timeLimitMs = 2000;

    @Column(name = "memory_limit_mb")
    @Builder.Default
    private Integer memoryLimitMb = 256;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @Column(name = "show_hidden_test_details")
    @Builder.Default
    private Boolean showHiddenTestDetails = false;

    @Column(name = "test_cases", columnDefinition = "jsonb")
    @Builder.Default
    private String testCases = "[]";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
