package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "prescription_review")
public class PrescriptionReviewEntity extends BaseAuditableEntity {

    @Column(name = "prescription_id")
    private Long prescriptionId;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "risk_level", length = 32)
    private String riskLevel;

    @Column(name = "local_rule_hits", columnDefinition = "TEXT")
    private String localRuleHits;

    @Column(name = "rule_engine_status", length = 32)
    private String ruleEngineStatus;

    @Column(name = "context_missing_items", columnDefinition = "TEXT")
    private String contextMissingItems;

    @Column(name = "llm_suggestion", columnDefinition = "TEXT")
    private String llmSuggestion;

    @Column(name = "llm_summary", columnDefinition = "TEXT")
    private String llmSummary;

    @Column(name = "llm_call_status", length = 32)
    private String llmCallStatus;

    @Column(name = "ai_call_record_id")
    private Long aiCallRecordId;

    @Column(name = "prescription_snapshot_hash", length = 128)
    private String prescriptionSnapshotHash;

    @Column(name = "review_context_hash", length = 128)
    private String reviewContextHash;

    @Column(name = "manual_confirmation", columnDefinition = "TEXT")
    private String manualConfirmation;

    @Column(name = "bind_status", length = 32)
    private String bindStatus;

    @Version
    @Column(nullable = false)
    private Integer version;

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getLocalRuleHits() {
        return localRuleHits;
    }

    public void setLocalRuleHits(String localRuleHits) {
        this.localRuleHits = localRuleHits;
    }

    public String getRuleEngineStatus() {
        return ruleEngineStatus;
    }

    public void setRuleEngineStatus(String ruleEngineStatus) {
        this.ruleEngineStatus = ruleEngineStatus;
    }

    public String getContextMissingItems() {
        return contextMissingItems;
    }

    public void setContextMissingItems(String contextMissingItems) {
        this.contextMissingItems = contextMissingItems;
    }

    public String getLlmSuggestion() {
        return llmSuggestion;
    }

    public void setLlmSuggestion(String llmSuggestion) {
        this.llmSuggestion = llmSuggestion;
    }

    public String getLlmSummary() {
        return llmSummary;
    }

    public void setLlmSummary(String llmSummary) {
        this.llmSummary = llmSummary;
    }

    public String getLlmCallStatus() {
        return llmCallStatus;
    }

    public void setLlmCallStatus(String llmCallStatus) {
        this.llmCallStatus = llmCallStatus;
    }

    public Long getAiCallRecordId() {
        return aiCallRecordId;
    }

    public void setAiCallRecordId(Long aiCallRecordId) {
        this.aiCallRecordId = aiCallRecordId;
    }

    public String getPrescriptionSnapshotHash() {
        return prescriptionSnapshotHash;
    }

    public void setPrescriptionSnapshotHash(String prescriptionSnapshotHash) {
        this.prescriptionSnapshotHash = prescriptionSnapshotHash;
    }

    public String getReviewContextHash() {
        return reviewContextHash;
    }

    public void setReviewContextHash(String reviewContextHash) {
        this.reviewContextHash = reviewContextHash;
    }

    public String getManualConfirmation() {
        return manualConfirmation;
    }

    public void setManualConfirmation(String manualConfirmation) {
        this.manualConfirmation = manualConfirmation;
    }

    public String getBindStatus() {
        return bindStatus;
    }

    public void setBindStatus(String bindStatus) {
        this.bindStatus = bindStatus;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
