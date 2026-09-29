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
@Table(name = "events")
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

    @Column(name = "organisateur_id", nullable = false)
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

    @Column(name = "type_evenement", nullable = false)
    private String typeEvenement;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "lieu")
    private String lieu;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "limite_places")
    private Integer limitePlaces;

    @Builder.Default
    @Column(name = "nb_inscrits", nullable = false)
    private Integer nbInscrits = 0;

    @Column(name = "statut", nullable = false)
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
     * Visible dans les listes publiques de l'API.
     *
     * <p>Wrapper et non primitif : sur une mise a jour partielle, {@code null}
     * signifie « non fourni » et ne doit donc pas écraser la valeur existante
     * (un primitif serait déballé et lèverait un NPE, ou pire,.remettrait
     * `false` par défaut). La colonne reste NOT NULL avec un defaut en base.
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean publicEvent = Boolean.FALSE;

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

    /** Identifiant du direct associe (voir V200). */
    @Column(name = "stream_id")
    private UUID streamId;

    @Column(name = "compte_rendu")
    private String compteRendu;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

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
