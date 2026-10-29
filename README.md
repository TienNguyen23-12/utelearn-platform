# 🎓 UTELearn - Nền Tảng Học Trực Tuyến Theo Đợt & Quản Trị Giáo Dục Doanh Nghiệp

> **Đồ án cuối kỳ môn Lập trình Web** - Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE)  
> Hệ thống giáo dục trực tuyến kết hợp đào tạo theo đợt học (Cohort-Based Learning), tích hợp thanh toán VietQR đối soát tự động, kiểm duyệt đa cấp, sổ điểm Excel và tính lương giảng viên/kiểm duyệt viên.

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-blue.svg)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🌟 Điểm Sáng Nổi Bật & Công Nghệ Chuẩn Doanh Nghiệp

1. **Mô Hình Đào Tạo Theo Đợt Học (Cohort-Based Learning)**: Quản lý lớp học có lịch trình biểu, giới hạn sĩ số, theo dõi tiến độ bài học và phòng trao đổi trực tiếp thời gian thực.
2. **Thanh Toán VietQR & Đối Soát SePay Webhook**: Tự động tạo mã QR động ngân hàng chuẩn NAPAS; tiếp nhận Webhook đối soát số dư thời gian thực và tự động cộng tiền vào Ví UTELearn.
3. **Phòng Chat Lớp Học & Thông Báo Real-time (WebSocket / STOMP)**: Giao tiếp hai chiều tức thời bằng SockJS + STOMP Broker cho từng đợt học và kênh thông báo đẩy riêng biệt cho từng người dùng.
4. **Sổ Điểm Điện Tử & Import/Export Excel (Apache POI)**: Tự động tổng hợp điểm chuyên cần, quiz, bài tập; xuất bảng điểm chuẩn `.xlsx` và nhập điểm hàng loạt với cơ chế kiểm tra lỗi dữ liệu chặt chẽ.
5. **Chứng Chỉ Số Có Thể Xác Minh Toàn Cầu**: Cấp chứng chỉ tự động với mã định danh duy nhất, cho phép quét QR tra cứu công khai và đánh giá khóa học gắn liền với chứng chỉ.
6. **Kiểm Duyệt Khóa Học Đa Cấp (Content Moderation Workflow)**: Phân luồng kiểm duyệt nội dung: Giảng viên nộp -> Kiểm duyệt viên đánh giá/phản hồi/leo thang -> Quản trị viên phê duyệt cuối.
7. **Hệ Thống Chấm Công & Tính Lương (Payroll & Attendance Engine)**: Quản lý ca trực, Check-in/Check-out, làm thêm giờ (OT) của Moderator; tính toán tự động tỷ lệ chia sẻ doanh thu (Revenue Share) cho Giảng viên và tính lương theo phút/ca/thưởng SLA cho Kiểm duyệt viên.
8. **Tải Lên Trực Tiếp Ký Số Cloudinary (Direct Signed Uploads)**: Máy chủ tạo chữ ký SHA-1 cho phép trình duyệt tải trực tiếp video, bài giảng, tài liệu lên CDN Cloudinary, giải phóng 100% băng thông server.
9. **Cơ Chế Tự Duy Trì Máy Chủ Đám Mây (Self Keep-Alive Service)**: Tự động kích hoạt luồng HTTP Ping định kỳ giúp ứng dụng không bị đóng băng (Cold Start) khi triển khai trên hạ tầng PaaS.

---

## 👥 Phân Chia Nghiệp Vụ Thực Hiện (2 Thành Viên)

| Thành viên | Luồng Nghiệp Vụ Trọng Tâm | Chi Tiết Chức Năng Chính |
| :--- | :--- | :--- |
| **Thành viên A** | **1. Đợt học & Học tập tương tác thời gian thực** | - Vận hành đợt học (Cohort): Sĩ số, lịch trình, trạng thái động (`UPCOMING`, `OPENING`, `ONGOING`, `CLOSED`).<br>- Phòng chat lớp học WebSocket (STOMP/SockJS) theo đợt học.<br>- Ghi chú bài học thông minh gắn mốc thời gian video (`timestamp`). |
| | **2. Sổ điểm tự động & Xử lý Excel doanh nghiệp** | - Tính toán điểm tổng hợp tự động từ bài học, trắc nghiệm và bài tập.<br>- Xuất danh sách sổ điểm lớp học/khóa học ra tệp Excel `.xlsx` (Apache POI).<br>- Nhập sổ điểm từ Excel kèm báo cáo lỗi dữ liệu theo từng dòng. |
| | **3. Đánh giá năng lực & Hệ thống Khảo thí** | - Ngân hàng câu hỏi trắc nghiệm, tạo đề thi ngẫu nhiên, chấm điểm tự động.<br>- Nộp bài tập tự luận (tập tin đính kèm/link) và chấm điểm kèm tiêu chí phản hồi. |
| | **4. Chứng chỉ số & Sân chơi Lập trình (OJ)** | - Cấp chứng chỉ số tự động khi đạt điều kiện hoàn thành khóa học.<br>- Trang tra cứu và xác thực chứng chỉ công khai (`/certificates/verify?code=...`).<br>- Sân chơi thực hành thuật toán lập trình trực tuyến (Online Judge playground). |
| **Thành viên B** | **1. Tài chính, Ví điện tử & Cổng VietQR SePay** | - Tích hợp sinh mã VietQR động theo đơn hàng nạp ví/thanh toán.<br>- Endpoint tiếp nhận Webhook SePay đối soát biến động số dư ngân hàng tự động.<br>- Quản lý Ví UTELearn, thanh toán khóa học bằng số dư ví và mã giảm giá (Discount Engine). |
| | **2. Quy trình Kiểm duyệt Nội dung Đa cấp** | - Luồng kiểm duyệt khóa học và đợt học: Nộp duyệt -> Tiếp nhận -> Duyệt/Từ chối có lý do -> Leo thang Admin.<br>- Theo dõi và xử lý vi phạm thời gian cam kết dịch vụ (SLA) của kiểm duyệt viên. |
| | **3. Chấm công & Tính toán Lương Doanh nghiệp** | - Hệ thống chấm công ca trực, Check-in / Check-out, làm thêm giờ (OT) của Moderator.<br>- Công thức tính bảng lương tự động: Chia sẻ doanh thu cho Giảng viên; tính lương theo phút/ca/thưởng cho Kiểm duyệt viên kèm ghi đè cá nhân. |
| | **4. Quản lý Tài nguyên Số Chữ Ký Bảo Mật** | - Tạo chữ ký số Cloudinary Signature để upload tệp trực tiếp an toàn.<br>- Kho tài nguyên số giảng viên với 4 cấp phân quyền: `PRIVATE`, `COHORT_PUBLIC`, `DEPARTMENT_SHARED`, `DIRECT_EMAIL_SHARED`. |
| | **5. Bảo mật Đa tầng & Phân quyền RBAC Động** | - Đăng nhập tài khoản & Google OAuth2 một chạm.<br>- Xác thực phân quyền kết hợp JWT Cookie + Session và chống giả mạo kép CSRF.<br>- Quản lý vai trò (Role) và quyền hạn (Permission) động từ giao diện Quản trị. |

---

## 🛠️ Yêu Cầu Môi Trường (Prerequisites)

* **Java Development Kit (JDK)**: Phiên bản 21 trở lên (khuyến nghị Eclipse Temurin hoặc Oracle OpenJDK 21).
* **Apache Maven**: Phiên bản 3.9+ (hoặc sử dụng wrapper `mvnw` đi kèm).
* **Cơ sở dữ liệu**: PostgreSQL 15+ (hoặc PostgreSQL chạy qua Docker).
* **Trình duyệt**: Chrome, Edge, Firefox phiên bản hiện đại hỗ trợ WebSocket.

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Tại Máy Local

### Bước 1: Khởi tạo Cơ sở dữ liệu PostgreSQL
Mở pgAdmin hoặc PostgreSQL CLI và tạo một database mới:
```sql
CREATE DATABASE utelearn_db;
```

### Bước 2: Cấu hình Biến Môi Trường hoặc `application.yml`
Ứng dụng có thể cấu hình trực tiếp qua file `src/main/resources/application.yml` hoặc thông qua các biến môi trường (Environment Variables). 

Ví dụ thiết lập biến môi trường trên Windows PowerShell:
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/utelearn_db"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="your_postgres_password"
$env:DEFAULT_SEED_PASSWORD="Abc@1234"
```

*(Ghi chú: Nếu không thiết lập các dịch vụ bên ngoài như SePay, Cloudinary hay Google OAuth2, hệ thống vẫn hoạt động đầy đủ ở chế độ Simulation / Demo).*

### Bước 3: Biên dịch & Khởi chạy Ứng dụng

Chạy lệnh Maven để tải thư viện, biên dịch và khởi động máy chủ Spring Boot:
```bash
# Biên dịch mã nguồn
mvn clean compile

# Khởi chạy ứng dụng
mvn spring-boot:run
```

Khi màn hình console xuất hiện dòng thông báo:
```text
Started UtelearnApplication in X.XXX seconds
```
Truy cập hệ thống tại địa chỉ: **`http://localhost:8080`**

---

## 🔑 Tài Khoản Trải Nghiệm Mặc Định (Seed Users)

Hệ thống tích hợp sẵn bộ dữ liệu mẫu (`DataSeeder`) tự động khởi tạo các vai trò khi ứng dụng chạy lần đầu. Mật khẩu mặc định cho toàn bộ tài khoản bên dưới là: **`Abc@1234`**

| Vai trò (Role) | Email đăng nhập | Quyền hạn chính |
| :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin@utelearn.edu.vn` | Quản lý toàn bộ hệ thống, đơn hàng, phân quyền, bảng lương, cài đặt. |
| **Tài chính (Finance)** | `finance@utelearn.edu.vn` | Quản lý hóa đơn, duyệt hoàn tiền, xem báo cáo doanh thu và bảng lương. |
| **Giảng viên (Instructor)** | `instructor@utelearn.edu.vn` | Biên soạn khóa học, mở đợt học, chấm bài, tải tài nguyên, xuất sổ điểm. |
| **Kiểm duyệt (Moderator)** | `moderator@utelearn.edu.vn` | Chấm công ca trực, kiểm duyệt nội dung bài giảng, duyệt đợt học mới. |
| **Học viên (Student)** | `student@utelearn.edu.vn` | Đăng ký khóa học, nạp ví, làm bài thi, chat đợt học, nhận chứng chỉ. |

---

## 📑 Tài Liệu API (Swagger / OpenAPI)

Sau khi ứng dụng khởi chạy thành công tại local, truy cập giao diện kiểm thử API tương tác tại:
* **Swagger UI**: `http://localhost:8080/swagger-ui.html`
* **OpenAPI Docs**: `http://localhost:8080/v3/api-docs`

---

## 📂 Cấu Trúc Thư Mục Dự Án

```text
utelearn/
├── src/main/java/vn/edu/ute/utelearn/
│   ├── config/          # Cấu hình Spring Security, WebSocket, Cloudinary, Seeder
│   ├── controller/      # Web MVC Controller & REST API Controllers (Admin, Instructor, Moderator)
│   ├── dao/             # Data Access Object (Spring Data JPA Repositories)
│   ├── dto/             # Data Transfer Objects
│   ├── entity/          # JPA Domain Entities
│   ├── security/        # JWT Authentication, OAuth2 Google, Custom Handlers
│   ├── service/         # Interfaces nghiệp vụ & Services Implementation
│   └── util/            # Tiện ích tiền tệ, ngày tháng, mã hóa bảo mật
├── src/main/resources/
│   ├── templates/       # Giao diện Thymeleaf SSR (Admin, Học viên, Giảng viên, Đơn hàng)
│   ├── static/          # CSS, JS, hình ảnh, vendor assets
│   └── application.yml  # File cấu hình trung tâm của ứng dụng
├── src/test/java/       # 30 bộ kiểm thử tự động (Unit & Integration Tests)
└── pom.xml              # Maven dependencies & build configuration
```

---

## 📄 Bản Quyền & Giấy Phép
Dự án được xây dựng phục vụ mục đích nghiên cứu và bảo vệ đồ án tốt nghiệp/cuối kỳ ngành Công nghệ Thông tin - HCMUTE.
Mã nguồn phát hành theo giấy phép [MIT License](LICENSE).
