package com.discipolat.modules.appointments.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Appointment {

    public enum Statut { EN_ATTENTE, CONFIRME, REFUSE, ANNULE, TERMINE }

    public enum Motif { CONSEIL, CONFESSION, SUIVI, FORMATION, AUTRE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "demandeur_id", nullable = false)
    private UUID demandeurId;

    @Column(name = "recepteur_id", nullable = false)
    private UUID recepteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif", nullable = false)
    private Motif motif;

    @Column(name = "objet")
    private String objet;

    @Column(name = "date_prevue", nullable = false)
    private LocalDateTime datePrevue;

    @Column(name = "duree_minutes", nullable = false)
    private int dureeMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private Statut statut = Statut.EN_ATTENTE;

    @Column(name = "reponse")
    private String reponse;

    @Column(name = "traite_par")
    private UUID traitePar;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @Column(name = "rappel_envoye", nullable = false)
    private boolean rappelEnvoye = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
}
