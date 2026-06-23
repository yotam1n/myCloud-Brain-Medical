package com.cloudbrain.repository;

import com.cloudbrain.entity.core.MedicalRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordJpaRepository extends JpaRepository<MedicalRecordEntity, Long> {

    List<MedicalRecordEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<MedicalRecordEntity> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    List<MedicalRecordEntity> findByRegistrationIdOrderByVersionDesc(Long registrationId);

    Optional<MedicalRecordEntity> findFirstByRegistrationIdOrderByVersionDesc(Long registrationId);

    Optional<MedicalRecordEntity> findByAiCallRecordId(Long aiCallRecordId);
}
