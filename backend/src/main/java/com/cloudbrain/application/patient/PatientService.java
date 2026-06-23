package com.cloudbrain.application.patient;

import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.domain.auth.PatientProfile;
import com.cloudbrain.dto.patient.PatientInfoResponse;
import com.cloudbrain.dto.patient.PatientUpdateRequest;
import com.cloudbrain.repository.IdentityRepository;
import com.cloudbrain.security.ActorContext;
import com.cloudbrain.security.RolePolicy;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final IdentityRepository identityRepository;
    private final RolePolicy rolePolicy;

    public PatientService(IdentityRepository identityRepository, RolePolicy rolePolicy) {
        this.identityRepository = identityRepository;
        this.rolePolicy = rolePolicy;
    }

    public PatientInfoResponse getCurrentPatientInfo(ActorContext actorContext) {
        PatientProfile profile = requireCurrentProfile(actorContext);
        return PatientInfoResponse.from(profile);
    }

    public PatientInfoResponse updateCurrentPatientInfo(ActorContext actorContext, PatientUpdateRequest request) {
        PatientProfile profile = requireCurrentProfile(actorContext);
        PatientProfile updated = new PatientProfile(
                profile.patientId(),
                profile.username(),
                firstNonBlank(request.realName(), profile.realName()),
                firstNonBlank(request.gender(), profile.gender()),
                request.age() != null ? request.age() : profile.age(),
                firstNonBlank(request.phone(), profile.phone()),
                firstNonBlank(request.idCardNumber(), profile.idCardNumber()),
                firstNonBlank(request.medicalHistory(), profile.medicalHistory()),
                firstNonBlank(request.remark(), profile.remark())
        );
        return PatientInfoResponse.from(identityRepository.savePatientProfile(updated));
    }

    private PatientProfile requireCurrentProfile(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isPatient()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "patient permission required");
        }
        Long patientId = actorContext.getRequiredPatientId();
        if (!rolePolicy.canViewMedicalRecord(actorContext, patientId)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "patient permission required");
        }
        return identityRepository.findPatientProfile(patientId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "patient profile not found"));
    }

    private String firstNonBlank(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        return candidate;
    }
}
