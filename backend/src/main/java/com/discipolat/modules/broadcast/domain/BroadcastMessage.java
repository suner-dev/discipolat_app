package com.discipolat.modules.broadcast.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "broadcast_messages")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class BroadcastMessage {

    public enum Statut { BROUILLON, PROGRAMMÉ, ENVOYÉ, ÉCHOUÉ }
    public enum Cible { TOUS, DÉPARTEMENT, FAMILLE, RÔLE, SEGMENT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cible cible = Cible.TOUS;

    @Column(columnDefinition = "TEXT")
    private String cibleIds; // JSON array of target IDs

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.BROUILLON;

    @Column(nullable = false)
    private UUID envoyéPar;

    private LocalDateTime programméAt;
    private LocalDateTime envoyéAt;
    private LocalDateTime expiresAt;

    private int totalEnvoyé = 0;
    private int totalLu = 0;

    @OneToMany(mappedBy = "broadcast", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BroadcastReceipt> receipts = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public Cible getCible() { return cible; }
    public void setCible(Cible cible) { this.cible = cible; }
    public String getCibleIds() { return cibleIds; }
    public void setCibleIds(String cibleIds) { this.cibleIds = cibleIds; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public UUID getEnvoyéPar() { return envoyéPar; }
    public void setEnvoyéPar(UUID envoyéPar) { this.envoyéPar = envoyéPar; }
    public LocalDateTime getProgramméAt() { return programméAt; }
    public void setProgramméAt(LocalDateTime programméAt) { this.programméAt = programméAt; }
    public LocalDateTime getEnvoyéAt() { return envoyéAt; }
    public void setEnvoyéAt(LocalDateTime envoyéAt) { this.envoyéAt = envoyéAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public int getTotalEnvoyé() { return totalEnvoyé; }
    public void setTotalEnvoyé(int totalEnvoyé) { this.totalEnvoyé = totalEnvoyé; }
    public int getTotalLu() { return totalLu; }
    public void setTotalLu(int totalLu) { this.totalLu = totalLu; }
    public List<BroadcastReceipt> getReceipts() { return receipts; }
    public void setReceipts(List<BroadcastReceipt> receipts) { this.receipts = receipts; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
