package vn.edu.ute.utelearn.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "utelearn")
public class UtelearnProperties {

    private Jwt jwt = new Jwt();
    private Cloudinary cloudinary = new Cloudinary();
    private Moderation moderation = new Moderation();
    private Websocket websocket = new Websocket();
    private Seeder seeder = new Seeder();
    private Payment payment = new Payment();
    private Render render = new Render();

    @Data
    public static class Render {
        private String keepAliveUrl = "https://utelearn-platform.onrender.com";
        private boolean keepAliveEnabled = true;
    }

    @Data
    public static class Payment {
        private String bankId = "Vietcombank";
        private String accountNo = "";
        private String accountName = "";
        private String template = "compact2";
        private String sepayApiKey = "";
        private boolean simulationEnabled = true;
    }

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs = 900000L;
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
        private String defaultPassword = "Abc@1234";
    }
}
