package com.discipolat.modules.relations.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.Payloads;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.platform.domain.DictionaryEntry;
import com.discipolat.modules.platform.domain.DictionaryEntryRepository;
import com.discipolat.modules.relations.api.dto.MemberRelationView;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Hiérarchie & relations personnelles (« Mon encadrement ») — V231.
 *
 * <p>Un membre déclare lui-même ses autorités enregistrées (pasteur,
 * supérieur, responsable, mentor, parrain…) selon le paramétrage de son
 * église (dictionnaire {@code MEMBER_RELATION_TYPE}). L'effet est
 * IMMÉDIAT et AUTOMATIQUE : la relation passe ACTIVE, le supérieur reçoit
 * une notification in-app et le membre apparaît dans sa liste
 * « ses membres » ({@link #listMembersOf}).
 *
 * <h2>Invariants garantis par ce service</h2>
 * <ul>
 *   <li><b>Isolation multi-tenant</b> — vérifiée explicitement sur le
 *       déclarant ET le destinataire ({@link #requireTenantUser}), jamais
 *       déléguée au seul filtre Hibernate.</li>
 *   <li><b>Pas d'auto-rattachement</b>, destinataire enregistré et ACTIF.</li>
 *   <li><b>Un seul ACTIVE par (tenant, from, to, type)</b>.</li>
 *   <li><b>Plafonds</b> : {@value #MAX_OUTGOING_PER_MEMBER} déclarations
 *       sortantes, {@value #MAX_INCOMING_PER_MEMBER} entrantes.</li>
 *   <li><b>Paramétrage d'église respecté</b> : un type désactivé par
 *       l'église est <i>rejeté</i> à la déclaration et invisible dans
 *       {@link #availableTypes} — et non simplement masqué.</li>
 *   <li><b>Aucune purge</b> : fin de relation = statut {@code REVOKED}.</li>
 * </ul>
 *
 * <p><b>Volumétrie</b> : toutes les vues sont batchées (résolution des noms
 * en un seul {@code findAllById}, catalogue des types chargé une fois par
 * appel) — aucun N+1, y compris sur « ses membres » qui est paginé.
 */
@Service
@Transactional
public class MemberRelationService {

    private static final Logger log = LoggerFactory.getLogger(MemberRelationService.class);

    /** Clé du dictionnaire de paramétrage par église. */
    public static final String DICT_KEY = "MEMBER_RELATION_TYPE";

    /** Repli code → libellé si le dictionnaire n'a (encore) aucune copie. */
    static final Map<String, String> DEFAULT_TYPES = Map.of(
            "PASTEUR", "Mon pasteur",
            "SUPERIEUR", "Mon supérieur",
            "RESPONSABLE", "Mon responsable",
            "MENTOR", "Mon mentor",
            "PARRAIN", "Mon parrain / père (mère) spirituel(le)");

    /** Garde-fou : max de déclarations SORTANTES (mes encadrants) par membre. */
    public static final int MAX_OUTGOING_PER_MEMBER = 10;

    /** Garde-fou : max de déclarations ENTRANTES (« ses membres ») par encadrant. */
    public static final int MAX_INCOMING_PER_MEMBER = 500;

    /** Pagination « ses membres » : taille par défaut et plafond. */
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 200;

    private static final Sort RECENT_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    /** Rôles autorisés à modérer les rattachements d'autrui. */
    private static final Set<String> MODERATOR_AUTHORITIES = Set.of("ROLE_ADMIN", "ROLE_PASTEUR");

    private final MemberRelationRepository repository;
    private final UserRepository userRepository;
    private final DictionaryEntryRepository dictionaryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;

    public MemberRelationService(MemberRelationRepository repository,
                                 UserRepository userRepository,
                                 DictionaryEntryRepository dictionaryRepository,
                                 NotificationService notificationService,
                                 AuditService auditService,
                                 EntityPropagationPublisher propagationPublisher) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.dictionaryRepository = dictionaryRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
    }

    // ==================== CATALOGUE DES TYPES (paramétrage église) ====================

    /**
     * Un type de rattachement tel que paramétré par une église.
     *
     * @param code   code technique (PASTEUR, MENTOR…)
     * @param label  libellé affiché (dictionnaire de l'église)
     * @param actif  {@code false} si l'église a désactivé ce type
     * @param ordre  rang d'affichage
     */
    public record RelationType(String code, String label, boolean actif, int ordre) {}

    /**
     * Catalogue résolu pour un tenant, avec precedence
     * <b>ligne du tenant &gt; ligne globale (tenant_id IS NULL) &gt; défaut code</b>.
     *
     * <p>Conséquence recherchée : <b>désactiver un type dans une église le
     * rend réellement inutilisable</b> (déclaration rejetée), et non
     * simplement invisible. Les lignes d'un autre tenant sont ignorées.
     */
    @Transactional(readOnly = true)
    public List<RelationType> typeCatalog(UUID tenantId) {
        Map<String, DictionaryEntry> tenantRows = new LinkedHashMap<>();
        Map<String, DictionaryEntry> globalRows = new LinkedHashMap<>();
        for (DictionaryEntry e : dictionaryRepository.findByDictKeyOrderByOrdreAsc(DICT_KEY)) {
            if (e == null || !DICT_KEY.equals(e.getDictKey()) || e.getCode() == null) {
                continue;
            }
            if (e.getTenantId() == null) {
                globalRows.putIfAbsent(e.getCode(), e);
            } else if (tenantId != null && tenantId.equals(e.getTenantId())) {
                tenantRows.put(e.getCode(), e);
            }
        }

        Set<String> codes = new LinkedHashSet<>(DEFAULT_TYPES.keySet());
        codes.addAll(tenantRows.keySet());
        codes.addAll(globalRows.keySet());

        List<RelationType> catalog = new ArrayList<>(codes.size());
        for (String code : codes) {
            DictionaryEntry winner = tenantRows.get(code);
            if (winner == null) {
                winner = globalRows.get(code);
            }
            if (winner != null) {
                catalog.add(new RelationType(code, winner.getLabel(), winner.isActif(),
                        winner.getOrdre()));
            } else {
                // Aucune ligne de dictionnaire nulle part : on retombe sur le
                // libellé technique, toujours actif, en fin de liste.
                catalog.add(new RelationType(code, DEFAULT_TYPES.get(code), true, 999));
            }
        }
        catalog.sort(Comparator.comparingInt(RelationType::ordre).thenComparing(RelationType::code));
        return catalog;
    }

    /**
     * Types <b>actifs</b> — c'est exactement ce que l'interface doit proposer.
     * Un type désactivé par l'église en est absent.
     */
    @Transactional(readOnly = true)
    public Map<String, String> availableTypes(UUID tenantId) {
        Map<String, String> types = new LinkedHashMap<>();
        for (RelationType t : typeCatalog(tenantId)) {
            if (t.actif()) {
                types.put(t.code(), t.label());
            }
        }
        return types;
    }

    /** Libellé d'un type pour le tenant (repli : code humanisé). */
    @Transactional(readOnly = true)
    public String typeLabel(UUID tenantId, String code) {
        if (code == null) {
            return null;
        }
        for (RelationType t : typeCatalog(tenantId)) {
            if (t.code().equals(code)) {
                return t.label();
            }
        }
        return code.replace('_', ' ');
    }

    // ==================== DÉCLARATION ====================

    /**
     * Le membre {@code fromUserId} déclare {@code toUserId} (ou l'email de
     * son compte enregistré) comme autorité {@code relationType}.
     * ACTIVE immédiat + notification in-app chez le destinataire.
     *
     * @param actorId auteur de l'action : {@code fromUserId} en
     *                self-service, ou un modérateur (ADMIN/PASTEUR) qui
     *                déclare pour le compte d'un membre.
     */
    public MemberRelation declare(UUID tenantId, UUID fromUserId, UUID toUserId, String toEmail,
                                  String relationType, String note, UUID actorId) {
        if (relationType == null || relationType.isBlank()) {
            throw new BusinessRuleException("Le type de relation est requis", "RELATION_TYPE_REQUIRED");
        }
        String type = relationType.trim().toUpperCase();
        RelationType catalogued = null;
        for (RelationType t : typeCatalog(tenantId)) {
            if (t.code().equals(type)) {
                catalogued = t;
                break;
            }
        }
        if (catalogued == null) {
            throw new BusinessRuleException(
                    "Type de relation inconnu pour cette église : " + relationType, "RELATION_TYPE_UNKNOWN");
        }
        if (!catalogued.actif()) {
            throw new BusinessRuleException("Le type « " + catalogued.label()
                    + " » est désactivé dans le paramétrage de cette église", "RELATION_TYPE_DISABLED");
        }

        User from = requireTenantUser(tenantId, fromUserId, "Membre déclarant");
        User to = resolveTarget(tenantId, toUserId, toEmail);
        if (from.getId().equals(to.getId())) {
            throw new BusinessRuleException("Un membre ne peut pas se déclarer son propre encadrant",
                    "RELATION_SELF");
        }

        boolean moderator = actorId != null && !actorId.equals(fromUserId);
        if (moderator && !currentActorCanModerate()) {
            throw new BusinessRuleException("Seul un pasteur ou un administrateur peut déclarer "
                    + "un encadrement pour un autre membre", "RELATION_FORBIDDEN");
        }

        Optional<MemberRelation> existing = repository
                .findByTenantIdAndFromUserIdAndToUserIdAndRelationTypeAndStatut(
                        tenantId, from.getId(), to.getId(), type, MemberRelation.RelationStatus.ACTIVE);
        if (existing.isPresent()) {
            throw new BusinessRuleException("Rattachement déjà actif auprès de " + fullName(to),
                    "RELATION_DUPLICATE");
        }
        long outgoing = repository.countByTenantIdAndFromUserIdAndStatut(tenantId, from.getId(),
                MemberRelation.RelationStatus.ACTIVE);
        if (outgoing >= MAX_OUTGOING_PER_MEMBER) {
            throw new BusinessRuleException("Nombre maximum de rattachements atteint ("
                    + MAX_OUTGOING_PER_MEMBER + ")", "RELATION_OUTGOING_QUOTA");
        }
        long incoming = repository.countByTenantIdAndToUserIdAndStatut(tenantId, to.getId(),
                MemberRelation.RelationStatus.ACTIVE);
        if (incoming >= MAX_INCOMING_PER_MEMBER) {
            throw new BusinessRuleException("Cet encadrant atteint la limite de " + MAX_INCOMING_PER_MEMBER
                    + " membres rattachés ; retirez un rattachement ou contactez un administrateur",
                    "RELATION_INCOMING_QUOTA");
        }

        MemberRelation relation = repository.save(MemberRelation.builder()
                .tenantId(tenantId)
                .fromUserId(from.getId())
                .toUserId(to.getId())
                .relationType(type)
                .statut(MemberRelation.RelationStatus.ACTIVE)
                .note(note)
                .declaredBy(actorId != null ? actorId : from.getId())
                .build());

        auditService.log(actorId, tenantId, "RELATION_DECLARED", "MEMBER_RELATION", relation.getId(),
                "SUCCESS", Payloads.of(
                        "fromUserId", from.getId(), "toUserId", to.getId(), "type", type,
                        "moderatedBy", moderator ? actorId : null), null, null, null);
        notifyQuietly(to, TypeNotification.RELATION_DECLAREE, relation, catalogued.label(),
                "Nouveau rattachement : " + catalogued.label(), fullName(from)
                        + " vous a rattaché comme « " + catalogued.label() + " ».");
        publishQuietly("MEMBER_RELATION", relation.getId(),
                Map.of("tenantId", tenantId, "fromUserId", from.getId(), "toUserId", to.getId()),
                "RELATION_DECLARED: " + type);
        return relation;
    }

    // ==================== RÉVOCATION ====================

    /**
     * Met fin à une relation (statut {@code REVOKED}, jamais de purge).
     *
     * <p>Autorisé à : le déclarant, l'encadrant concerné (il peut détacher
     * un membre mal rattaché), ou un modérateur (ADMIN/PASTEUR). Idempotent.
     * L'autre partie est notifiée dans tous les cas.
     */
    public MemberRelation revoke(UUID tenantId, UUID relationId, UUID actorId) {
        MemberRelation relation = repository.findByTenantIdAndId(tenantId, relationId)
                .orElseThrow(() -> new EntityNotFoundException("MemberRelation", relationId));
        if (relation.getStatut() == MemberRelation.RelationStatus.REVOKED) {
            return relation; // idempotent
        }
        boolean isDeclarant = relation.getFromUserId().equals(actorId);
        boolean isEncadrant = relation.getToUserId().equals(actorId);
        if (!isDeclarant && !isEncadrant && !currentActorCanModerate()) {
            throw new BusinessRuleException("Seul le membre concerné, son encadrant, un pasteur "
                    + "ou un administrateur peut retirer ce rattachement", "RELATION_NOT_OWNER");
        }
        relation.setStatut(MemberRelation.RelationStatus.REVOKED);
        relation.setEndedAt(Instant.now());
        relation = repository.save(relation);
        final MemberRelation revoked = relation; // lambda-safe

        auditService.log(actorId, tenantId, "RELATION_REVOQUED", "MEMBER_RELATION", revoked.getId(),
                "SUCCESS", Payloads.of(
                        "fromUserId", revoked.getFromUserId(), "toUserId", revoked.getToUserId(),
                        "type", revoked.getRelationType(),
                        "byDeclarant", isDeclarant, "byEncadrant", isEncadrant), null, null, null);

        // L'autre partie est informée — défensif.
        String label = typeLabel(tenantId, revoked.getRelationType());
        userRepository.findById(revoked.getToUserId()).ifPresent(to -> notifyQuietly(to,
                TypeNotification.RELATION_REVOQUEE, revoked, label,
                "Rattachement retiré : " + label,
                fullNameQuiet(revoked.getFromUserId()) + " a retiré son rattachement « " + label + " »."));
        return revoked;
    }

    // ==================== LECTURES ====================

    /** Rattachements ACTIVE sortants (mes encadrants). */
    @Transactional(readOnly = true)
    public List<MemberRelation> listOutgoing(UUID tenantId, UUID userId) {
        return repository.findByTenantIdAndFromUserIdAndStatut(tenantId, userId,
                MemberRelation.RelationStatus.ACTIVE);
    }

    /** Rattachements ACTIVE entrants (« ses membres » de l'encadrant). */
    @Transactional(readOnly = true)
    public List<MemberRelation> listIncoming(UUID tenantId, UUID userId) {
        return repository.findByTenantIdAndToUserIdAndStatut(tenantId, userId,
                MemberRelation.RelationStatus.ACTIVE);
    }

    /** Vue agrégée {sortantes, entrantes} avec noms résolus (fiche, profil). */
    @Transactional(readOnly = true)
    public Map<String, Object> summary(UUID tenantId, UUID userId) {
        return summary(tenantId, userId, userId);
    }

    /**
     * Vue agrégée en indicating, pour chaque relation, si l'utilisateur
     * courant ({@code viewerId}) peut la retirer — évite d'exposer un bouton
     * qui échouerait en 403.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> summary(UUID tenantId, UUID userId, UUID viewerId) {
        List<MemberRelation> out = listOutgoing(tenantId, userId);
        List<MemberRelation> in = listIncoming(tenantId, userId);
        Map<UUID, String> names = resolveNames(out, in);
        Map<String, String> labels = availableTypes(tenantId);
        boolean moderator = currentActorCanModerate();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sortantes", views(tenantId, out, true, names, labels, viewerId, moderator));
        result.put("entrantes", views(tenantId, in, false, names, labels, viewerId, moderator));
        result.put("typesDisponibles", new ArrayList<>(labels.keySet()));
        return result;
    }

    /**
     * « Ses membres » — paginé : les membres enregistrés qui se sont déclarés
     * sous {@code userId}. La pagination est obligatoire : un pasteur peut
     *cumuler jusqu'à {@value #MAX_INCOMING_PER_MEMBER} rattachements.
     */
    @Transactional(readOnly = true)
    public Page<MemberRelationView> listMembersOf(UUID tenantId, UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size), RECENT_FIRST);
        Page<MemberRelation> found = repository.findByTenantIdAndToUserIdAndStatut(tenantId, userId,
                MemberRelation.RelationStatus.ACTIVE, pageable);
        if (found.isEmpty()) {
            return Page.empty(pageable);
        }
        List<MemberRelationView> content = views(tenantId, found.getContent(), false,
                resolveNames(found.getContent()), availableTypes(tenantId), userId, currentActorCanModerate());
        return new org.springframework.data.domain.PageImpl<>(content, pageable, found.getTotalElements());
    }

    /** Vue d'une relation isolée (après déclaration / révocation). */
    @Transactional(readOnly = true)
    public MemberRelationView viewOf(UUID tenantId, MemberRelation relation, UUID viewerId) {
        Map<UUID, String> names = resolveNames(List.of(relation));
        return views(tenantId, List.of(relation), relation.getFromUserId().equals(viewerId),
                names, availableTypes(tenantId), viewerId, currentActorCanModerate()).get(0);
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private List<MemberRelationView> views(UUID tenantId, List<MemberRelation> relations, boolean outbound,
                                            Map<UUID, String> names, Map<String, String> labels,
                                            UUID viewerId, boolean moderator) {
        List<MemberRelationView> out = new ArrayList<>(relations.size());
        for (MemberRelation r : relations) {
            UUID otherId = outbound ? r.getToUserId() : r.getFromUserId();
            String label = labels.get(r.getRelationType());
            if (label == null) {
                label = typeLabel(tenantId, r.getRelationType());
            }
            out.add(new MemberRelationView(
                    r.getId(),
                    r.getFromUserId(), names.get(r.getFromUserId()),
                    r.getToUserId(), names.get(r.getToUserId()),
                    otherId, names.getOrDefault(otherId, "—"),
                    r.getRelationType(), label,
                    r.getStatut().name(), r.getNote(),
                    r.getCreatedAt() != null ? r.getCreatedAt().toString() : null,
                    r.getEndedAt() != null ? r.getEndedAt().toString() : null,
                    r.getDeclaredBy(),
                    revocableBy(r, viewerId, moderator)));
        }
        return out;
    }

    /** Le membre, son encadrant, ou un modérateur peut retirer la relation. */
    private boolean revocableBy(MemberRelation r, UUID viewerId, boolean moderator) {
        return moderator || viewerId == null
                || r.getFromUserId().equals(viewerId)
                || r.getToUserId().equals(viewerId);
    }

    /**
     * Résolution batch des noms : un seul {@code findAllById} pour toutes les
     * relations de la vue (au lieu d'un SELECT par relation).
     */
    private Map<UUID, String> resolveNames(Collection<MemberRelation>... groups) {
        return resolveNames(java.util.Arrays.stream(groups)
                .flatMap(java.util.Collection::stream).toList());
    }

    private Map<UUID, String> resolveNames(List<MemberRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return Map.of();
        }
        Set<UUID> ids = new LinkedHashSet<>();
        for (MemberRelation r : relations) {
            ids.add(r.getFromUserId());
            ids.add(r.getToUserId());
        }
        Map<UUID, String> names = new HashMap<>();
        for (User u : userRepository.findAllById(ids)) {
            if (u != null && !u.isDeleted()) {
                names.put(u.getId(), fullName(u));
            }
        }
        return names;
    }

    // ==================== AIDES ====================

    /**
     * Charge un utilisateur en vérifiant <b>explicitement</b> le tenant et
     * l'absence de suppression. Le filtre Hibernate ne le garantit pas hors
     * contexte requête — un compte d'une autre église doit être INVISIBLE.
     */
    private User requireTenantUser(UUID tenantId, UUID userId, String label) {
        if (userId == null) {
            throw new BusinessRuleException(label + " manquant", "RELATION_USER_REQUIRED");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(label, userId));
        if (user.isDeleted() || !user.getTenantId().equals(tenantId)) {
            // Isolation stricte : un compte d'une autre église est invisible.
            throw new EntityNotFoundException(label, userId);
        }
        return user;
    }

    private User resolveTarget(UUID tenantId, UUID toUserId, String toEmail) {
        if (toUserId != null) {
            User to = requireTenantUser(tenantId, toUserId, "Encadrant déclaré");
            requireRegistered(to);
            return to;
        }
        if (toEmail != null && !toEmail.isBlank()) {
            User to = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, toEmail.trim())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Aucun membre enregistré avec cet email dans cette église : " + toEmail,
                            "RELATION_TARGET_NOT_REGISTERED"));
            if (to.isDeleted()) {
                throw new BusinessRuleException("Membre enregistré introuvable",
                        "RELATION_TARGET_NOT_REGISTERED");
            }
            requireRegistered(to);
            return to;
        }
        throw new BusinessRuleException("toUserId ou toEmail requis", "RELATION_TARGET_REQUIRED");
    }

    private void requireRegistered(User to) {
        if (to.getStatut() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("L'encadrant déclaré doit avoir un compte actif "
                    + "(invitation en attente ?)", "RELATION_TARGET_INACTIVE");
        }
    }

    private String fullName(User u) {
        String n = ((u.getFirstName() == null ? "" : u.getFirstName() + " ")
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }

    private String fullNameQuiet(UUID userId) {
        return userRepository.findById(userId).map(this::fullName).orElse("—");
    }

    /** ADMIN / PASTEUR — pour modérer les rattachements d'autrui. */
    private static boolean currentActorCanModerate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (MODERATOR_AUTHORITIES.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /** La notification ne doit JAMAIS faire échouer l'opération métier. */
    private void notifyQuietly(User to, TypeNotification type, MemberRelation relation, String typeLabel,
                               String title, String message) {
        try {
            notificationService.create(relation.getTenantId(), to.getId(), type, CanalNotification.IN_APP,
                    title, message, relation.getId(), "MEMBER_RELATION");
        } catch (Exception e) {
            log.warn("Notification de rattachement ({}) échouée pour {} : {}",
                    type, to.getId(), e.getMessage());
        }
    }

    private void publishQuietly(String entityType, UUID entityId, Map<String, Object> payload, String label) {
        try {
            propagationPublisher.publishCreated(entityType, entityId, payload, label);
        } catch (Exception e) {
            log.debug("Propagation {},{} ignorée : {}", entityType, entityId, e.getMessage());
        }
    }
}
