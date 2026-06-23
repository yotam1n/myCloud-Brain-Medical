package com.cloudbrain.dto.patient;

import com.cloudbrain.domain.auth.PatientProfile;

public record PatientInfoResponse(
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

    public static PatientInfoResponse from(PatientProfile profile) {
        return new PatientInfoResponse(
                profile.patientId(),
                profile.username(),
                profile.realName(),
                profile.gender(),
                profile.age(),
                profile.phone(),
                profile.idCardNumber(),
                profile.medicalHistory(),
                profile.remark()
        );
    }
}
