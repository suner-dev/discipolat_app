package com.discipolat.modules.families.domain;

import com.discipolat.common.enums.NiveauRisque;
import com.discipolat.common.enums.StatutEntite;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "families")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "chef_famille_id", nullable = false)
    private UUID chefFamilleId;

    @Column(name = "chef_adjoint_id")
    private UUID chefAdjointId;

    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutEntite statut = StatutEntite.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "zone")
    private String zone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "niveau_risque", nullable = false)
    private NiveauRisque niveauRisque = NiveauRisque.NORMAL;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
