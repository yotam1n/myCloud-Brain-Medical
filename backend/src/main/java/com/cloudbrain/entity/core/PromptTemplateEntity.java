package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "prompt_template")
public class PromptTemplateEntity extends BaseAuditableEntity {

    @Column(name = "template_code", nullable = false, unique = true, length = 128)
    private String templateCode;

    @Column(name = "task_type", nullable = false, length = 64)
    private String taskType;

    @Column(name = "dept_code", length = 64)
    private String deptCode;

    @Column(name = "template_body", columnDefinition = "TEXT")
    private String templateBody;

    @Column(name = "variable_whitelist", columnDefinition = "TEXT")
    private String variableWhitelist;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "is_default", nullable = false)
    private Boolean defaultTemplate;

    @Column(nullable = false, length = 32)
    private String status;

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getTemplateBody() {
        return templateBody;
    }

    public void setTemplateBody(String templateBody) {
        this.templateBody = templateBody;
    }

    public String getVariableWhitelist() {
        return variableWhitelist;
    }

    public void setVariableWhitelist(String variableWhitelist) {
        this.variableWhitelist = variableWhitelist;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getDefaultTemplate() {
        return defaultTemplate;
    }

    public void setDefaultTemplate(Boolean defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
