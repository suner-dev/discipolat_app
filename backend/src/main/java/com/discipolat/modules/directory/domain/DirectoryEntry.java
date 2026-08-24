package com.discipolat.modules.directory.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "directory_entries")
public class DirectoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID membreId;

    private boolean publicProfil = true;

    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String téléphone;
    private String email;

    private String département;
    private String rôle;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getMembreId() { return membreId; }
    public void setMembreId(UUID membreId) { this.membreId = membreId; }
    public boolean isPublicProfil() { return publicProfil; }
    public void setPublicProfil(boolean publicProfil) { this.publicProfil = publicProfil; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getTéléphone() { return téléphone; }
    public void setTéléphone(String téléphone) { this.téléphone = téléphone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDépartement() { return département; }
    public void setDépartement(String département) { this.département = département; }
    public String getRôle() { return rôle; }
    public void setRôle(String rôle) { this.rôle = rôle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
