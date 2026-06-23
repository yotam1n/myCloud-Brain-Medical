package com.cloudbrain.repository;

import com.cloudbrain.entity.core.NotificationRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRecordJpaRepository extends JpaRepository<NotificationRecordEntity, Long> {
}
