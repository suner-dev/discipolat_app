package com.discipolat.modules.events.domain;

import com.discipolat.modules.scoping.domain.ResourceScope;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
// ARBITRAGE D1 (orchestrateur, 2026-09-30) — source unique : la table vivante
// « event ». Historique de la dérive : V158 avait renommé « events » en
// « legacy_events » sans publier de mappage — tout /api/v1/events (dont
// FIRST_EVENT du wizard) répondait 500 sur les bases migrées, le profil H2
// (ddl-auto create-drop) masquant la dérive. V194 avait rebaptisé la table
// physique en « events » ; V203 a ramené sur « event » les colonnes d'isolation
// (famille_id, department_id, resource_scope, organization_unit_id), les
// options du contrat (V200/V202) et des CHECK élargis FR ∪ EN. L'entité
// double ChurchEvent est retirée : UNE seule entité mappe « event ».
//
// Vocabulaire : les propriétés Java et les colonnes conservent les noms
// français du contrat §3 (figé, R2) — seul le mappage suit l'anglais vivant
// (titre→title, date_debut→start_at…). Les valeurs FR (statut PLANIFIE,
// type REUNIO…N) restent telles quelles : la conversion FR→EN de V158 était
// AVEC PERTE (REUNION/SORTIE/VISITE→MEETING non inversible), d'où les CHECK
// en union V203.
//
// Horodatages : colonnes TIMESTAMPTZ, propriétés LocalDateTime — convention
// V158 « naive = UTC » (date_debut AT TIME ZONE 'UTC'), identique à celle de
// l'ex-ChurchEvent (OffsetDateTime/TIMESTAMPTZ rendu par le même driver).
// L'autorité de visibilité est la colonne « visibility » (PRIVATE/TEAM/
// CHURCH/PUBLIC) : is_public du contrat n'en est que la lecture (== PUBLIC).
// nbInscrits est un compteur CALCULÉ sur event_registrations (la colonne
// nb_inscrits n'existe pas sur la table vivante) ; il est porté par le
// champ transitoire ci-dessous, renseigné par EventService à chaque lecture.
@Table(name = "event", indexes = {
    @Index(name = "idx_event_tenant", columnList = "tenant_id"),
    @Index(name = "idx_event_status", columnList = "status"),
    @Index(name = "idx_event_start", columnList = "start_at"),
    @Index(name = "idx_event_tenant_start", columnList = "tenant_id, start_at"),
    @Index(name = "idx_event_deleted", columnList = "deleted_at"),
    @Index(name = "idx_event_organizer", columnList = "organizer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // Organizer du modèle vivant (colonne « organizer_id ») ; le contrat §3
    // expose ce même organisateur sous le nom « organisateurId ».
    @Column(name = "organizer_id")
    private UUID organisateurId;

    @Column(name = "famille_id")
    private UUID familleId;

    @Column(name = "department_id")
    private UUID departmentId;

    // G1.8 §54 — Resource scoping
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_scope", nullable = false, length = 20)
    @Builder.Default
    private ResourceScope resourceScope = ResourceScope.TENANT_GLOBAL;

    @Column(name = "organization_unit_id")
    private UUID organizationUnitId;

    @Column(name = "type", nullable = false)
    private String typeEvenement;

    @Column(name = "title", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "lieu")
    private String lieu;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "end_at")
    private LocalDateTime dateFin;

    /** Fuseau du modèle vivant (absorbé de ChurchEvent, V203/D1). */
    @Column(name = "timezone", length = 64)
    private String timezone;

    /** Récurrence du modèle vivant (absorbée de ChurchEvent, V203/D1). */
    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(name = "recurrence_rule", columnDefinition = "text")
    private String recurrenceRule;

    /** Auteur technique (absorbé de ChurchEvent, V203/D1). */
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "limite_places")
    private Integer limitePlaces;

    /**
     * Compteur CALCULÉ sur event_registrations (décision V202/D1 : la colonne
     * nb_inscrits n'existe pas sur la table vivante). Transitoire : renseigné à
     * chaque lecture par EventService avant exposition du contrat §3.
     */
    @Transient
    @Builder.Default
    private Integer nbInscrits = 0;

    @Column(name = "status", nullable = false)
    private String statut = "PLANIFIE";

    /** Image de couverture (URL d'un fichier televersé). */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Étiquettes libres, indexées (GIN en base). Vide = aucune.
     *
     * <p>{@code @JdbcTypeCode(SqlTypes.ARRAY)} est indispensable : sans lui,
     * Hibernate génère {@code text[*][]}, que ni PostgreSQL ni H2 n'acceptent —
     * et la table {@code events} n'était alors plus créée du tout (constaté :
     * 9 tests de vérification de schéma en échec).
     *
     * <p>Pas de {@code columnDefinition} : {@code text[]} est une syntaxe
     * PostgreSQL que H2 refuse. La colonne de PRODUCTION est créée par la
     * migration V200 en {@code TEXT[]}; ici on laisse Hibernate déduire un type
     * tableau portable, et c'est le serveur qui fait foi.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags")
    @Builder.Default
    private String[] tags = new String[0];

    /**
     * Autorité de visibilité du modèle vivant (PRIVATE/TEAM/CHURCH/PUBLIC).
     * Defaut {@code CHURCH} — c'est exactement le {@code DEFAULT 'CHURCH'} de
     * la colonne vivante (V158) : l'evenement est interne a l'eglise tant que
     * le contrat ne demande pas la publicite (equivalent de {@code is_public =
     * false} avant V203). {@code @Builder.Default} est indispensable ici : les
     * creations completes (controller FR, generation de programme hebdo,
     * FIRST_EVENT) construisent via builder SANS nommer la visibilite ; sans
     * defaut, Hibernate insererait NULL dans une colonne NOT NULL et H2 (comme
     * PostgreSQL) rejetterait la ligne.
     *
     * <p>Le patch partiel n'est pas casse pour autant : les appelants de mise
     * a jour ({@code EventController#update}, {@code ChurchEventDto#toPatch}）
     * passent TOUJOURS {@code visibility} explicitement — {@code null} quand le
     * requeteur ne la touche pas (et {@code @Builder.Default} ne s'applique que
     * sur omission de l'appel). EventService#update applique alors
     * {@code if (updated.getVisibility() != null)} : un null reste « muet ».
     */
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private String visibility = "CHURCH";

    // ARBITRAGE D1 — « is_public »/« publicEvent » du contrat §3 n'est plus un
    // champ stocke : la colonne « visibility » en est l'unique autorite, et
    // ces accesseurs la traduisent (PUBLIC <-> le reste). Volontairement
    // depourvus de champ transitoire : le builder Lombok ecrirait directement
    // ce champ et court-circuiterait setPublicEvent, creant deux sources de
    // verite. Le contrat passe donc par visibility (le controller traduit
    // isPublic -> PUBLIC/CHURCH a la frontiere).
    public Boolean getPublicEvent() {
        return visibility == null ? null : "PUBLIC".equals(visibility);
    }

    public void setPublicEvent(Boolean isPublic) {
        if (isPublic == null) return;
        // Le contrat ne connaît que deux états. « true » promeut PUBLIC ;
        // « false » ne rétrograde que depuis PUBLIC (retour au defaut
        // d'église) et laisse intactes les granularités TEAM/PRIVATE posées
        // par le modèle vivant — comme la colonne is_public le faisait.
        if (Boolean.TRUE.equals(isPublic)) {
            this.visibility = "PUBLIC";
        } else if ("PUBLIC".equals(this.visibility)) {
            this.visibility = "CHURCH";
        }
    }

    /**
     * Le serveur refuse l'inscription tant que faux (voir EventService#register).
     *
     * <p>Defaut {@code TRUE} : c'est le comportement AVANT V200 (tout le monde
     * pouvait s'inscrire). Un defaut {@code false} casserait retroactivement
     * l'inscription a tous les evenements deja en base.
     */
    @Column(name = "requires_registration", nullable = false)
    @Builder.Default
    private Boolean requiresRegistration = Boolean.TRUE;

    /**
     * Le serveur refuse le pointage tant que faux (voir EventService#markAttendance).
     *
     * <p>Defaut {@code TRUE} pour la meme raison : l'emargement existait deja
     * pour tout evenement avant V200.
     */
    @Column(name = "has_checkin", nullable = false)
    @Builder.Default
    private Boolean checkinEnabled = Boolean.TRUE;

    /**
     * Latitude du lieu (degres, WGS84). {@code null} = pas de geolocalisation
     * configuree : le serveur refuse alors tout pointage geolocalise (fail-closed).
     */
    @Column(name = "latitude", precision = 9)
    private Double latitude;

    /** Longitude du lieu (degres, WGS84). */
    @Column(name = "longitude", precision = 9)
    private Double longitude;

    /** Rayon d'effet du perimetre, en metres (10 a 5000, contrainte en base). */
    @Builder.Default
    @Column(name = "geofence_radius_m", nullable = false)
    private int geofenceRadiusMeters = 200;

    /** Identifiant du direct associe (voir V200). */
    @Column(name = "stream_id")
    private UUID streamId;

    @Column(name = "compte_rendu")
    private String compteRendu;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Suppression logique alignée sur le modèle vivant (colonne « deleted_at »
     * de V158) : remplace le boolean « deleted » de la table héritée. Les
     * accesseurs {@code isDeleted()}/{@code setDeleted()} du contrat sont
     * conservés comme vues transitoires de {@code deletedAt} ; le champ
     * support n'est ni mappé ni persisté.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Transient
    private boolean deleted;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void setDeleted(boolean deleted) {
        this.deletedAt = deleted ? LocalDateTime.now() : null;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return id != null && id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
