package com.discipolat.modules.sermon.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SermonTranslationRepository extends JpaRepository<SermonTranslation, UUID> {
    List<SermonTranslation> findBySermonIdOrderByCreeLeDesc(UUID sermonId);
    List<SermonTranslation> findByTenantIdOrderByCreeLeDesc(UUID tenantId);
}
