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
    private final SecureRandom random = new SecureRandom();

    public JoinCodeService(TenantJoinCodeRepository joinCodeRepository,
                           TenantRepository tenantRepository) {
        this.joinCodeRepository = joinCodeRepository;
        this.tenantRepository = tenantRepository;
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
     */
    @Transactional
    public TenantJoinCode generate(UUID tenantId, UUID orgNodeId, String label,
                                   JoinMode joinMode, UUID createdBy) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException("Tenant introuvable",
                        HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND"));
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

    /** Rotation : l'ancien code meurt (isActive=false), un nouveau naît (même scope). */
    @Transactional
    public TenantJoinCode rotate(UUID codeId, UUID currentTenantId, UUID actorId) {
        TenantJoinCode existing = requireOwned(codeId, currentTenantId);
        existing.setActive(false);
        joinCodeRepository.save(existing);
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

    /** Résolution interne (inscription par code, rejointure authentifiée). */
    @Transactional(readOnly = true)
    public Optional<TenantJoinCode> findActiveByRawInput(String raw) {
        String normalized = normalize(raw);
        if (normalized == null) return Optional.empty();
        return joinCodeRepository.findByCodeAndIsActiveTrue(normalized);
    }

    /** Code principal (église racine) actif d'un tenant — lien vanity /j/<slug>. */
    @Transactional(readOnly = true)
    public Optional<TenantJoinCode> findPrimaryActiveCode(UUID tenantId) {
        return joinCodeRepository.findByTenantIdAndOrgNodeIdIsNullAndIsActiveTrue(tenantId);
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
        JoinMode mode = joinCodeRepository
                .findByTenantIdAndOrgNodeIdIsNullAndIsActiveTrue(tenant.getId())
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
        return new JoinLookup(true, tenant.getName(), code.getLabel(), tenant.getSlug(),
                code.getJoinMode(), code.getJoinMode() == JoinMode.APPROVAL);
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
