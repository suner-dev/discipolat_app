package com.discipolat.modules.forms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "form_responses")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class FormResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID formTemplateId;

    private UUID submittedBy;

    /** JSON answers array */
    @Column(columnDefinition = "TEXT")
    private String answersJson = "[]";

    private LocalDateTime submittedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getFormTemplateId() { return formTemplateId; }
    public void setFormTemplateId(UUID formTemplateId) { this.formTemplateId = formTemplateId; }
    public UUID getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(UUID submittedBy) { this.submittedBy = submittedBy; }
    public String getAnswersJson() { return answersJson; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
