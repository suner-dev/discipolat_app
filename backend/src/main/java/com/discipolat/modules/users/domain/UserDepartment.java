package com.discipolat.modules.users.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Table de liaison utilisateur ↔ département pour les responsables multi-départements.
 */
@Entity
@Table(name = "user_departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserDepartmentId.class)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class UserDepartment {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "role_dans_dept", nullable = false)
    private String roleDansDept = "MEMBRE";

    @Column(name = "date_affectation", nullable = false, updatable = false)
    private LocalDateTime dateAffectation;

    @PrePersist
    protected void onCreate() {
        if (this.dateAffectation == null) {
            this.dateAffectation = LocalDateTime.now();
        }
    }
}
