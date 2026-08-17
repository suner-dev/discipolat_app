package com.discipolat.modules.souls.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "soul_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SoulHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ame_id", nullable = false)
    private UUID ameId;

    @Column(name = "type_evenement", nullable = false)
    private String typeEvenement;

    @Column(name = "description")
    private String description;

    @Column(name = "ancien_statut")
    private String ancienStatut;

    @Column(name = "nouveau_statut")
    private String nouveauStatut;

    @Column(name = "ancien_faiseur_id")
    private UUID ancienFaiseurId;

    @Column(name = "nouveau_faiseur_id")
    private UUID nouveauFaiseurId;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
