package com.discipolat.modules.authentication.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:noreply@discipolat.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * US-02: Send welcome email with activation link
     */
    public void sendWelcomeEmail(String to, String firstName, String activationLink) {
        try {
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
            mailSender.send(message);
            log.info("Welcome email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * US-03: Send password reset email
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
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
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }
}
