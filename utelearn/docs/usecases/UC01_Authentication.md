# UC01 – Xác thực & Phân quyền (Authentication & Authorization)

## Use Case Diagram

```mermaid
graph LR
    Guest["Guest"]
    User["Authenticated User"]
    Google["Google OAuth2<br/>(External System)"]
    MailServer["Mail Server<br/>(External System)"]

    subgraph UC_Auth["Authentication & Authorization"]
        UC01["UC-01: Đăng nhập<br/>bằng Email/Password"]
        UC02["UC-02: Đăng ký tài khoản mới"]
        UC03["UC-03: Đăng nhập qua Google OAuth2"]
        UC04["UC-04: Đăng xuất"]
        UC05["UC-05: Quên mật khẩu"]
        UC06["UC-06: Đặt lại mật khẩu"]
        UC07["UC-07: Cập nhật hồ sơ cá nhân"]
    end

    Guest --> UC01
    Guest --> UC02
    Guest --> UC03
    Guest --> UC05

    User --> UC04
    User --> UC07

    UC03 -.->|"<<uses>>"| Google
    UC05 -.->|"<<uses>>"| MailServer
    UC06 -.->|"<<extends>>"| UC05

    UC01 -.->|"<<includes>>"| JWT["Cấp JWT Token"]
    UC03 -.->|"<<includes>>"| JWT
```

## Chi tiết Use Case

### UC-01: Đăng nhập bằng Email/Password

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Guest |
| **Mô tả** | Người dùng đăng nhập bằng email và mật khẩu đã đăng ký |
| **Tiền điều kiện** | Đã có tài khoản trong hệ thống |
| **Luồng chính** | 1. Guest truy cập `/login` <br/> 2. Nhập email và password <br/> 3. Hệ thống xác thực thông tin <br/> 4. Cấp JWT Token lưu vào Cookie `UTELearn_Token` <br/> 5. Redirect theo role: Admin -> `/dashboard`, Others -> `/home` |
| **Luồng ngoại lệ** | - Sai mật khẩu -> Hiển thị lỗi <br/> - Tài khoản bị khóa -> Thông báo liên hệ admin |
| **Hậu điều kiện** | User đã đăng nhập, JWT Token hợp lệ |

### UC-02: Đăng ký tài khoản mới

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Guest |
| **Mô tả** | Tạo tài khoản mới với email, mật khẩu và xác nhận mật khẩu |
| **Tiền điều kiện** | Chưa có tài khoản |
| **Luồng chính** | 1. Guest truy cập `/register` <br/> 2. Nhập email, password, confirm password <br/> 3. Hệ thống validate (email unique, password match) <br/> 4. Tạo user với role mặc định `STUDENT` <br/> 5. Tự động đăng nhập và redirect -> `/home` |
| **Luồng ngoại lệ** | - Email đã tồn tại -> Thông báo lỗi <br/> - Password không khớp -> Hiển thị lỗi validation |
| **Hậu điều kiện** | Tài khoản mới được tạo với role STUDENT |

### UC-03: Đăng nhập qua Google OAuth2

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Guest |
| **Mô tả** | Đăng nhập bằng tài khoản Google, hệ thống tự động tạo tài khoản nếu chưa có |
| **Tiền điều kiện** | Có tài khoản Google hợp lệ |
| **Luồng chính** | 1. Guest click "Đăng nhập với Google" <br/> 2. Redirect đến Google OAuth2 consent screen <br/> 3. Xác thực thành công -> Callback về hệ thống <br/> 4. Hệ thống kiểm tra email, tự tạo account nếu cần <br/> 5. Cấp JWT Token -> Redirect theo role |
| **Luồng ngoại lệ** | - Từ chối ủy quyền -> Quay lại `/login` |
| **Hậu điều kiện** | User đăng nhập thành công |

### UC-04: Đăng xuất

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Authenticated User (tất cả roles) |
| **Mô tả** | Thoát khỏi phiên làm việc hiện tại |
| **Luồng chính** | 1. Click "Đăng xuất" <br/> 2. Xóa cookie `UTELearn_Token` và `JSESSIONID` <br/> 3. Redirect -> `/login?logout=true` |
| **Hậu điều kiện** | JWT Token bị xóa, session kết thúc |

### UC-05: Quên mật khẩu

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Guest |
| **Mô tả** | Yêu cầu reset mật khẩu qua email |
| **Luồng chính** | 1. Guest truy cập `/forgot-password` <br/> 2. Nhập email đã đăng ký <br/> 3. Hệ thống tạo token reset và gửi email <br/> 4. Hiển thị thông báo "Đã gửi email" |
| **Luồng ngoại lệ** | - Email không tồn tại -> Vẫn hiển thị thông báo (bảo mật) |

### UC-06: Đặt lại mật khẩu

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Guest (có token hợp lệ) |
| **Mô tả** | Đặt mật khẩu mới thông qua link reset |
| **Luồng chính** | 1. Click link trong email `/reset-password?token=xxx` <br/> 2. Nhập mật khẩu mới và xác nhận <br/> 3. Hệ thống validate token và cập nhật password <br/> 4. Redirect -> `/login` |
| **Luồng ngoại lệ** | - Token hết hạn hoặc không hợp lệ -> Thông báo lỗi |

### UC-07: Cập nhật hồ sơ cá nhân

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Authenticated User |
| **Mô tả** | Cập nhật thông tin cá nhân (họ tên, SĐT, avatar...) |
| **Luồng chính** | 1. Truy cập `/profile` <br/> 2. Chỉnh sửa thông tin (fullName, phone, avatar) <br/> 3. Submit form -> Validate -> Lưu DB <br/> 4. Hiển thị toast thành công |
| **Hậu điều kiện** | Thông tin profile được cập nhật |
