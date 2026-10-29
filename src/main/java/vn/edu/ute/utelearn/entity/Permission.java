package vn.edu.ute.utelearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String module;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public String getDisplayName() {
        if (code == null) {
            return name != null ? name : "";
        }
        return switch (code) {
            case "USER_MANAGE" -> "Quản lý người dùng";
            case "ROLE_MANAGE" -> "Cấu hình phân quyền";
            case "COURSE_CREATE" -> "Tạo & quản lý khóa học";
            case "CONTENT_APPROVE" -> "Kiểm duyệt nội dung";
            case "ASSET_MANAGE" -> "Quản lý kho tài nguyên";
            case "CODE_SUBMIT" -> "Luyện code Online Judge";
            case "ORDER_MANAGE" -> "Quản lý đơn hàng & doanh thu";
            default -> (name != null && !name.isBlank() && !name.equalsIgnoreCase(code)) ? name : code;
        };
    }
}
