package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "prescription_item")
public class PrescriptionItemEntity extends BaseAuditableEntity {

    @Column(name = "prescription_id", nullable = false)
    private Long prescriptionId;

    @Column(name = "drug_id")
    private Long drugId;

    @Column(name = "drug_name", nullable = false, length = 128)
    private String drugName;

    @Column(length = 255)
    private String specification;

    @Column(name = "dosage_form", length = 64)
    private String dosageForm;

    @Column(name = "package_unit", length = 32)
    private String packageUnit;

    @Column(length = 255)
    private String manufacturer;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "default_usage", length = 128)
    private String defaultUsage;

    @Column(precision = 12, scale = 2)
    private BigDecimal dosage;

    @Column(length = 64)
    private String frequency;

    @Column(length = 64)
    private String duration;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "usage_instruction", columnDefinition = "TEXT")
    private String usageInstruction;

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public Long getDrugId() {
        return drugId;
    }

    public void setDrugId(Long drugId) {
        this.drugId = drugId;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getPackageUnit() {
        return packageUnit;
    }

    public void setPackageUnit(String packageUnit) {
        this.packageUnit = packageUnit;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getDefaultUsage() {
        return defaultUsage;
    }

    public void setDefaultUsage(String defaultUsage) {
        this.defaultUsage = defaultUsage;
    }

    public BigDecimal getDosage() {
        return dosage;
    }

    public void setDosage(BigDecimal dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUsageInstruction() {
        return usageInstruction;
    }

    public void setUsageInstruction(String usageInstruction) {
        this.usageInstruction = usageInstruction;
    }
}
