package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "medical_record")
public class MedicalRecordEntity extends BaseAuditableEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "present_illness", columnDefinition = "TEXT")
    private String presentIllness;

    @Column(name = "past_history", columnDefinition = "TEXT")
    private String pastHistory;

    @Column(name = "physical_exam", columnDefinition = "TEXT")
    private String physicalExam;

    @Column(name = "preliminary_diagnosis", columnDefinition = "TEXT")
    private String preliminaryDiagnosis;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(name = "conversation_text", columnDefinition = "TEXT")
    private String conversationText;

    @Column(name = "ai_generated", nullable = false)
    private Boolean aiGenerated;

    @Column(name = "ai_call_record_id")
    private Long aiCallRecordId;

    @Version
    @Column(nullable = false)
    private Integer version;

    @Column(name = "doc_note", columnDefinition = "TEXT")
    private String docNote;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getPresentIllness() {
        return presentIllness;
    }

    public void setPresentIllness(String presentIllness) {
        this.presentIllness = presentIllness;
    }

    public String getPastHistory() {
        return pastHistory;
    }

    public void setPastHistory(String pastHistory) {
        this.pastHistory = pastHistory;
    }

    public String getPhysicalExam() {
        return physicalExam;
    }

    public void setPhysicalExam(String physicalExam) {
        this.physicalExam = physicalExam;
    }

    public String getPreliminaryDiagnosis() {
        return preliminaryDiagnosis;
    }

    public void setPreliminaryDiagnosis(String preliminaryDiagnosis) {
        this.preliminaryDiagnosis = preliminaryDiagnosis;
    }

    public String getTreatmentPlan() {
        return treatmentPlan;
    }

    public void setTreatmentPlan(String treatmentPlan) {
        this.treatmentPlan = treatmentPlan;
    }

    public String getConversationText() {
        return conversationText;
    }

    public void setConversationText(String conversationText) {
        this.conversationText = conversationText;
    }

    public Boolean getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public Long getAiCallRecordId() {
        return aiCallRecordId;
    }

    public void setAiCallRecordId(Long aiCallRecordId) {
        this.aiCallRecordId = aiCallRecordId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDocNote() {
        return docNote;
    }

    public void setDocNote(String docNote) {
        this.docNote = docNote;
    }
}
