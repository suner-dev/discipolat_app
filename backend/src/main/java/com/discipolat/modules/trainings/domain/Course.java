package com.discipolat.modules.trainings.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Course {

    public enum Niveau { DEBUTANT, INTERMEDIAIRE, AVANCE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description")
    private String description;

    @Column(name = "categorie", nullable = false)
    private String categorie = "DISCIPOLAT";

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false)
    @Builder.Default
    private Niveau niveau = Niveau.DEBUTANT;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(name = "formateur_id")
    private UUID formateurId;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
