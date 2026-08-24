package com.discipolat.modules.familyResources.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_resources")
public class FamilyResource {

    public enum Type { DOCUMENT, VIDÉO, ÉTUDE_BIBLIQUE, AUDIO, LIEN, AUTRE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID familleId;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type = Type.DOCUMENT;

    private String url;

    private UUID uploadéPar;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private int téléchargements = 0;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getFamilleId() { return familleId; }
    public void setFamilleId(UUID familleId) { this.familleId = familleId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public UUID getUploadéPar() { return uploadéPar; }
    public void setUploadéPar(UUID uploadéPar) { this.uploadéPar = uploadéPar; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getTéléchargements() { return téléchargements; }
    public void setTéléchargements(int téléchargements) { this.téléchargements = téléchargements; }
}
