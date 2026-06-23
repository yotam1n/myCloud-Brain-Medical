package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "triage_record")
public class TriageRecordEntity extends BaseAuditableEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "chief_complaint", nullable = false, columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "recommended_dept", length = 128)
    private String recommendedDept;

    @Column(name = "recommended_doctors", columnDefinition = "TEXT")
    private String recommendedDoctors;

    @Column(name = "ai_response_raw", columnDefinition = "TEXT")
    private String aiResponseRaw;

    @Column(name = "call_status", nullable = false, length = 32)
    private String callStatus;

    @Column(name = "recommendation_source", length = 64)
    private String recommendationSource;

    @Column(name = "ai_call_record_id")
    private Long aiCallRecordId;

    @Column(name = "registration_id")
    private Long registrationId;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getRecommendedDept() {
        return recommendedDept;
    }

    public void setRecommendedDept(String recommendedDept) {
        this.recommendedDept = recommendedDept;
    }

    public String getRecommendedDoctors() {
        return recommendedDoctors;
    }

    public void setRecommendedDoctors(String recommendedDoctors) {
        this.recommendedDoctors = recommendedDoctors;
    }

    public String getAiResponseRaw() {
        return aiResponseRaw;
    }

    public void setAiResponseRaw(String aiResponseRaw) {
        this.aiResponseRaw = aiResponseRaw;
    }

    public String getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(String callStatus) {
        this.callStatus = callStatus;
    }

    public String getRecommendationSource() {
        return recommendationSource;
    }

    public void setRecommendationSource(String recommendationSource) {
        this.recommendationSource = recommendationSource;
    }

    public Long getAiCallRecordId() {
        return aiCallRecordId;
    }

    public void setAiCallRecordId(Long aiCallRecordId) {
        this.aiCallRecordId = aiCallRecordId;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }
}
