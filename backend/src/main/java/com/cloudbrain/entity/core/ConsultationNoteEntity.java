package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "consultation_note")
public class ConsultationNoteEntity extends BaseAuditableEntity {

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "conversation_text", columnDefinition = "TEXT")
    private String conversationText;

    @Column(name = "chief_complaint_summary", columnDefinition = "TEXT")
    private String chiefComplaintSummary;

    @Column(name = "diagnosis_direction", columnDefinition = "TEXT")
    private String diagnosisDirection;

    @Column(name = "patient_context", columnDefinition = "TEXT")
    private String patientContext;

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

    public String getConversationText() {
        return conversationText;
    }

    public void setConversationText(String conversationText) {
        this.conversationText = conversationText;
    }

    public String getChiefComplaintSummary() {
        return chiefComplaintSummary;
    }

    public void setChiefComplaintSummary(String chiefComplaintSummary) {
        this.chiefComplaintSummary = chiefComplaintSummary;
    }

    public String getDiagnosisDirection() {
        return diagnosisDirection;
    }

    public void setDiagnosisDirection(String diagnosisDirection) {
        this.diagnosisDirection = diagnosisDirection;
    }

    public String getPatientContext() {
        return patientContext;
    }

    public void setPatientContext(String patientContext) {
        this.patientContext = patientContext;
    }
}
