package com.cloudbrain.repository;

import com.cloudbrain.domain.auth.AccountProfile;
import com.cloudbrain.domain.auth.PatientProfile;
import com.cloudbrain.dto.auth.RegisterRequest;
import java.util.Optional;

public interface IdentityRepository {

    Optional<AccountProfile> findByUsername(String username);

    Optional<AccountProfile> findByUserId(Long userId);

    Optional<PatientProfile> findPatientProfile(Long patientId);

    AccountProfile registerPatient(RegisterRequest request, String passwordHash);

    PatientProfile savePatientProfile(PatientProfile profile);
}
