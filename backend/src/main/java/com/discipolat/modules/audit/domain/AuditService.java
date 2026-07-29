package com.discipolat.modules.audit.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.discipolat.common.domain.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    public AuditService(AuditLogRepository auditLogRepository, SecurityUtils securityUtils) {
        this.auditLogRepository = auditLogRepository;
        this.securityUtils = securityUtils;
    }

    public AuditLog findById(UUID id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AuditLog", id));
    }

    public void log(String action, String entiteType, UUID entiteId,
                    Map<String, Object> ancienValeur, Map<String, Object> nouvelleValeur,
                    HttpServletRequest request) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entiteType(entiteType)
                .entiteId(entiteId)
                .ancienValeur(ancienValeur)
                .nouvelleValeur(nouvelleValeur)
                .adresseIp(request != null ? request.getRemoteAddr() : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();

        try {
            log.setUtilisateurId(securityUtils.getCurrentUserId());
        } catch (Exception e) {
            // System operations
        }

        auditLogRepository.save(log);
    }

    public void logSimple(String action, String entiteType, UUID entiteId) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entiteType(entiteType)
                .entiteId(entiteId)
                .build();

        try {
            log.setUtilisateurId(securityUtils.getCurrentUserId());
        } catch (Exception e) {
            // System operations
        }

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByUtilisateur(UUID utilisateurId, Pageable pageable) {
        return auditLogRepository.findByUtilisateurIdOrderByCreatedAtDesc(utilisateurId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findByEntite(String entiteType, UUID entiteId, Pageable pageable) {
        return auditLogRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(entiteType, entiteId, pageable);
    }
}
