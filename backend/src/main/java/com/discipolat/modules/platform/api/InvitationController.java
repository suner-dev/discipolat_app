package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationController {

    private final InvitationRepository invitationRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TenantMembershipService membershipService;

    public InvitationController(InvitationRepository invitationRepository,
                                TenantMembershipRepository membershipRepository,
                                UserRepository userRepository,
                                TenantMembershipService membershipService) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.membershipService = membershipService;
    }

    /**
     * Créer une invitation par email
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> createInvitation(
            @RequestBody Map<String, Object> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = TenantContext.getTenantId();
        
        String email = ((String) request.get("email")).toLowerCase();
        String role = (String) request.get("role");
        UUID organizationNodeId = request.get("organizationNodeId") != null ?
            UUID.fromString((String) request.get("organizationNodeId")) : null;
        
        if (email == null || role == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "email et role sont requis"
            ));
        }
        
        // Vérifier si l'utilisateur existe
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // Vérifier si l'utilisateur a déjà une membership active
            Optional<TenantMembership> existingMembership = membershipRepository
                .findByUserIdAndTenantIdAndStatus(user.getId(), tenantId, MembershipStatus.ACTIVE);
            
            if (existingMembership.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cet utilisateur appartient déjà à ce tenant",
                    "userId", user.getId().toString()
                ));
            }
            
            // Réactiver l'utilisateur s'il est inactif
            if (user.getStatut() == UserStatus.INACTIVE || user.getStatut() == UserStatus.ACTIVE) {
                // Créer la membership directement
                TenantMembership membership = membershipService.addMembership(
                    user.getId(), tenantId, role, currentUserId);
                
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "userId", user.getId().toString(),
                    "email", email,
                    "role", role,
                    "message", "Utilisateur ajouté avec succès"
                ));
            }
            
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Cet utilisateur est inactif"
            ));
        }
        
        // Vérifier si une invitation existe déjà pour cet email
        Optional<Invitation> existingInvitation = invitationRepository
            .findByEmailAndTenantIdAndStatus(email, tenantId, InvitationStatus.PENDING);
        
        if (existingInvitation.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Une invitation est déjà en attente pour cet email",
                "invitationId", existingInvitation.get().getId().toString(),
                "expiresAt", existingInvitation.get().getExpiresAt().toString()
            ));
        }
        
        // Créer l'invitation
        String token = UUID.randomUUID().toString();
        
        Invitation invitation = Invitation.builder()
            .tenantId(tenantId)
            .email(email)
            .role(role)
            .invitedBy(currentUserId)
            .invitationToken(token)
            .status(InvitationStatus.PENDING)
            .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
            .organizationNodeId(organizationNodeId)
            .build();
        
        invitation = invitationRepository.save(invitation);
        
        // Ici, envoyer l'email d'invitation (via un service d'email)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "invitationId", invitation.getId().toString(),
            "email", email,
            "role", role,
            "token", token,
            "expiresAt", invitation.getExpiresAt().toString(),
            "message", "Invitation créée. L'invité recevra un email avec un lien d'invitation."
        ));
    }

    /**
     * Accepter une invitation (depuis le lien d'invitation)
     */
    @PostMapping("/accept")
    public ResponseEntity<Map<String, Object>> acceptInvitation(
            @RequestBody Map<String, Object> request) {
        
        String token = (String) request.get("token");
        String password = (String) request.get("password");
        String firstName = (String) request.get("firstName");
        String lastName = (String) request.get("lastName");
        
        if (token == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Token d'invitation requis"
            ));
        }
        
        // Trouver l'invitation
        Optional<Invitation> invitationOpt = invitationRepository.findByInvitationToken(token);
        
        if (invitationOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invitation invalide ou expirée"
            ));
        }
        
        Invitation invitation = invitationOpt.get();
        
        // Vérifier si l'invitation a expiré
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Cette invitation a expiré"
            ));
        }
        
        // Vérifier si l'invitation est déjà acceptée ou annulée
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Cette invitation a déjà été traitée"
            ));
        }
        
        // Vérifier si l'utilisateur existe déjà avec cet email
        Optional<User> existingUser = userRepository.findByEmail(invitation.getEmail());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // Créer la membership
            TenantMembership membership = membershipService.addMembership(
                user.getId(), invitation.getTenantId(), invitation.getRole(), invitation.getInvitedBy());
            
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setUserId(user.getId());
            invitation.setAcceptedAt(Instant.now());
            invitationRepository.save(invitation);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", user.getId().toString(),
                "email", invitation.getEmail(),
                "role", invitation.getRole(),
                "message", "Votre compte a été lié avec succès"
            ));
        }
        
        // Créer un nouvel utilisateur
        if (password == null || password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Mot de passe requis (minimum 8 caractères)",
                "requiresRegistration", true
            ));
        }
        
        // Créer l'utilisateur (le hash sera fait par le service d'authentification)
        // Pour simplifier, on utilise directement le repository
        User newUser = User.builder()
            .tenantId(invitation.getTenantId())
            .email(invitation.getEmail())
            .firstName(firstName != null ? firstName : "")
            .lastName(lastName != null ? lastName : "")
            .passwordHash("HASH_" + password) // Dans un vrai cas, hasher avec BCrypt
            .role(UserRole.valueOf(invitation.getRole()))
            .activeRole(UserRole.valueOf(invitation.getRole()))
            .statut(UserStatus.ACTIVE)
            .build();
        
        newUser = userRepository.save(newUser);
        
        // Créer la membership
        TenantMembership membership = membershipService.addMembership(
            newUser.getId(), invitation.getTenantId(), invitation.getRole(), invitation.getInvitedBy());
        
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setUserId(newUser.getId());
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "userId", newUser.getId().toString(),
            "email", invitation.getEmail(),
            "role", invitation.getRole(),
            "message", "Compte créé et invitation acceptée"
        ));
    }

    /**
     * Vérifier le statut d'une invitation
     */
    @GetMapping("/check/{token}")
    public ResponseEntity<Map<String, Object>> checkInvitation(@PathVariable String token) {
        Optional<Invitation> invitationOpt = invitationRepository.findByInvitationToken(token);
        
        if (invitationOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "error", "Invitation introuvable"
            ));
        }
        
        Invitation invitation = invitationOpt.get();
        
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.ok(Map.of(
                "valid", false,
                "error", "Invitation expirée",
                "status", "EXPIRED"
            ));
        }
        
        return ResponseEntity.ok(Map.of(
            "valid", true,
            "email", invitation.getEmail(),
            "role", invitation.getRole(),
            "status", invitation.getStatus().name(),
            "expiresAt", invitation.getExpiresAt().toString()
        ));
    }

    /**
     * Récupérer les invitations d'un tenant
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listInvitations() {
        UUID tenantId = TenantContext.requireTenantId();
        
        List<Invitation> invitations = invitationRepository.findByTenantId(tenantId);
        
        return ResponseEntity.ok(invitations.stream().map(i -> Map.of(
            "id", i.getId().toString(),
            "email", i.getEmail(),
            "role", i.getRole(),
            "status", i.getStatus().name(),
            "invitedBy", i.getInvitedBy() != null ? i.getInvitedBy().toString() : null,
            "createdAt", i.getCreatedAt().toString(),
            "expiresAt", i.getExpiresAt().toString(),
            "acceptedAt", i.getAcceptedAt() != null ? i.getAcceptedAt().toString() : null
        )).collect(Collectors.toList()));
    }

    /**
     * Annuler une invitation
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Void> cancelInvitation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = TenantContext.getTenantId();
        
        Optional<Invitation> invitationOpt = invitationRepository.findById(id);
        
        if (invitationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Invitation invitation = invitationOpt.get();
        
        if (!invitation.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Impossible d'annuler une invitation déjà traitée"
            ));
        }
        
        invitation.setStatus(InvitationStatus.CANCELED);
        invitationRepository.save(invitation);
        
        return ResponseEntity.noContent().build();
    }
}
