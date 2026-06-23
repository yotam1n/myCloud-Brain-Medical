package com.cloudbrain.repository;

import com.cloudbrain.entity.core.PromptTemplateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateJpaRepository extends JpaRepository<PromptTemplateEntity, Long> {

    Optional<PromptTemplateEntity> findByTemplateCode(String templateCode);

    Optional<PromptTemplateEntity> findFirstByTaskTypeAndDeptCodeAndDefaultTemplateTrueAndStatusOrderByVersionDesc(
            String taskType,
            String deptCode,
            String status
    );

    Optional<PromptTemplateEntity> findFirstByTaskTypeAndDeptCodeIsNullAndDefaultTemplateTrueAndStatusOrderByVersionDesc(
            String taskType,
            String status
    );

    List<PromptTemplateEntity> findByTaskTypeAndStatusOrderByVersionDesc(String taskType, String status);
}
