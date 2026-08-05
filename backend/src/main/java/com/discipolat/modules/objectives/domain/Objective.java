package com.discipolat.modules.objectives.domain;

import com.discipolat.common.domain.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "objectives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Objective {

    public enum Periode { MENSUEL, TRIMESTRIEL, ANNUEL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ObjectiveType type;

    @Column(name = "cible", nullable = false)
    private int cible;

    @Enumerated(EnumType.STRING)
    @Column(name = "periode", nullable = false)
    private Periode periode = Periode.MENSUEL;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "cree_par")
    private UUID creePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @PrePersist
    protected void onCreate() {
        this.creeLe = LocalDateTime.now();
    }
}
