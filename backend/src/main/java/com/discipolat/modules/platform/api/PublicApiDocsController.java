package com.discipolat.modules.platform.api;

import com.discipolat.modules.platform.domain.PlatformFeatureFlagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P0 #2 — API publique documentée (OpenAPI/Swagger).
 * Endpoint 100% public (pas d'auth) pour la documentation de l'API.
 * Les intégrateurs tiers peuvent découvrir l'API sans token.
 */
@RestController
@RequestMapping("/api/v1/public/docs")
public class PublicApiDocsController {

    private final PlatformFeatureFlagService featureFlagService;

    public PublicApiDocsController(PlatformFeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPublicDocs() {
        featureFlagService.requireEnabled(PlatformFeatureFlagService.DOCS_ENABLED);
        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("title", "Discipolat API");
        docs.put("version", "2.0.0");
        docs.put("description", "API REST multi-tenant pour la gestion d'églises — Discipolat. " +
                "Authentification JWT RS256, RBAC, chiffrement AES-256-GCM.");

        // Swagger UI
        docs.put("swagger-ui", "/swagger-ui.html");
        docs.put("openapi-spec", "/api-docs");

        // Auth
        docs.put("authentication", Map.of(
                "type", "Bearer JWT (RS256)",
                "header", "Authorization: Bearer <token>",
                "login", "POST /api/v1/auth/login",
                "refresh", "POST /api/v1/auth/refresh",
                "token-lifetime", "15 minutes (access) / 7 jours (refresh)"
        ));

        // Rate limiting
        docs.put("rateLimiting", Map.of(
                "login", "10 req/min per IP",
                "api", "30 req/min per IP per endpoint",
                "headers", "X-RateLimit-Remaining, Retry-After"
        ));

        // Modules disponibles
        docs.put("modules", List.of(
                Map.of("name", "Souls", "path", "/api/v1/souls", "description", "Gestion des âmes (disciples)"),
                Map.of("name", "Events", "path", "/api/v1/events", "description", "Événements et RSVP"),
                Map.of("name", "Reports", "path", "/api/v1/reports", "description", "Rapports pastoraux"),
                Map.of("name", "Families", "path", "/api/v1/families", "description", "Familles spirituelles"),
                Map.of("name", "Departments", "path", "/api/v1/departments", "description", "Départements"),
                Map.of("name", "Finances", "path", "/api/v1/finances", "description", "Transactions et budgets"),
                Map.of("name", "Payments", "path", "/api/v1/payments", "description", "Paiements Mobile Money"),
                Map.of("name", "Messages", "path", "/api/v1/messages", "description", "Messagerie temps réel"),
                Map.of("name", "Notifications", "path", "/api/v1/notifications", "description", "Notifications multi-canal"),
                Map.of("name", "Surveys", "path", "/api/v1/surveys", "description", "Sondages et feedback"),
                Map.of("name", "Trainings", "path", "/api/v1/trainings", "description", "Formations et quiz"),
                Map.of("name", "Evangelism", "path", "/api/v1/evangelism", "description", "Pipeline d'évangélisation"),
                Map.of("name", "Tontine", "path", "/api/v1/tontine", "description", "Tontine numérique"),
                Map.of("name", "Webhooks", "path", "/api/v1/webhooks", "description", "Webhooks sortants"),
                Map.of("name", "Connectors", "path", "/api/v1/connectors", "description", "Connecteurs tiers (Zapier, Make, Calendar)"),
                Map.of("name", "GDPR", "path", "/api/v1/gdpr", "description", "Compliance RGPD/CCPA"),
                Map.of("name", "WhatsApp", "path", "/api/v1/whatsapp", "description", "Pont WhatsApp Business"),
                Map.of("name", "Currency", "path", "/api/currencies", "description", "Multi-devise et fuseaux horaires"),
                Map.of("name", "Onboarding", "path", "/api/v1/onboarding-wizard",
                        "description", "Wizard de configuration d'une eglise (7 etapes, contrat fige)"),
                Map.of("name", "AI", "path", "/api/v1/ai", "description", "Assistant IA pastoral"),
                Map.of("name", "Map", "path", "/api/v1/map", "description", "Carte interactive et géofencing"),
                Map.of("name", "DigitalTwin", "path", "/api/v1/twin", "description", "Jumeau numérique")
        ));

        // Webhooks
        docs.put("webhooks", Map.of(
                "description", "Webhooks sortants avec signature HMAC-SHA256",
                "events", List.of(
                        "soul.created", "soul.updated", "soul.deleted",
                        "report.submitted", "report.reviewed",
                        "payment.received", "payment.failed",
                        "event.created", "event.registration",
                        "message.received", "whatsapp.inbound",
                        "gdpr.export.completed", "gdpr.deletion.completed"
                ),
                "configuration", "POST /api/v1/webhooks (ADMIN)"
        ));

        // SDKs
        docs.put("sdks", Map.of(
                "javascript", "npm install @discipolat/sdk",
                "flutter", "pub add discipolat_sdk"
        ));

        return ResponseEntity.ok(docs);
    }

    /**
     * Le `produces` est explicite : sans lui, Spring renvoie `text/plain` et les
     * generateurs de SDK / outils OpenAPI refusent ou devinent le format. Le
     * media type officiel de l'OpenAPI est
     * `application/vnd.oai.openapi`.
     */
    /** Media type officiel de l'OpenAPI. */
    static final String OPENAPI_MEDIA_TYPE = "application/vnd.oai.openapi;version=3.0";

    @GetMapping(value = "/openapi.yaml", produces = OPENAPI_MEDIA_TYPE)
    public ResponseEntity<String> getOpenApiYaml() {
        featureFlagService.requireEnabled(PlatformFeatureFlagService.DOCS_ENABLED);
        String yaml = """
                openapi: 3.0.3
                info:
                  title: Discipolat API
                  version: 2.0.0
                  description: API REST multi-tenant pour la gestion d'églises
                  contact:
                    name: Discipolat Support
                    email: support@discipolat.com
                    url: https://discipolat.com
                  license:
                    name: Propriétaire
                    url: https://discipolat.com/license
                servers:
                  - url: /api/v1
                    description: Serveur principal
                security:
                  - bearerAuth: []
                components:
                  securitySchemes:
                    bearerAuth:
                      type: http
                      scheme: bearer
                      bearerFormat: JWT
                      description: JWT RS256 — obtenez le token via POST /auth/login
                  schemas:
                    Soul:
                      type: object
                      properties:
                        id: { type: string, format: uuid }
                        nom: { type: string }
                        prenom: { type: string }
                        email: { type: string, format: email }
                        telephone: { type: string }
                        statut: { type: string, enum: [ACTIF, INACTIF, EN_COURS] }
                        etatSpirituel: { type: string, enum: [TIÈDE, ACTIF, BRÛLANT, DÉCROCHEUR] }
                        typeDisciple: { type: string, enum: [MEMBRE, DISCIPLE, FAISEUR, LEADER] }
                    Event:
                      type: object
                      properties:
                        id: { type: string, format: uuid }
                        titre: { type: string }
                        description: { type: string }
                        lieu: { type: string }
                        typeEvenement: { type: string }
                        dateDebut: { type: string, format: date-time }
                        dateFin: { type: string, format: date-time }
                        limitePlaces: { type: integer }
                    Report:
                      type: object
                      properties:
                        id: { type: string, format: uuid }
                        titre: { type: string }
                        typeRapport: { type: string }
                        contenu: { type: string }
                        statut: { type: string, enum: [BROUILLON, SOUMIS, VALIDE] }
                    Transaction:
                      type: object
                      properties:
                        id: { type: string, format: uuid }
                        montant: { type: number }
                        devise: { type: string }
                        typeTransaction: { type: string }
                        methodePaiement: { type: string }
                    WebhookRegistration:
                      type: object
                      properties:
                        id: { type: string, format: uuid }
                        url: { type: string, format: uri }
                        events: { type: array, items: { type: string } }
                        secret: { type: string }
                        isActive: { type: boolean }
                paths:
                  /auth/login:
                    post:
                      summary: Connexion
                      tags: [Auth]
                      requestBody:
                        content:
                          application/json:
                            schema:
                              type: object
                              properties:
                                email: { type: string }
                                password: { type: string }
                      responses:
                        '200':
                          description: Token JWT
                        '429':
                          description: Rate limit dépassé (10 req/min)
                  /souls:
                    get:
                      summary: Liste des âmes
                      tags: [Souls]
                      security: [{ bearerAuth: [] }]
                    post:
                      summary: Créer une âme
                      tags: [Souls]
                  /souls/{id}:
                    get:
                      summary: Détail d'une âme
                      tags: [Souls]
                    put:
                      summary: Modifier une âme
                      tags: [Souls]
                    delete:
                      summary: Supprimer une âme
                      tags: [Souls]
                  /events:
                    get:
                      summary: Liste des événements
                      tags: [Events]
                    post:
                      summary: Créer un événement
                      tags: [Events]
                  /events/upcoming/mine:
                    get:
                      summary: Calendrier personnel du membre
                      tags: [Events]
                  /reports:
                    get:
                      summary: Liste des rapports
                      tags: [Reports]
                    post:
                      summary: Créer un rapport
                      tags: [Reports]
                  /finances/transactions:
                    get:
                      summary: Liste des transactions
                      tags: [Finances]
                  /webhooks:
                    get:
                      summary: Liste des webhooks
                      tags: [Webhooks]
                    post:
                      summary: Créer un webhook
                      tags: [Webhooks]
                  /connectors:
                    get:
                      summary: Liste des connecteurs tiers
                      tags: [Connectors]
                  /gdpr/requests:
                    get:
                      summary: Demandes RGPD
                      tags: [GDPR]
                  /currencies:
                    get:
                      summary: Devises configurées
                      tags: [Currency]
                  /currencies/convert:
                    post:
                      summary: Convertir un montant
                      tags: [Currency]
                  /onboarding-wizard:
                    get:
                      summary: Les 7 etapes du wizard (initialise si vide)
                      description: |
                        Contrat fige. Renvoie `OnboardingStepResponse[]` avec les
                        champs `id`, `stepType`, `stepOrder`, `title`, `description`,
                        `status`, `isCompleted`, `isSkippable`, `skipRequiresReason`,
                        `startedAt`, `completedAt`, `completedData` (objet JSON).
                      tags: [Onboarding]
                  /onboarding-wizard/progress:
                    get:
                      summary: Progression globale du wizard
                      description: Renvoie `totalSteps`, `completedSteps` (COMPLETED uniquement),
                        `skippedSteps`, `percentage`, `isComplete` et `steps`.
                      tags: [Onboarding]
                  /onboarding-wizard/status:
                    get:
                      summary: Etat d'achèvement de l'onboarding du tenant
                      description: Renvoie `completed`, `completedAt`, `completedBy`, `totalSteps`,
                        `completedSteps`, `skippedSteps`, `percentage`.
                      tags: [Onboarding]
                  /onboarding-wizard/initialize:
                    post:
                      summary: (Re)cree les 7 etapes — idempotent
                      description: Aucune duplication garantie par l'index unique
                        `uk_onboarding_step_tenant_type`. Reserve aux administrateurs de tenant.
                      tags: [Onboarding]
                  /onboarding-wizard/{id}/start:
                    post:
                      summary: Demarre une etape (PENDING -> IN_PROGRESS)
                      description: 404 `STEP_NOT_FOUND` si l'etape appartient a un autre tenant ;
                        409 `STEP_ORDER_VIOLATION` si une etape precedente n'est pas reglee.
                      tags: [Onboarding]
                  /onboarding-wizard/{id}/complete:
                    post:
                      summary: Complete une etape en executant son action metier reelle
                      description: |
                        Corps **facultatif** : `{"data": {...}}` ou absent (equivalent `{}`).
                        Une donnee invalide donne 400 `STEP_DATA_INVALID` avec le champ fautif.
                        409 `STEP_ALREADY_COMPLETED` / `STEP_ORDER_VIOLATION` / `STEP_PRECONDITION_FAILED`.
                      tags: [Onboarding]
                  /onboarding-wizard/{id}/skip:
                    post:
                      summary: Saute une etape
                      description: Motif obligatoire si `skipRequiresReason` (400
                        `STEP_SKIP_REASON_REQUIRED`) ; 409 `STEP_NOT_SKIPPABLE` sinon.
                      tags: [Onboarding]
                  /onboarding-wizard/templates/{role}:
                    get:
                      summary: Template onboarding par rôle
                      tags: [Onboarding]
                  /auth/registration-status:
                    post:
                      summary: Suivi public d'une demande d'inscription d'eglise
                      description: |
                        Endpoint PUBLIC : 3 requetes / 5 min / IP, `Cache-Control: no-store`,
                        aucune fuite d'information. Renvoie `status`
                        (`PENDING_APPROVAL` | `APPROVED` | `REJECTED` | `NONE`), `decidedAt`,
                        `reason` (uniquement si `REJECTED`) et `canLogin`.
                      tags: [Auth]
                """;
        // MediaType pose EXPLICITEMENT dans la reponse (et pas seulement via
        // `produces`) : c'est verifiable sans couche MVC, et garantit le type
        // meme devant un proxy ou un `produces` ecrase par une configuration.
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(OPENAPI_MEDIA_TYPE))
                .body(yaml);
    }
}
