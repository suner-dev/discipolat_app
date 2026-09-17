package com.discipolat.modules.audit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "business_history", indexes = {
    @Index(name = "idx_biz_hist_object", columnList = "object_type, object_id, happened_at"),
    @Index(name = "idx_biz_hist_space", columnList = "space_id, happened_at"),
    @Index(name = "idx_biz_hist_tenant", columnList = "tenant_id, happened_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "object_type", nullable = false, length = 100)
    private String objectType;

    @Column(name = "object_id", nullable = false)
    private UUID objectId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json", columnDefinition = "jsonb")
    private Map<String, Object> detailJson;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role", length = 80)
    private String actorRole;

    @Column(name = "space_id")
    private UUID spaceId;

    @Column(name = "happened_at", nullable = false)
    private OffsetDateTime happenedAt;
}