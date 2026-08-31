package com.discipolat.modules.passport.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpiritualPassportEntryRepository extends JpaRepository<SpiritualPassportEntry, UUID> {

    List<SpiritualPassportEntry> findByPassportIdOrderByCreatedAtAsc(UUID passportId);

    List<SpiritualPassportEntry> findByPassportIdInOrderByCreatedAtAsc(List<UUID> passportIds);
}
