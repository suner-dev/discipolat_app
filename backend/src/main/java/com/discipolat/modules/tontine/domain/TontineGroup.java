package com.discipolat.modules.tontine.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Groupe de tontine (confiance & vœux) : échéancier partagé entre membres. */
@Entity
@Table(name = "tontine_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TontineGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "montant_par_tour", nullable = false)
    @Builder.Default
    private BigDecimal montantParTour = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "periodicite", nullable = false)
    @Builder.Default
    private Periodicite periodicite = Periodicite.MENSUELLE;

    @Column(name = "tour_actuel", nullable = false)
    @Builder.Default
    private int tourActuel = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private Statut statut = Statut.ACTIVE;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToMany(mappedBy = "groupId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<TontineMember> members = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Periodicite { HEBDOMADAIRE, MENSUELLE, TRIMESTRIELLE }

    public enum Statut { ACTIVE, TERMINEE, SUSPENDUE }
}
