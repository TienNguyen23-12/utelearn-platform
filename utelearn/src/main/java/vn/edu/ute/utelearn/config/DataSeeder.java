package vn.edu.ute.utelearn.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * UTELearn DataSeeder:
 * Automatically loads initial data (Roles, Permissions, Dynamic RBAC mapping, Admin & Sample Users, Categories)
 * using Java code instead of manual INSERT commands from the database.
 * The Idempotent mechanism (ON CONFLICT DO NOTHING) ensures absolute safety when starting multiple times.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final UtelearnProperties properties;

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.getSeeder().isEnabled()) {
            log.info("[DataSeeder] Đã tắt tính năng tự động nạp dữ liệu (utelearn.seeder.enabled=false).");
            return;
        }

        if (!isTablePresent("roles") || !isTablePresent("users")) {
            log.warn("[DataSeeder] Bảng 'roles' hoặc 'users' chưa được tạo trong PostgreSQL. DataSeeder tạm thời bỏ qua.");
            return;
        }

        log.info("[DataSeeder] Bắt đầu quá trình nạp dữ liệu khởi tạo cho UTELearn...");

        try {
            seedRoles();
            seedPermissions();
            assignPermissionsToRoles();

            if (properties.getSeeder().isSeedSampleUsers()) {
                seedUsers();
            }

            seedCategories();

            log.info("[DataSeeder] Quá trình nạp dữ liệu mẫu hoàn tất thành công!");
        } catch (Exception e) {
            log.error("[DataSeeder] Có lỗi xảy ra trong quá trình nạp dữ liệu mẫu: {}", e.getMessage(), e);
        }
    }

    private boolean isTablePresent(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class,
                tableName
            );
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.warn("[DataSeeder] Không thể kiểm tra sự tồn tại của bảng {}: {}", tableName, e.getMessage());
            return false;
        }
    }

    private void seedRoles() {
        log.info("[DataSeeder] Nạp danh sách Vai trò (Roles)...");
        String sql = "INSERT INTO roles (code, name, description) VALUES (?, ?, ?) ON CONFLICT (code) DO NOTHING";

        List<Object[]> roles = List.of(
            new Object[]{"ADMIN", "Quản trị viên toàn hệ thống", "Toàn quyền cấu hình và quản trị nền tảng UTELearn"},
            new Object[]{"MODERATOR", "Người kiểm duyệt nội dung", "Nhận nhiệm vụ từ hàng đợi để duyệt bài giảng, đề thi và OJ"},
            new Object[]{"INSTRUCTOR", "Giảng viên", "Soạn giáo trình, quản lý kho tài nguyên số, mở lớp theo đợt và chấm bài"},
            new Object[]{"TA", "Trợ giảng", "Hỗ trợ giải đáp lớp học qua WebSocket và trợ giảng"},
            new Object[]{"STUDENT", "Học viên", "Đăng ký lớp học theo đợt, làm bài tập và luyện code Online Judge"}
        );

        for (Object[] role : roles) {
            jdbcTemplate.update(sql, role);
        }
    }

    private void seedPermissions() {
        log.info("[DataSeeder] Nạp danh sách Quyền hạn (Permissions)...");
        String sql = "INSERT INTO permissions (code, name, module) VALUES (?, ?, ?) ON CONFLICT (code) DO NOTHING";

        List<Object[]> permissions = List.of(
            new Object[]{"USER_MANAGE", "Quản lý tài khoản người dùng", "AUTH"},
            new Object[]{"ROLE_MANAGE", "Cấu hình phân quyền động", "AUTH"},
            new Object[]{"COURSE_CREATE", "Tạo và chỉnh sửa khóa học chuẩn", "COURSE"},
            new Object[]{"CONTENT_APPROVE", "Phê duyệt nội dung bài giảng và đề thi", "MODERATION"},
            new Object[]{"ASSET_MANAGE", "Quản lý kho tài nguyên số cá nhân", "ASSET"},
            new Object[]{"CODE_SUBMIT", "Nộp bài chấm Online Judge", "OJ"}
        );

        for (Object[] permission : permissions) {
            jdbcTemplate.update(sql, permission);
        }
    }

    private void assignPermissionsToRoles() {
        log.info("[DataSeeder] Ánh xạ Quyền hạn cho các Vai trò (Role-Permission Mapping)...");

        jdbcTemplate.update(
            "INSERT INTO role_permissions (role_id, permission_id) " +
            "SELECT r.id, p.id FROM roles r, permissions p " +
            "WHERE r.code = 'ADMIN' " +
            "ON CONFLICT DO NOTHING"
        );

        jdbcTemplate.update(
            "INSERT INTO role_permissions (role_id, permission_id) " +
            "SELECT r.id, p.id FROM roles r, permissions p " +
            "WHERE r.code = 'MODERATOR' AND p.code = 'CONTENT_APPROVE' " +
            "ON CONFLICT DO NOTHING"
        );

        jdbcTemplate.update(
            "INSERT INTO role_permissions (role_id, permission_id) " +
            "SELECT r.id, p.id FROM roles r, permissions p " +
            "WHERE r.code = 'INSTRUCTOR' AND p.code IN ('COURSE_CREATE', 'ASSET_MANAGE') " +
            "ON CONFLICT DO NOTHING"
        );

        jdbcTemplate.update(
            "INSERT INTO role_permissions (role_id, permission_id) " +
            "SELECT r.id, p.id FROM roles r, permissions p " +
            "WHERE r.code = 'STUDENT' AND p.code = 'CODE_SUBMIT' " +
            "ON CONFLICT DO NOTHING"
        );
    }

    private void seedUsers() {
        log.info("[DataSeeder] Nạp tài khoản Admin và tài khoản mẫu...");
        String defaultRawPassword = properties.getSeeder().getDefaultPassword();
        String defaultHashedPassword = passwordEncoder.encode(defaultRawPassword);

        List<Map<String, String>> sampleUsers = List.of(
            Map.of(
                "username", "admin",
                "email", "admin@utelearn.edu.vn",
                "fullName", "Quản trị viên hệ thống",
                "phone", "0901234567",
                "role", "ADMIN"
            ),
            Map.of(
                "username", "instructor",
                "email", "instructor@utelearn.edu.vn",
                "fullName", "Giảng viên Khoa CNTT",
                "phone", "0902345678",
                "role", "INSTRUCTOR"
            ),
            Map.of(
                "username", "moderator",
                "email", "moderator@utelearn.edu.vn",
                "fullName", "Người kiểm duyệt nội dung",
                "phone", "0903456789",
                "role", "MODERATOR"
            ),
            Map.of(
                "username", "student",
                "email", "student@utelearn.edu.vn",
                "fullName", "Học viên UTELearn",
                "phone", "0904567890",
                "role", "STUDENT"
            )
        );

        for (Map<String, String> user : sampleUsers) {
            String username = user.get("username");
            String email = user.get("email");
            String fullName = user.get("fullName");
            String phone = user.get("phone");
            String roleCode = user.get("role");

            jdbcTemplate.update(
                "INSERT INTO users (username, email, password_hash, full_name, phone_number, is_active) " +
                "VALUES (?, ?, ?, ?, ?, true) " +
                "ON CONFLICT (username) DO NOTHING",
                username, email, defaultHashedPassword, fullName, phone
            );

            jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) " +
                "SELECT u.id, r.id FROM users u, roles r " +
                "WHERE u.username = ? AND r.code = ? " +
                "ON CONFLICT DO NOTHING",
                username, roleCode
            );
        }

        log.info("[DataSeeder] Mật khẩu đăng nhập mặc định cho các tài khoản mẫu là: '{}'", defaultRawPassword);
    }

    private void seedCategories() {
        if (!isTablePresent("categories")) {
            return;
        }

        log.info("[DataSeeder] Nạp danh mục khóa học mẫu...");
        String sql = "INSERT INTO categories (name, slug) VALUES (?, ?) ON CONFLICT (slug) DO NOTHING";

        List<Object[]> categories = List.of(
            new Object[]{"Lập trình Web & Backend", "lap-trinh-web-backend"},
            new Object[]{"Khoa học Dữ liệu & AI", "khoa-hoc-du-lieu-ai"},
            new Object[]{"Luyện thi Thuật toán & OJ", "luyen-thi-thuat-toan-oj"}
        );

        for (Object[] cat : categories) {
            jdbcTemplate.update(sql, cat);
        }
    }
}
