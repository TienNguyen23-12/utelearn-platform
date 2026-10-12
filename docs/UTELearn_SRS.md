# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)
# UTELearn: NỀN TẢNG KINH DOANH KHÓA HỌC TRỰC TUYẾN VÀ QUẢN LÝ HỌC TẬP SỐ THEO MÔ HÌNH COURSE - COHORT

---

## 1. KIẾN TRÚC & CÔNG NGHỆ SỬ DỤNG (ENTERPRISE TECH STACK)
- **Backend Stack:** Spring Boot 4.x (Java 21 LTS), Spring Security 6 (Stateless JWT HTTP-Only Cookie + Google OAuth2 Client), Spring Data JPA / Hibernate, Spring WebSocket (STOMP over SockJS), Spring Mail (SMTP), Spring Validation & OpenAPI (Swagger UI).
- **Frontend Stack:** Thymeleaf (Server-Side Rendering), Bootstrap 5.3 (Responsive Enterprise UI), JavaScript (ES6+ Fetch API / XMLHttpRequest), Plyr.io (Trình phát Video đa luồng), Microsoft Office Live & PDF Embedded Viewer, Toastify JS.
- **Database:** PostgreSQL (14+) – Tối ưu hóa kết hợp **Relational Schema + JSONB & GIN Indexes**, chuẩn hóa thời gian thực tế với `TIMESTAMPTZ`, đảm bảo tính toàn vẹn giao dịch (ACID).
- **Hạ tầng Triển khai & CI/CD (DevOps):** Docker & Docker Compose (Multi-stage build cho Dev & Production), GitHub Actions (Automated CI Quality Gate & Test Pipeline).
- **Dịch vụ tích hợp chuẩn doanh nghiệp (3rd Party Enterprise Integrations):**
  - **Cloud Media & CDN:** Cloudinary API (Direct Signed Upload sử dụng chữ ký mật mã SHA-1 từ trình duyệt cho Video, PDF, Slide, ZIP; bảo vệ quyền tải tài liệu qua Backend Security Proxy).
  - **Cổng thanh toán điện tử (Payment Gateway):** Tích hợp cổng thanh toán trực tuyến (VNPay / MoMo / VietQR Webhook) xử lý giao dịch tự động theo chuẩn IPN (Instant Payment Notification).
  - **Định danh Liên kết (Federated Identity):** Google OAuth 2.0 / OpenID Connect.
- **Định hướng phát triển (Future Development Scope – Phase 2):**
  - **Online Judge (OJ) Microservice Sandbox:** Tích hợp Judge0 Engine / Docker Isolated Sandbox và trình soạn thảo Monaco/CodeMirror để biên dịch, chấm bài tập lập trình tự động.
- **Trạng thái dự án:** In Progress – Active Development.

---

## Chapter 01: Tổng quan về đề tài

### 1.1 Giới thiệu chung
**Dự án:** UTELearn – Nền tảng Kinh doanh khóa học trực tuyến và Quản lý học tập số theo mô hình phân cấp Course – Cohort (Enterprise Course-Cohort E-Learning & LMS Platform) tại Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE).

Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này xác định toàn diện các yêu cầu chức năng và phi chức năng chuẩn doanh nghiệp cho hệ thống UTELearn:

1. **Kiến trúc Nghiệp vụ Phân cấp Course – Cohort (Course-Cohort Business Model):**
   - **Course (Khóa học lớn / Container tổng thể):** Đóng vai trò là chương trình đào tạo khung, chứa thông tin định danh (tiêu đề, mã khóa học, danh mục, cấp độ, mục tiêu đầu ra, yêu cầu tiên quyết) và **Chính sách Giá trọn gói (Full Course Package Price)** ưu đãi. Một Course bao gồm một hoặc nhiều Cohort thành phần.
   - **Cohort (Lớp học / Module thành phần):** Trực thuộc một Course cụ thể. Mỗi Cohort có mã lớp, tên lớp, **giá bán lẻ riêng (`price`)**, sĩ số tối đa (`max_capacity`), thời gian mở đăng ký, thời gian đào tạo, lịch mở bài học (`cohort_schedules`), giáo trình học tập và phòng thảo luận riêng biệt.
   - **Chính sách Thương mại Điện tử & Nâng cấp linh hoạt (3 lựa chọn cho Học viên):**
     - **Lựa chọn 1 – Mua Full Course:** Học viên đăng ký trọn bộ Course để sở hữu tất cả các Cohort thành phần với mức giá ưu đãi trọn gói.
     - **Lựa chọn 2 – Mua lẻ Cohort:** Học viên chọn mua 1 hoặc nhiều Cohort riêng lẻ theo nhu cầu thực tế (`Tổng tiền = Sum(Cohort.price)`).
     - **Lựa chọn 3 – Nâng cấp lên Full Course (Upgrade Upsell):** Học viên đã mua lẻ một số Cohort trước đó có thể nâng cấp lên gói Full Course bất kỳ lúc nào bằng cách chỉ thanh toán phần tiền chênh lệch: `Số tiền nâng cấp = Giá Full Course - Tổng số tiền đã thanh toán cho các Cohort trước đó`.

2. **Không gian Học tập Đa phương tiện & Đánh giá Trắc nghiệm (Multimedia Learning & Assessment):**
   - Tích hợp trình phát video **Plyr** (hỗ trợ YouTube, Vimeo, Google Drive, Cloudinary MP4/MKV) và trình đọc tài liệu trực tuyến (PDF Iframe & Microsoft Office Live Viewer cho DOC/DOCX/PPT/PPTX/XLS).
   - Hệ thống kiểm tra đánh giá (Quiz) hỗ trợ câu hỏi trắc nghiệm 1 đáp án (`SINGLE_CHOICE`), nhiều đáp án (`MULTI_SELECT`), điền khuyết (`FILL_BLANK`) và tự luận (`ESSAY`) lưu trữ linh hoạt bằng `JSONB`, có đồng hồ đếm ngược và chấm điểm tự động trên Server.

3. **Kho Tài nguyên Số Cá nhân cho Giảng viên (Instructor Digital Asset Library):**
   - Không gian lưu trữ đám mây độc lập cho từng giảng viên kết nối trực tiếp với Cloudinary (Direct Signed Upload), phân loại theo cây thư mục ảo, kiểm soát quyền truy cập (`PRIVATE`, `DEPARTMENT_SHARED`, `COHORT_PUBLIC`, chia sẻ qua email nội bộ) và tái sử dụng tài nguyên xuyên suốt các bài giảng mà không cần tải lên lại.

4. **Cơ chế Phân quyền Động (Dynamic RBAC) & Bảo mật Đa lớp:**
   - Quản trị viên tùy biến vai trò (`Role`) và danh sách quyền hạn (`Permission`) trực tiếp trên giao diện quản trị, lưu trữ trong PostgreSQL mà không cần sửa đổi mã nguồn.
   - Hỗ trợ xác thực kết hợp Cookie JWT (`UTELearn_Token`) và đăng nhập liên kết qua **Google OAuth2**.

5. **Hệ thống Kiểm duyệt Nội dung & Giám sát Ca trực theo SLA (Content Moderation & SLA Queue):**
   - Kiểm duyệt viên (Moderator) thực hiện điểm danh ca trực (`Check-in / Check-out`), theo dõi năng suất làm việc thực tế và nhận nhiệm vụ duyệt khóa học/nội dung theo SLA.
   - Đảm bảo quy tắc **Chống tự duyệt bài** (`author_id <> assigned_to`), hỗ trợ **Phê duyệt (`PUBLISHED`)**, **Từ chối kèm nhận xét phản hồi (`REJECTED`)** và **Chuyển cấp thẩm quyền (`ESCALATED`)** lên Admin khi gặp trường hợp phức tạp.

6. **Định hướng Phát triển Mở rộng – Hệ thống Chấm Code Tự động (Online Judge - OJ):**
   - *Phạm vi giai đoạn 2 (Future Scope):* Phân hệ Online Judge (soạn đề lập trình, nộp mã nguồn Java/C++/Python, chấm tự động qua Sandbox theo bộ test case mẫu/ẩn) đã được thiết kế sẵn lược đồ cơ sở dữ liệu (`coding_problems`, `coding_submissions`) và đặc tả Use Case (`UC-26` đến `UC-29`), sẵn sàng mở rộng theo kiến trúc Microservice trong giai đoạn tiếp theo.

---

## 1.2 Các Tác nhân Hệ thống (System Actors) & Ma trận Phân quyền (RBAC Matrix)

| Tác nhân (Actor) | Vai trò | Quyền hạn và Phạm vi hoạt động chính |
|------------------|---------|--------------------------------------|
| **Guest** | Khách vãng lai | Xem trang chủ, duyệt & tìm kiếm Course/Cohort, xem chi tiết khóa học, học thử bài giảng miễn phí (`is_free_preview`), đăng ký, đăng nhập, quên mật khẩu. |
| **Student** | Học viên | Mua Full Course, mua lẻ Cohort hoặc nâng cấp bù giá chênh lệch; thanh toán trực tuyến; học video/tài liệu; làm bài kiểm tra Quiz; theo dõi tiến độ học tập; tham gia phòng chat lớp WebSocket. |
| **TA** | Trợ giảng | Tham gia hỗ trợ giải đáp thắc mắc cho học viên trong các Cohort được phân công qua phòng chat WebSocket; xem nội dung bài giảng. |
| **Instructor** | Giảng viên | Tạo & quản lý Course, cấu hình giá Full Course, tạo các Cohort thành phần, soạn giáo trình (Section, Lesson, Quiz), quản lý lịch mở bài, quản lý kho tài nguyên số Cloudinary, gửi duyệt khóa học. |
| **Moderator** | Kiểm duyệt viên | Điểm danh ca trực hàng ngày, xem hàng đợi kiểm duyệt SLA, kiểm tra nội dung Course/Cohort, phê duyệt, từ chối kèm nhận xét hoặc chuyển cấp lên Admin. |
| **Admin** | Quản trị viên | Toàn quyền quản trị hệ thống: Quản lý người dùng, phân quyền động RBAC, quản lý danh mục, duyệt vượt cấp (`ESCALATED`), giám sát điểm danh Moderator, xem Dashboard thống kê doanh thu & vận hành. |

| Quyền hạn (Permission) | Admin | Moderator | Instructor | TA | Student |
|------------------------|:-----:|:---------:|:----------:|:--:|:-------:|
| `USER_MANAGE` | V | - | - | - | - |
| `ROLE_MANAGE` | V | - | - | - | - |
| `COURSE_CREATE` | V | - | V | - | - |
| `COHORT_MANAGE` | V | - | V | - | - |
| `CONTENT_APPROVE` | V | V | - | - | - |
| `ASSET_MANAGE` | V | - | V | - | - |
| `COURSE_ENROLL` | - | - | - | - | V |
| `COHORT_CHAT` | V | - | V | V | V |
| `CODE_SUBMIT` *(Dự kiến)* | - | - | - | - | V |

---

## 1.6 Yêu cầu chức năng chi tiết (Functional Requirements - 37 Use Cases)

### 1.6.1 Phân hệ 1: Xác thực & Hồ sơ cá nhân (Authentication & Profile - UC-01 đến UC-07)
* **FR-AUTH-01 (Đăng nhập & JWT Cookie - UC-01, UC-04):** Xác thực Email/Password bằng BCrypt, cấp phát JWT lưu vào HTTP-Only Cookie `UTELearn_Token`, tự động điều hướng theo vai trò (`/dashboard` cho Admin, `/home` cho các vai trò khác).
* **FR-AUTH-02 (Đăng ký & Google OAuth2 - UC-02, UC-03):** Đăng ký tài khoản mới (gán mặc định `STUDENT`); hỗ trợ đăng nhập qua Google OAuth2 (tự động đồng bộ tài khoản `auth_provider = GOOGLE` và vô hiệu hóa đổi mật khẩu cục bộ đối với tài khoản OAuth2).
* **FR-AUTH-03 (Khôi phục mật khẩu & Hồ sơ - UC-05, UC-06, UC-07):** Gửi liên kết đặt lại mật khẩu qua SMTP Email với token có thời hạn (`password_reset_tokens`); cho phép người dùng cập nhật họ tên, số điện thoại, ảnh đại diện và mật khẩu tại `/profile`.

### 1.6.2 Phân hệ 2: Quản trị Hệ thống (Admin System Management - UC-08 đến UC-12)
* **FR-ADM-01 (Quản lý Người dùng & Dynamic RBAC - UC-08, UC-09):** Admin thực hiện CRUD người dùng, khóa/mở khóa tài khoản, gán vai trò; tạo mới/chỉnh sửa Role và ánh xạ chi tiết từng Permission lưu trong bảng `role_permissions`.
* **FR-ADM-02 (Dashboard Thống kê Toàn diện - UC-10):** Thống kê KPIs thời gian thực gồm tổng doanh thu (`orders`), tổng số người dùng, số học viên, khóa học, nhiệm vụ chờ duyệt và lượt điểm danh theo bộ lọc: Hôm nay (`day`), Tuần này (`week`), Tháng này (`month`), Toàn thời gian (`all`).
* **FR-ADM-03 (Quản lý Danh mục & Giám sát Điểm danh - UC-11, UC-12):** Quản lý danh mục khóa học đa cấp (`categories`) và giám sát bảng chấm công ca trực của toàn bộ Moderator (`/admin/attendance`).

### 1.6.3 Phân hệ 3: Quản lý Khóa học Lớn (Course Container Management - UC-13 đến UC-17)
* **FR-CRS-01 (Khởi tạo & Chỉnh sửa Course - UC-13, UC-14):** Giảng viên tạo và cập nhật thông tin Course (mã khóa học duy nhất, tiêu đề, slug tự động, headline, mô tả, ảnh bìa, cấp độ, danh mục, mục tiêu `objectives` JSONB, yêu cầu `requirements` JSONB).
* **FR-CRS-02 (Cấu hình Giá trọn gói Full Course - UC-17):** Thiết lập mức giá trọn bộ Full Course ưu đãi so với tổng giá mua lẻ các Cohort thành phần (`Sum(Cohort.price)`).
* **FR-CRS-03 (Quản lý Danh sách Cohort & Gửi duyệt - UC-15, UC-16):** Quản lý danh sách các Cohort trực thuộc Course và gửi yêu cầu kiểm duyệt xuất bản (`DRAFT` -> `PENDING_MODERATOR` / `PENDING_REVIEW`).

### 1.6.4 Phân hệ 4: Quản lý Lớp học / Module Thành phần (Cohort Management - UC-33 đến UC-37)
* **FR-COH-01 (Khởi tạo Cohort trong Course - UC-33):** Giảng viên tạo các Cohort thành phần trực thuộc Course với mã lớp, tên lớp, **giá bán lẻ (`price`)**, sĩ số tối đa (`max_capacity`), ngày mở/đóng đăng ký (`enrollment_start`, `enrollment_end`) và ngày bắt đầu/kết thúc học (`study_start`, `study_end`).
* **FR-COH-02 (Soạn Giáo trình & Đề kiểm tra Quiz - UC-37):** Xây dựng cây nội dung học tập gồm các Chương (`Section`), Bài học (`Lesson`: Video, Document, Quiz) và tạo đề kiểm tra trắc nghiệm (`Quiz` & `Question` lưu đáp án bằng `JSONB`).
* **FR-COH-03 (Quản lý Lịch học & Drip Content - UC-36):** Cấu hình lịch mở bài học (`unlock_at`), hạn chót nộp bài (`deadline_at`), chế độ công bố điểm (`IMMEDIATELY`, `AFTER_DEADLINE`, `MANUAL`) và quyền xem lại lời giải (`allow_review_answers`) trong bảng `cohort_schedules`.
* **FR-COH-04 (Thảo luận Lớp học Thời gian thực - UC-35):** Phòng chat WebSocket (STOMP/SockJS) riêng biệt cho từng Cohort (`/topic/cohort/{cohortId}`) giữa Giảng viên, Trợ giảng (TA) và Học viên, lưu lịch sử vào `cohort_messages`.

### 1.6.5 Phân hệ 5: Học trực tuyến & Thanh toán Thương mại Điện tử (Student Learning & Checkout - UC-21 đến UC-25, UC-34)
* **FR-LRN-01 (Duyệt, Tìm kiếm & So sánh Gói học - UC-21):** Tìm kiếm, lọc khóa học theo từ khóa, danh mục, cấp độ; xem chi tiết Course cùng danh sách Cohort thành phần, so sánh giá Full Course và giá lẻ từng Cohort, xem thử bài giảng miễn phí (`is_free_preview`).
* **FR-LRN-02 (Mua Full Course, Mua lẻ Cohort & Nâng cấp bù chênh lệch - UC-22, UC-34):**
  - Hỗ trợ học viên chọn mua trọn gói Full Course, mua lẻ 1 hoặc nhiều Cohort, hoặc **Nâng cấp (Upgrade)** lên Full Course (tự động khấu trừ số tiền đã thanh toán cho các Cohort lẻ trước đó).
  - Xử lý giao dịch qua Cổng thanh toán điện tử (VNPay / MoMo / Payment Webhook), lưu đơn hàng vào bảng `orders` (`items` JSONB) và tự động kích hoạt quyền học tập trong `enrollments` / `cohort_members`.
* **FR-LRN-03 (Học Video/Tài liệu, Làm Quiz & Theo dõi Tiến độ - UC-23, UC-24, UC-25):**
  - Học bài giảng qua trình phát Plyr và trình xem PDF/Office trực tiếp; đánh dấu hoàn thành bài học.
  - Làm bài kiểm tra trắc nghiệm (`Quiz`) có tính giờ đếm ngược, chấm điểm tự động và lưu kết quả vào `quiz_submissions`.
  - Theo dõi % tiến độ hoàn thành tại `/courses/my-courses` và hiển thị gợi ý nâng cấp (Upsell) các Cohort còn lại.

### 1.6.6 Phân hệ 6: Kiểm duyệt Nội dung & Cân bằng Tải (Moderation System - UC-18 đến UC-20)
* **FR-MOD-01 (Điểm danh Ca trực & Hàng đợi SLA - UC-12, UC-18):** Moderator check-in/check-out ca trực hàng ngày (`moderator_attendances`), theo dõi định mức thời gian làm việc (`max_daily_minutes`) và danh sách nhiệm vụ chờ duyệt (`moderation_tasks`).
* **FR-MOD-02 (Phê duyệt, Từ chối & Chuyển cấp - UC-19, UC-20):**
  - Kiểm tra toàn diện thông tin Course, Cohort và cây giáo trình.
  - **Phê duyệt (`APPROVED`):** Mở bán công khai khóa học (`PUBLISHED`).
  - **Từ chối (`REJECTED`):** Gửi nhận xét phản hồi chi tiết (`feedback`) để giảng viên chỉnh sửa.
  - **Chuyển cấp (`ESCALATED`):** Đẩy nhiệm vụ phức tạp lên Admin (`/admin/moderation`, `escalation_level = 2`) kèm lý do (`escalation_reason`).
  - Ràng buộc cứng chống tự duyệt bài của chính mình (`author_id <> assigned_to`).

### 1.6.7 Phân hệ 7: Kho Tài nguyên Số Giảng viên (Digital Asset Library - UC-30 đến UC-32)
* **FR-AST-01 (Direct Signed Upload & Quản lý Kho tài nguyên - UC-30, UC-31):** Tải file trực tiếp lên Cloudinary với chữ ký bảo mật SHA-1, tự động kiểm tra định dạng file theo danh mục và chuẩn hóa phần mở rộng; quản lý theo thư mục ảo và quyền hiển thị (`PRIVATE`, `DEPARTMENT_SHARED`, `COHORT_PUBLIC`).
* **FR-AST-02 (Tái sử dụng & Tải tài nguyên bảo mật - UC-32):** Nhúng nhanh tài nguyên từ kho vào bài giảng qua Asset Picker; tải xuống an toàn qua Proxy Controller (`/assets/download/{id}`) có kiểm tra phân quyền RBAC.

### 1.6.8 Phân hệ 8: Hệ thống Chấm Code Tự động – Online Judge (ĐỊNH HƯỚNG PHÁT TRIỂN - UC-26 đến UC-29)
* **FR-OJ-01 (Định hướng phát triển - Future Scope):** Các chức năng xem đề bài lập trình (`UC-26`), nộp mã nguồn chấm tự động qua Sandbox (`UC-27`), xem kết quả từng test case (`UC-28`) và soạn đề bài lập trình kèm test case ẩn/mẫu JSONB (`UC-29`) đã được thiết kế sẵn cấu trúc bảng CSDL (`coding_problems`, `coding_submissions`) để sẵn sàng tích hợp microservice Judge Engine trong giai đoạn phát triển tiếp theo.

---

## Chapter 02: Thiết kế Cơ sở Dữ liệu Mức Khái niệm (Entity Schema)

### 1. Phân quyền động & Tài khoản
* `roles`, `permissions`, `users`, `role_permissions`, `user_roles`, `password_reset_tokens`.

### 2. Kho tài nguyên số
* `instructor_assets`: Quản lý tài nguyên số, link Cloudinary, cây thư mục ảo (`folder_path`), phạm vi chia sẻ (`PRIVATE`, `DEPARTMENT_SHARED`, `COHORT_PUBLIC`) và danh sách email được cấp quyền.

### 3. Khóa học lớn (Course) & Lớp học/Module thành phần (Cohort)
* `categories`, `courses` (lưu mục tiêu/yêu cầu bằng `JSONB`, giá trọn gói Full Course), `sections`, `lessons` (hỗ trợ Video, Document, Asset đính kèm, Quiz).
* `enrollments`, `cohorts` (giá bán lẻ `price`, sĩ số `max_capacity`, thời gian đăng ký & học tập), `cohort_members` (`STUDENT`, `TEACHER`, `TEACHING_ASSISTANT`).
* `cohort_schedules`: Điều phối ngày mở bài (`unlock_at`), hạn nộp (`deadline_at`), chế độ xem điểm (`show_score_type`), quyền xem lại đáp án (`allow_review_answers`).

### 4. Đánh giá Trắc nghiệm & Online Judge (Tối ưu JSONB)
* **Đang triển khai:** `quizzes`, `questions` (`options JSONB`), `quiz_submissions` (`answers JSONB`).
* **Dự phòng cho Định hướng phát triển (Online Judge):** `coding_problems` (`test_cases JSONB`), `coding_submissions` (`test_results JSONB`).

### 5. Hàng đợi Kiểm duyệt & Điểm danh Ca trực
* `moderator_attendances`: Theo dõi ca trực và tiến độ công việc theo ngày (`assigned_tasks_count`, `completed_tasks_count`, `active_tasks_count`, `total_workload_minutes`, `max_daily_minutes`).
* `moderation_tasks`: Hàng đợi duyệt nội dung, dự toán thời gian (`estimated_review_minutes`), chuyển cấp (`escalation_level`, `escalated_by`, `escalation_reason`), nhận xét (`feedback`), ràng buộc chặn tự duyệt (`chk_not_self_review`).

### 6. Đơn hàng Thương mại Điện tử & Realtime Chat
* `orders` (`items JSONB` lưu danh sách Course/Cohort được mua hoặc nâng cấp, `payment_method`, `transaction_id`, `status`), `cohort_messages` (lưu lịch sử chat WebSocket STOMP theo từng Cohort).
