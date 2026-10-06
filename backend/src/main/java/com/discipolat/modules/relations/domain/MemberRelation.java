package com.discipolat.modules.relations.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

/**
 * Hiérarchie & relations personnelles (« Mon encadrement ») — V231.
 *
 * <p>Arête dirigée : {@code fromUserId} (le membre) déclare une autorité
 * {@code toUserId} (pasteur, supérieur, responsable, mentor, parrain…)
 * dont le type est paramétrable par l'église (dictionnaire
 * {@code MEMBER_RELATION_TYPE}). Déclaration = ACTIVE immédiate +
 * notification chez {@code toUserId} ; fin = {@link RelationStatus#REVOKED}
 * (jamais de purge physique — traçabilité, même discipline que
 * {@code MemberRoleAssignment}).
 *
 * <p>Unicité « un seul ACTIVE par (from, to, type) » : règle métier,
 * vérifiée au service (dialecte-indépendante H2/PostgreSQL — pas
 * d'index partiel).
 */
@Entity
@Table(name = "member_relations",
        indexes = {
                @Index(name = "idx_member_relations_from", columnList = "from_user_id, statut"),
                @Index(name = "idx_member_relations_to", columnList = "to_user_id, statut"),
                @Index(name = "idx_member_relations_tenant", columnList = "tenant_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Pas de @FilterDef ici : le filtre `tenantFilter` est DÉFINI une seule fois
// dans l'application, sur l'entité User. Redéclarer le même @FilterDef lève
// `Multiple '@FilterDef' annotations define a filter named 'tenantFilter'`
// au démarrage de l'EntityManagerFactory (cf. TenantFilterDefArchitectureTest
// qui verrouille cette règle). Ici on ne référence que le filtre.
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MemberRelation {

    public enum RelationStatus { ACTIVE, REVOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Le membre qui déclare son encadrant. */
    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    /** L'autorité déclarée (utilisateur enregistré du même tenant). */
    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    /** Code du dictionnaire {@code MEMBER_RELATION_TYPE} (PASTEUR, SUPERIEUR…). */
    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private RelationStatus statut = RelationStatus.ACTIVE;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Auteur de la déclaration (normalement {@code fromUserId} ; ADMIN/PASTEUR possible). */
    @Column(name = "declared_by")
    private UUID declaredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.statut == null) this.statut = RelationStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberRelation that = (MemberRelation) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
