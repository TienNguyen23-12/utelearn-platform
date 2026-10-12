# BÁO CÁO RÀ SOÁT TIẾN ĐỘ ĐỒ ÁN & DANH SÁCH TÍNH NĂNG CHƯA LÀM (CHUẨN DOANH NGHIỆP)
**Dự án:** UTELearn – Nền tảng Kinh doanh Khóa học & Quản lý Học tập số theo mô hình Course - Cohort  
**Cập nhật theo:** Bộ đặc tả 37 Use Cases (`docs/usecases/`) & SRS Chuẩn Doanh Nghiệp (Course-Cohort Architecture, Cổng thanh toán IPN/Webhook, WebSocket STOMP, Online Judge định hướng phát triển)

---

## PHẦN 1: TỔNG QUAN NHỮNG GÌ ĐÃ LÀM XONG (ĐÃ CÓ CODE HOÀN CHỈNH)

| Phân hệ | Use Case đã hoàn thành | Thành phần Code đã có | Trạng thái |
| :--- | :--- | :--- | :---: |
| **1. Xác thực & Hồ sơ (Auth & Profile)** | `UC-01` -> `UC-07`: Đăng nhập JWT HTTP-Only Cookie, Đăng ký, Google OAuth2 (chặn đổi pass tài khoản OAuth2), Đăng xuất, Quên/Đặt lại mật khẩu qua SMTP Email, Cập nhật Profile. | `AuthController`, `AuthApiController`, `ProfileController`, `OAuth2AuthenticationSuccessHandler`, `EmailServiceImpl` | **100% Hoàn thành** |
| **2. Quản trị Hệ thống (Admin)** | `UC-08`, `UC-09`, `UC-10`, `UC-12`: Quản lý User, Phân quyền động RBAC (Role/Permission), Dashboard thống kê doanh thu/học viên theo ngày-tuần-tháng, Giám sát điểm danh Moderator. | `UserController`, `RoleController`, `DashboardController`, `AdminAttendanceController`, `AdminModerationController` | **~85% Hoàn thành** *(Thiếu UI CRUD Category)* |
| **3. Kho Tài nguyên số (Digital Assets)** | `UC-30`, `UC-31`, `UC-32`: Direct Signed Upload lên Cloudinary (chữ ký SHA-1), tự khóa đuôi file theo loại, chuẩn hóa phần mở rộng, phân quyền `PRIVATE`/`DEPARTMENT_SHARED`/`COHORT_PUBLIC`, tải file qua Backend Security Proxy. | `InstructorAssetController`, `AssetDownloadController`, `CloudinaryServiceImpl`, `InstructorAssetServiceImpl` | **100% Hoàn thành** |
| **4. Kiểm duyệt Nội dung (Moderation)** | `UC-18`, `UC-19`, `UC-20`: Điểm danh Check-in/Check-out ca trực Moderator, Hàng đợi duyệt khóa học, Phê duyệt (`PUBLISHED`), Từ chối kèm nhận xét (`REJECTED`), Chuyển cấp lên Admin (`ESCALATED`). | `ModeratorController`, `ModeratorApiController`, `ModerationServiceImpl`, `AttendanceServiceImpl` | **~85% Hoàn thành** *(Cần bổ sung ràng buộc SLA)* |
| **5. Khóa học & Học tập cơ bản** | `UC-13`, `UC-14`, `UC-15`, `UC-21`, `UC-23`: Tạo/Sửa/Xóa Course, Soạn Section & Lesson (nhúng từ Kho tài nguyên), Duyệt/Tìm kiếm Course, Học Video (Plyr: YouTube/Vimeo/MP4) & Đọc tài liệu (PDF/Office Viewer). | `InstructorCourseController`, `InstructorCurriculumApiController`, `CourseController`, `CourseApiController`, `learn.html`, `detail.html` | **~65% Hoàn thành** *(Đang chạy theo Course đơn, chưa ghép Cohort & Giá)* |

---

## PHẦN 2: DANH SÁCH CÁC TÍNH NĂNG CHƯA LÀM (CHIA THEO MỨC ĐỘ KHẨN CẤP)

---

### 🔴 MỨC ĐỘ 1: KHẨN CẤP – BẮT BUỘC PHẢI LÀM NGAY (CRITICAL / P0)
*Lý do khẩn cấp: Khắc phục các đường dẫn đang bị lỗi 404 trên Sidebar và hoàn thiện xương sống của kiến trúc nghiệp vụ **Course - Cohort**.*

#### 1.1. Quản lý Lớp học / Module thành phần – Cohort CRUD (`UC-16`, `UC-33`)
- **Hiện trạng trong code:**
  - Trong `fragments/sidebar.html` (dòng 117) đã có menu **"Đợt học (Cohorts)"** trỏ tới `/instructor/cohorts`, nhưng **chưa có `CohortController` và `CohortService`** -> Bấm vào sẽ bị lỗi `404 Not Found`.
  - Đã có sẵn Entity `Cohort.java`, `CohortMember.java` và `CohortMemberRepository.java`, nhưng chưa có `CohortRepository.java`.
- **Cần code những gì:**
  1. Tạo `CohortRepository`, `CohortService`, `InstructorCohortController` (`/instructor/cohorts` và tab quản lý Cohort trong `/instructor/courses/{id}`).
  2. Cho phép Giảng viên tạo/sửa/xóa Cohort thuộc một Course: nhập Mã lớp (`code`), Tên lớp (`name`), Giá bán lẻ (`price`), Sĩ số tối đa (`maxCapacity`), Ngày mở/đóng đăng ký (`enrollmentStart`, `enrollmentEnd`), Ngày bắt đầu/kết thúc học (`studyStart`, `studyEnd`).
  3. Tự động gán Giảng viên tạo lớp vào `cohort_members` với vai trò `TEACHER` và hỗ trợ gán Trợ giảng (`TEACHING_ASSISTANT`).

#### 1.2. Cấu hình Giá trọn gói Full Course & Chính sách Mua 3 Lựa chọn (`UC-17`, `UC-22`, `UC-34`)
- **Hiện trạng trong code:**
  - Bảng `courses` (`Course.java` & `CourseRequestDTO.java`) hiện **chưa có trường `price` (Giá trọn gói Full Course)**.
  - Trang chi tiết khóa học `templates/courses/detail.html` (dòng 377) đang hiển thị cứng chữ `"Miễn phí"` và nút `"Tham gia ngay"` chỉ lưu thẳng vào bảng `Enrollment`.
- **Cần code những gì:**
  1. Bổ sung trường `price` (Giá Full Course trọn bộ ưu đãi) vào `Course.java`, `CourseRequestDTO.java` và form tạo/sửa khóa học (`instructor/courses/form.html`).
  2. Trên trang `/courses/{slug}` (`detail.html`), hiển thị danh sách các Cohort thành phần kèm giá lẻ từng Cohort và bảng so sánh với Giá Full Course ưu đãi.
  3. Cho phép Học viên chọn 1 trong 3 hình thức mua chuẩn thương mại điện tử (`UC-22`, `UC-34`):
     - **Mua Full Course:** Sở hữu toàn bộ các Cohort trong Course với giá ưu đãi trọn gói.
     - **Mua lẻ 1 hoặc nhiều Cohort:** Tick chọn các Cohort muốn mua -> Tính tổng tiền = `Sum(Cohort.price)`.
     - **Nâng cấp (Upgrade) lên Full Course:** Nếu học viên đã mua lẻ một số Cohort trước đó, tự động tính số tiền bù chênh lệch = `Giá Full Course - Tổng tiền đã thanh toán cho các Cohort trước`.

#### 1.3. Xử lý điều hướng mục Online Judge trên Sidebar để tránh lỗi 404 (`UC-26` -> `UC-29`) [ĐÃ HOÀN THÀNH - DONE]
- **Hiện trạng:** Đã tạo `OjController.java`, giao diện `templates/oj/problems.html` (Roadmap Phase 2), bổ sung route vào `SecurityConfig`, và gắn badge `Coming Soon` tại `fragments/sidebar.html` & `system-menus.json`. An toàn 100% khi demo/bảo vệ đồ án.

---

### 🟠 MỨC ĐỘ 2: QUAN TRỌNG CAO – NGHIỆP VỤ TRỌNG TÂM CHUẨN DOANH NGHIỆP (HIGH / P1)
*Lý do quan trọng: Hoàn thiện luồng Thanh toán Đơn hàng (`Order`) và Hệ thống Kiểm tra Đánh giá (`Quiz`) để khép kín vòng đời học tập và doanh thu.*

#### 2.1. Hệ thống Đơn hàng & Cổng Thanh toán Điện tử (`UC-22`, `UC-34`)
- **Hiện trạng trong code:**
  - Đã có Entity `Order.java` (lưu `orderCode`, `totalAmount`, `status`, `paymentMethod`, `transactionId`, `items` JSONB) và `OrderRepository.java` (đã viết sẵn hàm tính doanh thu cho Dashboard), nhưng **chưa có `OrderController` và `OrderService`**.
- **Cần code những gì:**
  1. Xây dựng `OrderService` & `OrderController` (`/checkout`, `/orders`, `/api/payment/vnpay-return` hoặc Webhook IPN):
     - Khởi tạo đơn hàng `Order` (`status = PENDING`, lưu danh sách Course/Cohort mua mới hoặc nâng cấp vào cột `items` JSONB).
     - Tích hợp luồng thanh toán chuẩn doanh nghiệp (VNPay Sandbox / VietQR IPN Webhook).
     - Khi giao dịch thành công (`status = SUCCESS`), tự động ghi nhận `transactionId`, kích hoạt bản ghi `Enrollment` và `CohortMember` (cộng `currentEnrolled`), đồng thời cập nhật ngay doanh thu trên Admin Dashboard (`/dashboard`).

#### 2.2. Hệ thống Bài kiểm tra Trắc nghiệm – Quiz & Question (`UC-24`, `UC-37`)
- **Hiện trạng trong code:**
  - Đã có 3 Entity: `Quiz.java`, `Question.java` (lưu `options` bằng JSONB), `QuizSubmission.java` (lưu `answers` bằng JSONB).
  - Trong `curriculum.html` đã cho phép tạo bài học loại `QUIZ`, nhưng **chưa có giao diện soạn câu hỏi cho Quiz** và trong `learn.html` **chưa có giao diện làm bài thi trắc nghiệm**.
- **Cần code những gì:**
  1. **Cho Giảng viên (`UC-37`):** Giao diện cấu hình `Quiz` (thời gian làm bài `durationMinutes`, điểm đạt `passingScore`, số lần làm tối đa `maxAttempts`, trộn đề `shuffleQuestions`) và CRUD danh sách câu hỏi `Question` (`SINGLE_CHOICE`, `MULTI_SELECT`, `FILL_BLANK`).
  2. **Cho Học viên (`UC-24`):** Giao diện làm bài thi trong `learn.html` có đồng hồ đếm ngược, tự động nộp bài khi hết giờ, chấm điểm tự động trên Server và lưu kết quả vào `quiz_submissions`.

---

### 🟡 MỨC ĐỘ 3: TRUNG BÌNH – HOÀN THIỆN CHI TIẾT USE CASE (MEDIUM / P2)
*Lý do: Hoàn thiện đầy đủ các tính năng tương tác thời gian thực, lịch mở bài học và quản trị danh mục.*

#### 3.1. Phòng Chat Thảo luận Lớp học Thời gian thực qua WebSocket (`UC-35`)
- **Hiện trạng trong code:**
  - Đã khai báo thư viện `spring-boot-starter-websocket` trong `pom.xml`, đã có cấu hình `/ws`, `/app`, `/topic` trong `application.yml` và đã có Entity `CohortMessage.java`.
  - **Chưa có:** `WebSocketConfig.java`, `CohortChatController.java`, `CohortMessageRepository.java` và giao diện khung chat.
- **Cần code những gì:**
  - Cấu hình STOMP WebSocket endpoint `/ws`, kiểm tra quyền thành viên Cohort (`cohort_members`), broadcast tin nhắn qua `/topic/cohort/{cohortId}` và lưu lịch sử vào `cohort_messages`.

#### 3.2. Đánh dấu Hoàn thành Bài học, Theo dõi Tiến độ & Gợi ý Nâng cấp (`UC-23`, `UC-25`)
- **Hiện trạng trong code:**
  - Bảng `Enrollment` đã có cột `progress` (Double), trang `/courses/my-courses` đã hiển thị danh sách khóa học đã tham gia.
  - **Chưa có:** Nút *"Đánh dấu hoàn thành bài học"* trên `learn.html` để cập nhật `% progress`, và khối *"Gợi ý nâng cấp trọn bộ Full Course (Upsell)"* trên `/courses/my-courses`.

#### 3.3. Quản lý Lịch mở bài học & Hạn nộp bài theo Cohort (`UC-36`)
- **Hiện trạng trong code:**
  - Đã có Entity `CohortSchedule.java` (`unlockAt`, `deadlineAt`, `showScoreType`, `allowReviewAnswers`), cần bổ sung giao diện cho Giảng viên thiết lập lịch mở khóa từng bài học (Drip Content) và cấu hình chế độ xem điểm cho Cohort.

#### 3.4. Quản lý Danh mục Khóa học cho Admin (`UC-11`) & Ràng buộc Kiểm duyệt SLA (`UC-18`, `UC-19`)
- **Quản lý Danh mục (`UC-11`):** Tạo trang `/admin/categories` để Admin thêm/sửa/xóa `Category` đa cấp (hiện mới chỉ đọc danh mục từ DB).
- **Hoàn thiện Kiểm duyệt (`UC-18`, `UC-19`):**
  - Bổ sung kiểm tra trong `ModerationServiceImpl`: chặn Giảng viên tự duyệt khóa học của chính mình (`authorId != moderatorId`), kiểm tra Moderator đang trong ca trực (`isActive = true`), và tự động cập nhật thống kê năng suất (`assignedTasksCount`, `completedTasksCount`) vào `moderator_attendances`.

---

### 🟢 MỨC ĐỘ 4: ĐỊNH HƯỚNG PHÁT TRIỂN TƯƠNG LAI (FUTURE SCOPE / P3 – KHÔNG CODE)
*Đúng theo đặc tả trong `UC06_OnlineJudge.md` và `UTELearn_SRS.md`:*

- **Phân hệ 8 – Online Judge (`UC-26`, `UC-27`, `UC-28`, `UC-29`):**
  - Soạn đề bài lập trình (`CodingProblem`), nộp mã nguồn (`CodingSubmission`), chấm tự động qua Judge0 / Docker Sandbox.
  - **Hành động:** Giữ nguyên Database Schema (`coding_problems`, `coding_submissions`) và tài liệu Use Case để trình bày trong chương **"Định hướng phát triển (Future Work)"** của báo cáo đồ án.
