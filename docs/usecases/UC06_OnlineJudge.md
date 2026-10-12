# UC06 – Online Judge (OJ) – DINH HUONG PHAT TRIEN

> **Lưu ý:** Module Online Judge hiện là **định hướng phát triển** trong tương lai.
> Các Use Case dưới đây mô tả chức năng dự kiến, chưa được triển khai trong phiên bản hiện tại.

## Use Case Diagram

```mermaid
graph LR
    Student["Student"]
    Instructor["Instructor"]
    Moderator["Moderator"]
    JudgeEngine["Judge Engine<br/>(External System)"]

    subgraph UC_OJ["Online Judge -- DINH HUONG PHAT TRIEN"]
        UC26["UC-26: Xem đề bài OJ"]
        UC27["UC-27: Nộp bài chấm code"]
        UC28["UC-28: Xem kết quả chấm"]
        UC29["UC-29: Tạo đề bài OJ"]
    end

    Student --> UC26
    Student --> UC27
    Student --> UC28

    Instructor --> UC29

    UC27 -.->|"<<uses>>"| JudgeEngine
    UC29 -.->|"<<triggers>>"| ModQueue["Đẩy vào hàng đợi<br/>kiểm duyệt"]
    ModQueue -.->|"<<notifies>>"| Moderator

    UC27 -.->|"<<includes>>"| UC27a["Chọn ngôn ngữ<br/>lập trình"]
    UC27 -.->|"<<includes>>"| UC27b["Viết / Paste<br/>source code"]
    UC28 -.->|"<<includes>>"| UC28a["Xem từng<br/>test case"]
```

## Chi tiết Use Case (Dự kiến)

### UC-26: Xem đề bài OJ

| Thuộc tính | Mô tả |
|------------|--------|
| **Trạng thái** | Định hướng phát triển |
| **Actor** | Student |
| **Mô tả** | Duyệt và xem danh sách đề bài lập trình |
| **Luồng chính** | 1. Student truy cập trang OJ <br/> 2. Xem danh sách problems (filter theo difficulty, tag) <br/> 3. Click vào problem -> Xem: <br/> - Mô tả đề bài <br/> - Input / Output format <br/> - Constraints <br/> - Sample test cases |
| **Dữ liệu** | `CodingProblem`: title, description, difficulty, time/memory limit |

### UC-27: Nộp bài chấm code

| Thuộc tính | Mô tả |
|------------|--------|
| **Trạng thái** | Định hướng phát triển |
| **Actor** | Student |
| **Permission** | `CODE_SUBMIT` |
| **Mô tả** | Submit source code để chấm tự động |
| **Luồng chính** | 1. Student chọn ngôn ngữ lập trình (Java/C++/Python...) <br/> 2. Viết hoặc paste code vào editor <br/> 3. Click "Submit" <br/> 4. Hệ thống gửi code đến Judge Engine <br/> 5. Judge chạy code với các test case <br/> 6. Trả về kết quả |
| **Hậu điều kiện** | Bản ghi `CodingSubmission` được tạo |

### UC-28: Xem kết quả chấm

| Thuộc tính | Mô tả |
|------------|--------|
| **Trạng thái** | Định hướng phát triển |
| **Actor** | Student |
| **Mô tả** | Xem kết quả submission sau khi chấm |
| **Luồng chính** | 1. Student xem lịch sử submissions <br/> 2. Mỗi submission hiển thị: <br/> - Status: AC / WA / TLE / RE / CE <br/> - Thời gian chạy, bộ nhớ sử dụng <br/> - Kết quả từng test case (nếu cho phép) |
| **Trạng thái kết quả** | `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILATION_ERROR` |

### UC-29: Tạo đề bài OJ

| Thuộc tính | Mô tả |
|------------|--------|
| **Trạng thái** | Định hướng phát triển |
| **Actor** | Instructor |
| **Mô tả** | Tạo đề bài lập trình mới cho Online Judge |
| **Luồng chính** | 1. Instructor truy cập trang tạo đề OJ <br/> 2. Nhập: title, description, difficulty, tags <br/> 3. Cấu hình: time limit, memory limit <br/> 4. Thêm test cases (input/output) <br/> 5. Submit -> Đẩy vào hàng đợi kiểm duyệt |
| **Hậu điều kiện** | Đề bài ở trạng thái PENDING, chờ Moderator duyệt |
