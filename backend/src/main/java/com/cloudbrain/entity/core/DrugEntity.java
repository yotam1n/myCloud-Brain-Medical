package com.cloudbrain.entity.core;

import com.cloudbrain.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "drug")
public class DrugEntity extends BaseAuditableEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "pinyin_code", length = 128)
    private String pinyinCode;

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

    @Column(name = "default_usage", columnDefinition = "TEXT")
    private String defaultUsage;

    @Column(columnDefinition = "TEXT")
    private String contraindications;

    @Column(columnDefinition = "TEXT")
    private String precautions;

    @Column(columnDefinition = "TEXT")
    private String indications;

    @Column(name = "interaction_summary", columnDefinition = "TEXT")
    private String interactionSummary;

    @Column(nullable = false, length = 32)
    private String status;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPinyinCode() {
        return pinyinCode;
    }

    public void setPinyinCode(String pinyinCode) {
        this.pinyinCode = pinyinCode;
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

    public String getContraindications() {
        return contraindications;
    }

    public void setContraindications(String contraindications) {
        this.contraindications = contraindications;
    }

    public String getPrecautions() {
        return precautions;
    }

    public void setPrecautions(String precautions) {
        this.precautions = precautions;
    }

    public String getIndications() {
        return indications;
    }

    public void setIndications(String indications) {
        this.indications = indications;
    }

    public String getInteractionSummary() {
        return interactionSummary;
    }

    public void setInteractionSummary(String interactionSummary) {
        this.interactionSummary = interactionSummary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
