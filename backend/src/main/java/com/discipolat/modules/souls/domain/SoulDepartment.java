package com.discipolat.modules.souls.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Table de liaison ManyToMany âme ↔ département.
 * Un membre peut appartenir à plusieurs départements.
 */
@Entity
@Table(name = "soul_departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(SoulDepartmentId.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SoulDepartment {

    @Id
    @Column(name = "soul_id")
    private UUID soulId;

    @Id
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "date_affectation", nullable = false, updatable = false)
    private LocalDateTime dateAffectation;

    @Column(name = "date_desaffectation")
    private LocalDateTime dateDesaffectation;

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    /** Compte utilisateur à l'origine du rattachement (traçabilité). */
    @Column(name = "created_by")
    private UUID createdBy;

    /** Origine du rattachement : MANUEL | SIGNUP | TRANSFERT. */
    @Column(name = "origine")
    private String origine;

    @PrePersist
    protected void onCreate() {
        if (this.dateAffectation == null) {
            this.dateAffectation = LocalDateTime.now();
        }
    }
}
