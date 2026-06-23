package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "prescription_rule_definition")
public class PrescriptionRuleDefinitionEntity extends BaseAuditableEntity {

    @Column(name = "rule_code", nullable = false, unique = true, length = 128)
    private String ruleCode;

    @Column(name = "rule_type", nullable = false, length = 64)
    private String ruleType;

    @Column(name = "applicable_drugs", columnDefinition = "TEXT")
    private String applicableDrugs;

    @Column(name = "applicable_diseases", length = 255)
    private String applicableDiseases;

    @Column(name = "applicable_populations", length = 64)
    private String applicablePopulations;

    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression;

    @Column(name = "risk_level", length = 32)
    private String riskLevel;

    @Column(name = "alert_message", columnDefinition = "TEXT")
    private String alertMessage;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(columnDefinition = "TEXT")
    private String basis;

    @Column(nullable = false)
    private Boolean seeded;

    @Version
    @Column(nullable = false)
    private Integer version;

    @Column(name = "validation_status", length = 32)
    private String validationStatus;

    @Column(nullable = false, length = 32)
    private String status;

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public String getApplicableDrugs() {
        return applicableDrugs;
    }

    public void setApplicableDrugs(String applicableDrugs) {
        this.applicableDrugs = applicableDrugs;
    }

    public String getApplicableDiseases() {
        return applicableDiseases;
    }

    public void setApplicableDiseases(String applicableDiseases) {
        this.applicableDiseases = applicableDiseases;
    }

    public String getApplicablePopulations() {
        return applicablePopulations;
    }

    public void setApplicablePopulations(String applicablePopulations) {
        this.applicablePopulations = applicablePopulations;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getBasis() {
        return basis;
    }

    public void setBasis(String basis) {
        this.basis = basis;
    }

    public Boolean getSeeded() {
        return seeded;
    }

    public void setSeeded(Boolean seeded) {
        this.seeded = seeded;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
