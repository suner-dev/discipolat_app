package com.discipolat.modules.authentication.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /**
     * §G6.4 — l'envoi SMTP se fait hors du thread de requête HTTP : sans cela,
     * un serveur mail injoignable (ex : localhost:1025 en local/CI) bloque la
     * réponse d'inscription/invitation une trentaine de secondes et fait
     * expirer les parcours E2E. Les logs d'échec sont conservés.
     */
    private static final ExecutorService MAIL_EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "email-sender");
        t.setDaemon(true);
        return t;
    });

    private final JavaMailSender mailSender;
    private final String fromAddress;
    /** §G6.4 — MAIL_ENABLED=false (dev/E2E) : plus aucune connexion SMTP, tracé seul. */
    private final boolean enabled;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:noreply@discipolat.com}") String fromAddress,
                        @Value("${app.email.enabled:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.enabled = enabled;
    }

    /**
     * US-02: Send welcome email with activation link
     */
    public void sendWelcomeEmail(String to, String firstName, String activationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Bienvenue sur Discipolat - Activez votre compte");
        message.setText(String.format(
                "Bonjour %s,\n\n" +
                "Bienvenue sur la plateforme Discipolat ! Votre compte a été créé avec succès.\n\n" +
                "Pour activer votre compte et définir votre mot de passe, veuillez cliquer sur le lien suivant :\n%s\n\n" +
                "Ce lien est valable 48 heures.\n\n" +
                "Si vous n'avez pas demandé la création de ce compte, veuillez ignorer cet email.\n\n" +
                "Cordialement,\nL'équipe Discipolat",
                firstName, activationLink));
        dispatch(message, "Welcome");
    }

    /**
     * US-03: Send password reset email
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject("Réinitialisation de votre mot de passe Discipolat");
        message.setText(String.format(
                "Bonjour,\n\n" +
                "Vous avez demandé la réinitialisation de votre mot de passe.\n\n" +
                "Cliquez sur le lien suivant pour définir un nouveau mot de passe :\n%s\n\n" +
                "Ce lien est valable 30 minutes.\n\n" +
                "Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email.\n\n" +
                "Cordialement,\nL'équipe Discipolat",
                resetLink));
        dispatch(message, "Password reset");
    }

    /** Envoie un email simple (utilisé par le magic link & flow OAuth). */
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        dispatch(message, "Email");
    }

    private void dispatch(SimpleMailMessage message, String label) {
        String to = message.getTo() != null && message.getTo().length > 0 ? message.getTo()[0] : "?";
        if (!enabled) {
            log.info("{} not sent (app.email.enabled=false) to: {}", label, to);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                mailSender.send(message);
                log.info("{} sent to: {}", label, to);
            } catch (Exception e) {
                log.error("Failed to send {} to {}: {}", label.toLowerCase(), to, e.getMessage());
            }
        }, MAIL_EXECUTOR);
    }

    // ==================================================================
    // Inscription publique d'une église (constat M2, contrat §3.3)
    // ==================================================================
    //
    // Les trois méthodes ci-dessous renvoient un `boolean` et ne lèvent JAMAIS
    // (décision D10) : un SMTP non configuré ne doit pas faire échouer une
    // transaction métier, il doit être signalé par `false` et journalisé.
    // L'exigence de retour `boolean` est explicite dans le plan §4 A6.

    /** Accusé de réception à la soumission d'une demande d'inscription. */
    public boolean sendRegistrationReceived(String to, String firstName) {
        String subject = "Discipolat — votre demande d'inscription a bien été reçue";
        String body = String.format(
                "Bonjour %s,\n\n"
                        + "Nous avons bien reçu votre demande d'inscription pour votre église.\n\n"
                        + "Notre équipe l'examine avant de vous donner accès. Vous recevrez un email "
                        + "dès qu'une décision sera prise. Vous pouvez consulter l'avancement à tout moment "
                        + "depuis la page « Suivre ma demande » avec cette adresse email.\n\n"
                        + "Cordialement,\nL'équipe Discipolat",
                firstName);
        return sendTracked(to, subject, body, "registration_received");
    }

    /** Notification d'approbation, avec le lien de connexion. */
    public boolean sendRegistrationApproved(String to, String firstName, String loginUrl) {
        String subject = "Discipolat — votre église est approuvée, vous pouvez vous connecter";
        String body = String.format(
                "Bonjour %s,\n\n"
                        + "Bonne nouvelle : votre demande d'inscription a été approuvée. "
                        + "Votre église est prête.\n\n"
                        + "Connectez-vous avec cette adresse email pour commencer la configuration :\n%s\n\n"
                        + "Après connexion, nous vous proposerons les 7 étapes de configuration.\n\n"
                        + "Cordialement,\nL'équipe Discipolat",
                firstName, loginUrl);
        return sendTracked(to, subject, body, "registration_approved");
    }

    /** Notification de rejet, avec le motif communiqué par le Super Admin. */
    public boolean sendRegistrationRejected(String to, String firstName, String reason) {
        String subject = "Discipolat — votre demande d'inscription n'a pas pu être retenue";
        String body = String.format(
                "Bonjour %s,\n\n"
                        + "Votre demande d'inscription n'a pas pu être retenue.\n\n"
                        + "Motif communiqué par notre équipe :\n%s\n\n"
                        + "Si vous pensez qu'il s'agit d'une erreur, répondez simplement à cet email.\n\n"
                        + "Cordialement,\nL'équipe Discipolat",
                firstName, reason == null || reason.isBlank() ? "Non précisé." : reason);
        return sendTracked(to, subject, body, "registration_rejected");
    }

    /**
     * Envoi générique journalisé : renvoie {@code true} si l'envoi a réussi,
     * {@code false} sinon, sans jamais propager l'exception.
     */
    private boolean sendTracked(String to, String subject, String body, String label) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email {} envoyé à {}", label, to);
            return true;
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email {} à {} : {}", label, to, e.getMessage());
            return false;
        }
    }

    // ==================================================================
    // Invitations (constat M4) — bienvenue et relances
    // ==================================================================

    /**
     * Email de bienvenue envoyé APRÈS acceptation d'une invitation.
     *
     * <p>Avant le correctif, l'acceptation d'une invitation ne produisait AUCUN
     * email : l'invité découvrait son nouveau compte sans aucune confirmation.
     */
    public boolean sendInvitationWelcome(String to, String firstName, String churchName, String loginUrl) {
        String subject = "Bienvenue dans " + (churchName == null || churchName.isBlank()
                ? "votre église" : churchName);
        String body = String.format(
                "Bonjour %s,\n\n"
                        + "Votre invitation a été acceptée : votre compte est prêt.\n\n"
                        + "Connectez-vous dès maintenant pour commencer la configuration de votre église :\n%s\n\n"
                        + "Au programme : l'identité de l'église, vos départements et vos familles, "
                        + "les responsables à inviter, votre identité visuelle, vos modules et votre premier événement.\n\n"
                        + "Cordialement,\nL'équipe Discipolat",
                firstName == null || firstName.isBlank() ? "bienvenue" : firstName,
                loginUrl);
        return sendTracked(to, subject, body, "invitation_welcome");
    }

    /**
     * Relance automatique d'une invitation en attente (constat M4).
     *
     * @param daysLeft jours restant avant expiration de l'invitation (3 ou 1)
     */
    public boolean sendInvitationReminder(String to, String firstName, String churchName,
                                           String invitationLink, int daysLeft) {
        String subject = "Votre invitation à rejoindre "
                + (churchName == null || churchName.isBlank() ? "une église" : churchName)
                + " expire bientôt";
        String body = String.format(
                "Bonjour %s,\n\n"
                        + "Vous avez été invité(e) à rejoindre %s, et votre invitation n'a pas encore été acceptée.\n\n"
                        + "Elle expire dans %d jour(s). Si vous souhaitez rejoindre cette église, "
                        + "acceptez votre invitation :\n%s\n\n"
                        + "Si vous ne connaissez pas cette église, vous pouvez ignorer ce message.\n\n"
                        + "Cordialement,\nL'équipe Discipolat",
                firstName == null || firstName.isBlank() ? "bonjour" : firstName,
                churchName == null || churchName.isBlank() ? "cette église" : churchName,
                Math.max(1, daysLeft),
                invitationLink);
        return sendTracked(to, subject, body, "invitation_reminder_d" + daysLeft);
    }
}
