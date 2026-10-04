package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.multitenancy.CrossTenantScopeAccess;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.tenants.api.CreateTenantRequest;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SPEC_ONBOARDING_FLOWS (BE-2, décision D1) — « Créer mon église » en
 * self-service : le fondateur obtient immédiatement son tenant, son rôle
 * {@code TENANT_OWNER} et son code de rejointure, SANS passer par une
 * approbation Super Admin (le flux legacy de demande approuvée reste intact
 * via {@code TenantRegistrationService}).
 *
 * <p>Le déroulé reproduit {@code PlatformProvisioningService.provision} :
 * création du tenant (slug unique, abonnement initial, modules, dictionnaires)
 * puis, sous {@link CrossTenantScopeAccess#callForTenantSwitch} (pattern H4 —
 * sinon le filtre Hibernate masque les lignes fraîchement écrites), nœud
 * racine de l'église, membership owner et code de rejointure. Le compte
 * fondateur est créé ACTIF (le mot de passe est saisi par l'intéressé dans le
 * formulaire, l'email d'activation serait une friction inutile) ; la session
 * est réémise par le contrôleur via {@code AuthService.issueSession}.</p>
 */
@Service
public class SelfServiceChurchService {

    private static final Logger log = LoggerFactory.getLogger(SelfServiceChurchService.class);
    private static final String OWNER_ROLE_KEY = "TENANT_OWNER";
    private static final int SLUG_MAX_LENGTH = 40;

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final OrganizationNodeService organizationNodeService;
    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JoinCodeService joinCodeService;
    private final AuditService auditService;
    private final CrossTenantScopeAccess crossTenant;
    private final EmailService emailService;
    private final String frontendUrl;

    public SelfServiceChurchService(TenantService tenantService,
                                    TenantRepository tenantRepository,
                                    OrganizationNodeService organizationNodeService,
                                    TenantMembershipRepository membershipRepository,
                                    RoleRepository roleRepository,
                                    UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    JoinCodeService joinCodeService,
                                    AuditService auditService,
                                    CrossTenantScopeAccess crossTenant,
                                    EmailService emailService,
                                    @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.organizationNodeService = organizationNodeService;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.joinCodeService = joinCodeService;
        this.auditService = auditService;
        this.crossTenant = crossTenant;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    public record ChurchCreation(TenantResponse tenant, User founder, String joinCode, OrganizationNode rootChurch) {
    }

    /**
     * @throws BusinessRuleException EMAIL_TAKEN / NAME_REQUIRED / CHURCH_NAME_REQUIRED
     *                                (refus AVANT toute écriture — aucune création partielle)
     */
    @Transactional
    public ChurchCreation createChurch(String email, String rawPassword, String firstName, String lastName,
                                       String phone, String churchName, String plan, String country) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessRuleException("Email et mot de passe sont requis", "NAME_REQUIRED");
        }
        if (churchName == null || churchName.isBlank()) {
            throw new BusinessRuleException("Le nom de l'église est requis", "CHURCH_NAME_REQUIRED");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessRuleException("Email already exists: " + normalizedEmail);
        }
        String name = churchName.trim();
        String slug = uniqueSlug(name);

        // 1) Tenant ACTIVE, abonnement initial, modules et dictionnaires (réutilise
        //    TenantService.create tel quel — même contrat que le provisioning plateforme).
        TenantResponse tenant = tenantService.create(new CreateTenantRequest(
                name, slug, plan, country, null, null, null, null, null, null));
        final UUID tenantId = tenant.id();

        // 2) Fondateur = compte ACTIVE, rôle legacy ADMIN (l'éditeur moderne
        //    TENANT_OWNER est posé par le membership ci-dessous).
        User founder = userRepository.save(User.builder()
                .tenantId(tenantId)
                .email(normalizedEmail)
                .firstName(firstName != null ? firstName.trim() : null)
                .lastName(lastName != null ? lastName.trim() : null)
                .phone(phone != null ? phone.trim() : null)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(UserRole.ADMIN)
                .roles(Set.of(UserRole.ADMIN))
                .activeRole(UserRole.ADMIN)
                .statut(UserStatus.ACTIVE)
                .estChefDeFamille(false)
                .twoFactorEnabled(false)
                .build());

        // 3) H4 : bascule volontaire sur le tenant qui vient de naître — sans
        //    suspension du filtre, le nœud et le membership écrits seraient
        //    invisibles à la lecture suivante (404 mesuré sur le provisioning).
        final UUID previousTenantId = TenantContext.getTenantId();
        final AtomicReference<String> joinCodeRef = new AtomicReference<>();
        OrganizationNode rootChurch = crossTenant.callForTenantSwitch(() -> {
            try {
                TenantContext.setTenantId(tenantId);
                String churchCode = "ROOT_CHURCH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
                OrganizationNode church = organizationNodeService.createRootChurch(tenantId, name, churchCode, founder.getId());
                ensureOwnerMembership(tenantId, founder.getId());
                TenantJoinCode code = joinCodeService.generate(tenantId, null, name, JoinMode.OPEN, founder.getId());
                joinCodeRef.set(code.getCode());
                sendWelcomeEmail(founder, name, code.getCode(), tenant.slug());
                return church;
            } finally {
                if (previousTenantId != null) {
                    TenantContext.setTenantId(previousTenantId);
                } else {
                    TenantContext.clear();
                }
            }
        });

        auditService.log(founder.getId(), tenantId, "TENANT_SELF_SERVICE_CREATED", "TENANT", tenantId,
                "SUCCESS", Map.of("slug", slug, "founderId", founder.getId().toString()), null, null, null);
        return new ChurchCreation(tenant, founder, joinCodeRef.get(), rootChurch);
    }

    /** Copie du pattern {@code TenantOwnerProvisioningService.ensureOwnerMembership} (FK role NOT NULL). */
    private void ensureOwnerMembership(UUID tenantId, UUID userId) {
        boolean alreadyOwner = membershipRepository
                .findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE)
                .stream()
                .anyMatch(m -> m.getScopeType() == MembershipScopeType.TENANT);
        if (alreadyOwner) {
            return;
        }
        Role ownerRole = roleRepository.findGlobalByKey(OWNER_ROLE_KEY)
                .orElseThrow(() -> new BusinessRuleException(
                        "Le rôle propriétaire est absent — provisionnement impossible", "OWNER_ROLE_MISSING"));
        membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantId)
                .userId(userId)
                .role(ownerRole)
                .roleLegacy(OWNER_ROLE_KEY)
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .build());
    }

    /** Email de bienvenue : jamais bloquant (décision D10, pattern InvitationService). */
    private void sendWelcomeEmail(User founder, String churchName, String joinCode, String slug) {
        try {
            emailService.send(
                    founder.getEmail(),
                    "Votre église " + churchName + " est ouverte sur Discipolat",
                    "Bonjour " + founder.getFirstName() + ",\n\n"
                            + "Votre église « " + churchName + " » vient d'être créée. "
                            + "Vous en êtes l'administrateur propriétaire.\n\n"
                            + "Code de rejointure à partager avec vos membres : " + joinCode + "\n"
                            + "Lien d'invitation : " + frontendUrl + "/j/" + slug + "\n\n"
                            + "Cordialement,\nL'équipe Discipolat"
            );
        } catch (RuntimeException failure) {
            log.warn("Email de bienvenue non envoyé pour {}: {}", founder.getEmail(), failure.getMessage());
        }
    }

    /** Slug dérivé du nom, débarrassé des accents, unique (suffixe numérique si collision). */
    private String uniqueSlug(String churchName) {
        String base = Normalizer.normalize(churchName, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isEmpty()) {
            base = "eglise";
        }
        if (base.length() > SLUG_MAX_LENGTH) {
            base = base.substring(0, SLUG_MAX_LENGTH).replaceAll("-+$", "");
        }
        String candidate = base;
        for (int suffix = 2; tenantRepository.existsBySlug(candidate); suffix++) {
            candidate = base + "-" + suffix;
        }
        return candidate;
    }
}
