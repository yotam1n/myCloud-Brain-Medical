package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "feedback")
public class FeedbackEntity extends BaseAuditableEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "registration_id", nullable = false, unique = true)
    private Long registrationId;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "triage_accurate")
    private Boolean triageAccurate;

    @Column(columnDefinition = "TEXT")
    private String comment;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Boolean getTriageAccurate() {
        return triageAccurate;
    }

    public void setTriageAccurate(Boolean triageAccurate) {
        this.triageAccurate = triageAccurate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
