package com.discipolat.modules.badges.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    public enum Niveau { BRONZE, ARGENT, OR, DIAMANT }

    public enum Critere { VISITES, PRESENCE, EVANGELISATION, INTERACTIONS, FIDELITE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "description")
    private String description;

    @Column(name = "icone")
    private String icone;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false)
    @Builder.Default
    private Niveau niveau = Niveau.BRONZE;

    @Enumerated(EnumType.STRING)
    @Column(name = "critere", nullable = false)
    private Critere critere;

    @Column(name = "seuil", nullable = false)
    private int seuil;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
