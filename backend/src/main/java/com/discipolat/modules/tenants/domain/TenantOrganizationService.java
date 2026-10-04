package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.Payloads;
import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Création et organisation des <b>réseaux</b> d'organisations.
 * SPEC_ORGANISATION_DENOMINATION_V2 §7.1 / T-B3.
 *
 * <p>Ce service matérialise le modèle hybride de la §1.3 :
 * <ul>
 *   <li>une <b>dénomination</b> est une organisation <i>conteneur</i> : elle
 *       fédère des églises, son propriétaire est « le roi » qui délègue et
 *       produit un code par église fille ;</li>
 *   <li>une <b>église enfant</b> est soit un nœud {@code OrganizationNode}
 *       (mode LÉGER — mêmes données, simple re-périmètre), soit un
 *       <b>tenant distinct</b> (mode AUTONOME — son stock de membres, ses
 *       quotas, sa facturation).</li>
 * </ul>
 *
 * <p><b>Décisions de conception à respecter</b> :
 * <ul>
 *   <li>Toute création passe par {@link TenantService#create} — on ne duplique
 *       pas la logique de provisionnement (abonnement, modules, dictionnaires,
 *       nœud racine, membership propriétaire), sinon les deux chemins
 *       divergeraient sur le premier correctif ;</li>
 *   <li>le {@code rootTenantId} est hérité du parent : c'est lui qui rend le
 *       transfert de membre détectable (§4.4) ;</li>
 *   <li>aucune suppression : une organisation avec des enfants est protégée
 *       en base ({@code ON DELETE RESTRICT}) et le statut {@code CANCELLED}
 *       reste la seule voie d'arrêt (spéc §8.6).</li>
 * </ul>
 */
@Service
public class TenantOrganizationService {

    private static final String AUDIT_CREATED = "TENANT_ORG_CREATED";
    private static final String AUDIT_CHILD_CREATED = "TENANT_ORG_CHILD_CREATED";

    private final TenantRepository tenantRepository;
    private final TenantService tenantService;
    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final OrganizationNodeService organizationNodeService;
    private final AuditService auditService;

    private static final String OWNER_ROLE_KEY = "TENANT_OWNER";

    public TenantOrganizationService(TenantRepository tenantRepository,
                                     TenantService tenantService,
                                     TenantMembershipRepository membershipRepository,
                                     RoleRepository roleRepository,
                                     OrganizationNodeService organizationNodeService,
                                     AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.tenantService = tenantService;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.organizationNodeService = organizationNodeService;
        this.auditService = auditService;
    }

    /** Vue d'une organisation dans le réseau — agrégats uniquement (D7). */
    public record OrganizationView(UUID id, String name, String slug, String kind,
                                   UUID parentTenantId, UUID rootTenantId,
                                   String status, String plan, int childCount) {
    }

    // ============================================================
    // CRÉATION
    // ============================================================

    /**
     * Crée une organisation <b>racine</b> — une dénomination, une association,
     * une méga-association.
     *
     * <p>Son propriétaire est le « roi » : il gouverne ses enfants, délègue
     * des administrateurs, produit un code ou un lien par église fille, et
     * n'en voit que les <b>agrégats</b> (jamais les membres nominatifs, D7).
     *
     * @param name         nom affiché
     * @param kind         nature de l'organisation (D2)
     * @param creatorUserId compte du propriétaire ; sa participation à la
     *                     nouvelle organisation est créée dans la MÊME
     *                     transaction (atomicité : pas de racine sans roi)
     * @param plan         plan initial ; {@code null} = plan par défaut
     */
    @Transactional
    public Tenant createDenomination(String name, UUID creatorUserId, TenantKind kind, String plan) {
        String cleanName = requireName(name);
        TenantKind effectiveKind = kind == null ? TenantKind.DENOMINATION : kind;
        if (!effectiveKind.canHaveChildren()) {
            throw new DomainException(
                    "Une église ne peut pas fédérer d'autres églises — créez une dénomination",
                    HttpStatus.BAD_REQUEST, "KIND_CANNOT_HAVE_CHILDREN");
        }

        com.discipolat.modules.tenants.api.TenantResponse rootDto =
                tenantService.create(new com.discipolat.modules.tenants.api.CreateTenantRequest(
                cleanName, uniqueSlug(cleanName, null), plan, null, null, null, null, null, null, null));

        Tenant root = requireEntity(rootDto);
        // compare des valeurs non nulles, donc ce champ doit l'être.
        root.setKind(effectiveKind);
        root.setParentTenantId(null);
        root.ensureRootTenantId();
        Tenant saved = tenantRepository.save(root);

        // Nœud racine + propriété : sans cela la dénomination n'a aucune
        // structure d'organisation exploitable par l'IHM.
        attachRootChurchNode(saved, creatorUserId);
        grantOwnership(saved, creatorUserId);

        auditService.log(creatorUserId, saved.getId(), AUDIT_CREATED, "TENANT", saved.getId(),
                "SUCCESS",
                Payloads.of("name", saved.getName(), "slug", saved.getSlug(),
                        "kind", effectiveKind.name(), "rootTenantId", saved.effectiveRootTenantId()),
                null, null, null);
        return saved;
    }

    /**
     * Crée une <b>église enfant autonome</b> : un vrai tenant distinct, avec son
     * propre stock de membres, ses quotas et sa facturation (mode AUTONOME,
     * §1.3).
     *
     * <p>Le {@code rootTenantId} est <b>hérité du parent</b> : c'est ce qui
     * rendra le transfert de membre détectable (§4.4) — un membre qui passe
     * d'une église fille à une autre est transféré, pas réinscrit.
     *
     * <p>Si {@code parentTenantId} est {@code null}, l'église créée est une
     * racine isolée : le modèle continue de fonctionner sans réseau.
     */
    @Transactional
    public Tenant createChildChurch(UUID parentTenantId, String name, UUID creatorUserId, String plan) {
        String cleanName = requireName(name);
        Tenant parent = null;
        if (parentTenantId != null) {
            parent = tenantRepository.findById(parentTenantId)
                    .orElseThrow(() -> new DomainException("Organisation parente introuvable",
                            HttpStatus.NOT_FOUND, "PARENT_TENANT_NOT_FOUND"));
            if (parent.getStatus() != TenantStatus.ACTIVE) {
                throw new DomainException("L'organisation parente n'accepte pas de nouvelles églises",
                        HttpStatus.GONE, "PARENT_TENANT_NOT_ACCEPTING");
            }
            if (parent.getKind() != null && !parent.getKind().canHaveChildren()) {
                throw new DomainException(
                        "Une église ne peut pas avoir d'église enfant — choisissez une dénomination",
                        HttpStatus.BAD_REQUEST, "PARENT_KIND_CANNOT_HAVE_CHILDREN");
            }
            if (tenantRepository.existsByParentTenantIdAndNameIgnoreCase(parent.getId(), cleanName)) {
                throw new DomainException("Une église de ce nom existe déjà dans cette organisation",
                        HttpStatus.CONFLICT, "CHILD_NAME_TAKEN");
            }
        }

        com.discipolat.modules.tenants.api.TenantResponse childDto =
                tenantService.create(new com.discipolat.modules.tenants.api.CreateTenantRequest(
                cleanName, uniqueSlug(cleanName, parent == null ? null : parent.getId()),
                plan, parent == null ? null : parent.getCountry(),
                null, null, null, null, null, null));

        Tenant child = requireEntity(childDto);
        child.setParentTenantId(parent == null ? null : parent.getId());
        // Héritage de racine : le cœur du modèle de transfert.
        child.setRootTenantId(parent == null ? null : parent.effectiveRootTenantId());
        child.ensureRootTenantId();
        Tenant saved = tenantRepository.save(child);

        attachRootChurchNode(saved, creatorUserId);
        if (creatorUserId != null) {
            grantOwnership(saved, creatorUserId);
        }

        auditService.log(creatorUserId, saved.getId(), AUDIT_CHILD_CREATED, "TENANT", saved.getId(),
                "SUCCESS",
                Payloads.of("name", saved.getName(), "slug", saved.getSlug(),
                        "parentTenantId", parent == null ? null : parent.getId(),
                        "rootTenantId", saved.effectiveRootTenantId()),
                null, null, null);
        return saved;
    }

    // ============================================================
    // LECTURE DU RÉSEAU
    // ============================================================

    /**
     * Vue réseau d'une organisation : ses enfants directs et sa racine.
     *
     * <p><b>D7 — agrégats seulement.</b> Cette vue ne contient aucun nom de
     * membre ni email : si elle en contenait ne serait-ce qu'un, ce serait une
     * fuite inter-organisation. Le contenu d'une église s'obtient par
     * <b>impersonation</b> journalisée, limitée et révocable.
     */
    @Transactional(readOnly = true)
    public OrganizationView view(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException("Église introuvable",
                        HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND"));
        return toView(tenant, (int) tenantRepository.countByRootTenantId(tenant.effectiveRootTenantId()));
    }

    /** Les enfants directs d'une organisation (un niveau). */
    @Transactional(readOnly = true)
    public List<OrganizationView> childrenOf(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException("Église introuvable",
                        HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND"));
        UUID rootId = tenant.effectiveRootTenantId();
        return tenantRepository.findByParentTenantIdOrderByNameAsc(tenant.getId()).stream()
                .map(child -> toView(child, (int) tenantRepository.countByRootTenantId(rootId)))
                .toList();
    }

    /**
     * Toute la descendance d'une racine — la « communauté WhatsApp » du client.
     *
     * <p>Utilisé par l'écran réseau (§7.2 / T-W5) pour présenter l'arborescence
     * d'une dénomination et ses codes d'entrée.
     */
    @Transactional(readOnly = true)
    public List<OrganizationView> networkOf(UUID rootTenantId) {
        Tenant root = tenantRepository.findById(rootTenantId)
                .orElseThrow(() -> new DomainException("Organisation introuvable",
                        HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND"));
        long childCount = tenantRepository.countByRootTenantId(root.effectiveRootTenantId());
        return tenantRepository.findByRootTenantId(root.effectiveRootTenantId()).stream()
                .map(t -> toView(t, (int) childCount))
                .toList();
    }

    /** Les organisations racines — vue « réseau » de la console plateforme. */
    @Transactional(readOnly = true)
    public List<OrganizationView> roots() {
        return tenantRepository.findByParentTenantIdIsNullOrderByNameAsc().stream()
                .map(t -> toView(t, (int) tenantRepository.countByRootTenantId(t.effectiveRootTenantId())))
                .toList();
    }

    // ============================================================
    // INTERNAL
    // ============================================================

    /**
     * Recharge l'entité persistée après {@link TenantService#create}.
     *
     * <p>{@code TenantService.create} renvoie un {@code TenantResponse} (contrat
     * d'API). On relit l'entité pour disposer de l'identifiant attribué et
     * passer par les setters du modèle — plutôt que de reconstruire un
     * {@code Tenant} à la main, qui perdrait les valeurs par défaut
     * Appliquées en base.
     */
    private Tenant requireEntity(com.discipolat.modules.tenants.api.TenantResponse dto) {
        return tenantRepository.findById(dto.id())
                .orElseThrow(() -> new DomainException("Organisation introuvable juste après création",
                        HttpStatus.CONFLICT, "TENANT_NOT_PERSISTED"));
    }

    private OrganizationView toView(Tenant tenant, int childCount) {
        return new OrganizationView(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getKind() == null ? TenantKind.CHURCH.name() : tenant.getKind().name(),
                tenant.getParentTenantId(),
                tenant.effectiveRootTenantId(),
                tenant.getStatus().name(),
                tenant.getPlan(),
                childCount);
    }

    /**
     * Nœud racine de l'organisation.
     *
     * <p>Sans lui, l'arborescence interne ({@code OrganizationNode}) est vide
     * alors que la hiérarchie entre organisations existe — deux notions
     * distinctes qu'il ne faut pas confondre (§1.2). La création suit le
     * même pattern que {@code SelfServiceChurchService} : bascule explicite du
     * {@code TenantContext}, restauration en {@code finally}.
     */
    private void attachRootChurchNode(Tenant tenant, UUID ownerUserId) {
        String nodeCode = "ROOT_CHURCH_"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        organizationNodeService.createRootChurch(tenant.getId(), tenant.getName(), nodeCode, ownerUserId);
    }

    /** Le propriétaire de l'organisation : membership {@code TENANT_OWNER}. */
    private void grantOwnership(Tenant tenant, UUID ownerUserId) {
        if (ownerUserId == null) {
            return;
        }
        Role ownerRole = roleRepository.findGlobalByKey(OWNER_ROLE_KEY)
                .orElseThrow(() -> new DomainException(
                        "Le rôle propriétaire est absent — provisionnement impossible",
                        HttpStatus.CONFLICT, "OWNER_ROLE_MISSING"));
        membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(ownerUserId)
                .role(ownerRole)
                .roleLegacy(OWNER_ROLE_KEY)
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .build());
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Le nom de l'organisation est requis",
                    HttpStatus.BAD_REQUEST, "NAME_REQUIRED");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 150) {
            throw new DomainException("Nom trop long (150 caractères maximum)",
                    HttpStatus.BAD_REQUEST, "NAME_TOO_LONG");
        }
        return trimmed;
    }

    /**
     * Slug unique, dérivé du nom et dégagé des accents.
     *
     * <p>Le suffixe numérique évite la collision entre une église isolée et
     * une église enfant d'une même dénomination portant le même nom : le slug
     * est global (colonne {@code UNIQUE}), pas unique par parent.
     */
    private String uniqueSlug(String name, UUID parentTenantId) {
        String base = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isEmpty()) {
            base = "organisation";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40).replaceAll("-+$", "");
        }
        String candidate = base;
        for (int suffix = 2; tenantRepository.existsBySlug(candidate); suffix++) {
            candidate = base + "-" + suffix;
        }
        return candidate;
    }
}
