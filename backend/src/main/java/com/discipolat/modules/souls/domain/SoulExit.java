package com.discipolat.modules.souls.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "soul_exits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoulExit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ame_id", nullable = false)
    private UUID ameId;

    @Column(name = "faiseur_id", nullable = false)
    private UUID faiseurId;

    @Column(name = "motif", nullable = false)
    private String motif;

    @Column(name = "motif_detail")
    private String motifDetail;

    @Column(name = "peut_reintegrer", nullable = false)
    private boolean peutReintegrer;

    @Column(name = "date_sortie", nullable = false)
    private LocalDate dateSortie;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
