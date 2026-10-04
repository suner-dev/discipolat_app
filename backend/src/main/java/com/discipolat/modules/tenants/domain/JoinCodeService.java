package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (BE-1) — cycle de vie des codes de rejointure.
 *
 * <p>D2 : format {@code PREFIXE-XXXX}, préfixe = slug du tenant tronqué à 8
 * caractères alphanumériques (MAJUSCULES), suffixe = 4 caractères dans un
 * alphabet non ambigu (sans I/L/O/U/0/1). Un seul code ACTIF par valeur
 * (index partiel V219) ; la rotation déactive l'ancien et émet le nouveau.</p>
 */
@Service
public class JoinCodeService {

    private static final Logger log = LoggerFactory.getLogger(JoinCodeService.class);

    /** Alphabet sans confusion visuelle (pas de I, L, O, U, 0, 1). */
    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SUFFIX_LENGTH = 4;
    private static final int MAX_GENERATION_ATTEMPTS = 25;
    private static final int PREFIX_MAX_LENGTH = 8;

    private final TenantJoinCodeRepository joinCodeRepository;
    private final TenantRepository tenantRepository;
    private final OrganizationNodeRepository organizationNodeRepository;
    private final SecureRandom random = new SecureRandom();

    public JoinCodeService(TenantJoinCodeRepository joinCodeRepository,
                           TenantRepository tenantRepository,
                           OrganizationNodeRepository organizationNodeRepository) {
        this.joinCodeRepository = joinCodeRepository;
        this.tenantRepository = tenantRepository;
        this.organizationNodeRepository = organizationNodeRepository;
    }

    /**
     * Résultat du lookup public : vitrine uniquement — jamais de tenant_id,
     * d'email ni de liste de membres (cf. §6 garde-fous de la spec).
     */
    public record JoinLookup(boolean found,
                             String churchName,
                             String orgNodeLabel,
                             String slug,
                             JoinMode joinMode,
                             boolean requiresApproval) {

        static JoinLookup notFound() {
            return new JoinLookup(false, null, null, null, null, false);
        }
    }

    /**
     * Le lookup cible-t-il une sous-église plutôt que l'église racine ?
     *
     * <p>T-B4 (F14) : l'IHM en a besoin pour proposer explicitement « rejoindre
     * le campus Nord » plutôt qu'une adhésion générique à l'église.
     */
    public static boolean targetsSubChurch(JoinLookup lookup) {
        return lookup != null
                && lookup.found()
                && lookup.orgNodeLabel() != null
                && !lookup.orgNodeLabel().isBlank();
    }

    /** Normalise la saisie utilisateur : trim, MAJUSCULES, espaces → tirets. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toUpperCase();
        s = s.replaceAll("\\s+", "-");
        s = s.replaceAll("-+", "-");
        if (s.startsWith("-")) s = s.substring(1);
        if (s.endsWith("-")) s = s.substring(0, s.length() - 1);
        return s.isEmpty() ? null : s;
    }

    /**
     * Génère et persiste un code actif pour le tenant (et éventuellement une
     * sous-église D3). Appelé à la création du tenant (self-service ET
     * provisioning plateforme) et par l'admin pour chaque sous-église.
     *
     * <p><b>SPF ORGANISATION DENOMINATION V2 §7.1 / T-B4 (faille F16).</b> Si la
     * cible est l'église racine ({@code orgNodeId == null}), les codes
     * principaux <b>existants sont désactivés</b> avant l'insertion : D9
     * (« un code actif par cible ») doit être garanti par la donnée, pas
     * seulement par l'intention. Sans cela, un second code racine produit une
     * liste de deux lignes et faisait échouer en {@code 500} la résolution
     * publique par slug ({@code findPrimaryActiveCode} / {@code lookupBySlug}).
     *
     * <p>Les codes <b>historiques</b> ne sont pas supprimés : ils restent
     * injoignables ({@code isActive = false}) et l'historique d'adhésion est
     * préservé.
     */
    @Transactional
    public TenantJoinCode generate(UUID tenantId, UUID orgNodeId, String label,
                                   JoinMode joinMode, UUID createdBy) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException("Tenant introuvable",
                        HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND"));
        if (orgNodeId == null) {
            retireExistingPrimaryCodes(tenantId);
        }
        String prefix = buildPrefix(tenant);
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = prefix + "-" + randomSuffix();
            if (joinCodeRepository.existsByCodeAndIsActiveTrue(candidate)) {
                continue;
            }
            TenantJoinCode saved = joinCodeRepository.save(TenantJoinCode.builder()
                    .tenantId(tenantId)
                    .orgNodeId(orgNodeId)
                    .code(candidate)
                    .label(label)
                    .joinMode(joinMode == null ? JoinMode.OPEN : joinMode)
                    .isActive(true)
                    .createdBy(createdBy)
                    .build());
            log.info("Join code generated tenantId={} orgNodeId={} codeId={}",
                    tenantId, orgNodeId, saved.getId());
            return saved;
        }
        throw new DomainException("Impossible de générer un code unique, réessayez",
                HttpStatus.CONFLICT, "JOIN_CODE_GENERATION_FAILED");
    }

    /**
     * Rotation : l'ancien code meurt ({@code isActive=false}), un nouveau naît
     * (même scope).
     *
     * <p><b>Unicité maintenue (F16).</b> {@link #generate} ne désactive que le
     * code <b>principal</b> existant quand la cible est la racine ; pour une
     * sous-église, c'est donc la rotation qui doit appeler
     * {@link #retireExistingCodesForTarget}, sinon deux codes actifs
     * cohabitent sur le même nœud et le nommage devient ambigu pour
     * l'utilisateur comme pour la résolution.
     */
    @Transactional
    public TenantJoinCode rotate(UUID codeId, UUID currentTenantId, UUID actorId) {
        TenantJoinCode existing = requireOwned(codeId, currentTenantId);
        existing.setActive(false);
        joinCodeRepository.save(existing);
        retireExistingCodesForTarget(existing.getTenantId(), existing.getOrgNodeId());
        TenantJoinCode fresh = generate(existing.getTenantId(), existing.getOrgNodeId(),
                existing.getLabel(), existing.getJoinMode(), actorId);
        fresh.setRotatedAt(Instant.now());
        return joinCodeRepository.save(fresh);
    }

    @Transactional
    public TenantJoinCode update(UUID codeId, UUID currentTenantId,
                                 String label, JoinMode joinMode, Boolean isActive) {
        TenantJoinCode code = requireOwned(codeId, currentTenantId);
        if (label != null && !label.isBlank()) {
            code.setLabel(label.trim());
        }
        if (joinMode != null) {
            code.setJoinMode(joinMode);
        }
        if (isActive != null) {
            code.setActive(isActive);
        }
        return joinCodeRepository.save(code);
    }

    /** Suppression douce : le code cesse d'être joignable, l'historique reste. */
    @Transactional
    public void deactivate(UUID codeId, UUID currentTenantId) {
        TenantJoinCode code = requireOwned(codeId, currentTenantId);
        code.setActive(false);
        joinCodeRepository.save(code);
    }

    @Transactional(readOnly = true)
    public List<TenantJoinCode> listForTenant(UUID tenantId) {
        return joinCodeRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    /**
     * Codes ACTIFS d'un tenant — requête unique pour les usages croisés
     * (sélecteur de sous-église, indicateurs).
     */
    @Transactional(readOnly = true)
    public List<TenantJoinCode> listActiveForTenant(UUID tenantId) {
        return joinCodeRepository.findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId);
    }

    /** Résolution interne (inscription par code, rejointure authentifiée). */
    @Transactional(readOnly = true)
    public Optional<TenantJoinCode> findActiveByRawInput(String raw) {
        String normalized = normalize(raw);
        if (normalized == null) return Optional.empty();
        return joinCodeRepository.findByCodeAndIsActiveTrue(normalized);
    }

    /**
     * Code principal (église racine) actif d'un tenant — lien vanity
     * {@code /j/<slug>}.
     *
     * <p><b>F16.</b> Après correction, {@code generate} garantit qu'il n'existe
     * qu'un code principal actif : la requête renvoie le plus récent plutôt
     * qu'un {@code Optional}, ce qui rend l'appel total même si des données
     * héritées contredisent l'invariant.
     */
    @Transactional(readOnly = true)
    public Optional<TenantJoinCode> findPrimaryActiveCode(UUID tenantId) {
        List<TenantJoinCode> candidates = primaryActiveCodes(tenantId);
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
    }

    /**
     * Codes principaux ACTIFS d'un tenant, du plus récent au plus ancien.
     *
     * <p>Lecture défensive : même si des données héritées comportent plusieurs
     * codes racine actifs, l'appelant reçoit une liste triée au lieu d'une
     * exception (F16).
     */
    @Transactional(readOnly = true)
    public List<TenantJoinCode> primaryActiveCodes(UUID tenantId) {
        if (tenantId == null) {
            return List.of();
        }
        return joinCodeRepository
                .findByTenantIdAndOrgNodeIdIsNullAndIsActiveTrueOrderByCreatedAtDesc(tenantId);
    }

    /** Désactive tous les codes principaux ACTIFS (invariant D9 pour la racine). */
    private void retireExistingPrimaryCodes(UUID tenantId) {
        for (TenantJoinCode stale : primaryActiveCodes(tenantId)) {
            stale.setActive(false);
            stale.setUpdatedAt(Instant.now());
            joinCodeRepository.save(stale);
            log.info("Join code principal retiré (tenantId={} codeId={})", tenantId, stale.getId());
        }
    }

    /**
     * Désactive les codes ACTIFS de la même cible (racine ou nœud).
     *
     * <p>Appelé avant {@link #generate} par {@link #rotate} : la rotation doit
     * conserver l'unicité aussi sur une sous-église, là où
     * {@link #retireExistingPrimaryCodes} ne suffirait pas.
     */
    private void retireExistingCodesForTarget(UUID tenantId, UUID orgNodeId) {
        if (orgNodeId == null) {
            return; // déjà traité par generate()
        }
        joinCodeRepository.findByTenantIdAndOrgNodeIdAndIsActiveTrue(tenantId, orgNodeId)
                .ifPresent(stale -> {
                    stale.setActive(false);
                    stale.setUpdatedAt(Instant.now());
                    joinCodeRepository.save(stale);
                    log.info("Join code sous-église retiré (tenantId={} codeId={})", tenantId, stale.getId());
                });
    }

    /**
     * Lookup public par code : renvoie la vitrine de l'église si le code est
     * actif et que le tenant est ACTIVE. Le code lui-même n'est jamais révélé
     * par un lookup par slug.
     */
    @Transactional(readOnly = true)
    public JoinLookup lookupByCode(String raw) {
        return findActiveByRawInput(raw)
                .map(this::toLookup)
                .orElse(JoinLookup.notFound());
    }

    /** Lookup public par slug (lien vanity /j/&lt;slug&gt;). */
    @Transactional(readOnly = true)
    public JoinLookup lookupBySlug(String slug) {
        if (slug == null || slug.isBlank()) return JoinLookup.notFound();
        Optional<Tenant> tenantOpt = tenantRepository.findBySlug(slug.trim().toLowerCase());
        if (tenantOpt.isEmpty() || tenantOpt.get().getStatus() != TenantStatus.ACTIVE) {
            return JoinLookup.notFound();
        }
        Tenant tenant = tenantOpt.get();
        // Le mode du code principal, s'il existe ; défaut OPEN pour les
        // tenants historiques sans code (le lien slug reste valable).
        // F16 : lecture par liste triée, jamais par Optional multi-lignes.
        JoinMode mode = primaryActiveCodes(tenant.getId()).stream()
                .findFirst()
                .map(TenantJoinCode::getJoinMode)
                .orElse(JoinMode.OPEN);
        return new JoinLookup(true, tenant.getName(), null, tenant.getSlug(), mode,
                mode == JoinMode.APPROVAL);
    }

    private JoinLookup toLookup(TenantJoinCode code) {
        Tenant tenant = tenantRepository.findById(code.getTenantId()).orElse(null);
        if (tenant == null || tenant.getStatus() != TenantStatus.ACTIVE) {
            return JoinLookup.notFound();
        }
        String orgLabel = code.getOrgNodeId() == null
                ? null
                : resolveNodeLabel(tenant.getId(), code.getOrgNodeId());
        return new JoinLookup(true, tenant.getName(), orgLabel, tenant.getSlug(),
                code.getJoinMode(), code.getJoinMode() == JoinMode.APPROVAL);
    }

    /**
     * Libellé d'une sous-église, pour l'affichage public du lookup.
     *
     * <p>Le nom du nœud est une information de vitrine (le visiteur doit savoir
     * dans quel campus il entre) ; il ne contient aucune donnée personnelle.
     * Une lecture ratée n'est jamais bloquante : on renvoie {@code null} et
     * l'appelant affiche le seul nom de l'église.
     */
    private String resolveNodeLabel(UUID tenantId, UUID orgNodeId) {
        return organizationNodeRepository.findById(orgNodeId)
                .map(node -> node.getName() != null && !node.getName().isBlank()
                        ? node.getName()
                        : node.getCode())
                .orElse(null);
    }

    private TenantJoinCode requireOwned(UUID codeId, UUID currentTenantId) {
        TenantJoinCode code = joinCodeRepository.findById(codeId)
                .orElseThrow(() -> new DomainException("Code introuvable",
                        HttpStatus.NOT_FOUND, "JOIN_CODE_NOT_FOUND"));
        if (currentTenantId == null || !code.getTenantId().equals(currentTenantId)) {
            // Cross-tenant interdit même pour un admin authentifié sur un autre tenant.
            throw new DomainException("Code introuvable",
                    HttpStatus.NOT_FOUND, "JOIN_CODE_NOT_FOUND");
        }
        return code;
    }

    private String buildPrefix(Tenant tenant) {
        String source = tenant.getSlug() != null && !tenant.getSlug().isBlank()
                ? tenant.getSlug() : tenant.getName();
        String cleaned = source == null ? "" :
                source.toUpperCase().replaceAll("[^A-Z0-9]", "");
        // Purge des caractères hors alphabet (I/L/O/U/0/1) pour que le préfixe
        // reste 100 % saisissable sans ambiguïté.
        cleaned = cleaned.replaceAll("[ILOU01]", "");
        if (cleaned.isEmpty()) cleaned = "EGLISE";
        return cleaned.length() > PREFIX_MAX_LENGTH
                ? cleaned.substring(0, PREFIX_MAX_LENGTH) : cleaned;
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
