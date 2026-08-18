package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.TypeNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository des modèles de notification. Le filtrage multi-tenant est EXPRESSE :
 * chaque requête porte le {@code tenantId} en paramètre, ce qui reste fiable même
 * hors contexte requête (jobs planifiés) où le filtre Hibernate n'est pas actif.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    List<NotificationTemplate> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<NotificationTemplate> findByTenantIdAndEvent(UUID tenantId, TypeNotification event);

    Optional<NotificationTemplate> findByTenantIdAndEventAndActifTrue(UUID tenantId, TypeNotification event);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndActifTrue(UUID tenantId);
}
