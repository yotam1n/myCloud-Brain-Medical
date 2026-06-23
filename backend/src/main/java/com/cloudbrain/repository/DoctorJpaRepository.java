package com.cloudbrain.repository;

import com.cloudbrain.entity.auth.DoctorEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorJpaRepository extends JpaRepository<DoctorEntity, Long> {

    Optional<DoctorEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    List<DoctorEntity> findByDepartmentIdAndStatusOrderByNameAsc(Long departmentId, String status);

    Optional<DoctorEntity> findByIdAndStatus(Long id, String status);
}
