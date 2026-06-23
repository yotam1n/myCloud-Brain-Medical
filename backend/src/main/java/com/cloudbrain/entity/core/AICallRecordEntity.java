package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_call_record")
public class AICallRecordEntity extends BaseAuditableEntity {

    @Column(name = "task_type", nullable = false, length = 64)
    private String taskType;

    @Column(name = "business_record_id")
    private Long businessRecordId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_role", length = 32)
    private String operatorRole;

    @Column(length = 64)
    private String provider;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(name = "config_version", length = 64)
    private String configVersion;

    @Column(name = "prompt_version", length = 64)
    private String promptVersion;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    @Column(name = "call_status", nullable = false, length = 32)
    private String callStatus;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(nullable = false)
    private Boolean degraded;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Long getBusinessRecordId() {
        return businessRecordId;
    }

    public void setBusinessRecordId(Long businessRecordId) {
        this.businessRecordId = businessRecordId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public void setOperatorRole(String operatorRole) {
        this.operatorRole = operatorRole;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public void setOutputSummary(String outputSummary) {
        this.outputSummary = outputSummary;
    }

    public String getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(String callStatus) {
        this.callStatus = callStatus;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Boolean getDegraded() {
        return degraded;
    }

    public void setDegraded(Boolean degraded) {
        this.degraded = degraded;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }
}
