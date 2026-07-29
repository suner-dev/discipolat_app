package com.discipolat.modules.prayers.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PrayerService {

    private final PrayerRepository prayerRepository;
    private final SecurityUtils securityUtils;

    public PrayerService(PrayerRepository prayerRepository, SecurityUtils securityUtils) {
        this.prayerRepository = prayerRepository;
        this.securityUtils = securityUtils;
    }

    public Prayer create(Prayer prayer) {
        prayer.setAuteurId(securityUtils.getCurrentUserId());
        prayer.setStatut("EN_COURS");
        return prayerRepository.save(prayer);
    }

    @Transactional(readOnly = true)
    public Prayer findById(UUID id) {
        return prayerRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Prayer", id));
    }

    @Transactional(readOnly = true)
    public Page<Prayer> findAll(Pageable pageable) {
        return prayerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Prayer> findByAuteurId(UUID auteurId, Pageable pageable) {
        return prayerRepository.findByAuteurIdAndDeletedFalse(auteurId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Prayer> findByFamilleId(UUID familleId, Pageable pageable) {
        // US-47: Use custom sort by priority (HAUTE first) then date
        return prayerRepository.findByFamilleIdAndDeletedFalseOrderByPrioriteDateDesc(familleId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Prayer> findByFamilleIdAndStatut(UUID familleId, String statut, Pageable pageable) {
        return prayerRepository.findByFamilleIdAndStatutAndDeletedFalse(familleId, statut, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Prayer> findByFamilleIdAndCategorie(UUID familleId, String categorie, Pageable pageable) {
        return prayerRepository.findByFamilleIdAndCategorieAndDeletedFalse(familleId, categorie, pageable);
    }

    @Transactional(readOnly = true)
    public List<Prayer> findByAmeId(UUID ameId) {
        return prayerRepository.findByAmeIdAndDeletedFalse(ameId);
    }

    // ======================== US-48: ACTIONS DE GRÂCE ========================

    @Transactional(readOnly = true)
    public List<Prayer> findAllAnswered() {
        return prayerRepository.findByStatutAndDeletedFalseOrderByDateExauceeDesc("EXAUCE");
    }

    @Transactional(readOnly = true)
    public List<Prayer> findAnsweredByFamille(UUID familleId) {
        return prayerRepository.findByFamilleIdAndStatutAndDeletedFalseOrderByDateExauceeDesc(familleId, "EXAUCE");
    }

    public Prayer update(UUID id, Prayer updated) {
        Prayer prayer = findById(id);
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!prayer.getAuteurId().equals(currentUserId)) {
            throw new SecurityException("Only the author can modify a prayer");
        }
        if (updated.getTitre() != null) prayer.setTitre(updated.getTitre());
        if (updated.getDescription() != null) prayer.setDescription(updated.getDescription());
        if (updated.getCategorie() != null) prayer.setCategorie(updated.getCategorie());
        if (updated.getPriorite() != null) prayer.setPriorite(updated.getPriorite());
        if (updated.getVisibilite() != null) prayer.setVisibilite(updated.getVisibilite());
        return prayerRepository.save(prayer);
    }

    public Prayer markAsAnswered(UUID id, String temoignage) {
        Prayer prayer = findById(id);
        prayer.setStatut("EXAUCE");
        prayer.setTemoignage(temoignage);
        prayer.setDateExaucee(LocalDateTime.now());
        return prayerRepository.save(prayer);
    }

    public void delete(UUID id) {
        Prayer prayer = findById(id);
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!prayer.getAuteurId().equals(currentUserId)) {
            throw new SecurityException("Only the author can delete a prayer");
        }
        prayer.setDeleted(true);
        prayerRepository.save(prayer);
    }
}
