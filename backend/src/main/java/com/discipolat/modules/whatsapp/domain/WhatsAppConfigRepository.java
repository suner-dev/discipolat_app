package com.discipolat.modules.whatsapp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppConfigRepository extends JpaRepository<WhatsAppConfig, UUID> {
    Optional<WhatsAppConfig> findByTenantId(UUID tenantId);
    Optional<WhatsAppConfig> findByPhoneNumberId(String phoneNumberId);
}
