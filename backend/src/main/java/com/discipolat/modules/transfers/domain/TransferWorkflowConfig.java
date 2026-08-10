package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.TransferType;
import com.discipolat.common.enums.ValidationMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Paramétrage du workflow de transfert pour un type donné.
 * Entièrement configurable par le pasteur depuis l'administration :
 * rôles initiateurs, mode de validation, nombre de validations requises,
 * délais de traitement, notifications automatiques, modèles de messages
 * et règles d'exécution — le tout SANS modification de code.
 */
@Entity
@Table(name = "transfer_workflow_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferWorkflowConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, unique = true)
    private TransferType transferType;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /** Rôles autorisés à initier une demande de ce type. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles_initiateurs", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<String> rolesInitiateurs = new ArrayList<>(List.of("PASTEUR"));

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_validation", nullable = false)
    @Builder.Default
    private ValidationMode modeValidation = ValidationMode.SEQUENTIEL;

    @Builder.Default
    @Column(name = "nombre_validations_requises", nullable = false)
    private Integer nombreValidationsRequises = 1;

    @Builder.Default
    @Column(name = "delai_traitement_heures", nullable = false)
    private Integer delaiTraitementHeures = 72;

    @Builder.Default
    @Column(name = "notifications_auto", nullable = false)
    private boolean notificationsAuto = true;

    @Column(name = "modele_message_demande")
    private String modeleMessageDemande;

    @Column(name = "modele_message_validation")
    private String modeleMessageValidation;

    @Column(name = "modele_message_refus")
    private String modeleMessageRefus;

    @Column(name = "modele_message_execution")
    private String modeleMessageExecution;

    /** Règles d'exécution (ex : transfererAmes, notifierX). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "regles_execution", columnDefinition = "jsonb")
    private Map<String, Object> reglesExecution;

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
