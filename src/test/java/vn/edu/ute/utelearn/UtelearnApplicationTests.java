package vn.edu.ute.utelearn;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
    }
}
