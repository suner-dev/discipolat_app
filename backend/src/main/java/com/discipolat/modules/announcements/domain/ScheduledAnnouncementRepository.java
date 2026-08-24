package com.discipolat.modules.announcements.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledAnnouncementRepository extends JpaRepository<ScheduledAnnouncement, UUID> {
    List<ScheduledAnnouncement> findByTenantIdAndStatusOrderByScheduledAtDesc(UUID tenantId, ScheduledAnnouncement.Status status);
    List<ScheduledAnnouncement> findByTenantIdAndStatusInOrderByScheduledAtDesc(UUID tenantId, List<ScheduledAnnouncement.Status> statuses);
    List<ScheduledAnnouncement> findByTenantIdAndStatusAndScheduledAtBefore(UUID tenantId, ScheduledAnnouncement.Status status, LocalDateTime now);
}
