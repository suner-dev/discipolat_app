package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final OrganizationNodeRepository organizationNodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public InvitationService(InvitationRepository invitationRepository,
                             UserRepository userRepository,
                             TenantMembershipRepository membershipRepository,
                             RoleRepository roleRepository,
                             OrganizationNodeRepository organizationNodeRepository,
                             PasswordEncoder passwordEncoder,
                             AuditService auditService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.organizationNodeRepository = organizationNodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(noRollbackFor = DomainException.class)
    public Invitation validate(String token) {
        Invitation invitation = findPending(token, false);
        resolveRole(invitation);
        validateScope(invitation);
        return invitation;
    }

    @Transactional(noRollbackFor = DomainException.class)
    public AcceptanceResult accept(String token, String password, String firstName, String lastName) {
        Invitation invitation = findPending(token, true);
        Role role = resolveRole(invitation);
        validateScope(invitation);

        Optional<User> existingUser = userRepository.findByTenantIdAndEmail(
                invitation.getTenantId(), invitation.getEmail());
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            if (password == null || password.isBlank()) {
                throw new DomainException(
                        "Mot de passe requis pour nouveau compte",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_PASSWORD_REQUIRED"
                );
            }
            if (password.length() < 8) {
                throw new DomainException(
                        "Le mot de passe doit contenir au moins 8 caractères",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_PASSWORD_TOO_SHORT"
                );
            }
            if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new DomainException(
                        "Le mot de passe est trop long",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_PASSWORD_TOO_LONG"
                );
            }
            String normalizedFirstName = firstName != null ? firstName.trim() : "";
            String normalizedLastName = lastName != null ? lastName.trim() : "";
            if (normalizedFirstName.length() > 100 || normalizedLastName.length() > 100) {
                throw new DomainException(
                        "Le prénom ou le nom est trop long",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_NAME_INVALID"
                );
            }
            user = User.builder()
                    .tenantId(invitation.getTenantId())
                    .email(invitation.getEmail())
                    .passwordHash(passwordEncoder.encode(password))
                    .firstName(normalizedFirstName)
                    .lastName(normalizedLastName)
                    .role(UserRole.MEMBRE)
                    .statut(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
        }

        MembershipScopeType membershipScopeType = scopeType(invitation);
        boolean alreadyMember = membershipRepository.existsExactActiveMembership(
                user.getId(),
                invitation.getTenantId(),
                role.getId(),
                MembershipStatus.ACTIVE,
                membershipScopeType,
                invitation.getScopeId()
        );
        if (!alreadyMember) {
            membershipRepository.save(TenantMembership.builder()
                    .tenantId(invitation.getTenantId())
                    .userId(user.getId())
                    .role(role)
                    .roleLegacy(role.getKey())
                    .scopeType(membershipScopeType)
                    .scopeId(invitation.getScopeId())
                    .status(MembershipStatus.ACTIVE)
                    .invitedBy(invitation.getInviterId())
                    .build());
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);
        auditService.logSimple("INVITATION_ACCEPTED", "INVITATION", invitation.getId());

        return new AcceptanceResult(user.getId(), user.getEmail(), invitation.getTenantId(), alreadyMember);
    }

    private Invitation findPending(String token, boolean lock) {
        if (token == null || token.isBlank()) {
            throw new DomainException("Invitation invalide", HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND");
        }

        String tokenHash = InvitationTokenHasher.hash(token);
        Optional<Invitation> found = lock
                ? invitationRepository.findByTokenHashForUpdate(tokenHash)
                : invitationRepository.findByTokenHash(tokenHash);
        if (found.isEmpty()) {
            throw new DomainException("Invitation invalide", HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND");
        }

        Invitation invitation = found.get();
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new DomainException(
                    "Invitation expirée ou déjà utilisée",
                    HttpStatus.GONE,
                    "INVITATION_NOT_PENDING",
                    Map.of("status", invitation.getStatus().name())
            );
        }

        if (!invitation.getExpiresAt().isAfter(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new DomainException("Invitation expirée", HttpStatus.GONE, "INVITATION_EXPIRED");
        }
        return invitation;
    }

    private Role resolveRole(Invitation invitation) {
        String roleKey = invitation.getRole().toUpperCase();
        Role role = roleRepository.findByTenantIdAndKey(invitation.getTenantId(), roleKey)
                .or(() -> roleRepository.findGlobalByKey(roleKey))
                .orElseThrow(() -> new DomainException(
                        "Le rôle de l'invitation n'existe plus",
                        HttpStatus.GONE,
                        "INVITATION_ROLE_UNAVAILABLE"
                ));
        if (role.getTenantId() == null && role.getKey().startsWith("PLATFORM_")) {
            throw new DomainException(
                    "Un rôle plateforme ne peut pas être attribué par une invitation tenant",
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_PLATFORM_ROLE_FORBIDDEN"
            );
        }
        return role;
    }

    private void validateScope(Invitation invitation) {
        MembershipScopeType type = scopeType(invitation);
        if (type == MembershipScopeType.TENANT) {
            if (invitation.getScopeId() != null || invitation.getOrganizationNodeId() != null) {
                throw new DomainException(
                        "Un scope tenant ne doit pas contenir de ressource",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_SCOPE_INVALID"
                );
            }
            return;
        }

        if (invitation.getOrganizationNodeId() == null
                || invitation.getScopeId() == null
                || !invitation.getOrganizationNodeId().equals(invitation.getScopeId())) {
            throw new DomainException(
                    "Le scope de l'invitation est invalide",
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_SCOPE_INVALID"
                );
        }

        OrganizationNode node = organizationNodeRepository.findById(invitation.getOrganizationNodeId())
                .orElseThrow(() -> new DomainException(
                        "Le nœud organisationnel de l'invitation n'existe pas",
                        HttpStatus.GONE,
                        "INVITATION_SCOPE_UNAVAILABLE"
                ));
        if (!invitation.getTenantId().equals(node.getTenantId())) {
            throw new DomainException(
                    "Le scope de l'invitation appartient à un autre tenant",
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_SCOPE_INVALID"
            );
        }
    }

    private MembershipScopeType scopeType(Invitation invitation) {
        if (invitation.getScopeType() == null || invitation.getScopeType().isBlank()) {
            return MembershipScopeType.TENANT;
        }
        try {
            return MembershipScopeType.valueOf(invitation.getScopeType());
        } catch (IllegalArgumentException exception) {
            throw new DomainException(
                    "Le type de scope de l'invitation est invalide",
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_SCOPE_INVALID"
            );
        }
    }

    public record AcceptanceResult(UUID userId, String email, UUID tenantId, boolean alreadyMember) {
    }
}
