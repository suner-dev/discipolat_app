package com.discipolat.modules.platform.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Entrée d'un dictionnaire de la plateforme (référentiel configurable).
 * Chaque dictionnaire (types d'événements, statuts d'âmes, raisons
 * d'absence…) est un ensemble d'entrées : code stable, libellé éditable,
 * couleur, ordre d'affichage et état actif/inactif. L'administrateur
 * adapte ces listes à l'église sans modifier le code.
 */
@Entity
@Table(name = "dictionary_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Clé du dictionnaire (ex : EVENT_TYPE, SOUL_STATUS, ABSENCE_RAISON). */
    @Column(name = "dict_key", nullable = false, length = 50)
    private String dictKey;

    /** Code stable de l'entrée (valeur stockée en base par les entités). */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /** Libellé affiché à l'utilisateur (éditable par l'admin). */
    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "description")
    private String description;

    /** Couleur associée (badges, pastilles) au format hexadécimal. */
    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "ordre", nullable = false)
    @Builder.Default
    private int ordre = 0;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    /** Entrée fournie par défaut (réinitialisable) vs créée par l'admin. */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

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
