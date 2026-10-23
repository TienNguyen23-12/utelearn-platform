# UTELearn – Tài Liệu Đặc Tả Use Case Hệ Thống

Thư mục này chứa toàn bộ các biểu đồ Use Case (sử dụng Mermaid Diagram) và bản đặc tả chi tiết cho nền tảng học trực tuyến **UTELearn** (Trường Đại học Sư phạm Kỹ thuật TP.HCM - HCMUTE).

---

## 1. Cấu trúc Tài liệu Use Case

| Tệp tin | Phân hệ chức năng | Mô tả trọng tâm |
|---------|-------------------|-----------------|
| [`UC00_TongQuan.md`](./UC00_TongQuan.md) | Tổng quan Hệ thống | Sơ đồ Use Case toàn cảnh, kiến trúc phân cấp Course - Cohort, danh mục 37 Use Case. |
| [`UC01_Authentication.md`](./UC01_Authentication.md) | Xác thực & Phân quyền | Đăng nhập/Đăng ký, Google OAuth2, JWT Cookie, Quên & Đặt lại mật khẩu, Hồ sơ cá nhân. |
| [`UC02_QuanTriHeThong.md`](./UC02_QuanTriHeThong.md) | Quản trị Hệ thống (Admin) | Quản lý người dùng, cấu hình phân quyền động RBAC, Dashboard thống kê, Danh mục, Giám sát ca trực Moderator. |
| [`UC03_QuanLyKhoaHoc.md`](./UC03_QuanLyKhoaHoc.md) | Quản lý Khóa học (Course) | Tạo khóa học lớn (Master Course), biên soạn toàn bộ giáo trình chuẩn (Section, Lesson, Quiz), gửi kiểm duyệt xuất bản. |
| [`UC04_KiemDuyetNoiDung.md`](./UC04_KiemDuyetNoiDung.md) | Kiểm duyệt Nội dung | Hàng đợi kiểm duyệt SLA, phê duyệt/từ chối Course & Cohort, chống tự kiểm duyệt, phản hồi góp ý. |
| [`UC05_HocTrucTuyen.md`](./UC05_HocTrucTuyen.md) | Học trực tuyến (Student) | Duyệt & tìm kiếm Course, đăng ký ghi danh theo Đợt học (Cohort), học video/tài liệu, làm Quiz, theo dõi tiến độ. |
| [`UC06_OnlineJudge.md`](./UC06_OnlineJudge.md) | Online Judge (OJ) | **Định hướng phát triển**: Luyện tập thuật toán, nộp mã nguồn chấm tự động bằng Sandbox, quản lý test case. |
| [`UC07_QuanLyTaiNguyen.md`](./UC07_QuanLyTaiNguyen.md) | Kho Tài nguyên số (Assets) | Quản lý kho tệp media giảng dạy cá nhân của Giảng viên (Video, PDF, Slide), tải tài liệu bài giảng. |
| [`UC08_QuanLyLopHocDot.md`](./UC08_QuanLyLopHocDot.md) | Quản lý Đợt học (Cohort) | Mở Đợt/Lớp học mới cho Course, thiết lập học phí đợt, sĩ số, điều phối lịch mở bài (Drip Content) & deadline, Chat WebSocket. |

---

## 2. Mô hình Nghiệp vụ Course - Cohort

Hệ thống thiết kế theo mô hình **Khóa học Khung & Lớp học theo Đợt (Master Course & Cohort-based Learning)**:
1. **Course (Khóa học lớn / Khung giáo trình chuẩn)**:
   - Là chương trình học tổng thể hoàn chỉnh do Giảng viên biên soạn một lần.
   - Chứa thông tin định danh: Tiêu đề, Mã khóa học, Danh mục, Cấp độ, Mục tiêu, Yêu cầu, Ảnh bìa.
   - Chứa toàn bộ cây giáo trình chuẩn: Các Chương (`Section`), Bài giảng (`Lesson`), Bài kiểm tra (`Quiz`), Bài tập lập trình (`CodingProblem`).
   - Được gửi qua Ban Kiểm duyệt (Moderator) để phê duyệt chất lượng trước khi được phép mở các đợt học.
2. **Cohort (Đợt học / Lớp học thành phần cụ thể)**:
   - Nằm trực thuộc một Course cha cụ thể; một Course có thể mở nhiều Cohort theo từng giai đoạn tuyển sinh.
   - Mỗi Cohort đại diện cho một lớp học thực tế: Tên lớp (VD: Lớp K24-01), Mã lớp, Học phí của đợt (`price`), Sĩ số tối đa (`max_capacity`), Thời gian mở/đóng ghi danh và Thời gian học tập.
   - Kế thừa toàn bộ giáo trình chuẩn từ Course cha, đồng thời điều phối lịch học riêng: Thời điểm mở bài (`unlock_at`), Hạn nộp bài (`deadline_at`), Chế độ công bố điểm (`show_score_type`).
   - Mỗi Cohort tích hợp không gian thảo luận thời gian thực qua WebSocket (STOMP/SockJS) dành riêng cho các thành viên của lớp đó.
3. **Quy trình Đăng ký & Học tập**:
   - **Tìm kiếm & Chọn khóa học**: Học viên duyệt danh mục Course tổng thể, xem thông tin giáo trình và các bài học xem trước miễn phí (`is_free_preview`).
   - **Ghi danh theo Đợt học (Cohort)**: Học viên chọn Đợt học (Cohort) phù hợp với thời gian biểu của bản thân trong danh sách các đợt đang mở tuyển sinh để tiến hành thanh toán/ghi danh.
   - **Học tập & Tương tác**: Học viên học theo lộ trình mở bài (Drip Content), nộp bài kiểm tra đúng hạn chót của Cohort, và tương tác trực tiếp với Giảng viên, Trợ giảng trong phòng chat của lớp.

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
