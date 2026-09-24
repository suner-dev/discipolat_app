package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.api.CreateTenantRequest;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.SaasPlanService;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantPlanPolicy;
import com.discipolat.modules.tenants.domain.TenantService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class TenantRegistrationService {

    private final TenantRegistrationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;
    private final OrganizationNodeService organizationNodeService;
    private final RoleRepository roleRepository;
    private final TenantMembershipRepository membershipRepository;
    private final AuditService auditService;

    public TenantRegistrationService(TenantRegistrationRequestRepository requestRepository,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     TenantService tenantService,
                                     OrganizationNodeService organizationNodeService,
                                     RoleRepository roleRepository,
                                     TenantMembershipRepository membershipRepository,
                                     AuditService auditService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
        this.organizationNodeService = organizationNodeService;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TenantRegistrationRequest submit(String email, String rawPassword, String firstName,
                                             String lastName, String phone) {
        return submit(email, rawPassword, firstName, lastName, phone, null);
    }

    @Transactional
    public TenantRegistrationRequest submit(String email, String rawPassword, String firstName,
                                             String lastName, String phone, String requestedPlan) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        TenantRegistrationRequest request = requestRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> TenantRegistrationRequest.builder().email(normalizedEmail).build());
        if (request.getStatus() != null && request.getStatus() != TenantRegistrationStatus.REJECTED) {
            throw new BusinessRuleException("Une demande existe déjà pour cet email", "REGISTRATION_EXISTS");
        }
        String localPart = normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
        String organizationName = capitalize(localPart) + " — Église";
        request.setPasswordHash(passwordEncoder.encode(rawPassword));
        request.setFirstName(firstName.trim());
        request.setLastName(lastName.trim());
        request.setPhone(phone == null ? null : phone.trim());
        request.setOrganizationName(organizationName);
        request.setSlug(slugify(organizationName));
        String canonicalPlan = TenantPlanPolicy.canonicalizePlanKey(requestedPlan);
        if (canonicalPlan == null) {
            canonicalPlan = "DISCOVERY";
        }
        if (!SaasPlanService.PUBLIC_PLAN_KEYS.contains(canonicalPlan)) {
            throw new BusinessRuleException("Le plan sélectionné n'est pas disponible", "INVALID_PLAN");
        }
        request.setPlan(canonicalPlan);
        request.setCountry("CM");
        request.setCurrency("XAF");
        request.setTimezone("Africa/Douala");
        request.setLocale("fr");
        request.setStatus(TenantRegistrationStatus.PENDING_APPROVAL);
        request.setReviewerId(null);
        request.setDecisionReason(null);
        request.setReviewedAt(null);
        request.setCreatedAt(Instant.now());
        return requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Page<TenantRegistrationRequest> listPending(Pageable pageable) {
        return requestRepository.findByStatusOrderByCreatedAtDesc(TenantRegistrationStatus.PENDING_APPROVAL, pageable);
    }

    @Transactional
    public ApprovalResult approve(UUID requestId, String reason) {
        TenantRegistrationRequest request = requirePending(requestId);
        UUID actorId = SecurityUtils.getCurrentUserId();
        TenantResponse tenant = tenantService.create(new CreateTenantRequest(
                request.getOrganizationName(), request.getSlug(), request.getPlan(), request.getCountry(),
                request.getCurrency(), request.getTimezone(), request.getLocale(), null, null, null
        ));
        UUID tenantId = tenant.id();
        UUID previousTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            OrganizationNode church = organizationNodeService.createRootChurch(
                    tenantId, request.getOrganizationName(), "ROOT_CHURCH_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), actorId);
            User owner = userRepository.findGlobalByEmail(request.getEmail()).orElse(null);
            if (owner != null && !tenantId.equals(owner.getTenantId())) {
                throw new BusinessRuleException("L'email appartient déjà à un autre tenant", "USER_TENANT_MISMATCH");
            }
            if (owner == null) {
                owner = userRepository.save(
                        User.builder()
                                .email(request.getEmail())
                                .passwordHash(request.getPasswordHash())
                                .firstName(request.getFirstName())
                                .lastName(request.getLastName())
                                .phone(request.getPhone())
                                .role(com.discipolat.common.domain.UserRole.PASTEUR)
                                .roles(new HashSet<>(Set.of(com.discipolat.common.domain.UserRole.PASTEUR)))
                                .activeRole(com.discipolat.common.domain.UserRole.PASTEUR)
                                .statut(UserStatus.ACTIVE)
                                .estChefDeFamille(false)
                                .twoFactorEnabled(false)
                                .tenantId(tenantId)
                                .build()
                );
            }
            Role ownerRole = roleRepository.findGlobalByKey("TENANT_OWNER")
                    .orElseThrow(() -> new BusinessRuleException("Le rôle TENANT_OWNER est introuvable", "ROLE_MISSING"));
            membershipRepository.save(TenantMembership.builder()
                    .tenantId(tenantId)
                    .userId(owner.getId())
                    .role(ownerRole)
                    .roleLegacy("TENANT_OWNER")
                    .scopeType(MembershipScopeType.TENANT)
                    .status(MembershipStatus.ACTIVE)
                    .build());
            request.setStatus(TenantRegistrationStatus.APPROVED);
            request.setReviewerId(actorId);
            request.setDecisionReason(reason);
            request.setReviewedAt(Instant.now());
            requestRepository.save(request);
            auditService.log(actorId, tenantId, "TENANT_REGISTRATION_APPROVED", "TENANT", tenantId,
                    "SUCCESS", Map.of("requestId", requestId.toString(), "email", request.getEmail()), null, null, null);
            return new ApprovalResult(request, tenant, church, owner);
        } finally {
            if (previousTenantId != null) {
                TenantContext.setTenantId(previousTenantId);
            } else {
                TenantContext.clear();
            }
        }
    }

    @Transactional
    public TenantRegistrationRequest reject(UUID requestId, String reason) {
        TenantRegistrationRequest request = requirePending(requestId);
        request.setStatus(TenantRegistrationStatus.REJECTED);
        request.setReviewerId(SecurityUtils.getCurrentUserId());
        request.setDecisionReason(reason);
        request.setReviewedAt(Instant.now());
        return requestRepository.save(request);
    }

    private TenantRegistrationRequest requirePending(UUID requestId) {
        TenantRegistrationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("TenantRegistrationRequest", requestId));
        if (request.getStatus() != TenantRegistrationStatus.PENDING_APPROVAL) {
            throw new BusinessRuleException("La demande a déjà été traitée", "REGISTRATION_ALREADY_PROCESSED");
        }
        return request;
    }

    private String slugify(String value) {
        String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.substring(0, Math.min(50, slug.length()));
    }

    private String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public record ApprovalResult(TenantRegistrationRequest request, TenantResponse tenant,
                                 OrganizationNode church, User owner) {}
}
