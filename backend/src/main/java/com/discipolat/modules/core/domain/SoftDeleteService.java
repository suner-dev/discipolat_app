package com.discipolat.modules.core.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.BusinessHistory;
import com.discipolat.modules.audit.repository.BusinessHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class SoftDeleteService {

    private final EntityManager entityManager;
    private final BusinessHistoryRepository businessHistoryRepository;
    private final SecurityUtils securityUtils;

    /**
     * Soft delete an entity by ID.
     * Logs to soft_delete_audit and creates business_history entry.
     * Only super admins can perform physical deletes.
     *
     * @param entityType the entity type (e.g., "souls", "users", "families")
     * @param entityId the UUID of the entity to delete
     * @param reason optional reason for deletion
     * @return true if deleted, false if not found
     */
    public boolean softDelete(String entityType, UUID entityId, String reason) {
        // Check if user is super admin for physical delete
        boolean isSuperAdmin = securityUtils.hasActiveRole("PLATFORM_SUPER_ADMIN", "TENANT_OWNER", "TENANT_ADMIN");
        
        // Get current tenant and user
        UUID tenantId = TenantContext.getCurrentTenantId();
        UUID userId = securityUtils.getCurrentUserId();
        
        // Validate entity exists and belongs to tenant
        String checkSql = "SELECT 1 FROM " + entityType + " WHERE id = :id AND tenant_id = :tenantId AND deleted = false";
        Query checkQuery = entityManager.createNativeQuery(checkSql);
        checkQuery.setParameter("id", entityId);
        checkQuery.setParameter("tenantId", tenantId);
        
        Object exists = checkQuery.getSingleResult();
        if (exists == null) {
            return false; // Entity not found or already deleted
        }
        
        // Get previous values for audit
        String selectSql = "SELECT * FROM " + entityType + " WHERE id = :id";
        Query selectQuery = entityManager.createNativeQuery(selectSql);
        selectQuery.setParameter("id", entityId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> previousValues = (Map<String, Object>) selectQuery.getSingleResult();
        
        // Perform soft delete
        String deleteSql = "UPDATE " + entityType + " SET deleted = true, deleted_at = :deletedAt, deleted_by = :deletedBy WHERE id = :id";
        Query deleteQuery = entityManager.createNativeQuery(deleteSql);
        deleteQuery.setParameter("deletedAt", Instant.now());
        deleteQuery.setParameter("deletedBy", userId);
        deleteQuery.setParameter("id", entityId);
        int updated = deleteQuery.executeUpdate();
        
        if (updated == 0) {
            return false;
        }
        
        // Log to soft_delete_audit table
        logSoftDeleteAudit(entityType, entityId, userId, reason, previousValues);
        
        // Create business_history entry
        createBusinessHistory(entityType, entityId, userId, "SOFT_DELETED", reason, previousValues);
        
        return true;
    }
    
    /**
     * Physical delete - only allowed for super admins.
     * This permanently removes the entity from the database.
     */
    public boolean physicalDelete(String entityType, UUID entityId, String reason) {
        // Check if user is super admin
        boolean isSuperAdmin = securityUtils.hasActiveRole("PLATFORM_SUPER_ADMIN", "TENANT_OWNER", "TENANT_ADMIN");
        
        if (!isSuperAdmin) {
            throw new SecurityException("Physical delete is restricted to super admins only");
        }
        
        UUID tenantId = TenantContext.getCurrentTenantId();
        UUID userId = securityUtils.getCurrentUserId();
        
        // Verify entity exists and belongs to tenant
        String checkSql = "SELECT 1 FROM " + entityType + " WHERE id = :id AND tenant_id = :tenantId";
        Query checkQuery = entityManager.createNativeQuery(checkSql);
        checkQuery.setParameter("id", entityId);
        checkQuery.setParameter("tenantId", tenantId);
        
        Object exists = checkQuery.getSingleResult();
        if (exists == null) {
            return false;
        }
        
        // Get previous values for audit
        String selectSql = "SELECT * FROM " + entityType + " WHERE id = :id";
        Query selectQuery = entityManager.createNativeQuery(selectSql);
        selectQuery.setParameter("id", entityId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> previousValues = (Map<String, Object>) selectQuery.getSingleResult();
        
        // Log before physical delete
        logSoftDeleteAudit(entityType, entityId, userId, "PHYSICAL_DELETE: " + reason, previousValues);
        createBusinessHistory(entityType, entityId, userId, "PHYSICALLY_DELETED", reason, previousValues);
        
        // Perform physical delete
        String deleteSql = "DELETE FROM " + entityType + " WHERE id = :id";
        Query deleteQuery = entityManager.createNativeQuery(deleteSql);
        deleteQuery.setParameter("id", entityId);
        int deleted = deleteQuery.executeUpdate();
        
        return deleted > 0;
    }
    
    /**
     * Restore a soft-deleted entity.
     */
    public boolean restore(String entityType, UUID entityId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        UUID userId = securityUtils.getCurrentUserId();
        
        String checkSql = "SELECT 1 FROM " + entityType + " WHERE id = :id AND tenant_id = :tenantId AND deleted = true";
        Query checkQuery = entityManager.createNativeQuery(checkSql);
        checkQuery.setParameter("id", entityId);
        checkQuery.setParameter("tenantId", tenantId);
        
        Object exists = checkQuery.getSingleResult();
        if (exists == null) {
            return false;
        }
        
        String restoreSql = "UPDATE " + entityType + " SET deleted = false, deleted_at = NULL, deleted_by = NULL WHERE id = :id";
        Query restoreQuery = entityManager.createNativeQuery(restoreSql);
        restoreQuery.setParameter("id", entityId);
        int updated = restoreQuery.executeUpdate();
        
        if (updated > 0) {
            // Log restoration
            createBusinessHistory(entityType, entityId, userId, "RESTORED", "Entity restored from soft delete", null);
        }
        
        return updated > 0;
    }
    
    /**
     * Get all soft-deleted entities of a type for a tenant (admin view).
     */
    public List<Map<String, Object>> getSoftDeleted(String entityType, UUID tenantId, int limit, int offset) {
        String sql = "SELECT * FROM " + entityType + " WHERE tenant_id = :tenantId AND deleted = true ORDER BY deleted_at DESC LIMIT :limit OFFSET :offset";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("tenantId", tenantId);
        query.setParameter("limit", limit);
        query.setParameter("offset", offset);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = query.getResultList();
        return results;
    }
    
    private void logSoftDeleteAudit(String entityType, UUID entityId, UUID deletedBy, String reason, Map<String, Object> previousValues) {
        try {
            UUID tenantId = TenantContext.getCurrentTenantId();
            
            String sql = """
                INSERT INTO soft_delete_audit (tenant_id, entity_type, entity_id, deleted_by, deleted_at, reason, previous_values_json, ip, user_agent)
                VALUES (:tenantId, :entityType, :entityId, :deletedBy, :deletedAt, :reason, :previousValues, :ip, :userAgent)
                """;
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("tenantId", tenantId);
            query.setParameter("entityType", entityType);
            query.setParameter("entityId", entityId);
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            var request = attributes != null ? attributes.getRequest() : null;
            query.setParameter("ip", request != null ? request.getRemoteAddr() : null);
            query.setParameter("userAgent", request != null ? request.getHeader("User-Agent") : null);
            query.setParameter("deletedBy", deletedBy);
            query.setParameter("deletedAt", Instant.now());
            query.setParameter("reason", reason);
            query.setParameter("previousValues", previousValues != null ? previousValues.toString() : null);
            
            query.executeUpdate();
        } catch (Exception e) {
            // Log but don't fail the delete
            System.err.println("Failed to log soft delete audit: " + e.getMessage());
        }
    }
    
    private void createBusinessHistory(String entityType, UUID entityId, UUID actorId, String eventType, String summary, Map<String, Object> detail) {
        try {
            UUID tenantId = TenantContext.getCurrentTenantId();
            
            BusinessHistory history = BusinessHistory.builder()
                    .tenantId(tenantId)
                    .objectType(entityType)
                    .objectId(entityId)
                    .eventType(eventType)
                    .summary(summary)
                    .detailJson(detail)
                    .actorId(actorId)
                    .actorRole(securityUtils.getCurrentUserRole() != null ? securityUtils.getCurrentUserRole() : "UNKNOWN")
                    .happenedAt(OffsetDateTime.now())
                    .build();
            
            businessHistoryRepository.save(history);
        } catch (Exception e) {
            System.err.println("Failed to create business history: " + e.getMessage());
        }
    }
}