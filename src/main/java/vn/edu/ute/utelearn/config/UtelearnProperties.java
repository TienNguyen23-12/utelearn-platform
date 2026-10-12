package vn.edu.ute.utelearn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration properties mapping cho hệ thống UTELearn từ application.yml
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "utelearn")
public class UtelearnProperties {

    private Jwt jwt = new Jwt();
    private Cloudinary cloudinary = new Cloudinary();
    private Moderation moderation = new Moderation();
    private Websocket websocket = new Websocket();
    private Seeder seeder = new Seeder();

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs = 900000L; // 15 minutes (15 * 60 * 1000 ms)
        private long refreshTokenExpirationMs = 604800000L;
        private String cookieName = "UTELearn_Token";
        private String headerPrefix = "Bearer ";
    }

    @Data
    public static class Cloudinary {
        private String cloudName;
        private String apiKey;
        private String apiSecret;
        private String folderBase = "utelearn_assets";
    }

    @Data
    public static class Moderation {
        private int maxDailyMinutes = 480;
        private int defaultReviewMinutes = 60;
        private int slaTimeoutHours = 24;
    }

    @Data
    public static class Websocket {
        private String endpoint = "/ws";
        private String appDestinationPrefix = "/app";
        private String topicDestinationPrefix = "/topic";
        private String queueDestinationPrefix = "/queue";
    }

    @Data
    public static class Seeder {
        private boolean enabled = true;
        private boolean seedSampleUsers = true;
        private String defaultPassword = "password123";
    }
}
