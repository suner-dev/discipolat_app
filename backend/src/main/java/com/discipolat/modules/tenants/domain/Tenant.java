package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status;

    // ============================================================
    // Modèle d'organisation — SPEC_ORGANISATION_DENOMINATION_V2 §5 / V222
    // ============================================================
    // Un tenant n'est plus « une église » : c'est une entité dont la
    // nature est explicite (D2) et qui peut se rattacher à une autre
    // organisation (D1). Ces trois colonnes sont ADDITIVES — aucune
    // migration V≤221 n'est touchée (D14).

    /**
     * Nature de l'organisation (D2).
     *
     * <p>Valeur {@code CHURCH} par défaut : une organisation isolée reste
     * le cas le plus fréquent, et le modèle doit continuer à fonctionner
     * sans action de l'utilisateur.
     *
     * <p><b>Le {@code columnDefinition} porte le DEFAULT, et ce n'est pas un
     * détail.</b> Il aligne le schéma produit par {@code ddl-auto} (profil de
     * test, où Flyway est désactivé et où le schéma vient des annotations) sur
     * la migration V222, qui déclare {@code DEFAULT 'CHURCH'}. Sans lui,
     * l'entité imposait une colonne {@code NOT NULL} sans valeur par défaut
     * alors que la base de production en fournit une : tout INSERT ne
     * mentionnant pas {@code kind} — les fixtures de test, et tout script
     * d'import ou d'administration futur — échouait en
     * {@code NULL not allowed for column "kind"}. La divergence était invisible
     * parce qu'aucune exécution locale ne joue les migrations : le schéma de
     * test et le schéma de production étaient simplement deux bases
     * différentes. {@code @Builder.Default} ne protège que le builder Lombok,
     * pas l'INSERT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30,
            columnDefinition = "varchar(30) default 'CHURCH'")
    @Builder.Default
    private TenantKind kind = TenantKind.CHURCH;

    /**
     * Rattachement direct dans l'arborescence (§1.3, mode AUTONOME).
     *
     * <p>{@code null} pour une organisation racine. Le noyau de la
     * contrainte est en base ({@code ON DELETE RESTRICT}) : une dénomination
     * qui a des enfants ne peut pas être supprimée.
     */
    @Column(name = "parent_tenant_id")
    private UUID parentTenantId;

    /**
     * Racine du réseau, c'est-à-dire la dénomination de rattachement (D3).
     *
     * <p><b>Détecteur du transfert de membre</b> (§4.4) : deux organisations
     * dont la racine <b>résolue</b> coïncide appartiennent au même réseau ; un
     * membre qui passe de l'une à l'autre est <b>transféré</b> et non
     * réinscrit.
     *
     * <p><b>Colonne volontairement NULLABLE en base</b> (V222) : elle doit
     * pouvoir valoir l'identifiant de la ligne elle-même, ce qu'aucun
     * {@code DEFAULT} ni trigger ne peut exprimer de façon fiable (le trigger
     * précède l'affectation de l'UUID). L'invariant « toute organisation a une
     * racine » est donc porté par {@link #effectiveRootTenantId()}, qui
     * normalise, et par les services qui écrivent — pas par une contrainte.
     * {@code null} signifie « je suis ma propre racine », ce qui est exact
     * pour une église isolée.
     *
     * <p>Ne jamais lire ce champ directement dans une règle métier : passer
     * par {@link #effectiveRootTenantId()}, sinon une racine isolée créée
     * hors provisionnement passerait pour un « parent inconnu ».
     */
    @Column(name = "root_tenant_id")
    private UUID rootTenantId;

    @Column(name = "plan", nullable = false)
    private String plan;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "locale", length = 10)
    private String locale;

    // Correctif 2026-09-24 : `columnDefinition = "jsonb"` ne suffit pas avec
    // Hibernate 6 — sans type JDBC explicite, l'insert envoyait un VARCHAR et
    // PostgreSQL rejetait la requête (« column "branding_json" is of type jsonb
    // but expression is of type character varying »), ce qui rendait la création
    // d'un tenant impossible en production. @JdbcTypeCode(SqlTypes.JSON) oblige
    // Hibernate à binder réellement du JSON.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "branding_json", columnDefinition = "jsonb")
    private String brandingJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features_json", columnDefinition = "jsonb")
    private String featuresJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings_json", columnDefinition = "jsonb")
    private String settingsJson;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    /**
     * Fin de l'onboarding du tenant (migration V183, décision D2).
     * Colonnes ADDITIVES : porter l'achèvement ici plutôt que dans un nouvel
     * état {@code ONBOARDING} de {@link TenantStatus} évite de casser l'enum,
     * les seeds, les tableaux de bord et une dizaine de tests existants.
     * {@code null} = onboarding non terminé.
     */
    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    /** Acteur ayant terminé l'onboarding du tenant. */
    @Column(name = "onboarding_completed_by")
    private UUID onboardingCompletedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.plan == null) this.plan = "free";
        if (this.status == null) this.status = TenantStatus.PENDING_SETUP;
        if (this.country == null) this.country = "CM";
        if (this.currency == null) this.currency = "XAF";
        if (this.timezone == null) this.timezone = "Africa/Douala";
        if (this.locale == null) this.locale = "fr";
        if (this.kind == null) this.kind = TenantKind.CHURCH;
    }

    /**
     * Une organisation est sa propre racine tant qu'aucune hiérarchie n'est
     * établie (SPEC_ORGANISATION_DENOMINATION_V2 §7.1 / T-B1).
     *
     * <p><b>Best-effort, et c'est délibéré.</b> La colonne
     * {@code root_tenant_id} est nullable en base (V222) précisément parce
     * qu'aucun mécanisme déclaratif ne peut y écrire l'identifiant de la
     * ligne : le déclencheur SQL s'exécute avant que l'UUID ne soit connu, et
     * l'ordre d'affectation de l'identifiant par Hibernate n'est pas un
     * contrat sur lequel on peut miser une contrainte d'intégrité.
     *
     * <p>Cette méthode renseigne donc le champ <i>si</i> l'identifiant est
     * déjà connu, mais ne prétend pas être l'unique filet : la vérité est
     * {@link #effectiveRootTenantId()}, qui est totale. Si ce setter s'avère
     * inutile un jour, la suppression ne changera aucun comportement — c'est
     * la conception qui le garantit, pas ce commentaire.
     */
    public void ensureRootTenantId() {
        if (this.rootTenantId == null && this.id != null) {
            this.rootTenantId = this.id;
        }
    }

    /** Vrai si l'organisation est la racine d'un réseau (pas d'elle-même enfant). */
    public boolean isNetworkRoot() {
        return parentTenantId == null || parentTenantId.equals(id);
    }

    /**
     * Rattachement effectif : la racine du réseau dont fait partie cette
     * organisation. Utilisé par la console plateforme pour éviter de
     * remonter l'arborescence complète.
     */
    public UUID effectiveRootTenantId() {
        return rootTenantId != null ? rootTenantId : id;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tenant tenant = (Tenant) o;
        return id != null && id.equals(tenant.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Explicit getters/setters for Lombok compatibility
}
