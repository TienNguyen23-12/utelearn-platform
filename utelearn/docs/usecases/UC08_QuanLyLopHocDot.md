# UC08 – Quản lý Cohort (Lớp học / Module trong Course)

## 1. Mô hình Quan hệ Khóa học (Course) - Cohort

```mermaid
graph TD
    Course["Course: Khóa học lớn (Container)<br/>Ví dụ: Lộ trình Lập trình Java Chuyên sâu<br/>Giá Full Course: 1.100.000 VNĐ"]

    Course --> Cohort1["Cohort 1: Java Core & OOP<br/>Giá riêng: 350.000 VNĐ | Sĩ số: 50"]
    Course --> Cohort2["Cohort 2: Spring Boot RESTful API<br/>Giá riêng: 450.000 VNĐ | Sĩ số: 50"]
    Course --> Cohort3["Cohort 3: Microservices & Docker<br/>Giá riêng: 500.000 VNĐ | Sĩ số: 40"]

    subgraph C1_Content["Nội dung học tập Cohort 1"]
        direction TB
        Sec1["Section 1: Cú pháp Java & Hướng đối tượng"]
        Sec1 --> Les1["Lesson 1.1: Cú pháp cơ bản (Video)"]
        Sec1 --> Les2["Lesson 1.2: Kế thừa & Đa hình (Video)"]
        Sec1 --> Quiz1["Quiz 1: Trắc nghiệm OOP"]
    end

    Cohort1 --> C1_Content

    subgraph StudentOptions["Chính sách Đăng ký & Sở hữu của Học viên"]
        Opt1["Cách 1: Mua Full Course<br/>(Sở hữu tất cả Cohort 1, 2, 3 với giá ưu đãi 1.100.000 VNĐ)"]
        Opt2["Cách 2: Mua lẻ từng Cohort<br/>(Chỉ chọn học Cohort 1 hoặc Cohort 2 theo nhu cầu)"]
        Opt3["Cách 3: Nâng cấp (Upgrade)<br/>(Đã mua Cohort 1, trả chênh lệch để mở khóa toàn bộ Course)"]
    end
```

---

## 2. Use Case Diagram

```mermaid
graph LR
    Instructor["Instructor<br/>(Giảng viên)"]
    Student["Student<br/>(Học viên)"]
    TA["TA<br/>(Trợ giảng)"]

    subgraph UC_Cohort["Quản lý Cohort (Lớp học / Module thành phần)"]
        UC33["UC-33: Tạo Cohort trong Course"]
        UC34["UC-34: Mua lẻ Cohort / Nâng cấp Full Course"]
        UC35["UC-35: Chat thảo luận Cohort (WebSocket)"]
        UC36["UC-36: Quản lý lịch học Cohort"]
        UC37["UC-37: Soạn giáo trình & đề kiểm tra Cohort"]
    end

    Instructor --> UC33
    Instructor --> UC36
    Instructor --> UC37
    Student --> UC34
    Student --> UC35
    TA --> UC35
    Instructor --> UC35

    UC33 -.->|"<<includes>>"| UC33a["Thiết lập tên, mã code,<br/>giá riêng, sĩ số"]
    UC34 -.->|"<<extends>>"| UC34a["Mua 1 hoặc nhiều Cohort lẻ"]
    UC34 -.->|"<<extends>>"| UC34b["Nâng cấp bù chênh lệch<br/>lên Full Course"]
    UC35 -.->|"<<uses>>"| WSS["WebSocket Server<br/>(STOMP / SockJS)"]
    UC36 -.->|"<<includes>>"| UC36a["Cấu hình thời gian mở bài<br/>& Deadline nộp bài"]
    UC37 -.->|"<<includes>>"| UC37a["Tạo Section & Lesson"]
    UC37 -.->|"<<includes>>"| UC37b["Tạo Quiz & Câu hỏi trắc nghiệm"]
```

---

## 3. Luồng Nghiệp vụ Nâng cấp và Mua Hàng (Course & Cohort Purchase Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Student as Học viên (Student)
    participant UI as Giao diện Khóa học
    participant OrderSvc as Order & Cart Service
    participant PayGate as Cổng thanh toán
    participant CohortSvc as Cohort Enrollment Service

    Student->>UI: Xem chi tiết Course & danh sách Cohort
    alt Mua trọn bộ Full Course
        Student->>UI: Chọn "Đăng ký Full Course" (Giá ưu đãi)
        UI->>OrderSvc: Tạo đơn hàng Full Course
    else Mua chọn lọc 1 hoặc nhiều Cohort lẻ
        Student->>UI: Tick chọn các Cohort muốn học (Ví dụ: Cohort 1 & Cohort 2)
        UI->>OrderSvc: Tạo đơn hàng danh sách Cohort (Tổng = Sum giá các Cohort đã chọn)
    else Nâng cấp lên Full Course (Đã mua 1 phần trước đó)
        Student->>UI: Bấm "Nâng cấp lên Full Course"
        UI->>OrderSvc: Tính số tiền chênh lệch = (Giá Full Course - Số tiền đã thanh toán các Cohort trước)
    end

    OrderSvc->>PayGate: Yêu cầu giao dịch thanh toán
    Student->>PayGate: Hoàn tất thanh toán
    PayGate-->>OrderSvc: Xác nhận thanh toán thành công
    OrderSvc->>CohortSvc: Kích hoạt ghi danh (Gán quyền vào các Cohort tương ứng)
    CohortSvc-->>UI: Cấp quyền truy cập bài học, phòng chat WebSocket
    UI-->>Student: Chuyển hướng đến không gian học tập của Cohort
```

---

## 4. Chi tiết Use Case

### UC-33: Tạo Cohort trong Course

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-33 |
| **Tên Use Case** | Tạo Cohort trong Course (Create Cohort within Course) |
| **Actor chính** | Instructor |
| **Tiền điều kiện** | Khóa học cha (Course) đã được tạo (ở trạng thái `DRAFT` hoặc đang chỉnh sửa). |
| **Mô tả** | Giảng viên tạo thêm một lớp học / module thành phần (Cohort) bên trong Course cha, định nghĩa giá bán lẻ và các thông số hoạt động của lớp. |
| **Luồng sự kiện chính** | 1. Giảng viên vào chi tiết Course, chọn tab **"Danh sách Cohort"**.<br/>2. Nhấn nút **"Thêm Cohort mới"**.<br/>3. Điền các trường thông tin:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Mã lớp (`code` - duy nhất trong hệ thống, VD: `K23-JAVA-01`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Tên lớp / module (`name` - VD: `Lớp Java Core & OOP`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Giá bán mua lẻ (`price` - VNĐ, áp dụng khi học viên mua riêng lẻ Cohort này).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thứ tự hiển thị trong lộ trình (`orderIndex`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thời gian mở và đóng đăng ký (`enrollment_start`, `enrollment_end`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thời gian học (`study_start`, `study_end`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Sĩ số tối đa (`max_capacity`, mặc định 50).<br/>4. Nhấn **"Lưu Cohort"**.<br/>5. Hệ thống kiểm tra tính hợp lệ dữ liệu (ngày mở đăng ký < đóng đăng ký, ngày bắt đầu học < kết thúc học, mã duy nhất).<br/>6. Hệ thống tạo bản ghi mới trong bảng `cohorts` liên kết với `course_id`.<br/>7. Tự động thêm Giảng viên vào bảng `cohort_members` với vai trò `TEACHER`. |
| **Luồng ngoại lệ** | - Trùng mã code hoặc khoảng thời gian không hợp lệ -> Hệ thống hiển thị cảnh báo lỗi tương ứng. |
| **Hậu điều kiện** | Cohort mới sẵn sàng để xây dựng nội dung bài học (Section, Lesson, Quiz). |

---

### UC-34: Mua lẻ Cohort / Nâng cấp Full Course

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-34 |
| **Tên Use Case** | Mua lẻ Cohort / Nâng cấp Full Course (Cohort Purchase & Upgrade) |
| **Actor chính** | Student |
| **Tiền điều kiện** | Học viên đã đăng nhập tài khoản UTELearn. |
| **Mô tả** | Cho phép học viên linh hoạt mua 1 hoặc nhiều Cohort theo nhu cầu, hoặc nâng cấp lên gói Full Course để tiết kiệm chi phí. |
| **Luồng sự kiện chính** | 1. Học viên vào trang chi tiết khóa học `/courses/{slug}`.<br/>2. Giao diện hiển thị rõ danh sách các Cohort thành phần, giá riêng từng Cohort và giá ưu đãi Full Course.<br/>3. Học viên thực hiện một trong các thao tác:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- **Mua lẻ Cohort**: Tick chọn 1 hoặc nhiều Cohort muốn mua -> Hệ thống tính tổng tiền = tổng giá các Cohort đã tick.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- **Mua Full Course**: Bấm nút "Mua trọn bộ Full Course" -> Hệ thống áp dụng mức giá trọn gói ưu đãi.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- **Nâng cấp (Upgrade)**: Với học viên đã từng mua 1 vài Cohort trong Course này, hệ thống hiển thị tùy chọn "Nâng cấp lên Full Course" với số tiền chênh lệch = `Giá Full Course - Tổng tiền đã chi trả trước đó`.<br/>4. Học viên xác nhận và tiến hành thanh toán qua cổng thanh toán tích hợp.<br/>5. Khi giao dịch thành công, hệ thống tạo bản ghi `orders`, đồng thời thêm học viên vào `cohort_members` của tất cả các Cohort tương ứng với role `STUDENT`.<br/>6. Tự động tăng biến `current_enrolled` của các Cohort đó. |
| **Luồng ngoại lệ** | - Cohort đã đủ sĩ số tối đa (`current_enrolled >= max_capacity`) -> Thông báo lớp đã đầy, không thể mua lẻ Cohort đó.<br/>- Đã qua thời hạn đăng ký (`enrollment_end`) -> Thông báo đợt đăng ký đã kết thúc. |
| **Hậu điều kiện** | Học viên sở hữu quyền truy cập nội dung bài học, nộp bài và vào phòng chat của các Cohort đã mua. |

---

### UC-35: Chat thảo luận Cohort (WebSocket)

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-35 |
| **Tên Use Case** | Chat thảo luận Cohort thời gian thực (Real-time Cohort Chat) |
| **Actor chính** | Student, Teaching Assistant (TA), Instructor |
| **Tiền điều kiện** | Người dùng đã được ghi danh trong Cohort (`cohort_members`). |
| **Mô tả** | Phòng chat trao đổi học tập thời gian thực giữa Giảng viên, Trợ giảng và Học viên trong phạm vi từng Cohort. |
| **Luồng sự kiện chính** | 1. Học viên truy cập không gian học tập của Cohort đã mua.<br/>2. Mở panel chat thảo luận của Cohort.<br/>3. Ứng dụng client thiết lập kết nối WebSocket (STOMP qua SockJS) đến server endpoint `/ws`.<br/>4. Client đăng ký lắng nghe (subscribe) topic của lớp: `/topic/cohort/{cohortId}`.<br/>5. Người dùng nhập tin nhắn và gửi đến `/app/cohort/{cohortId}/send`.<br/>6. Server xác thực quyền thành viên của sender trong Cohort đó, lưu tin nhắn vào bảng `cohort_messages`, và broadcast tin nhắn đến tất cả thành viên đang online trong topic.<br/>7. Giao diện người dùng hiển thị tin nhắn ngay lập tức kèm họ tên, avatar và role của người gửi. |
| **Luồng ngoại lệ** | - Người dùng chưa mua / không thuộc Cohort -> Server từ chối kết nối hoặc gửi tin nhắn (403 Forbidden). |
| **Dữ liệu lưu trữ** | `cohort_messages`: id, cohort_id, sender_id, message, message_type, created_at. |

---

### UC-36: Quản lý lịch học Cohort

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-36 |
| **Tên Use Case** | Quản lý lịch học Cohort (Cohort Schedule & Unlock Management) |
| **Actor chính** | Instructor |
| **Actor xem** | Student, TA |
| **Mô tả** | Thiết lập thời gian mở từng bài học (Lesson Unlock), thời hạn chót nộp bài (Deadline), và chế độ công bố điểm số trong Cohort. |
| **Luồng sự kiện chính** | 1. Giảng viên vào mục **"Lịch học & Điều phối"** của Cohort.<br/>2. Hệ thống liệt kê toàn bộ bài học (Lesson) và bài kiểm tra (Quiz) của Cohort.<br/>3. Với từng bài học, Giảng viên cấu hình:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thời điểm mở bài (`unlock_at` - học viên chỉ được xem sau thời điểm này).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Hạn nộp bài (`deadline_at` - áp dụng cho bài tập / quiz).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Chế độ xem điểm (`show_score_type`: IMMEDIATELY sau khi nộp, AFTER_DEADLINE, hoặc MANUAL khi GV duyệt).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Cho phép xem lời giải chi tiết (`allow_review_answers`).<br/>4. Nhấn **"Lưu lịch học"**.<br/>5. Dữ liệu được lưu vào bảng `cohort_schedules`. Học viên truy cập lớp học sẽ thấy timeline lịch học trực quan. |
| **Hậu điều kiện** | Bài giảng tự động mở theo đúng lịch trình thiết lập. |

---

### UC-37: Soạn giáo trình & đề kiểm tra Cohort

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-37 |
| **Tên Use Case** | Soạn giáo trình & đề kiểm tra Cohort (Cohort Curriculum & Assessments) |
| **Actor chính** | Instructor |
| **Mô tả** | Xây dựng cây nội dung học tập hoàn chỉnh cho từng Cohort gồm các Section (Chương), Lesson (Bài học) và Quiz (Bài kiểm tra). |
| **Luồng sự kiện chính** | 1. Giảng viên mở giao diện soạn giáo trình của Cohort.<br/>2. **Thao tác Section**: Nhấn "Thêm Section mới", đặt tên chương, kéo thả sắp xếp thứ tự.<br/>3. **Thao tác Lesson**: Trong từng Section, thêm các bài học với loại nội dung tương ứng:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- `VIDEO`: Nhập URL video hoặc chọn từ kho tài nguyên (`InstructorAsset`), điền thời lượng.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- `DOCUMENT`: Soạn thảo nội dung định dạng phong phú (Rich text / Markdown) hoặc đính kèm tài liệu.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Đánh dấu `is_free_preview` nếu cho phép học viên học thử miễn phí trước khi mua.<br/>4. **Thao tác Quiz**: Tạo bài kiểm tra trắc nghiệm gắn với bài học:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thiết lập thời gian làm bài (`duration_minutes`), điểm đạt (`passing_score`), số lần làm bài tối đa.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thêm câu hỏi (nội dung, loại câu hỏi: single choice / multi-select, đáp án, giải thích chi tiết).<br/>5. Giảng viên lưu lại. Toàn bộ nội dung gắn chặt với Cohort đó. |
| **Hậu điều kiện** | Nội dung học tập của Cohort hoàn tất, sẵn sàng cho việc kiểm duyệt và giảng dạy. |
