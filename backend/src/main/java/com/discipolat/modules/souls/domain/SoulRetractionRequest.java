package com.discipolat.modules.souls.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "soul_retraction_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoulRetractionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ame_id", nullable = false)
    private UUID ameId;

    @Column(name = "demandeur_id", nullable = false)
    private UUID demandeurId;

    @Column(name = "justification", nullable = false)
    private String justification;

    @Column(name = "statut", nullable = false)
    private String statut = "EN_ATTENTE";

    @Column(name = "traite_par")
    private UUID traitePar;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @Column(name = "commentaire_reponse")
    private String commentaireReponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoulRetractionRequest r = (SoulRetractionRequest) o;
        return id != null && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
