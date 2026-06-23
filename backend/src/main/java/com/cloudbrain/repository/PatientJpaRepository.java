package com.cloudbrain.repository;

import com.cloudbrain.entity.auth.PatientEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientJpaRepository extends JpaRepository<PatientEntity, Long> {

    Optional<PatientEntity> findByUsername(String username);

    Optional<PatientEntity> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);
}
