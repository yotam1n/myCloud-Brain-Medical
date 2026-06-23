package com.cloudbrain.repository;

import com.cloudbrain.entity.core.HISSyncRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HISSyncRecordJpaRepository extends JpaRepository<HISSyncRecordEntity, Long> {
}
