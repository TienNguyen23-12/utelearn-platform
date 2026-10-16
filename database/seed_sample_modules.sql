-- ==============================================================================
-- DỮ LIỆU MẪU HỆ THỐNG UTELEARN (CHUẨN HÓA MÔ HÌNH COURSE & MODULE)
-- Course: Khóa học lớn / Lộ trình trọn gói (Full Course)
-- Cohort: Module / Chuyên đề / Chặng học con độc lập (Có thể mua lẻ từng phần)
-- ==============================================================================

BEGIN;

-- 1. KHÓA HỌC 1: LẬP TRÌNH WEB CƠ BẢN ĐẾN CHUYÊN SÂU
INSERT INTO courses (
    code, title, slug, headline, description, thumbnail_url, level, status, price,
    category_id, created_by, objectives, requirements, created_at, updated_at
) VALUES (
    'CS-WEB-01',
    'Lập trình Web Cơ bản đến Chuyên sâu (HTML/CSS, JS, Node.js)',
    'lap-trinh-web-co-ban',
    'Lộ trình toàn diện từ số 0 trở thành Web Developer với HTML5, CSS3, JavaScript ES6+ và Backend Node.js Express.',
    'Khóa học được thiết kế theo cấu trúc module hóa linh hoạt. Học viên có thể đăng ký trọn bộ khóa học để nhận ưu đãi hoặc lựa chọn mua lẻ từng module theo nhu cầu bổ trợ kiến thức. Khóa học bao gồm lý thuyết thực chiến, bài tập dự án và chứng chỉ số sau khi hoàn thành.',
    'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800&auto=format&fit=crop&q=80',
    'BEGINNER',
    'PUBLISHED',
    699000.00,
    (SELECT id FROM categories WHERE slug = 'lap-trinh-web-backend' LIMIT 1),
    (SELECT id FROM users WHERE username = 'instructor' LIMIT 1),
    '["Thành thạo xây dựng giao diện chuẩn responsive với HTML5 và CSS3", "Làm chủ JavaScript hiện đại, lập trình bất đồng bộ và tương tác DOM", "Xây dựng hệ thống RESTful API hoàn chỉnh với Node.js, Express và PostgreSQL", "Hiểu sâu kiến trúc Web Client - Server và cơ chế xác thực JWT"]'::jsonb,
    '["Máy tính cá nhân có kết nối Internet", "Không yêu cầu kiến thức lập trình trước đó - đào tạo từ đầu"]'::jsonb,
    NOW(),
    NOW()
) ON CONFLICT (slug) DO UPDATE SET 
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    price = EXCLUDED.price,
    thumbnail_url = EXCLUDED.thumbnail_url;

-- CÁC MODULE (COHORTS) THUỘC KHÓA HỌC 1
INSERT INTO cohorts (
    course_id, code, name, enrollment_start, enrollment_end, study_start, study_end,
    max_capacity, current_enrolled, price, status, created_at
) VALUES 
(
    (SELECT id FROM courses WHERE slug = 'lap-trinh-web-co-ban'),
    'MOD-WEB-01',
    'Nền tảng Web với HTML5, CSS3 & Responsive Design',
    NOW() - INTERVAL '10 days',
    NOW() + INTERVAL '60 days',
    NOW() - INTERVAL '5 days',
    NOW() + INTERVAL '120 days',
    50,
    0,
    250000.00,
    'ENROLLING',
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'lap-trinh-web-co-ban'),
    'MOD-WEB-02',
    'Lập trình JavaScript Hiện đại & DOM Manipulation',
    NOW() - INTERVAL '10 days',
    NOW() + INTERVAL '60 days',
    NOW() - INTERVAL '5 days',
    NOW() + INTERVAL '120 days',
    50,
    0,
    300000.00,
    'ENROLLING',
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'lap-trinh-web-co-ban'),
    'MOD-WEB-03',
    'Lập trình Backend RESTful API với Node.js & Express',
    NOW() - INTERVAL '10 days',
    NOW() + INTERVAL '60 days',
    NOW() - INTERVAL '5 days',
    NOW() + INTERVAL '120 days',
    50,
    0,
    350000.00,
    'ENROLLING',
    NOW()
)
ON CONFLICT (code) DO UPDATE SET 
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    status = EXCLUDED.status;

-- CÁC CHƯƠNG (SECTIONS) CỦA KHÓA HỌC 1
INSERT INTO sections (course_id, title, order_index, created_at) VALUES
(
    (SELECT id FROM courses WHERE slug = 'lap-trinh-web-co-ban'),
    'Chương 1: Nhập môn Giao diện Web với HTML5 & CSS3',
    1,
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'lap-trinh-web-co-ban'),
    'Chương 2: Lập trình JavaScript Cốt lõi & Xử lý Sự kiện',
    2,
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'lap-trinh-web-co-ban'),
    'Chương 3: Xây dựng RESTful API Backend với Node.js',
    3,
    NOW()
)
ON CONFLICT DO NOTHING;

-- BÀI HỌC (LESSONS) CHO KHÓA HỌC 1
INSERT INTO lessons (
    section_id, title, lesson_type, order_index, is_free_preview, 
    video_url, duration_seconds, document_content, created_at
) VALUES
-- Chương 1
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'lap-trinh-web-co-ban' AND s.order_index = 1),
    '1.1 Kiến trúc Web và Cấu trúc chuẩn của một trang HTML5',
    'VIDEO',
    1,
    TRUE,
    'https://www.youtube.com/watch?v=kUMe1FH4CHE',
    900,
    'Bài học giới thiệu mô hình Client - Server, vai trò của HTML/CSS/JS và cách cấu trúc một file index.html chuẩn semantic.',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'lap-trinh-web-co-ban' AND s.order_index = 1),
    '1.2 Bố cục linh hoạt với CSS Flexbox và Responsive Media Query',
    'VIDEO',
    2,
    FALSE,
    'https://www.youtube.com/watch?v=JJSoEo8JSnc',
    1200,
    'Nắm vững trục chính (main axis), trục phụ (cross axis), justify-content, align-items và tạo giao diện co giãn trên di động.',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'lap-trinh-web-co-ban' AND s.order_index = 1),
    '1.3 Tài liệu & Thực hành thiết kế Landing Page hoàn chỉnh',
    'DOCUMENT',
    3,
    FALSE,
    NULL,
    0,
    'Hướng dẫn từng bước thực hành dự án Landing Page giới thiệu sản phẩm. Yêu cầu tải source code mẫu và hoàn thiện theo tiêu chí bài tập.',
    NOW()
),
-- Chương 2
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'lap-trinh-web-co-ban' AND s.order_index = 2),
    '2.1 Cú pháp JavaScript Hiện đại: ES6+, Arrow Function & Destructuring',
    'VIDEO',
    1,
    FALSE,
    'https://www.youtube.com/watch?v=W6NZfCO5SIk',
    1100,
    'Tìm hiểu let/const, template literals, destructuring assignment, spread/rest operator và các tính năng quan trọng trong ES6.',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'lap-trinh-web-co-ban' AND s.order_index = 2),
    '2.2 Tương tác DOM, Bắt sự kiện và Xử lý Form động',
    'VIDEO',
    2,
    FALSE,
    'https://www.youtube.com/watch?v=0ik6X4DJKCc',
    1300,
    'Thao tác với document.querySelector, addEventListener, submit form và validate dữ liệu đầu vào ngay trên trình duyệt.',
    NOW()
),
-- Chương 3
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'lap-trinh-web-co-ban' AND s.order_index = 3),
    '3.1 Khởi tạo Node.js Project & Thiết kế RESTful API với Express.js',
    'VIDEO',
    1,
    FALSE,
    'https://www.youtube.com/watch?v=f2EqECiTBL8',
    1500,
    'Tạo server HTTP với Express, cấu hình middleware, routing các phương thức GET, POST, PUT, DELETE chuẩn REST convention.',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'lap-trinh-web-co-ban' AND s.order_index = 3),
    '3.2 Kết nối PostgreSQL và Bảo mật Xác thực qua JSON Web Token (JWT)',
    'VIDEO',
    2,
    FALSE,
    'https://www.youtube.com/watch?v=mbsmsi7l3r4',
    1800,
    'Truy vấn dữ liệu với pool connection, mã hóa mật khẩu bcrypt, phát hành token JWT khi đăng nhập và phân quyền truy cập endpoint.',
    NOW()
)
ON CONFLICT DO NOTHING;


-- 2. KHÓA HỌC 2: CHUYÊN GIA LẬP TRÌNH JAVA & SPRING BOOT DOANH NGHIỆP
INSERT INTO courses (
    code, title, slug, headline, description, thumbnail_url, level, status, price,
    category_id, created_by, objectives, requirements, created_at, updated_at
) VALUES (
    'CS-JAVA-01',
    'Kiến Trúc Java Backend & Spring Boot Doanh Nghiệp',
    'java-spring-boot-doanh-nghiep',
    'Xây dựng các hệ thống Enterprise quy mô lớn với Java 21, Spring Boot 3, Spring Data JPA, Security và Microservices.',
    'Lộ trình đào tạo kỹ sư backend chuyên nghiệp theo tiêu chuẩn doanh nghiệp. Khóa học phân chia thành 3 module chuyên đề rõ ràng: Java OOP nâng cao, Xây dựng REST API với JPA, và Tích hợp Bảo mật Spring Security JWT. Học viên có thể đăng ký từng phần để tối ưu chi phí và lộ trình cá nhân.',
    'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop&q=80',
    'INTERMEDIATE',
    'PUBLISHED',
    990000.00,
    (SELECT id FROM categories WHERE slug = 'lap-trinh-web-backend' LIMIT 1),
    (SELECT id FROM users WHERE username = 'instructor' LIMIT 1),
    '["Thành thạo Java Core và 4 tính chất OOP chuyên sâu", "Thiết kế REST API chuẩn kiến trúc 3 lớp với Spring Boot 3", "Tối ưu truy vấn dữ liệu với Spring Data JPA & Hibernate", "Bảo mật hệ thống đa vai trò với Spring Security và JWT"]'::jsonb,
    '["Đã có kiến thức lập trình cơ bản về biến, vòng lặp, hàm", "Cài đặt sẵn JDK 17/21 và IntelliJ IDEA hoặc VS Code"]'::jsonb,
    NOW(),
    NOW()
) ON CONFLICT (slug) DO UPDATE SET 
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    price = EXCLUDED.price,
    thumbnail_url = EXCLUDED.thumbnail_url;

-- CÁC MODULE (COHORTS) THUỘC KHÓA HỌC 2
INSERT INTO cohorts (
    course_id, code, name, enrollment_start, enrollment_end, study_start, study_end,
    max_capacity, current_enrolled, price, status, created_at
) VALUES 
(
    (SELECT id FROM courses WHERE slug = 'java-spring-boot-doanh-nghiep'),
    'MOD-JAVA-01',
    'Java Core & Lập trình Hướng đối tượng OOP Chuyên sâu',
    NOW() - INTERVAL '15 days',
    NOW() + INTERVAL '45 days',
    NOW() - INTERVAL '5 days',
    NOW() + INTERVAL '120 days',
    60,
    0,
    290000.00,
    'ENROLLING',
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'java-spring-boot-doanh-nghiep'),
    'MOD-JAVA-02',
    'Xây dựng REST API Doanh nghiệp với Spring Boot & JPA',
    NOW() - INTERVAL '15 days',
    NOW() + INTERVAL '45 days',
    NOW() - INTERVAL '5 days',
    NOW() + INTERVAL '120 days',
    60,
    0,
    450000.00,
    'ENROLLING',
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'java-spring-boot-doanh-nghiep'),
    'MOD-JAVA-03',
    'Bảo mật Hệ thống với Spring Security & JWT',
    NOW() - INTERVAL '15 days',
    NOW() + INTERVAL '45 days',
    NOW() - INTERVAL '5 days',
    NOW() + INTERVAL '120 days',
    60,
    0,
    490000.00,
    'ENROLLING',
    NOW()
)
ON CONFLICT (code) DO UPDATE SET 
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    status = EXCLUDED.status;

-- CÁC CHƯƠNG (SECTIONS) CỦA KHÓA HỌC 2
INSERT INTO sections (course_id, title, order_index, created_at) VALUES
(
    (SELECT id FROM courses WHERE slug = 'java-spring-boot-doanh-nghiep'),
    'Chương 1: Java Core, OOP & Collections Framework',
    1,
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'java-spring-boot-doanh-nghiep'),
    'Chương 2: Spring Boot Architecture & Spring Data JPA',
    2,
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'java-spring-boot-doanh-nghiep'),
    'Chương 3: Bảo mật Phân quyền với Spring Security',
    3,
    NOW()
)
ON CONFLICT DO NOTHING;

-- BÀI HỌC (LESSONS) CHO KHÓA HỌC 2
INSERT INTO lessons (
    section_id, title, lesson_type, order_index, is_free_preview, 
    video_url, duration_seconds, document_content, created_at
) VALUES
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'java-spring-boot-doanh-nghiep' AND s.order_index = 1),
    '1.1 4 Trụ cột Lập trình Hướng đối tượng và Design Patterns cơ bản',
    'VIDEO',
    1,
    TRUE,
    'https://www.youtube.com/watch?v=eIrMbAQSU34',
    1200,
    'Phân tích Encapsulation, Inheritance, Polymorphism, Abstraction và cách vận dụng Factory, Singleton pattern trong Java.',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'java-spring-boot-doanh-nghiep' AND s.order_index = 1),
    '1.2 Làm chủ Java Collections, Generics và Stream API',
    'VIDEO',
    2,
    FALSE,
    'https://www.youtube.com/watch?v=1opIas94CJM',
    1400,
    'Tối ưu hóa thao tác dữ liệu với List, Set, Map, lambda expressions và chuỗi xử lý Stream filter, map, collect.',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'java-spring-boot-doanh-nghiep' AND s.order_index = 2),
    '2.1 Dependency Injection & Kiến trúc đa tầng (Controller - Service - Repository)',
    'VIDEO',
    1,
    FALSE,
    'https://www.youtube.com/watch?v=9SGDpanrc8U',
    1600,
    'Cơ chế Inversion of Control (IoC), cấu hình Component, Service, Autowired và xử lý ngoại lệ toàn cục @RestControllerAdvice.',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'java-spring-boot-doanh-nghiep' AND s.order_index = 2),
    '2.2 Tương tác Cơ sở dữ liệu với Spring Data JPA & Transaction Management',
    'VIDEO',
    2,
    FALSE,
    'https://www.youtube.com/watch?v=8SGI_XS5OPw',
    1800,
    'Định nghĩa Entity, quan hệ OneToMany, ManyToMany, viết câu lệnh JPQL và quản lý tính toàn vẹn giao dịch @Transactional.',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'java-spring-boot-doanh-nghiep' AND s.order_index = 3),
    '3.1 Cơ chế Filter Chain và Cấu hình Spring Security 6',
    'VIDEO',
    1,
    FALSE,
    'https://www.youtube.com/watch?v=her_7pa0vrg',
    1900,
    'Kiến trúc SecurityFilterChain, xử lý AuthenticationProvider, UserDetailsService và bảo vệ các route nhạy cảm.',
    NOW()
)
ON CONFLICT DO NOTHING;


-- 3. KHÓA HỌC 3: LUYỆN THI THUẬT TOÁN & CẤU TRÚC DỮ LIỆU ONLINE JUDGE
INSERT INTO courses (
    code, title, slug, headline, description, thumbnail_url, level, status, price,
    category_id, created_by, objectives, requirements, created_at, updated_at
) VALUES (
    'CS-OJ-01',
    'Luyện Thi Thuật Toán & Cấu Trúc Dữ Liệu Online Judge',
    'luyen-thi-thuat-toan-oj',
    'Rèn luyện tư duy giải thuật đỉnh cao, chinh phục các bài toán khó trên hệ thống Online Judge và phỏng vấn Big Tech.',
    'Khóa học tập trung giải quyết các bài toán lập trình thi đấu và phỏng vấn kỹ thuật. Tách biệt thành 2 module: Giải thuật nền tảng (Two Pointers, Binary Search, Đệ quy) và Cấu trúc dữ liệu nâng cao (Tree, Graph, Dynamic Programming).',
    'https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&auto=format&fit=crop&q=80',
    'ADVANCED',
    'PUBLISHED',
    450000.00,
    (SELECT id FROM categories WHERE slug = 'luyen-thi-thuat-toan-oj' LIMIT 1),
    (SELECT id FROM users WHERE username = 'instructor' LIMIT 1),
    '["Làm chủ kỹ thuật phân tích độ phức tạp thời gian & không gian Big O", "Thuần thục các kỹ thuật giải thuật Two Pointers, Binary Search, Prefix Sum", "Chinh phục các bài toán đồ thị BFS, DFS và quy hoạch động đỉnh cao"]'::jsonb,
    '["Nắm vững một ngôn ngữ lập trình (C++, Java hoặc Python)", "Có tinh thần kiên trì thử thách bản thân với các bài toán khó"]'::jsonb,
    NOW(),
    NOW()
) ON CONFLICT (slug) DO UPDATE SET 
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    price = EXCLUDED.price,
    thumbnail_url = EXCLUDED.thumbnail_url;

-- CÁC MODULE (COHORTS) THUỘC KHÓA HỌC 3
INSERT INTO cohorts (
    course_id, code, name, enrollment_start, enrollment_end, study_start, study_end,
    max_capacity, current_enrolled, price, status, created_at
) VALUES 
(
    (SELECT id FROM courses WHERE slug = 'luyen-thi-thuat-toan-oj'),
    'MOD-OJ-01',
    'Giải thuật Cơ bản, Hai con trỏ & Đệ quy Quay lui',
    NOW() - INTERVAL '20 days',
    NOW() + INTERVAL '40 days',
    NOW() - INTERVAL '5 days',
    NOW() + INTERVAL '90 days',
    50,
    0,
    199000.00,
    'ENROLLING',
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'luyen-thi-thuat-toan-oj'),
    'MOD-OJ-02',
    'Cấu trúc Dữ liệu Nâng cao & Quy hoạch Động',
    NOW() - INTERVAL '20 days',
    NOW() + INTERVAL '40 days',
    NOW() - INTERVAL '5 days',
    NOW() + INTERVAL '90 days',
    50,
    0,
    349000.00,
    'ENROLLING',
    NOW()
)
ON CONFLICT (code) DO UPDATE SET 
    name = EXCLUDED.name,
    price = EXCLUDED.price,
    status = EXCLUDED.status;

-- CÁC CHƯƠNG (SECTIONS) CỦA KHÓA HỌC 3
INSERT INTO sections (course_id, title, order_index, created_at) VALUES
(
    (SELECT id FROM courses WHERE slug = 'luyen-thi-thuat-toan-oj'),
    'Chương 1: Phân tích Độ phức tạp & Kỹ thuật Hai con trỏ',
    1,
    NOW()
),
(
    (SELECT id FROM courses WHERE slug = 'luyen-thi-thuat-toan-oj'),
    'Chương 2: Cây, Đồ thị và Quy hoạch Động',
    2,
    NOW()
)
ON CONFLICT DO NOTHING;

-- BÀI HỌC (LESSONS) CHO KHÓA HỌC 3
INSERT INTO lessons (
    section_id, title, lesson_type, order_index, is_free_preview, 
    video_url, duration_seconds, document_content, created_at
) VALUES
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'luyen-thi-thuat-toan-oj' AND s.order_index = 1),
    '1.1 Độ phức tạp thuật toán Big O & Tư duy tối ưu không gian bộ nhớ',
    'DOCUMENT',
    1,
    TRUE,
    NULL,
    0,
    'Tài liệu phân tích cận trên Big O, cận dưới Big Omega, và cách đánh giá số phép toán dựa trên giới hạn testcase của các bài toán Online Judge.',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'luyen-thi-thuat-toan-oj' AND s.order_index = 1),
    '1.2 Kỹ thuật Hai con trỏ (Two Pointers) & Cửa sổ trượt (Sliding Window)',
    'VIDEO',
    2,
    FALSE,
    'https://www.youtube.com/watch?v=On03HWe2tZM',
    1500,
    'Áp dụng Two Pointers giải quyết bài toán mảng sắp xếp, tìm cặp số tổng bằng K trong độ phức tạp tuyến tính O(N).',
    NOW()
),
(
    (SELECT s.id FROM sections s JOIN courses c ON s.course_id = c.id WHERE c.slug = 'luyen-thi-thuat-toan-oj' AND s.order_index = 2),
    '2.1 Duyệt đồ thị theo chiều rộng (BFS) và chiều sâu (DFS)',
    'VIDEO',
    1,
    FALSE,
    'https://www.youtube.com/watch?v=TIbUeeksXcI',
    1700,
    'Cài đặt danh sách kề, ma trận kề, tìm đường đi ngắn nhất trên đồ thị không trọng số và kiểm tra tính liên thông.',
    NOW()
)
ON CONFLICT DO NOTHING;

INSERT INTO cohort_schedules (cohort_id, lesson_id, unlock_at, show_score_type, is_score_published)
SELECT c.id, l.id, NOW() - INTERVAL '5 days', 'IMMEDIATELY', TRUE
FROM cohorts c
JOIN courses cr ON c.course_id = cr.id
JOIN sections s ON s.course_id = cr.id
JOIN lessons l ON l.section_id = s.id
WHERE (c.code = 'MOD-WEB-01' AND cr.slug = 'lap-trinh-web-co-ban' AND s.order_index = 1)
   OR (c.code = 'MOD-WEB-02' AND cr.slug = 'lap-trinh-web-co-ban' AND s.order_index = 2)
   OR (c.code = 'MOD-WEB-03' AND cr.slug = 'lap-trinh-web-co-ban' AND s.order_index = 3)
   OR (c.code = 'MOD-JAVA-01' AND cr.slug = 'java-spring-boot-doanh-nghiep' AND s.order_index = 1)
   OR (c.code = 'MOD-JAVA-02' AND cr.slug = 'java-spring-boot-doanh-nghiep' AND s.order_index = 2)
   OR (c.code = 'MOD-JAVA-03' AND cr.slug = 'java-spring-boot-doanh-nghiep' AND s.order_index = 3)
   OR (c.code = 'MOD-OJ-01' AND cr.slug = 'luyen-thi-thuat-toan-oj' AND s.order_index = 1)
   OR (c.code = 'MOD-OJ-02' AND cr.slug = 'luyen-thi-thuat-toan-oj' AND s.order_index = 2)
ON CONFLICT (cohort_id, lesson_id) DO NOTHING;

UPDATE cohorts SET created_by = 2 WHERE created_by IS NULL OR created_by != 2;
UPDATE cohorts SET visibility = 'PUBLIC' WHERE visibility IS NULL;

COMMIT;
