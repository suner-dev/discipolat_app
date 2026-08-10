package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.PrioriteTransfert;
import com.discipolat.common.enums.TransferStatus;
import com.discipolat.common.enums.TransferType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Demande de transfert. Cycle de vie complet :
 * BROUILLON → SOUMIS → EN_ATTENTE_VALIDATION → VALIDATION_PARTIELLE → VALIDE → EXECUTE
 * (ou REFUSE / ANNULE), puis ARCHIVE.
 * Toutes les transitions sont historisées (TransferHistory) et auditées.
 */
@Entity
@Table(name = "transfer_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_transfert", nullable = false)
    private TransferType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private TransferStatus statut = TransferStatus.BROUILLON;

    /** Personne concernée (id de l'âme ou de l'utilisateur). */
    @Column(name = "personne_id", nullable = false)
    private UUID personneId;

    /** SOUL ou USER. */
    @Column(name = "personne_type", nullable = false)
    @Builder.Default
    private String personneType = "SOUL";

    /** Affectation actuelle : {type, id, nom}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ancienne_affectation", columnDefinition = "jsonb")
    private Map<String, Object> ancienneAffectation;

    /** Nouvelle affectation : {type, id, nom}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelle_affectation", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> nouvelleAffectation;

    @Column(name = "demandeur_id", nullable = false)
    private UUID demandeurId;

    @Column(name = "justification", nullable = false, length = 4000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "priorite", nullable = false)
    @Builder.Default
    private PrioriteTransfert priorite = PrioriteTransfert.MOYENNE;

    @Column(name = "commentaires", length = 4000)
    private String commentaires;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @Column(name = "date_execution")
    private LocalDateTime dateExecution;

    @Column(name = "delai_limite")
    private LocalDateTime delaiLimite;

    /** Index de l'étape de validation courante (mode séquentiel). */
    @Builder.Default
    @Column(name = "etape_courante", nullable = false)
    private Integer etapeCourante = 0;

    /** Nombre d'approbations obtenues (modes PARALLELE / N_VALIDATIONS_REQUISES). */
    @Builder.Default
    @Column(name = "approbations_obtenues", nullable = false)
    private Integer approbationsObtenues = 0;

    /** Config de workflow utilisée au moment de la soumission (instantané). */
    @Column(name = "workflow_config_id")
    private UUID workflowConfigId;

    /** Règles d'exécution propres à la demande (ex : transfererAmes). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "regles_execution", columnDefinition = "jsonb")
    private Map<String, Object> reglesExecution;

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
