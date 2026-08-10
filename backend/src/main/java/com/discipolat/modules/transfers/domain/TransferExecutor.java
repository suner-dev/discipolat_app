package com.discipolat.modules.transfers.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyChiefHistory;
import com.discipolat.modules.families.domain.FamilyChiefHistoryRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.members.domain.MemberDepartment;
import com.discipolat.modules.members.domain.MemberDepartmentRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulHistory;
import com.discipolat.modules.souls.domain.SoulHistoryRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserDepartment;
import com.discipolat.modules.users.domain.UserDepartmentRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * EXÉCUTION AUTOMATIQUE des transferts.
 * Une fois toutes les validations obtenues, le moteur applique le transfert :
 * mise à jour des relations en base, historiques métier, recalcul des
 * indicateurs (les tableaux de bord étant calculés à la volée, la mise à
 * jour des données suffit), permissions, et notification de toutes les
 * personnes concernées. Aucune étape manuelle n'est nécessaire.
 */
@Service
@Transactional
public class TransferExecutor {

    private static final Logger log = LoggerFactory.getLogger(TransferExecutor.class);

    private final SoulRepository soulRepository;
    private final FamilyRepository familyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final MemberDepartmentRepository memberDepartmentRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final FamilyChiefHistoryRepository chiefHistoryRepository;
    private final SoulHistoryRepository soulHistoryRepository;
    private final NotificationService notificationService;

    public TransferExecutor(SoulRepository soulRepository,
                            FamilyRepository familyRepository,
                            DepartmentRepository departmentRepository,
                            UserRepository userRepository,
                            SoulDepartmentRepository soulDepartmentRepository,
                            MemberDepartmentRepository memberDepartmentRepository,
                            UserDepartmentRepository userDepartmentRepository,
                            FamilyChiefHistoryRepository chiefHistoryRepository,
                            SoulHistoryRepository soulHistoryRepository,
                            NotificationService notificationService) {
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.memberDepartmentRepository = memberDepartmentRepository;
        this.userDepartmentRepository = userDepartmentRepository;
        this.chiefHistoryRepository = chiefHistoryRepository;
        this.soulHistoryRepository = soulHistoryRepository;
        this.notificationService = notificationService;
    }

    /** Exécute le transfert pour une demande validée, avec les règles de la config de workflow. */
    public void execute(TransferRequest req, Map<String, Object> reglesExecution) {
        Map<String, Object> regles = reglesExecution != null ? reglesExecution : Map.of();
        UUID targetId = idOf(req.getNouvelleAffectation());
        switch (req.getType()) {
            case MEMBRE_DEPARTEMENT_TRANSFERT -> transferMemberDepartment(req, targetId, regles);
            case MEMBRE_DEPARTEMENT_AJOUT -> addMemberDepartment(req, targetId, regles);
            case MEMBRE_DEPARTEMENT_RETRAIT -> removeMemberDepartment(req, targetId, regles);
            case DISCIPLE_FAMILLE_TRANSFERT -> transferDiscipleFamily(req, targetId, regles);
            case FAISEUR_FAMILLE_TRANSFERT -> transferFaiseurFamily(req, targetId, regles);
            case CHEF_FAMILLE_TRANSFERT -> changeChefFamille(req, targetId, regles);
            case FAISEUR_DISCIPLE_CHANGEMENT -> changeDiscipleFaiseur(req, targetId, regles);
            case RESPONSABLE_DEPARTEMENT_CHANGEMENT -> changeDepartementResponsable(req, targetId, regles);
            case CHEF_ADJOINT_CHANGEMENT -> changeChefAdjoint(req, targetId, regles);
        }
    }

    // ========================================================================
    // DÉPARTEMENTS
    // ========================================================================

    /** Transfert d'un membre (âme) d'un département vers un autre. */
    private void transferMemberDepartment(TransferRequest req, UUID newDeptId, Map<String, Object> reglesExecution) {
        Soul soul = soul(req.getPersonneId());
        Department newDept = department(newDeptId);

        UUID oldDeptId = idOf(req.getAncienneAffectation());
        if (oldDeptId != null) {
            desaffecterDept(soul.getId(), oldDeptId);
            memberDepartmentRepository.findBySoulId(soul.getId()).stream()
                    .filter(md -> md.getDepartmentId().equals(oldDeptId))
                    .forEach(memberDepartmentRepository::delete);
        }
        affecterDept(soul.getId(), newDeptId);
        if (!memberDepartmentRepository.existsBySoulIdAndDepartmentId(soul.getId(), newDeptId)) {
            memberDepartmentRepository.save(MemberDepartment.builder()
                    .soulId(soul.getId()).departmentId(newDeptId).build());
        }

        logSoulHistory(soul, "TRANSFERT_DEPARTEMENT",
                "Transfert de département vers « " + newDept.getNom() + " »",
                Map.of("ancienDepartementId", oldDeptId, "nouveauDepartementId", newDeptId));
        notify(req, "Vous avez été transféré(e) vers le département « " + newDept.getNom() + " »", TypeNotification.TRANSFERT_EXECUTEE);
        notifyDeptResponsable(newDeptId, "Un membre a été transféré vers votre département : " + soul.getNomComplet());
    }

    /** Ajout d'un membre dans un nouveau département. */
    private void addMemberDepartment(TransferRequest req, UUID deptId, Map<String, Object> reglesExecution) {
        Soul soul = soul(req.getPersonneId());
        Department dept = department(deptId);
        affecterDept(soul.getId(), deptId);
        if (!memberDepartmentRepository.existsBySoulIdAndDepartmentId(soul.getId(), deptId)) {
            memberDepartmentRepository.save(MemberDepartment.builder()
                    .soulId(soul.getId()).departmentId(deptId).build());
        }
        logSoulHistory(soul, "AJOUT_DEPARTEMENT",
                "Ajout au département « " + dept.getNom() + " »",
                Map.of("departementId", deptId));
        notify(req, "Vous avez été ajouté(e) au département « " + dept.getNom() + " »", TypeNotification.TRANSFERT_EXECUTEE);
        notifyDeptResponsable(deptId, "Un membre a rejoint votre département : " + soul.getNomComplet());
    }

    /** Retrait d'un membre d'un département. */
    private void removeMemberDepartment(TransferRequest req, UUID deptId, Map<String, Object> reglesExecution) {
        Soul soul = soul(req.getPersonneId());
        Department dept = department(deptId);
        desaffecterDept(soul.getId(), deptId);
        memberDepartmentRepository.findBySoulId(soul.getId()).stream()
                .filter(md -> md.getDepartmentId().equals(deptId))
                .forEach(memberDepartmentRepository::delete);
        logSoulHistory(soul, "RETRAIT_DEPARTEMENT",
                "Retrait du département « " + dept.getNom() + " »",
                Map.of("departementId", deptId));
        notify(req, "Vous avez été retiré(e) du département « " + dept.getNom() + " »", TypeNotification.TRANSFERT_EXECUTEE);
        notifyDeptResponsable(deptId, "Un membre a été retiré de votre département : " + soul.getNomComplet());
    }

    // ========================================================================
    // FAMILLES DE DISCIPLES
    // ========================================================================

    /** Transfert d'un disciple (âme) vers une autre famille. */
    private void transferDiscipleFamily(TransferRequest req, UUID newFamilyId, Map<String, Object> reglesExecution) {
        Soul soul = soul(req.getPersonneId());
        Family newFamily = family(newFamilyId);
        UUID oldFamilyId = soul.getFamilleId();
        soul.setFamilleId(newFamilyId);
        soulRepository.save(soul);
        logSoulHistory(soul, "TRANSFERT_FAMILLE",
                "Transfert vers la famille « " + newFamily.getNom() + " »",
                Map.of("ancienneFamilleId", oldFamilyId, "nouvelleFamilleId", newFamilyId));
        notify(req, "Vous avez été transféré(e) vers la famille « " + newFamily.getNom() + " »", TypeNotification.TRANSFERT_EXECUTEE);
    }

    /** Transfert d'un faiseur vers une autre famille (avec ou sans ses disciples). */
    private void transferFaiseurFamily(TransferRequest req, UUID newFamilyId, Map<String, Object> reglesExecution) {
        User faiseur = user(req.getPersonneId());
        Family newFamily = family(newFamilyId);
        UUID oldFamilyId = faiseur.getFamilleGereeId();
        faiseur.setFamilleGereeId(newFamilyId);
        userRepository.save(faiseur);

        // Règle d'exécution configurable : transfererAmes (par défaut : non)
        boolean transfererAmes = configFlag(reglesExecution, "transfererAmes", false);
        if (transfererAmes) {
            for (Soul s : soulRepository.findAllByFaiseurId(faiseur.getId())) {
                if (s.isDeleted()) continue;
                UUID oldFam = s.getFamilleId();
                s.setFamilleId(newFamilyId);
                soulRepository.save(s);
                logSoulHistory(s, "TRANSFERT_FAMILLE",
                        "Disciple transféré avec son faiseur vers « " + newFamily.getNom() + " »",
                        Map.of("ancienneFamilleId", oldFam, "nouvelleFamilleId", newFamilyId));
            }
        }
        log.info("Faiseur {} transféré vers la famille {}", faiseur.getId(), newFamilyId);
        notify(req, "Votre famille d'affectation est désormais « " + newFamily.getNom() + " »", TypeNotification.TRANSFERT_EXECUTEE);
    }

    /** Transfert d'un chef de famille vers une autre famille (changement de chef). */
    private void changeChefFamille(TransferRequest req, UUID newFamilyId, Map<String, Object> reglesExecution) {
        User newChef = user(req.getPersonneId());
        Family family = family(newFamilyId);
        UUID oldChefId = family.getChefFamilleId();

        family.setChefFamilleId(newChef.getId());
        family.setUserId(newChef.getId());
        familyRepository.save(family);

        chiefHistoryRepository.save(FamilyChiefHistory.builder()
                .familleId(newFamilyId)
                .ancienChefId(oldChefId != null && oldChefId.equals(newChef.getId()) ? null : oldChefId)
                .nouveauChefId(newChef.getId())
                .changedBy(req.getDemandeurId())
                .raison("Transfert de chef de famille (workflow de transfert)")
                .build());

        if (oldChefId != null) {
            userRepository.findById(oldChefId).ifPresent(oldChef -> {
                oldChef.setEstChefDeFamille(false);
                oldChef.setFamilleGereeId(null);
                userRepository.save(oldChef);
            });
        }
        newChef.setEstChefDeFamille(true);
        newChef.setFamilleGereeId(newFamilyId);
        userRepository.save(newChef);

        notify(req, "Vous êtes désormais chef de la famille « " + family.getNom() + " »", TypeNotification.TRANSFERT_EXECUTEE);
    }

    /** Changement du chef adjoint d'une famille. */
    private void changeChefAdjoint(TransferRequest req, UUID newFamilyId, Map<String, Object> reglesExecution) {
        User newAdjoint = user(req.getPersonneId());
        Family family = family(newFamilyId);
        family.setChefAdjointId(newAdjoint.getId());
        familyRepository.save(family);
        notify(req, "Vous êtes désormais chef adjoint de la famille « " + family.getNom() + " »", TypeNotification.TRANSFERT_EXECUTEE);
    }

    // ========================================================================
    // AFFECTATIONS
    // ========================================================================

    /** Changement du faiseur responsable d'un disciple. */
    private void changeDiscipleFaiseur(TransferRequest req, UUID newFaiseurId, Map<String, Object> reglesExecution) {
        Soul soul = soul(req.getPersonneId());
        User newFaiseur = user(newFaiseurId);
        UUID oldFaiseurId = soul.getFaiseurId();
        soul.setFaiseurId(newFaiseurId);
        soulRepository.save(soul);

        logSoulHistory(soul, "REAFFECTATION_FAISEUR",
                "Réaffecté au faiseur « " + newFaiseur.getFirstName() + " " + newFaiseur.getLastName() + " »",
                Map.of("ancienFaiseurId", oldFaiseurId, "nouveauFaiseurId", newFaiseurId));

        notify(req, "Votre nouveau faiseur de suivi est " + newFaiseur.getFirstName() + " " + newFaiseur.getLastName(),
                TypeNotification.TRANSFERT_EXECUTEE);
        if (oldFaiseurId != null && !oldFaiseurId.equals(newFaiseurId)) {
            notifyUser(oldFaiseurId, "L'âme « " + soul.getNomComplet() + " » vous a été retirée de votre suivi",
                    TypeNotification.TRANSFERT_EXECUTEE, req.getId());
        }
        notifyUser(newFaiseurId, "L'âme « " + soul.getNomComplet() + " » vous a été affectée pour suivi",
                TypeNotification.TRANSFERT_EXECUTEE, req.getId());
    }

    /** Changement du responsable principal d'un département. */
    private void changeDepartementResponsable(TransferRequest req, UUID deptId, Map<String, Object> reglesExecution) {
        User newResponsable = user(req.getPersonneId());
        Department dept = department(deptId);
        UUID oldResponsableId = dept.getResponsableId();
        dept.setResponsableId(newResponsable.getId());
        departmentRepository.save(dept);

        // Mise à jour de la table de liaison user_departments
        userDepartmentRepository.findByDepartmentId(deptId).stream()
                .filter(ud -> "RESPONSABLE".equals(ud.getRoleDansDept()))
                .forEach(userDepartmentRepository::delete);
        if (!userDepartmentRepository.existsByUserIdAndDepartmentId(newResponsable.getId(), deptId)) {
            userDepartmentRepository.save(UserDepartment.builder()
                    .userId(newResponsable.getId())
                    .departmentId(deptId)
                    .roleDansDept("RESPONSABLE")
                    .build());
        } else {
            userDepartmentRepository.findByUserId(newResponsable.getId()).stream()
                    .filter(ud -> ud.getDepartmentId().equals(deptId))
                    .forEach(ud -> ud.setRoleDansDept("RESPONSABLE"));
        }

        notify(req, "Vous êtes désormais responsable du département « " + dept.getNom() + " »",
                TypeNotification.TRANSFERT_EXECUTEE);
        if (oldResponsableId != null && !oldResponsableId.equals(newResponsable.getId())) {
            notifyUser(oldResponsableId, "Vous n'êtes plus responsable du département « " + dept.getNom() + " »",
                    TypeNotification.TRANSFERT_EXECUTEE, req.getId());
        }
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    /** Règle d'exécution booléenne issue de la config de workflow. */
    private boolean configFlag(Map<String, Object> reglesExecution, String key, boolean defaut) {
        Object value = reglesExecution.get(key);
        if (value == null) return defaut;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private void affecterDept(UUID soulId, UUID deptId) {
        List<SoulDepartment> existing = soulDepartmentRepository.findBySoulId(soulId).stream()
                .filter(sd -> sd.getDepartmentId().equals(deptId))
                .toList();
        if (existing.isEmpty()) {
            soulDepartmentRepository.save(SoulDepartment.builder()
                    .soulId(soulId).departmentId(deptId)
                    .actif(true).dateAffectation(LocalDateTime.now())
                    .build());
        } else {
            SoulDepartment sd = existing.get(0);
            sd.setActif(true);
            sd.setDateDesaffectation(null);
            soulDepartmentRepository.save(sd);
        }
    }

    private void desaffecterDept(UUID soulId, UUID deptId) {
        for (SoulDepartment sd : soulDepartmentRepository.findBySoulId(soulId)) {
            if (sd.getDepartmentId().equals(deptId)) {
                sd.setActif(false);
                sd.setDateDesaffectation(LocalDateTime.now());
                soulDepartmentRepository.save(sd);
            }
        }
    }

    private void logSoulHistory(Soul soul, String type, String description, Map<String, Object> metadata) {
        try {
            soulHistoryRepository.save(SoulHistory.builder()
                    .ameId(soul.getId())
                    .typeEvenement(type)
                    .description(description)
                    .metadata(metadata)
                    .build());
        } catch (Exception e) {
            log.warn("Échec de l'écriture de l'historique d'âme : {}", e.getMessage());
        }
    }

    /** Notification au demandeur / à la personne concernée (si elle a un compte). */
    private void notify(TransferRequest req, String message, TypeNotification type) {
        notifyUser(req.getPersonneId(), message, type, req.getId());
        if (!req.getPersonneId().equals(req.getDemandeurId())) {
            notifyUser(req.getDemandeurId(), message, type, req.getId());
        }
    }

    private void notifyUser(UUID userId, String message, TypeNotification type, UUID entiteId) {
        try {
            notificationService.create(userId, type, CanalNotification.IN_APP,
                    "Transfert exécuté", message, entiteId, "TRANSFER");
        } catch (Exception e) {
            log.warn("Notification de transfert non envoyée à {} : {}", userId, e.getMessage());
        }
    }

    private void notifyDeptResponsable(UUID deptId, String message) {
        departmentRepository.findById(deptId).ifPresent(dept -> {
            if (dept.getResponsableId() != null) {
                notifyUser(dept.getResponsableId(), message, TypeNotification.TRANSFERT_EXECUTEE, deptId);
            }
        });
    }

    private UUID idOf(Map<String, Object> affectation) {
        if (affectation == null) return null;
        Object id = affectation.get("id");
        if (id == null) return null;
        if (id instanceof UUID uuid) return uuid;
        return UUID.fromString(id.toString());
    }

    private Soul soul(UUID id) {
        return soulRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Soul", id));
    }

    private User user(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    private Family family(UUID id) {
        return familyRepository.findById(id)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Family", id));
    }

    private Department department(UUID id) {
        return departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Department", id));
    }
}
