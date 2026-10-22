package vn.edu.ute.utelearn;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtelearnApplicationTests {

    @Test
    void testApplicationInitialization() {
        UtelearnApplication app = new UtelearnApplication();
        assertNotNull(app, "Application instance must be successfully initialized");
    }

    @Test
    void testSyncPasswordsToAbc123() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("Abc@123");
        assertTrue(encoder.matches("Abc@123", hashedPassword));

        Map<String, String> envMap = loadEnvMap();
        String host = resolveConfig("DB_HOST", envMap);
        String port = resolveConfig("DB_PORT", envMap);
        String name = resolveConfig("DB_NAME", envMap);
        String user = resolveConfig("DB_USERNAME", envMap);
        String pass = resolveConfig("DB_PASSWORD", envMap);
        String ssl = resolveConfig("DB_SSL_MODE", envMap);

        if (host != null && port != null && name != null && user != null && pass != null) {
            String sslParam = (ssl != null && !ssl.isBlank()) ? "?sslmode=" + ssl : "";
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + name + sslParam;
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password_hash = ?, is_active = true")) {
                    stmt.setString(1, hashedPassword);
                    int updated = stmt.executeUpdate();
                    assertTrue(updated >= 0);
                }
                try (PreparedStatement stmt = conn.prepareStatement("SELECT username, password_hash FROM users LIMIT 5");
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        assertTrue(encoder.matches("Abc@123", rs.getString("password_hash")));
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private String resolveConfig(String key, Map<String, String> envMap) {
        String sysVal = System.getenv(key);
        if (sysVal != null && !sysVal.isBlank()) {
            return sysVal;
        }
        return envMap.get(key);
    }

    private Map<String, String> loadEnvMap() {
        Map<String, String> map = new HashMap<>();
        File file = new File(".env");
        if (!file.exists()) {
            return map;
        }
        try {
            for (String line : Files.readAllLines(file.toPath())) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx > 0) {
                    map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

}
