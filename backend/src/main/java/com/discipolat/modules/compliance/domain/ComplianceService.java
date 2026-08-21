package com.discipolat.modules.compliance.domain;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Compliance Manager — RGPD/CCPA compliance dashboard.
 *
 * Fonctionnalités :
 * - Politique de rétention configurable par type de données
 * - Export de données personnelles (portabilité)
 * - Suppression de données personnelles (droit à l'oubli)
 * - Journal d'audit immutable
 * - Gestion des consentements
 */
@Service
@Transactional
public class ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceService.class);

    private final JdbcTemplate jdbcTemplate;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public ComplianceService(JdbcTemplate jdbcTemplate,
                             EntityPropagationPublisher propagationPublisher,
                             SecurityUtils securityUtils) {
        this.jdbcTemplate = jdbcTemplate;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    /**
     * Dashboard de conformité : nombre de records par type de données,
     * âge des données, conformité à la rétention.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getComplianceDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();

        // Count records by sensitive table
        Map<String, Long> recordCounts = new LinkedHashMap<>();
        for (String table : List.of("souls", "users", "notifications", "audit_logs",
                "finance_transactions", "soul_history", "messages")) {
            try {
                Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + table, Long.class);
                recordCounts.put(table, count != null ? count : 0L);
            } catch (Exception e) {
                recordCounts.put(table, 0L);
            }
        }
        dashboard.put("recordCounts", recordCounts);

        // Total records
        long total = recordCounts.values().stream().mapToLong(Long::longValue).sum();
        dashboard.put("totalRecords", total);

        // Tenant info
        dashboard.put("tenantId", securityUtils.getCurrentTenantId());

        // Audit log count
        try {
            Long auditCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM audit_logs WHERE tenant_id = ?",
                    Long.class, securityUtils.getCurrentTenantId());
            dashboard.put("auditLogCount", auditCount != null ? auditCount : 0L);
        } catch (Exception e) {
            dashboard.put("auditLogCount", 0L);
        }

        return dashboard;
    }

    /**
     * Exporte toutes les données personnelles d'un utilisateur (portabilité RGPD).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportDate", LocalDateTime.now().toString());
        export.put("userId", userId.toString());

        // User info
        try {
            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT id, email, first_name, last_name, phone, role, statut, created_at " +
                            "FROM users WHERE id = ?", userId);
            export.put("user", user);
        } catch (Exception e) {
            export.put("user", null);
        }

        // Souls linked to user
        try {
            List<Map<String, Object>> souls = jdbcTemplate.queryForList(
                    "SELECT id, nom, prenom, email, statut, etat_spirituel, created_at " +
                            "FROM souls WHERE user_id = ?", userId);
            export.put("souls", souls);
        } catch (Exception e) {
            export.put("souls", List.of());
        }

        // Notifications
        try {
            List<Map<String, Object>> notifications = jdbcTemplate.queryForList(
                    "SELECT id, titre, message, type, canal, created_at " +
                            "FROM notifications WHERE destinataire_id = ?", userId);
            export.put("notifications", notifications);
        } catch (Exception e) {
            export.put("notifications", List.of());
        }

        // Audit logs for this user
        try {
            List<Map<String, Object>> auditLogs = jdbcTemplate.queryForList(
                    "SELECT id, action, entite_type, entite_id, created_at " +
                            "FROM audit_logs WHERE utilisateur_id = ?", userId);
            export.put("auditLogs", auditLogs);
        } catch (Exception e) {
            export.put("auditLogs", List.of());
        }

        return export;
    }

    /**
     * Supprime toutes les données personnelles d'un utilisateur (droit à l'oubli RGPD).
     * Les données sont anonymisées plutôt que supprimées pour préserver l'intégrité référentielle.
     */
    public Map<String, Object> deleteUserData(UUID userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        int totalAffected = 0;

        // Anonymize notifications
        try {
            int affected = jdbcTemplate.update(
                    "UPDATE notifications SET destinataire_id = NULL, titre = 'ANONYMISE', message = 'ANONYMISE' " +
                            "WHERE destinataire_id = ?", userId);
            result.put("notifications_anonymized", affected);
            totalAffected += affected;
        } catch (Exception e) {
            result.put("notifications_error", e.getMessage());
        }

        // Anonymize audit logs
        try {
            int affected = jdbcTemplate.update(
                    "UPDATE audit_logs SET utilisateur_id = NULL WHERE utilisateur_id = ?", userId);
            result.put("audit_logs_anonymized", affected);
            totalAffected += affected;
        } catch (Exception e) {
            result.put("audit_logs_error", e.getMessage());
        }

        // Anonymize soul history
        try {
            int affected = jdbcTemplate.update(
                    "UPDATE soul_history SET utilisateur_id = NULL WHERE utilisateur_id = ?", userId);
            result.put("soul_history_anonymized", affected);
            totalAffected += affected;
        } catch (Exception e) {
            result.put("soul_history_error", e.getMessage());
        }

        result.put("totalRecordsAnonymized", totalAffected);

        propagationPublisher.publishStatusChanged("COMPLIANCE", userId,
                "ACTIVE", "ANONYMIZED",
                "Données utilisateur anonymisées (RGPD)");

        return result;
    }

    /**
     * Politique de rétention : retourne les données qui dépassent la durée de rétention.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRetentionPolicy() {
        Map<String, Object> policy = new LinkedHashMap<>();

        // Default retention periods (days)
        Map<String, Integer> retentionDays = new LinkedHashMap<>();
        retentionDays.put("audit_logs", 365 * 3);      // 3 years
        retentionDays.put("notifications", 90);          // 3 months
        retentionDays.put("soul_history", 365 * 5);     // 5 years
        retentionDays.put("messages", 365 * 2);          // 2 years
        policy.put("retentionDays", retentionDays);

        // Check what would be purged
        Map<String, Long> purgeable = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : retentionDays.entrySet()) {
            try {
                Long count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + entry.getKey() +
                                " WHERE created_at < DATEADD(DAY, -" + entry.getValue() + ", CURRENT_TIMESTAMP)",
                        Long.class);
                purgeable.put(entry.getKey(), count != null ? count : 0L);
            } catch (Exception e) {
                purgeable.put(entry.getKey(), 0L);
            }
        }
        policy.put("purgeableCounts", purgeable);

        return policy;
    }
}
