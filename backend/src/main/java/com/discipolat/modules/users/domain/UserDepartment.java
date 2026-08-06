package com.discipolat.modules.users.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
public class UserDepartment {

    @Id
    @Column(name = "user_id")
    private java.util.UUID userId;

    @Id
    @Column(name = "department_id")
    private java.util.UUID departmentId;

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
