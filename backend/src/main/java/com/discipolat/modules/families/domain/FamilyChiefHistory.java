package com.discipolat.modules.families.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_chief_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyChiefHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "famille_id", nullable = false)
    private UUID familleId;

    @Column(name = "ancien_chef_id")
    private UUID ancienChefId;

    @Column(name = "nouveau_chef_id", nullable = false)
    private UUID nouveauChefId;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "raison")
    private String raison;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
