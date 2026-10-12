package vn.edu.ute.utelearn.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

public interface CloudinaryService {
    /**
     * Upload file lên Cloudinary
     * @param file File cần upload
     * @param folder Tên thư mục con (ví dụ: /java/week1)
     * @param resourceType Loại file (auto, image, video, raw)
     * @return Map chứa thông tin file trả về từ Cloudinary (url, public_id, bytes...)
     */
    Map<String, Object> uploadFile(MultipartFile file, String folder, String resourceType) throws IOException;

    /**
     * Xóa file khỏi Cloudinary
     * @param publicId ID của file
     * @param resourceType Loại file cần xóa
     * @return true nếu xóa thành công
     */
    boolean deleteFile(String publicId, String resourceType) throws IOException;

    /**
     * Tạo signature để frontend upload trực tiếp
     */
    Map<String, Object> generateSignature(String folder, String resourceType, String originalFilename);
}
