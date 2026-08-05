package com.discipolat.modules.souls.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "spiritual_score_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpiritualScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "soul_id", nullable = false)
    private UUID soulId;

    @Column(name = "semaine", nullable = false)
    private LocalDate semaine;

    @Column(name = "score_global", nullable = false)
    private int scoreGlobal;

    @Column(name = "sante", nullable = false)
    private int sante;

    @Column(name = "fidelite", nullable = false)
    private int fidelite;

    @Column(name = "engagement", nullable = false)
    private int engagement;

    @Column(name = "participation", nullable = false)
    private int participation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
