package com.discipolat.modules.communications.domain;

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
 * Annonce de l'église avec cible de diffusion (TOUS / rôle / famille /
 * département). La publication déclenche des notifications IN_APP vers
 * les destinataires. Suppression = archivage (soft delete).
 */
@Entity
@Table(name = "communications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Communication {

    public enum Cible { TOUS, ROLE, FAMILLE, DEPARTEMENT }

    public enum Statut { BROUILLON, PUBLIEE, ARCHIVEE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @Column(name = "contenu", nullable = false, columnDefinition = "text")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(name = "cible", nullable = false, length = 20)
    private Cible cible;

    /** Rôles destinataires quand cible = ROLE. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Column(name = "famille_id")
    private UUID familleId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private Statut statut = Statut.BROUILLON;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

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
