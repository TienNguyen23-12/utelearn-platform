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
            
            if (isTablePresent("cohorts")) {
                jdbcTemplate.execute("ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS description TEXT");
                jdbcTemplate.execute("ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS objectives TEXT");
                jdbcTemplate.execute("ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id) ON DELETE SET NULL");
                jdbcTemplate.execute("ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS visibility VARCHAR(30) DEFAULT 'PUBLIC'");
                jdbcTemplate.execute("ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(30) DEFAULT 'APPROVED'");
                jdbcTemplate.execute("ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS announcement_title VARCHAR(255)");
                jdbcTemplate.execute("ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS announcement_content TEXT");
                jdbcTemplate.execute("ALTER TABLE cohorts ADD COLUMN IF NOT EXISTS order_index INT DEFAULT 1");
                jdbcTemplate.execute("ALTER TABLE cohorts ALTER COLUMN course_id DROP NOT NULL");
                jdbcTemplate.execute("UPDATE cohorts c SET created_by = COALESCE((SELECT co.created_by FROM courses co WHERE co.id = c.course_id), 2) WHERE c.created_by IS NULL");
                jdbcTemplate.execute("UPDATE cohorts SET visibility = 'PUBLIC' WHERE visibility IS NULL");
                jdbcTemplate.execute("UPDATE cohorts SET visibility = 'PRIVATE' WHERE code LIKE '%-ALL' OR name ILIKE 'Toàn bộ khóa học%'");
                jdbcTemplate.execute("UPDATE cohorts SET moderation_status = 'APPROVED' WHERE moderation_status IS NULL");
                jdbcTemplate.execute("UPDATE cohorts SET order_index = 1 WHERE order_index IS NULL");
                jdbcTemplate.execute("UPDATE cohorts SET name = REGEXP_REPLACE(name, '^Module\\s*[0-9]+[:\\.\\-]\\s*', '', 'i') WHERE code IN ('MOD-WEB-01', 'MOD-WEB-02', 'MOD-WEB-03', 'MOD-JAVA-01', 'MOD-JAVA-02', 'MOD-JAVA-03', 'MOD-OJ-01', 'MOD-OJ-02') AND name ~* '^Module\\s*[0-9]+'");
                seedCohortObjectives();
                if (isTablePresent("cohort_members")) {
                    jdbcTemplate.execute("UPDATE cohorts c SET current_enrolled = (SELECT COUNT(*) FROM cohort_members cm WHERE cm.cohort_id = c.id)");
                }
                if (isTablePresent("sections")) {
                    try {
                        jdbcTemplate.execute("ALTER TABLE sections DROP CONSTRAINT IF EXISTS fk2umk8kh0l48nxphb0tneb6yv2");
                        jdbcTemplate.execute("ALTER TABLE sections DROP CONSTRAINT IF EXISTS fk_sections_cohort");
                        jdbcTemplate.execute("ALTER TABLE sections ADD CONSTRAINT fk_sections_cohort FOREIGN KEY (cohort_id) REFERENCES cohorts(id) ON DELETE CASCADE");
                    } catch (Exception ignored) {}
                }
            }

            int fixedCourses = jdbcTemplate.update("UPDATE courses SET status = 'PUBLISHED' WHERE status = 'APPROVED'");
            if (fixedCourses > 0) {
                log.info("[DataSeeder] Đã tự động sửa " + fixedCourses + " khóa học bị kẹt ở trạng thái APPROVED sang PUBLISHED.");
            }

            log.info("[DataSeeder] Quá trình nạp dữ liệu mẫu hoàn tất thành công!");
        } catch (Exception e) {
            log.error("[DataSeeder] Có lỗi xảy ra trong quá trình nạp dữ liệu mẫu: {}", e.getMessage(), e);
        }
    }

    private void seedCohortObjectives() {
        jdbcTemplate.update("UPDATE cohorts SET description = ?, objectives = ? WHERE code = 'MOD-WEB-01' AND (objectives IS NULL OR objectives = '')",
                "Khám phá toàn diện nền tảng xây dựng giao diện Web hiện đại. Thành thạo cấu trúc ngữ nghĩa HTML5, định kiểu nâng cao với CSS3, hệ thống Flexbox, CSS Grid và kỹ thuật thiết kế Responsive thích ứng mọi thiết bị.",
                "[\"Hiểu sâu cấu trúc DOM và các thẻ ngữ nghĩa HTML5 chuẩn SEO\", \"Làm chủ CSS3: Typography, Box Model, Flexbox và Grid Layout\", \"Thiết kế giao diện thích ứng chuẩn Responsive trên Mobile, Tablet và Desktop\", \"Tự tay xây dựng hoàn chỉnh giao diện Landing Page chuyên nghiệp\"]");

        jdbcTemplate.update("UPDATE cohorts SET description = ?, objectives = ? WHERE code = 'MOD-WEB-02' AND (objectives IS NULL OR objectives = '')",
                "Nâng cao tư duy lập trình và kỹ năng tương tác giao diện người dùng với JavaScript chuẩn ES6+. Nắm vững lập trình hướng đối tượng, xử lý bất đồng bộ Promise/Async-Await và tương tác chuyên sâu với DOM API.",
                "[\"Thành thạo cú pháp JavaScript hiện đại ES6+ (Arrow Functions, Destructuring, Spread)\", \"Thao tác DOM, bắt sự kiện và render dữ liệu động linh hoạt\", \"Làm chủ lập trình bất đồng bộ với Promise, Async/Await và Fetch API\", \"Xử lý lưu trữ dữ liệu phía client với LocalStorage và SessionStorage\"]");

        jdbcTemplate.update("UPDATE cohorts SET description = ?, objectives = ? WHERE code = 'MOD-WEB-03' AND (objectives IS NULL OR objectives = '')",
                "Chinh phục kỹ năng lập trình phía Server với hệ sinh thái Node.js và Express Framework. Xây dựng các API RESTful chuẩn mực, kết nối cơ sở dữ liệu PostgreSQL và tích hợp cơ chế xác thực an toàn.",
                "[\"Hiểu rõ kiến trúc Client - Server và chu trình Request - Response\", \"Xây dựng hệ thống RESTful API hoàn chỉnh với Node.js và Express\", \"Tương tác cơ sở dữ liệu quan hệ PostgreSQL thông qua các truy vấn và ORM\", \"Triển khai cơ chế xác thực người dùng an toàn với JWT và Middleware bảo mật\"]");

        jdbcTemplate.update("UPDATE cohorts SET description = ?, objectives = ? WHERE code = 'MOD-JAVA-01' AND (objectives IS NULL OR objectives = '')",
                "Nắm vững các nguyên lý lập trình hướng đối tượng cốt lõi, cú pháp Java hiện đại và các tính năng mới từ Java 17 đến Java 21.",
                "[\"Làm chủ 4 tính chất OOP trong Java\", \"Sử dụng thành thạo Java Collections Framework\", \"Xử lý ngoại lệ, I/O Stream và Java Generics\", \"Lập trình hàm với Lambda Expressions và Stream API\"]");

        jdbcTemplate.update("UPDATE cohorts SET description = ?, objectives = ? WHERE code = 'MOD-JAVA-02' AND (objectives IS NULL OR objectives = '')",
                "Xây dựng ứng dụng doanh nghiệp chuẩn mực với Spring Boot 3, Spring Data JPA, Hibernate và cơ chế Dependency Injection.",
                "[\"Hiểu sâu vòng đời Bean và Spring IoC Container\", \"Thiết kế mô hình dữ liệu quan hệ với JPA và Hibernate\", \"Xây dựng dịch vụ RESTful API hoàn chỉnh\", \"Xử lý Transaction và tối ưu truy vấn Database\"]");

        jdbcTemplate.update("UPDATE cohorts SET description = ?, objectives = ? WHERE code = 'MOD-JAVA-03' AND (objectives IS NULL OR objectives = '')",
                "Triển khai giải pháp phân quyền nâng cao RBAC và bảo mật hệ thống microservice với Spring Security 6 và JWT.",
                "[\"Cấu hình Spring Security Filter Chain chuẩn xác\", \"Tích hợp xác thực Stateless với JSON Web Token (JWT)\", \"Phân quyền phương thức và endpoint động với @PreAuthorize\", \"Triển khai Single Sign-On (SSO) OAuth2 Google\"]");
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
                "ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash",
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
