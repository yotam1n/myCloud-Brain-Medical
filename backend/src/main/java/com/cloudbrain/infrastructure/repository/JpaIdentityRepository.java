package com.cloudbrain.infrastructure.repository;

import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.domain.auth.AccountProfile;
import com.cloudbrain.domain.auth.PatientProfile;
import com.cloudbrain.dto.auth.RegisterRequest;
import com.cloudbrain.entity.auth.AdminEntity;
import com.cloudbrain.entity.auth.DoctorEntity;
import com.cloudbrain.entity.auth.PatientEntity;
import com.cloudbrain.repository.AdminJpaRepository;
import com.cloudbrain.repository.DoctorJpaRepository;
import com.cloudbrain.repository.IdentityRepository;
import com.cloudbrain.repository.PatientJpaRepository;
import com.cloudbrain.security.ActorRole;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaIdentityRepository implements IdentityRepository {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String ADMIN_ROLE = "ADMIN";

    private final PatientJpaRepository patientJpaRepository;
    private final DoctorJpaRepository doctorJpaRepository;
    private final AdminJpaRepository adminJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public JpaIdentityRepository(PatientJpaRepository patientJpaRepository,
                                 DoctorJpaRepository doctorJpaRepository,
                                 AdminJpaRepository adminJpaRepository,
                                 PasswordEncoder passwordEncoder) {
        this.patientJpaRepository = patientJpaRepository;
        this.doctorJpaRepository = doctorJpaRepository;
        this.adminJpaRepository = adminJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<AccountProfile> findByUsername(String username) {
        return patientJpaRepository.findByUsername(username)
                .map(this::toAccountProfile)
                .or(() -> doctorJpaRepository.findByUsername(username).map(this::toAccountProfile))
                .or(() -> adminJpaRepository.findByUsername(username).map(this::toAccountProfile));
    }

    @Override
    public Optional<AccountProfile> findByUserId(Long userId) {
        return patientJpaRepository.findById(userId)
                .map(this::toAccountProfile)
                .or(() -> doctorJpaRepository.findById(userId).map(this::toAccountProfile))
                .or(() -> adminJpaRepository.findById(userId).map(this::toAccountProfile));
    }

    @Override
    public Optional<PatientProfile> findPatientProfile(Long patientId) {
        return patientJpaRepository.findById(patientId).map(this::toPatientProfile);
    }

    @Override
    @Transactional
    public synchronized AccountProfile registerPatient(RegisterRequest request, String passwordHash) {
        if (findByUsername(request.username()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "username already exists");
        }
        if (request.phone() != null && patientJpaRepository.existsByPhone(request.phone())) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "phone already exists");
        }

        PatientEntity patientEntity = new PatientEntity();
        patientEntity.setUsername(request.username());
        patientEntity.setPasswordHash(passwordHash);
        patientEntity.setPhone(request.phone());
        patientEntity.setName(request.realName());
        patientEntity.setGender(request.gender());
        patientEntity.setAge(request.age());
        patientEntity.setStatus(ACTIVE_STATUS);
        PatientEntity saved = patientJpaRepository.save(patientEntity);
        return toAccountProfile(saved);
    }

    @Override
    @Transactional
    public PatientProfile savePatientProfile(PatientProfile profile) {
        PatientEntity patientEntity = patientJpaRepository.findById(profile.patientId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "patient profile not found"));

        if (profile.phone() != null) {
            patientJpaRepository.findByPhone(profile.phone())
                    .filter(existing -> !existing.getId().equals(profile.patientId()))
                    .ifPresent(existing -> {
                        throw new ApiException(HttpStatus.CONFLICT.value(), "phone already exists");
                    });
        }

        patientEntity.setName(profile.realName());
        patientEntity.setGender(profile.gender());
        patientEntity.setAge(profile.age());
        patientEntity.setPhone(profile.phone());
        patientEntity.setIdCardNumber(profile.idCardNumber());
        patientEntity.setMedicalHistory(profile.medicalHistory());
        patientEntity.setRemark(profile.remark());
        patientEntity.setStatus(ACTIVE_STATUS);
        return toPatientProfile(patientJpaRepository.save(patientEntity));
    }

    private AccountProfile toAccountProfile(PatientEntity patientEntity) {
        return new AccountProfile(
                patientEntity.getId(),
                ActorRole.PATIENT,
                patientEntity.getUsername(),
                patientEntity.getPasswordHash(),
                patientEntity.getId(),
                null,
                patientEntity.getName(),
                patientEntity.getPhone()
        );
    }

    private AccountProfile toAccountProfile(DoctorEntity doctorEntity) {
        return new AccountProfile(
                doctorEntity.getId(),
                ActorRole.DOCTOR,
                doctorEntity.getUsername(),
                doctorEntity.getPasswordHash(),
                null,
                doctorEntity.getId(),
                doctorEntity.getName(),
                null
        );
    }

    private AccountProfile toAccountProfile(AdminEntity adminEntity) {
        return new AccountProfile(
                adminEntity.getId(),
                ActorRole.ADMIN,
                adminEntity.getUsername(),
                adminEntity.getPasswordHash(),
                null,
                null,
                adminEntity.getName(),
                null
        );
    }

    private PatientProfile toPatientProfile(PatientEntity patientEntity) {
        return new PatientProfile(
                patientEntity.getId(),
                patientEntity.getUsername(),
                patientEntity.getName(),
                patientEntity.getGender(),
                patientEntity.getAge(),
                patientEntity.getPhone(),
                patientEntity.getIdCardNumber(),
                patientEntity.getMedicalHistory(),
                patientEntity.getRemark()
        );
    }
}
