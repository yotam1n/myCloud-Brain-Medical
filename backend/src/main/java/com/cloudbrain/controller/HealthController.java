package com.cloudbrain.controller;

import com.cloudbrain.common.Result;
import com.cloudbrain.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<HealthResponse> health() {
        HealthResponse response = new HealthResponse(
                "cloud-brain-medical-backend",
                "UP",
                System.getProperty("java.version", "unknown")
        );
        return Result.success(response);
    }
}
