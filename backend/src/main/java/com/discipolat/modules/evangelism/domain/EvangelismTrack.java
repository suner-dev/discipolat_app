package com.discipolat.modules.evangelism.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "evangelism_track")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class EvangelismTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "soul_id", nullable = false, unique = true)
    private UUID soulId;

    @Enumerated(EnumType.STRING)
    @Column(name = "etape", nullable = false)
    private EvangelismEtape etape;

    @Column(name = "date_etape", nullable = false)
    private LocalDate dateEtape;

    @Column(name = "note")
    private String note;

    @Column(name = "cree_par")
    private UUID creePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @Column(name = "maj_le", nullable = false)
    private LocalDateTime majLe;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.dateEtape == null) this.dateEtape = LocalDate.now();
        this.creeLe = now;
        this.majLe = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.majLe = LocalDateTime.now();
    }
}
