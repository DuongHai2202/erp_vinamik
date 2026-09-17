package vn.vinamik.erp_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class erp_backend_application {

    public static void main(String[] args) {
        SpringApplication.run(erp_backend_application.class, args);
    }
}