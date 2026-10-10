# UC07 – Quản lý Tài nguyên số (Instructor Asset Management)

## Use Case Diagram

```mermaid
graph LR
    Instructor["Instructor"]
    Student["Student"]

    subgraph UC_Asset["Kho Tài nguyên số"]
        UC30["UC-30: Upload tài nguyên"]
        UC31["UC-31: Quản lý kho<br/>tài nguyên"]
        UC32["UC-32: Tải tài nguyên"]
    end

    Instructor --> UC30
    Instructor --> UC31
    Student --> UC32

    UC30 -.->|"<<includes>>"| UC30a["Upload file<br/>(PDF/Video/Image/Doc)"]
    UC30 -.->|"<<includes>>"| UC30b["Gắn metadata<br/>(title, type, course)"]
    UC31 -.->|"<<includes>>"| UC31a["Xem / Xóa /<br/>Cập nhật"]
    UC31 -.->|"<<includes>>"| UC31b["Filter theo<br/>loại / khóa học"]
```

## Chi tiết Use Case

### UC-30: Upload tài nguyên

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Instructor |
| **Permission** | `ASSET_MANAGE` |
| **Mô tả** | Upload file tài liệu, video, hình ảnh vào kho cá nhân |
| **Luồng chính** | 1. Instructor truy cập `/instructor/assets` <br/> 2. Click "Upload" <br/> 3. Chọn file từ máy tính <br/> 4. Nhập metadata: title, type (PDF/VIDEO/IMAGE/DOCUMENT), mô tả <br/> 5. Liên kết với khóa học (optional) <br/> 6. Submit -> File được lưu trữ |
| **Giới hạn** | Kích thước tối đa theo cấu hình hệ thống |
| **Dữ liệu** | `InstructorAsset`: fileName, fileType, fileSize, storagePath |

### UC-31: Quản lý kho tài nguyên

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Instructor |
| **Permission** | `ASSET_MANAGE` |
| **Mô tả** | Xem, sửa, xóa tài nguyên đã upload |
| **Luồng chính** | 1. Instructor truy cập `/instructor/assets` <br/> 2. Xem danh sách tài nguyên (phân trang) <br/> 3. Filter theo: type, khóa học liên kết <br/> 4. Actions: <br/> - **Xem**: Preview/Download file <br/> - **Sửa**: Đổi title, mô tả <br/> - **Xóa**: Xóa file và bản ghi |
| **Luồng ngoại lệ** | - File đang được sử dụng trong Lesson -> Cảnh báo trước khi xóa |

### UC-32: Tải tài nguyên

| Thuộc tính | Mô tả |
|------------|--------|
| **Actor** | Student (đã đăng ký khóa học) |
| **Tiền điều kiện** | Student đã enroll vào khóa học chứa tài nguyên |
| **Mô tả** | Tải file tài nguyên đính kèm trong bài giảng |
| **Luồng chính** | 1. Student đang học Lesson có file đính kèm <br/> 2. Click "Tải xuống" <br/> 3. Hệ thống kiểm tra quyền truy cập <br/> 4. Serve file qua `/assets/download/{id}` |
| **Luồng ngoại lệ** | - Chưa đăng ký -> 403 Forbidden |
