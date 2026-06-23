package com.cloudbrain.repository;

import com.cloudbrain.entity.core.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackJpaRepository extends JpaRepository<FeedbackEntity, Long> {
}
