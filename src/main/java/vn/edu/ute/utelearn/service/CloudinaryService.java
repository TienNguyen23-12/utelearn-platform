package vn.edu.ute.utelearn.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

public interface CloudinaryService {
        Map<String, Object> uploadFile(MultipartFile file, String folder, String resourceType) throws IOException;

        boolean deleteFile(String publicId, String resourceType) throws IOException;

        Map<String, Object> generateSignature(String folder, String resourceType, String originalFilename);

    Integer getVideoDuration(String videoUrl);
}
