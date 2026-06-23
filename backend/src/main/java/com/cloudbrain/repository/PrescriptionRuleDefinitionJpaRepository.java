package com.cloudbrain.repository;

import com.cloudbrain.entity.core.PrescriptionRuleDefinitionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRuleDefinitionJpaRepository extends JpaRepository<PrescriptionRuleDefinitionEntity, Long> {

    Optional<PrescriptionRuleDefinitionEntity> findByRuleCode(String ruleCode);

    List<PrescriptionRuleDefinitionEntity> findByStatusOrderByRuleCodeAsc(String status);

    List<PrescriptionRuleDefinitionEntity> findByRuleTypeAndStatusOrderByRuleCodeAsc(String ruleType, String status);

    List<PrescriptionRuleDefinitionEntity> findByApplicableDiseasesContainingAndStatusOrderByRuleCodeAsc(
            String disease,
            String status
    );
}
