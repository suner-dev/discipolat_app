package com.discipolat.modules.platform.api;

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

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPublicDocs() {
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
                Map.of("name", "Onboarding", "path", "/api/onboarding-wizard", "description", "Wizard de configuration"),
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

    @GetMapping("/openapi.yaml")
    public ResponseEntity<String> getOpenApiYaml() {
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
                      summary: Étapes du wizard
                      tags: [Onboarding]
                  /onboarding-wizard/templates/{role}:
                    get:
                      summary: Template onboarding par rôle
                      tags: [Onboarding]
                """;
        return ResponseEntity.ok(yaml);
    }
}
