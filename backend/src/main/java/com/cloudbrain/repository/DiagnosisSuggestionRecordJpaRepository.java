package com.cloudbrain.repository;

import com.cloudbrain.entity.core.DiagnosisSuggestionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisSuggestionRecordJpaRepository extends JpaRepository<DiagnosisSuggestionRecordEntity, Long> {
}
