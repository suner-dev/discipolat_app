package com.discipolat.modules.events.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
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
public class ChurchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "type", length = 50)
    private String type;

    /**
     * Statut, dans le vocabulaire du produit.
     *
     * <p>Le defaut etait {@code DRAFT}, vocabulaire de la table telle que
     * {@code V158} l'avait creee. {@code V203} y a depose le vocabulaire du
     * produit (dictionnaire {@code EVENT_STATUS} : {@code PLANIFIE},
     * {@code EN_COURS}, {@code TERMINE}, {@code ANNULE}), decision deja prise par
     * {@code V62} pour la table morte. Conserver {@code DRAFT} ici ferait
     * echouer l'insertion de {@code POST /api/v1/church-events} sur la
     * contrainte — le defaut doit suivre la contrainte, pas l'inverse.
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PLANIFIE";

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "timezone", length = 64)
    private String timezone;

    /**
     * Récurrence — colonne {@code NOT NULL DEFAULT FALSE} depuis V158.
     *
     * <p>Le {@code columnDefinition} n'est pas décoratif : il porte le
     * {@code DEFAULT false} que PostgreSQL a bien (V158) mais que
     * {@code @Column(nullable = false)} n'exprime pas. Sans lui, le DDL
     * reconstruit par Hibernate en test créait la colonne {@code NOT NULL} sans
     * défaut — et toute écriture de l'autre entité de cette table,
     * {@code Event}, qui ne renseigne pas cette colonne, se heurtait à
     * {@code NULL not allowed for column "is_recurring"} (500 sur
     * {@code POST /api/v1/events}). La déclaration est donc alignée sur le
     * schéma réel, dans les deux sens : ce que PostgreSQL fait et ce que
     * l'annotation affirme.
     *
     * <p>C'est le prix d'avoir deux entités sur une table. Le corriger
     * proprement — n'en garder qu'une — est le chantier de refonte du module,
     * distinct de cet alignement.
     */
    @Column(name = "is_recurring", nullable = false, columnDefinition = "boolean default false")
    private Boolean isRecurring = false;

    @Column(name = "recurrence_rule", columnDefinition = "text")
    private String recurrenceRule;

    @Column(name = "visibility", nullable = false, length = 30)
    private String visibility = "CHURCH";

    @Column(name = "organizer_id")
    private UUID organizerId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }
}