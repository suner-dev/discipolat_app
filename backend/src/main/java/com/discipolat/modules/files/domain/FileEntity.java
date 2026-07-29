package com.discipolat.modules.files.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "type_fichier", nullable = false)
    private String typeFichier;

    @Column(name = "taille", nullable = false)
    private Long taille;

    @Column(name = "chemin", nullable = false)
    private String chemin;

    @Column(name = "description")
    private String description;

    @Column(name = "famille_id")
    private UUID familleId;

    @Column(name = "evenement_id")
    private UUID evenementId;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Column(name = "categorie", nullable = false)
    private String categorie = "DOCUMENT";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEntity file = (FileEntity) o;
        return id != null && id.equals(file.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
