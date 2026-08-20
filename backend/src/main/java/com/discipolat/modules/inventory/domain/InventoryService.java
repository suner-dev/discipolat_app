package com.discipolat.modules.inventory.domain;

import com.discipolat.common.domain.EntityNotFoundException;
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
    private final SecurityUtils securityUtils;

    @Transactional
    public InventoryItem create(InventoryItem item) {
        item.setId(null);
        if (item.getQuantiteDisponible() == null) {
            item.setQuantiteDisponible(item.getQuantite());
        }
        InventoryItem saved = repository.save(item);
        auditService.logSimple("INVENTORY_CREATED", "INVENTORY_ITEM", saved.getId());
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
        auditService.logSimple("INVENTORY_UPDATED", "INVENTORY_ITEM", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
        auditService.logSimple("INVENTORY_DELETED", "INVENTORY_ITEM", id);
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
        auditService.logSimple("INVENTORY_ASSIGNED", "INVENTORY_ITEM", saved.getId());
        return saved;
    }

    @Transactional
    public InventoryItem unassign(UUID id) {
        InventoryItem item = findById(id);
        item.setAffecteAId(null);
        item.setStatut("DISPONIBLE");
        item.setQuantiteDisponible(
                (item.getQuantiteDisponible() != null ? item.getQuantiteDisponible() : 0) + 1
        );
        InventoryItem saved = repository.save(item);
        auditService.logSimple("INVENTORY_UNASSIGNED", "INVENTORY_ITEM", saved.getId());
        return saved;
    }

    @Transactional
    public InventoryItem markMaintenance(UUID id) {
        InventoryItem item = findById(id);
        item.setStatut("EN_MAINTENANCE");
        item.setDerniereMaintenance(LocalDateTime.now());
        InventoryItem saved = repository.save(item);
        auditService.logSimple("INVENTORY_MAINTENANCE", "INVENTORY_ITEM", saved.getId());
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
}
