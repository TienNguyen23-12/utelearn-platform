package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cohort_id")
    private Cohort cohort;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public String getName() {
        if ("COURSE".equalsIgnoreCase(itemType) && course != null) {
            return course.getTitle();
        }
        if ("COHORT".equalsIgnoreCase(itemType) && cohort != null) {
            return cohort.getName();
        }
        return "Sản phẩm";
    }

    public BigDecimal getPrice() {
        if ("COURSE".equalsIgnoreCase(itemType) && course != null) {
            return course.getPrice() != null ? course.getPrice() : BigDecimal.ZERO;
        }
        if ("COHORT".equalsIgnoreCase(itemType) && cohort != null) {
            if (cohort.getCourse() != null && cohort.getCourse().getPrice() != null && cohort.getCourse().getPrice().compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return cohort.getPrice() != null ? cohort.getPrice() : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    public String getFormattedPrice() {
        BigDecimal price = getPrice();
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return "Miễn phí";
        }
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(price) + " VNĐ";
    }

    public String getImageUrl() {
        if ("COURSE".equalsIgnoreCase(itemType) && course != null) {
            return course.getThumbnailUrl();
        }
        if ("COHORT".equalsIgnoreCase(itemType) && cohort != null && cohort.getCourse() != null) {
            return cohort.getCourse().getThumbnailUrl();
        }
        return "/images/courses/default-course.jpg";
    }

    public String getSlugOrCode() {
        if ("COURSE".equalsIgnoreCase(itemType) && course != null) {
            return course.getSlug();
        }
        if ("COHORT".equalsIgnoreCase(itemType) && cohort != null) {
            return cohort.getCode();
        }
        return "";
    }
}
