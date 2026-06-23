package com.cloudbrain.repository;

import com.cloudbrain.entity.core.PrescriptionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionJpaRepository extends JpaRepository<PrescriptionEntity, Long> {

    List<PrescriptionEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<PrescriptionEntity> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    List<PrescriptionEntity> findByRegistrationIdOrderByCreatedAtDesc(Long registrationId);

    Optional<PrescriptionEntity> findByReviewId(Long reviewId);
}
