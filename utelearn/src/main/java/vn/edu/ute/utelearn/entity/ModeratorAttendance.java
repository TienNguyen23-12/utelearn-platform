package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "moderator_attendances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"moderator_id", "work_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeratorAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "max_daily_minutes")
    @Builder.Default
    private Integer maxDailyMinutes = 480;

    @Column(name = "assigned_tasks_count")
    @Builder.Default
    private Integer assignedTasksCount = 0;

    @Column(name = "completed_tasks_count")
    @Builder.Default
    private Integer completedTasksCount = 0;

    @Column(name = "active_tasks_count")
    @Builder.Default
    private Integer activeTasksCount = 0;

    @Column(name = "total_workload_minutes")
    @Builder.Default
    private Integer totalWorkloadMinutes = 0;

    @CreationTimestamp
    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;
}
