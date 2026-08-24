package com.discipolat.modules.prayerJournal.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P1 #50 — Journal de prière personnel avec partage avec faiseur.
 */
@Service
@Transactional
public class PrayerJournalService {

    private final PrayerJournalRepository repository;

    public PrayerJournalService(PrayerJournalRepository repository) {
        this.repository = repository;
    }

    public Page<PrayerJournalEntry> listByMember(UUID membreId, Pageable pageable) {
        return repository.findByMembreIdOrderByCreatedAtDesc(membreId, pageable);
    }

    public PrayerJournalEntry getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PrayerJournalEntry", id));
    }

    public PrayerJournalEntry create(UUID membreId, String contenu, String category, String visibilité) {
        PrayerJournalEntry entry = new PrayerJournalEntry();
        entry.setTenantId(TenantContext.getCurrentTenantId());
        entry.setMembreId(membreId);
        entry.setContenu(contenu);
        entry.setCategory(category);
        entry.setVisibilité(PrayerJournalEntry.Visibilité.valueOf(visibilité != null ? visibilité : "PRIVÉE"));
        entry.setStatut(PrayerJournalEntry.Statut.EN_COURS);
        return repository.save(entry);
    }

    public PrayerJournalEntry markAnswered(UUID id, String réponse) {
        PrayerJournalEntry entry = getById(id);
        entry.setStatut(PrayerJournalEntry.Statut.EXAUCÉE);
        entry.setRéponse(réponse);
        entry.setExaucéeAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    public PrayerJournalEntry markRemembered(UUID id) {
        PrayerJournalEntry entry = getById(id);
        entry.setStatut(PrayerJournalEntry.Statut.MÉMORISÉE);
        entry.setUpdatedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    /**
     * Share prayer with faiseur (change visibility).
     */
    public PrayerJournalEntry shareWithFaiseur(UUID id) {
        PrayerJournalEntry entry = getById(id);
        entry.setVisibilité(PrayerJournalEntry.Visibilité.FAISEUR);
        entry.setUpdatedAt(LocalDateTime.now());
        return repository.save(entry);
    }

    /**
     * Get prayers that are still in progress (need follow-up).
     */
    public List<PrayerJournalEntry> getPendingPrayers(UUID membreId) {
        return repository.findByMembreIdOrderByCreatedAtDesc(membreId, PageRequest.of(0, 100))
                .stream()
                .filter(e -> e.getStatut() == PrayerJournalEntry.Statut.EN_COURS)
                .collect(Collectors.toList());
    }

    /**
     * Get prayer journal statistics for a member.
     */
    public Map<String, Object> getStats(UUID membreId) {
        var entries = repository.findByMembreIdOrderByCreatedAtDesc(membreId, PageRequest.of(0, 1000));
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", entries.getNumberOfElements());
        stats.put("enCours", entries.stream().filter(e -> e.getStatut() == PrayerJournalEntry.Statut.EN_COURS).count());
        stats.put("exaucées", entries.stream().filter(e -> e.getStatut() == PrayerJournalEntry.Statut.EXAUCÉE).count());
        stats.put("mémorisées", entries.stream().filter(e -> e.getStatut() == PrayerJournalEntry.Statut.MÉMORISÉE).count());
        stats.put("partagées", entries.stream().filter(e -> e.getVisibilité() == PrayerJournalEntry.Visibilité.FAISEUR).count());
        return stats;
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
