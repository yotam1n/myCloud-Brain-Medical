package com.cloudbrain.repository;

import com.cloudbrain.entity.core.ConsultationNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationNoteJpaRepository extends JpaRepository<ConsultationNoteEntity, Long> {
}
