package com.discipolat.modules.users.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.ForbiddenException;
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
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.SoulExitRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.departments.domain.DepartmentDossierService;
import com.discipolat.modules.evaluations.domain.EvaluationService;
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
    private final WorkspaceScopeService workspaceScopeService;
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final FamilyRepository familyRepository;
    private final DepartmentRepository departmentRepository;
    private final EvaluationService evaluationService;
    private final DepartmentDossierService dossierService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       SecurityUtils securityUtils, SoulRepository soulRepository,
                       SoulExitRepository soulExitRepository,
                       SoulHistoryRepository soulHistoryRepository,
                       AuditService auditService,
                       WorkspaceScopeService workspaceScopeService,
                       SoulDepartmentRepository soulDepartmentRepository,
                       FamilyRepository familyRepository,
                       DepartmentRepository departmentRepository,
                       EvaluationService evaluationService,
                       DepartmentDossierService dossierService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
        this.soulRepository = soulRepository;
        this.soulExitRepository = soulExitRepository;
        this.soulHistoryRepository = soulHistoryRepository;
        this.auditService = auditService;
        this.workspaceScopeService = workspaceScopeService;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.evaluationService = evaluationService;
        this.dossierService = dossierService;
    }

    // ======================== US-12: PROMOTE TO FAISEUR ========================

    public User promoteToFaiseur(UUID userId) {
        User user = findById(userId);
        if (user.getRoles().contains(UserRole.FAISEUR)) {
            throw new BusinessRuleException("User already has the Faiseur role");
        }
        user.getRoles().add(UserRole.FAISEUR);
        user.setRole(UserRole.FAISEUR);
        // Auto-set active role if not set
        if (user.getActiveRole() == null) {
            user.setActiveRole(UserRole.FAISEUR);
        }
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
        if (!user.getRoles().contains(UserRole.FAISEUR)) {
            throw new BusinessRuleException("User does not have the Faiseur role");
        }
        // US-17: Check that souls are reassigned before demoting
        long activeSouls = soulRepository.countByFaiseurId(userId);
        if (activeSouls > 0) {
            throw new BusinessRuleException(
                    "Cannot demote faiseur with " + activeSouls + " active soul(s). Please reassign them first.",
                    "FAISEUR_HAS_ACTIVE_SOULS");
        }
        // Remove FAISEUR from roles set
        user.getRoles().remove(UserRole.FAISEUR);
        // If new role provided, add it
        if (newRole != null && newRole != UserRole.FAISEUR) {
            user.getRoles().add(newRole);
            user.setRole(newRole);
            user.setActiveRole(newRole);
        } else {
            // Fallback: keep the primary role, just remove FAISEUR
            if (user.getRoles().isEmpty()) {
                user.getRoles().add(UserRole.MEMBRE);
                user.setRole(UserRole.MEMBRE);
                user.setActiveRole(UserRole.MEMBRE);
            } else {
                UserRole fallback = user.getRoles().iterator().next();
                user.setRole(fallback);
                user.setActiveRole(fallback);
            }
        }
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
        assertCanAssignRoles(user.getRole(), user.getRoles());
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessRuleException("Email already exists: " + user.getEmail());
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        // US-02: New accounts start in PENDING_ACTIVATION status
        user.setStatut(UserStatus.PENDING_ACTIVATION);
        user.setEstChefDeFamille(false);
        user.setTwoFactorEnabled(false);
        // Ensure roles set contains at least the primary role
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(new HashSet<>(Set.of(user.getRole())));
        }
        // Ensure active role is set
        if (user.getActiveRole() == null) {
            user.setActiveRole(user.getRole());
        }
        return userRepository.save(user);
    }

    public User findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
        assertUserAccessible(user);
        return user;
    }

    /**
     * Checks if the current user can access the target user based on
     * both tenant isolation and workspace scope.
     */
    private void assertUserAccessible(User target) {
        if (securityUtils.isSuperUser()) return;
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (target.getId().equals(currentUserId)) return;
        if (securityUtils.hasActiveRole("RESPONSABLE")) {
            Set<UUID> accessibleFaiseurIds = workspaceScopeService.accessibleFaiseurIds();
            if (accessibleFaiseurIds.contains(target.getId())) return;
        }
        throw new com.discipolat.common.exception.ForbiddenException("You do not have access to this user");
    }

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User update(User updatedUser) {
        assertCanAssignRoles(updatedUser.getRole(), updatedUser.getRoles());
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
        // Update roles if provided
        if (updatedUser.getRoles() != null && !updatedUser.getRoles().isEmpty()) {
            existing.setRoles(updatedUser.getRoles());
        }
        // Update active role if provided
        if (updatedUser.getActiveRole() != null) {
            existing.setActiveRole(updatedUser.getActiveRole());
        }
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
        if (!securityUtils.isSuperUser() && !workspaceScopeService.canAccessFamily(familleId)) {
            throw new ForbiddenException("You do not have access to this family");
        }
        return userRepository.findByFamilleGereeId(familleId);
    }

    // ======================== MULTI-ROLE METHODS ========================

    /** Find users whose roles set contains the given role */
    @Transactional(readOnly = true)
    public List<User> findByRolesContaining(UserRole role) {
        return userRepository.findByRolesContaining(role);
    }

    /** Find users whose roles set contains the given role (paginated) */
    @Transactional(readOnly = true)
    public Page<User> findByRolesContaining(UserRole role, Pageable pageable) {
        return userRepository.findByRolesContaining(role, pageable);
    }

    /** Add a role to a user's roles set */
    public User addRole(UUID userId, UserRole role) {
        assertCanAssignRoles(role, java.util.Set.of(role));
        User user = findById(userId);
        user.getRoles().add(role);
        user.markUpdated();
        return userRepository.save(user);
    }

    /** Remove a role from a user's roles set */
    public User removeRole(UUID userId, UserRole role) {
        User user = findById(userId);
        user.getRoles().remove(role);
        // Don't remove the last role — fallback to MEMBRE
        if (user.getRoles().isEmpty()) {
            user.getRoles().add(UserRole.MEMBRE);
        }
        // If active role was removed, switch to another
        if (user.getActiveRole() == role && !user.getRoles().isEmpty()) {
            user.setActiveRole(user.getRoles().iterator().next());
            user.setRole(user.getActiveRole());
        }
        user.markUpdated();
        return userRepository.save(user);
    }

    /** Set the active role for a user */
    public User setActiveRole(UUID userId, UserRole activeRole) {
        User user = findById(userId);
        if (!user.getRoles().contains(activeRole)) {
            throw new BusinessRuleException("User does not have the role: " + activeRole);
        }
        user.setActiveRole(activeRole);
        user.setRole(activeRole);
        user.markUpdated();
        return userRepository.save(user);
    }

    /** Replace the entire roles set for a user (admin operation) */
    public User replaceRoles(UUID userId, Set<UserRole> newRoles) {
        if (newRoles == null || newRoles.isEmpty()) {
            throw new BusinessRuleException("User must have at least one role");
        }
        assertCanAssignRoles(newRoles.iterator().next(), newRoles);
        User user = findById(userId);
        user.setRoles(new HashSet<>(newRoles));
        // Ensure active role is valid
        if (user.getActiveRole() == null || !newRoles.contains(user.getActiveRole())) {
            user.setActiveRole(newRoles.iterator().next());
            user.setRole(user.getActiveRole());
        }
        user.markUpdated();
        return userRepository.save(user);
    }

    /** Check if the user's only role is the given one (helper for controller filtering) */
    @Transactional(readOnly = true)
    public boolean isOnlyRole(String currentRole, String roleName) {
        if (currentRole == null) return false;
        try {
            UUID userId = securityUtils.getCurrentUserId();
            User user = findById(userId);
            return user.getRoles().size() == 1 && user.getRoles().contains(UserRole.valueOf(roleName));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Anti-escalation : le rôle actif limite les rôles qu'un utilisateur peut assigner.
     * ADMIN : tout ; PASTEUR : tout sauf ADMIN ; RESPONSABLE : jamais ADMIN/PASTEUR/RESPONSABLE.
     */
    private void assertCanAssignRoles(UserRole primaryRole, Set<UserRole> roles) {
        if (securityUtils.hasActiveRole("ADMIN")) return;
        Set<UserRole> assigned = new HashSet<>();
        if (primaryRole != null) assigned.add(primaryRole);
        if (roles != null) assigned.addAll(roles);
        if (assigned.contains(UserRole.ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Seul un administrateur peut assigner le rôle ADMIN");
        }
        if (securityUtils.hasActiveRole("PASTEUR")) return;
        if (assigned.stream().anyMatch(r -> r == UserRole.PASTEUR || r == UserRole.RESPONSABLE)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous ne pouvez pas assigner le rôle PASTEUR ou RESPONSABLE");
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "Opération non autorisée dans votre espace métier");
    }

    public User promoteToChefDeFamille(UUID userId, UUID familleId) {
        User user = findById(userId);
        user.setEstChefDeFamille(true);
        user.setFamilleGereeId(familleId);
        // Ensure CHEF_DE_FAMILLE role is in the roles set
        user.getRoles().add(UserRole.CHEF_DE_FAMILLE);
        user.markUpdated();
        return userRepository.save(user);
    }

    public User demoteFromChefDeFamille(UUID userId) {
        User user = findById(userId);
        user.setEstChefDeFamille(false);
        user.setFamilleGereeId(null);
        // Remove CHEF_DE_FAMILLE role from the set, but keep other roles
        user.getRoles().remove(UserRole.CHEF_DE_FAMILLE);
        // If active role was CHEF_DE_FAMILLE, switch to another role
        if (user.getActiveRole() == UserRole.CHEF_DE_FAMILLE && !user.getRoles().isEmpty()) {
            user.setActiveRole(user.getRoles().iterator().next());
            user.setRole(user.getActiveRole());
        }
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

    /**
     * Charge de travail des faiseurs, scopée par rôle actif :
     * - super-utilisateurs (Admin/Pasteur) : tous les faiseurs (ou filtrés par familleId) ;
     * - CHEF_DE_FAMILLE actif : les faiseurs de SA famille (familleGereeId) ;
     * - RESPONSABLE actif : les faiseurs des membres de SES départements
     *   (via WorkspaceScopeService.accessibleFaiseurIds) ;
     * - tout autre rôle : liste vide (hors espace métier).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFaiseurWorkload(UUID familleId) {
        List<UUID> faiseurIds;

        if (securityUtils.isSuperUser()) {
            if (familleId != null) {
                // Super-utilisateurs : faiseurs des âmes de la famille demandée
                faiseurIds = soulRepository.findAllByFamilleId(familleId).stream()
                        .filter(s -> !s.isDeleted() && s.getFaiseurId() != null)
                        .map(Soul::getFaiseurId)
                        .distinct()
                        .toList();
            } else {
                faiseurIds = userRepository.findByRole(UserRole.FAISEUR).stream()
                        .map(User::getId)
                        .toList();
            }
        } else if (securityUtils.hasActiveRole("CHEF_DE_FAMILLE")) {
            // Chef : uniquement les faiseurs de la famille qu'il gère.
            UUID fam = userRepository.findById(securityUtils.getCurrentUserId())
                    .map(User::getFamilleGereeId)
                    .orElse(null);
            if (fam == null) return List.of();
            faiseurIds = soulRepository.findAllByFamilleId(fam).stream()
                    .filter(s -> !s.isDeleted() && s.getFaiseurId() != null)
                    .map(Soul::getFaiseurId)
                    .distinct()
                    .toList();
        } else if (securityUtils.hasActiveRole("RESPONSABLE")) {
            // Responsable : faiseurs des membres de ses départements.
            faiseurIds = workspaceScopeService.accessibleFaiseurIds().stream().toList();
        } else {
            return List.of();
        }

        if (faiseurIds.isEmpty()) return List.of();

        Map<UUID, User> faiseursById = userRepository.findAllById(faiseurIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<Map<String, Object>> workloads = new ArrayList<>();
        for (UUID faiseurId : faiseurIds) {
            User faiseur = faiseursById.get(faiseurId);
            if (faiseur == null) continue;

            long soulCount = soulRepository.countByFaiseurId(faiseurId);
            Map<String, Object> workload = new LinkedHashMap<>();
            workload.put("faiseurId", faiseur.getId());
            workload.put("faiseurName", faiseur.getFirstName() + " " + faiseur.getLastName());
            workload.put("email", faiseur.getEmail());
            workload.put("soulCount", soulCount);
            workload.put("familleId", familleId);
            workload.put("familleName", null);

            // US-14: Indicateur léger/normal/surchargé — codes normalisés sans
            // accent pour correspondre au dictionnaire configurable USER_CHARGE.
            String indicator;
            if (soulCount <= 3) indicator = "LEGER";
            else if (soulCount <= 7) indicator = "NORMAL";
            else indicator = "SURCHARGE";
            workload.put("charge", indicator);

            workloads.add(workload);
        }

        // Les plus chargés en premier
        workloads.sort(Comparator.comparingLong(
                (Map<String, Object> w) -> (Long) w.get("soulCount")).reversed());
        return workloads;
    }

    // ======================== US-16: FAISEUR HISTORY ========================

    // ======================== FICHE UTILISATEUR COMPLÈTE ========================

    /**
     * Fiche complète d'un utilisateur pour l'encadrement (responsable, chef de
     * famille, faiseur, pasteur/admin) : identité, âme liée au compte, âmes
     * suivies (si faiseur), départements + membres (si responsable), famille
     * gérée (si chef de famille), évaluations reçues et MES évaluations.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserDetail(UUID userId) {
        User u = findById(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", u.getId());
        result.put("firstName", u.getFirstName());
        result.put("lastName", u.getLastName());
        result.put("email", u.getEmail());
        result.put("phone", u.getPhone());
        result.put("role", u.getRole().name());
        result.put("statut", u.getStatut().name());
        result.put("estChefDeFamille", u.isEstChefDeFamille());
        result.put("dateCreation", u.getCreatedAt());

        // Âme(s) liée(s) au compte (soul.userId)
        List<Soul> linkedSouls = soulRepository.findAllByUserId(userId).stream()
                .filter(s -> !s.isDeleted()).toList();
        Soul linkedSoul = linkedSouls.isEmpty() ? null : linkedSouls.get(0);
        if (linkedSoul != null) {
            Soul s = linkedSoul;
            Map<String, Object> ame = new LinkedHashMap<>();
            ame.put("id", s.getId());
            ame.put("nomComplet", s.getNomComplet());
            ame.put("telephone", s.getTelephone());
            ame.put("email", s.getEmail());
            ame.put("profession", s.getProfession());
            ame.put("statut", s.getStatut().name());
            ame.put("typeDisciple", s.getTypeDisciple().name());
            ame.put("dateIntegration", s.getDateIntegration() != null ? s.getDateIntegration().toString() : null);
            ame.put("familleId", s.getFamilleId());
            ame.put("familleNom", s.getFamilleId() != null
                    ? familyRepository.findById(s.getFamilleId()).map(Family::getNom).orElse(null) : null);
            ame.put("faiseurId", s.getFaiseurId());
            ame.put("faiseurNom", s.getFaiseurId() != null
                    ? userRepository.findById(s.getFaiseurId())
                            .map(f -> f.getFirstName() + " " + f.getLastName()).orElse(null) : null);
            result.put("ame", ame);
        } else {
            result.put("ame", null);
        }

        // Faiseur : âmes qu'il suit (actuelles) + sorties passées
        if (u.getRoles().contains(UserRole.FAISEUR)) {
            List<Map<String, Object>> amesSuivies = soulRepository.findAllByFaiseurId(userId).stream()
                    .filter(s -> !s.isDeleted())
                    .map(s -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", s.getId());
                        m.put("nom", s.getNomComplet());
                        m.put("statut", s.getStatut().name());
                        m.put("typeDisciple", s.getTypeDisciple().name());
                        m.put("familleNom", s.getFamilleId() != null
                                ? familyRepository.findById(s.getFamilleId()).map(Family::getNom).orElse(null) : null);
                        m.put("dateIntegration", s.getDateIntegration() != null ? s.getDateIntegration().toString() : null);
                        m.put("hasCompte", s.getUserId() != null);
                        m.put("userId", s.getUserId());
                        return m;
                    })
                    .toList();
            result.put("amesSuivies", amesSuivies);
            result.put("nombreAmesSuivies", (long) amesSuivies.size());
            result.put("sorties", soulExitRepository.findByFaiseurIdOrderByCreatedAtDesc(userId).stream()
                    .map(ex -> Map.<String, Object>of(
                            "ameId", ex.getAmeId(),
                            "motif", ex.getMotif(),
                            "dateSortie", ex.getDateSortie().toString()))
                    .toList());
        }

        // Responsable : départements dirigés + leurs membres
        if (u.getRoles().contains(UserRole.RESPONSABLE)) {
            List<Map<String, Object>> departements = new ArrayList<>();
            for (Department d : departmentRepository.findByResponsableId(userId)) {
                Map<String, Object> dm = new LinkedHashMap<>();
                dm.put("id", d.getId());
                dm.put("nom", d.getNom());
                List<UUID> soulIds = soulDepartmentRepository.findByDepartmentIdAndActifTrue(d.getId()).stream()
                        .map(sd -> sd.getSoulId()).toList();
                List<Map<String, Object>> membres = new ArrayList<>();
                if (!soulIds.isEmpty()) {
                    for (Soul s : soulRepository.findAllById(soulIds)) {
                        if (s.isDeleted()) continue;
                        Map<String, Object> mm = new LinkedHashMap<>();
                        mm.put("id", s.getId());
                        mm.put("nomComplet", s.getNomComplet());
                        mm.put("statut", s.getStatut().name());
                        mm.put("typeDisciple", s.getTypeDisciple().name());
                        mm.put("telephone", s.getTelephone());
                        mm.put("email", s.getEmail());
                        mm.put("familleNom", s.getFamilleId() != null
                                ? familyRepository.findById(s.getFamilleId()).map(Family::getNom).orElse(null) : null);
                        mm.put("faiseurNom", s.getFaiseurId() != null
                                ? userRepository.findById(s.getFaiseurId())
                                        .map(f -> f.getFirstName() + " " + f.getLastName()).orElse(null) : null);
                        mm.put("userId", s.getUserId());
                        membres.add(mm);
                    }
                    membres.sort(Comparator.comparing(m -> String.valueOf(m.get("nomComplet"))));
                }
                dm.put("membres", membres);
                departements.add(dm);
            }
            result.put("departements", departements);
        }

        // Chef de famille : famille gérée + ses membres
        if (u.getFamilleGereeId() != null) {
            familyRepository.findById(u.getFamilleGereeId()).ifPresent(fam -> {
                Map<String, Object> famille = new LinkedHashMap<>();
                famille.put("id", fam.getId());
                famille.put("nom", fam.getNom());
                List<Map<String, Object>> membres = soulRepository.findAllByFamilleId(fam.getId()).stream()
                        .filter(s -> !s.isDeleted())
                        .map(s -> {
                            Map<String, Object> mm = new LinkedHashMap<>();
                            mm.put("id", s.getId());
                            mm.put("nomComplet", s.getNomComplet());
                            mm.put("statut", s.getStatut().name());
                            mm.put("faiseurNom", s.getFaiseurId() != null
                                    ? userRepository.findById(s.getFaiseurId())
                                            .map(f -> f.getFirstName() + " " + f.getLastName()).orElse(null) : null);
                            return mm;
                        })
                        .toList();
                famille.put("membres", membres);
                result.put("familleGeree", famille);
            });
        }

        // Évaluations reçues (agrégat anonyme) + MES évaluations (pour modifier)
        result.put("evaluations", evaluationService.getUserEvalScores(userId));
        result.put("monEvaluation", evaluationService.getMyEvaluationsFor(userId));

        // Dossier du membre (objectifs, rapports du responsable, notes,
        // documents) — limité aux départements accessibles à l'appelant
        // (super-utilisateur : tous ; responsable : ses départements ;
        // chef de famille / faiseur : aucun).
        if (linkedSoul != null) {
            Set<UUID> accessibleDeptIds = securityUtils.isSuperUser()
                    ? departmentRepository.findAll().stream().map(Department::getId).collect(Collectors.toSet())
                    : workspaceScopeService.accessibleDepartmentIds();
            List<Map<String, Object>> dossier = dossierService.dossierUtilisateur(linkedSoul.getId(), accessibleDeptIds);
            for (Map<String, Object> d : dossier) {
                UUID deptId = (UUID) d.get("departmentId");
                d.put("departmentNom", departmentRepository.findById(deptId).map(Department::getNom).orElse(null));
            }
            result.put("dossier", dossier);
            result.put("dossierDocuments", dossierService.dossierDocuments(linkedSoul.getId()));
        } else {
            result.put("dossier", List.of());
            result.put("dossierDocuments", List.of());
        }
        return result;
    }

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

}
