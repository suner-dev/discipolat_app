package com.discipolat.modules.announcements.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnouncementService {

    private final ScheduledAnnouncementRepository announcementRepo;

    public AnnouncementService(ScheduledAnnouncementRepository announcementRepo) {
        this.announcementRepo = announcementRepo;
    }

    public List<ScheduledAnnouncement> listAll() {
        return announcementRepo.findByTenantIdAndStatusInOrderByScheduledAtDesc(
                TenantContext.getCurrentTenantId(),
                List.of(ScheduledAnnouncement.Status.DRAFT, ScheduledAnnouncement.Status.SCHEDULED, ScheduledAnnouncement.Status.PUBLISHED));
    }

    public ScheduledAnnouncement get(UUID id) {
        return announcementRepo.findById(id).orElseThrow(() -> new RuntimeException("Annonce introuvable: " + id));
    }

    public ScheduledAnnouncement create(ScheduledAnnouncement announcement) {
        announcement.setTenantId(TenantContext.getCurrentTenantId());
        announcement.setStatus(ScheduledAnnouncement.Status.DRAFT);
        announcement.setCreatedAt(LocalDateTime.now());
        return announcementRepo.save(announcement);
    }

    public ScheduledAnnouncement update(UUID id, ScheduledAnnouncement updates) {
        ScheduledAnnouncement existing = get(id);
        if (updates.getTitle() != null) existing.setTitle(updates.getTitle());
        if (updates.getContent() != null) existing.setContent(updates.getContent());
        if (updates.getTarget() != null) existing.setTarget(updates.getTarget());
        if (updates.getTargetId() != null) existing.setTargetId(updates.getTargetId());
        if (updates.getScheduledAt() != null) existing.setScheduledAt(updates.getScheduledAt());
        if (updates.getExpiresAt() != null) existing.setExpiresAt(updates.getExpiresAt());
        if (updates.getPinToTop() != null) existing.setPinToTop(updates.getPinToTop());
        if (updates.getSendNotification() != null) existing.setSendNotification(updates.getSendNotification());
        return announcementRepo.save(existing);
    }

    public ScheduledAnnouncement schedule(UUID id, LocalDateTime scheduledAt) {
        ScheduledAnnouncement announcement = get(id);
        announcement.setStatus(ScheduledAnnouncement.Status.SCHEDULED);
        announcement.setScheduledAt(scheduledAt);
        return announcementRepo.save(announcement);
    }

    public ScheduledAnnouncement publishNow(UUID id) {
        ScheduledAnnouncement announcement = get(id);
        announcement.setStatus(ScheduledAnnouncement.Status.PUBLISHED);
        announcement.setPublishedAt(LocalDateTime.now());
        return announcementRepo.save(announcement);
    }

    public void cancel(UUID id) {
        ScheduledAnnouncement announcement = get(id);
        announcement.setStatus(ScheduledAnnouncement.Status.CANCELLED);
        announcementRepo.save(announcement);
    }

    public void delete(UUID id) {
        announcementRepo.deleteById(id);
    }

    /** Called by scheduler to publish due announcements */
    public List<ScheduledAnnouncement> publishDue() {
        List<ScheduledAnnouncement> due = announcementRepo
                .findByTenantIdAndStatusAndScheduledAtBefore(TenantContext.getCurrentTenantId(),
                        ScheduledAnnouncement.Status.SCHEDULED, LocalDateTime.now());
        due.forEach(a -> {
            a.setStatus(ScheduledAnnouncement.Status.PUBLISHED);
            a.setPublishedAt(LocalDateTime.now());
        });
        return announcementRepo.saveAll(due);
    }

    /** Expire old announcements */
    public int expireOld() {
        var published = announcementRepo.findByTenantIdAndStatusOrderByScheduledAtDesc(
                TenantContext.getCurrentTenantId(), ScheduledAnnouncement.Status.PUBLISHED);
        int count = 0;
        for (ScheduledAnnouncement a : published) {
            if (a.getExpiresAt() != null && a.getExpiresAt().isBefore(LocalDateTime.now())) {
                a.setStatus(ScheduledAnnouncement.Status.EXPIRED);
                announcementRepo.save(a);
                count++;
            }
        }
        return count;
    }

    public Map<String, Object> getStats() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, Object> stats = new HashMap<>();
        var all = announcementRepo.findByTenantIdAndStatusInOrderByScheduledAtDesc(tenantId, List.of(ScheduledAnnouncement.Status.values()));
        stats.put("total", all.size());
        stats.put("drafts", all.stream().filter(a -> a.getStatus() == ScheduledAnnouncement.Status.DRAFT).count());
        stats.put("scheduled", all.stream().filter(a -> a.getStatus() == ScheduledAnnouncement.Status.SCHEDULED).count());
        stats.put("published", all.stream().filter(a -> a.getStatus() == ScheduledAnnouncement.Status.PUBLISHED).count());
        stats.put("expired", all.stream().filter(a -> a.getStatus() == ScheduledAnnouncement.Status.EXPIRED).count());
        return stats;
    }
}
