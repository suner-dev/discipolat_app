package com.discipolat.modules.authentication.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.exception.DomainException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.platform.domain.TenantRegistrationRequest;
import com.discipolat.modules.platform.domain.TenantRegistrationService;
import com.discipolat.modules.security.domain.RefreshTokenSessionService;
import com.discipolat.modules.security.domain.TokenRevocationService;
import com.discipolat.modules.tenants.domain.TenantStatusGuard;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;
    private static final int PASSWORD_RESET_VALIDITY_MINUTES = 30;
    private static final int ACTIVATION_VALIDITY_HOURS = 48;

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final TenantRegistrationService tenantRegistrationService;
    private final TokenRevocationService tokenRevocationService;
    private final RefreshTokenSessionService refreshTokenSessionService;
    private final TenantStatusGuard tenantStatusGuard;
    private final com.discipolat.modules.tenants.domain.ActiveTenantService activeTenantService;
    /** §G3.1 — inscription automatique au répertoire People après vérification. */
    private final com.discipolat.modules.people.service.PeopleService peopleService;
    /** §G3.1/§G6.4 — rattachement du self-signup à l'église (tenant) demandée. */
    private final com.discipolat.modules.tenants.domain.TenantRepository tenantRepository;
    private final String frontendUrl;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder, SecurityUtils securityUtils,
                       ActivationTokenRepository activationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService,
                       TenantRegistrationService tenantRegistrationService,
                       TokenRevocationService tokenRevocationService,
                       RefreshTokenSessionService refreshTokenSessionService,
                       TenantStatusGuard tenantStatusGuard,
                       com.discipolat.modules.tenants.domain.ActiveTenantService activeTenantService,
                       com.discipolat.modules.people.service.PeopleService peopleService,
                       com.discipolat.modules.tenants.domain.TenantRepository tenantRepository,
                       @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.securityUtils = securityUtils;
        this.activationTokenRepository = activationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.tenantRegistrationService = tenantRegistrationService;
        this.tokenRevocationService = tokenRevocationService;
        this.refreshTokenSessionService = refreshTokenSessionService;
        this.tenantStatusGuard = tenantStatusGuard;
        this.activeTenantService = activeTenantService;
        this.peopleService = peopleService;
        this.tenantRepository = tenantRepository;
        this.frontendUrl = frontendUrl;
    }

    public record AuthResult(String accessToken, String refreshToken, User user, String activeRole) {
        public AuthResult(String accessToken, String refreshToken, User user) {
            this(accessToken, refreshToken, user, user.getActiveRole() != null ? user.getActiveRole().name() : user.getRole().name());
        }
    }

    // ======================== SELF-REGISTRATION ========================

    public TenantRegistrationRequest register(String email, String rawPassword, String firstName,
                                                String lastName, String phone, String inviteCode) {
        return register(email, rawPassword, firstName, lastName, phone, inviteCode, null, null);
    }

    public TenantRegistrationRequest register(String email, String rawPassword, String firstName,
                                                String lastName, String phone, String inviteCode,
                                                String requestedPlan) {
        return register(email, rawPassword, firstName, lastName, phone, inviteCode, requestedPlan, null);
    }

    /**
     * Enregistrement public (demande d'organisation) — les consentements RGPD
     * (CGU, confidentialité, art. 9) sont obligatoires et horodatés.
     * Le rattachement direct du self-signup à une église est traité séparément
     * par {@link #registerInChurch} (§G3.1/§G6.4, apport Develop1).
     */
    public TenantRegistrationRequest register(String email, String rawPassword, String firstName,
                                                String lastName, String phone, String inviteCode,
                                                String requestedPlan,
                                                TenantRegistrationService.ConsentInfo consent) {
        if (inviteCode != null && !inviteCode.isBlank()) {
            throw new DomainException(
                    "Les invitations doivent être acceptées via leur lien dédié",
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "INVITATION_ACCEPTANCE_REQUIRED"
            );
        }
        return tenantRegistrationService.submit(email, rawPassword, firstName, lastName, phone,
                requestedPlan, consent);
    }

    /**
     * PORT Develop1 (§G3.1/§G6.4) — « quand un membre s'inscrit AU NOM D'UNE
     * ÉGLISE » : création directe d'un compte MEMBRE rattaché au tenant demandé
     * (tenantId côté mobile, tenantSlug côté web — lien /register?tenant=<slug>),
     * puis email d'activation ; l'inscription au répertoire suit la vérification
     * (voir activateAccount). Sans rattachement valide, la demande est refusée.
     *
     * <p>Chemin AJOUTÉ : la demande d'organisation de main (méthode register
     * ci-dessus, avec consentements RGPD et approbation Super Admin) reste le
     * flux par défaut et n'est pas modifiée.
     */
    public User registerInChurch(String email, String rawPassword, String firstName, String lastName,
                                 String phone, String tenantSlug, java.util.UUID tenantId) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessRuleException("Email already exists: " + normalizedEmail);
        }
        java.util.UUID resolvedTenantId = resolveSignupTenant(tenantSlug, tenantId);
        if (resolvedTenantId == null) {
            throw new BusinessRuleException("Aucune église demandée : passer tenantSlug ou tenantId");
        }
        User user = User.builder()
                .tenantId(resolvedTenantId)
                .email(normalizedEmail)
                .firstName(firstName != null ? firstName.trim() : null)
                .lastName(lastName != null ? lastName.trim() : null)
                .phone(phone != null ? phone.trim() : null)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(UserRole.MEMBRE)
                .roles(new HashSet<>(Set.of(UserRole.MEMBRE)))
                .activeRole(UserRole.MEMBRE)
                .statut(UserStatus.PENDING_ACTIVATION)
                .estChefDeFamille(false)
                .twoFactorEnabled(false)
                .build();
        User saved = userRepository.save(user);
        sendActivationEmail(saved.getId());
        return saved;
    }

    /** Le rattachement demandé doit exister et être ACTIVE (jamais un tenant suspendu). */
    private java.util.UUID resolveSignupTenant(String tenantSlug, java.util.UUID tenantId) {
        if (tenantId != null) {
            com.discipolat.modules.tenants.domain.Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new BusinessRuleException("Église introuvable (tenantId inconnu)"));
            if (tenant.getStatus() != com.discipolat.modules.tenants.domain.TenantStatus.ACTIVE) {
                throw new BusinessRuleException("Cette église n'accepte pas d'inscriptions actuellement");
            }
            return tenant.getId();
        }
        if (tenantSlug != null && !tenantSlug.isBlank()) {
            com.discipolat.modules.tenants.domain.Tenant tenant =
                    tenantRepository.findBySlug(tenantSlug.trim().toLowerCase())
                            .or(() -> tenantRepository.findBySlug(tenantSlug.trim()))
                            .orElseThrow(() -> new BusinessRuleException("Église introuvable : " + tenantSlug));
            if (tenant.getStatus() != com.discipolat.modules.tenants.domain.TenantStatus.ACTIVE) {
                throw new BusinessRuleException("Cette église n'accepte pas d'inscriptions actuellement");
            }
            return tenant.getId();
        }
        return null;
    }

    // ======================== LOGIN ========================

    public AuthResult login(String email, String password) {
        // B4 : resolution d'identite insensible a la casse (index unique V185 sur LOWER(email)).
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // US-01: Account lockout after 5 failed attempts
        if (user.isAccountLocked()) {
            throw new BadCredentialsException("Account is temporarily locked. Please try again later.");
        }

        // US-02: Check if account is activated
        if (user.getStatut() == UserStatus.PENDING_ACTIVATION) {
            throw new BadCredentialsException("Account not activated. Please check your email for the activation link.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getStatut() == UserStatus.INACTIVE) {
            throw new BadCredentialsException("Account is inactive");
        }

        // B1 : un tenant SUSPENDED / CANCELLED ne doit pas pouvoir se connecter.
        // Le controle est fait dans issueSession(), donc APRES les verifications
        // de compte (statut, mot de passe) — on ne divulgue rien sur un compte en
        // attente d'activation ou bloque. Il ne doit pas etre appele ici en plus :
        // la connexion sociale passe elle aussi par issueSession(), et deux
        // appels pour une seule tentative rendraient le double comptage illisible.
        //
        // Emission de la session : logique factorisee dans issueSession(), partagee
        // avec la connexion par identite externe (Google/Microsoft) pour qu'aucun
        // chemin d'authentification ne puisse deriver du login par mot de passe.
        return issueSession(user);
    }

    /**
     * Emet une sessioncomplete pour un compte deja authentifie (mot de passe
     * verifie, ou identite externe verifiee par {@code SocialIdentityVerifier}).
     *
     * <p>Centralise ce qui doit etre <b>identique</b> quel que soit le moyen
     * d'authentification : remise a zero des tentatives echouees,
     * synchronisation de l'ensemble des roles, role actif par priorite,
     * <b>garde de statut du tenant</b>, puis access token + refresh token enregstre
     * en session serveur.
     *
     * <p>La garde `tenantStatusGuard` est Appeliée ici et non seulement dans
     * {@code login()} : une eglise suspendue doit interdire la connexion
     * PAR TOUS LES CHEMINS, y compris « Se connecter avec Google ». C'etait un
     * trou : la connexion sociale ne verifiait pas le statut du tenant.
     */
    public AuthResult issueSession(User user) {
        tenantStatusGuard.assertAccessible(user.getTenantId());

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        // Initialize roles from existing role + estChefDeFamille flag
        Set<String> roleNames = new HashSet<>();
        roleNames.add(user.getRole().name());
        if (user.isEstChefDeFamille() && !roleNames.contains(UserRole.CHEF_DE_FAMILLE.name())) {
            roleNames.add(UserRole.CHEF_DE_FAMILLE.name());
        }
        // Ensure admin also has PASTEUR-level access
        if (user.getRole() == UserRole.ADMIN) {
            roleNames.add(UserRole.PASTEUR.name());
        }

        // Sync User entity roles set
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            Set<UserRole> roles = roleNames.stream()
                    .map(UserRole::valueOf)
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        // Set active role to default (highest priority) at every login.
        // FIX: un rôle actif obsolète (ex: FAISEUR persisté pour un compte
        // RESPONSABLE) provoquait une redirection de tous les menus vers le
        // mauvais espace métier. Le rôle actif repart toujours du rôle
        // prioritaire à chaque connexion ; l'utilisateur peut ensuite changer
        // de rôle via /auth/switch-role pendant sa session.
        user.setActiveRole(getDefaultActiveRole(user));
        userRepository.save(user);

        String activeRoleStr = user.getActiveRole().name();
        // G5.4 (§55) : le token porte le tenant ACTIF choisi (validé), sinon le tenant maison.
        java.util.UUID tokenTenant = activeTenantService.resolveTokenTenantId(user);
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.isEstChefDeFamille(), tokenTenant);
        UUID familyId = UUID.randomUUID();
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                tokenTenant, familyId);
        refreshTokenSessionService.register(
                refreshToken, user.getId(), familyId, jwtTokenProvider.getTokenExpiration(refreshToken));

        return new AuthResult(accessToken, refreshToken, user, activeRoleStr);
    }

    // ======================== ACTIVATION (US-02) ========================

    /**
     * Generate activation token and send welcome email
     */
    public void sendActivationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        String token = UUID.randomUUID().toString();
        ActivationToken activationToken = ActivationToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(Instant.now().plus(ACTIVATION_VALIDITY_HOURS, ChronoUnit.HOURS))
                .used(false)
                .build();
        activationTokenRepository.save(activationToken);

        String activationLink = frontendUrl + "/activate?token=" + token;
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), activationLink);
    }

    /**
     * Activate account using token
     */
    public void activateAccount(String token) {
        ActivationToken activationToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired activation token"));

        if (activationToken.isUsed()) {
            throw new BadCredentialsException("Activation token has already been used");
        }

        if (activationToken.isExpired()) {
            throw new BadCredentialsException("Activation token has expired. Please contact an administrator.");
        }

        User user = userRepository.findById(activationToken.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User", activationToken.getUserId()));

        user.setStatut(UserStatus.ACTIVE);
        userRepository.save(user);

        // §G3.1 — vérification (email) aboutie → inscription AUTOMATIQUE au
        // répertoire de l'église : compte + personne + membership (transaction
        // propre dans PeopleService, événement MemberRegistered publié).
        // Un doublon (même email/téléphone) n'est jamais recréé ; une échec du
        // répertoire ne bloque jamais l'activation du compte.
        if (user.getTenantId() != null) {
            try {
                com.discipolat.modules.people.domain.Person person =
                        com.discipolat.modules.people.domain.Person.builder()
                                .firstName(user.getFirstName() != null && !user.getFirstName().isBlank()
                                        ? user.getFirstName() : "Membre")
                                .lastName(user.getLastName() != null && !user.getLastName().isBlank()
                                        ? user.getLastName() : "")
                                .emailNormalized(user.getEmail())
                                .phoneNormalized(user.getPhone())
                                .build();
                peopleService.registerPerson(user.getTenantId(), person, "SELF_SIGNUP", user.getId());
            } catch (com.discipolat.modules.people.service.PeopleService.PersonAlreadyExistsException exists) {
                // Déjà au répertoire : on ne crée pas de fiche en double.
            } catch (RuntimeException directoryIssue) {
                // Journaliser sans faire échouer l'activation.
                org.slf4j.LoggerFactory.getLogger(AuthService.class)
                        .warn("Inscription répertoire impossible pour {}: {}",
                                user.getEmail(), directoryIssue.getMessage());
            }
        }

        activationToken.setUsed(true);
        activationTokenRepository.save(activationToken);
    }

    /**
     * Resend activation email
     */
    public void resendActivationEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("If the email exists, a new activation link has been sent."));

        if (user.getStatut() != UserStatus.PENDING_ACTIVATION) {
            throw new BadCredentialsException("Account is already activated");
        }

        // Invalidate old tokens
        activationTokenRepository.findByUserIdAndUsedFalse(user.getId())
                .ifPresent(oldToken -> {
                    oldToken.setUsed(true);
                    activationTokenRepository.save(oldToken);
                });

        sendActivationEmail(user.getId());
    }

    // ======================== PASSWORD RESET (US-03) ========================

    /**
     * Generate password reset token (valid 30 min)
     */
    public String generatePasswordResetToken(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return "If the email exists, a reset link has been sent.";
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(Instant.now().plus(PASSWORD_RESET_VALIDITY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        return "If the email exists, a reset link has been sent.";
    }

    /**
     * Reset password using the token (US-03)
     */
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new BadCredentialsException("Reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new BadCredentialsException("Reset token has expired. Please request a new password reset.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User", resetToken.getUserId()));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // ======================== TOKEN MANAGEMENT ========================

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResult refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        UUID userId = jwtTokenProvider.extractUserId(refreshToken);
        UUID familyId = jwtTokenProvider.extractRefreshFamilyId(refreshToken);
        if (familyId == null) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
        if (user.getStatut() != UserStatus.ACTIVE || user.isDeleted()) {
            throw new BadCredentialsException("User account is not active");
        }

        // B1 : un jeton rafraichi ne doit pas survivre a la suspension du tenant.
        // Place AVANT la consommation/rotation : un tenant suspendu ne consomme donc
        // pas sa famille de jetons, et aucun nouveau jeton n'est emis.
        tenantStatusGuard.assertAccessible(user.getTenantId());

        RefreshTokenSessionService.ConsumptionResult consumption = refreshTokenSessionService.consume(
                refreshToken, userId, familyId);
        if (consumption != RefreshTokenSessionService.ConsumptionResult.ROTATED) {
            throw new BadCredentialsException(consumption == RefreshTokenSessionService.ConsumptionResult.REUSE
                    ? "Refresh token reuse detected"
                    : "Refresh token has been revoked");
        }

        tokenRevocationService.revoke(refreshToken, "refresh",
                jwtTokenProvider.getTokenExpiration(refreshToken), "rotated");

        String activeRoleStr = user.getActiveRole() != null ? user.getActiveRole().name() : user.getRole().name();
        // G5.4 (§55) : le refresh NE DOIT PAS faire régresser le tenant actif choisi.
        java.util.UUID tokenTenant = activeTenantService.resolveTokenTenantId(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.isEstChefDeFamille(), tokenTenant);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                tokenTenant, familyId);
        refreshTokenSessionService.register(
                newRefreshToken, user.getId(), familyId, jwtTokenProvider.getTokenExpiration(newRefreshToken));

        return new AuthResult(newAccessToken, newRefreshToken, user, activeRoleStr);
    }

    public void logout(String refreshToken) {
        try {
            if (jwtTokenProvider.validateToken(refreshToken) && jwtTokenProvider.isRefreshToken(refreshToken)) {
                refreshTokenSessionService.revokeByToken(refreshToken);
                tokenRevocationService.revoke(refreshToken, "refresh",
                        jwtTokenProvider.getTokenExpiration(refreshToken), "logout");
            }
        } catch (Exception ignored) {
        }
    }

    public AuthResult getCurrentUser() {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
        return new AuthResult(null, null, user);
    }

    /**
     * Change password for authenticated user
     */
    public void changePassword(String currentPassword, String newPassword) {
        UUID userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ======================== ROLE SWITCHING ========================

    /**
     * Switch the active role for the current user.
     * Returns a new access token with the updated active role.
     */
    public AuthResult switchActiveRole(UUID userId, UserRole newActiveRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (!user.getRoles().contains(newActiveRole)) {
            throw new BusinessRuleException("User does not have the role: " + newActiveRole,
                    "INVALID_ROLE");
        }

        user.setActiveRole(newActiveRole);
        userRepository.save(user);

        String activeRoleStr = newActiveRole.name();
        // G5.4 (§55) : changement de rôle ≠ retour au tenant maison — conserver le tenant actif.
        java.util.UUID tokenTenant = activeTenantService.resolveTokenTenantId(user);
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.isEstChefDeFamille(), tokenTenant);
        UUID familyId = UUID.randomUUID();
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), activeRoleStr,
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                tokenTenant, familyId);
        refreshTokenSessionService.register(
                refreshToken, user.getId(), familyId, jwtTokenProvider.getTokenExpiration(refreshToken));

        return new AuthResult(accessToken, refreshToken, user, activeRoleStr);
    }

    /**
     * Returns the default active role based on priority.
     * Priority: ADMIN > PASTEUR > RESPONSABLE > CHEF_DE_FAMILLE > FAISEUR > MEMBRE
     */
    /**
     * Returns the current authenticated user's ID
     */
    public UUID getCurrentUserId() {
        return securityUtils.getCurrentUserId();
    }

    private UserRole getDefaultActiveRole(User user) {
        List<UserRole> priority = List.of(
                UserRole.ADMIN,
                UserRole.PASTEUR,
                UserRole.RESPONSABLE,
                UserRole.CHEF_DE_FAMILLE,
                UserRole.FAISEUR,
                UserRole.MEMBRE
        );
        for (UserRole role : priority) {
            if (user.getRoles() != null && user.getRoles().contains(role)) {
                return role;
            }
        }
        return user.getRole();
    }

    // ======================== MAGIC LINK ========================

    private final java.util.concurrent.ConcurrentHashMap<String, MagicLinkEntry> magicLinks = new java.util.concurrent.ConcurrentHashMap<>();

    /** Génère un token magic link valide 15 minutes. */
    public String generateMagicLink(String email) {
        String token = java.util.UUID.randomUUID().toString();
        magicLinks.put(token, new MagicLinkEntry(email, java.time.LocalDateTime.now().plusMinutes(15)));
        return token;
    }

    /** Vérifie et consomme un magic link. */
    public User verifyMagicLink(String token) {
        MagicLinkEntry entry = magicLinks.remove(token);
        if (entry == null || entry.expiresAt.isBefore(java.time.LocalDateTime.now())) {
            // A15 : BusinessRuleException suit la convention (message, code).
            // Les arguments étaient INVERSÉS : le codeFrançais partait dans le
            // `detail` de la réponse et le message technique dans le `title`
            // (donc dans le champ `title` du ProblemDetail, lu par les clients).
            throw new com.discipolat.common.domain.BusinessRuleException(
                    "Lien magique invalide ou expiré", "MAGIC_LINK_EXPIRED");
        }
        return userRepository.findByEmailIgnoreCase(entry.email)
                .orElseThrow(() -> new com.discipolat.common.domain.BusinessRuleException(
                        "Aucun compte associé à cet email", "USER_NOT_FOUND"));
    }

    /** Envoie le magic link par email. */
    public void sendMagicLinkEmail(String email, String token) {
        String link = frontendUrl + "/auth/magic-link?token=" + token;
        String subject = "Connexion rapide à Discipolat";
        String body = "Bonjour,\n\nCliquez sur ce lien pour vous connecter (valable 15 min) :\n\n"
                + link + "\n\nSi vous n'avez pas demandé ce lien, ignorez ce message.";
        try {
            emailService.send(email, subject, body);
        } catch (Exception e) {
            log.warn("Failed to send magic link email to {}: {}", email, e.getMessage());
        }
    }

    private record MagicLinkEntry(String email, java.time.LocalDateTime expiresAt) {}
}
