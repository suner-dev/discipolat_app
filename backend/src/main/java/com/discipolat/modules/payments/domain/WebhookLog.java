package com.discipolat.modules.payments.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Log de livraison webhook — enregistre chaque callback reçu d'un opérateur.
 *
 * <p>Utilisé pour l'audit trail, le debugging et la surveillance des paiements.
 * Tous les champs sont nullable sauf les identifiants et le statut.</p>
 */
@Entity
@Table(name = "webhook_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Opérateur source du callback (M_PESA, MTN_MOMO, ORANGE_MONEY, GENERIC). */
    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    /** Endpoint reçu (/webhooks/mpesa, /webhooks/orange, etc.). */
    @Column(name = "endpoint", nullable = false, length = 100)
    private String endpoint;

    /** IP source du callback (X-Forwarded-For ou remoteAddr). */
    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    /** Code HTTP retourné (200, 403, 500…). */
    @Column(name = "status_code")
    private Integer statusCode;

    /** Statut du traitement : RECEIVED → VERIFIED → PROCESSED | REJECTED | ERROR. */
    @Column(name = "status_label", nullable = false, length = 20)
    @Builder.Default
    private String statusLabel = "RECEIVED";

    /** Référence opérateur (CheckoutRequestID, pay_token, etc.). */
    @Column(name = "reference", length = 100)
    private String reference;

    /** ID de la PaymentIntent traitée (si trouvée). */
    @Column(name = "payment_id")
    private UUID paymentId;

    /** La vérification HMAC a-t-elle réussi ? null si non vérifié. */
    @Column(name = "signature_valid")
    private Boolean signatureValid;

    /** Headers pertinents de la requête (sanitisés — sans secrets). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", columnDefinition = "jsonb")
    private Map<String, String> requestHeaders;

    /** Body brut de la requête (peut être tronqué en production). */
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    /** Réponse envoyée par le serveur. */
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    /** Message d'erreur si le traitement a échoué. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Temps de traitement en millisecondes. */
    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
