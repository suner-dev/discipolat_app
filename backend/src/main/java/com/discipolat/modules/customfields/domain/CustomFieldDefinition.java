package com.discipolat.modules.customfields.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "custom_field_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class CustomFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "entite_type", nullable = false, length = 50)
    private String entiteType;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "obligatoire", nullable = false)
    @Builder.Default
    private boolean obligatoire = false;

    @Column(name = "ordre", nullable = false)
    @Builder.Default
    private int ordre = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<String> options;

    @Column(name = "placeholder")
    private String placeholder;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles_lecture", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> rolesLecture = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles_ecriture", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> rolesEcriture = new ArrayList<>();

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}