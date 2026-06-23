package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_record")
public class NotificationRecordEntity extends BaseAuditableEntity {

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "recipient_role", nullable = false, length = 32)
    private String recipientRole;

    @Column(name = "alert_type", nullable = false, length = 64)
    private String alertType;

    @Column(name = "statistics_bucket", length = 64)
    private String statisticsBucket;

    @Column(name = "display_level", length = 32)
    private String displayLevel;

    @Column(name = "business_record_id")
    private Long businessRecordId;

    @Column(name = "patient_summary", columnDefinition = "TEXT")
    private String patientSummary;

    @Column(name = "risk_summary", columnDefinition = "TEXT")
    private String riskSummary;

    @Column(name = "is_read", nullable = false)
    private Boolean read;

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientRole() {
        return recipientRole;
    }

    public void setRecipientRole(String recipientRole) {
        this.recipientRole = recipientRole;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getStatisticsBucket() {
        return statisticsBucket;
    }

    public void setStatisticsBucket(String statisticsBucket) {
        this.statisticsBucket = statisticsBucket;
    }

    public String getDisplayLevel() {
        return displayLevel;
    }

    public void setDisplayLevel(String displayLevel) {
        this.displayLevel = displayLevel;
    }

    public Long getBusinessRecordId() {
        return businessRecordId;
    }

    public void setBusinessRecordId(Long businessRecordId) {
        this.businessRecordId = businessRecordId;
    }

    public String getPatientSummary() {
        return patientSummary;
    }

    public void setPatientSummary(String patientSummary) {
        this.patientSummary = patientSummary;
    }

    public String getRiskSummary() {
        return riskSummary;
    }

    public void setRiskSummary(String riskSummary) {
        this.riskSummary = riskSummary;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }
}
