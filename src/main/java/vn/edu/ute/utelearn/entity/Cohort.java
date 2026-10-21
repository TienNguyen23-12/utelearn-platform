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

    @Column(name = "moderation_status", length = 30)
    @Builder.Default
    private String moderationStatus = "DRAFT";

    @Column(length = 30)
    @Builder.Default
    private String visibility = "PUBLIC";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String objectives;

    @Column(name = "announcement_title", length = 255)
    private String announcementTitle;

    @Column(name = "announcement_content", columnDefinition = "TEXT")
    private String announcementContent;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 1;

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
    public String getObjectivesAsLines() {
        java.util.List<String> list = getParsedObjectives();
        return list.isEmpty() ? "" : String.join("\n", list);
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

    @Transient
    public String getEffectiveAnnouncementTitle() {
        if (announcementTitle != null && !announcementTitle.trim().isBlank()) {
            return announcementTitle.trim();
        }
        return "Thông báo chung & Hướng dẫn học tập";
    }

    @Transient
    public String getEffectiveAnnouncementContent() {
        if (announcementContent != null && !announcementContent.trim().isBlank()) {
            return announcementContent.trim();
        }
        return "Khóa học được thiết kế theo chuẩn sư phạm tích cực. Sinh viên vui lòng hoàn thành bài giảng lý thuyết trước khi làm bài tập thực hành và kiểm tra trắc nghiệm.";
    }

    public String calculateDynamicStatus() {
        if ("COMPLETED".equalsIgnoreCase(this.status)) {
            return "COMPLETED";
        }
        Instant now = Instant.now();
        if (studyEnd != null && now.isAfter(studyEnd)) {
            return "COMPLETED";
        }
        if (studyStart != null && !now.isBefore(studyStart)) {
            return "IN_PROGRESS";
        }
        if (enrollmentEnd != null && now.isAfter(enrollmentEnd)) {
            if (studyStart != null && now.isBefore(studyStart)) {
                return "UPCOMING";
            }
            return "COMPLETED";
        }
        if (enrollmentStart != null) {
            if (now.isBefore(enrollmentStart)) {
                return "UPCOMING";
            }
            return "ENROLLING";
        }
        return (this.status != null && !this.status.isBlank()) ? this.status : "UPCOMING";
    }

    @PostLoad
    public void syncDynamicStatus() {
        this.status = calculateDynamicStatus();
    }

    @PrePersist
    @PreUpdate
    public void updateDynamicStatusBeforeSave() {
        this.status = calculateDynamicStatus();
    }

    public String getStatus() {
        return calculateDynamicStatus();
    }
}

