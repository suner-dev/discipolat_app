package com.discipolat.modules.passport.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service du Passeport Spirituel portable et vérifiable (PHASE 2).
 *
 * Garanties :
 * - identité vérifiable : contenu signé RSA-SHA256, falsification détectable ;
 * - multi-tenancy : création forcée au tenant courant + filtre Hibernate ;
 * - traçabilité : chaque vérification publique est journalisée ;
 * - exposition minimale : la vérification publique ne renvoie que le nécessaire.
 */
@Service
public class PassportService {

    private static final Logger log = LoggerFactory.getLogger(PassportService.class);

    private final SpiritualPassportRepository passportRepository;
    private final SpiritualPassportEntryRepository entryRepository;
    private final PassportVerificationRepository verificationRepository;
    private final PassportSignatureService signatureService;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final com.discipolat.common.infrastructure.security.SecurityUtils securityUtils;

    public PassportService(SpiritualPassportRepository passportRepository,
                           SpiritualPassportEntryRepository entryRepository,
                           PassportVerificationRepository verificationRepository,
                           PassportSignatureService signatureService,
                           UserRepository userRepository,
                           TenantRepository tenantRepository,
                           com.discipolat.common.infrastructure.security.SecurityUtils securityUtils) {
        this.passportRepository = passportRepository;
        this.entryRepository = entryRepository;
        this.verificationRepository = verificationRepository;
        this.signatureService = signatureService;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.securityUtils = securityUtils;
    }

    // ======================== ÉMISSION ========================

    /**
     * Émet (ou retourne si déjà émis) le passeport d'un membre du tenant courant.
     */
    @Transactional
    public SpiritualPassport issuePassport(UUID memberId) {
        UUID tenantId = TenantContext.requireTenantId();
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("User", memberId));

        SpiritualPassport passport = passportRepository.findByTenantIdAndMemberId(tenantId, memberId)
                .orElseGet(() -> {
                    SpiritualPassport p = new SpiritualPassport();
                    p.setTenantId(tenantId);
                    p.setMemberId(memberId);
                    p.setPassportCode(generatePassportCode());
                    p.setStatus("ACTIVE");
                    return passportRepository.save(p);
                });

        resign(passport);
        log.info("[Passport] Passport issued: {} for member {} by tenant {}", passport.getPassportCode(), memberId, tenantId);
        return passport;
    }

    /** Ajoute une entrée d'historique et re-signe le passeport. */
    @Transactional
    public SpiritualPassportEntry addEntry(UUID passportId, SpiritualPassportEntry entry) {
        UUID tenantId = TenantContext.requireTenantId();
        SpiritualPassport passport = passportRepository.findByIdAndTenantId(passportId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("SpiritualPassport", passportId));
        if (!"ACTIVE".equals(passport.getStatus())) {
            throw new IllegalStateException("Impossible d'ajouter une entrée à un passeport " + passport.getStatus() + ".");
        }
        entry.setTenantId(tenantId);
        entry.setPassportId(passportId);
        entry.setCreatedByUserId(SecurityUtils.getCurrentUserId());
        SpiritualPassportEntry saved = entryRepository.save(entry);
        resign(passport);
        log.info("[Passport] Entry added to {}: {} by tenant {}", passport.getPassportCode(), entry.getEntryType(), tenantId);
        return saved;
    }

    /** Révoque un passeport (perte, sortie de l'église, fraude suspectée). */
    @Transactional
    public SpiritualPassport revokePassport(UUID passportId, String reason) {
        UUID tenantId = TenantContext.requireTenantId();
        SpiritualPassport passport = passportRepository.findByIdAndTenantId(passportId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("SpiritualPassport", passportId));
        passport.setStatus("REVOKED");
        passport.setRevokedAt(LocalDateTime.now());
        passport.setRevokedReason(reason);
        resign(passport);
        log.info("[Passport] Passport revoked: {} by tenant {}", passport.getPassportCode(), tenantId);
        return passport;
    }

    // ======================== LECTURE (authentifiée) ========================

    /** Passeport du membre courant. */
    @Transactional(readOnly = true)
    public SpiritualPassport getMyPassport() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID memberId = SecurityUtils.getCurrentUserId();
        return passportRepository.findByTenantIdAndMemberId(tenantId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("SpiritualPassport", memberId));
    }

    /** Passeport d'un membre du tenant courant (usage pastoral). */
    @Transactional(readOnly = true)
    public SpiritualPassport getMemberPassport(UUID memberId) {
        UUID tenantId = TenantContext.requireTenantId();
        return passportRepository.findByTenantIdAndMemberId(tenantId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("SpiritualPassport", memberId));
    }

    /** Entrées d'historique d'un passeport. */
    @Transactional(readOnly = true)
    public List<SpiritualPassportEntry> getEntries(UUID passportId) {
        return entryRepository.findByPassportIdOrderByCreatedAtAsc(passportId);
    }

    /**
     * Charge un passeport du tenant courant : le titulaire y accède toujours,
     * les autres membres uniquement si rôle responsable (ADMIN/PASTEUR/RESPONSABLE).
     */
    @Transactional(readOnly = true)
    public SpiritualPassport getPassportForCurrentUser(UUID passportId) {
        UUID tenantId = TenantContext.requireTenantId();
        SpiritualPassport passport = passportRepository.findByIdAndTenantId(passportId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("SpiritualPassport", passportId));
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null || !passport.getMemberId().equals(userId)) {
            if (!securityUtils.hasActiveRole("ADMIN", "PASTEUR", "RESPONSABLE")) {
                throw new SecurityException("Accès au passeport d'un autre membre non autorisé.");
            }
        }
        return passport;
    }

    // ======================== VÉRIFICATION PUBLIQUE ========================

    /**
     * Vérification publique par code (QR). Retourne un verdict et journalise
     * la tentative. N'expose jamais les données privées détaillées.
     */
    @Transactional
    public VerificationResult verifyByCode(String code, String remoteAddr, String userAgent) {
        SpiritualPassport passport = passportRepository.findByPassportCode(code).orElse(null);

        VerificationResult result;
        if (passport == null) {
            result = new VerificationResult("NOT_FOUND", null, null, null, null, 0, false);
        } else {
            String effectiveStatus = effectiveStatus(passport);
            boolean signatureValid = isSignatureValid(passport);
            String verdict;
            if (!signatureValid) {
                verdict = "INVALID";
            } else if ("REVOKED".equals(effectiveStatus)) {
                verdict = "REVOKED";
            } else if ("EXPIRED".equals(effectiveStatus)) {
                verdict = "EXPIRED";
            } else {
                verdict = "VALID";
            }
            result = new VerificationResult(
                    verdict,
                    holderName(passport.getMemberId()),
                    churchName(passport.getTenantId()),
                    passport.getIssuedAt(),
                    passport.getExpiresAt(),
                    entryRepository.findByPassportIdOrderByCreatedAtAsc(passport.getId()).size(),
                    signatureValid);
        }

        PassportVerification trace = new PassportVerification();
        trace.setPassportCode(code);
        trace.setResult(result.status());
        trace.setRemoteAddr(truncate(remoteAddr, 60));
        trace.setUserAgent(truncate(userAgent, 300));
        verificationRepository.save(trace);

        log.info("[Passport] Verification of {}: {} (from {})", code, result.status(), remoteAddr);
        return result;
    }

    /** Historique des vérifications d'un passeport (traçabilité, usage interne). */
    @Transactional(readOnly = true)
    public List<PassportVerification> getVerifications(String passportCode) {
        return verificationRepository
                .findByPassportCodeOrderByVerifiedAtDesc(passportCode, PageRequest.of(0, 50))
                .getContent();
    }

    // ======================== HELPERS ========================

    private String effectiveStatus(SpiritualPassport passport) {
        if ("REVOKED".equals(passport.getStatus())) {
            return "REVOKED";
        }
        if (passport.getExpiresAt() != null && passport.getExpiresAt().isBefore(LocalDateTime.now())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    /** Re-vérifie la signature : payload canonique + empreinte des entrées. */
    private boolean isSignatureValid(SpiritualPassport passport) {
        if (!signatureService.isVerificationConfigured()) {
            return false;
        }
        String payload = canonicalPayload(passport);
        return signatureService.verify(payload, passport.getSignature());
    }

    private String canonicalPayload(SpiritualPassport passport) {
        String payloadHash = computePayloadHash(passport.getId());
        return passport.getPassportCode() + "|" + passport.getMemberId() + "|"
                + passport.getStatus() + "|" + payloadHash;
    }

    /** Empreinte stable des entrées (ordre de création), liée à la signature. */
    private String computePayloadHash(UUID passportId) {
        List<SpiritualPassportEntry> entries = entryRepository.findByPassportIdOrderByCreatedAtAsc(passportId);
        StringBuilder canonical = new StringBuilder();
        entries.stream()
                .sorted(Comparator.comparing(SpiritualPassportEntry::getCreatedAt)
                        .thenComparing(e -> e.getId() == null ? "" : e.getId().toString()))
                .forEach(e -> canonical
                        .append(e.getEntryType()).append('|')
                        .append(e.getTitle()).append('|')
                        .append(e.getOccurredAt() == null ? "" : e.getOccurredAt()).append('|')
                        .append(e.getIssuingOrganization() == null ? "" : e.getIssuingOrganization()).append('|')
                        .append(Boolean.TRUE.equals(e.getVerified())).append(';'));
        return signatureService.sha256Hex(canonical.toString());
    }

    /** (Re)calcule l'empreinte et la signature du passeport. */
    void resign(SpiritualPassport passport) {
        if (!signatureService.isSigningConfigured()) {
            log.warn("[Passport] Signature désactivée (clé privée absente) — passeport {} non signé", passport.getPassportCode());
            return;
        }
        String payloadHash = computePayloadHash(passport.getId());
        passport.setPayloadHash(payloadHash);
        passport.setSignature(signatureService.sign(
                passport.getPassportCode() + "|" + passport.getMemberId() + "|"
                        + passport.getStatus() + "|" + payloadHash));
        passportRepository.save(passport);
    }

    private String holderName(UUID memberId) {
        return userRepository.findById(memberId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Titulaire inconnu");
    }

    private String churchName(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(Tenant::getName).orElse(null);
    }

    /**
     * Code public : préfixe + 12 caractères hex dérivés d'un UUID aléatoire.
     * Ne contient aucune donnée personnelle.
     */
    private String generatePassportCode() {
        String hex = String.format("%012X", UUID.randomUUID().getMostSignificantBits() & 0xFFFFFFFFFFFFL);
        return "DP-" + hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-" + hex.substring(8);
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** Résultat de vérification publique (exposition minimale). */
    public record VerificationResult(
            String status,
            String holderName,
            String issuedBy,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt,
            int entryCount,
            boolean signatureValid) {
    }
}
