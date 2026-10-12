package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "instructor_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstructorAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private User instructor;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "asset_type", nullable = false, length = 50)
    private String assetType;

    @Column(name = "cloudinary_url", columnDefinition = "TEXT", nullable = false)
    private String cloudinaryUrl;

    @Column(name = "file_size_bytes")
    @Builder.Default
    private Long fileSizeBytes = 0L;

    @Column(name = "folder_path", length = 255)
    @Builder.Default
    private String folderPath = "/";

    @Column(name = "visibility", length = 30)
    @Builder.Default
    private String visibility = "PRIVATE";

    @Column(name = "shared_emails", columnDefinition = "TEXT")
    private String sharedEmails;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
