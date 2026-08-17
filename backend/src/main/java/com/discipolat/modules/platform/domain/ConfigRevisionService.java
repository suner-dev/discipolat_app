package com.discipolat.modules.platform.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service du versionnage de configuration : consigne une révision
 * append-only à chaque changement de configuration plateforme et permet
 * de la relire (audit / reprise). L'auteur est résolu depuis le contexte
 * de sécurité quand il existe.
 */
@Service
@Transactional
public class ConfigRevisionService {

    private final ConfigRevisionRepository repository;
    private final SecurityUtils securityUtils;

    public ConfigRevisionService(ConfigRevisionRepository repository, SecurityUtils securityUtils) {
        this.repository = repository;
        this.securityUtils = securityUtils;
    }

    /**
     * Consigne une révision. N'échoue jamais si l'utilisateur est introuvable
     * (opérations système) : l'auteur reste alors nul.
     */
    public void record(String entityType, String entityKey, String action, Map<String, Object> payload) {
        ConfigRevision revision = ConfigRevision.builder()
                .entityType(entityType)
                .entityKey(entityKey)
                .action(action)
                .payload(payload)
                .build();
        try {
            revision.setUserId(securityUtils.getCurrentUserId());
        } catch (Exception e) {
            // Opération système (sans contexte utilisateur).
        }
        repository.save(revision);
    }

    @Transactional(readOnly = true)
    public Page<ConfigRevision> list(String entityType, Pageable pageable) {
        return repository.findFiltered(entityType, pageable);
    }
}
