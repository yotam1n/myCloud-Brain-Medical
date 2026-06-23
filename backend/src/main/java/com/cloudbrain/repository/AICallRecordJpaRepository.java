package com.cloudbrain.repository;

import com.cloudbrain.entity.core.AICallRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AICallRecordJpaRepository extends JpaRepository<AICallRecordEntity, Long> {
}
