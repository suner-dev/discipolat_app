package com.discipolat.modules.automations.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "automation_executions")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class AutomationExecution {

    public enum Statut { SUCCÈS, ÉCHEC, EN_ATTENTE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private AutomationRule rule;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(columnDefinition = "TEXT")
    private String contexte; // JSON: données déclencheur

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE;

    @Column(columnDefinition = "TEXT")
    private String résultat;

    @Column(nullable = false)
    private LocalDateTime exécutéLe = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public AutomationRule getRule() { return rule; }
    public void setRule(AutomationRule rule) { this.rule = rule; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getContexte() { return contexte; }
    public void setContexte(String contexte) { this.contexte = contexte; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public String getRésultat() { return résultat; }
    public void setRésultat(String résultat) { this.résultat = résultat; }
    public LocalDateTime getExécutéLe() { return exécutéLe; }
    public void setExécutéLe(LocalDateTime exécutéLe) { this.exécutéLe = exécutéLe; }
}
