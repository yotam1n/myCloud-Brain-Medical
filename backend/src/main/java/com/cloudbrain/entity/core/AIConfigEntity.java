package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_config")
public class AIConfigEntity extends BaseAuditableEntity {

    @Column(nullable = false, length = 64)
    private String provider;

    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "api_url", length = 255)
    private String apiUrl;

    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(name = "key_version", length = 64)
    private String keyVersion;

    @Column(name = "task_scope", nullable = false, length = 64)
    private String taskScope;

    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds;

    @Column(name = "is_default", nullable = false)
    private Boolean defaultConfig;

    @Column(name = "health_status", length = 32)
    private String healthStatus;

    @Column(name = "config_version", nullable = false, length = 64)
    private String configVersion;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false, length = 32)
    private String status;

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

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public void setApiKeyEncrypted(String apiKeyEncrypted) {
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public String getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(String keyVersion) {
        this.keyVersion = keyVersion;
    }

    public String getTaskScope() {
        return taskScope;
    }

    public void setTaskScope(String taskScope) {
        this.taskScope = taskScope;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Boolean getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(Boolean defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
