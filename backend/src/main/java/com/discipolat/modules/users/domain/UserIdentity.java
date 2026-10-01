package com.discipolat.modules.users.domain;

import com.discipolat.modules.authentication.domain.SocialProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Identité de connexion externe rattachée à un compte Discipolat
 * (migration {@code V217__user_identities.sql} — renumérotée depuis V206, collision avec le port Develop1).
 *
 * <p><b>Ce n'est PAS une entité tenant.</b> Elle ne porte ni {@code tenant_id}
 * ni filtre Hibernate {@code tenantFilter}, à dessein : l'identité appartient
 * au COMPTE, et un compte peut être membre de plusieurs églises
 * ({@code tenant_membership}). Un filtre multi-tenant rendrait une identité
 * invisible depuis une autre église et ferait échouer le reconnectement social
 * d'un membre multi-église. Toutes les requêtes partent d'un {@code user_id}
 * déjà résolu — aucune fuite inter-tenant n'est donc possible.
 *
 * <p>Clé métier : {@code (provider, subject)}. Jamais l'email, parce qu'Apple ne
 * restitue l'email qu'à la première autorisation.
 */
@Entity
@Table(name = "user_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_identities_provider_subject",
                columnNames = {"provider", "subject"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    /** Claim {@code sub} du fournisseur : identifiant opaque, seule clé d'authentification. */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /** Email vérifié par le fournisseur AU MOMENT du linkage — traçabilité RGPD, jamais une clé. */
    @Column(name = "email_at_link", length = 255)
    private String emailAtLink;

    @Column(name = "picture_url", length = 512)
    private String pictureUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Enregistre une connexion réussie par cette identité (traçabilité, pas sécurité). */
    public void touchLogin() {
        this.lastLoginAt = Instant.now();
    }
}
