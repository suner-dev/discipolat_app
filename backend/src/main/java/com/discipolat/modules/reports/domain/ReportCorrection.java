package com.discipolat.modules.reports.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "report_corrections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Column(name = "corrected_by", nullable = false)
    private UUID correctedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ancienne_valeur", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> ancienneValeur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelle_valeur", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> nouvelleValeur;

    @Column(name = "raison", nullable = false)
    private String raison;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
