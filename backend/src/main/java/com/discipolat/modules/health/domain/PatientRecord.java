package com.discipolat.modules.health.domain;

import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.users.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_records", indexes = {
    @Index(name = "idx_patient_record_person", columnList = "person_id"),
    @Index(name = "idx_patient_record_tenant", columnList = "tenant_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class PatientRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private User person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    private Family family;

    @Column(name = "groupe_sanguin")
    private String groupeSanguin;

    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "antecedents", columnDefinition = "TEXT")
    private String antecedents;

    @Column(name = "medecin_traitant")
    private String medecinTraitant;

    @Column(name = "medecin_tel")
    private String medecinTel;

    @Column(name = "mesures", columnDefinition = "TEXT")
    private String mesures;

    @Column(name = "notes_sensibles", columnDefinition = "TEXT")
    private String notesSensibles;

    @Column(name = "numero_assurance")
    private String numeroAssurance;

    @Column(name = "poids_kg")
    private Double poidsKg;

    @Column(name = "taille_cm")
    private Double tailleCm;

    @Column(name = "tension_arterielle")
    private String tensionArterielle;

    @Column(name = "glycemie")
    private String glycemie;

    @Column(name = "pack_year")
    private String packYear;

    @Column(name = "abouchement")
    private String abouchement;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "confidentiality_level", nullable = false)
    private ConfidentialityLevel confidentialityLevel = ConfidentialityLevel.STRICT;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ConfidentialityLevel {
        STRICT, INTERNAL, GENERAL
    }

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
