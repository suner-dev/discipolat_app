package com.discipolat.modules.inventory.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryItemRepository repository;
    @Mock private AuditService auditService;
    @Mock private SecurityUtils securityUtils;

    private InventoryService service;
    private UUID tenantId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        service = new InventoryService(repository, auditService, securityUtils);
        tenantId = UUID.randomUUID();
        itemId = UUID.randomUUID();
    }

    // ==================== CREATE ====================

    @Test
    void create_CallsSaveAndAudits() {
        InventoryItem input = buildItem("Microphone", "TECHNIQUE");
        UUID savedId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(inv -> {
            InventoryItem saved = inv.getArgument(0);
            saved.setId(savedId);
            return saved;
        });

        InventoryItem result = service.create(input);

        assertNotNull(result.getId());
        assertEquals(savedId, result.getId());
        verify(repository).save(any());
        verify(auditService).logSimple("INVENTORY_CREATED", "INVENTORY_ITEM", savedId);
    }

    @Test
    void create_SetsQuantiteDisponibleWhenNull() {
        InventoryItem input = buildItem("Microphone", "TECHNIQUE");
        input.setQuantite(5);
        input.setQuantiteDisponible(null);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem result = service.create(input);

        assertEquals(5, result.getQuantiteDisponible());
    }

    @Test
    void create_KeepsExistingQuantiteDisponible() {
        InventoryItem input = buildItem("Microphone", "TECHNIQUE");
        input.setQuantite(10);
        input.setQuantiteDisponible(3);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem result = service.create(input);

        assertEquals(3, result.getQuantiteDisponible());
    }

    // ==================== FIND BY ID ====================

    @Test
    void findById_ExistingItem_ReturnsItem() {
        InventoryItem item = buildItem("Micro", "TECHNIQUE");
        item.setId(itemId);
        when(repository.findById(itemId)).thenReturn(Optional.of(item));

        InventoryItem result = service.findById(itemId);

        assertEquals("Micro", result.getNom());
    }

    @Test
    void findById_NonExistingItem_ThrowsException() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findById(itemId));
    }

    // ==================== FIND ALL ====================

    @Test
    void findAll_WithoutFilters_DelegatesToFindByTenant() {
        Page<InventoryItem> page = new PageImpl<>(List.of());
        when(repository.findByTenantIdOrderByCreatedAtDesc(eq(tenantId), any(Pageable.class))).thenReturn(page);

        Page<InventoryItem> result = service.findAll(tenantId, null, null, null, Pageable.unpaged());

        assertNotNull(result);
        verify(repository).findByTenantIdOrderByCreatedAtDesc(eq(tenantId), any(Pageable.class));
    }

    @Test
    void findAll_WithSearch_DelegatesToSearch() {
        Page<InventoryItem> page = new PageImpl<>(List.of());
        when(repository.search(eq(tenantId), eq("mic"), any(Pageable.class))).thenReturn(page);

        service.findAll(tenantId, null, null, "mic", Pageable.unpaged());

        verify(repository).search(eq(tenantId), eq("mic"), any(Pageable.class));
    }

    @Test
    void findAll_WithCategorie_DelegatesToFindByCategorie() {
        Page<InventoryItem> page = new PageImpl<>(List.of());
        when(repository.findByTenantIdAndCategorie(eq(tenantId), eq("TECHNIQUE"), any(Pageable.class))).thenReturn(page);

        service.findAll(tenantId, "TECHNIQUE", null, null, Pageable.unpaged());

        verify(repository).findByTenantIdAndCategorie(eq(tenantId), eq("TECHNIQUE"), any(Pageable.class));
    }

    @Test
    void findAll_WithStatut_DelegatesToFindByStatut() {
        Page<InventoryItem> page = new PageImpl<>(List.of());
        when(repository.findByTenantIdAndStatut(eq(tenantId), eq("AFFECTE"), any(Pageable.class))).thenReturn(page);

        service.findAll(tenantId, null, "AFFECTE", null, Pageable.unpaged());

        verify(repository).findByTenantIdAndStatut(eq(tenantId), eq("AFFECTE"), any(Pageable.class));
    }

    @Test
    void findAll_SearchTakesPrecedenceOverFilters() {
        Page<InventoryItem> page = new PageImpl<>(List.of());
        when(repository.search(eq(tenantId), eq("test"), any(Pageable.class))).thenReturn(page);

        service.findAll(tenantId, "TECHNIQUE", "AFFECTE", "test", Pageable.unpaged());

        verify(repository).search(eq(tenantId), eq("test"), any(Pageable.class));
        verify(repository, never()).findByTenantIdAndCategorie(any(), any(), any());
    }

    // ==================== UPDATE ====================

    @Test
    void update_ExistingItem_UpdatesAllFields() {
        InventoryItem existing = buildItem("Old Name", "MATERIEL");
        existing.setId(itemId);
        when(repository.findById(itemId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem update = buildItem("New Name", "TECHNIQUE");
        update.setDescription("New description");
        update.setStatut("AFFECTE");
        update.setQuantite(10);
        update.setQuantiteDisponible(7);
        update.setValeurUnitaire(250.0);
        update.setLieuStockage("Salle B");
        update.setNumeroSerie("SN-123");

        InventoryItem result = service.update(itemId, update);

        assertEquals("New Name", result.getNom());
        assertEquals("TECHNIQUE", result.getCategorie());
        assertEquals("New description", result.getDescription());
        assertEquals("AFFECTE", result.getStatut());
        assertEquals(10, result.getQuantite());
        assertEquals(7, result.getQuantiteDisponible());
        assertEquals(250.0, result.getValeurUnitaire());
        assertEquals("Salle B", result.getLieuStockage());
        assertEquals("SN-123", result.getNumeroSerie());
        verify(auditService).logSimple("INVENTORY_UPDATED", "INVENTORY_ITEM", itemId);
    }

    @Test
    void update_NonExistingItem_ThrowsException() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        InventoryItem update = buildItem("X", "X");

        assertThrows(EntityNotFoundException.class, () -> service.update(itemId, update));
    }

    // ==================== DELETE ====================

    @Test
    void delete_ExistingItem_DeletesAndAudits() {
        doNothing().when(repository).deleteById(itemId);

        service.delete(itemId);

        verify(repository).deleteById(itemId);
        verify(auditService).logSimple("INVENTORY_DELETED", "INVENTORY_ITEM", itemId);
    }

    // ==================== ASSIGN ====================

    @Test
    void assign_AvailableItem_SetsAffecteAndDecrementsQuantity() {
        InventoryItem item = buildItem("Micro", "TECHNIQUE");
        item.setId(itemId);
        item.setStatut("DISPONIBLE");
        item.setQuantiteDisponible(3);
        when(repository.findById(itemId)).thenReturn(Optional.of(item));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID memberId = UUID.randomUUID();
        InventoryItem result = service.assign(itemId, memberId);

        assertEquals("AFFECTE", result.getStatut());
        assertEquals(memberId, result.getAffecteAId());
        assertEquals(2, result.getQuantiteDisponible());
        verify(auditService).logSimple("INVENTORY_ASSIGNED", "INVENTORY_ITEM", itemId);
    }

    @Test
    void assign_ZeroAvailable_ThrowsIllegalState() {
        InventoryItem item = buildItem("Micro", "TECHNIQUE");
        item.setId(itemId);
        item.setQuantiteDisponible(0);
        when(repository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(IllegalStateException.class, () -> service.assign(itemId, UUID.randomUUID()));
    }

    @Test
    void assign_NullAvailable_ThrowsIllegalState() {
        InventoryItem item = buildItem("Micro", "TECHNIQUE");
        item.setId(itemId);
        item.setQuantiteDisponible(null);
        when(repository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(IllegalStateException.class, () -> service.assign(itemId, UUID.randomUUID()));
    }

    // ==================== UNASSIGN ====================

    @Test
    void unassign_AssignedItem_SetsDisponibleAndIncrementsQuantity() {
        InventoryItem item = buildItem("Micro", "TECHNIQUE");
        item.setId(itemId);
        item.setStatut("AFFECTE");
        item.setAffecteAId(UUID.randomUUID());
        item.setQuantiteDisponible(2);
        when(repository.findById(itemId)).thenReturn(Optional.of(item));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem result = service.unassign(itemId);

        assertEquals("DISPONIBLE", result.getStatut());
        assertNull(result.getAffecteAId());
        assertEquals(3, result.getQuantiteDisponible());
        verify(auditService).logSimple("INVENTORY_UNASSIGNED", "INVENTORY_ITEM", itemId);
    }

    @Test
    void unassign_NullQuantity_HandlesGracefully() {
        InventoryItem item = buildItem("Micro", "TECHNIQUE");
        item.setId(itemId);
        item.setStatut("AFFECTE");
        item.setQuantiteDisponible(null);
        when(repository.findById(itemId)).thenReturn(Optional.of(item));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem result = service.unassign(itemId);

        assertEquals(1, result.getQuantiteDisponible());
    }

    // ==================== MARK MAINTENANCE ====================

    @Test
    void markMaintenance_SetsStatusAndDate() {
        InventoryItem item = buildItem("Micro", "TECHNIQUE");
        item.setId(itemId);
        item.setStatut("DISPONIBLE");
        when(repository.findById(itemId)).thenReturn(Optional.of(item));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryItem result = service.markMaintenance(itemId);

        assertEquals("EN_MAINTENANCE", result.getStatut());
        assertNotNull(result.getDerniereMaintenance());
        verify(auditService).logSimple("INVENTORY_MAINTENANCE", "INVENTORY_ITEM", itemId);
    }

    // ==================== GET STATS ====================

    @Test
    void getStats_ReturnsAllCounters() {
        when(repository.countByTenantId(tenantId)).thenReturn(50L);
        when(repository.countByTenantIdAndStatut(tenantId, "DISPONIBLE")).thenReturn(20L);
        when(repository.countByTenantIdAndStatut(tenantId, "AFFECTE")).thenReturn(15L);
        when(repository.countByTenantIdAndStatut(tenantId, "EN_MAINTENANCE")).thenReturn(5L);
        when(repository.countByTenantIdAndStatut(tenantId, "RETIRE")).thenReturn(10L);

        Map<String, Object> stats = service.getStats(tenantId);

        assertEquals(50L, stats.get("total"));
        assertEquals(20L, stats.get("disponibles"));
        assertEquals(15L, stats.get("affectes"));
        assertEquals(5L, stats.get("enMaintenance"));
        assertEquals(10L, stats.get("retires"));
    }

    @Test
    void getStats_ZeroTenant_ReturnsZeros() {
        when(repository.countByTenantId(any())).thenReturn(0L);
        when(repository.countByTenantIdAndStatut(any(), any())).thenReturn(0L);

        Map<String, Object> stats = service.getStats(UUID.randomUUID());

        assertEquals(0L, stats.get("total"));
    }

    // ==================== HELPERS ====================

    private InventoryItem buildItem(String nom, String categorie) {
        InventoryItem item = new InventoryItem();
        item.setTenantId(tenantId);
        item.setNom(nom);
        item.setCategorie(categorie);
        item.setStatut("DISPONIBLE");
        item.setQuantite(1);
        item.setQuantiteDisponible(1);
        return item;
    }
}
