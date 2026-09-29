package com.discipolat.modules.notifications.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0 — Règle de sélection de la passerelle push.
 *
 * <p>Aucun appel réseau ici : {@link FirebaseAdminPushGateway} initialise
 * Firebase paresseusement, au premier envoi réel. Seule l'absence de
 * passerelle silencieuse est vérifiée.</p>
 */
class PushGatewaySelectionTest {

    @Test
    void selectsNoOpGatewayWhenPushIsDisabled() {
        PushProperties properties = new PushProperties();
        properties.setEnabled(false);
        properties.setCredentialsPath("/tmp/does-not-matter.json");

        PushGateway gateway = PushGatewayConfiguration.select(properties);

        assertThat(gateway).isInstanceOf(NoOpPushGateway.class);
        assertThat(((NoOpPushGateway) gateway).reason())
                .contains("app.push.enabled=false")
                .contains("Aucun push n'est envoyé");
    }

    @Test
    void selectsNoOpGatewayWhenEnabledButCredentialsAreMissing() {
        PushProperties properties = new PushProperties();
        properties.setEnabled(true);
        properties.setCredentialsPath("");
        properties.setDryRun(false);

        PushGateway gateway = PushGatewayConfiguration.select(properties);

        assertThat(gateway).isInstanceOf(NoOpPushGateway.class);
        assertThat(((NoOpPushGateway) gateway).reason())
                .contains("app.push.credentials-path est vide");
    }

    @Test
    void selectsNoOpGatewayWhenCredentialsFileIsMissingFromDisk() {
        PushProperties properties = new PushProperties();
        properties.setEnabled(true);
        properties.setCredentialsPath("/tmp/discipolat-credentials-absentes.json");
        properties.setDryRun(false);

        assertThat(properties.isConfigured()).isFalse();
        assertThat(PushGatewayConfiguration.select(properties)).isInstanceOf(NoOpPushGateway.class);
    }

    @Test
    void selectsFirebaseGatewayWhenEnabledAndConfigured(@TempDir Path tempDir) throws IOException {
        Path credentials = Files.createFile(tempDir.resolve("service-account.json"));
        PushProperties properties = new PushProperties();
        properties.setEnabled(true);
        properties.setCredentialsPath(credentials.toString());
        properties.setDryRun(true);

        PushGateway gateway = PushGatewayConfiguration.select(properties);

        assertThat(gateway).isInstanceOf(FirebaseAdminPushGateway.class);
        assertThat(properties.isConfigured()).isTrue();
    }

    @Test
    void reportsDryRunAsAnExplicitNonOperationalState(@TempDir Path tempDir) throws IOException {
        Path credentials = Files.createFile(tempDir.resolve("service-account.json"));
        PushProperties properties = new PushProperties();
        properties.setEnabled(true);
        properties.setCredentialsPath(credentials.toString());
        properties.setDryRun(true);

        assertThat(properties.reason())
                .contains("app.push.dry-run=true")
                .contains("aucun envoi réel");
    }

    @Test
    void dryRunGatewayNeverTouchesFirebaseAndReportsNoDelivery(@TempDir Path tempDir) throws IOException {
        Path credentials = Files.createFile(tempDir.resolve("service-account.json"));
        PushProperties properties = new PushProperties();
        properties.setEnabled(true);
        properties.setCredentialsPath(credentials.toString());
        properties.setDryRun(true);
        FirebaseAdminPushGateway gateway = new FirebaseAdminPushGateway(properties);

        // Fichier volontairement vide : si la gateway Ouvrait le compte de service,
        // ce test échouerait au lieu de passer en silence.
        PushResult result = gateway.send(List.of("token-1", "token-2"),
                new PushMessage("Titre", "Corps", java.util.Map.of("eventType", "TaskAssigned")));

        assertThat(result.success()).isTrue();
        assertThat(result.sentCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
    }

    @Test
    void noOpGatewayDropsEverySendAndStaysQuietAboutIt() {
        NoOpPushGateway gateway = new NoOpPushGateway("test");

        PushResult result = gateway.send(List.of("token-1"), new PushMessage("Titre", "Corps"));

        assertThat(result.success()).isFalse();
        assertThat(result.sentCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
        assertThat(gateway.reason()).isEqualTo("test");
    }

    @Test
    void noOpGatewayReturnsNothingToSendForAnEmptyDeviceList() {
        assertThat(new NoOpPushGateway("test").send(List.of(), new PushMessage("T", "B")).success()).isFalse();
        assertThat(new NoOpPushGateway("test").send(null, new PushMessage("T", "B")).success()).isFalse();
    }
}
