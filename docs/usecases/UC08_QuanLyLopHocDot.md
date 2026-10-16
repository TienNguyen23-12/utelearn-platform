# UC08 – Quản lý Đợt học / Lớp học (Cohort Management)

## 1. Mô hình Quan hệ Khóa học (Course) - Đợt học (Cohort)

```mermaid
graph TD
    Course["Course: Khóa học lớn (Master Course)<br/>Ví dụ: Lập trình Java Chuyên sâu<br/>Chứa toàn bộ giáo trình chuẩn (Section, Lesson, Quiz)"]

    Course -->|"Mở đợt học 1"| Cohort1["Cohort 1: Lớp K24 - Đợt 1 (Mã: K24-JAVA-01)<br/>Học phí: 450.000 VNĐ | Sĩ số: 50 | Khai giảng: 15/10"]
    Course -->|"Mở đợt học 2"| Cohort2["Cohort 2: Lớp K24 - Đợt 2 (Mã: K24-JAVA-02)<br/>Học phí: 450.000 VNĐ | Sĩ số: 50 | Khai giảng: 01/11"]
    Course -->|"Mở đợt học 3"| Cohort3["Cohort 3: Lớp K25 - Đợt Đầu năm<br/>Học phí: 500.000 VNĐ | Sĩ số: 40 | Trạng thái: UPCOMING"]

    subgraph C1_Operations["Không gian Hoạt động & Điều phối của Cohort 1"]
        direction TB
        Sched["Lịch trình Lớp (Cohort Schedule):<br/>- Drip Content: Mở bài theo lịch T2-T4-T6<br/>- Deadline nộp Quiz & Code bài tập"]
        Chat["Phòng Chat Thời gian thực (WebSocket):<br/>- Giảng viên, Trợ giảng, Học viên lớp K24-01"]
        Members["Danh sách Thành viên (Cohort Members):<br/>- Học viên đã ghi danh vào đợt 1"]
    end

    Cohort1 --> C1_Operations

    subgraph StudentFlow["Quy trình Học viên Đăng ký & Học tập"]
        Step1["1. Xem thông tin khóa học tổng thể (Course)"]
        Step2["2. Xem danh sách các Đợt học (Cohorts) đang mở tuyển sinh"]
        Step3["3. Chọn Đợt học có thời gian phù hợp và tiến hành ghi danh / thanh toán"]
        Step4["4. Vào học theo lịch mở bài của Cohort & tham gia trao đổi lớp"]
        Step1 --> Step2 --> Step3 --> Step4
    end
```

---

## 2. Use Case Diagram

```mermaid
graph LR
    Instructor["Instructor<br/>(Giảng viên)"]
    Student["Student<br/>(Học viên)"]
    TA["TA<br/>(Trợ giảng)"]

    subgraph UC_Cohort["Quản lý Đợt học / Lớp học (Cohort Management)"]
        UC33["UC-33: Mở Đợt học mới cho Course"]
        UC34["UC-34: Đăng ký / Ghi danh vào Đợt học"]
        UC35["UC-35: Chat thảo luận lớp học (WebSocket)"]
        UC36["UC-36: Điều phối lịch mở bài & Deadline"]
        UC37["UC-37: Quản lý thành viên & Tiến độ lớp"]
    end

    Instructor --> UC33
    Instructor --> UC36
    Instructor --> UC37
    Student --> UC34
    Student --> UC35
    TA --> UC35
    Instructor --> UC35

    UC33 -.->|"<<includes>>"| UC33a["Chọn Course cha, nhập tên lớp,<br/>mã code, học phí đợt, sĩ số"]
    UC34 -.->|"<<includes>>"| UC34a["Kiểm tra thời hạn tuyển sinh & sĩ số"]
    UC35 -.->|"<<uses>>"| WSS["WebSocket Server<br/>(STOMP / SockJS)"]
    UC36 -.->|"<<includes>>"| UC36a["Cấu hình unlock_at<br/>& deadline_at cho từng bài học"]
    UC37 -.->|"<<includes>>"| UC37a["Xem danh sách học viên,<br/>theo dõi điểm số và hoàn thành"]
```

---

## 3. Luồng Nghiệp vụ Đăng ký Đợt học (Cohort Enrollment Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Student as Học viên (Student)
    participant UI as Giao diện Chi tiết Khóa học
    participant OrderSvc as Thanh toán / Ghi danh Service
    participant PayGate as Cổng thanh toán
    participant CohortSvc as Cohort Member Service

    Student->>UI: Xem chi tiết Course & danh sách các Đợt học đang mở (Cohorts)
    Student->>UI: Chọn Đợt học (Cohort) phù hợp và bấm "Ghi danh"
    alt Khóa học miễn phí (Học phí = 0)
        UI->>CohortSvc: Ghi danh trực tiếp học viên vào Cohort
        CohortSvc-->>UI: Cấp quyền truy cập bài học và phòng chat
    else Khóa học có phí
        UI->>OrderSvc: Khởi tạo yêu cầu thanh toán cho Đợt học đã chọn
        OrderSvc->>PayGate: Chuyển hướng thanh toán
        Student->>PayGate: Hoàn tất giao dịch
        PayGate-->>OrderSvc: Xác nhận thanh toán thành công
        OrderSvc->>CohortSvc: Kích hoạt ghi danh vào Cohort
        CohortSvc-->>UI: Cấp quyền thành viên lớp học
    end
    UI-->>Student: Chuyển hướng đến không gian học tập của Đợt học
```

---

## 4. Chi tiết Use Case

### UC-33: Mở Đợt học mới cho Course

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-33 |
| **Tên Use Case** | Mở Đợt học mới cho Course (Open New Cohort for Course) |
| **Actor chính** | Instructor |
| **Tiền điều kiện** | Khóa học cha (Course) đã được tạo và thuộc sở hữu của Giảng viên. |
| **Mô tả** | Giảng viên tạo thêm một đợt tuyển sinh / lớp học cụ thể (Cohort) trực thuộc Course cha, định nghĩa học phí của đợt, sĩ số và thời gian đào tạo. |
| **Luồng sự kiện chính** | 1. Giảng viên vào `/instructor/cohorts/create`.<br/>2. Chọn Khóa học cha (`course_id`) từ danh sách các Course do mình phụ trách.<br/>3. Điền các trường thông tin của đợt học:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Mã lớp (`code` - duy nhất trong hệ thống, VD: `K24-JAVA-01`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Tên lớp (`name` - VD: `Lớp Java Chuyên sâu K24 - Đợt 1`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Học phí của đợt (`price` - VNĐ).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thời gian mở và đóng đăng ký (`enrollment_start`, `enrollment_end`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thời gian bắt đầu và kết thúc học (`study_start`, `study_end`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Sĩ số tối đa (`max_capacity`, mặc định 50).<br/>4. Nhấn **"Lưu lớp học"**.<br/>5. Hệ thống kiểm tra tính hợp lệ dữ liệu (ngày mở < đóng đăng ký, ngày bắt đầu < kết thúc học, mã duy nhất).<br/>6. Hệ thống tạo bản ghi mới trong bảng `cohorts` liên kết với `course_id`. |
| **Luồng ngoại lệ** | - Trùng mã code hoặc khoảng thời gian không hợp lệ -> Hệ thống báo lỗi tương ứng. |
| **Hậu điều kiện** | Đợt học mới sẵn sàng để tiếp nhận học viên và điều phối lịch học. |

---

### UC-34: Đăng ký / Ghi danh vào Đợt học

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-34 |
| **Tên Use Case** | Đăng ký / Ghi danh vào Đợt học (Cohort Enrollment) |
| **Actor chính** | Student |
| **Tiền điều kiện** | Học viên đã đăng nhập tài khoản UTELearn. |
| **Mô tả** | Học viên chọn một Đợt học cụ thể của khóa học đang mở tuyển sinh để đăng ký tham gia. |
| **Luồng sự kiện chính** | 1. Học viên vào trang chi tiết khóa học `/courses/{slug}`.<br/>2. Giao diện hiển thị danh sách các Đợt học (Cohorts) đang mở hoặc sắp mở của khóa học đó.<br/>3. Học viên chọn Đợt học mong muốn và bấm **"Ghi danh"**.<br/>4. Nếu lớp có học phí > 0, hệ thống chuyển hướng đến trang thanh toán `/checkout/{cohortId}`.<br/>5. Học viên hoàn tất thanh toán (hoặc ghi danh trực tiếp nếu miễn phí).<br/>6. Hệ thống thêm học viên vào bảng `cohort_members` với vai trò `STUDENT`.<br/>7. Tự động tăng số lượng `current_enrolled` của Cohort đó lên 1. |
| **Luồng ngoại lệ** | - Lớp đã đủ sĩ số tối đa (`current_enrolled >= max_capacity`) -> Thông báo lớp đã đủ số lượng học viên.<br/>- Đã qua thời hạn đăng ký (`enrollment_end`) -> Thông báo đợt đăng ký đã kết thúc. |
| **Hậu điều kiện** | Học viên sở hữu quyền truy cập nội dung bài học, nộp bài tập và vào phòng chat của Đợt học đã chọn. |

---

### UC-35: Chat thảo luận lớp học (WebSocket)

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-35 |
| **Tên Use Case** | Chat thảo luận lớp học thời gian thực (Real-time Cohort Chat) |
| **Actor chính** | Student, Teaching Assistant (TA), Instructor |
| **Tiền điều kiện** | Người dùng đã được ghi danh trong Cohort (`cohort_members`). |
| **Mô tả** | Không gian trao đổi học tập thời gian thực giữa Giảng viên, Trợ giảng và Học viên trong phạm vi từng Đợt học. |
| **Luồng sự kiện chính** | 1. Học viên truy cập không gian học tập của Đợt học.<br/>2. Mở panel chat thảo luận của lớp.<br/>3. Ứng dụng client thiết lập kết nối WebSocket (STOMP qua SockJS) đến server endpoint `/ws`.<br/>4. Client đăng ký lắng nghe (subscribe) topic của lớp: `/topic/cohort/{cohortId}`.<br/>5. Người dùng gửi tin nhắn đến `/app/cohort/{cohortId}/send`.<br/>6. Server xác thực quyền thành viên trong Cohort, lưu tin nhắn vào bảng `cohort_messages`, và broadcast tin nhắn đến tất cả thành viên trong phòng.<br/>7. Giao diện người dùng hiển thị tin nhắn ngay lập tức kèm họ tên, avatar và vai trò của người gửi. |
| **Luồng ngoại lệ** | - Người dùng chưa ghi danh trong Cohort -> Server từ chối kết nối (403 Forbidden). |
| **Dữ liệu lưu trữ** | `cohort_messages`: id, cohort_id, sender_id, message, message_type, created_at. |

---

### UC-36: Điều phối lịch mở bài & Deadline (Drip Content)

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-36 |
| **Tên Use Case** | Điều phối lịch mở bài & Deadline (Cohort Schedule & Unlock Management) |
| **Actor chính** | Instructor |
| **Actor xem** | Student, TA |
| **Mô tả** | Thiết lập thời gian mở từng bài học (Lesson Unlock - Drip Content), thời hạn chót nộp bài (Deadline), và chế độ công bố điểm số trong Đợt học. |
| **Luồng sự kiện chính** | 1. Giảng viên truy cập `/instructor/cohorts/{id}/schedule`.<br/>2. Hệ thống liệt kê toàn bộ các bài học (Lesson) và bài kiểm tra (Quiz) kế thừa từ Course cha.<br/>3. Với từng bài học, Giảng viên cấu hình:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thời điểm mở bài (`unlock_at` - học viên chỉ xem được sau thời điểm này).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Hạn nộp bài (`deadline_at` - áp dụng cho quiz và bài tập lập trình).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Chế độ xem điểm (`show_score_type`: IMMEDIATELY, AFTER_DEADLINE, MANUAL).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Cho phép xem lại đáp án (`allow_review_answers`).<br/>4. Nhấn **"Lưu lịch học"**.<br/>5. Dữ liệu được lưu vào bảng `cohort_schedules`. Học viên truy cập lớp học sẽ thấy lịch trình mở bài rõ ràng. |
| **Hậu điều kiện** | Bài giảng và bài kiểm tra tự động khóa/mở theo đúng lịch trình của Đợt học. |

---

### UC-37: Quản lý thành viên & Tiến độ lớp học

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-37 |
| **Tên Use Case** | Quản lý thành viên & Tiến độ lớp (Cohort Members & Progress Management) |
| **Actor chính** | Instructor |
| **Mô tả** | Giảng viên theo dõi danh sách học viên đã ghi danh vào Đợt học, kiểm tra tiến độ học bài và kết quả bài thi. |
| **Luồng sự kiện chính** | 1. Giảng viên mở mục quản lý Đợt học `/instructor/cohorts`.<br/>2. Xem thống kê sĩ số hiện tại (`current_enrolled / max_capacity`) và trạng thái lớp (`UPCOMING`, `ENROLLING`, `IN_PROGRESS`, `COMPLETED`).<br/>3. Xem danh sách thành viên trong bảng `cohort_members` (họ tên, email, ngày ghi danh, kết quả đạt/chưa đạt).<br/>4. Cập nhật trạng thái đợt học khi kết thúc khóa đào tạo. |
| **Hậu điều kiện** | Giảng viên nắm bắt toàn diện tiến độ của lớp học theo từng đợt. |
