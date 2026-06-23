package com.cloudbrain.repository;

import com.cloudbrain.entity.core.DepartmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, Long> {

    Optional<DepartmentEntity> findByCode(String code);

    List<DepartmentEntity> findByStatusOrderByNameAsc(String status);
}
