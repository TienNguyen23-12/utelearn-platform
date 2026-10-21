package vn.edu.ute.utelearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UtelearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(UtelearnApplication.class, args);
    }

}
