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
| **5. Quản lý Khóa học & Đợt học (Course - Cohort)** | `UC-13`, `UC-14`, `UC-15`, `UC-16`, `UC-17`, `UC-33`: Tạo/Sửa/Xóa Course; Cấu hình Giá Full Course trọn bộ; CRUD Lớp học / Module Cohort (mã lớp, tên lớp, giá bán lẻ, sĩ số, lịch học); Quản lý Giáo trình Module độc lập. | `InstructorCourseController`, `InstructorCohortController`, `CohortApiController`, `CohortService`, `CohortRepository`, `templates/instructor/cohorts/` | **100% Hoàn thành** |
| **6. Quản lý Lộ trình & Lịch học Drip Content** | `UC-36`: Cấu hình Lộ trình Module theo Cohort; Thiết lập thời điểm mở khóa từng bài học (`unlock_at`), hạn nộp bài (`deadline_at`), chế độ công bố điểm (`showScoreType`); Nút mở nhanh tất cả bài học. | `CohortSchedule`, `CohortScheduleRepository`, `InstructorCohortController` (`/schedule`), `templates/instructor/cohorts/schedule.html` | **100% Hoàn thành** |
| **7. Thương mại Điện tử & Thanh toán Khóa học** | `UC-21`, `UC-22`, `UC-34`: Trang chi tiết khóa học/module; So sánh giá Full Course trọn bộ vs Mua lẻ từng Module; Trang Checkout tính toán tự động khấu trừ nâng cấp (`discountAmount`); Xử lý ghi danh học tập. | `CourseController`, `CheckoutController`, `templates/courses/detail.html`, `templates/courses/module-detail.html`, `templates/checkout/index.html` | **~80% Hoàn thành** *(Đã có luồng Checkout & ghi danh; đang chờ tích hợp VNPay IPN)* |
| **8. Trải nghiệm Học tập (Learning Experience)** | `UC-23`, `UC-25`: Giao diện học tập chuẩn Moodle Layout responsive; Học Video (Plyr Player), đọc tài liệu (PDF/Office viewer); Đánh dấu hoàn thành bài học realtime; Theo dõi `% Tiến độ`; Trang Khóa học của tôi phân loại 2 tab (Trọn gói & Module lẻ). | `templates/courses/module-learn.html`, `templates/courses/my-courses.html`, `UserEnrollmentSummaryDTO` | **100% Hoàn thành** |
| **9. Điều hướng An toàn Online Judge (UI Guard)** | `UC-26` -> `UC-29`: Trang giới thiệu & Roadmap Phase 2; Gắn badge `Coming Soon` trên Sidebar & Navbar; Chống lỗi 404 Not Found khi bảo vệ đồ án. | `OjController`, `templates/oj/problems.html`, `fragments/sidebar.html`, `system-menus.json` | **100% Hoàn thành** |

---

## PHẦN 2: BẢNG TIẾN ĐỘ THEO DÕI CÔNG VIỆC (TO-DO TRACKER)

| Mã Task | Mức độ khẩn cấp | Phân hệ (Module) | Use Case | Tên tính năng / Công việc | Trạng thái (Status) | Ghi chú nghiệm thu |
| :---: | :---: | :--- | :---: | :--- | :---: | :--- |
| **TASK-01** | 🔴 P0 - Khẩn cấp | Module 4: Quản lý Cohort | `UC-16, UC-33` | Quản lý Lớp học / Module thành phần (Cohort CRUD) | **✅ DONE** | Đã hoàn thành 100% (`CohortController`, `CohortService`, UI form/list, sửa lỗi 404 Sidebar). |
| **TASK-02** | 🔴 P0 - Khẩn cấp | Module 3 & 5: Pricing | `UC-17, UC-21, UC-22, UC-34` | Cấu hình Giá Full Course & Chính sách mua 3 lựa chọn | **✅ DONE** | Đã hoàn thành 100% (Giá Full Course, trang `/checkout`, tính chiết khấu khi nâng cấp). |
| **TASK-03** | 🔴 P0 - Khẩn cấp | Module 8: OJ Guard | `UC-26 -> UC-29` | Xử lý điều hướng mục Online Judge trên Sidebar | **✅ DONE** | Đã hoàn thành 100% (Badge Coming Soon, trang Roadmap Phase 2 chống 404). |
| **TASK-08** | 🟡 P2 - Trung bình | Module 4: Lịch học | `UC-36` | Quản lý Lịch mở bài học (Drip Content) theo Cohort | **✅ DONE** | Đã hoàn thành 100% (`CohortScheduleRepository`, UI `schedule.html`, mở bài, hạn nộp, xem điểm). |
| **TASK-07** | 🟡 P2 - Trung bình | Module 5: Tiến độ | `UC-23, UC-25` | Đánh dấu hoàn thành bài học (% Tiến độ) & My Courses | **✅ DONE** | Đã hoàn thành 100% (Progress-bar, nút đánh dấu hoàn thành, phân 2 tab tại `my-courses.html`). |
| **TASK-04** | 🟠 P1 - Quan trọng cao | Module 5: Đơn hàng | `UC-22, UC-34` | Hệ thống Đơn hàng (Order) & Cổng thanh toán VNPay | **⏳ IN_PROGRESS** | Đã có Checkout xử lý mua lẻ và trọn gói; đang chờ đấu nối cổng thanh toán VNPay Sandbox. |
| **TASK-05** | 🟠 P1 - Quan trọng cao | Module 4 & 5: Quiz | `UC-24, UC-37` | Soạn đề Quiz (Giảng viên) & Làm bài trắc nghiệm (Học viên) | **❌ TODO** | Entity JSONB đã có sẵn; cần làm Controller, Service và UI làm bài trắc nghiệm tính giờ. |
| **TASK-06** | 🟡 P2 - Trung bình | Module 4: Realtime Chat | `UC-35` | Phòng Chat thảo luận lớp học realtime qua WebSocket | **❌ TODO** | Đã có entity `CohortMessage` và thư viện; cần cấu hình `WebSocketConfig` và UI khung chat. |
| **TASK-09** | 🟡 P2 - Trung bình | Module 2 & 6: Admin/SLA | `UC-11, UC-18, UC-19` | Quản lý Danh mục cho Admin & Ràng buộc SLA Kiểm duyệt | **❌ TODO** | Cần làm trang `/admin/categories` và bổ sung chặn tự duyệt bài trong `ModerationServiceImpl`. |
| **TASK-10** | 🟢 P3 - Định hướng | Module 8: OJ Sandbox | `UC-26 -> UC-29` | Hệ thống Chấm bài Lập trình tự động (Judge0 Sandbox) | **🎯 FUTURE_SCOPE** | Giai đoạn 2 (Future Work); giữ nguyên Schema & Tài liệu Use Case để báo cáo. |

---

## PHẦN 3: CHI TIẾT CÁC CÔNG VIỆC CÒN LẠI CẦN LÀM TIẾP

---

### 🟠 MỨC ĐỘ 1: QUAN TRỌNG CAO – NGHIỆP VỤ TRỌNG TÂM (HIGH / P1)

#### 1. Hoàn thiện Cổng thanh toán Điện tử VNPay & Lưu trữ Đơn hàng (`TASK-04` - `UC-22`, `UC-34`)
- **Hiện trạng:** `CheckoutController` đã xử lý luồng thanh toán mô phỏng và ghi danh học viên vào `cohort_members`.
- **Cần làm tiếp:**
  1. Tạo `OrderService` ghi nhận lịch sử giao dịch vào bảng `orders` (`orderCode`, `totalAmount`, `status`, `paymentMethod`, `items` JSONB).
  2. Tích hợp thư viện thanh toán VNPay Sandbox (Tạo URL thanh toán, xử lý Return URL và Webhook IPN).
  3. Cập nhật trạng thái `SUCCESS` và tự động cộng doanh thu vào thống kê Dashboard.

#### 2. Hệ thống Bài kiểm tra Trắc nghiệm – Quiz & Question (`TASK-05` - `UC-24`, `UC-37`)
- **Hiện trạng:** Đã có 3 Entity `Quiz.java`, `Question.java` (lưu options bằng JSONB), `QuizSubmission.java`.
- **Cần làm tiếp:**
  1. **Cho Giảng viên (`UC-37`):** Giao diện cấu hình Quiz (thời gian làm bài `durationMinutes`, điểm đạt `passingScore`, số lần làm tối đa `maxAttempts`, trộn đề) và CRUD câu hỏi `Question` (`SINGLE_CHOICE`, `MULTI_SELECT`, `FILL_BLANK`).
  2. **Cho Học viên (`UC-24`):** Giao diện làm bài thi trong `module-learn.html` có đồng hồ đếm ngược, tự động nộp bài khi hết giờ, chấm điểm tự động trên Server và lưu kết quả vào `quiz_submissions`.

---

### 🟡 MỨC ĐỘ 2: TRUNG BÌNH – HOÀN THIỆN TÍNH NĂNG NÂNG CAO (MEDIUM / P2)

#### 3. Phòng Chat Thảo luận Lớp học Thời gian thực qua WebSocket (`TASK-06` - `UC-35`)
- **Hiện trạng:** Đã có `spring-boot-starter-websocket` trong `pom.xml`, cấu hình STOMP trong `application.yml` và Entity `CohortMessage.java`.
- **Cần làm tiếp:**
  - Viết `WebSocketConfig.java`, `CohortChatController.java`, `CohortMessageRepository.java`.
  - Tích hợp khung chat realtime tại góc phải giao diện `module-learn.html` qua SockJS/STOMP.

#### 4. Quản lý Danh mục Khóa học cho Admin & Ràng buộc Kiểm duyệt SLA (`TASK-09` - `UC-11`, `UC-18`, `UC-19`)
- **Quản lý Danh mục (`UC-11`):** Tạo trang `/admin/categories` để Admin thêm/sửa/xóa danh mục đa cấp.
- **Ràng buộc Kiểm duyệt (`UC-18`, `UC-19`):** Trong `ModerationServiceImpl`, bổ sung kiểm tra chặn Giảng viên tự duyệt khóa học của chính mình (`authorId != moderatorId`) và bắt buộc Moderator Check-in ca trực trước khi duyệt bài.

---

### 🟢 MỨC ĐỘ 3: ĐỊNH HƯỚNG PHÁT TRIỂN TƯƠNG LAI (FUTURE SCOPE / P3 – KHÔNG CODE)
- **TASK-10 (Module 8: Online Judge Sandbox - `UC-26` -> `UC-29`):**
  - Trình biên dịch mã nguồn đa ngôn ngữ và chạy test case qua Docker Sandbox.
  - Đã có trang giới thiệu tại `/oj/problems` và giữ nguyên trong Báo cáo Đồ án mục "Định hướng phát triển".
