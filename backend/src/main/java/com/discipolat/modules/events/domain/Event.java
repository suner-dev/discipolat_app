package com.discipolat.modules.events.domain;

import com.discipolat.common.infrastructure.persistence.LocalDateTimeToUtcConverter;
import com.discipolat.modules.scoping.domain.ResourceScope;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evenement, sur la table VIVANTE {@code event}.
 *
 * <p><b>Historique de ce mapping, et pourquoi il a change.</b> L'entite pointait
 * sur {@code events} (au pluriel). Or {@code V158} a renomme {@code events} en
 * {@code legacy_events} et cree la table de remplacement {@code event} : sur
 * toute base migree, la relation {@code events} n'existait plus et le module
 * {@code /api/v1/events} repondait 500. Constate sur PostgreSQL 16 reel, puis
 * corrige par {@code V203} qui a completee la table vivante. Le detail et les
 * arbitrages sont dans {@code docs/architecture/schema-events-drift.md}.
 *
 * <p><b>Ce qui n'a pas change, volontairement.</b> Les noms de champs Java
 * restent en francais ({@code titre}, {@code dateDebut}, {@code statut},
 * {@code familleId}…). Ce sont les NOMS DE COLONNES qui ont ete realignes sur la
 * table vivante ({@code title}, {@code start_at}, {@code status}). Un acceseur
 * n'est pas un contrat d'API : le contrat public est {@code EventResponse}, qui
 * expose deja ces noms francais, et les 12 consommateurs du domaine
 * (DashboardService, MemberService, ScheduledJobs, PageBuilderService…) n'ont
 * ainsi pas a eter recables. Changer le nom de colonne n'est pas un renommage de
 * metier ; changer le nom de champ en aurait ete un.
 *
 * <p>Le vocabulaire des valeurs, lui, a bien change et suit desormais les
 * dictionnaires {@code EVENT_TYPE} / {@code EVENT_STATUS} du produit (V42) :
 * {@code PLANIFIE}, {@code CULTE}, {@code RETRAITE}… et non plus
 * {@code DRAFT}, {@code MEETING}… Ce sont les valeurs que les clients envoient
 * et attendent depuis le debut.
 */
@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Event {

    /** Valeurs acceptees par {@code event.status} (dictionnaire EVENT_STATUS). */
    public static final String STATUT_PLANIFIE = "PLANIFIE";
    public static final String STATUT_EN_COURS = "EN_COURS";
    public static final String STATUT_TERMINE = "TERMINE";
    public static final String STATUT_ANNULE = "ANNULE";

    /** Valeur de {@code event.visibility} qui rend l'evenement public. */
    public static final String VISIBILITY_PUBLIC = "PUBLIC";
    /** Portee par defaut : visible de l'eglise, pas public. */
    public static final String VISIBILITY_CHURCH = "CHURCH";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "organizer_id", nullable = false)
    private UUID organisateurId;

    /**
     * Perimetre famille.
     *
     * <p>Colonne ajoutee par {@code V203} sur la table vivante. Elle porte
     * l'isolation par role ACTIF ({@code WorkspaceScopeService.canAccessFamily})
     * : {@code null} signifie « evenement d'eglise », visible de tous dans le
     * tenant. Le filtre de tenant ne remplace pas ce perimetre — il est
     * lui-meme porte par {@code tenant_id}.
     */
    @Column(name = "famille_id")
    private UUID familleId;

    /** Perimetre departement (espace Responsable). Ajoute par {@code V203}. */
    @Column(name = "department_id")
    private UUID departmentId;

    // G1.8 §54 — Resource scoping
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_scope", nullable = false, length = 20)
    @Builder.Default
    private ResourceScope resourceScope = ResourceScope.TENANT_GLOBAL;

    @Column(name = "organization_unit_id")
    private UUID organizationUnitId;

    /**
     * Type d'evenement, dans le vocabulaire du produit
     * (dictionnaire {@code EVENT_TYPE} de {@code V42} : {@code CULTE},
     * {@code SORTIE}, {@code RETRAITE}, {@code REUNION}…).
     */
    @Column(name = "type", nullable = false, length = 50)
    private String typeEvenement;

    @Column(name = "title", nullable = false, length = 255)
    private String titre;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "lieu", length = 255)
    private String lieu;

    /** Instant de debut. {@code start_at} est un {@code timestamptz}. */
    @Convert(converter = LocalDateTimeToUtcConverter.class)
    @Column(name = "start_at", nullable = false)
    private LocalDateTime dateDebut;

    @Convert(converter = LocalDateTimeToUtcConverter.class)
    @Column(name = "end_at")
    private LocalDateTime dateFin;

    @Column(name = "limite_places")
    private Integer limitePlaces;

    /**
     * Statut, dans le vocabulaire du produit (dictionnaire {@code EVENT_STATUS}).
     *
     * <p>La colonne vivante s'appelait {@code status} et n'acceptait que
     * {@code DRAFT}/{@code PUBLISHED}/{@code COMPLETED}/{@code CANCELLED}/
     * {@code ARCHIVED} : un vocabulaire que ni l'entite, ni les controleurs, ni
     * les clients ne parlaient. {@code V203} y a depose celui du produit, en
     * reprenant la decision deja prise par {@code V62} pour la table morte.
     */
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String statut = STATUT_PLANIFIE;

    /** Image de couverture (URL d'un fichier televersé). */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Étiquettes libres, indexées (GIN en base). Vide = aucune.
     *
     * <p>{@code @JdbcTypeCode(SqlTypes.ARRAY)} est indispensable : sans lui,
     * Hibernate génère {@code text[*][]}, que ni PostgreSQL ni H2 n'acceptent —
     * et la table n'était alors plus créée du tout (constaté :
     * 9 tests de vérification de schéma en échec).
     *
     * <p>Pas de {@code columnDefinition} : {@code text[]} est une syntaxe
     * PostgreSQL que H2 refuse. La colonne de PRODUCTION est
     * {@code TEXT[]} (V200 puis V202) ; ici on laisse Hibernate déduire un type
     * tableau portable, et c'est le serveur qui fait foi.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags")
    @Builder.Default
    private String[] tags = new String[0];

    /**
     * Visibilite. C'est elle qui fait autorite sur la visibilite d'un
     * evenement ; contrainte a {@code ('PRIVATE','TEAM','CHURCH','PUBLIC')}.
     *
     * <p>Il n'y a pas de colonne {@code is_public} : ce serait une seconde
     * verite sur la meme question. {@link #isPublicEvent()} en est une lecture,
     * et {@link #setPublicEvent(Boolean)} l'ecriture — avec la regle qu'un
     * {@code false} ne redescend jamais sous {@code CHURCH}, pour ne pas
     * elargir par accident un evenement {@code PRIVATE} ou {@code TEAM}.
     */
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private String visibility = VISIBILITY_CHURCH;

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
    @Column(name = "latitude")
    private Double latitude;

    /** Longitude du lieu (degres, WGS84). */
    @Column(name = "longitude")
    private Double longitude;

    /** Rayon d'effet du perimetre, en metres (10 a 5000, contrainte en base). */
    @Builder.Default
    @Column(name = "geofence_radius_m", nullable = false)
    private int geofenceRadiusMeters = 200;

    /** Identifiant du direct associe (voir V200). */
    @Column(name = "stream_id")
    private UUID streamId;

    @Column(name = "compte_rendu", columnDefinition = "text")
    private String compteRendu;

    @Convert(converter = LocalDateTimeToUtcConverter.class)
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Convert(converter = LocalDateTimeToUtcConverter.class)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Suppression logique horodatee. La table vivante n'a pas de booleen
     * {@code deleted} : V158 l'a remplacee par {@code deleted_at}, ce qui est
     * mieux (on sait quand), mais impose de reecrire les predicats
     * {@code ...AndDeletedFalse} en {@code ...AndDeletedAtIsNull}.
     */
    @Convert(converter = LocalDateTimeToUtcConverter.class)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Le compteur d'inscrits, qui n'est pas une colonne.
     *
     * <p>Il se calcule sur {@code event_registrations} (dont la cle etrangere
     * pointe vers {@code event}). Une colonne {@code nb_inscrits} serait un
     * second compteur, desynchronise de la seule source — c'est exactement le
     * mensonge de contrat que ce chantier supprime.
     */
    @Transient
    private Integer nbInscrits = 0;

    /**
     * Drapeau d'ECRITURE de la visibilite, jamais lu.
     *
     * <p>Il ne sert qu'a transporter le `isPublic` d'une requete jusqu'a
     * {@link #setPublicEvent(Boolean)}, qui sait le traduire sans elargir par
     * accident un evenement {@code PRIVATE} ou {@code TEAM}. Il est
     * deliberement nomme « flag » : la seule lecture de la visibilite est
     * {@link #isPublicEvent()}, qui derive de {@link #visibility}. Confondre
     * les deux, ce serait lire un patch comme un etat.
     */
    @Transient
    private Boolean publicEventFlag;

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

    /**
     * Renseigne le compteur derive. Appele par le service avant de serialiser ;
     * jamais persiste, donc jamais ecrit en base.
     */
    public Event withNbInscrits(int nbInscrits) {
        this.nbInscrits = nbInscrits;
        return this;
    }

    /** Suppression logique : horodate au lieu de poser un booleen. */
    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Lecture de {@link #visibility} : c'est elle qui fait autorite. */
    public boolean isPublicEvent() {
        return VISIBILITY_PUBLIC.equals(visibility);
    }

    /**
     * Ecriture de la visibilite a partir du drapeau expose par l'API.
     *
     * <p>{@code true} rend l'evenement public. {@code false} ne fait
     * <em>que</em> ramener {@code PUBLIC} vers {@code CHURCH} : redescendre
     * aussi {@code PRIVATE} ou {@code TEAM} elargirait l'evenement, alors que
     * l'appelant demandait seulement « ne le rends pas public ».
     *
     * <p>{@code null} ne touche a rien — c'est la semantique de patch du
     * {@code UpdateEventRequest}.
     */
    public void setPublicEvent(Boolean isPublic) {
        if (Boolean.TRUE.equals(isPublic)) {
            this.visibility = VISIBILITY_PUBLIC;
        } else if (isPublic != null && VISIBILITY_PUBLIC.equals(this.visibility)) {
            this.visibility = VISIBILITY_CHURCH;
        }
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
