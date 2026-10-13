# UC03 – Quản lý Khóa học (Course Management – Instructor)

## 1. Mô hình Phân cấp Khóa học & Cohort

```mermaid
graph TB
    subgraph Course["Course: Khóa học lớn (Master Course Tổng thể)"]
        direction TB
        C_Info["Thông tin định danh:<br/>- Tiêu đề (Title), Mã (Code), Slug<br/>- Mô tả, Headline, Thumbnail<br/>- Danh mục (Category), Cấp độ (Level)<br/>- Mục tiêu & Yêu cầu học tập"]
        
        subgraph Curriculum["Khung Giáo trình Chuẩn (Curriculum)"]
            direction TB
            Sec1["Section 1: Kiến thức nền tảng"]
            Sec1 --> Les1["Lesson 1.1: Giới thiệu & Cài đặt (Video)"]
            Sec1 --> Les2["Lesson 1.2: Cú pháp cốt lõi (Document)"]
            Sec1 --> Quiz1["Quiz 1: Trắc nghiệm tổng kết"]
            Sec2["Section 2: Lập trình nâng cao & Đồ án"]
            Sec2 --> Les3["Lesson 2.1: Kiến trúc hệ thống"]
            Sec2 --> Code1["Coding Problem: Bài tập thuật toán"]
        end
    end

    subgraph CohortRuns["Các Đợt học / Lớp học cụ thể mở từ Course (Cohorts)"]
        direction LR
        subgraph Cohort1["Cohort 1: Lớp K24 - Đợt 1"]
            C1_Info["Học phí đợt: 450.000 VNĐ<br/>Sĩ số: 50 | Khai giảng: 15/10<br/>Lịch Drip Content: T2-T4-T6<br/>Chat phòng học WebSocket K24-01"]
        end
        subgraph Cohort2["Cohort 2: Lớp K24 - Đợt 2"]
            C2_Info["Học phí đợt: 450.000 VNĐ<br/>Sĩ số: 50 | Khai giảng: 01/11<br/>Lịch Drip Content: T3-T5-T7<br/>Chat phòng học WebSocket K24-02"]
        end
        subgraph Cohort3["Cohort 3: Lớp K25 - Đợt đầu năm"]
            C3_Info["Học phí đợt: 500.000 VNĐ<br/>Sĩ số: 40 | Trạng thái: UPCOMING<br/>Tuyển sinh trước"]
        end
    end

    Course -->|"Mở các đợt học kế thừa giáo trình"| CohortRuns
    Student["Student (Học viên)"] -->|"1. Xem thông tin khóa & học thử bài preview"| Course
    Student -->|"2. Lựa chọn Đợt học (Cohort) phù hợp để ghi danh"| CohortRuns
```

---

## 2. Use Case Diagram

```mermaid
graph LR
    Instructor["Instructor<br/>(Giảng viên)"]
    Admin["Admin<br/>(Quản trị viên)"]
    Moderator["Moderator<br/>(Kiểm duyệt viên)"]

    subgraph UC_Course["Quản lý Khóa học lớn (Course Management)"]
        UC13["UC-13: Tạo khóa học chuẩn mới"]
        UC14["UC-14: Chỉnh sửa thông tin khóa học"]
        UC15["UC-15: Gửi xuất bản khóa học"]
        UC16["UC-16: Soạn giáo trình chuẩn (Section, Lesson)"]
        UC17["UC-17: Biên soạn đề kiểm tra Quiz chuẩn"]
    end

    Instructor --> UC13
    Instructor --> UC14
    Instructor --> UC15
    Instructor --> UC16
    Instructor --> UC17

    Admin -.->|"<<inherits>>"| Instructor

    UC13 -.->|"<<includes>>"| UC13a["Nhập tiêu đề, mô tả,<br/>thumbnail, danh mục, cấp độ"]
    UC14 -.->|"<<includes>>"| UC14a["Cập nhật metadata<br/>khóa học"]
    UC15 -.->|"<<includes>>"| UC15a["Kiểm tra điều kiện xuất bản<br/>(Tối thiểu 1 Section & 1 Lesson)"]
    UC15 -.->|"<<triggers>>"| ReviewQueue["Đẩy vào hàng đợi<br/>kiểm duyệt"]
    ReviewQueue -.->|"<<notifies>>"| Moderator

    UC16 -.->|"<<includes>>"| UC16a["Tạo Section & sắp xếp thứ tự"]
    UC16 -.->|"<<includes>>"| UC16b["Thêm Lesson: Video, Document,<br/>Tài nguyên từ Asset Library"]
    UC17 -.->|"<<includes>>"| UC17a["Thiết lập thời gian làm bài, điểm đạt"]
    UC17 -.->|"<<includes>>"| UC17b["Thêm câu hỏi trắc nghiệm & đáp án"]
```

---

## 3. Chi tiết Use Case

### UC-13: Tạo khóa học chuẩn mới

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-13 |
| **Tên Use Case** | Tạo khóa học chuẩn mới (Create Master Course) |
| **Actor chính** | Instructor |
| **Actor phụ** | Admin (thừa kế quyền) |
| **Quyền hạn (Permission)** | `COURSE_CREATE` |
| **Tiền điều kiện** | Giảng viên đã đăng nhập thành công vào hệ thống. |
| **Mô tả** | Giảng viên tạo khung chương trình khóa học chuẩn (Course). Khóa học này đóng vai trò là chương trình tổng thể, chứa giáo trình gốc và là cơ sở để mở các Đợt học (Cohorts) sau này. |
| **Luồng sự kiện chính** | 1. Giảng viên truy cập vào menu `/instructor/courses/create`.<br/>2. Hệ thống hiển thị form tạo khóa học gồm các trường:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Mã khóa học (`code` - duy nhất, ví dụ: `CS101-JAVA`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Tên khóa học (`title`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Danh mục (`category_id` - chọn từ danh mục có sẵn).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Tiêu đề ngắn (`headline`) và Mô tả chi tiết (`description`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Ảnh bìa đại diện (`thumbnail_url`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Cấp độ (`level`: BEGINNER / INTERMEDIATE / ADVANCED / ALL_LEVELS).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Mục tiêu đạt được (`objectives`) và Yêu cầu đầu vào (`requirements`).<br/>3. Giảng viên nhấn nút **"Tạo khóa học"**.<br/>4. Hệ thống kiểm tra tính hợp lệ dữ liệu (mã duy nhất, tiêu đề không rỗng).<br/>5. Hệ thống lưu khóa học vào bảng `courses` với trạng thái ban đầu là `DRAFT`.<br/>6. Hệ thống chuyển hướng giảng viên đến trang danh sách khóa học `/instructor/courses` kèm thông báo thành công. |
| **Luồng ngoại lệ** | - **4a. Trùng mã hoặc slug khóa học**: Hệ thống hiển thị thông báo lỗi yêu cầu đổi mã khóa học.<br/>- **4b. Thiếu trường bắt buộc**: Báo lỗi tương ứng ngay tại input vi phạm. |
| **Hậu điều kiện** | Khóa học mới được tạo ở trạng thái `DRAFT`, sẵn sàng để Giảng viên bắt đầu soạn giáo trình chuẩn. |

---

### UC-14: Chỉnh sửa thông tin khóa học

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-14 |
| **Tên Use Case** | Chỉnh sửa thông tin khóa học (Edit Course Metadata) |
| **Actor chính** | Instructor (chủ sở hữu khóa học) |
| **Quyền hạn (Permission)** | `COURSE_CREATE` |
| **Tiền điều kiện** | Khóa học tồn tại và thuộc quyền sở hữu của Giảng viên đăng nhập. |
| **Mô tả** | Cập nhật thông tin tổng thể của Course (tiêu đề, mô tả, ảnh đại diện, danh mục, cấp độ, mục tiêu). |
| **Luồng sự kiện chính** | 1. Giảng viên truy cập `/instructor/courses`.<br/>2. Chọn khóa học cần chỉnh sửa -> vào `/instructor/courses/{id}/edit`.<br/>3. Thay đổi các thông tin cần thiết.<br/>4. Nhấn **"Cập nhật"**.<br/>5. Hệ thống cập nhật bảng `courses` và thông báo cập nhật thành công qua Flash message. |
| **Luồng ngoại lệ** | - **Giảng viên không phải chủ sở hữu**: Hệ thống từ chối truy cập (HTTP 403 Forbidden).<br/>- **Khóa học đang ở trạng thái `PENDING_REVIEW`**: Khóa chỉnh sửa thông tin quan trọng để chờ kết quả kiểm duyệt. |
| **Hậu điều kiện** | Thông tin khóa học được cập nhật thành công. |

---

### UC-15: Gửi xuất bản khóa học

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-15 |
| **Tên Use Case** | Gửi xuất bản khóa học (Submit Course for Moderation) |
| **Actor chính** | Instructor |
| **Actor liên quan** | Moderator |
| **Tiền điều kiện** | Khóa học đang ở trạng thái `DRAFT` hoặc `REJECTED`. |
| **Mô tả** | Giảng viên hoàn tất xây dựng giáo trình khóa học chuẩn, gửi lên hệ thống để Ban Kiểm duyệt (Moderator) đánh giá chất lượng trước khi được phép mở lớp tuyển sinh công khai. |
| **Luồng sự kiện chính** | 1. Giảng viên vào trang quản lý khóa học `/instructor/courses`, click nút **"Gửi duyệt"** tại khóa học tương ứng.<br/>2. Hệ thống kiểm tra điều kiện xuất bản:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Course phải có ít nhất 1 Chương (`Section`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Có ít nhất 1 Bài học (`Lesson`) hợp lệ (đã có video hoặc tài liệu).<br/>3. Nếu thỏa mãn điều kiện, hệ thống chuyển trạng thái `status` của Course từ `DRAFT` sang `PENDING_REVIEW`.<br/>4. Hệ thống tự động tạo một nhiệm vụ kiểm duyệt (`ModerationTask`) với loại `COURSE`, ghi nhận vào hàng đợi kiểm duyệt cân bằng tải.<br/>5. Hệ thống gửi thông báo xác nhận cho Giảng viên. |
| **Luồng ngoại lệ** | - **Chưa đủ giáo trình chuẩn**: Hệ thống hiển thị thông báo lỗi yêu cầu hoàn thiện bài học trước khi gửi duyệt. |
| **Vòng đời trạng thái** | `DRAFT` -> `PENDING_REVIEW` -> `PUBLISHED` (Nếu duyệt thành công) HOẶC `REJECTED` (Nếu bị từ chối kèm lý do). |

---

### UC-16: Soạn giáo trình chuẩn Course (Curriculum Management)

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-16 |
| **Tên Use Case** | Soạn giáo trình chuẩn Course (Manage Course Curriculum) |
| **Actor chính** | Instructor |
| **Mô tả** | Giảng viên xây dựng cây giáo trình chuẩn hoàn chỉnh của Khóa học gồm các Chương (Section) và Bài học (Lesson). Giáo trình này sẽ được dùng chung cho tất cả các Đợt học (Cohorts) mở sau này. |
| **Luồng sự kiện chính** | 1. Giảng viên mở giao diện soạn giáo trình tại `/instructor/courses/{id}/curriculum`.<br/>2. **Thao tác Chương (Section)**:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Nhấn "Thêm chương mới", nhập tiêu đề chương.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Sắp xếp thứ tự các chương (`orderIndex`).<br/>3. **Thao tác Bài học (Lesson)**:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thêm bài học vào chương với loại nội dung: `VIDEO` hoặc `DOCUMENT`.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Nhập URL video hoặc chọn tệp đã tải lên từ Kho Tài nguyên (`InstructorAsset`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Soạn thảo nội dung tài liệu (`document_content`) hoặc đính kèm tệp tài liệu.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Đánh dấu `is_free_preview` nếu cho phép học viên học thử bài này trước khi đăng ký.<br/>4. Nhấn **"Lưu"**. Dữ liệu được cập nhật trực tiếp vào bảng `sections` và `lessons` gắn với `course_id`. |
| **Hậu điều kiện** | Khung giáo trình chuẩn của Course được hoàn thiện và lưu trữ an toàn. |

---

### UC-17: Biên soạn đề kiểm tra Quiz chuẩn

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-17 |
| **Tên Use Case** | Biên soạn đề kiểm tra Quiz chuẩn (Course Assessment Authoring) |
| **Actor chính** | Instructor |
| **Mô tả** | Giảng viên tạo các bài kiểm tra trắc nghiệm gắn với bài học hoặc chương học trong khóa học chuẩn. |
| **Luồng sự kiện chính** | 1. Giảng viên tạo bài học có loại `QUIZ` trong giáo trình chuẩn.<br/>2. Thiết lập thông số bài thi: Thời lượng làm bài (`duration_minutes`), Điểm đạt tối thiểu (`passing_score`), Số lần làm tối đa.<br/>3. Thêm danh sách câu hỏi trắc nghiệm (Nội dung câu hỏi, các lựa chọn đáp án `options JSONB`, đáp án đúng, giải thích câu trả lời).<br/>4. Lưu đề kiểm tra vào bảng `quizzes` và `questions`. |
| **Hậu điều kiện** | Bộ đề kiểm tra Quiz chuẩn sẵn sàng để học viên trong các Đợt học (Cohorts) làm bài. |
