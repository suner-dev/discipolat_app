package com.discipolat.modules.prayers.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PrayerService {

    private final PrayerRepository prayerRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SoulRepository soulRepository;

    public PrayerService(PrayerRepository prayerRepository, SecurityUtils securityUtils,
                         NotificationService notificationService, UserRepository userRepository,
                         SoulRepository soulRepository) {
        this.prayerRepository = prayerRepository;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.soulRepository = soulRepository;
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

    /**
     * Phase 9: Find all prayers filtered by the current user's role-based visibility.
     * PASTEUR/ADMIN see everything.
     * RESPONSABLE sees GENERALE, PASTEUR_RESPONSABLE, and their own PRIVEE.
     * FAISEUR/Chef sees GENERALE, FAISEUR, and their own PRIVEE.
     */
    @Transactional(readOnly = true)
    public Page<Prayer> findAll(Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        String currentUserRole = securityUtils.getCurrentUserRole();

        if ("PASTEUR".equals(currentUserRole) || "ADMIN".equals(currentUserRole)) {
            return prayerRepository.findAll(pageable);
        }

        List<String> allowedVisibilites = new ArrayList<>();
        allowedVisibilites.add("GENERALE");
        allowedVisibilites.add("PARTAGEE"); // legacy family scope

        if ("RESPONSABLE".equals(currentUserRole)) {
            allowedVisibilites.add("PASTEUR_RESPONSABLE");
        } else {
            // FAISEUR or Chef de famille
            allowedVisibilites.add("FAISEUR");
        }

        // Return prayers the user can see (by visibility level + their own private prayers)
        return prayerRepository.findByAuteurIdOrVisibiliteIn(currentUserId, allowedVisibilites, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Prayer> findByVisibilite(String visibilite, Pageable pageable) {
        return prayerRepository.findByVisibiliteInAndDeletedFalse(List.of(visibilite), pageable);
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

    /**
     * Phase 9: Find answered prayers (actions de grâce) filtered by role-based visibility.
     */
    @Transactional(readOnly = true)
    public List<Prayer> findAllAnsweredByVisibility() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        String currentUserRole = securityUtils.getCurrentUserRole();

        if ("PASTEUR".equals(currentUserRole) || "ADMIN".equals(currentUserRole)) {
            return prayerRepository.findByStatutAndDeletedFalseOrderByDateExauceeDesc("EXAUCE");
        }

        List<String> allowedVisibilites = new ArrayList<>();
        allowedVisibilites.add("GENERALE");
        allowedVisibilites.add("PARTAGEE"); // legacy family scope

        if ("RESPONSABLE".equals(currentUserRole)) {
            allowedVisibilites.add("PASTEUR_RESPONSABLE");
        } else {
            allowedVisibilites.add("FAISEUR");
        }

        return prayerRepository.findAll().stream()
                .filter(p -> "EXAUCE".equals(p.getStatut()) && !p.isDeleted()
                        && (allowedVisibilites.contains(p.getVisibilite()) || p.getAuteurId().equals(currentUserId)))
                .sorted((a, b) -> {
                    if (a.getDateExaucee() == null) return 1;
                    if (b.getDateExaucee() == null) return -1;
                    return b.getDateExaucee().compareTo(a.getDateExaucee());
                })
                .toList();
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

    /**
     * Mark a prayer as answered and notify subscribers:
     * - The prayer author
     * - The pasteur(s)
     * - The faiseur of the referenced soul (if any)
     * - The chef de famille of the referenced family (if any)
     */
    public Prayer markAsAnswered(UUID id, String temoignage) {
        Prayer prayer = findById(id);
        prayer.setStatut("EXAUCE");
        prayer.setTemoignage(temoignage);
        prayer.setDateExaucee(LocalDateTime.now());
        Prayer saved = prayerRepository.save(prayer);

        // Notify subscribers about the answered prayer
        notifyAnsweredPrayer(saved);

        return saved;
    }

    /**
     * Send in-app + email notifications to all subscribers of a prayer.
     * Notified users:
     * - The prayer author
     * - The faiseur of the referenced soul (if any)
     * - The chef de famille (if familleId set)
     * - All PASTEURs
     */
    private void notifyAnsweredPrayer(Prayer prayer) {
        Set<UUID> notifiedUsers = new HashSet<>();
        String titre = "🙏 Prière exaucée : " + prayer.getTitre();
        String temoignageMsg = prayer.getTemoignage() != null && !prayer.getTemoignage().isEmpty()
                ? "\"" + prayer.getTemoignage() + "\""
                : "Dieu a répondu à cette prière !";

        // Helper: send IN_APP + EMAIL notifications to a user
        // (EMAIL records are stored for future email-sending infrastructure)
        java.util.function.Consumer<UUID> notify = (userId) -> {
            if (notifiedUsers.contains(userId)) return;
            notificationService.create(
                    userId, TypeNotification.PRIERE_EXAUCEE, CanalNotification.IN_APP,
                    titre, temoignageMsg, prayer.getId(), "PRAYER"
            );
            notificationService.create(
                    userId, TypeNotification.PRIERE_EXAUCEE, CanalNotification.EMAIL,
                    titre, temoignageMsg, prayer.getId(), "PRAYER"
            );
            notifiedUsers.add(userId);
        };

        // 1. Prayer author
        notify.accept(prayer.getAuteurId());

        // 2. Faiseur of the referenced soul
        if (prayer.getAmeId() != null) {
            soulRepository.findById(prayer.getAmeId()).ifPresent(soul -> {
                if (soul.getFaiseurId() != null) {
                    notify.accept(soul.getFaiseurId());
                }
            });
        }

        // 3. Chef de famille (if set)
        if (prayer.getFamilleId() != null) {
            userRepository.findByFamilleGereeId(prayer.getFamilleId()).stream()
                    .filter(User::isEstChefDeFamille)
                    .findFirst()
                    .ifPresent(chef -> notify.accept(chef.getId()));
        }

        // 4. All PASTEURs
        userRepository.findByRole(UserRole.PASTEUR)
                .forEach(pasteur -> notify.accept(pasteur.getId()));
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
