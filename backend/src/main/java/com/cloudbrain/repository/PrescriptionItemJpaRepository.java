package com.cloudbrain.repository;

import com.cloudbrain.entity.core.PrescriptionItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemJpaRepository extends JpaRepository<PrescriptionItemEntity, Long> {
}
