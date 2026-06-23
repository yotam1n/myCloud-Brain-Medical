package com.cloudbrain.repository;

import com.cloudbrain.entity.core.PrescriptionReviewEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionReviewJpaRepository extends JpaRepository<PrescriptionReviewEntity, Long> {

    Optional<PrescriptionReviewEntity> findByIdAndBindStatus(Long id, String bindStatus);

    List<PrescriptionReviewEntity> findByRegistrationIdOrderByCreatedAtDesc(Long registrationId);

    List<PrescriptionReviewEntity> findByPrescriptionIdOrderByCreatedAtDesc(Long prescriptionId);

    List<PrescriptionReviewEntity> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    List<PrescriptionReviewEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<PrescriptionReviewEntity> findByRiskLevelOrderByCreatedAtDesc(String riskLevel);

    Optional<PrescriptionReviewEntity> findFirstByPrescriptionIdAndPrescriptionSnapshotHashAndReviewContextHashOrderByCreatedAtDesc(
            Long prescriptionId,
            String prescriptionSnapshotHash,
            String reviewContextHash
    );

    Optional<PrescriptionReviewEntity> findByAiCallRecordId(Long aiCallRecordId);
}
