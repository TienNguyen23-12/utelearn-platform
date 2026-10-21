package vn.edu.ute.utelearn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class KeepAliveService {

    @Value("${utelearn.render.keep-alive-url:${RENDER_EXTERNAL_URL:}}")
    private String keepAliveUrl;

    @Value("${utelearn.render.keep-alive-enabled:true}")
    private boolean keepAliveEnabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Scheduled(fixedRate = 720000, initialDelay = 60000)
    public void pingSelf() {
        if (!keepAliveEnabled || keepAliveUrl == null || keepAliveUrl.isBlank()) {
            return;
        }

        String targetUrl = resolveTargetUrl(keepAliveUrl);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Render keep-alive ping success: {} (Status: {})", targetUrl, response.statusCode());
            } else {
                log.warn("Render keep-alive ping non-2xx status: {} (Status: {})", targetUrl, response.statusCode());
            }
        } catch (Exception e) {
            log.error("Render keep-alive ping failed for {}: {}", targetUrl, e.getMessage());
        }
    }

    private String resolveTargetUrl(String rawUrl) {
        String cleanUrl = rawUrl.trim();
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://" + cleanUrl;
        }
        cleanUrl = cleanUrl.replaceAll("/+$", "");
        if (cleanUrl.endsWith(".onrender.com") || cleanUrl.matches("^https?://[^/]+$")) {
            return cleanUrl + "/api/health";
        }
        return cleanUrl;
    }
}
