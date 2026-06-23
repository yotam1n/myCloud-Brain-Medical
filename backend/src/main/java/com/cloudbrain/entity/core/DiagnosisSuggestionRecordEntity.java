package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagnosis_suggestion_record")
public class DiagnosisSuggestionRecordEntity extends BaseAuditableEntity {

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "consultation_note_id")
    private Long consultationNoteId;

    @Column(name = "suggested_diagnoses", columnDefinition = "TEXT")
    private String suggestedDiagnoses;

    @Column(name = "suggested_exam_items", columnDefinition = "TEXT")
    private String suggestedExamItems;

    @Column(name = "adoption_status", length = 32)
    private String adoptionStatus;

    @Column(name = "final_diagnosis_direction", length = 255)
    private String finalDiagnosisDirection;

    @Column(name = "adoption_doctor_id")
    private Long adoptionDoctorId;

    @Column(name = "adoption_time")
    private LocalDateTime adoptionTime;

    @Column(name = "ai_call_record_id")
    private Long aiCallRecordId;

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

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

    public Long getConsultationNoteId() {
        return consultationNoteId;
    }

    public void setConsultationNoteId(Long consultationNoteId) {
        this.consultationNoteId = consultationNoteId;
    }

    public String getSuggestedDiagnoses() {
        return suggestedDiagnoses;
    }

    public void setSuggestedDiagnoses(String suggestedDiagnoses) {
        this.suggestedDiagnoses = suggestedDiagnoses;
    }

    public String getSuggestedExamItems() {
        return suggestedExamItems;
    }

    public void setSuggestedExamItems(String suggestedExamItems) {
        this.suggestedExamItems = suggestedExamItems;
    }

    public String getAdoptionStatus() {
        return adoptionStatus;
    }

    public void setAdoptionStatus(String adoptionStatus) {
        this.adoptionStatus = adoptionStatus;
    }

    public String getFinalDiagnosisDirection() {
        return finalDiagnosisDirection;
    }

    public void setFinalDiagnosisDirection(String finalDiagnosisDirection) {
        this.finalDiagnosisDirection = finalDiagnosisDirection;
    }

    public Long getAdoptionDoctorId() {
        return adoptionDoctorId;
    }

    public void setAdoptionDoctorId(Long adoptionDoctorId) {
        this.adoptionDoctorId = adoptionDoctorId;
    }

    public LocalDateTime getAdoptionTime() {
        return adoptionTime;
    }

    public void setAdoptionTime(LocalDateTime adoptionTime) {
        this.adoptionTime = adoptionTime;
    }

    public Long getAiCallRecordId() {
        return aiCallRecordId;
    }

    public void setAiCallRecordId(Long aiCallRecordId) {
        this.aiCallRecordId = aiCallRecordId;
    }
}
