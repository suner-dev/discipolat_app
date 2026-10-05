package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * LOT 2 §GR — affectation d'une entrée de menu à un {@link NavigationGroup}.
 *
 * <p>{@code href} est la clé de jointure <b>logique</b>, volontairement
 * dénormalisée : la même table sert à regrouper
 * <ul>
 *   <li>les entrées {@code menu_entries} (qui existent en base), et</li>
 *   <li>les ~175 entrées du menu <b>statique</b> du frontend, qui
 *       n'existent que dans {@code workspaces.ts} — aucune table ne peut y
 *       référencer une FK.</li>
 * </ul>
 * {@code itemKey} est renseigné quand l'entrée a une clé stable en base
 * ({@code menu_entries.key}) ; sinon l'identifiant interne de l'entrée de nav.
 */
@Entity
@Table(name = "navigation_group_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_navigation_group_item",
                columnNames = {"group_id", "href"}),
        indexes = {
                @Index(name = "idx_navigation_group_items_group", columnList = "group_id, display_order"),
                @Index(name = "idx_navigation_group_items_href", columnList = "href")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavigationGroupItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "item_key", length = 60)
    private String itemKey;

    @Column(name = "href", nullable = false, length = 255)
    private String href;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NavigationGroupItem that = (NavigationGroupItem) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}