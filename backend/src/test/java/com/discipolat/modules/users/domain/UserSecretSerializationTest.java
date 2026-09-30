package com.discipolat.modules.users.domain;

import com.discipolat.common.domain.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Non-régression du lot sécurité « secrets 2FA en WRITE_ONLY » (campagne hors
 * périmètre onboarding, récupérée dans {@code main} — TODO reprise §0.2/§1.2).
 *
 * <p>Ce qui est verrouillé ici n'est pas l'annotation sur le champ nu — elle
 * est déjà testée implicitement par son existence — mais le comportement dans
 * les <b>serialisations imbriquées</b> : l'entité {@code User} est rendue
 * brute dans des réponses de santé et de transfers, embarquée dans d'autres
 * objets. Un {@code WRITE_ONLY} positionné trop tard, retiré par un refactor
 * (Lombok, nouveau mixIn, {@code @JsonView}) ferait sortir
 * {@code two_factor_secret} sans aucun test rouge. Les trois formes ci-dessous
 * (entité seule, entité dans une Map — le cas « réponse brute » réel —, entité
 * dans une collection) couvrent les chemins Jackson qui contournent
 * historiquement ce genre de garde.
 */
class UserSecretSerializationTest {

    private static final String SECRET = "JBSWY3DPEHPK3PXP-TOTP-SECRET";
    private static final String BACKUP_CODES = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User userWithSecrets() {
        return User.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .email("owner@eglise.test")
                .passwordHash("$2a$10$hash")
                .firstName("Paul")
                .lastName("Chedi")
                .role(UserRole.PASTEUR)
                .roles(Set.of(UserRole.PASTEUR))
                .statut(UserStatus.ACTIVE)
                .twoFactorEnabled(true)
                .twoFactorSecret(SECRET)
                .twoFactorBackupCodes(BACKUP_CODES)
                .build();
    }

    @Test
    @DisplayName("User sérialisé seul : aucun secret 2FA ni hash de mot de passe ne sortent")
    void userAloneLeaksNoSecret() throws Exception {
        String json = objectMapper.writeValueAsString(userWithSecrets());

        assertThat(json).doesNotContain(SECRET);
        assertThat(json).doesNotContain(BACKUP_CODES);
        assertThat(json).doesNotContain("$2a$10$hash");
        assertThat(json).doesNotContain("two_factor_secret");
        // Nom Jackson du champ (camelCase) : c'est la clé qui apparaîtrait.
        assertThat(json).doesNotContain("\"twoFactorSecret\"");
        assertThat(json).doesNotContain("\"twoFactorBackupCodes\"");
        assertThat(json).doesNotContain("\"passwordHash\"");
    }

    @Test
    @DisplayName("User imbriqué dans une Map (réponses brutes santé/transfers) : toujours aucun secret")
    void nestedUserInMapLeaksNoSecret() throws Exception {
        Map<String, Object> rawResponse = Map.of(
                "status", "UP",
                "responsable", userWithSecrets());

        String json = objectMapper.writeValueAsString(rawResponse);

        assertThat(json).doesNotContain(SECRET);
        assertThat(json).doesNotContain(BACKUP_CODES);
        assertThat(json).doesNotContain("\"twoFactorSecret\"");
        assertThat(json).doesNotContain("\"twoFactorBackupCodes\"");
    }

    @Test
    @DisplayName("User dans une collection (listés bruts) : toujours aucun secret")
    void userInCollectionLeaksNoSecret() throws Exception {
        String json = objectMapper.writeValueAsString(
                Map.of("members", java.util.List.of(userWithSecrets(), userWithSecrets())));

        assertThat(json).doesNotContain(SECRET);
        assertThat(json).doesNotContain("\"twoFactorSecret\"");
    }
}
