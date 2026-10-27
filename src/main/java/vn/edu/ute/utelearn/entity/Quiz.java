package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true)
    private Lesson lesson;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 15;

    @Column(name = "passing_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal passingScore = new BigDecimal("5.0");

    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 1;

    @Column(name = "shuffle_questions")
    @Builder.Default
    private Boolean shuffleQuestions = true;

    @Column(name = "allow_review")
    @Builder.Default
    private Boolean allowReview = true;

    public static final String DEFAULT_EXAM_RULES = "Đồng hồ đếm ngược sẽ bắt đầu chạy ngay khi bạn nhấn nút \"Bắt đầu làm bài thi\".\n"
            + "Khi hết thời gian quy định, hệ thống sẽ tự động thu và chấm bài thi của bạn.\n"
            + "Đạt từ 5.0 điểm trở lên để được hệ thống tự động ghi nhận hoàn thành bài học.\n"
            + "Thời hạn nộp bài: Áp dụng theo thời hạn quy định của đợt học.\n"
            + "Xem lại bài làm: Được phép xem lại chi tiết câu hỏi & đáp án sau khi nộp bài.\n"
            + "Tránh tải lại trang (F5) hoặc thoát khỏi trình duyệt trong suốt quá trình làm bài.";

    @Column(name = "allow_late_submission")
    @Builder.Default
    private Boolean allowLateSubmission = true;

    @Column(name = "exam_rules", columnDefinition = "TEXT")
    @Builder.Default
    private String examRules = DEFAULT_EXAM_RULES;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
