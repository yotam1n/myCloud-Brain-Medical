package com.cloudbrain.repository;

import com.cloudbrain.entity.core.AIConfigEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIConfigJpaRepository extends JpaRepository<AIConfigEntity, Long> {

    Optional<AIConfigEntity> findFirstByTaskScopeAndDefaultConfigTrueAndEnabledTrueAndStatusOrderByPriorityDescUpdatedAtDesc(
            String taskScope,
            String status
    );

    Optional<AIConfigEntity> findFirstByTaskScopeAndEnabledTrueAndStatusOrderByPriorityDescUpdatedAtDesc(
            String taskScope,
            String status
    );

    List<AIConfigEntity> findByTaskScopeAndEnabledTrueAndStatusOrderByPriorityDescUpdatedAtDesc(
            String taskScope,
            String status
    );

    boolean existsByTaskScopeAndDefaultConfigTrueAndEnabledTrueAndStatus(String taskScope, String status);
}
