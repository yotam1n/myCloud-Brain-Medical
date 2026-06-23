package com.cloudbrain.dto;

public record HealthResponse(String service, String status, String javaVersion) {
}
