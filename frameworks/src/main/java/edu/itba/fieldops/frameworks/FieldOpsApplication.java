package edu.itba.fieldops.frameworks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"edu.itba.fieldops.api", "edu.itba.fieldops.frameworks"})
public class FieldOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(FieldOpsApplication.class, args);
    }
}
