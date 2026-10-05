package vn.edu.ute.utelearn;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UtelearnApplicationTests {

    @Test
    void testApplicationInitialization() {
        UtelearnApplication app = new UtelearnApplication();
        assertNotNull(app, "Application instance must be successfully initialized");
    }

}

