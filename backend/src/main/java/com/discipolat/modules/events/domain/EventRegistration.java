package com.discipolat.modules.events.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "event_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    @Column(name = "statut_inscription", nullable = false)
    private String statutInscription = "INSCRIT";

    @Column(name = "date_inscription", nullable = false)
    private LocalDateTime dateInscription;

    @Column(name = "date_emargement")
    private LocalDateTime dateEmargement;

    /** Position mesuree lors du pointage (preuve,see V201). */
    @Column(name = "checkin_latitude", precision = 9)
    private Double checkinLatitude;

    @Column(name = "checkin_longitude", precision = 9)
    private Double checkinLongitude;

    /** Precision GPS annoncee par l'appareil. */
    @Column(name = "checkin_accuracy_m", precision = 7)
    private Double checkinAccuracyMeters;

    /** Distance calculee entre le participant et le lieu. */
    @Column(name = "checkin_distance_m", precision = 9)
    private Double checkinDistanceMeters;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.dateInscription == null) this.dateInscription = LocalDateTime.now();
    }
}
