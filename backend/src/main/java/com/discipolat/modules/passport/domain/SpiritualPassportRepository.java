package com.discipolat.modules.passport.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpiritualPassportRepository extends JpaRepository<SpiritualPassport, UUID> {

    Optional<SpiritualPassport> findByPassportCode(String passportCode);

    Optional<SpiritualPassport> findByTenantIdAndMemberId(UUID tenantId, UUID memberId);

    Optional<SpiritualPassport> findByIdAndTenantId(UUID id, UUID tenantId);
}
