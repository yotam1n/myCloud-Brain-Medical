package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration")
public class RegistrationEntity extends BaseAuditableEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "triage_record_id")
    private Long triageRecordId;

    @Column(name = "registration_time", nullable = false)
    private LocalDateTime registrationTime;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "department_snapshot", length = 128)
    private String departmentSnapshot;

    @Column(name = "doctor_snapshot", length = 255)
    private String doctorSnapshot;

    @Column(name = "visit_level_snapshot", length = 64)
    private String visitLevelSnapshot;

    @Column(name = "consultation_start_time")
    private LocalDateTime consultationStartTime;

    @Column(name = "record_confirmed_time")
    private LocalDateTime recordConfirmedTime;

    @Column(name = "prescription_submitted_time")
    private LocalDateTime prescriptionSubmittedTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    @Column(name = "cancelled_time")
    private LocalDateTime cancelledTime;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "slot_released", nullable = false)
    private Boolean slotReleased;

    @Version
    @Column(nullable = false)
    private Integer version;

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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Long getTriageRecordId() {
        return triageRecordId;
    }

    public void setTriageRecordId(Long triageRecordId) {
        this.triageRecordId = triageRecordId;
    }

    public LocalDateTime getRegistrationTime() {
        return registrationTime;
    }

    public void setRegistrationTime(LocalDateTime registrationTime) {
        this.registrationTime = registrationTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDepartmentSnapshot() {
        return departmentSnapshot;
    }

    public void setDepartmentSnapshot(String departmentSnapshot) {
        this.departmentSnapshot = departmentSnapshot;
    }

    public String getDoctorSnapshot() {
        return doctorSnapshot;
    }

    public void setDoctorSnapshot(String doctorSnapshot) {
        this.doctorSnapshot = doctorSnapshot;
    }

    public String getVisitLevelSnapshot() {
        return visitLevelSnapshot;
    }

    public void setVisitLevelSnapshot(String visitLevelSnapshot) {
        this.visitLevelSnapshot = visitLevelSnapshot;
    }

    public LocalDateTime getConsultationStartTime() {
        return consultationStartTime;
    }

    public void setConsultationStartTime(LocalDateTime consultationStartTime) {
        this.consultationStartTime = consultationStartTime;
    }

    public LocalDateTime getRecordConfirmedTime() {
        return recordConfirmedTime;
    }

    public void setRecordConfirmedTime(LocalDateTime recordConfirmedTime) {
        this.recordConfirmedTime = recordConfirmedTime;
    }

    public LocalDateTime getPrescriptionSubmittedTime() {
        return prescriptionSubmittedTime;
    }

    public void setPrescriptionSubmittedTime(LocalDateTime prescriptionSubmittedTime) {
        this.prescriptionSubmittedTime = prescriptionSubmittedTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public LocalDateTime getCancelledTime() {
        return cancelledTime;
    }

    public void setCancelledTime(LocalDateTime cancelledTime) {
        this.cancelledTime = cancelledTime;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Boolean getSlotReleased() {
        return slotReleased;
    }

    public void setSlotReleased(Boolean slotReleased) {
        this.slotReleased = slotReleased;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
