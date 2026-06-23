package com.cloudbrain.dto.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 128) String password,
        @NotBlank @Size(max = 64) String realName,
        @NotBlank @Size(max = 32) String phone,
        @Size(max = 32) String gender,
        @Min(0) Integer age
) {
}
