package com.discipolat.modules.sermon.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sermon_translations")
@org.hibernate.annotations.FilterDef(name = "tenantFilter", parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = UUID.class))
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class SermonTranslation {

    public enum Statut { EN_COURS, TERMINE, ERREUR }
    public enum Langue { FR, EN, ES, PT, SW, AR }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID sermonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Langue langueCible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_COURS;

    @Column(columnDefinition = "TEXT")
    private String transcriptionOriginale;

    @Column(columnDefinition = "TEXT")
    private String traductionTexte;

    /** JSON array of timed subtitles: [{start, end, text}] */
    @Column(columnDefinition = "TEXT")
    private String subtitlesJson;

    private double confiance; // 0-1

    private LocalDateTime creeLe = LocalDateTime.now();
    private LocalDateTime termineLe;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getSermonId() { return sermonId; }
    public void setSermonId(UUID sermonId) { this.sermonId = sermonId; }
    public Langue getLangueCible() { return langueCible; }
    public void setLangueCible(Langue langueCible) { this.langueCible = langueCible; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public String getTranscriptionOriginale() { return transcriptionOriginale; }
    public void setTranscriptionOriginale(String t) { this.transcriptionOriginale = t; }
    public String getTraductionTexte() { return traductionTexte; }
    public void setTraductionTexte(String t) { this.traductionTexte = t; }
    public String getSubtitlesJson() { return subtitlesJson; }
    public void setSubtitlesJson(String s) { this.subtitlesJson = s; }
    public double getConfiance() { return confiance; }
    public void setConfiance(double confiance) { this.confiance = confiance; }
    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }
    public LocalDateTime getTermineLe() { return termineLe; }
    public void setTermineLe(LocalDateTime termineLe) { this.termineLe = termineLe; }
}
