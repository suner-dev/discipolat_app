package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/api-docs")
public class ApiDocsController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> getApiDocs() {
        Map<String, Object> docs = new LinkedHashMap<>();
        docs.put("title", "Discipolat API");
        docs.put("version", "1.0.0");
        docs.put("description", "API REST complète pour la plateforme Discipolat — gestion d'église multi-tenant");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("openapi", "3.0.3");
        info.put("info", Map.of("title", "Discipolat API", "version", "1.0.0",
                "description", "API pour l'intégration avec des systèmes tiers"));
        docs.putAll(info);

        List<Map<String, String>> endpoints = new ArrayList<>();
        endpoints.add(Map.of("path", "/api/v1/souls", "method", "GET", "description", "Liste des âmes (disciples)"));
        endpoints.add(Map.of("path", "/api/v1/souls", "method", "POST", "description", "Créer une âme"));
        endpoints.add(Map.of("path", "/api/v1/souls/{id}", "method", "GET", "description", "Détail d'une âme"));
        endpoints.add(Map.of("path", "/api/v1/reports", "method", "GET", "description", "Liste des rapports"));
        endpoints.add(Map.of("path", "/api/v1/reports", "method", "POST", "description", "Créer un rapport"));
        endpoints.add(Map.of("path", "/api/v1/events", "method", "GET", "description", "Liste des événements"));
        endpoints.add(Map.of("path", "/api/v1/surveys", "method", "GET", "description", "Liste des sondages"));
        endpoints.add(Map.of("path", "/api/v1/surveys", "method", "POST", "description", "Créer un sondage"));
        endpoints.add(Map.of("path", "/api/v1/testimonies", "method", "GET", "description", "Liste des témoignages"));
        endpoints.add(Map.of("path", "/api/v1/tickets", "method", "GET", "description", "Liste des tickets"));
        endpoints.add(Map.of("path", "/api/v1/leave-requests", "method", "GET", "description", "Demandes d'absence"));
        endpoints.add(Map.of("path", "/api/v1/referrals", "method", "GET", "description", "Parrainages"));
        endpoints.add(Map.of("path", "/api/v1/calendar", "method", "GET", "description", "Événements calendrier"));
        endpoints.add(Map.of("path", "/api/v1/skills", "method", "GET", "description", "Matrice de compétences"));
        endpoints.add(Map.of("path", "/api/v1/team-tasks", "method", "GET", "description", "Tâches d'équipe (Gantt)"));
        endpoints.add(Map.of("path", "/api/v1/compliance/gdpr", "method", "GET", "description", "Demandes RGPD"));
        endpoints.add(Map.of("path", "/api/v1/departments", "method", "GET", "description", "Départements"));
        endpoints.add(Map.of("path", "/api/v1/families", "method", "GET", "description", "Familles"));
        endpoints.add(Map.of("path", "/api/v1/messages", "method", "GET", "description", "Messagerie"));
        endpoints.add(Map.of("path", "/api/v1/notifications", "method", "GET", "description", "Notifications"));
        docs.put("endpoints", endpoints);

        docs.put("auth", Map.of(
                "type", "Bearer JWT",
                "header", "Authorization",
                "format", "Bearer <token>",
                "obtain_token", "POST /api/v1/auth/login"
        ));

        docs.put("webhooks", Map.of(
                "description", "Webhooks sortants avec signature HMAC-SHA256",
                "events", List.of("soul.created", "soul.updated", "report.submitted", "payment.received", "event.created")
        ));

        return ResponseEntity.ok(docs);
    }

    @GetMapping("/openapi.yaml")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<String> getOpenApiSpec() {
        String yaml = """
                openapi: 3.0.3
                info:
                  title: Discipolat API
                  version: 1.0.0
                  description: API REST pour la plateforme Discipolat
                servers:
                  - url: /api/v1
                security:
                  - bearerAuth: []
                components:
                  securitySchemes:
                    bearerAuth:
                      type: http
                      scheme: bearer
                      bearerFormat: JWT
                  schemas:
                    Soul:
                      type: object
                      properties:
                        id: { type: string, format: uuid }
                        nom: { type: string }
                        prenom: { type: string }
                        email: { type: string }
                        statut: { type: string, enum: [ACTIF, INACTIF, EN_COURS] }
                    Survey:
                      type: object
                      properties:
                        id: { type: string, format: uuid }
                        titre: { type: string }
                        type: { type: string, enum: [CHOIX_UNIQUE, CHOIX_MULTIPLE, ECHAUFFEMENT, TEXTE_LIBRE] }
                        statut: { type: string, enum: [BROUILLON, ACTIF, FERME] }
                    Ticket:
                      type: object
                      properties:
                        id: { type: string, format: uuid }
                        titre: { type: string }
                        categorie: { type: string, enum: [TECHNIQUE, COMPTE, FEATURE, BUG, QUESTION, AUTRE] }
                        statut: { type: string, enum: [OUVERT, EN_COURS, RESOLU, FERME] }
                        priorite: { type: string, enum: [BASSE, MOYENNE, HAUTE, CRITIQUE] }
                paths:
                  /souls:
                    get:
                      summary: Liste des âmes
                      tags: [Souls]
                    post:
                      summary: Créer une âme
                      tags: [Souls]
                  /surveys:
                    get:
                      summary: Liste des sondages
                      tags: [Surveys]
                    post:
                      summary: Créer un sondage
                      tags: [Surveys]
                  /tickets:
                    get:
                      summary: Liste des tickets
                      tags: [Tickets]
                    post:
                      summary: Créer un ticket
                      tags: [Tickets]
                  /testimonies:
                    get:
                      summary: Liste des témoignages
                      tags: [Testimonies]
                  /leave-requests:
                    get:
                      summary: Demandes d'absence
                      tags: [LeaveRequests]
                  /calendar:
                    get:
                      summary: Événements calendrier
                      tags: [Calendar]
                  /referrals:
                    get:
                      summary: Parrainages
                      tags: [Referrals]
                  /skills:
                    get:
                      summary: Matrice de compétences
                      tags: [Skills]
                  /team-tasks:
                    get:
                      summary: Tâches d'équipe
                      tags: [TeamTasks]
                  /compliance/gdpr:
                    get:
                      summary: Demandes RGPD
                      tags: [Compliance]
                """;
        return ResponseEntity.ok(yaml);
    }
}
