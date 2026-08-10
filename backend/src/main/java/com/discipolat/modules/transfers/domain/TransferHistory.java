package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Historique immuable d'une demande de transfert.
 * Chaque transition d'état, décision, demande d'informations ou correction
 * est enregistrée avec son auteur, son rôle actif, son commentaire et les
 * anciennes/nouvelles valeurs. Aucune donnée n'est perdue.
 */
@Entity
@Table(name = "transfer_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transfer_request_id", nullable = false)
    private UUID transferRequestId;

    @Column(name = "action", nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut")
    private TransferStatus ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut")
    private TransferStatus nouveauStatut;

    @Column(name = "utilisateur_id")
    private UUID utilisateurId;

    @Column(name = "role_actif")
    private String roleActif;

    @Column(name = "commentaire", length = 2000)
    private String commentaire;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ancienne_valeur", columnDefinition = "jsonb")
    private Map<String, Object> ancienneValeur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelle_valeur", columnDefinition = "jsonb")
    private Map<String, Object> nouvelleValeur;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
