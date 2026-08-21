package com.discipolat.modules.communications.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.communications.api.CommunicationRequest;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Outil métier COMMUNICATION : annonces de l'église avec diffusion ciblée.
 * La publication envoie une notification IN_APP à chaque destinataire
 * (TOUS / rôle / famille / département). La lecture est ouverte à tous les
 * rôles authentifiés ; la gestion est réservée ADMIN / PASTEUR (contrôlé
 * au niveau contrôleur + garde-fou de module ModuleGateFilter).
 */
@Service
@Transactional
public class CommunicationService {

    private final CommunicationRepository communicationRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;
    private final UserRepository userRepository;
    private final SoulRepository soulRepository;
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final DepartmentRepository departmentRepository;

    public CommunicationService(CommunicationRepository communicationRepository,
                                NotificationService notificationService,
                                SecurityUtils securityUtils,
                                AuditService auditService,
                                EntityPropagationPublisher propagationPublisher,
                                UserRepository userRepository,
                                SoulRepository soulRepository,
                                SoulDepartmentRepository soulDepartmentRepository,
                                DepartmentRepository departmentRepository) {
        this.communicationRepository = communicationRepository;
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
        this.userRepository = userRepository;
        this.soulRepository = soulRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.departmentRepository = departmentRepository;
    }

    /* ------------------------------ Lecture ------------------------------ */

    /** Annonces publiées visibles par l'utilisateur courant (cible respectée). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listForCurrentUser() {
        UUID userId = securityUtils.getCurrentUserId();
        return communicationRepository.findByDeletedFalseAndStatutOrderByDatePublicationDesc(
                        Communication.Statut.PUBLIEE).stream()
                .filter(c -> targetUserIds(c).contains(userId))
                .map(this::toMap)
                .toList();
    }

    /** Toutes les annonces (gestion ADMIN / PASTEUR). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll() {
        return communicationRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toMap)
                .toList();
    }

    /* ------------------------------ Gestion ------------------------------ */

    public Map<String, Object> create(CommunicationRequest request) {
        Communication c = Communication.builder()
                .titre(request.titre().trim())
                .contenu(request.contenu().trim())
                .cible(request.cible())
                .roles(request.cible() == Communication.Cible.ROLE && request.roles() != null
                        ? request.roles().stream().map(String::toUpperCase).toList() : List.of())
                .familleId(request.cible() == Communication.Cible.FAMILLE ? request.familleId() : null)
                .departmentId(request.cible() == Communication.Cible.DEPARTEMENT ? request.departmentId() : null)
                .createdBy(securityUtils.getCurrentUserId())
                .build();
        Communication saved = communicationRepository.save(c);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishCreated("COMMUNICATION", saved.getId(),
                Map.of("titre", saved.getTitre(), "cible", saved.getCible().name()),
                "Annonce créée: " + saved.getTitre());
        return toMap(saved);
    }

    public Map<String, Object> update(UUID id, CommunicationRequest request) {
        Communication c = communicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Communication", id));
        String oldTitre = c.getTitre();
        c.setTitre(request.titre().trim());
        c.setContenu(request.contenu().trim());
        c.setCible(request.cible());
        c.setRoles(request.cible() == Communication.Cible.ROLE && request.roles() != null
                ? request.roles().stream().map(String::toUpperCase).toList() : List.of());
        c.setFamilleId(request.cible() == Communication.Cible.FAMILLE ? request.familleId() : null);
        c.setDepartmentId(request.cible() == Communication.Cible.DEPARTEMENT ? request.departmentId() : null);
        Communication saved = communicationRepository.save(c);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishUpdated("COMMUNICATION", saved.getId(),
                Map.of("titre", oldTitre),
                Map.of("titre", saved.getTitre()),
                "Annonce mise à jour: " + saved.getTitre());
        return toMap(saved);
    }

    /** Publie l'annonce et diffuse une notification IN_APP aux destinataires. */
    public Map<String, Object> publish(UUID id) {
        Communication c = communicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Communication", id));
        String oldStatut = c.getStatut().name();
        c.setStatut(Communication.Statut.PUBLIEE);
        c.setDatePublication(LocalDateTime.now());
        communicationRepository.save(c);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("COMMUNICATION", id,
                oldStatut, Communication.Statut.PUBLIEE.name(),
                "Annonce publiée: " + c.getTitre());

        Set<UUID> targets = targetUserIds(c);
        int sent = 0;
        for (UUID userId : targets) {
            notificationService.create(userId, TypeNotification.INFORMATION, CanalNotification.IN_APP,
                    "Nouvelle annonce : " + c.getTitre(),
                    c.getContenu().length() > 180 ? c.getContenu().substring(0, 180) + "…" : c.getContenu(),
                    id, "COMMUNICATION");
            sent++;
        }
        Map<String, Object> map = toMap(c);
        map.put("destinataires", sent);
        return map;
    }

    public void delete(UUID id) {
        Communication c = communicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Communication", id));
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishSoftDeleted("COMMUNICATION", id,
                Map.of("titre", c.getTitre(), "statut", c.getStatut().name()),
                "Annonce supprimée: " + c.getTitre());
        c.setDeleted(true);
        communicationRepository.save(c);
    }

    /* ------------------------------ Ciblage ------------------------------ */

    /** Utilisateurs destinataires d'une annonce selon sa cible. */
    private Set<UUID> targetUserIds(Communication c) {
        return switch (c.getCible()) {
            case TOUS -> userRepository.findAll().stream()
                    .filter(u -> u.getStatut() == UserStatus.ACTIVE && !u.isDeleted())
                    .map(User::getId)
                    .collect(Collectors.toSet());
            case ROLE -> {
                Set<UUID> ids = new HashSet<>();
                for (String role : c.getRoles()) {
                    try {
                        ids.addAll(userRepository.findByRolesContaining(UserRole.valueOf(role)).stream()
                                .map(User::getId).toList());
                    } catch (IllegalArgumentException ignored) {
                        // rôle inconnu : ignoré
                    }
                }
                yield ids;
            }
            case FAMILLE -> familyUserIds(c.getFamilleId());
            case DEPARTEMENT -> departmentUserIds(c.getDepartmentId());
        };
    }

    private Set<UUID> familyUserIds(UUID familleId) {
        if (familleId == null) return Set.of();
        Set<UUID> ids = new HashSet<>();
        ids.addAll(userRepository.findByFamilleGereeId(familleId).stream().map(User::getId).toList());
        ids.addAll(soulRepository.findAllByFamilleId(familleId).stream()
                .map(Soul::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return ids;
    }

    private Set<UUID> departmentUserIds(UUID departmentId) {
        if (departmentId == null) return Set.of();
        Set<UUID> ids = new HashSet<>();
        departmentRepository.findById(departmentId)
                .map(Department::getResponsableId).ifPresent(ids::add);
        List<UUID> soulIds = soulDepartmentRepository.findByDepartmentIdAndActifTrue(departmentId).stream()
                .map(sd -> sd.getSoulId()).toList();
        if (!soulIds.isEmpty()) {
            ids.addAll(soulRepository.findAllById(soulIds).stream()
                    .map(Soul::getUserId).filter(Objects::nonNull).collect(Collectors.toSet()));
        }
        return ids;
    }

    /* ------------------------------- Helpers ------------------------------- */

    private Map<String, Object> toMap(Communication c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("titre", c.getTitre());
        map.put("contenu", c.getContenu());
        map.put("cible", c.getCible() != null ? c.getCible().name() : "");
        map.put("roles", c.getRoles());
        map.put("familleId", c.getFamilleId());
        map.put("departmentId", c.getDepartmentId());
        map.put("statut", c.getStatut() != null ? c.getStatut().name() : "");
        map.put("datePublication", c.getDatePublication() != null ? c.getDatePublication().toString() : "");
        map.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
        return map;
    }
}
