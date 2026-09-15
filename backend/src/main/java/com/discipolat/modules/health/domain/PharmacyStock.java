package com.discipolat.modules.health.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pharmacy_stock", indexes = {
    @Index(name = "idx_pharm_stock_item", columnList = "pharmacy_item_id"),
    @Index(name = "idx_pharm_stock_lot", columnList = "lot_number"),
    @Index(name = "idx_pharm_stock_tenant", columnList = "tenant_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PharmacyStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_item_id", nullable = false)
    private PharmacyItem pharmacyItem;

    @Column(name = "lot_number", nullable = false)
    private String lotNumber;

    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    @Column(name = "seuil_alerte", nullable = false)
    private Integer seuilAlerte = 10;

    @Column(name = "date_expiration", nullable = false)
    private LocalDate dateExpiration;

    @Column(name = "prix_unitaire")
    private Double prixUnitaire;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StockStatus status = StockStatus.EN_STOCK;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum StockStatus {
        EN_STOCK, STOCK_FAIBLE, EXPIRANT, EXPIRÉ, ÉPUISÉ
    }

    @PrePersist protected void onCreate() { this.createdAt = LocalDateTime.now(); this.updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
