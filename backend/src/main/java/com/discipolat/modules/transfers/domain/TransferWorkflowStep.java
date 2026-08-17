package com.discipolat.modules.transfers.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Une étape du circuit de validation d'un workflow de transfert.
 * Chaque étape définit les rôles autorisés à valider, son ordre dans le
 * circuit et son caractère requis.
 */
@Entity
@Table(name = "transfer_workflow_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TransferWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "workflow_config_id", nullable = false)
    private UUID workflowConfigId;

    @Builder.Default
    @Column(name = "etape_ordre", nullable = false)
    private Integer etapeOrdre = 1;

    /** Rôles autorisés à valider cette étape (rôle ACTIF du validateur). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles_validateurs", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> rolesValidateurs = new ArrayList<>(List.of("PASTEUR"));

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "requis", nullable = false)
    private boolean requis = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
