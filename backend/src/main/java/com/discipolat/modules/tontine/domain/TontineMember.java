package com.discipolat.modules.tontine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Membre d'une tontine avec son ordre de passage. */
@Entity
@Table(name = "tontine_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TontineMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "soul_id")
    private UUID soulId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "ordre_passage", nullable = false)
    @Builder.Default
    private int ordrePassage = 1;

    @Column(name = "a_recu_tour", nullable = false)
    @Builder.Default
    private boolean aRecuTour = false;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
