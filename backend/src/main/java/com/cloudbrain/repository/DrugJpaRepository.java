package com.cloudbrain.repository;

import com.cloudbrain.entity.core.DrugEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrugJpaRepository extends JpaRepository<DrugEntity, Long> {

    Optional<DrugEntity> findByCode(String code);

    Optional<DrugEntity> findByIdAndStatus(Long id, String status);

    List<DrugEntity> findByNameContainingAndStatusOrderByNameAsc(String keyword, String status);

    List<DrugEntity> findByPinyinCodeContainingAndStatusOrderByPinyinCodeAsc(String keyword, String status);
}
