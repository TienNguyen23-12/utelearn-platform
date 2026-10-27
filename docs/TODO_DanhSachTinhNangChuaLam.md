# BÁO CÁO RÀ SOÁT TIẾN ĐỘ ĐỒ ÁN & DANH SÁCH TÍNH NĂNG CHƯA LÀM (CHUẨN DOANH NGHIỆP)
**Dự án:** UTELearn – Nền tảng Kinh doanh Khóa học & Quản lý Học tập số theo mô hình Course - Cohort  
**Cập nhật theo:** Bộ đặc tả 37 Use Cases (`docs/usecases/`) & SRS Chuẩn Doanh Nghiệp (Course-Cohort Architecture, Cổng thanh toán IPN/Webhook, WebSocket STOMP, Online Judge định hướng phát triển)  
**Thời điểm đối chiếu & cập nhật:** 22/10/2026 (Đã đối chiếu 100% khớp với mã nguồn thực tế)

---

## PHẦN 1: TỔNG QUAN NHỮNG GÌ ĐÃ LÀM XONG (ĐÃ CÓ CODE HOÀN CHỈNH)

| Phân hệ | Use Case đã hoàn thành | Thành phần Code thực tế đã có | Trạng thái hiện tại |
| :--- | :--- | :--- | :---: |
| **1. Xác thực & Hồ sơ (Auth & Profile)** | `UC-01` -> `UC-07`: Đăng nhập JWT HTTP-Only Cookie, Đăng ký, Google OAuth2, Đổi mật khẩu, Quên/Đặt lại mật khẩu qua SMTP Email, Cập nhật Profile, RBAC View Guard. | `AuthController`, `AuthApiController`, `ProfileController`, `OAuth2AuthenticationSuccessHandler`, `EmailServiceImpl`, `RbacHelper`, `SecurityConfig` | **100% Hoàn thành** |
| **2. Quản trị Hệ thống (Admin)** | `UC-08`, `UC-09`, `UC-10`, `UC-11`, `UC-12`: Quản lý Người dùng, Phân quyền động RBAC (Role/Permission), Quản lý Danh mục khóa học đa cấp (Category CRUD hoàn chỉnh với KPI stat-widget và Modal UI xác nhận xóa), Dashboard thống kê doanh thu/học viên/đơn hàng, Giám sát điểm danh Moderator. | `UserController`, `RoleController`, `AdminCategoryController`, `CategoryServiceImpl`, `DashboardController`, `AdminAttendanceController`, `AdminModerationController`, `templates/admin/` | **100% Hoàn thành** |
| **3. Kho Tài nguyên số (Digital Assets)** | `UC-30`, `UC-31`, `UC-32`: Direct Signed Upload lên Cloudinary (chữ ký bảo mật SHA-1), tự động khóa đuôi file theo loại, chuẩn hóa phần mở rộng, phân quyền `PRIVATE`/`DEPARTMENT_SHARED`/`COHORT_PUBLIC`, tải file qua Backend Security Proxy (cấp quyền thẩm định tải tài liệu cho Moderator). | `InstructorAssetController`, `AssetDownloadController`, `CloudinaryServiceImpl`, `InstructorAssetServiceImpl`, `templates/instructor/assets/` | **100% Hoàn thành** |
| **4. Kiểm duyệt Nội dung (Moderation)** | `UC-18`, `UC-19`, `UC-20`: Điểm danh Check-in/Check-out ca trực Moderator, Hàng đợi duyệt khóa học theo SLA, Chống tự kiểm duyệt, Phê duyệt (`PUBLISHED`), Từ chối kèm nhận xét (`REJECTED`), Chuyển cấp lên Admin (`ESCALATED`), Tự động cập nhật năng suất ca trực, Phân định triệt để quyền biên tập (`canEdit`) và quyền thanh tra (`hasAccessRole`), Cấp quyền thẩm định nội dung bài học và tài liệu mà không yêu cầu ghi danh. | `ModeratorController`, `ModeratorApiController`, `ModerationServiceImpl`, `AttendanceServiceImpl`, `CourseController`, `AssetDownloadController`, `templates/moderator/` | **100% Hoàn thành** |
| **5. Khóa học lớn & Module thành phần (Course - Cohort)** | `UC-13` -> `UC-17`, `UC-33`: Tạo/Sửa/Xóa Course lớn, CRUD Module (Cohort) cho Giảng viên, Quản lý Section & Lesson phân nhóm theo Module, Cấu hình giá Full Course & giá lẻ Module, Server-Side Rendering (Thymeleaf SSR), Chế độ chỉnh sửa trực tiếp tại chỗ (Inline Editing). | `CourseController`, `InstructorCourseController`, `InstructorCohortController`, `CohortServiceImpl`, `CourseServiceImpl`, `SectionServiceImpl`, `LessonServiceImpl`, `detail.html`, `templates/instructor/` | **100% Hoàn thành** |
| **6. Học tập số & Tiến độ (Learning & Drip Content)** | `UC-21`, `UC-23`, `UC-25`, `UC-36`: Học Video đa nguồn (YouTube, Vimeo, Google Drive, Cloudinary MP4 qua Plyr) & Đọc tài liệu (PDF, Office Viewer), Khóa bài theo lịch mở (Drip Content `unlock_at` - wipe media URL server-side), Quản lý lịch mở & hạn nộp bài (`schedule.html`), Lưu trữ tiến độ học vào DB PostgreSQL (`lesson_completions`), Nút hoàn thành bài học tương tác tức thì, Widget tiến độ % và hạn nộp sắp tới, Khối gợi ý Nâng cấp trọn bộ Full Course (Upsell) tự động tính bù chênh lệch trên `my-courses`. | `LessonCompletion`, `LessonCompletionRepository`, `CohortScheduleRepository`, `CohortServiceImpl`, `CheckoutController`, `CourseUpsellDTO`, `module-learn.html`, `my-courses.html`, `checkout/index.html` | **100% Hoàn thành** |
| **7. Ghi danh & Thanh toán Đơn hàng (Checkout, Order & VietQR SePay)** | `UC-22`, `UC-34`: Luồng đăng ký mua lẻ từng Module hoặc mua trọn gói Full Course ưu đãi combo, lưu vết bảng `orders` (`orderCode`, `totalAmount`, `status = PENDING`, `items` JSONB), tích hợp thanh toán VietQR tự động qua SePay Webhook IPN, kích hoạt `CohortMember` & `Enrollment` khi thành công, Lịch sử đơn hàng học viên (`/orders`, `/orders/{orderCode}`), và Trang quản trị đối soát phê duyệt thủ công cho Admin & Finance (`/admin/orders`). | `Order`, `OrderRepository`, `OrderService`, `OrderServiceImpl`, `CheckoutController`, `OrderController`, `AdminOrderController`, `SepayWebhookApiController`, `templates/checkout/payment.html`, `templates/orders/`, `templates/admin/orders/` | **100% Hoàn thành** |
| **8. Bảo vệ Giao diện Online Judge (UI Guard)** | `UC-26` -> `UC-29`: Điều hướng an toàn mục Online Judge trên Sidebar và Navbar, hiển thị badge `Coming Soon` và trang Roadmap Phase 2 chống lỗi 404 khi demo/bảo vệ đồ án. | `OjController`, `oj/problems.html`, `fragments/sidebar.html`, `fragments/navbar.html`, `system-menus.json` | **100% Hoàn thành** |

---

## PHẦN 2: DANH SÁCH CÁC TÍNH NĂNG CHƯA LÀM (CHIA THEO MỨC ĐỘ ƯU TIÊN THỰC TẾ)

---

### 🔴 MỨC ĐỘ 1: QUAN TRỌNG CAO – NGHIỆP VỤ TRỌNG TÂM CHUẨN DOANH NGHIỆP (HIGH / P1)
*Lý do quan trọng: Hoàn thiện phân hệ Đánh giá Học tập (`Quiz & Question`) chuẩn chỉnh theo Use Case.*

#### 1.1. Hệ thống Đơn hàng (Order) & Cổng Thanh toán Điện tử VietQR SePay (`UC-22`, `UC-34`) - [ĐÃ HOÀN THÀNH 100%]
- **Kết quả thực hiện:**
  - `Order.java` & `OrderRepository.java`: Mở rộng trường `paidAt`, định dạng tiền tệ VNĐ và thời gian hiển thị; bổ sung các phương thức truy vấn `findByOrderCode`, `findByUserOrderByCreatedAtDesc`, `findByStatusOrderByCreatedAtDesc`, và cập nhật tính tổng doanh thu tính cả trạng thái `SUCCESS` và `COMPLETED`.
  - `OrderService.java` & `OrderServiceImpl.java`: 
    - Sinh mã đơn hàng duy nhất dạng `UTE...` ngắn gọn, an toàn cho cú pháp chuyển khoản ngân hàng.
    - Lưu cấu trúc `items` chuẩn JSONB cho cả mua lẻ Module và mua trọn gói Full Course (hỗ trợ tính toán bù trừ chênh lệch Upsell).
    - Tạo đơn hàng trạng thái `PENDING`, hỗ trợ đơn miễn phí (`FREE`) kích hoạt ngay `SUCCESS`.
    - Xử lý Webhook SePay IPN: Kiểm tra API Key bảo mật, trích xuất mã đơn hàng qua biểu thức chính quy Regex, kiểm tra đối soát số tiền và tự động chuyển trạng thái `SUCCESS`.
    - Cơ chế kích hoạt tự động: Tự động ghi danh học viên vào `CohortMember`, cập nhật sĩ số lớp học `currentEnrolled`, và tạo bản ghi `Enrollment` khóa học (đảm bảo tính lũy kế Idempotent không ghi danh trùng).
    - Sinh liên kết mã VietQR tự động qua SePay QuickLink chuẩn ngân hàng.
  - `CheckoutController.java`: Nâng cấp điều hướng chọn phương thức thanh toán VietQR (`/{cohortId}/initiate`, `/course/{slug}/initiate`), màn hình thanh toán VietQR (`/payment/{orderCode}`) với trình thăm dò trạng thái thời gian thực qua AJAX (Realtime Polling 3s/lần) tự động chuyển hướng khi hoàn tất, và nút mô phỏng thanh toán (Test Demo Mode).
  - `SepayWebhookApiController.java`: REST API `/api/webhooks/sepay` tiếp nhận IPN từ SePay và `/api/orders/{orderCode}/status` phục vụ polling.
  - `OrderController.java`: Màn hình Lịch sử Đơn hàng học viên (`/orders`) và Chi tiết Hóa đơn in ấn điện tử (`/orders/{orderCode}`).
  - `AdminOrderController.java`: Phân hệ Quản trị Đơn hàng & Đối soát dành cho `ADMIN` và vai trò mới `FINANCE` (`/admin/orders`) có thống kê doanh thu, lọc trạng thái, tìm kiếm, modal Xác nhận thanh toán thủ công (`manualConfirmPayment`) và Hủy đơn hàng.
  - `DataSeeder.java` & `SecurityConfig.java`: Cấp quyền `ORDER_MANAGE`, tạo vai trò `FINANCE`, tài khoản mẫu `finance@utelearn.edu.vn`, và whitelist an toàn webhook endpoint.
  - Kiểm thử: `OrderFeatureTest.java` (10/10 unit tests passed); toàn bộ test suite dự án 104/104 tests passed.

#### 1.2. Hệ thống Bài kiểm tra Trắc nghiệm – Quiz & Question (`UC-24`, `UC-37`) - [ĐÃ HOÀN THÀNH 100%]
- **Kết quả thực hiện:**
  - `QuizRepository.java`, `QuestionRepository.java`, `QuizSubmissionRepository.java`: Đầy đủ các phương thức truy vấn JPA, đếm lượt làm bài, lấy điểm cao nhất và lịch sử bài nộp.
  - `QuizService.java` & `QuizServiceImpl.java`: 
    - Quản lý cấu hình `Quiz`: Thời gian làm bài (`durationMinutes`), Điểm đạt (`passingScore`), Số lần làm tối đa (`maxAttempts`), Xáo trộn câu hỏi (`shuffleQuestions`).
    - Hỗ trợ đầy đủ **4 dạng câu hỏi cốt lõi** lưu trữ linh hoạt bằng `JSONB`:
      1. Trắc nghiệm 1 đáp án (`SINGLE_CHOICE`).
      2. Trắc nghiệm nhiều đáp án (`MULTI_SELECT`).
      3. Đúng / Sai (`TRUE_FALSE`).
      4. Điền từ vào chỗ trống (`FILL_BLANK`).
    - Chấm điểm tự động 100% trên Server theo chuẩn thang điểm 10.0, tự động phân tích chi tiết từng câu hỏi đúng/sai, lưu vết bài làm vào `quiz_submissions`.
    - **Cơ chế tự động hoàn thành bài học:** Khi học viên đạt điểm sàn (`score >= passingScore`), hệ thống tự động ghi nhận vào `lesson_completions` và cập nhật tức thì tiến độ học tập trên giao diện.
  - `InstructorQuizApiController.java`: REST API `/api/instructor/quizzes/**` phân quyền `INSTRUCTOR`/`ADMIN` cho phép lấy cấu hình, cập nhật tham số bài thi và CRUD ngân hàng câu hỏi.
  - `QuizApiController.java`: REST API `/api/quizzes/**` phục vụ học viên truy vấn thông tin quiz, bắt đầu bấm giờ làm bài (ẩn cờ `isCorrect`), nộp bài thi, xem lịch sử và tra cứu chi tiết lời giải.
  - `module-learn.html`: 
    - Giao diện làm bài thi trực tuyến hiện đại: Tổng quan bài thi, thống kê điểm cao nhất và số lượt làm, đồng hồ đếm ngược thời gian thực, bảng điều hướng câu hỏi nhanh (Palette), form làm bài tương tác trực quan cho từng loại câu hỏi.
    - Màn hình kết quả vinh danh điểm số, vòng tròn tiến độ đạt/chưa đạt, khối xem lại chi tiết bài làm với đáp án đúng, lựa chọn của học viên và lời giải thích.
  - `curriculum.html` & `module-curriculum.html`: Nút "Soạn đề" Quiz chuyên dụng, Modal cấu hình tham số bài thi và trình soạn ngân hàng câu hỏi linh hoạt; tính năng tải mẫu Excel, nhập/xuất câu hỏi hàng loạt (.xlsx); tính năng thay đổi thứ tự câu hỏi trực quan bằng kéo thả (SortableJS) hoặc các nút mũi tên Up/Down.
  - `DataSeeder.java`: Tự động nạp dữ liệu bài kiểm tra mẫu OOP & Java Core với đầy đủ 4 dạng câu hỏi chuẩn mực; tự động migrate cột `order_index`.
  - Kiểm thử: `QuizFeatureTest.java` (27/27 unit tests passed); toàn bộ test suite dự án 147/147 tests passed.

---

### 🟡 MỨC ĐỘ 2: TRUNG BÌNH – HOÀN THIỆN CHI TIẾT TƯƠNG TÁC & QUẢN TRỊ (MEDIUM / P2)
*Lý do: Nâng cấp trải nghiệm tương tác thời gian thực, thúc đẩy chuyển đổi mua trọn gói khóa học và hoàn thiện các nghiệp vụ kiểm duyệt theo SLA (Phân hệ Quản trị Danh mục UC-11 đã hoàn thành 100%).*

#### 2.1. Phòng Chat Thảo luận Lớp học Thời gian thực qua WebSocket (`UC-35`) - [ĐÃ HOÀN THÀNH 100%]
- **Hiện trạng trong code:**
  - Đã khai báo thư viện `spring-boot-starter-websocket` trong `pom.xml`, cấu hình STOMP `/ws`, `/app`, `/topic` trong `application.yml` và có sẵn Entity `CohortMessage.java`.
  - **Chưa có:** `WebSocketConfig.java`, `CohortChatController.java`, `CohortMessageRepository.java` và khung giao diện chat realtime.
- **Cần code những gì:**
  - Cấu hình STOMP WebSocket endpoint `/ws`, kiểm tra quyền thành viên Cohort (`cohort_members`), broadcast tin nhắn realtime qua `/topic/cohort/{cohortId}` và lưu trữ lịch sử tin nhắn vào bảng `cohort_messages`.
  - Thiết kế tab thảo luận / khung chat nổi trực quan trong phòng học `module-learn.html`.

#### 2.2. Gợi ý Nâng cấp trọn bộ Full Course (Upsell) trên Khóa học của tôi (`UC-25`) - [ĐÃ HOÀN THÀNH 100%]
- **Kết quả thực hiện:**
  - `CourseUpsellDTO.java` & `UserEnrollmentSummaryDTO.java`: Định nghĩa cấu trúc DTO gợi ý nâng cấp, tính toán chi tiết giá Full Course, tổng tiền đã thanh toán các module trước đó, số tiền nâng cấp bù chênh lệch (`Số tiền nâng cấp = Giá Full Course - Tổng số tiền đã thanh toán cho các Cohort trước đó`), danh sách module còn lại và số tiền tiết kiệm so với mua lẻ.
  - `CohortServiceImpl.java`: Mở rộng `getUserEnrollmentSummary` tự động phát hiện các khóa học học viên đã mua lẻ một số module, tính toán chính xác công thức bù chênh lệch và đính kèm danh sách `upsellSuggestions`.
  - `CourseController.java`: Đưa `upsellSuggestions` vào Model cho trang `my-courses.html`.
  - `CheckoutController.java`: Trong `showCourseCheckoutPage`, phát hiện trạng thái nâng cấp (`isUpgrade = true`), áp dụng đúng công thức bù chênh lệch thay vì tính nguyên giá, hiển thị số tiền đã khấu trừ; `processCoursePayment` tự động kích hoạt tất cả các module còn lại vào `cohort_members`.
  - Giao diện `courses/my-courses.html`: Thẻ banner Upsell sang trọng tại tab "Module & Học phần lẻ" có thanh tiến độ module sở hữu, huy hiệu tiết kiệm, bảng đối chiếu giá bù chênh lệch và nút hành động "Nâng cấp ngay".
  - Giao diện `checkout/index.html`: Cập nhật bảng kê thanh toán hiển thị rõ giá gốc, số tiền đã trừ 100% từ các module cũ, số tiền bù thêm thực tế và số tiền tiết kiệm.
  - Kiểm thử: `CourseUpsellFeatureTest.java` đạt 5/5 unit tests passed; toàn bộ test suite dự án 86/86 passed.

#### 2.3. Ràng buộc Kiểm duyệt Nội dung theo SLA & Ca trực (`UC-18`, `UC-19`) - [ĐÃ HOÀN THÀNH 100%]
- **Kết quả thực hiện:**
  - `ModerationServiceImpl.java`: Bổ sung tính toán SLA deadline 48 giờ (`deadlineAt`) khi tạo task, thêm Guard Clauses chặn Giảng viên tự duyệt khóa học của chính mình (`authorId != moderatorId`), bắt buộc Moderator phải có ca trực hoạt động (`isActive = true`) trong ngày (Admin được bypass), và tự động cộng dồn số bài duyệt thành công (`completedTasksCount`) vào bảng `moderator_attendances`.
  - `ModeratorController.java`: Kiểm tra ca trực của Moderator trước khi truy cập hàng đợi `/moderator/queue` và trang chi tiết `/moderator/review/{taskId}`, cảnh báo và chuyển hướng sang điểm danh nếu chưa check-in; truyền cờ `isSelfAuthored` để phát hiện tự kiểm duyệt.
  - `ModeratorApiController.java`: Bổ sung `@Slf4j` và try-catch xử lý phản hồi lỗi nghiệp vụ HTTP 400 Bad Request kèm thông điệp rõ ràng cho client.
  - `CourseController.java` & `InstructorCurriculumApiController.java`: Phân định rạch ròi giữa quyền biên tập giáo trình (`canEdit` - chỉ dành cho Giảng viên sở hữu và Quản trị viên) và quyền thanh tra nội dung (`hasAccessRole` - dành cho Kiểm duyệt viên). Ẩn hoàn toàn công tắc chuyển chế độ chỉnh sửa, chặn API inline editing đối với Kiểm duyệt viên; đồng thời cho phép Kiểm duyệt viên vào phòng học (`/courses/{slug}/learn`, `/modules/{id}/learn`) mà không yêu cầu ghi danh.
  - `AssetDownloadController.java` & `review_course.html`: Cấp quyền thẩm định tải tài liệu/file đính kèm qua Backend Security Proxy cho vai trò `MODERATOR`; bổ sung liên kết trực tiếp "Xem nội dung" và nút tải tài nguyên thẩm định trên giao diện thẩm định.
  - Giao diện: `queue.html` hiển thị trạng thái ca trực và cột hạn SLA; `review_course.html` hiển thị banner cảnh báo chống tự kiểm duyệt, nút truy cập nội dung bài học và vô hiệu hóa các nút duyệt nếu vi phạm; toast thông báo từ response JSON.
  - Kiểm thử: `ModerationFeatureTest.java` (8/8 tests) và `CourseEditPermissionFeatureTest.java` (7/7 tests) passed; toàn bộ test suite dự án 94/94 tests passed.

---

### 🟢 MỨC ĐỘ 3: ĐỊNH HƯỚNG PHÁT TRIỂN TƯƠNG LAI (FUTURE SCOPE / P3 – KHÔNG CODE)
*Đúng theo đặc tả trong `UC06_OnlineJudge.md` và `UTELearn_SRS.md`:*

- **Phân hệ 8 – Online Judge Sandbox (`UC-26`, `UC-27`, `UC-28`, `UC-29`):**
  - Soạn bài tập lập trình (`CodingProblem`), nộp mã nguồn (`CodingSubmission`), biên dịch và chấm tự động qua Judge0 Microservice / Docker Sandbox.
  - **Hành động:** Giữ nguyên Database Schema (`coding_problems`, `coding_submissions`), router an toàn `/oj/problems` và tài liệu Use Case để trình bày trong chương **"Định hướng phát triển (Future Work / Phase 2)"** của báo cáo đồ án tốt nghiệp/khóa luận.
