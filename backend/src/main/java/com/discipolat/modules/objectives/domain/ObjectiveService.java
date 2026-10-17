package com.discipolat.modules.objectives.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.Payloads;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.evangelism.domain.EvangelismEtape;
import com.discipolat.modules.evangelism.domain.EvangelismTrackRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.interactions.domain.InteractionRepository;
import com.discipolat.modules.interactions.domain.InteractionType;
import com.discipolat.modules.members.domain.MemberDepartment;
import com.discipolat.modules.members.domain.MemberDepartmentRepository;
import com.discipolat.modules.members.domain.MemberPresence;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.objectives.api.CreateObjectiveRequest;
import com.discipolat.modules.objectives.api.ObjectiveProgressResponse;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Système d'objectifs : chaque rôle (pasteur, responsable, chef de famille,
 * faiseur) définit des objectifs chiffrés (visites, nouvelles âmes, disciples
 * actifs, évangélisation, suivis, présence). L'application mesure automatiquement
 * la progression, scopée sur le périmètre réel de l'utilisateur (toutes les âmes
 * pour le pasteur, ses départements pour le responsable, ses familles pour le
 * chef, ses disciples pour le faiseur).
 */
@Service
@Transactional
public class ObjectiveService {

    private final ObjectiveRepository objectiveRepository;
    private final SoulRepository soulRepository;
    private final InteractionRepository interactionRepository;
    private final EvangelismTrackRepository evangelismTrackRepository;
    private final MemberPresenceRepository memberPresenceRepository;
    private final MemberDepartmentRepository memberDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final EntityPropagationPublisher propagationPublisher;
    private final RoleRepository roleRepository;

    public ObjectiveService(ObjectiveRepository objectiveRepository,
                            SoulRepository soulRepository,
                            InteractionRepository interactionRepository,
                            EvangelismTrackRepository evangelismTrackRepository,
                            MemberPresenceRepository memberPresenceRepository,
                            MemberDepartmentRepository memberDepartmentRepository,
                            DepartmentRepository departmentRepository,
                            FamilyRepository familyRepository,
                            UserRepository userRepository,
                            SecurityUtils securityUtils,
                            EntityPropagationPublisher propagationPublisher,
                            RoleRepository roleRepository) {
        this.objectiveRepository = objectiveRepository;
        this.soulRepository = soulRepository;
        this.interactionRepository = interactionRepository;
        this.evangelismTrackRepository = evangelismTrackRepository;
        this.memberPresenceRepository = memberPresenceRepository;
        this.memberDepartmentRepository = memberDepartmentRepository;
        this.departmentRepository = departmentRepository;
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.propagationPublisher = propagationPublisher;
        this.roleRepository = roleRepository;
    }

    // ============================================================
    // Administration des objectifs
    // ============================================================

    @Transactional(readOnly = true)
    public List<Objective> findAll() {
        return objectiveRepository.findByActifTrueOrderByRoleAscTypeAsc();
    }

    public Objective create(CreateObjectiveRequest request) {
        // V263 — P7a : la cible « rôle » accepte désormais les clés du
        // catalogue de rôles servi du tenant, pas seulement les noms de
        // l'enum socle UserRole (union additive, jamais de rétrécissement).
        String roleKey = requireKnownRoleKey(request.role());
        Objective objective = Objective.builder()
                .role(roleKey)
                .type(request.type())
                .cible(request.cible())
                .periode(request.periode())
                .actif(true)
                .creePar(securityUtils.getCurrentUserId())
                .build();
        Objective saved = objectiveRepository.save(objective);
        propagationPublisher.publishCreated("OBJECTIVE", saved.getId(),
                Map.of("role", String.valueOf(saved.getRole()), "type", String.valueOf(saved.getType()),
                       "cible", String.valueOf(saved.getCible())),
                "Objectif créé: " + saved.getType() + " pour " + saved.getRole());
        return saved;
    }

    public Objective toggle(UUID id) {
        Objective objective = objectiveRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Objective", id));
        boolean oldActif = objective.isActif();
        objective.setActif(!oldActif);
        Objective saved = objectiveRepository.save(objective);
        propagationPublisher.publishStatusChanged("OBJECTIVE", id,
                oldActif ? "ACTIF" : "INACTIF", saved.isActif() ? "ACTIF" : "INACTIF",
                "Objectif " + (saved.isActif() ? "activé" : "désactivé") + ": " + saved.getType());
        return saved;
    }

    public void delete(UUID id) {
        propagationPublisher.publishDeleted("OBJECTIVE", id, Payloads.of(), "Objectif supprimé");
        objectiveRepository.deleteById(id);
    }

    // ============================================================
    // Progression automatique
    // ============================================================

    /**
     * Progression de tous les objectifs actifs, mesurée sur le périmètre du
     * rôle actif de l'utilisateur connecté.
     */
    @Transactional(readOnly = true)
    public List<ObjectiveProgressResponse> myProgress() {
        String role = activeRole();
        if (role == null || role.isEmpty()) return List.of();
        List<Objective> objectives = objectiveRepository.findByRoleAndActifTrue(role);
        if (objectives.isEmpty()) return List.of();

        UUID userId = securityUtils.getCurrentUserId();
        Scope scope = resolveScope(userId, role);

        List<ObjectiveProgressResponse> results = new ArrayList<>();
        for (Objective o : objectives) {
            results.add(ObjectiveProgressResponse.of(o, measure(o, scope)));
        }
        return results;
    }

    /** Mesure la valeur réalisée d'un objectif sur le périmètre donné. */
    private double measure(Objective o, Scope scope) {
        LocalDate[] period = periodOf(o.getPeriode());
        LocalDate from = period[0], to = period[1];
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay();

        return switch (o.getType()) {
            case VISITES -> countInteractions(InteractionType.VISITE, fromTs, toTs, scope);
            case SUIVIS -> countInteractions(InteractionType.SUIVI, fromTs, toTs, scope);
            case NOUVELLES_AMES -> scope.soulIds == null
                    ? soulRepository.countByDateIntegrationBetween(from, to)
                    : soulRepository.findAllById(scope.soulIds).stream()
                            .filter(s -> !s.isDeleted() && s.getDateIntegration() != null
                                    && !s.getDateIntegration().isBefore(from) && !s.getDateIntegration().isAfter(to))
                            .count();
            case DISCIPLES_ACTIFS -> scope.soulIds == null
                    ? soulRepository.countByStatut(StatutAme.ACTIF)
                    : soulRepository.findAllById(scope.soulIds).stream()
                            .filter(s -> !s.isDeleted() && s.getStatut() == StatutAme.ACTIF)
                            .count();
            case EVANGELISATION -> scope.soulIds == null
                    ? evangelismTrackRepository.countByEtapeAndDateEtapeBetween(EvangelismEtape.BAPTEME, from, to)
                    : evangelismTrackRepository.countByEtapeAndSoulIdInAndDateEtapeBetween(
                            EvangelismEtape.BAPTEME, scope.soulIds, from, to);
            case PRESENCE -> measurePresence(scope);
        };
    }

    /** Taux de présence moyen sur les 4 dernières semaines du périmètre. */
    private double measurePresence(Scope scope) {
        List<MemberPresence> presences = scope.soulIds == null
                ? memberPresenceRepository.findAllByOrderBySemaineDesc()
                : memberPresenceRepository.findBySoulIdInOrderBySemaineDesc(scope.soulIds);
        if (presences.isEmpty()) return 0;
        LocalDate limite = LocalDate.now().minusWeeks(4);
        long recentes = presences.stream()
                .filter(p -> p.getSemaine() != null && !p.getSemaine().isBefore(limite))
                .count();
        if (recentes == 0) return 0;
        long avecPresence = presences.stream()
                .filter(p -> p.getSemaine() != null && !p.getSemaine().isBefore(limite))
                .filter(p -> p.getPresences() != null && p.getPresences().values().stream().anyMatch(Boolean::booleanValue))
                .count();
        return Math.round((avecPresence * 100.0) / recentes * 10.0) / 10.0;
    }

    private long countInteractions(InteractionType type, LocalDateTime fromTs, LocalDateTime toTs, Scope scope) {
        return scope.soulIds == null
                ? interactionRepository.countByTypeAndDateInteractionBetween(type, fromTs, toTs)
                : interactionRepository.countByTypeAndSoulIdInAndDateInteractionBetween(type, scope.soulIds, fromTs, toTs);
    }

    // ============================================================
    // Périmètre scopé par rôle
    // ============================================================

    private record Scope(List<UUID> soulIds) {}

    /** Rôle actif (multi-rôles supportés) en clé de rôle — socle ou custom servi. */
    private String activeRole() {
        String raw = securityUtils.getCurrentUserRole();
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * V263 — P7a : normalise et valide la clé de rôle d'un objectif contre
     * l'union {noms de l'enum socle UserRole} ∪ {catalogue de rôles du tenant
     * servi} (rôles globaux inclus — même pont de clés que
     * {@code UserController#syncTenantMembershipRole}, V173). Clé inconnue →
     * 4xx métier, jamais de crash {@code Enum.valueOf} sur un rôle custom.
     */
    private String requireKnownRoleKey(String rawRole) {
        String key = rawRole == null ? "" : rawRole.trim().toUpperCase(Locale.ROOT);
        if (!key.matches("[A-Z][A-Z0-9_]{0,49}")) {
            throw new BusinessRuleException("Clé de rôle d'objectif invalide: " + rawRole,
                    "INVALID_OBJECTIVE_ROLE");
        }
        if (parseLegacyRole(key) != null) {
            return key;
        }
        UUID tenantId = TenantContext.getTenantId();
        boolean known = (tenantId != null && roleRepository.findByTenantIdAndKey(tenantId, key).isPresent())
                || roleRepository.findByTenantIdIsNullAndKey(key).isPresent();
        if (!known) {
            throw new BusinessRuleException(
                    "Rôle inconnu pour le ciblage d'objectif (ni rôle socle, ni catalogue du tenant): " + key,
                    "UNKNOWN_OBJECTIVE_ROLE");
        }
        return key;
    }

    /** Rôle socle historique depuis une clé de rôle ; null si clé custom servie. */
    private static UserRole parseLegacyRole(String roleKey) {
        try {
            return UserRole.valueOf(roleKey);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Scope resolveScope(UUID userId, String roleKey) {
        UserRole role = parseLegacyRole(roleKey);
        // Pasteur / admin : toutes les âmes
        if (role == UserRole.PASTEUR || role == UserRole.ADMIN) {
            return new Scope(null);
        }
        Set<UUID> soulIds = new LinkedHashSet<>();

        if (role == UserRole.RESPONSABLE) {
            List<UUID> deptIds = departmentRepository.findByResponsableId(userId)
                    .stream().map(Department::getId).toList();
            if (!deptIds.isEmpty()) {
                memberDepartmentRepository.findByDepartmentIdIn(deptIds)
                        .forEach(md -> soulIds.add(md.getSoulId()));
            }
        } else if (role == UserRole.CHEF_DE_FAMILLE) {
            List<UUID> famIds = familyRepository.findByChefFamilleId(userId)
                    .stream().map(Family::getId).toList();
            if (!famIds.isEmpty()) {
                soulRepository.findByFamilleIdIn(famIds).stream()
                        .filter(s -> !s.isDeleted())
                        .forEach(s -> soulIds.add(s.getId()));
            }
        } else if (role == UserRole.FAISEUR) {
            soulRepository.findAllByFaiseurId(userId).stream()
                    .filter(s -> !s.isDeleted())
                    .forEach(s -> soulIds.add(s.getId()));
        }
        // Clé custom servie (hors rôles socle) : pas de sémantique de périmètre
        // historique connue — périmètre vide (mesure à 0), plus de crash.
        return new Scope(List.copyOf(soulIds));
    }

    /** Borne de la période de mesure. */
    private LocalDate[] periodOf(Objective.Periode periode) {
        LocalDate now = LocalDate.now();
        return switch (periode) {
            case MENSUEL -> new LocalDate[]{now.withDayOfMonth(1), now};
            case TRIMESTRIEL -> new LocalDate[]{now.minusMonths(3).withDayOfMonth(1), now};
            case ANNUEL -> new LocalDate[]{now.withDayOfYear(1), now};
        };
    }
}
