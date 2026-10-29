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
        if (isTablePresent("notifications")) {
            try {
                jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check");
            } catch (Exception ignored) {}
        }

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
                try {
                    if (isTablePresent("cohort_members")) {
                        jdbcTemplate.execute("ALTER TABLE cohort_members ADD COLUMN IF NOT EXISTS rating INT");
                        jdbcTemplate.execute("ALTER TABLE cohort_members ADD COLUMN IF NOT EXISTS review_comment TEXT");
                        jdbcTemplate.execute("ALTER TABLE cohort_members ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ");
                        jdbcTemplate.execute("INSERT INTO cohort_members (cohort_id, user_id, enrolled_at, role_in_cohort) " +
                                "SELECT real_c.id, cm.user_id, cm.enrolled_at, cm.role_in_cohort " +
                                "FROM cohort_members cm " +
                                "JOIN cohorts dummy_c ON cm.cohort_id = dummy_c.id " +
                                "JOIN cohorts real_c ON real_c.course_id = dummy_c.course_id AND real_c.id <> dummy_c.id AND real_c.code NOT LIKE '%-ALL' " +
                                "WHERE (dummy_c.code LIKE '%-ALL' OR dummy_c.name ILIKE 'Toàn bộ khóa học%') " +
                                "ON CONFLICT DO NOTHING");
                        jdbcTemplate.execute("DELETE FROM cohort_members WHERE cohort_id IN (SELECT id FROM cohorts WHERE (code LIKE '%-ALL' OR name ILIKE 'Toàn bộ khóa học%') AND course_id IN (SELECT DISTINCT c2.course_id FROM cohorts c2 WHERE c2.code NOT LIKE '%-ALL' AND (c2.name IS NULL OR c2.name NOT ILIKE 'Toàn bộ khóa học%') AND c2.course_id IS NOT NULL))");
                    }
                    if (isTablePresent("cohort_schedules")) {
                        jdbcTemplate.execute("DELETE FROM cohort_schedules WHERE cohort_id IN (SELECT id FROM cohorts WHERE (code LIKE '%-ALL' OR name ILIKE 'Toàn bộ khóa học%') AND course_id IN (SELECT DISTINCT c2.course_id FROM cohorts c2 WHERE c2.code NOT LIKE '%-ALL' AND (c2.name IS NULL OR c2.name NOT ILIKE 'Toàn bộ khóa học%') AND c2.course_id IS NOT NULL))");
                    }
                    jdbcTemplate.execute("DELETE FROM cohorts WHERE (code LIKE '%-ALL' OR name ILIKE 'Toàn bộ khóa học%') AND course_id IN (SELECT DISTINCT c2.course_id FROM cohorts c2 WHERE c2.code NOT LIKE '%-ALL' AND (c2.name IS NULL OR c2.name NOT ILIKE 'Toàn bộ khóa học%') AND c2.course_id IS NOT NULL)");
                } catch (Exception ignored) {}
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
            jdbcTemplate.update("UPDATE courses SET status = 'PUBLISHED' WHERE slug = 'luyen-thi-thuat-toan-oj' AND status = 'DRAFT'");
            jdbcTemplate.update("UPDATE cohorts SET moderation_status = 'DRAFT' WHERE code = 'MOD-2026-09' AND moderation_status = 'APPROVED'");
            if (isTablePresent("quizzes")) {
                jdbcTemplate.update("UPDATE quizzes SET duration_minutes = 15 WHERE duration_minutes = 45");
            }

            if (isTablePresent("lessons")) {
                initAssignmentTables();
            }

            initCartAndWishlistTables();
            seedSampleQuizzes();

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

    private void seedSampleQuizzes() {
        if (!isTablePresent("quizzes") || !isTablePresent("questions") || !isTablePresent("lessons")) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS allow_review BOOLEAN DEFAULT true");
            jdbcTemplate.execute("UPDATE quizzes SET allow_review = true WHERE allow_review IS NULL");
            jdbcTemplate.execute("ALTER TABLE questions ADD COLUMN IF NOT EXISTS order_index INT DEFAULT 1");
            jdbcTemplate.execute("UPDATE questions SET order_index = 1 WHERE order_index IS NULL");
        } catch (Exception ignored) {}

        Integer quizCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quizzes", Integer.class);
        if (quizCount != null && quizCount > 0) {
            return;
        }

        List<Map<String, Object>> sections = jdbcTemplate.queryForList(
                "SELECT s.id, s.cohort_id FROM sections s WHERE s.cohort_id IS NOT NULL ORDER BY s.id ASC LIMIT 1");
        if (sections.isEmpty()) {
            return;
        }

        Long sectionId = ((Number) sections.get(0).get("id")).longValue();
        Long cohortId = ((Number) sections.get(0).get("cohort_id")).longValue();

        jdbcTemplate.update(
                "INSERT INTO lessons (section_id, title, lesson_type, order_index, is_free_preview, is_public_for_cohorts, status) " +
                "VALUES (?, ?, 'QUIZ', 99, false, true, 'PUBLISHED')",
                sectionId, "Bài kiểm tra Đánh giá Năng lực OOP & Java Core");

        Long lessonId = jdbcTemplate.queryForObject(
                "SELECT id FROM lessons WHERE section_id = ? AND lesson_type = 'QUIZ' ORDER BY id DESC LIMIT 1",
                Long.class, sectionId);

        if (lessonId == null) {
            return;
        }

        if (isTablePresent("cohort_schedules")) {
            jdbcTemplate.update(
                    "INSERT INTO cohort_schedules (cohort_id, lesson_id, unlock_at, show_score_type, is_score_published, allow_review_answers) " +
                    "VALUES (?, ?, CURRENT_TIMESTAMP, 'IMMEDIATELY', true, true) ON CONFLICT DO NOTHING",
                    cohortId, lessonId);
        }

        jdbcTemplate.update(
                "INSERT INTO quizzes (lesson_id, title, duration_minutes, passing_score, max_attempts, shuffle_questions) " +
                "VALUES (?, ?, 15, 5.0, 3, true)",
                lessonId, "Bài kiểm tra Đánh giá Năng lực OOP & Java Core");

        Long quizId = jdbcTemplate.queryForObject("SELECT id FROM quizzes WHERE lesson_id = ?", Long.class, lessonId);
        if (quizId == null) {
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO questions (quiz_id, content, question_type, points, explanation, options) VALUES (?, ?, ?, ?, ?, ?::jsonb)",
                quizId,
                "Trong ngôn ngữ Java, từ khóa nào sau đây được sử dụng để một lớp kế thừa từ một lớp cha?",
                "SINGLE_CHOICE",
                2.5,
                "Trong Java, từ khóa 'extends' được dùng để kế thừa một class, còn 'implements' dùng để triển khai interface.",
                "[{\"id\":\"opt_1\",\"text\":\"extends\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"implements\",\"isCorrect\":false},{\"id\":\"opt_3\",\"text\":\"inherits\",\"isCorrect\":false},{\"id\":\"opt_4\",\"text\":\"super\",\"isCorrect\":false}]");

        jdbcTemplate.update(
                "INSERT INTO questions (quiz_id, content, question_type, points, explanation, options) VALUES (?, ?, ?, ?, ?, ?::jsonb)",
                quizId,
                "Những tính chất nào sau đây là các trụ cột cốt lõi của Lập trình Hướng đối tượng (OOP)?",
                "MULTI_SELECT",
                2.5,
                "4 tính chất cốt lõi của OOP gồm: Đóng gói (Encapsulation), Kế thừa (Inheritance), Đa hình (Polymorphism) và Trừu tượng (Abstraction).",
                "[{\"id\":\"opt_1\",\"text\":\"Đóng gói (Encapsulation)\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"Kế thừa (Inheritance)\",\"isCorrect\":true},{\"id\":\"opt_3\",\"text\":\"Đa hình (Polymorphism)\",\"isCorrect\":true},{\"id\":\"opt_4\",\"text\":\"Đồng quy đa luồng (Concurrency)\",\"isCorrect\":false}]");

        jdbcTemplate.update(
                "INSERT INTO questions (quiz_id, content, question_type, points, explanation, options) VALUES (?, ?, ?, ?, ?, ?::jsonb)",
                quizId,
                "Trong Java, một lớp (class) có thể cùng lúc kế thừa trực tiếp từ nhiều lớp cha khác nhau (Multiple Class Inheritance).",
                "TRUE_FALSE",
                2.5,
                "Java không hỗ trợ đa kế thừa trực tiếp giữa các class để tránh lỗi nhập nhằng Diamond Problem.",
                "[{\"id\":\"opt_1\",\"text\":\"Đúng\",\"isCorrect\":false},{\"id\":\"opt_2\",\"text\":\"Sai\",\"isCorrect\":true}]");

        jdbcTemplate.update(
                "INSERT INTO questions (quiz_id, content, question_type, points, explanation, options) VALUES (?, ?, ?, ?, ?, ?::jsonb)",
                quizId,
                "Môi trường thực thi của Java chịu trách nhiệm nạp bytecode và chuyển đổi thành mã máy được gọi tắt là gì? (3 chữ cái)",
                "FILL_BLANK",
                2.5,
                "JVM viết tắt của Java Virtual Machine (Máy ảo Java).",
                "[{\"id\":\"opt_1\",\"text\":\"JVM\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"Java Virtual Machine\",\"isCorrect\":true}]");

        log.info("[DataSeeder] Đã tạo thành công bài kiểm tra mẫu OOP & Java Core gồm 4 dạng câu hỏi.");
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
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM roles", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        log.info("[DataSeeder] Nạp danh sách Vai trò (Roles)...");
        String sql = "INSERT INTO roles (code, name, description) VALUES (?, ?, ?) ON CONFLICT (code) DO NOTHING";

        List<Object[]> roles = List.of(
            new Object[]{"ADMIN", "Quản trị viên toàn hệ thống", "Toàn quyền cấu hình và quản trị nền tảng UTELearn"},
            new Object[]{"MODERATOR", "Người kiểm duyệt nội dung", "Nhận nhiệm vụ từ hàng đợi để duyệt bài giảng, đề thi và OJ"},
            new Object[]{"INSTRUCTOR", "Giảng viên", "Soạn giáo trình, quản lý kho tài nguyên số, mở lớp theo đợt và chấm bài"},
            new Object[]{"TA", "Trợ giảng", "Hỗ trợ giải đáp lớp học qua WebSocket và trợ giảng"},
            new Object[]{"STUDENT", "Học viên", "Đăng ký lớp học theo đợt, làm bài tập và luyện code Online Judge"},
            new Object[]{"FINANCE", "Kế toán / Quản lý thanh toán", "Theo dõi dòng tiền, đối soát giao dịch và duyệt thanh toán đơn hàng"}
        );

        for (Object[] role : roles) {
            jdbcTemplate.update(sql, role);
        }
    }

    private void seedPermissions() {
        log.info("[DataSeeder] Náº¡p vÃ  cáº­p nháº­t danh sÃ¡ch Quyá»n háº¡n (Permissions)...");
        String sql = "INSERT INTO permissions (code, name, module) VALUES (?, ?, ?) ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, module = EXCLUDED.module";

        List<Object[]> permissions = List.of(
            new Object[]{"USER_MANAGE", "Quản lý tài khoản người dùng", "AUTH"},
            new Object[]{"ROLE_MANAGE", "Cấu hình phân quyền động", "AUTH"},
            new Object[]{"COURSE_CREATE", "Tạo và chỉnh sửa khóa học chuẩn", "COURSE"},
            new Object[]{"CONTENT_APPROVE", "Phê duyệt nội dung bài giảng và đề thi", "MODERATION"},
            new Object[]{"ASSET_MANAGE", "Quản lý kho tài nguyên số cá nhân", "ASSET"},
            new Object[]{"ORDER_MANAGE", "Quản lý và đối soát đơn hàng", "PAYMENT"}
        );

        for (Object[] permission : permissions) {
            jdbcTemplate.update(sql, permission);
        }

        jdbcTemplate.update("DELETE FROM role_permissions WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'CODE_SUBMIT')");
        jdbcTemplate.update("DELETE FROM permissions WHERE code = 'CODE_SUBMIT'");
    }

    private void assignPermissionsToRoles() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM role_permissions", Integer.class);
        if (count != null && count > 0) {
            return;
        }
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
            "WHERE r.code = 'FINANCE' AND p.code = 'ORDER_MANAGE' " +
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
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM users", Integer.class);
        if (count != null && count > 0) {
            return;
        }
        log.info("[DataSeeder] Nạp tài khoản Admin và tài khoản mẫu...");
        String defaultRawPassword = properties.getSeeder().getDefaultPassword();
        if (defaultRawPassword == null || defaultRawPassword.isBlank()) {
            defaultRawPassword = "Abc@1234";
        }
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
                "username", "finance",
                "email", "finance@utelearn.edu.vn",
                "fullName", "Kế toán viên UTELearn",
                "phone", "0905678901",
                "role", "FINANCE"
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


        log.info("[DataSeeder] Đã đồng bộ mật khẩu mặc định '{}' cho các tài khoản mẫu trong hệ thống.", defaultRawPassword);
    }

    private void seedCategories() {
        if (!isTablePresent("categories")) {
            return;
        }

        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM categories", Integer.class);
        if (count != null && count > 0) {
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

    private void initAssignmentTables() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS assignments (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "lesson_id BIGINT UNIQUE NOT NULL REFERENCES lessons(id) ON DELETE CASCADE, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "instruction TEXT, " +
                    "attachment_url TEXT, " +
                    "max_score NUMERIC(5, 2) DEFAULT 10.0, " +
                    "passing_score NUMERIC(5, 2) DEFAULT 5.0, " +
                    "allow_file_submission BOOLEAN DEFAULT TRUE, " +
                    "allow_text_submission BOOLEAN DEFAULT TRUE, " +
                    "allowed_file_types VARCHAR(255) DEFAULT 'pdf,doc,docx,zip,rar,txt', " +
                    "max_file_size_mb INT DEFAULT 20, " +
                    "created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS assignment_submissions (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "assignment_id BIGINT NOT NULL REFERENCES assignments(id) ON DELETE CASCADE, " +
                    "user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "cohort_id BIGINT REFERENCES cohorts(id) ON DELETE CASCADE, " +
                    "submission_text TEXT, " +
                    "file_url TEXT, " +
                    "file_name VARCHAR(255), " +
                    "file_size_bytes BIGINT, " +
                    "status VARCHAR(30) DEFAULT 'SUBMITTED', " +
                    "score NUMERIC(5, 2), " +
                    "feedback TEXT, " +
                    "graded_by BIGINT REFERENCES users(id) ON DELETE SET NULL, " +
                    "graded_at TIMESTAMPTZ, " +
                    "submitted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, " +
                    "created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT uq_assignment_user_cohort UNIQUE (assignment_id, user_id, cohort_id))");

            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_assignment_submissions_user ON assignment_submissions(cohort_id, user_id, assignment_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_assignment_submissions_assignment ON assignment_submissions(assignment_id)");
            log.info("[DataSeeder] Đã kiểm tra và khởi tạo bảng assignments và assignment_submissions thành công.");
        } catch (Exception e) {
            log.warn("[DataSeeder] Không thể khởi tạo bảng assignment: {}", e.getMessage());
        }
    }

    private void initCartAndWishlistTables() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS cart_items (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "item_type VARCHAR(20) NOT NULL, " +
                    "course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE, " +
                    "cohort_id BIGINT REFERENCES cohorts(id) ON DELETE CASCADE, " +
                    "created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP)");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_user_course ON cart_items(user_id, course_id) WHERE course_id IS NOT NULL");
            jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_user_cohort ON cart_items(user_id, cohort_id) WHERE cohort_id IS NOT NULL");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_cart_items_user ON cart_items(user_id)");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS wishlists (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE, " +
                    "created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT uq_wishlist_user_course UNIQUE (user_id, course_id))");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_wishlists_user ON wishlists(user_id)");
            log.info("[DataSeeder] Đã kiểm tra và khởi tạo bảng cart_items và wishlists thành công.");
        } catch (Exception e) {
            log.warn("[DataSeeder] Không thể khởi tạo bảng cart_items hoặc wishlists: {}", e.getMessage());
        }
    }
}
