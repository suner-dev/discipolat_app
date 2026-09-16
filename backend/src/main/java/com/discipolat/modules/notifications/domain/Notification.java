package com.discipolat.modules.notifications.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "notifications")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "destinataire_id", nullable = false)
    private UUID destinataireId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeNotification type;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false)
    private CanalNotification canal;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "lu", nullable = false)
    private boolean lu = false;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @Column(name = "entite_reference_id")
    private UUID entiteReferenceId;

    @Column(name = "entite_reference_type")
    private String entiteReferenceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Explicit getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getDestinataireId() { return destinataireId; }
    public void setDestinataireId(UUID destinataireId) { this.destinataireId = destinataireId; }
    public TypeNotification getType() { return type; }
    public void setType(TypeNotification type) { this.type = type; }
    public CanalNotification getCanal() { return canal; }
    public void setCanal(CanalNotification canal) { this.canal = canal; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }
    public LocalDateTime getDateLecture() { return dateLecture; }
    public void setDateLecture(LocalDateTime dateLecture) { this.dateLecture = dateLecture; }
    public UUID getEntiteReferenceId() { return entiteReferenceId; }
    public void setEntiteReferenceId(UUID entiteReferenceId) { this.entiteReferenceId = entiteReferenceId; }
    public String getEntiteReferenceType() { return entiteReferenceType; }
    public void setEntiteReferenceType(String entiteReferenceType) { this.entiteReferenceType = entiteReferenceType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
