package vn.edu.ute.utelearn.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CloudinaryConfig {

    private final UtelearnProperties properties;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.getCloudinary().getCloudName(),
                "api_key", properties.getCloudinary().getApiKey(),
                "api_secret", properties.getCloudinary().getApiSecret(),
                "secure", true
        ));
    }
}
