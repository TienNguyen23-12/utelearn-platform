# UTELearn – Tài Liệu Đặc Tả Use Case Hệ Thống

Thư mục này chứa toàn bộ các biểu đồ Use Case (sử dụng Mermaid Diagram) và bản đặc tả chi tiết cho nền tảng học trực tuyến **UTELearn** (Trường Đại học Sư phạm Kỹ thuật TP.HCM - HCMUTE).

---

## 1. Cấu trúc Tài liệu Use Case

| Tệp tin | Phân hệ chức năng | Mô tả trọng tâm |
|---------|-------------------|-----------------|
| [`UC00_TongQuan.md`](./UC00_TongQuan.md) | Tổng quan Hệ thống | Sơ đồ Use Case toàn cảnh, kiến trúc phân cấp Course - Cohort, danh mục 37 Use Case. |
| [`UC01_Authentication.md`](./UC01_Authentication.md) | Xác thực & Phân quyền | Đăng nhập/Đăng ký, Google OAuth2, JWT Cookie, Quên & Đặt lại mật khẩu, Hồ sơ cá nhân. |
| [`UC02_QuanTriHeThong.md`](./UC02_QuanTriHeThong.md) | Quản trị Hệ thống (Admin) | Quản lý người dùng, cấu hình phân quyền động RBAC, Dashboard thống kê, Danh mục, Giám sát ca trực Moderator. |
| [`UC03_QuanLyKhoaHoc.md`](./UC03_QuanLyKhoaHoc.md) | Quản lý Khóa học (Course) | Tạo khóa học (Container), quản lý danh sách Cohort, cấu hình giá trọn gói Full Course, gửi kiểm duyệt. |
| [`UC04_KiemDuyetNoiDung.md`](./UC04_KiemDuyetNoiDung.md) | Kiểm duyệt Nội dung | Hàng đợi kiểm duyệt SLA, phê duyệt/từ chối Course & Cohort, chống tự kiểm duyệt, phản hồi góp ý. |
| [`UC05_HocTrucTuyen.md`](./UC05_HocTrucTuyen.md) | Học trực tuyến (Student) | Duyệt & tìm kiếm, mua Full Course hoặc từng Cohort lẻ, nâng cấp bù giá chênh lệch, học video/tài liệu, làm Quiz. |
| [`UC06_OnlineJudge.md`](./UC06_OnlineJudge.md) | Online Judge (OJ) | **Định hướng phát triển**: Luyện tập thuật toán, nộp mã nguồn chấm tự động bằng Sandbox, quản lý test case. |
| [`UC07_QuanLyTaiNguyen.md`](./UC07_QuanLyTaiNguyen.md) | Kho Tài nguyên số (Assets) | Quản lý kho tệp media giảng dạy cá nhân của Giảng viên (Video, PDF, Slide), tải tài liệu bài giảng. |
| [`UC08_QuanLyLopHocDot.md`](./UC08_QuanLyLopHocDot.md) | Quản lý Cohort (Lớp học / Module) | Tạo Cohort, thiết lập giá lẻ, sĩ số, soạn giáo trình Section/Lesson/Quiz cho Cohort, lịch học, Chat thời gian thực (WebSocket). |

---

## 2. Mô hình Nghiệp vụ Course - Cohort

Hệ thống thiết kế theo mô hình **Khóa học phân cấp (Course-Cohort Architecture)**:
1. **Course (Khóa học lớn)**:
   - Là chương trình tổng thể, đóng vai trò bao bọc (Container) cho các Cohort.
   - Chứa thông tin định danh: Tiêu đề, Mã khóa học, Danh mục, Cấp độ, Mục tiêu, Yêu cầu.
   - Thiết lập **Giá Full Course (Gói trọn bộ ưu đãi)** để khuyến khích học viên đăng ký toàn bộ lộ trình.
2. **Cohort (Lớp học / Module thành phần)**:
   - Nằm trực thuộc một Course cụ thể.
   - Mỗi Cohort có: Tên lớp, Mã lớp, Giá bán riêng lẻ (`price`), Sĩ số tối đa (`max_capacity`), Thời gian đào tạo.
   - Nội dung học tập (Section, Lesson, Quiz) được tổ chức theo từng Cohort.
   - Mỗi Cohort tích hợp phòng chat thời gian thực qua WebSocket (STOMP/SockJS) để trao đổi giữa Giảng viên, Trợ giảng và Học viên.
3. **Chính sách Mua học tập linh hoạt**:
   - **Mua Full Course**: Học viên sở hữu toàn bộ các Cohort với mức giá ưu đãi trọn gói.
   - **Mua lẻ Cohort**: Học viên có thể chọn mua 1 hoặc nhiều Cohort theo nhu cầu thực tế.
   - **Nâng cấp (Upgrade)**: Học viên đã mua một số Cohort có thể nâng cấp lên Full Course bất kỳ lúc nào bằng cách thanh toán số tiền chênh lệch (`Giá Full Course - Tổng tiền đã trả trước đó`).

---

## 3. Các Tác nhân Hệ thống (Actors)

| Tác nhân (Actor) | Vai trò | Quyền hạn và Phạm vi hoạt động |
|------------------|---------|---------------------------------|
| **Guest** | Khách vãng lai | Chưa đăng nhập; chỉ được xem trang chủ, danh mục khóa học, xem thử bài giảng miễn phí (`is_free_preview`), đăng ký và đăng nhập. |
| **Student** | Học viên | Tìm kiếm, mua Full Course hoặc từng Cohort lẻ, học bài giảng, làm bài kiểm tra trắc nghiệm, theo dõi tiến độ, tham gia chat lớp qua WebSocket. |
| **Instructor** | Giảng viên | Quản lý Course, tạo các Cohort thành phần, soạn giáo trình (Section/Lesson), tạo Quiz, quản lý kho tài nguyên số, gửi duyệt nội dung, điều phối lịch học. |
| **TA** | Trợ giảng | Hỗ trợ giải đáp thắc mắc trong các Cohort được phân công qua phòng chat WebSocket, theo dõi nội dung bài học. |
| **Moderator** | Kiểm duyệt viên | Điểm danh ca trực hàng ngày, nhận nhiệm vụ từ hàng đợi SLA, đánh giá chất lượng Course và Cohorts, phê duyệt hoặc từ chối kèm phản hồi chi tiết. |
| **Admin** | Quản trị viên | Toàn quyền kiểm soát hệ thống: Quản lý người dùng, phân quyền RBAC động, xem Dashboard thống kê doanh thu, cấu hình danh mục. |

---

## 4. Ma trận Phân quyền (RBAC Matrix)

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

*(Ghi chú: V = Có quyền; - = Không có quyền)*

---

## 5. Hướng dẫn Xem và Render Sơ đồ Use Case

Tất cả sơ đồ trong tài liệu được viết bằng chuẩn cú pháp **Mermaid**:
1. **Trong VS Code / Antigravity IDE**:
   - Cài đặt extension: `Markdown Preview Mermaid Support`.
   - Mở file `.md` bất kỳ và nhấn tổ hợp phím `Ctrl + Shift + V` (hoặc click icon Preview ở góc trên bên phải).
2. **Trên GitHub / GitLab**:
   - Nền tảng tự động biên dịch và hiển thị trực quan các khối ````mermaid`.
3. **Trực tuyến qua Mermaid Live Editor**:
   - Truy cập [mermaid.live](https://mermaid.live), sao chép đoạn mã trong khối ````mermaid` và dán vào trình soạn thảo để xem hoặc xuất định dạng PNG/SVG.
