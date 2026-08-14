package com.discipolat.modules.departments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Paramétrage du département : seuils configurables des alertes
 * intelligentes (absence répétée, inactivité, tâches en retard).
 * <p>
 * Une seule ligne par département, créée avec les valeurs par défaut
 * au premier accès (jamais de valeurs hardcodées dans les règles).
 */
@Entity
@Table(name = "department_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentSetting {

    @Id
    @Column(name = "department_id")
    private UUID departmentId;

    /** Nombre d'absences sur la période déclenchant l'alerte ABSENCE_REPETEE. */
    @Column(name = "absence_seuil", nullable = false)
    @Builder.Default
    private int absenceSeuil = 2;

    /** Nombre de semaines de présence considérées pour l'alerte absence. */
    @Column(name = "absence_periode", nullable = false)
    @Builder.Default
    private int absencePeriode = 3;

    /** Nombre de mois sans fiche de présence déclenchant l'alerte INACTIVITE (0 = désactivé). */
    @Column(name = "inactivite_mois", nullable = false)
    @Builder.Default
    private int inactiviteMois = 3;

    /** Active/désactive l'alerte TACHE_EN_RETARD. */
    @Column(name = "tache_retard_alerte", nullable = false)
    @Builder.Default
    private boolean tacheRetardAlerte = true;

    /** Jours avant l'événement pour le rappel au responsable (0 = désactivé). */
    @Column(name = "event_rappel_jours", nullable = false)
    @Builder.Default
    private int eventRappelJours = 1;

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
