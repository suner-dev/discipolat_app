package com.discipolat.modules.evangelism.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "evangelism_stage_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class EvangelismStageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "track_id", nullable = false)
    private UUID trackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "etape", nullable = false)
    private EvangelismEtape etape;

    @Column(name = "cree_par")
    private UUID creePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @PrePersist
    protected void onCreate() {
        this.creeLe = LocalDateTime.now();
    }
}
