package com.discipolat.modules.users.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.souls.domain.SoulHistory;
import com.discipolat.modules.souls.domain.SoulHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.SoulExitRepository;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final SoulRepository soulRepository;
    private final SoulExitRepository soulExitRepository;
    private final SoulHistoryRepository soulHistoryRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       SecurityUtils securityUtils, SoulRepository soulRepository,
                       SoulExitRepository soulExitRepository,
                       SoulHistoryRepository soulHistoryRepository,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
        this.soulRepository = soulRepository;
        this.soulExitRepository = soulExitRepository;
        this.soulHistoryRepository = soulHistoryRepository;
        this.auditService = auditService;
    }

    // ======================== US-12: PROMOTE TO FAISEUR ========================

    public User promoteToFaiseur(UUID userId) {
        User user = findById(userId);
        if (user.getRole() == UserRole.FAISEUR) {
            throw new BusinessRuleException("User is already a Faiseur");
        }
        user.setRole(UserRole.FAISEUR);
        user.markUpdated();
        user = userRepository.save(user);

        // US-12: Log promotion date in soul_history for audit trail
        try {
            SoulHistory history = new SoulHistory();
            history.setAmeId(userId);
            history.setTypeEvenement("PROMOTION_FAISEUR");
            history.setDescription("Disciple promu au rang de Faiseur");
            history.setUtilisateurId(securityUtils.getCurrentUserId());
            soulHistoryRepository.save(history);
            log.info("Promotion to Faiseur logged for user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to log promotion to history: {}", e.getMessage());
        }

        return user;
    }

    // ======================== US-17: DEMOTE FAISEUR ========================

    public User demoteFaiseur(UUID userId, UserRole newRole) {
        User user = findById(userId);
        if (user.getRole() != UserRole.FAISEUR) {
            throw new BusinessRuleException("User is not a Faiseur");
        }
        if (newRole == UserRole.FAISEUR) {
            throw new BusinessRuleException("Cannot demote to same role");
        }
        // US-17: Check that souls are reassigned before demoting
        long activeSouls = soulRepository.countByFaiseurId(userId);
        if (activeSouls > 0) {
            throw new BusinessRuleException(
                    "Cannot demote faiseur with " + activeSouls + " active soul(s). Please reassign them first.",
                    "FAISEUR_HAS_ACTIVE_SOULS");
        }
        user.setRole(newRole != null ? newRole : UserRole.FAISEUR);
        user.setEstChefDeFamille(false);
        user.setFamilleGereeId(null);
        user.markUpdated();
        return userRepository.save(user);
    }

    // ======================== US-57: RGPD HARD DELETE ========================

    public void hardDeleteUser(UUID userId) {
        User user = findById(userId);
        // Log to audit before permanent deletion
        auditService.logSimple("HARD_DELETE_USER", "USER", userId);
        userRepository.delete(user);
    }

    // ======================== US-60: RESTORE ENTITY ========================

    public User restoreUser(UUID userId) {
        User user = findById(userId);
        user.setStatut(UserStatus.ACTIVE);
        user.setDeleted(false);
        user.setDeletedAt(null);
        user.markUpdated();
        return userRepository.save(user);
    }

    public User create(User user, String rawPassword) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessRuleException("Email already exists: " + user.getEmail());
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        // US-02: New accounts start in PENDING_ACTIVATION status
        user.setStatut(UserStatus.PENDING_ACTIVATION);
        user.setEstChefDeFamille(false);
        user.setTwoFactorEnabled(false);
        return userRepository.save(user);
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User update(User updatedUser) {
        User existing = findById(updatedUser.getId());
        if (!existing.getEmail().equals(updatedUser.getEmail())
                && userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new BusinessRuleException("Email already exists: " + updatedUser.getEmail());
        }
        existing.setEmail(updatedUser.getEmail());
        existing.setFirstName(updatedUser.getFirstName());
        existing.setLastName(updatedUser.getLastName());
        existing.setPhone(updatedUser.getPhone());
        existing.setRole(updatedUser.getRole());
        existing.markUpdated();
        return userRepository.save(existing);
    }

    public void deactivate(UUID id) {
        User user = findById(id);
        user.setStatut(UserStatus.INACTIVE);
        user.markUpdated();
        userRepository.save(user);
    }

    public void activate(UUID id) {
        User user = findById(id);
        user.setStatut(UserStatus.ACTIVE);
        user.markUpdated();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User", "email", email));
    }

    @Transactional(readOnly = true)
    public List<User> findByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public Page<User> findByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable);
    }

    @Transactional(readOnly = true)
    public List<User> findByFamilleGereeId(UUID familleId) {
        return userRepository.findByFamilleGereeId(familleId);
    }

    public User promoteToChefDeFamille(UUID userId, UUID familleId) {
        User user = findById(userId);
        user.setEstChefDeFamille(true);
        user.setFamilleGereeId(familleId);
        user.markUpdated();
        return userRepository.save(user);
    }

    public User demoteFromChefDeFamille(UUID userId) {
        User user = findById(userId);
        user.setEstChefDeFamille(false);
        user.setFamilleGereeId(null);
        user.markUpdated();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findChefsDeFamille() {
        return userRepository.findByEstChefDeFamilleTrue();
    }

    /** Self-update: any authenticated user can update their own profile */
    public User updateMyProfile(String firstName, String lastName, String phone,
                                 java.time.LocalDate dateNaissance, String situationFamiliale) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User existing = findById(currentUserId);
        if (firstName != null) existing.setFirstName(firstName);
        if (lastName != null) existing.setLastName(lastName);
        if (phone != null) existing.setPhone(phone);
        if (dateNaissance != null) existing.setDateNaissance(dateNaissance);
        if (situationFamiliale != null) existing.setSituationFamiliale(situationFamiliale);
        existing.markUpdated();
        return userRepository.save(existing);
    }

    // ======================== US-14: FAISEUR WORKLOAD ========================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFaiseurWorkload(UUID familleId) {
        List<User> faiseurs = userRepository.findByRole(UserRole.FAISEUR);
        List<Map<String, Object>> workloads = new ArrayList<>();

        for (User faiseur : faiseurs) {
            long soulCount = soulRepository.countByFaiseurId(faiseur.getId());
            Map<String, Object> workload = new LinkedHashMap<>();
            workload.put("faiseurId", faiseur.getId());
            workload.put("nom", faiseur.getFirstName() + " " + faiseur.getLastName());
            workload.put("email", faiseur.getEmail());
            workload.put("nombreAmes", soulCount);

            // US-14: Indicator léger/normal/surchargé
            String indicator;
            if (soulCount <= 3) indicator = "LEGER";
            else if (soulCount <= 7) indicator = "NORMAL";
            else indicator = "SURCHARGÉ";
            workload.put("charge", indicator);

            workloads.add(workload);
        }

        return workloads;
    }

    // ======================== US-16: FAISEUR HISTORY ========================

    @Transactional(readOnly = true)
    public Map<String, Object> getFaiseurHistory(UUID faiseurId) {
        User faiseur = findById(faiseurId);
        Map<String, Object> history = new LinkedHashMap<>();
        history.put("faiseurId", faiseur.getId());
        history.put("nom", faiseur.getFirstName() + " " + faiseur.getLastName());
        history.put("role", faiseur.getRole());
        history.put("estChef", faiseur.isEstChefDeFamille());
        history.put("dateCreation", faiseur.getCreatedAt());

        // Souls currently managed
        List<Soul> currentSouls = soulRepository.findAllByFaiseurId(faiseur.getId());
        history.put("amesActuelles", currentSouls.stream()
                .filter(s -> !s.isDeleted())
                .map(s -> Map.of("id", (Object) s.getId(), "nom", s.getNomComplet(), "statut", s.getStatut().name()))
                .toList());
        history.put("nombreAmesActuelles", currentSouls.stream().filter(s -> !s.isDeleted()).count());

        // Past exits
        List<com.discipolat.modules.souls.domain.SoulExit> pastExits =
                soulExitRepository.findByFaiseurIdOrderByCreatedAtDesc(faiseur.getId());
        history.put("sorties", pastExits.stream()
                .map(ex -> Map.<String, Object>of(
                        "ameId", ex.getAmeId(),
                        "motif", ex.getMotif(),
                        "dateSortie", ex.getDateSortie().toString()))
                .toList());

        return history;
    }

    // ======================== US-13: TRANSFER FAISEUR ========================

    @Transactional(readOnly = true)
    public User transferFaiseur(UUID faiseurId, UUID nouvelleFamilleId, boolean transfererAmes) {
        User faiseur = findById(faiseurId);
        UUID ancienneFamilleId = faiseur.getFamilleGereeId();
        faiseur.setFamilleGereeId(nouvelleFamilleId);
        userRepository.save(faiseur);

        if (transfererAmes) {
            List<Soul> souls = soulRepository.findAllByFaiseurId(faiseurId);
            for (Soul soul : souls) {
                soul.setFamilleId(nouvelleFamilleId);
                soulRepository.save(soul);
            }
        }

        return faiseur;
    }
}
