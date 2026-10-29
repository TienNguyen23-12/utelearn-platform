package vn.edu.ute.utelearn.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.service.CloudinaryService;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final UtelearnProperties properties;

    @Override
    public Map<String, Object> uploadFile(MultipartFile file, String folder, String resourceType) throws IOException {
        String baseFolder = properties.getCloudinary().getFolderBase();
        String safeFolder = sanitizePath(folder);
        String fullPath = baseFolder + (safeFolder.startsWith("/") ? safeFolder : "/" + safeFolder);

        String fileName = file != null ? file.getOriginalFilename() : "";
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf("."));
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        
        String safeFileName = sanitizeFilename(fileName);
        String publicId = safeFileName + "_" + UUID.randomUUID().toString().substring(0, 8);
        if ("raw".equalsIgnoreCase(resourceType)) {
            publicId += extension;
        }

        Map<String, Object> options = ObjectUtils.asMap(
                "folder", fullPath,
                "public_id", publicId,
                "resource_type", resourceType
        );

        log.info("Uploading file to Cloudinary: {} (type: {})", safeFileName, resourceType);
        byte[] bytes = file != null ? file.getBytes() : new byte[0];
        return cloudinary.uploader().upload(bytes, options);
    }

    @Override
    public boolean deleteFile(String publicId, String resourceType) throws IOException {
        Map<String, Object> options = ObjectUtils.asMap("resource_type", resourceType);
        Map result = cloudinary.uploader().destroy(publicId, options);
        return "ok".equals(result.get("result"));
    }

    @Override
    public Map<String, Object> generateSignature(String folder, String resourceType, String originalFilename) {
        String baseFolder = properties.getCloudinary().getFolderBase();
        String safeFolder = sanitizePath(folder);
        String fullPath = baseFolder + (safeFolder.startsWith("/") ? safeFolder : "/" + safeFolder);
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }

        String fileName = originalFilename;
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf("."));
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        
        String safeFileName = sanitizeFilename(fileName);
        String publicId = fullPath + safeFileName + "_" + UUID.randomUUID().toString().substring(0, 8);
        if ("raw".equalsIgnoreCase(resourceType)) {
            publicId += extension;
        }

        long timestamp = System.currentTimeMillis() / 1000L;
        
        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        paramsToSign.put("public_id", publicId);

        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

        Map<String, Object> response = new HashMap<>();
        response.put("signature", signature);
        response.put("timestamp", timestamp);
        response.put("public_id", publicId);
        response.put("api_key", cloudinary.config.apiKey);
        response.put("cloud_name", cloudinary.config.cloudName);
        
        return response;
    }

    @Override
    public Integer getVideoDuration(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            return null;
        }
        try {
            int uploadIndex = videoUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }
            String path = videoUrl.substring(uploadIndex + 8);
            if (path.matches("^v\\d+/.*")) {
                path = path.replaceFirst("^v\\d+/", "");
            }
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex != -1) {
                path = path.substring(0, dotIndex);
            }
            String publicId = java.net.URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8);
            Map<?, ?> result = cloudinary.api().resource(publicId, ObjectUtils.asMap("resource_type", "video", "image_metadata", true));
            if (result != null && result.get("duration") != null) {
                double duration = Double.parseDouble(result.get("duration").toString());
                int rounded = (int) Math.round(duration);
                log.info("[CloudinaryService] Đã nhận diện thời lượng video: {}s cho file: {}", rounded, publicId);
                return rounded;
            }
        } catch (Exception e) {
            log.warn("[CloudinaryService] Không thể lấy thời lượng video từ Cloudinary: {}", e.getMessage());
        }
        return null;
    }

    private String sanitizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(path.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^a-zA-Z0-9_/-]", "_").replaceAll("/+", "/");
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        String normalized = Normalizer.normalize(filename.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
