# UC08 – Quản lý Đợt học / Lớp học (Cohort Management)

## 1. Mô hình Quan hệ Khóa học (Course) - Module / Đợt học (Cohort)

```mermaid
graph TD
    subgraph MasterCourseModel["1. Mô hình Đợt học trực thuộc Khóa học Khung (Course-based Cohort)"]
        Course["Course: Khóa học lớn (Master Course)<br/>Ví dụ: Lập trình Java Chuyên sâu<br/>Chứa khung giáo trình chuẩn (Section, Lesson, Quiz)"]
        Course -->|"Mở đợt học 1"| Cohort1["Cohort 1: Lớp K24 - Đợt 1 (Mã: K24-JAVA-01)<br/>Học phí: 450.000 VNĐ | Sĩ số: 50 | Khai giảng: 15/10"]
        Course -->|"Mở đợt học 2"| Cohort2["Cohort 2: Lớp K24 - Đợt 2 (Mã: K24-JAVA-02)<br/>Học phí: 450.000 VNĐ | Sĩ số: 50 | Khai giảng: 01/11"]
        Course -->|"Mở đợt học 3"| Cohort3["Cohort 3: Lớp K25 - Đợt Đầu năm<br/>Học phí: 500.000 VNĐ | Sĩ số: 40 | Trạng thái: UPCOMING"]
    end

    subgraph StandaloneModel["2. Mô hình Module / Chuyên đề Bán lẻ Độc lập (Standalone Cohort)"]
        StandaloneCohort["Standalone Module: Chuyên đề Độc lập (course = null)<br/>Ví dụ: Docker & CI/CD cho Lập trình viên Web<br/>Học phí mua lẻ riêng | Giáo trình độc lập | Duyệt nội dung riêng"]
        StandaloneCohort -->|"Quyền chia sẻ: PUBLIC"| Shared["Cho phép Giảng viên khác tái sử dụng trong Course lớn"]
        StandaloneCohort -->|"Quyền chia sẻ: PRIVATE"| Exclusive["Độc quyền tác giả tự khai thác và mở bán"]
    end

    subgraph C1_Operations["Không gian Hoạt động & Điều phối của Cohort"]
        direction TB
        Sched["Lịch trình Lớp (Cohort Schedule):<br/>- Drip Content: Mở bài theo lịch T2-T4-T6<br/>- Deadline nộp Quiz & Code bài tập"]
        Chat["Phòng Chat Thời gian thực (WebSocket):<br/>- Giảng viên, Trợ giảng, Học viên lớp"]
        Members["Danh sách Thành viên (Cohort Members):<br/>- Học viên đã ghi danh vào đợt"]
        Lifecycle["Vòng đời Trạng thái Tự động (Lifecycle):<br/>- UPCOMING -> ENROLLING -> IN_PROGRESS -> COMPLETED"]
    end

    Cohort1 --> C1_Operations
    StandaloneCohort --> C1_Operations
```

---

## 2. Use Case Diagram

```mermaid
graph LR
    Instructor["Instructor<br/>(Giảng viên)"]
    Student["Student<br/>(Học viên)"]
    TA["TA<br/>(Trợ giảng)"]

    subgraph UC_Cohort["Quản lý Đợt học / Lớp học (Cohort Management)"]
        UC33["UC-33: Mở Đợt học / Module mới"]
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

    UC33 -.->|"<<includes>>"| UC33a["Chọn Course cha hoặc tạo Module độc lập,<br/>nhập mã code, học phí lẻ, sĩ số"]
    UC33 -.->|"<<includes>>"| UC33b["Cấu hình quyền chia sẻ<br/>(Public / Private)"]
    UC34 -.->|"<<includes>>"| UC34a["Kiểm tra thời hạn tuyển sinh & sĩ số"]
    UC35 -.->|"<<uses>>"| WSS["WebSocket Server<br/>(STOMP / SockJS)"]
    UC36 -.->|"<<includes>>"| UC36a["Cấu hình unlock_at<br/>& deadline_at cho từng bài học"]
    UC37 -.->|"<<includes>>"| UC37a["Xem danh sách học viên,<br/>theo dõi điểm số và hoàn thành"]
    UC37 -.->|"<<includes>>"| UC37b["Tự động tính toán & Đồng bộ<br/>trạng thái theo thời gian thực"]
```


---

## 3. Sơ đồ Vòng đời Trạng thái Đợt học / Module (Cohort Lifecycle State Machine Diagram)

```mermaid
stateDiagram-v2
    [*] --> UPCOMING: 1. Giảng viên tạo Đợt học / Module (Mặc định hoặc lên lịch trong tương lai)
    
    UPCOMING --> ENROLLING: now >= enrollmentStart (Thời gian mở ghi danh đã tới)
    UPCOMING --> COMPLETED: now >= studyEnd hoặc Đóng thủ công
    
    ENROLLING --> IN_PROGRESS: now >= studyStart (Lớp bắt đầu thời gian đào tạo)
    ENROLLING --> COMPLETED: now >= studyEnd hoặc hết hạn tuyển sinh (không có studyStart)
    
    IN_PROGRESS --> COMPLETED: now >= studyEnd (Hoàn tất khóa đào tạo) hoặc Đóng thủ công
    
    COMPLETED --> [*]
    
    note right of ENROLLING
        Cơ chế tự động hóa (Realtime Auto-Sync):
        1. Dynamic Evaluation: getStatus() tính toán động theo Instant.now().
        2. JPA Lifecycle Callbacks: @PostLoad, @PrePersist, @PreUpdate đồng bộ thuộc tính entity.
        3. Bulk SQL Auto-Promote Queries: Tự động chạy khi truy vấn danh sách & khởi động ứng dụng.
    end note
```

---

## 4. Luồng Nghiệp vụ Đăng ký Đợt học (Cohort Enrollment Flow)

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

## 5. Chi tiết Use Case

### UC-33: Mở Đợt học / Module mới

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-33 |
| **Tên Use Case** | Mở Đợt học / Module mới (Open New Cohort / Standalone Module) |
| **Actor chính** | Instructor |
| **Tiền điều kiện** | Giảng viên đã đăng nhập tài khoản có quyền `INSTRUCTOR`. |
| **Mô tả** | Giảng viên tạo thêm một đợt tuyển sinh / lớp học cụ thể (Cohort) trực thuộc Course cha hoặc tạo **Module chuyên đề bán lẻ độc lập** (`course_id = null`), định nghĩa học phí mua lẻ, sĩ số, lịch mở tuyển sinh & đào tạo, và quyền chia sẻ (`visibility`). |
| **Luồng sự kiện chính** | 1. Giảng viên vào `/instructor/cohorts/create`.<br/>2. Chọn Khóa học cha (`course_id`) từ danh sách Course do mình phụ trách, HOẶC chọn **"Bán lẻ độc lập (Không thuộc khóa học)"**.<br/>3. Điền các trường thông tin của đợt học / module:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Mã lớp (`code` - hệ thống tự động sinh theo mẫu `{PREFIX}-YYYY-XX` nếu để trống).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Tên đợt / module (`name`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Học phí mua lẻ (`price` - VNĐ, nhập 0 nếu miễn phí).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thời gian mở và đóng đăng ký (`enrollment_start`, `enrollment_end`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thời gian bắt đầu và kết thúc học (`study_start`, `study_end`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Sĩ số tối đa (`max_capacity`, mặc định 50).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Quyền chia sẻ (`visibility`: `PUBLIC` - cho phép giảng viên khác dùng trong khóa học của họ, hoặc `PRIVATE` - độc quyền tác giả).<br/>4. Nhấn **"Lưu đợt học / module"**.<br/>5. Hệ thống kiểm tra tính hợp lệ dữ liệu (ngày mở < đóng đăng ký, ngày bắt đầu < kết thúc học, mã duy nhất).<br/>6. Hệ thống tự động tính toán trạng thái khởi tạo (`calculateDynamicStatus()`), lưu bản ghi vào bảng `cohorts`. Nếu là module độc lập, gán `moderation_status = DRAFT` để chuẩn bị gửi duyệt. |
| **Ràng buộc nghiệp vụ** | 1. **Tính hợp lệ thời gian**: Ngày mở đăng ký phải trước ngày đóng đăng ký; ngày bắt đầu học phải trước ngày kết thúc.<br/>2. **Phân định quyền chỉnh sửa (`canEdit`)**: Chỉ Giảng viên tạo Module (`cohort.createdBy`), Giảng viên sở hữu Course cha hoặc Quản trị viên (`ADMIN`) mới có quyền bật công tắc Chế độ chỉnh sửa (`moodleEditModeToggle`), sửa thông tin hoặc cấu trúc Module. Kiểm duyệt viên (Moderator) khi xem xét module chỉ có quyền thẩm định Read-Only (`hasAccessRole = true, canEdit = false`), hệ thống ẩn toàn bộ công tắc edit mode và chặn các API chỉnh sửa module. |
| **Luồng ngoại lệ** | - Trùng mã code hoặc khoảng thời gian không hợp lệ -> Hệ thống báo lỗi tương ứng. |
| **Hậu điều kiện** | Đợt học / module mới sẵn sàng để biên soạn giáo trình, điều phối lịch học hoặc gửi duyệt bán lẻ. |

---

### UC-34: Đăng ký / Ghi danh vào Đợt học

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-34 |
| **Tên Use Case** | Đăng ký / Ghi danh vào Đợt học (Cohort Enrollment) |
| **Actor chính** | Student |
| **Tiền điều kiện** | Học viên đã đăng nhập tài khoản UTELearn. |
| **Mô tả** | Học viên chọn một Đợt học hoặc Module bán lẻ đang mở tuyển sinh để đăng ký tham gia. |
| **Luồng sự kiện chính** | 1. Học viên vào trang chi tiết khóa học `/courses/{slug}` hoặc chi tiết module `/cohorts/{id}`.<br/>2. Giao diện hiển thị danh sách các Đợt học đang mở ghi danh (`ENROLLING`) hoặc sắp mở (`UPCOMING`).<br/>3. Học viên chọn Đợt học mong muốn và bấm **"Ghi danh"**.<br/>4. Nếu lớp có học phí > 0, hệ thống chuyển hướng đến trang thanh toán `/checkout/{cohortId}`.<br/>5. Học viên hoàn tất thanh toán (hoặc ghi danh trực tiếp nếu miễn phí).<br/>6. Hệ thống thêm học viên vào bảng `cohort_members` với vai trò `STUDENT`.<br/>7. Tự động tăng số lượng `current_enrolled` của Cohort đó lên 1. |
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

### UC-36: Điều phối lịch mở bài & Deadline (Drip Content & Late Submission Policy)

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-36 |
| **Tên Use Case** | Điều phối lịch mở bài & Deadline (Cohort Schedule & Unlock Management) |
| **Actor chính** | Instructor |
| **Actor xem** | Student, TA |
| **Mô tả** | Thiết lập thời gian mở từng bài học (Lesson Unlock - Drip Content), thời hạn chót nộp bài (Deadline) hiển thị thân thiện, chế độ công bố điểm số và kiểm soát chính sách nộp trễ trong Đợt học. |
| **Luồng sự kiện chính** | 1. Giảng viên truy cập `/instructor/cohorts/{id}/schedule`.<br/>2. Hệ thống liệt kê toàn bộ các bài học (Lesson), bài kiểm tra (Quiz) và bài tập tự luận (Assignment) kế thừa từ Course cha hoặc giáo trình module.<br/>3. Với từng bài học, Giảng viên cấu hình:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Thời điểm mở bài (`unlock_at` - học viên chỉ xem được sau thời điểm này).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Hạn nộp bài (`deadline_at` - áp dụng cho Quiz, Assignment và Coding Problem).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Chế độ xem điểm (`show_score_type`: IMMEDIATELY, AFTER_DEADLINE, MANUAL).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Cho phép xem lại đáp án (`allow_review_answers`).<br/>4. **Cơ chế hiển thị & Kiểm soát nộp trễ (Deadline Tracking & Late Policy)**:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Hệ thống tự động chuyển đổi ngày giờ hạn chót sang định dạng trực quan thân thiện (`deadlineAtDisplay`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Tự động đánh giá trạng thái quá hạn theo thời gian thực (`isDeadlinePassed` / `isPastDeadline`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Kết hợp cấu hình `allowLateSubmission`: Cho phép nộp trễ (gắn cờ `isLate = true` và badge đỏ cảnh báo) hoặc khóa hoàn toàn quyền nộp bài khi đã quá deadline.<br/>5. Nhấn **"Lưu lịch học"**.<br/>6. Dữ liệu được lưu vào bảng `cohort_schedules`. Học viên truy cập lớp học sẽ thấy lịch trình mở bài và hạn nộp rõ ràng. |
| **Hậu điều kiện** | Bài giảng và bài kiểm tra tự động khóa/mở và kiểm soát hạn chót nộp bài theo đúng lịch trình của Đợt học. |

---

### UC-37: Quản lý thành viên, Chấm bài tập Assignment & Tiến độ lớp học

| Thuộc tính | Chi tiết mô tả |
|------------|----------------|
| **Mã Use Case** | UC-37 |
| **Tên Use Case** | Quản lý thành viên, Chấm bài tập Assignment & Tiến độ lớp (Cohort Members & Assignment Grading) |
| **Actor chính** | Instructor |
| **Mô tả** | Giảng viên theo dõi danh sách học viên đã ghi danh vào Đợt học, kiểm tra tiến độ học bài, quản lý vòng đời lớp học tự động (Auto-Sync Lifecycle), và trực tiếp chấm điểm, nhận xét các bài tập tự luận (Assignment) do học viên nộp theo từng Cohort. |
| **Luồng sự kiện chính** | 1. **Đồng bộ Vòng đời Đợt học tự động**:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Giảng viên mở `/instructor/cohorts`, hệ thống tự động kích hoạt đồng bộ hàng loạt trạng thái (`syncCohortStatuses()`): các đợt đến giờ mở ghi danh chuyển thành `ENROLLING`, đến giờ học thành `IN_PROGRESS`, và hết giờ thành `COMPLETED`.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Giảng viên xem thống kê sĩ số hiện tại (`current_enrolled / max_capacity`) và danh sách thành viên trong bảng `cohort_members`.<br/>2. **Quản lý & Chấm điểm bài tập tự luận Assignment theo Cohort**:<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Tại giáo trình Cohort (`module-curriculum.html` hoặc `curriculum.html`), Giảng viên bấm nút **"Xem bài nộp"** tại bài tập Assignment tương ứng.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Hệ thống hiển thị bảng thống kê bài nộp của học viên trong lớp qua API `/api/instructor/assignments/{assignmentId}/submissions?cohortId={cohortId}`.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Hiển thị số lượng: Tổng bài nộp, số bài "Chờ chấm" (màu cam sẫm nổi bật `#b45309`), và các bài nộp trễ (badge đỏ `bg-danger text-white`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Bấm tải file bài làm của học viên về máy để thẩm định.<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Bấm **"Chấm điểm"** (hoặc "Nhận xét" nếu không tính điểm) -> Mở modal chấm điểm: Nhập số điểm đạt được (trên thang `maxScore`), ghi lời nhận xét đánh giá (`feedback`), tick chọn Đạt yêu cầu (`isPassed`).<br/>&nbsp;&nbsp;&nbsp;&nbsp;- Bấm "Lưu kết quả chấm" -> Hệ thống cập nhật bảng `assignment_submissions`, chuyển trạng thái thành `GRADED`. Nếu bài nộp đạt điểm sàn, tự động cập nhật hoàn thành bài học vào `lesson_completions` cho học viên.<br/>3. Giảng viên có thể chủ động đánh dấu hoàn thành/đóng đợt học (`COMPLETED`) bất kỳ lúc nào nếu kết thúc khóa học sớm. |
| **Hậu điều kiện** | Kết quả chấm điểm và nhận xét được lưu trữ đầy đủ, học viên nhận được phản hồi ngay lập tức trên không gian học tập. |

