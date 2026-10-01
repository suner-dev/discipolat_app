package com.discipolat.modules.users.repository;

import com.discipolat.modules.authentication.domain.SocialProvider;
import com.discipolat.modules.users.domain.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux identités de connexion externes (table {@code user_identities}).
 *
 * <p>Dépôt volontairement NON annoté {@code tenantFilter} : voir
 * {@link UserIdentity} pour la raison (l'identité est globale au compte).
 */
public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {

    /**
     * Résolution principale d'un login social : {@code (provider, subject)} est
     * la seule clé fiable — l'email du fournisseur n'est pas disponible de façon
     * stable (Apple ne le renvoie qu'à la première autorisation).
     */
    Optional<UserIdentity> findByProviderAndSubject(SocialProvider provider, String subject);

    /** Identités d'un compte, pour l'écran « mon compte » (aucune donnée sensible). */
    List<UserIdentity> findByUserIdOrderByCreatedAtAsc(UUID userId);

    boolean existsByUserIdAndProvider(UUID userId, SocialProvider provider);
}
