package com.discipolat.modules.trainings.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "training_certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class TrainingCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    @Column(name = "numero", nullable = false, unique = true)
    private String numero;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "score_final", nullable = false)
    private int scoreFinal;

    @Column(name = "delivre_le", nullable = false, updatable = false)
    private LocalDateTime delivreLe;

    @PrePersist
    protected void onCreate() {
        this.delivreLe = LocalDateTime.now();
    }
}
