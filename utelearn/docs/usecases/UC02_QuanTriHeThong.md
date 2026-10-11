# UC02 – Quản trị Hệ thống (Admin Module)

## Use Case Diagram

```mermaid
graph LR
    Admin["Admin"]

    subgraph UC_Admin["Quản trị Hệ thống"]
        UC08["UC-08: Quản lý tài khoản<br/>người dùng"]
        UC09["UC-09: Cấu hình phân quyền<br/>động RBAC"]
        UC10["UC-10: Xem Dashboard<br/>thống kê"]
        UC11["UC-11: Quản lý danh mục<br/>khóa học"]
        UC12["UC-12: Quản lý điểm danh<br/>Moderator"]
    end

    Admin --> UC08
    Admin --> UC09
    Admin --> UC10
    Admin --> UC11
    Admin --> UC12

    UC08 -.->|"<<includes>>"| UC08a["Tạo / Sửa / Khóa<br/>tài khoản"]
    UC08 -.->|"<<includes>>"| UC08b["Gán vai trò<br/>cho user"]
    UC09 -.->|"<<includes>>"| UC09a["Tạo / Sửa / Xóa<br/>Role"]
    UC09 -.->|"<<includes>>"| UC09b["Ánh xạ Permission<br/>cho Role"]
```

## Chi tiết Use Case

### UC-08: Quản lý tài khoản người dùng

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Admin |
| **Permission** | `USER_MANAGE` |
| **Mô tả** | CRUD tài khoản, gán vai trò, kích hoạt / khóa tài khoản |
| **Tiền điều kiện** | Admin đã đăng nhập, có quyền `USER_MANAGE` |
| **Luồng chính** | 1. Admin truy cập `/admin/users` <br/> 2. Xem danh sách user (phân trang, tìm kiếm) <br/> 3. Click vào user -> Xem / Sửa thông tin <br/> 4. Gán / Gỡ vai trò <br/> 5. Kích hoạt / Vô hiệu hóa tài khoản |
| **Hậu điều kiện** | Thông tin user được cập nhật trong DB |

### UC-09: Cấu hình phân quyền động RBAC

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Admin |
| **Permission** | `ROLE_MANAGE` |
| **Mô tả** | Quản lý Roles và Permissions, ánh xạ permission cho từng role |
| **Luồng chính** | 1. Admin truy cập `/admin/roles` <br/> 2. Xem danh sách Role hiện có <br/> 3. Tạo mới / Chỉnh sửa Role <br/> 4. Tick chọn các Permission áp dụng cho Role <br/> 5. Lưu -> Hệ thống cập nhật bảng `role_permissions` |
| **Luồng ngoại lệ** | - Xóa role đang được gán -> Cảnh báo |

### UC-10: Xem Dashboard thống kê

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Admin |
| **Mô tả** | Xem tổng quan hệ thống: số user, khóa học, doanh thu, hoạt động |
| **Luồng chính** | 1. Admin truy cập `/dashboard` <br/> 2. Hệ thống hiển thị KPIs: <br/> - Tổng số user, instructor, student <br/> - Số khóa học (theo trạng thái) <br/> - Doanh thu (nếu có) <br/> - Hoạt động gần đây |

### UC-11: Quản lý danh mục khóa học

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Admin |
| **Mô tả** | CRUD danh mục (Category) cho khóa học |
| **Luồng chính** | 1. Admin truy cập quản lý danh mục <br/> 2. Thêm / Sửa / Xóa category <br/> 3. Mỗi category có `name` và `slug` |

### UC-12: Quản lý điểm danh Moderator

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Admin |
| **Mô tả** | Theo dõi và quản lý bảng điểm danh của Moderator |
| **Luồng chính** | 1. Admin truy cập `/admin/attendance` <br/> 2. Xem danh sách điểm danh theo ngày/tuần <br/> 3. Xác nhận / Từ chối ca làm việc |
