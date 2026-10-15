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
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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

    @Column(length = 30)
    @Builder.Default
    private String visibility = "PUBLIC";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String objectives;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Transient
    public String getEffectiveThumbnailUrl() {
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            return thumbnailUrl;
        }
        try {
            if (course != null && org.hibernate.Hibernate.isInitialized(course)) {
                String courseThumb = course.getThumbnailUrl();
                if (courseThumb != null && !courseThumb.isBlank()) {
                    return courseThumb;
                }
            }
        } catch (Exception ignored) {
        }
        return "/images/courses/default-cover.png";
    }

    @Transient
    public java.util.List<String> getParsedObjectives() {
        if (objectives == null || objectives.isBlank() || objectives.equals("[]")) {
            return java.util.Collections.emptyList();
        }
        try {
            if (objectives.trim().startsWith("[")) {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                        objectives,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {}
                );
            } else {
                return java.util.Arrays.stream(objectives.split("\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
        } catch (Exception e) {
            return java.util.Arrays.stream(objectives.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    @Transient
    public String getFormattedEnrollmentStart() {
        if (enrollmentStart == null) return "Chưa thiết lập";
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(enrollmentStart);
    }

    @Transient
    public String getFormattedEnrollmentEnd() {
        if (enrollmentEnd == null) return "Chưa thiết lập";
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(enrollmentEnd);
    }

    @Transient
    public String getFormattedStudyStart() {
        if (studyStart == null) return "Chưa thiết lập";
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(studyStart);
    }

    @Transient
    public String getFormattedStudyEnd() {
        if (studyEnd == null) return "Chưa thiết lập";
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(studyEnd);
    }
}
