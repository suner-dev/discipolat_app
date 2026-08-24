package com.discipolat.modules.whatsapp.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_messages", indexes = {
        @Index(name = "idx_wa_messages_tenant_created", columnList = "tenant_id, createdAt"),
        @Index(name = "idx_wa_messages_phone", columnList = "tenant_id, phoneNumber")
})

@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
public class WhatsAppMessage {

    public enum Direction { INBOUND, OUTBOUND }
    public enum Status { RECEIVED, QUEUED, SENT, DELIVERED, READ, FAILED }
    public enum Kind { TEXT, TEMPLATE, COMMAND, BROADCAST, REMINDER }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private Direction direction;

    /** Numéro de téléphone au format international sans +. */
    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "wa_message_id", length = 128)
    private String waMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private Kind kind = Kind.TEXT;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** Référence optionnelle vers l'entité liée (annonce, événement…). */
    @Column(name = "reference_type", length = 64)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    /** Membre lié si le numéro correspond à un compte connu. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
