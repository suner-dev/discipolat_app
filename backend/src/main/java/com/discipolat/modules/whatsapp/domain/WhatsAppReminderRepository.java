package com.discipolat.modules.whatsapp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WhatsAppReminderRepository extends JpaRepository<WhatsAppReminder, UUID> {
    List<WhatsAppReminder> findByTenantIdAndStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
            UUID tenantId, String status, LocalDateTime now);
    List<WhatsAppReminder> findByTenantIdOrderByScheduledAtDesc(UUID tenantId);
}
