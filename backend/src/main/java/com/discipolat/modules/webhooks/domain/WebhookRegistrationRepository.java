package com.discipolat.modules.webhooks.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookRegistrationRepository extends JpaRepository<WebhookRegistration, UUID> {
}
