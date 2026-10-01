package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.ChurchEventArchive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChurchEventArchiveRepository extends JpaRepository<ChurchEventArchive, UUID> {

    List<ChurchEventArchive> findByTenantIdOrderByEventStartAtDesc(UUID tenantId);

    List<ChurchEventArchive> findByTenantIdAndEventYearOrderByEventStartAtDesc(UUID tenantId, Integer eventYear);

    List<ChurchEventArchive> findByTenantIdAndEventYearAndEventMonthOrderByEventStartAtDesc(
            UUID tenantId, Integer eventYear, Integer eventMonth);

    List<ChurchEventArchive> findByEventIdOrderByVersionDesc(UUID eventId);

    Optional<ChurchEventArchive> findFirstByEventIdOrderByVersionDesc(UUID eventId);

    long countByTenantId(UUID tenantId);
}
