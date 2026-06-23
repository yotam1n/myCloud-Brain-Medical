package com.cloudbrain.dto.patient;

import jakarta.validation.constraints.Size;

public record PatientUpdateRequest(
        @Size(max = 64) String realName,
        @Size(max = 32) String gender,
        Integer age,
        @Size(max = 32) String phone,
        @Size(max = 64) String idCardNumber,
        @Size(max = 512) String medicalHistory,
        @Size(max = 512) String remark
) {
}
