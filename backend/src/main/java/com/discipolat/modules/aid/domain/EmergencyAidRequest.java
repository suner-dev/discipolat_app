package com.discipolat.modules.aid.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Demande de secours / urgence pastorale avec plan de réponse automatisé. */
@Entity
@Table(name = "emergency_aid_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class EmergencyAidRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "soul_id")
    private UUID soulId;

    @Column(name = "family_id")
    private UUID familyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    @Builder.Default
    private Urgence urgency = Urgence.HAUTE;

    /** URGENCE_PASTORALE, MEDICAL, NOURRITURE, LOGEMENT, DEUIL… */
    @Column(name = "category", nullable = false)
    @Builder.Default
    private String category = "URGENCE_PASTORALE";

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Plan de secours généré (JSON structuré). */
    @Column(name = "plan_json", columnDefinition = "TEXT")
    private String planJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Statut statut = Statut.OUVERT;

    @Column(name = "amount_collected", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amountCollected = BigDecimal.ZERO;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Urgence { CRITIQUE, HAUTE, MOYENNE }

    public enum Statut { OUVERT, EN_COURS, RESOLU }
}
