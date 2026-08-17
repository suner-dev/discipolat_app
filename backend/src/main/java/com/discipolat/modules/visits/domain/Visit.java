package com.discipolat.modules.visits.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Visit {

    public enum StatutVisite { PLANIFIEE, REALISEE, ANNULEE, REPORTEE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "soul_id", nullable = false)
    private UUID soulId;

    @Column(name = "visiteur_id", nullable = false)
    private UUID visiteurId;

    @Column(name = "date_prevue", nullable = false)
    private LocalDate datePrevue;

    @Column(name = "date_realisee")
    private LocalDate dateRealisee;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutVisite statut = StatutVisite.PLANIFIEE;

    @Column(name = "motif")
    private String motif;

    @Column(name = "objectif")
    private String objectif;

    @Column(name = "compte_rendu")
    private String compteRendu;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "present")
    private Boolean present;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
