package com.discipolat.modules.notifications.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * P0 — Configuration déclarative de la chaîne push mobile.
 *
 * <p>Avant cette classe, l'application mobile s'abonnait à
 * {@code firebase_messaging} sans qu'aucune clé ne permette de brancher un
 * compte de service : la feature était une promesse non tenue. Ce bloc rend
 * l'état réel vérifiable et l'expose via
 * {@code GET /api/v1/notifications/push-status}.</p>
 *
 * <p>Règles d'honnêteté appliquées :</p>
 * <ul>
 *   <li><b>Désactivé par défaut</b> ({@code enabled=false}) : la passerelle
 *       active est alors {@link NoOpPushGateway}, qui journalise l'abandon des
 *       envois. Le démarrage ne casse jamais.</li>
 *   <li><b>Simulation par défaut</b> ({@code dry-run=true}) : la charge utile
 *       est journalisée, aucun appel réseau n'est effectué. Passer à
 *       {@code false} exige un fichier de compte de service valide.</li>
 *   <li><b>Aucun secret ici</b> : seul un <i>chemin de fichier</i> est stocké.
 *       Le contenu du compte de service n'est ni lu en variable
 *       d'environnement, ni journalisé, ni exposé par
 *       {@link #reason()}.</li>
 * </ul>
 *
 * <p>Enregistrement : {@link PushGatewayConfiguration}.</p>
 */
@ConfigurationProperties(prefix = "app.push")
public class PushProperties {

    /** Bascule générale : autoriser un envoi push réel. */
    private boolean enabled = false;

    /**
     * Chemin du fichier JSON de compte de service Firebase.
     * Vide = « non configuré ». Jamais le JSON lui-même.
     */
    private String credentialsPath = "";

    /** Simulation : journalise la charge utile sans contacter FCM. */
    private boolean dryRun = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public void setCredentialsPath(String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Un compte de service est-il réellement exploitable ? Le fichier doit
     * exister et être lisible : c'est le seul contrôle possible sans l'ouvrir.
     */
    public boolean isConfigured() {
        return credentialsFile() != null;
    }

    /**
     * Fichier de compte de service résolu, ou {@code null} si la configuration
     * est absente, illisible ou pointe dans le vide.
     */
    @Nullable
    public Path credentialsFile() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            return null;
        }
        try {
            Path path = Path.of(credentialsPath.trim());
            return Files.isRegularFile(path) && Files.isReadable(path) ? path : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /**
     * Explication précise de l'absence d'envoi, ou chaîne vide si l'envoi réel
     * est possible. Ne contient jamais le contenu du compte de service.
     */
    public String reason() {
        if (!enabled) {
            return "Notifications push désactivées : app.push.enabled=false. "
                    + "Aucun push n'est envoyé, seules les notifications in-app sont créées.";
        }
        if (credentialsPath == null || credentialsPath.isBlank()) {
            return "Notifications push activées mais app.push.credentials-path est vide : "
                    + "aucun compte de service Firebase n'est renseigné.";
        }
        if (credentialsFile() == null) {
            return "Notifications push activées mais le fichier de compte de service est introuvable "
                    + "ou illisible : " + credentialsPath;
        }
        if (dryRun) {
            return "Notifications push activées en mode simulation : app.push.dry-run=true. "
                    + "Les charges utiles sont journalisées, aucun envoi réel n'est effectué.";
        }
        return "";
    }

    /**
     * Représentation sûre : seul le chemin est exposé, et il ne contient pas
     * le secret lui-même (qui vit dans le fichier).
     */
    @Override
    public String toString() {
        return "PushProperties{enabled=" + enabled
                + ", credentialsPath='" + credentialsPath + "'"
                + ", dryRun=" + dryRun
                + ", configured=" + isConfigured() + '}';
    }
}
