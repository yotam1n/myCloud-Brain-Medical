package com.cloudbrain.repository;

import com.cloudbrain.entity.auth.AdminEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminJpaRepository extends JpaRepository<AdminEntity, Long> {

    Optional<AdminEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
