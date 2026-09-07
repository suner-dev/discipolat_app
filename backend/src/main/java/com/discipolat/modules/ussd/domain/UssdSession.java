package com.discipolat.modules.ussd.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Session USSD — chaque interaction USSD entrante crée une session.
 * La session persiste la navigation (menu courant, contexte) pour permettre
 * la continuité multi-écrans sur un feature phone.
 */
@Entity
@Table(name = "ussd_sessions", indexes = {
        @Index(name = "idx_ussd_session_id", columnList = "sessionId", unique = true),
        @Index(name = "idx_ussd_phone", columnList = "phoneNumber"),
        @Index(name = "idx_ussd_tenant", columnList = "tenantId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UssdSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID unique de session fourni par Africa's Talking. */
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    /** Numéro de téléphone de l'utilisateur (fourni par Africa's Talking). */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /** ID du tenant (église) — résolu via le code service USSD. */
    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    /** Menu courant dans la navigation (ex: "main", "giving", "prayer"). */
    @Column(name = "current_menu", length = 50)
    private String currentMenu;

    /** Données de contexte sérialisées (JSON léger pour navigation). */
    @Column(name = "context_data", length = 500)
    private String contextData;

    /** Étape dans le flux (0 = début). */
    @Column(name = "step")
    private int step;

    /** La session est-elle terminée (réponse "END" envoyée) ? */
    @Column(name = "ended")
    private boolean ended;

    /** Date de création. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Date de dernière activité. */
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
        if (currentMenu == null) currentMenu = "main";
        if (step == 0 && currentMenu.equals("main")) step = 0;
    }

    @PreUpdate
    void onUpdate() {
        lastActivity = LocalDateTime.now();
    }
}
