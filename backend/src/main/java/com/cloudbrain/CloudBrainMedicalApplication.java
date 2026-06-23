package com.cloudbrain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cloudbrain")
public class CloudBrainMedicalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudBrainMedicalApplication.class, args);
    }
}
