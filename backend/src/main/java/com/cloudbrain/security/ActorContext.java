package com.cloudbrain.security;

public record ActorContext(
        Long userId,
        ActorRole role,
        Long patientId,
        Long doctorId,
        String username,
        String displayName
) {

    public boolean isPatient() {
        return role == ActorRole.PATIENT;
    }

    public boolean isDoctor() {
        return role == ActorRole.DOCTOR;
    }

    public boolean isAdmin() {
        return role == ActorRole.ADMIN;
    }

    public Long getRequiredPatientId() {
        if (patientId == null) {
            throw new IllegalStateException("patient id is missing");
        }
        return patientId;
    }

    public Long getRequiredDoctorId() {
        if (doctorId == null) {
            throw new IllegalStateException("doctor id is missing");
        }
        return doctorId;
    }
}
