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
    UC11 -.->|"<<includes>>"| UC11a["Thêm / Sửa danh mục &<br/>Tự sinh slug tiếng Việt"]
    UC11 -.->|"<<includes>>"| UC11b["Phân cấp cha-con &<br/>Chống vòng lặp đệ quy"]
    UC11 -.->|"<<includes>>"| UC11c["Xóa an toàn &<br/>Giải phóng khóa học"]
    UC11 -.->|"<<includes>>"| UC11d["Xem thống kê KPI<br/>danh mục"]
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

### UC-11: Quản lý danh mục khóa học (Category CRUD)

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Admin |
| **Permission** | `ROLE_ADMIN` |
| **Mô tả** | Quản lý danh mục khóa học đa cấp (Category CRUD), hỗ trợ tự động sinh slug tiếng Việt chuẩn hóa, phân cấp cha-con, thuật toán chống vòng lặp đệ quy, hiển thị KPI widget và giải phóng an toàn khóa học khi xóa. |
| **Tiền điều kiện** | Admin đã đăng nhập với vai trò Quản trị viên (`ADMIN`). |
| **Luồng chính** | 1. Admin truy cập `/admin/categories`.<br/>2. Hệ thống hiển thị 4 widget KPI (Tổng danh mục, Danh mục gốc, Danh mục con, Tổng khóa học) cùng bảng danh sách danh mục phân trang và tìm kiếm.<br/>3. Admin nhấn **"Thêm danh mục"** (`/admin/categories/create`) hoặc nút **"Sửa"** (`/admin/categories/edit/{id}`).<br/>4. Admin nhập Tên danh mục -> Giao diện tự động sinh Slug tiếng Việt không dấu chuẩn hóa SEO (có thể tinh chỉnh thủ công).<br/>5. Admin chọn Danh mục cha từ danh sách (hoặc để trống nếu là danh mục gốc).<br/>6. Admin nhấn **"Lưu danh mục"**.<br/>7. Server kiểm tra tính hợp lệ dữ liệu: tên không rỗng, slug duy nhất không trùng lặp, chống gán danh mục cha là chính nó hoặc con cháu của nó (chống vòng lặp đệ quy).<br/>8. Dữ liệu được lưu vào bảng `categories`, hiển thị thông báo thành công. |
| **Luồng ngoại lệ** | - Trùng slug: Hệ thống báo lỗi "Slug danh mục đã tồn tại".<br/>- Phát hiện vòng lặp cha-con: Hệ thống ném ngoại lệ và yêu cầu chọn danh mục cha hợp lệ.<br/>- Khi xóa danh mục: Bật Modal xác nhận `#deleteCategoryModal`. Hệ thống tự động gán `category_id = NULL` cho các Course liên quan để bảo toàn khóa học và xử lý an toàn các danh mục con. |
| **Hậu điều kiện** | Cấu trúc danh mục khóa học được cập nhật tức thì trên toàn hệ thống (bao gồm Navbar, bộ lọc khóa học và form biên soạn khóa học). |

### UC-12: Quản lý điểm danh Moderator

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Admin |
| **Mô tả** | Theo dõi và quản lý bảng điểm danh của Moderator |
| **Luồng chính** | 1. Admin truy cập `/admin/attendance` <br/> 2. Xem danh sách điểm danh theo ngày/tuần <br/> 3. Xác nhận / Từ chối ca làm việc |

