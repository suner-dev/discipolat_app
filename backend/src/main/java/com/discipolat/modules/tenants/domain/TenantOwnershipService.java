package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.Payloads;
import com.discipolat.common.exception.DomainException;
import com.discipolat.common.multitenancy.CrossTenantScopeAccess;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (D6) — propriété de l'église « façon WhatsApp » :
 * le fondateur est {@code TENANT_OWNER} (« le roi »), peut déléguer des
 * {@code TENANT_ADMIN}, transférer la barre à un membre existant, et un admin
 * peut demander un remplacement assisté si l'owner a disparu (arbitrage
 * plateforme, jamais automatique).
 */
@Service
public class TenantOwnershipService {

    private static final Logger log = LoggerFactory.getLogger(TenantOwnershipService.class);
    private static final String OWNER_KEY = "TENANT_OWNER";
    private static final String ADMIN_KEY = "TENANT_ADMIN";
    private static final String MEMBER_KEY = "MEMBRE";

    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final CrossTenantScopeAccess crossTenant;
    private final AuditService auditService;
    private final EmailService emailService;

    public TenantOwnershipService(TenantMembershipRepository membershipRepository,
                                  RoleRepository roleRepository,
                                  UserRepository userRepository,
                                  TenantRepository tenantRepository,
                                  CrossTenantScopeAccess crossTenant,
                                  AuditService auditService,
                                  EmailService emailService) {
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.crossTenant = crossTenant;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    public record OwnershipView(UUID ownerUserId, String ownerEmail, String ownerName,
                                List<Map<String, Object>> admins) {
    }

    /** Lecture sous filtre suspendu : le tenant courant est bien le bon, mais
     *  les memberships portent la FK role en lazy — on stabilise le graphe. */
    @Transactional(readOnly = true)
    public OwnershipView overview(UUID tenantId) {
        return crossTenant.call(() -> {
            TenantMembership owner = findOwnerMembership(tenantId);
            User ownerUser = owner == null ? null
                    : userRepository.findById(owner.getUserId()).orElse(null);
            List<TenantMembership> admins = membershipRepository
                    .findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
                    // `roleKeyOf`, et non `getRoleLegacy()` : `RoleManagement
                    // Service.assignRoleToUser` fait `setRole(role)` SANS
                    // renseigner `role_legacy`. Un administrateur promu par
                    // l'écran des rôles avait donc une colonne legacy nulle et
                    // disparaissait de la liste des délégués — alors qu'il l'est
                    // bel et bien (c'est la FK `role` qui fait foi). Les deux
                    // colonnes sont lues partout ailleurs dans cette classe ;
                    // ici, l'écart était le dernier.
                    .filter(m -> ADMIN_KEY.equals(roleKeyOf(m)))
                    .toList();
            List<Map<String, Object>> adminViews = admins.stream()
                    .map(m -> {
                        User u = userRepository.findById(m.getUserId()).orElse(null);
                        return u == null ? Map.<String, Object>of() : Map.<String, Object>of(
                                "userId", u.getId(),
                                "email", u.getEmail(),
                                "name", (u.getFirstName() == null ? "" : u.getFirstName() + " ")
                                        + (u.getLastName() == null ? "" : u.getLastName()),
                                "joinedAt", m.getJoinedAt());
                    })
                    .filter(m -> !m.isEmpty())
                    .toList();
            return new OwnershipView(
                    ownerUser == null ? null : ownerUser.getId(),
                    ownerUser == null ? null : ownerUser.getEmail(),
                    ownerUser == null ? null
                            : (ownerUser.getFirstName() + " " + ownerUser.getLastName()),
                    adminViews);
        });
    }

    /**
     * Membres <b>promouvables</b> : ceux qui ne sont ni le propriétaire, ni
     * déjà administrateur délégué.
     *
     * <p><b>Pourquoi cette méthode existe (T-W7).</b> L'écran « Propriété &
     * délégation » doit permettre de nommer quelqu'un qui n'est pas encore
     * administrateur. L'API n'exposait que la liste des administrateurs
     * <i>existants</i> : le sélecteur était donc vide, et la délégation — la
     * demande explicite du client — était inatteignable depuis l'interface.
     * Le serveur sait faire, l'écran ne pouvait pas.
     *
     * <p><b>D7 respecté par construction</b> : seuls les membres du tenant
     * <i>courant</i> sont retournés. Aucun accès inter-organisation n'est
     * ouvert, et la lecture passe par le filtre suspendu
     * ({@link #overview}) sans élargir la requête.
     *
     * <p>Le propriétaire et les délégués existants sont exclus : les
     * nommer serait sans effet, et l'interface doit rendre cette impossibilité
     * visible plutôt que proposer une action qui ne changerait rien.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> promotableMembers(UUID tenantId) {
        return crossTenant.call(() -> {
            TenantMembership owner = findOwnerMembership(tenantId);
            UUID ownerId = owner == null ? null : owner.getUserId();

            return membershipRepository
                    .findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
                    .filter(m -> ownerId == null || !ownerId.equals(m.getUserId()))
                    .filter(m -> !ADMIN_KEY.equals(roleKeyOf(m)))
                    .map(m -> {
                        User u = userRepository.findById(m.getUserId()).orElse(null);
                        if (u == null) {
                            // Membre sans compte lisible : on l'omet plutôt que
                            // d'inventer une ligne illisible dans le sélecteur.
                            return null;
                        }
                        String first = u.getFirstName() == null ? "" : u.getFirstName().trim();
                        String last = u.getLastName() == null ? "" : u.getLastName().trim();
                        String fullName = (first + " " + last).trim();
                        Map<String, Object> view = new java.util.LinkedHashMap<>();
                        view.put("userId", u.getId());
                        view.put("email", u.getEmail());
                        // Nom si connu, email sinon : l'admin doit pouvoir
                        // identifier la personne sans avoir à deviner.
                        view.put("name", fullName.isEmpty() ? u.getEmail() : fullName);
                        view.put("joinedAt", m.getJoinedAt());
                        return view;
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        });
    }

    /**
     * Transfert de propriété : réservé au {@code TENANT_OWNER} courant ; la
     * cible doit déjà être membre ACTIVE de l'église (sinon 404 — on n'ajoute
     * pas un inconnu au passage). L'ancien owner rétrograde {@code TENANT_ADMIN}
     * (jamais simple membre : il garde la main pour la récupération).
     */
    @Transactional
    public OwnershipView transfer(UUID tenantId, UUID actorUserId, UUID toUserId) {
        return crossTenant.call(() -> {
            TenantMembership currentOwner = requireOwnerMembership(tenantId, actorUserId);
            // F17 : lecture par liste, pas par Optional (plusieurs périmètres
            // possibles pour la même organisation).
            TenantMembership target = membershipRepository
                    .findAllByUserIdAndTenantIdAndStatus(toUserId, tenantId, MembershipStatus.ACTIVE)
                    .stream()
                    .min(Comparator.comparing(
                            (TenantMembership m) -> m.getScopeType() == MembershipScopeType.TENANT ? 0 : 1)
                            .thenComparing(m -> m.getJoinedAt() == null ? Instant.EPOCH : m.getJoinedAt()))
                    .orElseThrow(() -> new DomainException(
                            "La cible n'est pas membre de cette église",
                            HttpStatus.NOT_FOUND, "TRANSFER_TARGET_NOT_MEMBER"));
            if (target.getUserId().equals(actorUserId)) {
                throw new DomainException("Vous êtes déjà propriétaire",
                        HttpStatus.BAD_REQUEST, "ALREADY_OWNER");
            }
            Role ownerRole = globalRole(OWNER_KEY);
            Role adminRole = globalRole(ADMIN_KEY);

            target.setRole(ownerRole);
            target.setRoleLegacy(OWNER_KEY);
            membershipRepository.save(target);

            currentOwner.setRole(adminRole);
            currentOwner.setRoleLegacy(ADMIN_KEY);
            membershipRepository.save(currentOwner);

            auditService.logSimple("TENANT_OWNERSHIP_TRANSFERRED", "TENANT", tenantId);
            sendNoticeQuietly(tenantId, target.getUserId(), currentOwner.getUserId(), "transférée");
            return overview(tenantId);
        });
    }

    /** Délégation WhatsApp : un owner promeut un membre TENANT_ADMIN. */
    @Transactional
    public void promoteAdmin(UUID tenantId, UUID actorUserId, UUID targetUserId) {
        requireOwnerMembership(tenantId, actorUserId);
        crossTenant.call(() -> {
            TenantMembership target = primaryMembershipOf(targetUserId, tenantId);
            // `roleKeyOf` : une appartenance dont `role_legacy` est nul (créée
            // par `RoleManagementService.assignRoleToUser`) mais dont la FK
            // `role` porte TENANT_OWNER doit être reconnue comme propriétaire,
            // sinon on pourrait nommer administrateur l'actuel propriétaire.
            if (OWNER_KEY.equals(roleKeyOf(target))) {
                throw new DomainException("Cible déjà propriétaire",
                        HttpStatus.BAD_REQUEST, "ALREADY_OWNER");
            }
            Role adminRole = globalRole(ADMIN_KEY);
            target.setRole(adminRole);
            target.setRoleLegacy(ADMIN_KEY);
            membershipRepository.save(target);
            auditService.logSimple("TENANT_ADMIN_PROMOTED", "USER", targetUserId);
            return null;
        });
    }

    /** Révocation de délégation : TENANT_ADMIN → MEMBRE (l'owner uniquement). */
    @Transactional
    public void demoteAdmin(UUID tenantId, UUID actorUserId, UUID targetUserId) {
        requireOwnerMembership(tenantId, actorUserId);
        crossTenant.call(() -> {
            TenantMembership target = primaryMembershipOf(targetUserId, tenantId);
            if (!ADMIN_KEY.equals(roleKeyOf(target))) {
                throw new DomainException("Cible n'est pas administrateur délégué",
                        HttpStatus.BAD_REQUEST, "NOT_A_DELEGATED_ADMIN");
            }
            Role memberRole = roleRepository.findByTenantIdAndKey(tenantId, MEMBER_KEY)
                    .or(() -> roleRepository.findGlobalByKey(MEMBER_KEY))
                    .orElseThrow(() -> new DomainException("Rôle membre indisponible",
                            HttpStatus.CONFLICT, "MEMBER_ROLE_MISSING"));
            target.setRole(memberRole);
            target.setRoleLegacy(memberRole.getKey());
            membershipRepository.save(target);
            auditService.logSimple("TENANT_ADMIN_DEMOTED", "USER", targetUserId);
            return null;
        });
    }

    /**
     * Demande de remplacement quand l'owner est injoignable (D6) : posée par un
     * {@code TENANT_ADMIN}, JAMAIS automatique — la réponse suggère le membre
     * le plus ancien pour l'arbitrage de la plateforme, l'audit garde la trace.
     */
    @Transactional
    public Map<String, Object> requestReplacement(UUID tenantId, UUID actorUserId, String reason) {
        TenantMembership actor = primaryMembershipOf(actorUserId, tenantId);
        if (!ADMIN_KEY.equals(roleKeyOf(actor))) {
            throw new DomainException("Seul un administrateur délégué peut demander un remplacement",
                    HttpStatus.FORBIDDEN, "REPLACEMENT_ADMIN_ONLY");
        }
        String suggested = crossTenant.call(() -> membershipRepository
                .findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
                .filter(m -> ADMIN_KEY.equals(roleKeyOf(m)) && !m.getUserId().equals(actorUserId))
                .min(Comparator.comparing(TenantMembership::getJoinedAt))
                .map(m -> m.getUserId().toString())
                .orElse(null));
        auditService.log(actorUserId, tenantId, "OWNERSHIP_REPLACEMENT_REQUESTED", "TENANT", tenantId,
                "SUCCESS", Payloads.of("reason", reason,
                        "suggestedCandidate", suggested), null, null, null);
        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("status", "REQUESTED");
        response.put("message", "Demande enregistrée — la plateforme (Super Admin) arbitrera.");
        response.put("suggestedCandidate", suggested);
        return response;
    }

    // ======================== INTERNAL ========================

    private TenantMembership findOwnerMembership(UUID tenantId) {
        return membershipRepository.findByTenantIdAndStatusAndRoleContaining(
                        tenantId, MembershipStatus.ACTIVE, OWNER_KEY).stream()
                .filter(m -> OWNER_KEY.equals(roleKeyOf(m)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Appartenance ACTIVE du propriétaire pour ce tenant.
     *
     * <p><b>F17.</b> Lecture par LISTE, jamais par
     * {@code findByUserIdAndTenantIdAndStatus(...)} : cette méthode renvoie un
     * {@code Optional} et lèverait
     * {@code IncorrectResultSizeDataAccessException} si l'utilisateur
     * possède plusieurs lignes ACTIVE dans la même organisation (périmètres
     * différents). Le tri place la portée {@code TENANT} — la seule qui porte
     * legitimately la propriété — en premier.
     */
    private TenantMembership requireOwnerMembership(UUID tenantId, UUID userId) {
        TenantMembership owner = crossTenant.call(() ->
                membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                                userId, tenantId, MembershipStatus.ACTIVE).stream()
                        .filter(m -> OWNER_KEY.equals(roleKeyOf(m)))
                        .min(Comparator.comparing(
                                (TenantMembership m) -> m.getScopeType() == MembershipScopeType.TENANT ? 0 : 1)
                                .thenComparing(m -> m.getJoinedAt() == null ? Instant.EPOCH : m.getJoinedAt()))
                        .orElse(null));
        if (owner == null) {
            throw new DomainException("Action réservée au propriétaire de l'église",
                    HttpStatus.FORBIDDEN, "OWNER_REQUIRED");
        }
        return owner;
    }

    /**
     * Appartenance « principale » d'un membre dans une organisation.
     *
     * <p><b>F17.</b> Un membre peut legitimately avoir plusieurs lignes ACTIVE
     * dans la même organisation selon les périmètres (spéc §1.3, mode LÉGER :
     * racine + campus). Une recherche renvoyant un {@code Optional} sur
     * {@code (user_id, tenant_id, status)} lèverait alors
     * {@code IncorrectResultSizeDataAccessException} — un 500 sur une simple
     * délégation. On lit donc la liste et on retient la portée {@code TENANT}
     * (le périmètre qui porte les droits d'organisation), puis la plus
     * ancienne à défaut.
     */
    private TenantMembership primaryMembershipOf(UUID userId, UUID tenantId) {
        return membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                        userId, tenantId, MembershipStatus.ACTIVE).stream()
                .min(Comparator.comparing(
                                (TenantMembership m) -> m.getScopeType() == MembershipScopeType.TENANT ? 0 : 1)
                        .thenComparing(m -> m.getJoinedAt() == null ? Instant.EPOCH : m.getJoinedAt()))
                .orElseThrow(() -> new DomainException("Cible non membre",
                        HttpStatus.NOT_FOUND, "TARGET_NOT_MEMBER"));
    }

    /** Clé de rôle normalisée : FK moderne si présente, sinon colonne legacy. */
    private static String roleKeyOf(TenantMembership membership) {
        String key = membership.getRole() != null ? membership.getRole().getKey() : null;
        return (key != null ? key : membership.getRoleLegacy()) != null
                ? (key != null ? key : membership.getRoleLegacy()).trim().toUpperCase(Locale.ROOT)
                : null;
    }

    private Role globalRole(String key) {
        return roleRepository.findGlobalByKey(key)
                .orElseThrow(() -> new DomainException("Rôle global indisponible : " + key,
                        HttpStatus.CONFLICT, "ROLE_MISSING"));
    }

    private void sendNoticeQuietly(UUID tenantId, UUID newOwnerUserId, UUID oldOwnerUserId, String verb) {
        try {
            String tenantName = tenantRepository.findById(tenantId).map(Tenant::getName).orElse("votre église");
            for (UUID uid : List.of(newOwnerUserId, oldOwnerUserId)) {
                userRepository.findById(uid).ifPresent(u -> {
                    try {
                        emailService.send(u.getEmail(),
                                "Propriété de " + tenantName + " " + verb,
                                "Bonjour " + (u.getFirstName() == null ? "" : u.getFirstName()) + ",\n\n"
                                        + "La propriété de l'église « " + tenantName + " » a été " + verb
                                        + " sur Discipolat. Si vous n'êtes pas à l'origine de ce changement, "
                                        + "contactez immédiatement la plateforme.\n\nL'équipe Discipolat");
                    } catch (RuntimeException ignored) {
                        log.debug("Email de transfert non envoyé à {}", u.getEmail());
                    }
                });
            }
        } catch (RuntimeException failure) {
            log.warn("Notification de transfert impossible (tenant {}) : {}", tenantId, failure.getMessage());
        }
    }
}
