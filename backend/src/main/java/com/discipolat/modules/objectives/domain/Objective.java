package com.discipolat.modules.objectives.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "objectives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Objective {

    public enum Periode { MENSUEL, TRIMESTRIEL, ANNUEL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * V263 — contrat de rôle élargi : clé de rôle en texte (nom du rôle
     * socle {@code UserRole} OU clé du catalogue de rôles servis du tenant),
     * validée côté service contre l'union enum ∪ catalogue (P7a).
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ObjectiveType type;

    @Column(name = "cible", nullable = false)
    private int cible;

    @Enumerated(EnumType.STRING)
    @Column(name = "periode", nullable = false)
    @Builder.Default
    private Periode periode = Periode.MENSUEL;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "cree_par")
    private UUID creePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @PrePersist
    protected void onCreate() {
        this.creeLe = LocalDateTime.now();
    }
}
