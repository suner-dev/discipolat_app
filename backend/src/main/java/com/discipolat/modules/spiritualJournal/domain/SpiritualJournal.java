package com.discipolat.modules.spiritualJournal.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * P1 #55 - Journal spirituel personnel
 */
@Entity
@Table(name = "spiritual_journals")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SpiritualJournal {

    public enum TypeEntree { PRIERE, REFLEXION, REMERCIEMENT, CONFESSION, LOUANGE, LECON }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID auteurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeEntree typeEntree;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    private LocalDate dateEntree = LocalDate.now();
    private boolean publique = false;
    private boolean favori = false;

    private LocalDateTime creeLe = LocalDateTime.now();
    private LocalDateTime modifieLe;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getAuteurId() { return auteurId; }
    public void setAuteurId(UUID auteurId) { this.auteurId = auteurId; }
    public TypeEntree getTypeEntree() { return typeEntree; }
    public void setTypeEntree(TypeEntree t) { this.typeEntree = t; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDate getDateEntree() { return dateEntree; }
    public void setDateEntree(LocalDate d) { this.dateEntree = d; }
    public boolean isPublique() { return publique; }
    public void setPublique(boolean p) { this.publique = p; }
    public boolean isFavori() { return favori; }
    public void setFavori(boolean f) { this.favori = f; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime l) { this.creeLe = l; }
    public LocalDateTime getModifieLe() { return modifieLe; }
    public void setModifieLe(LocalDateTime l) { this.modifieLe = l; }
}
