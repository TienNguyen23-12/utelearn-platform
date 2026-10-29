# 🎓 UTELearn - Nền Tảng Học Trực Tuyến Theo Đợt & Quản Trị Giáo Dục Doanh Nghiệp

> **Đồ án cuối kỳ môn Lập trình Web** - Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE)  
> Hệ thống giáo dục trực tuyến kết hợp đào tạo theo đợt học (Cohort-Based Learning), tích hợp thanh toán VietQR đối soát tự động, kiểm duyệt đa cấp, sổ điểm Excel và tính lương giảng viên/kiểm duyệt viên.

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-blue.svg)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 👥 Thành Viên Thực Hiện

| Họ và Tên | Mã số Sinh viên (MSSV) | Vai trò trong dự án |
| :--- | :--- | :--- |
| **Nguyễn Thành Tiến** | **24110352** | Trưởng nhóm / Full-stack Developer (Tài chính, Realtime, Cloud & Security) |
| **Nguyễn Đặng Cao Tường** | **24110375** | Thành viên / Full-stack Developer (Khảo thí, Sổ điểm, Kiểm duyệt & Lương) |

---

## 🌟 Điểm Sáng Nổi Bật & Công Nghệ Chuẩn Doanh Nghiệp

1. **Mô Hình Đào Tạo Theo Đợt Học (Cohort-Based Learning)**: Quản lý lớp học có lịch trình biểu, giới hạn sĩ số, theo dõi tiến độ bài học và trung tâm thảo luận trực tiếp thời gian thực.
2. **Thanh Toán VietQR & Đối Soát SePay Webhook**: Tự động tạo mã QR động ngân hàng chuẩn NAPAS; tiếp nhận Webhook đối soát số dư thời gian thực và tự động nạp tiền vào Ví UTELearn.
3. **Phòng Chat Lớp Học & Thông Báo Real-time (WebSocket / STOMP)**: Giao tiếp hai chiều tức thời bằng SockJS + STOMP Broker cho từng đợt học và kênh thông báo đẩy riêng biệt cho từng người dùng.
4. **Sổ Điểm Điện Tử & Import/Export Excel (Apache POI)**: Tự động tổng hợp điểm chuyên cần, quiz, bài tập; xuất bảng điểm chuẩn `.xlsx` và nhập điểm hàng loạt với cơ chế kiểm tra lỗi dữ liệu chặt chẽ.
5. **Chứng Chỉ Số Có Thể Xác Minh Toàn Cầu**: Cấp chứng chỉ tự động với mã định danh duy nhất, cho phép tra cứu công khai và đánh giá khóa học gắn liền với chứng chỉ.
6. **Kiểm Duyệt Khóa Học Đa Cấp (Content Moderation Workflow)**: Phân luồng kiểm duyệt nội dung: Giảng viên nộp -> Kiểm duyệt viên đánh giá/phản hồi/leo thang -> Quản trị viên phê duyệt cuối.
7. **Hệ Thống Chấm Công & Tính Lương (Payroll & Attendance Engine)**: Quản lý ca trực, Check-in/Check-out, làm thêm giờ (OT) của Moderator; tính toán tự động tỷ lệ chia sẻ doanh thu (Revenue Share) cho Giảng viên và tính lương theo phút/ca/thưởng SLA cho Kiểm duyệt viên.
8. **Tải Lên Trực Tiếp Ký Số Cloudinary (Direct Signed Uploads)**: Máy chủ tạo chữ ký SHA-1 cho phép trình duyệt tải trực tiếp video, bài giảng, tài liệu lên CDN Cloudinary, giải phóng 100% băng thông server.
9. **Cơ Chế Tự Duy Trì Máy Chủ Đám Mây (Self Keep-Alive Service)**: Tự động kích hoạt luồng HTTP Ping định kỳ giúp ứng dụng không bị đóng băng (Cold Start) khi triển khai trên hạ tầng PaaS.

---

## 📋 Bảng Phân Chia Nghiệp Vụ Cụ Thể

| Thành viên | Luồng Nghiệp Vụ Trọng Tâm | Chi Tiết Chức Năng Chính Phụ Trách |
| :--- | :--- | :--- |
| **Nguyễn Thành Tiến**<br>*(MSSV: 24110352)* | **1. Tài chính, Ví điện tử & Cổng VietQR SePay** | - Tích hợp sinh mã VietQR động theo đơn hàng nạp ví/mua khóa học.<br>- Xây dựng Webhook SePay đối soát biến động số dư ngân hàng thời gian thực.<br>- Vận hành Ví UTELearn, Giỏ hàng (Cart), Quầy thu ngân (POS) và Động cơ Voucher giảm giá (Discount Engine). |
| | **2. Trao đổi Real-time & Hệ thống Thông báo** | - Phát triển phòng chat đợt học (Cohort Chat) & Trung tâm thảo luận giảng viên qua WebSocket (STOMP/SockJS).<br>- Hệ thống thông báo đẩy toàn cục theo người dùng (`/topic/notifications/{userId}`). |
| | **3. Bảo mật Đa tầng & Phân quyền RBAC Động** | - Đăng ký xác thực qua Email OTP (Brevo SMTP), đăng nhập Google OAuth2 một chạm.<br>- Bảo mật kết hợp JWT Cookie + Session, phòng vệ kép CSRF và phân quyền vai trò động. |
| | **4. Tải lên Ký số Cloudinary & Hạ tầng CI/CD** | - Cơ chế ký số SHA-1 upload trực tiếp tài nguyên số lên Cloudinary CDN.<br>- Xây dựng Pipeline GitHub Actions CI/CD tự động kiểm thử và deploy lên Render. |
| **Nguyễn Đặng Cao Tường**<br>*(MSSV: 24110375)* | **1. Khảo thí & Đánh giá Năng lực Học viên** | - Ngân hàng câu hỏi trắc nghiệm, tạo đề thi ngẫu nhiên, cấu hình hạn chót và chấm điểm trắc nghiệm tự động (Quiz).<br>- Hệ thống nộp bài tập tự luận (Assignment Submission), chấm bài kèm tiêu chí (rubric) và phản hồi học viên. |
| | **2. Sổ điểm Tự động & Xử lý Excel Doanh nghiệp** | - Tính toán điểm tổng hợp tự động từ bài học, trắc nghiệm và bài tập.<br>- Đồng bộ Sổ điểm Đợt học (Cohort Gradebook) và Sổ điểm Toàn khóa (Course Gradebook).<br>- Xuất/Nhập bảng điểm định dạng `.xlsx` bằng Apache POI kèm bắt lỗi dữ liệu từng dòng. |
| | **3. Chứng chỉ Số & Đánh giá Khóa học (Rating)** | - Tự động thẩm định điều kiện hoàn thành khóa học và cấp Chứng chỉ số (Certificate Issuance).<br>- Trang tra cứu và xác thực chứng chỉ công khai (`/certificates/verify?code=...`).<br>- Hệ thống đánh giá sao và nhận xét khóa học/đợt học gắn liền với chứng chỉ. |
| | **4. Quy trình Kiểm duyệt Nội dung Đa cấp** | - Luồng kiểm duyệt khóa học và đợt học giữa Giảng viên và Moderator.<br>- Thẩm định nội dung, gửi phản hồi duyệt/từ chối, leo thang Admin và giám sát vi phạm thời gian cam kết dịch vụ (SLA). |
| | **5. Chấm công & Tính toán Lương Doanh nghiệp** | - Quản lý ca trực, Check-in / Check-out, làm thêm giờ (OT) của Kiểm duyệt viên.<br>- Công thức tính bảng lương tự động: Chia sẻ doanh thu cho Giảng viên; tính lương theo phút/ca/thưởng cho Moderator kèm ghi đè cá nhân. |

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
