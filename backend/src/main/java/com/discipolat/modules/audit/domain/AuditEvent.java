package com.discipolat.modules.audit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_event", indexes = {
    @Index(name = "idx_audit_event_tenant_entity", columnList = "tenant_id, entity, entity_id, timestamp"),
    @Index(name = "idx_audit_event_actor", columnList = "actor_id"),
    @Index(name = "idx_audit_event_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_event_hash", columnList = "hash")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity", nullable = false, length = 100)
    private String entity;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value_json", columnDefinition = "jsonb")
    private Map<String, Object> oldValueJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value_json", columnDefinition = "jsonb")
    private Map<String, Object> newValueJson;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "prev_hash", length = 64)
    private String prevHash;

    @Column(name = "hash", nullable = false, length = 64)
    private String hash;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;
}