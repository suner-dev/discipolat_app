package com.discipolat.modules.departments.domain;

import com.discipolat.modules.scoping.domain.ResourceScope;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Document du département : procédures, guides, documents, formulaires,
 * comptes rendus et ressources, avec lien externe optionnel.
 */
@Entity
@Table(name = "department_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class DepartmentDocument {

    public enum DocumentType {
        PROCEDURE, GUIDE, DOCUMENT, FORMULAIRE, COMPTE_RENDU, RESSOURCE
    }

    public enum DocumentStatus {
        ACTIF, ARCHIVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    // G1.8 §54 — Resource scoping
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_scope", nullable = false, length = 20)
    @Builder.Default
    private ResourceScope resourceScope = ResourceScope.TENANT_GLOBAL;

    @Column(name = "organization_unit_id")
    private UUID organizationUnitId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Builder.Default
    private DocumentType type = DocumentType.DOCUMENT;

    @Column(name = "description")
    private String description;

    @Column(name = "url")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private DocumentStatus statut = DocumentStatus.ACTIF;

    @Column(name = "created_by")
    private UUID createdBy;

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
