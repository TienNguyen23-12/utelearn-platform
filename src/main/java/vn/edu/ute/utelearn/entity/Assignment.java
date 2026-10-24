package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true)
    private Lesson lesson;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instruction;

    @Column(name = "attachment_url", columnDefinition = "TEXT")
    private String attachmentUrl;

    @Column(name = "max_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal maxScore = new BigDecimal("10.0");

    @Column(name = "passing_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal passingScore = new BigDecimal("5.0");

    @Column(name = "allow_file_submission")
    @Builder.Default
    private Boolean allowFileSubmission = true;

    @Column(name = "allow_text_submission")
    @Builder.Default
    private Boolean allowTextSubmission = true;

    @Column(name = "allowed_file_types", length = 255)
    @Builder.Default
    private String allowedFileTypes = "pdf,doc,docx,zip,rar,txt";

    @Column(name = "max_file_size_mb")
    @Builder.Default
    private Integer maxFileSizeMb = 20;

    @Column(name = "allow_late_submission")
    @Builder.Default
    private Boolean allowLateSubmission = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
