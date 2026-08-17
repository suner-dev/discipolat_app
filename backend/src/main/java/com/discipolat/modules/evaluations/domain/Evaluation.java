package com.discipolat.modules.evaluations.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "evalue_id", nullable = false)
    private UUID evalueId;

    /** NOT exposed in responses — ensures anonymity */
    @Column(name = "evaluateur_id", nullable = false)
    private UUID evaluateurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false)
    private CategorieEvaluation categorie;

    @Column(name = "note", nullable = false)
    private int note;

    @Column(name = "commentaire")
    private String commentaire;

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
}
