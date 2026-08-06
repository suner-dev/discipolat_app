package com.discipolat.modules.members.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Présence hebdomadaire saisie par le membre lui-même.
 * Une ligne par membre et par semaine ; les programmes de la semaine
 * sont stockés en JSON (ex : {"Culte du dimanche": true, "Étude biblique": false}).
 */
@Entity
@Table(name = "member_presences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "soul_id")
    private UUID soulId;

    /** Lundi de la semaine concernée. */
    @Column(name = "semaine", nullable = false)
    private LocalDate semaine;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "presences", columnDefinition = "jsonb")
    private Map<String, Boolean> presences;

    @Column(name = "notes")
    private String notes;

    /** Type de programme choisi (configuré par le pasteur) : DIMANCHE, CONVENTION, etc. */
    @Column(name = "type_programme")
    private String typeProgramme;

    /** Sous-programme choisi (ex : Premier culte, Deuxième culte). */
    @Column(name = "sous_programme")
    private String sousProgramme;

    @Column(name = "present")
    private Boolean present;

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
