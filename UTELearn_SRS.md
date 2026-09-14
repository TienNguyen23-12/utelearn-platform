# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)
# UTELearn: NỀN TẢNG KINH DOANH KHÓA HỌC TRỰC TUYẾN VÀ QUẢN LÝ HỌC TẬP SỐ THEO ĐỢT

---

## 1. CÔNG NGHỆ SỬ DỤNG
- **Backend Stack:** Spring Boot (Java 17/21), Spring Security (JWT), Spring Data JPA, Spring WebSocket (STOMP).
- **Frontend Stack:** Thymeleaf (Server-Side Rendering), Bootstrap 5 (Responsive UI), JavaScript (Monaco/CodeMirror Editor cho Online Judge).
- **Database:** PostgreSQL (14+) - Tối ưu hóa **Relational + JSONB & GIN Indexes**, `TIMESTAMPTZ`, và cơ chế `FOR UPDATE SKIP LOCKED`.
- **Hàng đợi & Điều phối (Queue/Worker):** PostgreSQL Advisory Lock & `FOR UPDATE SKIP LOCKED` / Spring In-Memory Queue (Hàng đợi điều phối công bằng theo khối lượng công việc thực tế - Weighted Fair Queueing).
- **Dịch vụ tích hợp (3rd Party):**
  - **Media & Storage:** Cloudinary (Lưu trữ ảnh thumbnail, video bài giảng, tài liệu số).
  - **Cổng thanh toán:** VNPay / MoMo (Thanh toán học phí / khóa học trực tuyến).
  - **Online Judge Sandbox:** Judge0 API hoặc Docker-based Sandbox cách ly để biên dịch và chấm code.
- **Trạng thái:** In Progress – Active Development.

---

## Chapter 01: Tổng quan về đề tài

### 1.1 Giới thiệu chung
**Dự án:** UTELearn - Nền tảng Kinh doanh khóa học trực tuyến và Quản lý học tập số theo đợt (Cohort-based E-Learning & LMS Platform).

Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này xác định toàn diện các yêu cầu chức năng và phi chức năng cho hệ thống UTELearn:
1. **Mô hình Khóa học theo Đợt (Cohort-based Learning):** Phân chia rõ ràng giữa Khung giáo trình chuẩn (Master Course) và các Đợt học/Lớp học cụ thể (Cohorts/Classes) có giảng viên phụ trách, lịch mở bài (Drip Content), hạn chót nộp bài (Deadline) và thảo luận riêng biệt.
2. **Hệ sinh thái Đánh giá toàn diện (Assessment & Online Judge):** Hỗ trợ kiểm tra trắc nghiệm đơn, trắc nghiệm nhiều đáp án (checkbox), điền khuyết, tự luận và bài tập lập trình chấm điểm tự động (OJ).
3. **Cơ chế Phân quyền động (Dynamic RBAC):** Tùy biến vai trò và danh sách quyền hạn không cố định trong mã nguồn.
4. **Hệ thống Kiểm duyệt Điều phối Cân bằng Khối lượng (Workload Fair Queue Moderation):** 
   - Điều phối công việc theo thời lượng duyệt thực tế (`estimated_review_minutes`) thay vì đếm số bài thuần túy.
   - Hỗ trợ mô hình **Kiểm duyệt đồng cấp (Peer Review)**: Giảng viên được phép duyệt chéo bài của nhau nhưng **nghiêm cấm tuyệt đối tự duyệt bài của chính mình** (`author_id <> assigned_to`).
   - Hỗ trợ **Quy trình Chuyển cấp (Escalation Workflow)** khi gặp tài liệu khó hoặc vượt thẩm quyền.
   - Cơ chế tự động thu hồi và phân bổ lại (Failover Reassignment) khi người duyệt nghỉ đột xuất.
5. **Kho Tài nguyên Số Cá nhân cho Giảng viên (Instructor Digital Asset Library):** Không gian lưu trữ độc lập cho từng giảng viên để quản lý, tái sử dụng tài nguyên trên nhiều lớp học.

---

## 1.6 Yêu cầu chức năng (Functional Requirements)

### 1.6.1 Xác thực & Phân quyền động (Dynamic RBAC)
* **FR-AUTH-01 (JWT):** Cấp phát Access/Refresh Token, bảo vệ route bằng Spring Security.
* **FR-AUTH-02 (Dynamic RBAC):** Admin tạo Role mới và gán Permission chi tiết lưu trong PostgreSQL.

### 1.6.2 Khóa học Khung & Lớp học theo đợt (Cohort Model)
* **FR-CRS-01 (Master Course):** Soạn giáo trình chuẩn (Section, Lesson).
* **FR-CRS-02 (Cohort Run):** Khởi tạo lớp học thực tế có ngày mở đăng ký, ngày khai giảng, ngày bế giảng, sĩ số giới hạn và giá học riêng.
* **FR-CRS-03 (Drip Content & Custom Deadlines):** Lớp học quy định ngày mở bài học (`unlock_at`) và hạn chót nộp bài (`deadline_at`). Video bài giảng sau khi mở được xem lại không giới hạn đến hết khóa.
* **FR-CRS-04 (Cấu hình Xem lại Điểm & Đáp án):** Giảng viên có quyền cấu hình cho từng lớp: Xem điểm ngay (`IMMEDIATELY`), hết hạn mới xem (`AFTER_DEADLINE`) hay công bố thủ công (`MANUAL`); Bật/tắt cho phép xem lại chi tiết bài làm và đáp án đúng chống lộ đề thi (`allow_review_answers`).

### 1.6.3 Kho Tài nguyên Số Giảng viên (Digital Asset Library)
* **FR-AST-01:** Kho lưu trữ cá nhân của từng giảng viên (Video, Slides, PDF, Test Case).
* **FR-AST-02:** Tái sử dụng nội dung (Reusability) đưa vào bài học của bất kỳ lớp nào mà không cần upload lại lên Cloudinary.

### 1.6.4 Hệ thống Kiểm duyệt Cân bằng Tải & Chuyển cấp (Moderation System)
* **FR-MOD-01 (Điểm danh & Theo dõi Định mức):** Ghi nhận ca trực, tự động thống kê số bài đã nhận (`assigned_tasks_count`), số bài đã xong (`completed_tasks_count`), số bài đang giữ (`active_tasks_count`), và tổng số phút công việc (`total_workload_minutes`) so với định mức ca (`max_daily_minutes`).
* **FR-MOD-02 (Cân bằng Tải Khối lượng - Least-Loaded Dispatcher):** Gán bài mới cho người có số việc đang ôm (`active_tasks_count`) hoặc tổng số phút duyệt ít nhất đang trực trong ngày.
* **FR-MOD-03 (Thời gian duyệt Động):** Thời gian duyệt (`estimated_review_minutes`) và hạn chót (`deadline_at`) được tính linh hoạt theo độ dài video hoặc số lượng test case.
* **FR-MOD-04 (Kiểm duyệt Đồng cấp & Chống tự duyệt):** Cho phép Giảng viên duyệt chéo bài của giảng viên khác (Peer Review). Ràng buộc cứng ở DB: `author_id <> assigned_to`.
* **FR-MOD-05 (Chuyển cấp thẩm quyền - Escalation):** Moderator có quyền bấm chuyển bài khó (`ESCALATED`) lên Trưởng bộ môn hoặc Admin kèm lý do (`escalation_reason`).
* **FR-MOD-06 (Xử lý Nghỉ đột xuất - Failover):** Tự động thu hồi các bài chưa xong của người báo nghỉ và chia lại cho các nhân sự còn lại.

### 1.6.5 Hệ thống Đánh giá & Online Judge (Assessment & OJ)
* **FR-EXM-01:** Hỗ trợ 4 dạng câu hỏi: Trắc nghiệm 1 đáp án, Chọn nhiều đáp án (Checkbox), Điền khuyết, Tự luận (Lưu trữ tinh gọn bằng `JSONB`).
* **FR-EXM-02:** Tính giờ làm bài chặt chẽ qua `started_at` và `submitted_at`.
* **FR-EXM-03 (Online Judge):** Chấm code tự động qua Sandbox. Quản lý test case ẩn/mẫu bằng `JSONB`. Cấu hình ẩn/hiện chi tiết lỗi của test ẩn (`show_hidden_test_details`).

---

## Chapter 02: Thiết kế Cơ sở Dữ liệu Mức Khái niệm (Entity Schema)

### 1. Phân quyền động & Tài khoản
* `roles`, `permissions`, `users`, `role_permissions`, `user_roles`.

### 2. Kho tài nguyên số
* `instructor_assets`: Quản lý tài nguyên số, link Cloudinary, cây thư mục ảo, phạm vi chia sẻ (`PRIVATE`, `DEPARTMENT_SHARED`, `COHORT_PUBLIC`).

### 3. Giáo trình mẹ & Lớp học theo đợt
* `categories`, `courses` (lưu mục tiêu/yêu cầu bằng `JSONB`), `sections`, `lessons` (gộp trực tiếp video/document).
* `cohorts`, `cohort_members`.
* `cohort_schedules`: **Điều phối ngày mở, deadline, chế độ xem điểm (`show_score_type`), quyền xem lại đáp án (`allow_review_answers`)**.

### 4. Đánh giá & Online Judge (Tối ưu JSONB)
* `quizzes`, `questions` (`options JSONB`), `quiz_submissions` (`answers JSONB`).
* `coding_problems` (`test_cases JSONB`), `coding_submissions` (`test_results JSONB`).

### 5. Hàng đợi Kiểm duyệt Cân bằng Tải
* `moderator_attendances`: **Theo dõi tiến độ thực tế theo ngày (`assigned_tasks_count`, `completed_tasks_count`, `active_tasks_count`, `total_workload_minutes`, `max_daily_minutes`)**.
* `moderation_tasks`: **Hàng đợi duyệt việc, dự toán thời gian (`estimated_review_minutes`), chuyển cấp (`escalated_by`, `escalation_reason`), ràng buộc chặn tự duyệt (`chk_not_self_review`)**.

### 6. Đơn hàng & Realtime Chat
* `orders` (`items JSONB`), `cohort_messages`.
