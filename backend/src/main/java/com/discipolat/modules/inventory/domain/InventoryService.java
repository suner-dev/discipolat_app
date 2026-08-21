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
}
