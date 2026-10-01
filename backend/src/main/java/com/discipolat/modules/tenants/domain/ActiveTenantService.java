package com.discipolat.modules.tenants.domain;

import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * G5.4 (§55) — Résolution et bascule du tenant ACTIF d'un utilisateur.
 *
 * Le tenant est porté par le JWT (claim {@code tenantId}) et relu à chaque
 * requête par le TenantInterceptor : une bascule qui ne faisait que toucher
 * le ThreadLocal ne survivait pas à la requête suivante. Ce service rend la
 * bascule RÉELLE :
 * <ol>
 *   <li>validation serveur de l'adhésion ACTIVE au tenant cible (§0.3 —
 *       jamais le frontend n'est cru sur parole) ;</li>
 *   <li>persistance du choix ({@code users.active_tenant_id}) ;</li>
 *   <li>émission d'une nouvelle paire de tokens portant le nouveau claim
 *       {@code tenantId} — le refresh ultérieur conserve le tenant choisi
 *       tant que l'adhésion reste ACTIVE, sinon retour au tenant maison.</li>
 * </ol>
 */
@Service
public class ActiveTenantService {

    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public ActiveTenantService(TenantMembershipRepository membershipRepository,
                               UserRepository userRepository,
                               JwtTokenProvider jwtTokenProvider) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** Résultat d'une bascule réussie : tokens à remplacer côté client. */
    public record SwitchOutcome(UUID tenantId, String accessToken, String refreshToken, String role) {}

    /**
     * Tenant à embarquer dans les tokens d'un utilisateur : son tenant actif
     * sélectionné s'il reste une adhésion ACTIVE, sinon son tenant maison
     * (le choix obsolète est nettoyé).
     */
    @Transactional
    public UUID resolveTokenTenantId(User user) {
        UUID active = user.getActiveTenantId();
        if (active == null || active.equals(user.getTenantId())) {
            return user.getTenantId();
        }
        boolean stillValid = membershipRepository
                .existsByUserIdAndTenantIdAndStatus(user.getId(), active, MembershipStatus.ACTIVE);
        if (!stillValid) {
            user.setActiveTenantId(null);
            userRepository.save(user);
            return user.getTenantId();
        }
        return active;
    }

    /**
     * Persiste le choix de tenant actif SANS réémettre de jeton : pour les flux
     * qui réémettent déjà eux-mêmes leur couple accessToken/refreshToken (bascule
     * de {@code TenantSwitcherController}, qui passe par la famille de sessions
     * refresh-token de main). L'appelant a validé l'adhésion ACTIVE et le statut
     * du tenant ; la lecture de l'utilisateur doit rester faite sur son tenant
     * d'origine, donc AVANT tout changement de TenantContext.
     */
    @Transactional
    public void markActiveTenant(User user, UUID tenantId) {
        if (user == null || tenantId == null) {
            return;
        }
        user.setActiveTenantId(tenantId);
        userRepository.save(user);
    }

    /**
     * Bascule le tenant actif de l'utilisateur et réémet les tokens.
     *
     * @throws AccessDeniedException si l'utilisateur n'a pas d'adhésion ACTIVE au tenant cible
     */
    @Transactional
    public SwitchOutcome switchTenant(UUID userId, UUID newTenantId) {
        if (newTenantId == null) {
            throw new IllegalArgumentException("tenantId requis");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        boolean hasAccess = membershipRepository
                .existsByUserIdAndTenantIdAndStatus(userId, newTenantId, MembershipStatus.ACTIVE);
        if (!hasAccess) {
            throw new AccessDeniedException("Accès non autorisé à ce tenant");
        }

        user.setActiveTenantId(newTenantId);
        userRepository.save(user);

        String activeRoleStr = user.getActiveRole() != null
                ? user.getActiveRole().name() : user.getRole().name();
        Set<String> roleNames = (user.getRoles() == null || user.getRoles().isEmpty())
                ? Set.of(user.getRole().name())
                : user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), activeRoleStr, roleNames,
                user.isEstChefDeFamille(), newTenantId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getEmail(), activeRoleStr, roleNames, newTenantId);

        return new SwitchOutcome(newTenantId, accessToken, refreshToken, activeRoleStr);
    }
}
