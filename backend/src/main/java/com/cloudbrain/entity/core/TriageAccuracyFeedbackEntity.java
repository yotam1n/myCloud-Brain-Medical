package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "triage_accuracy_feedback")
public class TriageAccuracyFeedbackEntity extends BaseAuditableEntity {

    @Column(name = "feedback_id", nullable = false)
    private Long feedbackId;

    @Column(name = "recommended_dept_snapshot", length = 128)
    private String recommendedDeptSnapshot;

    @Column(name = "actual_dept_snapshot", length = 128)
    private String actualDeptSnapshot;

    @Column(name = "accuracy_label", length = 32)
    private String accuracyLabel;

    @Column(name = "reason_tags", columnDefinition = "TEXT")
    private String reasonTags;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getRecommendedDeptSnapshot() {
        return recommendedDeptSnapshot;
    }

    public void setRecommendedDeptSnapshot(String recommendedDeptSnapshot) {
        this.recommendedDeptSnapshot = recommendedDeptSnapshot;
    }

    public String getActualDeptSnapshot() {
        return actualDeptSnapshot;
    }

    public void setActualDeptSnapshot(String actualDeptSnapshot) {
        this.actualDeptSnapshot = actualDeptSnapshot;
    }

    public String getAccuracyLabel() {
        return accuracyLabel;
    }

    public void setAccuracyLabel(String accuracyLabel) {
        this.accuracyLabel = accuracyLabel;
    }

    public String getReasonTags() {
        return reasonTags;
    }

    public void setReasonTags(String reasonTags) {
        this.reasonTags = reasonTags;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
