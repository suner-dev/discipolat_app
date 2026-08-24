package com.discipolat.modules.inventory.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationListener;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository repository;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;
    private final EntityPropagationListener propagationListener;
    private final SecurityUtils securityUtils;

    @Transactional
    public InventoryItem create(InventoryItem item) {
        item.setId(null);
        if (item.getQuantiteDisponible() == null) {
            item.setQuantiteDisponible(item.getQuantite());
        }
        InventoryItem saved = repository.save(item);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishCreated("INVENTORY_ITEM", saved.getId(),
                Map.of("nom", saved.getNom() != null ? saved.getNom() : "",
                        "categorie", saved.getCategorie() != null ? saved.getCategorie() : ""),
                "Équipement créé: " + (saved.getNom() != null ? saved.getNom() : "sans nom"));
        return saved;
    }

    @Transactional(readOnly = true)
    public InventoryItem findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("InventoryItem", id));
    }

    @Transactional(readOnly = true)
    public Page<InventoryItem> findAll(UUID tenantId, String categorie, String statut, String q, Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return repository.search(tenantId, q, pageable);
        }
        if (categorie != null && !categorie.isBlank()) {
            return repository.findByTenantIdAndCategorie(tenantId, categorie, pageable);
        }
        if (statut != null && !statut.isBlank()) {
            return repository.findByTenantIdAndStatut(tenantId, statut, pageable);
        }
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Transactional
    public InventoryItem update(UUID id, InventoryItem update) {
        InventoryItem item = findById(id);
        String oldNom = item.getNom();
        String oldStatut = item.getStatut();
        item.setNom(update.getNom());
        item.setDescription(update.getDescription());
        item.setCategorie(update.getCategorie());
        item.setStatut(update.getStatut());
        item.setQuantite(update.getQuantite());
        item.setQuantiteDisponible(update.getQuantiteDisponible());
        item.setValeurUnitaire(update.getValeurUnitaire());
        item.setLieuStockage(update.getLieuStockage());
        item.setNumeroSerie(update.getNumeroSerie());
        item.setDateAcquisition(update.getDateAcquisition());
        item.setDerniereMaintenance(update.getDerniereMaintenance());
        item.setProchaineMaintenance(update.getProchaineMaintenance());
        item.setDepartementId(update.getDepartementId());
        item.setNotes(update.getNotes());
        InventoryItem saved = repository.save(item);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishUpdated("INVENTORY_ITEM", saved.getId(),
                Map.of("nom", oldNom, "statut", oldStatut),
                Map.of("nom", saved.getNom(), "statut", saved.getStatut()),
                "Équipement mis à jour: " + saved.getNom());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        InventoryItem item = findById(id);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishDeleted("INVENTORY_ITEM", id,
                Map.of("nom", item.getNom(), "categorie", item.getCategorie() != null ? item.getCategorie() : ""),
                "Équipement supprimé: " + item.getNom());
        repository.deleteById(id);
    }

    @Transactional
    public InventoryItem assign(UUID id, UUID memberId) {
        InventoryItem item = findById(id);
        if (item.getQuantiteDisponible() == null || item.getQuantiteDisponible() <= 0) {
            throw new IllegalStateException("Aucune unité disponible pour affectation");
        }
        item.setAffecteAId(memberId);
        item.setStatut("AFFECTE");
        item.setQuantiteDisponible(item.getQuantiteDisponible() - 1);
        InventoryItem saved = repository.save(item);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishReassigned("INVENTORY_ITEM", saved.getId(), "affecteAId",
                item.getAffecteAId(), memberId,
                "Équipement affecté: " + saved.getNom());
        propagationListener.notifyInventoryAssigned(memberId, saved.getId(), saved.getNom());
        return saved;
    }

    @Transactional
    public InventoryItem unassign(UUID id) {
        InventoryItem item = findById(id);
        UUID oldAffecteAId = item.getAffecteAId();
        item.setAffecteAId(null);
        item.setStatut("DISPONIBLE");
        item.setQuantiteDisponible(
                (item.getQuantiteDisponible() != null ? item.getQuantiteDisponible() : 0) + 1
        );
        InventoryItem saved = repository.save(item);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishReassigned("INVENTORY_ITEM", saved.getId(), "affecteAId",
                oldAffecteAId, null,
                "Équipement désaffecté: " + saved.getNom());
        return saved;
    }

    @Transactional
    public InventoryItem markMaintenance(UUID id) {
        InventoryItem item = findById(id);
        String oldStatut = item.getStatut();
        item.setStatut("EN_MAINTENANCE");
        item.setDerniereMaintenance(LocalDateTime.now());
        InventoryItem saved = repository.save(item);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("INVENTORY_ITEM", saved.getId(),
                oldStatut, "EN_MAINTENANCE",
                "Équipement en maintenance: " + saved.getNom());
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(UUID tenantId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", repository.countByTenantId(tenantId));
        stats.put("disponibles", repository.countByTenantIdAndStatut(tenantId, "DISPONIBLE"));
        stats.put("affectes", repository.countByTenantIdAndStatut(tenantId, "AFFECTE"));
        stats.put("enMaintenance", repository.countByTenantIdAndStatut(tenantId, "EN_MAINTENANCE"));
        stats.put("retires", repository.countByTenantIdAndStatut(tenantId, "RETIRE"));
        return stats;
    }

    /**
     * Alertes stock intelligentes :
     *  - Stock bas (disponible <= 20% de la quantité totale)
     *  - Maintenance prochaine (dans les 30 prochains jours)
     *  - Équipements en panne/perdus
     *  - Suggestions de réapprovisionnement
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSmartAlerts(UUID tenantId) {
        List<InventoryItem> allItems = repository.findByTenantIdOrderByCreatedAtDesc(tenantId, Pageable.unpaged()).getContent();
        Map<String, Object> alerts = new LinkedHashMap<>();

        // 1. Stock bas
        List<Map<String, Object>> lowStock = allItems.stream()
                .filter(i -> i.getQuantite() != null && i.getQuantiteDisponible() != null
                        && i.getQuantite() > 0
                        && (double) i.getQuantiteDisponible() / i.getQuantite() <= 0.2)
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", i.getId());
                    m.put("nom", i.getNom());
                    m.put("categorie", i.getCategorie());
                    m.put("quantite", i.getQuantite());
                    m.put("disponible", i.getQuantiteDisponible());
                    m.put("pourcentage", Math.round((double) i.getQuantiteDisponible() / i.getQuantite() * 100));
                    m.put("suggestion", "Réapprovisionner à au moins " + Math.max(1, i.getQuantite() / 2) + " unités");
                    return m;
                })
                .toList();

        // 2. Maintenance prochaine (30 jours)
        java.time.LocalDateTime maintenant = java.time.LocalDateTime.now();
        java.time.LocalDateTime limite = maintenant.plusDays(30);
        List<Map<String, Object>> maintenanceDue = allItems.stream()
                .filter(i -> i.getProchaineMaintenance() != null
                        && i.getProchaineMaintenance().isAfter(maintenant)
                        && i.getProchaineMaintenance().isBefore(limite))
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", i.getId());
                    m.put("nom", i.getNom());
                    m.put("prochaineMaintenance", i.getProchaineMaintenance());
                    long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(maintenant, i.getProchaineMaintenance());
                    m.put("joursRestants", joursRestants);
                    m.put("urgent", joursRestants <= 7);
                    return m;
                })
                .toList();

        // 3. Maintenance en retard
        List<Map<String, Object>> maintenanceRetard = allItems.stream()
                .filter(i -> i.getProchaineMaintenance() != null
                        && i.getProchaineMaintenance().isBefore(maintenant))
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", i.getId());
                    m.put("nom", i.getNom());
                    m.put("prochaineMaintenance", i.getProchaineMaintenance());
                    m.put("joursRetard", java.time.temporal.ChronoUnit.DAYS.between(i.getProchaineMaintenance(), maintenant));
                    return m;
                })
                .toList();

        // 4. Équipements en panne/perdus
        long enPanne = allItems.stream().filter(i -> "EN_MAINTENANCE".equals(i.getStatut())).count();
        long perdus = allItems.stream().filter(i -> "PERDU".equals(i.getStatut())).count();

        // 5. Valeur totale inventaire
        double valeurTotale = allItems.stream()
                .filter(i -> i.getValeurUnitaire() != null && i.getQuantite() != null)
                .mapToDouble(i -> i.getValeurUnitaire() * i.getQuantite())
                .sum();

        alerts.put("stockBas", lowStock);
        alerts.put("stockBasCount", lowStock.size());
        alerts.put("maintenanceProchaine", maintenanceDue);
        alerts.put("maintenanceProchaineCount", maintenanceDue.size());
        alerts.put("maintenanceEnRetard", maintenanceRetard);
        alerts.put("maintenanceEnRetardCount", maintenanceRetard.size());
        alerts.put("enPanne", enPanne);
        alerts.put("perdus", perdus);
        alerts.put("valeurTotaleInventaire", Math.round(valeurTotale * 100.0) / 100.0);
        alerts.put("totalAlertes", lowStock.size() + maintenanceDue.size() + maintenanceRetard.size());

        return alerts;
    }
}
