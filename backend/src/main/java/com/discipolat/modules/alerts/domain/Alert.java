package com.discipolat.modules.alerts.domain;

import com.discipolat.common.enums.StatutAlerte;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ame_id")
    private UUID ameId;

    @Column(name = "faiseur_id")
    private UUID faiseurId;

    @Column(name = "famille_id")
    private UUID familleId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "cible", nullable = false)
    @Builder.Default
    private String cible = "PERSONNE";

    @Column(name = "priorite", nullable = false)
    @Builder.Default
    private String priorite = "MOYENNE";

    @Column(name = "titre")
    private String titre;

    @Column(name = "type_alerte_manuel")
    private String typeAlerteManuel;

    @Column(name = "type_alerte", nullable = false)
    private String typeAlerte;

    @Column(name = "message")
    private String message;

    @Column(name = "date_declenchement", nullable = false)
    private LocalDateTime dateDeclenchement;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutAlerte statut = StatutAlerte.ACTIVE;

    @Column(name = "date_resolution")
    private LocalDateTime dateResolution;

    @Column(name = "resolu_par")
    private UUID resoluPar;

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
