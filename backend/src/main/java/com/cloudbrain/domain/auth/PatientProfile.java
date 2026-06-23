package com.cloudbrain.domain.auth;

public record PatientProfile(
        Long patientId,
        String username,
        String realName,
        String gender,
        Integer age,
        String phone,
        String idCardNumber,
        String medicalHistory,
        String remark
) {
}
