package com.discipolat.modules.audit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entite_type", nullable = false)
    private String entiteType;

    @Column(name = "entite_id")
    private UUID entiteId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ancien_valeur", columnDefinition = "jsonb")
    private Map<String, Object> ancienValeur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelle_valeur", columnDefinition = "jsonb")
    private Map<String, Object> nouvelleValeur;

    @Column(name = "adresse_ip")
    private String adresseIp;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
