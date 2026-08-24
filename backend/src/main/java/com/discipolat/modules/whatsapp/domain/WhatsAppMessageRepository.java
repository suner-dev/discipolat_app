package com.discipolat.modules.whatsapp.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, UUID> {
    Page<WhatsAppMessage> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    List<WhatsAppMessage> findByTenantIdAndPhoneNumberOrderByCreatedAtDesc(UUID tenantId, String phoneNumber);
    Optional<WhatsAppMessage> findByTenantIdAndWaMessageId(UUID tenantId, String waMessageId);
}
