# CAHIER DE CHARGE — DISCIPOLAT
## Plateforme de Management et de Discipolat Évangélique

**Version** : 3.20.1  
**Date** : 7 septembre 2026  
**Projet** : suner-dev/discipolat_app  
**Statut** : Prêt avec réserves — 92/125 fonctionnalités fully implemented, 20 partielles, 12 cassées, 1 manquante

---

## TABLE DES MATIÈRES

1. [Contexte et Objectifs](#1-contexte-et-objectifs)
2. [Périmètre et Portée](#2-périmètre-et-portée)
3. [Architecture Technique](#3-architecture-technique)
4. [Glossaire](#4-glossaire)
5. [Acteurs et Rôles](#5-acteurs-et-rols)
6. [Modèle de Sécurité et Permissions](#6-modèle-de-sécurité-et-permissions)
7. [Multi-Tenancy et Isolation](#7-multi-tenancy-et-isolation)
8. [Spécifications par Domaine Fonctionnel — Use Cases Détaillés](#8-spécifications-par-domaine-fonctionnel---use-cases-détaillés)
9. [Les 20 Fonctionnalités Stratégiques](#9-les-20-fonctionnalités-stratégiques)
10. [Spécifications Fonctionnelles Détaillées (SFD)](#10-spécifications-fonctionnelles-détaillées-sfd)
11. [API REST — Spécifications Techniques](#11-api-rest---spécifications-techniques)
12. [Exigences Non-Fonctionnelles (NFR)](#12-exigences-non-fonctionnelles-nfr)
13. [Contraintes et Limites](#13-contraintes-et-limites)
14. [Protocole de Test et Validation](#14-protocole-de-test-et-validation)
15. [Annexes](#15-annexes)

---

## 1. CONTEXTE ET OBJECTIFS

### 1.1 Contexte

**Discipolat** est une **plateforme numérique complète de gestion, de suivi et de discipolat ecclésiastique**, conçue initialement pour les églises de tradition protestante évangélique, particulièrement adaptée aux réalités africaines (connectivité limitée, Mobile Money, multilinguisme, parcours de discipolat rigoureux).

La plateforme est née d'un constat : les églises gèrent aujourd'hui leurs membres, familles, départements, visites pastorales, rapports et événements avec des tableurs Excel, des groupes WhatsApp et des processus manuels. Cette fragmentation engendre des pertes de données, des doublons, un suivi inadéquat des âmes et une perte de la "vue 360°" sur l'ensemble de la congrégation.

### 1.2 Vision Produit

Transformer Discipolat en un **Digital Operating System for Christian Discipleship** : une infrastructure numérique complète permettant à une communauté de :

- **Gérer** ses membres, familles, départements et ressources ;
- **Former** ses disciples via des parcours structurés et des cours en ligne ;
- **Accompagner** chaque âme individuellement (visites, suivis, notes pastorales) ;
- **Connecter** les membres entre eux et avec l'église (messagerie, événements, réseau inter-églises) ;
- **Mobiliser** les dons, compétences et énergies via la gamification, le matching et les paiements ;
- **Décider** grâce à l'intelligence artificielle et aux tableaux de bord prédictifs.

### 1.3 Objectifs Métier

| Objectif | Description |
|----------|-------------|
| **Rétention** | Réduire le taux d'abandon des membres de 30% à moins de 5% grâce au suivi pastoral automatisé |
| **Adoption** | Atteindre 90% d'adoption parmi les membres actifs grâce à une UX mobile-first et l'offline-first |
| **Engagement** | Augmenter le score d'engagement moyen des familles de 40% via la gamification et les défis |
| **Productivité** | Réduire le temps de pointage présence de 20 min à 2 min via QR code ou reconnaissance faciale |
| **Réseau** | Connecter 100% des églises partenaires via le réseau inter-églises et le partage de ressources |
| **Données** | Offrir une visibilité en temps réel sur la santé spirituelle et financière de chaque église |

### 1.4 Livrables

| Livrable | Description | Format |
|----------|-------------|--------|
| Application Web | Interface administrateur et gestion complète | React 19 + Vite PWA |
| Application Mobile | Application native Android/iOS, offline-first | Flutter 3 |
| API Backend | API REST sécurisée, multi-tenant | Spring Boot 3 |
| Base de données | Schéma PostgreSQL versionné (Flyway) | 116 migrations |
| Documentation | Cahier de charge, API docs, guide utilisateur | Markdown |
| Tests | Suite de tests automatisés | JUnit 5 + Vitest + Flutter Test |

---

## 2. PÉRIMÈTRE ET PORTÉE

### 2.1 Périmètre Inclus

| Domaine | Portée |
|---------|--------|
| **Gestion des membres** | Création, modification, archivage, dossier 360°, score spirituel |
| **Gestion familiale** | Familles de disciples, chefs de famille, cohésion, réunions |
| **Gestion des départements** | Création, responsables, affectations, KPI, check-lists |
| **Événements & Présences** | Création, RSVP, QR check-in, géofencing, reconnaissance faciale |
| **Rapports pastoraux** | Rapports faiseurs/familles, workflow de validation, PDF |
| **Communications** | Broadcast, messagerie temps réel, annonces, rappels WhatsApp |
| **Finances & Dons** | Transactions, budget, Mobile Money (MTN/Orange/M-Pesa), tontine |
| **Parcours de discipolat** | Journey Engine, formations, quiz, certificats, objectifs |
| **Gamification** | Badges, niveaux, classements, quêtes, défis, streaks |
| **IA & Intelligence** | Assistant pastoral, prédictions, jumeau numérique, alertes smart |
| **Réseau inter-églises** | Ressources partagées, événements fédérés, annuaire, benchmark |
| **Administration** | Multi-tenant, RBAC, Page Builder, workflows, champs custom |
| **Intégrations** | WhatsApp, USSD, API publique, connecteurs tiers |
| **Sécurité & Conformité** | JWT, 2FA, RGPD, audit trail, protection d'écran |

### 2.2 Périmètre Exclus

| Exclus | Justification |
|--------|---------------|
| Système de culte en direct (streaming) | Remplacé par Jitsi/Zoom intégré |
| Gestion comptable complète (paie, impôts) | Module finances limité aux recettes/dépenses |
| Éditeur de sermons intégré | Remplacé par transcribeur + assistant IA |
| Messagerie WhatsApp privé intégré | Canal d'entrée via webhook |
| Application native hors Flutter | Toute la logique mobile est en Flutter |

---

## 3. ARCHITECTURE TECHNIQUE

### 3.1 Vue d'ensemble

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  React App   │    │  Flutter App │    │  Postman/CLI │
│  :5173/PWA   │    │  Android/iOS │    │              │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │ HTTPS / REST + WS
                           ▼
                    ┌──────────────┐
                    │   Nginx      │
                    │ Reverse Proxy│
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  Spring Boot │
                    │  API :8080   │
                    └──────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
   ┌──────────┐    ┌──────────┐    ┌──────────┐
   │PostgreSQL│    │  Redis 7 │    │   Files  │
   │  :5432   │    │  :6379   │    │  Bucket  │
      └──────────┘    └──────────┘    └──────────┘
```

### 3.2 Backend — Spring Boot 3.4.7

**Philosophie** : Architecture hexagonale + Spring Modulith, 38 modules métier autonomes.

**Technologies clés** : Spring Data JPA (Hibernate 6) avec filtres `@Filter(name="tenantFilter")`, Flyway (116 migrations), JWT RS256, Bucket4j rate limiter, SSE/STOMP temps réel, Redis 7 cache, JUnit 5 + Mockito (~1034 tests).

```
backend/
├── common/               # Multitenancy, sécurité (JWT, 2FA), propagations, exceptions
├── modules/              # 38 modules métier autonomes (api/domain/infrastructure/config)
├── db/migration/         # 116 migrations Flyway (V1 → V116)
└── pom.xml               # Spring Boot 3.4.7, Java 21
```

### 3.3 Frontend Web — React 19

**Technologies clés** : React Router v6+ (code-splitting, routes protégées), Axios (intercepteurs), TanStack Query, Recharts, Leaflet, Vitest, TypeScript strict, PWA (manifest, service worker). 167 pages lazy-loaded, 6 langues (FR, EN, PT, ES, SW, AR).

### 3.4 Mobile — Flutter 3

**Technologies clés** : Riverpod, GoRouter (159 routes), Drift SQLite (offline-first), Dio, JWT RS256, 2FA TOTP, biométrie (empreinte/facial), FCM push, FLAG_SECURE, Whisper API STT/TTS. 325 tests passés.

### 3.5 Base de Données — PostgreSQL 16

- **116 migrations Flyway** versionnées automatiquement
- **Multi-tenant** : colonne `tenant_id UUID NOT NULL` sur toutes tables métier
- **Soft delete** : `deleted`, `deleted_at`, `deleted_by`
- **JSONB** : colonnes flexibles (`presences_par_culte`, `stats_agregees`, `metadata`)
- **Audit trail** : table `audit_logs` avec avant/après (JSONB)

---

## 4. GLOSSAIRE

| Terme | Définition |
|-------|------------|
| **Âme (ou Disciple)** | Un membre/fidèle suivi par le système (nouvel arrivant, nouveau converti, membre régulier) |
| **Faiseur** | Membre responsable du suivi spirituel d'un groupe d'âmes (discipleur) |
| **Chef de Famille** | Responsable d'une famille de disciples ; supervise les faiseurs de sa famille |
| **Responsable** | Responsable d'un département ; supervise les familles du département |
| **Pasteur** | Pasteur principal ; accès global à toutes les données de l'église |
| **Famille** | Groupe d'âmes organisées par géôle, rattachées à un département |
| **Département** | Structure administrative (jeunesse, femmes, hommes, culte) |
| **Tenant** | Une église/organisation utilisant la plateforme (multi-tenant SaaS) |
| **Score Spirituel** | Score de 0 à 100 basé sur 12 axes (présence, prière, engagement, service) |
| **Rapport Faiseur** | Rapport hebdomadaire soumis par un faiseur sur l'état de ses âmes |
| **Rapport Famille** | Rapport hebdomadaire consolidé soumis par un chef de famille |
| **Discipolat ID** | Identité numérique unifiée du membre (profil 360°) |
| **Spiritual Passport** | Passeport spirituel (historique migratoire du disciple) |
| **Tontine** | Système d'épargne collaboratif intégré |

---

## 5. ACTEURS ET RÔLES

### 5.1 Système de Rôles (6 rôles)

| Rôle | Niveau | Description |
|------|--------|-------------|
| **ADMIN** | Super-utilisateur | Gère la plateforme pour tous les tenants. Accès complet. |
| **PASTEUR** | Pasteur | Pasteur d'une église. Accès global à toutes les données. |
| **RESPONSABLE** | Responsable départemental | Responsable d'un ou plusieurs départements. |
| **CHEF_DE_FAMILLE** | Chef de famille | Chef d'une ou plusieurs familles. Accès aux âmes de sa famille. |
| **FAISEUR** | Discipleur | Faiseur responsable de 1..N âmes. Accès uniquement à ses âmes. |
| **MEMBRE** | Membre | Membre de l'église. Accès limité à son profil, événements, dons. |

### 5.2 Switch de Rôle

Un compte utilisateur peut **changer de rôle** via `/auth/switch-role`. Le changement modifie instantanément le dashboard, les menus, les permissions et les notifications.

### 5.3 Personas Utilisateurs

| Persona | Rôle(s) | Objectif principal |
|---------|---------|-------------------|
| **Pasteur Martin** | PASTEUR | Vue 360° de l'église, détection décrochages, recommandations IA |
| **Responsable Sophie** | RESPONSABLE | Suivi KPI département, validation rapports, assignation faiseurs |
| **Chef de Famille Karim** | CHEF_DE_FAMILLE | Suivi cohésion familiale, validation rapports, réunions |
| **Faiseur Aïcha** | FAISEUR | Suivi âmes, rapports hebdo, signalement besoins |
| **Membre Bapmo** | MEMBRE | Profil, événements, dons, défis |
| **Administrateur Système** | ADMIN | Config tenant, modules, workflows, sécurité |

---

## 6. MODÈLE DE SÉCURITÉ ET PERMISSIONS

### 6.1 Authentification

| Mécanisme | Détail |
|-----------|--------|
| **JWT RS256** | Signature RSA 2048 bits. Clés via `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` (base64) |
| **Access Token** | 15 minutes (configurable) |
| **Refresh Token** | 7 jours, rotation à chaque utilisation |
| **2FA TOTP** | Authentification à deux facteurs (Google Authenticator/Authy) |
| **OAuth2 Social** | Google OAuth (web only — pas de mobile) |
| **Magic Link** | Connexion par lien magique (web only — pas de mobile) |
| **Biométrie** | Empreinte digitale / reconnaissance faciale (mobile) |
| **Rate Limiting** | Bucket4j sur `/auth/login` : 10 req/min/IP |

### 6.2 Autorisation (RBAC + Scopes)

| Niveau | Implémentation |
|--------|----------------|
| **Rôle** | `@PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")` sur chaque contrôleur |
| **Scope de données** | `WorkspaceScopeService` — un faiseur ne voit que ses âmes |
| **Permission fine** | `@perm.has('DOMAIN','ACTION')` via `PermissionGuard` (SpEL) |
| **Ownership** | Checks côté service sur chaque modification/suppression |
| **Audit** | `@EventListener` sur toutes mutations sensibles |

### 6.3 Matrice RBAC (extrait)

| Rôle | Âmes | Familles | Départements | Rapports | Alertes | Dashboard | Utilisateurs |
|------|------|----------|--------------|----------|---------|-----------|--------------|
| ADMIN | Toutes | Toutes | Tous | Tous | Toutes | Global | Tous |
| PASTEUR | Toutes | Toutes | Tous | Tous | Toutes | Global | Tous |
| RESPONSABLE | Son dépt. | Son dépt. | Son dépt. | Son dépt. | Son dépt. | Son dépt. | Non |
| CHEF_DE_FAMILLE | Sa famille | Sa famille | Lecture | Sa famille | Sa famille | Sa famille | Non |
| FAISEUR | Ses âmes | Lecture | Lecture | Ses rapports | Ses alertes | Non | Non |
| MEMBRE | Son profil | Lecture | Lecture | N/A | N/A | Membre | Non |

### 6.4 Protection des Données

| Aspect | Implémentation |
|--------|----------------|
| **Multi-tenancy** | Filtre Hibernate `@Filter(name="tenantFilter")`, `TenantContext` ThreadLocal |
| **IDOR Protection** | Vérification propriétaire sur chaque endpoint |
| **GDPR** | Export (`GdprDataExportRequest`), suppression (`GdprDeletionRequest`), consentements |
| **Audit Trail** | Table `audit_logs` (avant/après JSONB, horodatage, utilisateur) |
| **Secrets** | Variables d'environnement, jamais commitées |
| **Soft Delete** | `deleted`, `deleted_at`, `deleted_by` |
| **HTTPS** | TLS 1.2+ en production |
| **Headers** | CSP, HSTS, CORS (domaines via `FRONTEND_URL`) |
| **XSS** | Bean Validation + échappement React |
| **SQL Injection** | JPA/Hibernate paramétré |
| **Mot de passe** | BCrypt coût 12, min 8 caractères |
| **Webhook HMAC** | SHA256 vérifiée (WhatsApp, Mobile Money) |
| **Capture d'écran** | `FLAG_SECURE` (Android) |

---

## 7. MULTI-TENANCY ET ISOLATION

### 7.1 Architecture

- Chaque **Tenant** = une église/organisation
- Isolation DB : colonne `tenant_id UUID NOT NULL` sur toutes tables métier
- Isolation API : `SecurityUtils.getTenantId()` + filtre Hibernate
- Tenant context propagé via JWT (claim `tenant_id`)

### 7.2 Isolation des données

| Aspect | Statut |
|--------|--------|
| Isolation DB | ✅ Filtre Hibernate + TenantContext |
| Isolation API | ✅ SecurityUtils.getTenantId() + @PreAuthorize |
| Isolation fichiers | ⚠️ Pas de dossier par tenant (à améliorer) |
| Setup wizard | ❌ À implémenter |
| Seed par défaut | ⚠️ (fail-closed en prod) |
| Facturation par tenant | ❌ À implémenter |

Chaque tenant configure indépendamment : identité (nom/logo/couleurs), modules activés, menus, langue, fuseau, devise, workflows, champs custom, pages personnalisées.

---

## 9. LES 20 FONCTIONNALITÉS STRATÉGIQUES

La feuille de route produit s'articule autour de **20 fonctionnalités stratégiques**, organisées en 4 priorités (P0-P3).

### Priorité P0 — Fondations (Corrections critiques)

| # | Fonctionnalité | Description | Status |
|---|----------------|-------------|--------|
| 1 | **Discipolat ID** | Identité numérique unifiée (profil 360°, QR personnel, compétences, formations, parcours, objectifs, certificats, historique) | ✅ Complet |
| 2 | **Multi-Tenant Security** | Isolation DB + RBAC serveur + i18n + multi-devise | ✅ Corrigé |
| 3 | **2FA TOTP** | Authentification à deux facteurs | ✅ Complet |
| 4 | **Audit Trail** | Journalisation de toutes les actions sensibles | ✅ Complet |

### Priorité P1 — Mobile-First & Expérience

| # | Fonctionnalité | Description | Status |
|---|----------------|-------------|--------|
| 1 | **AI Pastoral Copilot** | Assistant IA conversationnel pour pasteurs (questions, recommandations, rapports). RAG + Ollama local. | ✅ Complet |
| 2 | **Rapport vocal IA Offline** | Dictée vocale → transcription → structuration → sync. Fonctionne sans connexion. | ✅ Complet |
| 3 | **WhatsApp ↔ Discipolat** | WhatsApp comme porte d'entrée (demande de prière, RSVP, onboarding). Webhook signé. | ✅ Complet |
| 4 | **Mobile Money / Giving** | Dons via MTN MoMo, Orange Money, M-Pesa. Webhook signé, reçus. | ✅ Complet |
| 5 | **Journey Engine** | Parcours de discipolat structuré (étapes, progression, mentor) | ✅ Complet |
| 6 | **Academy** | Système de formations (cours, chapitres, quiz, certificats) | ✅ Complet |
| 7 | **Discipolat Quest** | Gamification (XP, niveaux, badges, streaks, défis) | ✅ Complet |
| 8 | **Talent Matching** | Matching de compétences entre membres (consentement, départements) | ✅ Complet |
| 9 | **QR Check-in & Présence** | QR code personnel + scanneur, reconnaissance faciale offline | ✅ Complet |
| 10 | **Pastoral Care 360°** | Timeline complète, confidentialité notes pastorales | ✅ Complet |

### Priorité P2 — IA & Intelligence

| # | Fonctionnalité | Description | Status |
|---|----------------|-------------|--------|
| 1 | **Smart Alerts** | Détection automatique d'anomalies (absence, non-contact, rapport en retard) | ✅ Complet |
| 2 | **Prédictions IA** | Signaux faibles, recommandations prédictives | ✅ Complet |
| 3 | **Jumeau Numérique** | Simulation "et si", prévision besoins en leaders | ✅ Complet |
| 4 | **Journal Prophétique** | Journal de prophéties, corrélations avec les événements | ✅ Complet |
| 5 | **Assistant Vocal** | Commandes vocales (KPI, alertes), STT réel (Whisper API) | ✅ Complet |
| 6 | **Santé Prédictive** | Scores de santé par département, prédictions de décrochage | ✅ Complet |

### Priorité P3 — Écosystème & Réseau

| # | Fonctionnalité | Description | Status |
|---|----------------|-------------|--------|
| 1 | **Discipolat Network** | Ressources partagées, événements fédérés, RSVP, annuaire | ✅ Complet |
| 2 | **Compliance Manager** | Gestion exigences légales, export conforme RGPD | ✅ Complet |
| 3 | **API Publique / Connecteurs** | API pour intégrations (Zapier, Google Calendar, QuickBooks) | ✅ Complet |
| 4 | **Tontine Numérique** | Système d'épargne collaboratif | ✅ Complet |
| 5 | **AR Onboarding** | Onboarding augmenté pour nouveaux membres | ✅ Complet |
| 6 | **Carte Vivante des Âmes** | Carte interactive, clustering, zones de chaleur | ✅ Complet |
| 7 | **Benchmark Inter-Églises** | Comparaison anonymisée entre églises | ✅ Complet |
| 8 | **Visioconférence Intégrée** | Jitsi Meet intégré pour réunions | ✅ Complet |
| 9 | **Transcription Automatique** | Upload audio → transcription Whisper → résumé IA | ✅ Complet |

---

## 8. SPÉCIFICATIONS PAR DOMAINE FONCTIONNEL — USE CASES DÉTAILLÉS

### 8.1 Gestion des Membres (Discipolat ID)

**CU-1 : Créer un membre**
- *Préconditions* : Rôle FAISEUR, CHEF_DE_FAMILLE, RESPONSABLE, PASTEUR ou ADMIN
- *Déclencheur* : Clic sur "Nouveau membre"
- *Flux* : Remplir formulaire → système génère Discipolat ID (UUID) + QR code → assignation famille/faiseur/département → enregistrement
- *Post-conditions* : Membre créé, notification au responsable, score spirituel initial = 50
- *Exceptions* : Email déjà utilisé → erreur ; Téléphone invalide → validation

**CU-2 : Consulter le dossier 360° d'un membre**
- *Préconditions* : Accès autorisé au membre
- *Déclencheur* : Clic sur un membre ou scan de QR code
- *Flux* : Affichage Profil, Timeline, Score Spirituel (avec sparkline), Alertes, Notes pastorales
- *Post-conditions* : Score recalculé, historique mis à jour

### 8.2 Score Spirituel

**CU-3 : Calcul du Score Spirituel**
- *Axes* : 12 axes (présence, prière, engagement, progression, service, dons, événements, formations, badges, interactions, évangélisation, fidélité)
- *Calcul* : Score pondéré 0-100, recalcul hebdomadaire
- *Historique* : Snapshots via `SpiritualScoreHistory`
- *Alertes* : Score < 30 pendant 3 semaines → escalation

### 8.3 Gestion des Familles

**CU-4 : Organiser une réunion de famille**
- *Préconditions* : Chef de famille ou pasteur
- *Flux* : Créer événement type "Réunion familiale" → convocations → RSVP → liste de présence → cohésion mise à jour

### 8.4 Événements & Présences

**CU-5 : Pointage par QR Code**
- *Prerequisites* : Rôle autorisé
- *Flux* : Responsable génère QR code → membres scannent → présence enregistrie (timestamp + géolocalisation) → dashboard en temps réel

**CU-6 : Géofencing (pointage automatique)**
- *Flux* : Détection entrée zone 200m → notification → confirmation → présence enregistrée

### 8.5 Rapports Pastoraux

**CU-7 : Soumettre un rapport faiseur**
- *Flux* : Remplir formulaire → soumettre → notification chef → validation → PDF généré → propagation au responsable/pasteur
- *Workflow* : Faiseur → Chef → Responsable → Pasteur

**CU-8 : Générer un rapport exécutif IA**
- *Flux* : IA génère mensuellement tendances présence, nouveaux convertis, alertes prioritaires → PDF + résumé

### 8.6 Finances & Mobile Money

**CU-9 : Effectuer un don en Mobile Money**
- *Flux* : Saisir montant → choisir MTN/Orange/M-Pesa → initier transaction → webhook confirme → reçu PDF + email
- *Sandbox* : `MOBILEMONEY_ENABLED=false` → génération locale de référence
- *Polling* : 30s pour MTN (pas de webhook)

**CU-10 : Gérer une tontine**
- *Flux* : Créer tontine → invitations → contributions → attribution → distribution

### 8.7 Parcours de Discipolat & Académie

**CU-11 : Suivre un parcours de discipolat**
- *Étapes* : Prospect → Contact → Visité → Invité → Converti → Intégré → Actif → Leader
- *Flux* : Affichage progrès → validations → badge + certificat à l'issue

**CU-12 : Suivre une formation**
- *Flux* : Parcourir cours → chapitres → quiz → certificat

### 8.8 Gamification (Discipolat Quest)

**CU-13 : Participer à une quête**
- *Flux* : Accepter quête → accomplir tâches (prière, présence, don, partage) → XP + badges + streaks

**CU-14 : Consulter le classement**
- *Flux* : Affichage leaderboard par XP/badges/streaks → filtres période

### 8.9 IA & Intelligence

**CU-15 : Interroger l'AI Pastoral Copilot**
- *Flux* : Question (texte/voix) → IA (Ollama local + RAG) → réponse explicative → actions exécutables
- *Fallback* : Moteur déterministe si Ollama indisponible

**CU-16 : Recevoir des alertes smart**
- *Détection* : Absence 48h, non-contact, rapport en retard, score en baisse
- *Scan* : Automatique quotidien (6h) + manuel, déduplication 7j, escalade

**CU-17 : Utiliser le jumeau numérique**
- *Flux* : Snapshot temps réel → simulation "et si" → prévisions besoins/leaders

### 8.10 Messagerie & Communications

**CU-18 : Envoyer un message privé**
- *Flux* : Composer → envoyer (WebSocket STOMP) → notification push → accusé de réception

**CU-19 : Diffuser un broadcast**
- *Flux* : Composer → cibler (rôle/famille/département/segment) → envoyer (app+email+WhatsApp) → stats engagement

### 8.11 Intégrations Externes

**CU-20 : Interagir via WhatsApp**
- *Flux* : Message WhatsApp → webhook HMAC vérifié → mapping tel→compte → traitement → réponse automatique

**CU-21 : Utiliser l'USSD**
- *Flux* : Composer *XXX# → menu (pointage/dons/statut/événements) → traitement → sync

### 8.12 Administration & Configuration

**CU-22 : Configurer un workflow de transfert**
- *Flux* : Workflow Builder → définir étapes → validateurs → notifications → publier

**CU-23 : Créer une page personnalisée (Page Builder)**
- *Blocs* : 14 types (KPI, tableau, graphique, texte, etc.)
- *Sources* : 22 sources de données dynamiques
- *Layouts* : STACK, GRID_2, GRID_3
- *Permissions* : par rôle, versionnage ConfigRevision

### 8.13 Notifications Multi-canal

**CU-24 : Recevoir une notification**
- *Canaux* : Push (FCM), email (SMTP), SMS (TW), WhatsApp, USSD
- *Templates* : configurables avec variables
- *Ciblage* : par rôle, famille, membre, segment

### 8.14 Conformité & RGPD

**CU-25 : Gérer les données personnelles**
- *Export* : export complet (PDF/CSV)
- *Suppression* : suppression à la demande
- *Consentement* : gestion granularisée

### 8.15 Backup & Restauration

**CU-26 : Sauvegarder l'environnement**
- *Fréquence* : configurable (quotidien/hebdomadaire/mensuel)
- *Storage* : bucket cloud S3-compatible
- *Vérification* : POST `/backups/{id}/verify`
- *Restauration* : points de restauration

---

## 10. SPÉCIFICATIONS FONCTIONNELLES DÉTAILLÉES (SFD)

### 10.1 Gestion des Membres

#### SFD-1 : Création de membre
| Critère | Spécification |
|---------|--------------|
| **Trigger** | Clic sur "Nouveau membre" |
| **Préconditions** | Rôle FAISEUR, CHEF_DE_FAMILLE, RESPONSABLE, PASTEUR ou ADMIN |
| **Données d'entrée** | Nom, prénom, email, téléphone, date de naissance, genre, adresse, famille, faiseur, département |
| **Règles métier** | Email unique par tenant ; Téléphone format international ; Score spiritual initial = 50 |
| **Post-conditions** | Discipolat ID généré (UUID), QR code personnel, notification responsable |
| **Exceptions** | Email existant → erreur ; Téléphone invalide → validation |

#### SFD-2 : Score Spirituel
| Critère | Spécification |
|---------|--------------|
| **Axes** | 12 axes : présence, prière, engagement, progression, service, dons, événements, formations, badges, interactions, évangélisation, fidélité |
| **Calcul** | Score pondéré 0-100, recalcul hebdomadaire |
| **Historique** | Snapshots via `SpiritualScoreHistory` |
| **Alertes** | Score < 30 pendant 3 semaines → escalation |
| **Affichage** | Sparkline (mobile), graphique (web) |

### 10.2 Événements & Présences

#### SFD-3 : Pointage QR & Présence
| Critère | Spécification |
|---------|--------------|
| **QR Code** | Généré par responsable, scanné par les membres (mobile) |
| **Face Recognition** | Module `facerec`, reconnaissance faciale offline |
| **Géofencing** | Check-in automatique zone 200m (`GeofencingController`) |
| **Statistiques** | Dashboard temps réel, export CSV/PDF |
| **Permissions** | Scan : ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR |

### 10.3 Rapports Pastoraux

#### SFD-4 : Rapport Hebdomadaire
| Critère | Spécification |
|---------|--------------|
| **Types** | Hebdomadaire (faiseur/famille), Mensuel, Spécial |
| **Structure** | Présences, activités, alertes, prières, besoins, actions |
| **Workflow** | Faiseur → Chef → Responsable → Pasteur |
| **Validation** | Chaque niveau valide ou demande des modifications |
| **PDF** | Généré via OpenPDF, envoyé par email |
| **Versionnage** | ConfigRevision historise chaque changement |

#### SFD-5 : Rapport Exécutif IA
| Critère | Spécification |
|---------|--------------|
| **Fréquence** | Mensuel automatique |
| **Contenu** | Tendances présence, nouveaux convertis, alertes, recommandations |
| **Format** | PDF + résumé dans l'interface |

### 10.4 Finances & Mobile Money

#### SFD-6 : Dons & Paiements
| Critère | Spécification |
|---------|--------------|
| **Méthodes** | MTN MoMo, Orange Money, M-Pesa, carte bancaire |
| **Configuration** | `.env` : `MOBILEMONEY_ENABLED`, `MTN_*`, `ORANGE_*`, `MPESA_*` |
| **Webhook** | Signature HMAC-SHA256 vérifiée |
| **Polling** | 30s pour MTN (pas de webhook) |
| **Reçus** | PDF généré, envoyé par email |
| **Dons récurrents** | Migration V112 |
| **Sandbox** | `MOBILEMONEY_ENABLED=false` → génération locale de référence |

#### SFD-7 : Tontine Numérique
| Critère | Spécification |
|---------|--------------|
| **Création** | Nom, description, montant, fréquence, membres |
| **Contribution** | Paiement par Mobile Money |
| **Attribution** | Tirage au sort ou ordre cyclique |
| **Notification** | Rappel de paiement, attribution, échéance |

### 10.5 Parcours & Formations

#### SFD-8 : Journey Engine
| Critère | Spécification |
|---------|--------------|
| **Étapes** | Prospect → Contact → Visité → Invité → Converti → Intégré → Actif → Leader |
| **Suivi** | Progression, mentor, objectifs, défis |
| **Certification** | Passeport spirituel (migration V102) |

#### SFD-9 : Academy (Formations)
| Critère | Spécification |
|---------|--------------|
| **Structure** | Cours → Chapitres → Quiz → Certificat |
| **Progression** | Chapitre par chapitre, suivi |
| **Quiz** | Évaluation par chapitre et final |
| **Certificats** | Générés à l'issue d'un cours |

### 10.6 Gamification (Discipolat Quest)

#### SFD-10 : Quêtes, Badges & Classements
| Critère | Spécification |
|---------|--------------|
| **XP** | Gagné via présence, prière, don, partage, défis |
| **Niveaux** | Progression par points d'expérience |
| **Badges** | Attribués pour des réalisations |
| **Streaks** | Suites journalières d'activité |
| **Leaderboard** | Par tenant, par rôle, par période |

### 10.7 IA & Intelligence

#### SFD-11 : AI Pastoral Copilot
| Critère | Spécification |
|---------|--------------|
| **Provider** | Ollama local (OLLAMA_URL, AI_MODEL) |
| **Fallback** | Moteur déterministe contextuel |
| **RAG** | Retrieval sur données autorisées |
| **Guardrails** | Traçabilité, explication signaux, jamais verdicts spirituels |
| **Historique** | Conversations sauvegardées, effaçables |

#### SFD-12 : Smart Alerts & Prédictions
| Critère | Spécification |
|---------|--------------|
| **Anomalies** | Absence 48h, non-contact, rapport en retard, score en baisse |
| **Scan** | Automatique quotidien (6h) + manuel, déduplication 7j |
| **Prédictions** | Signaux faibles, cartes par type/risque/confiance |
| **Jumeau** | Snapshot temps réel, simulation "et si" |

### 10.8 Administration & Configuration

#### SFD-13 : Page Builder
| Critère | Spécification |
|---------|--------------|
| **Blocs** | 14 types : KPI, TABLEAU, LISTE, TEXTE, LIENS, RECHERCHE, IMAGES, GRAPHIQUE, CALENDRIER, TIMELINE, CHECKLIST, FICHIERS, TACHES, FORMULAIRE |
| **Sources** | 22 sources de données dynamiques |
| **Layouts** | 3 : STACK, GRID_2, GRID_3 |
| **Permissions** | Par rôle, versionnage ConfigRevision |

#### SFD-14 : Workflows Configurables
| Critère | Spécification |
|---------|--------------|
| **Types** | Transfert membre, escalation absentéisme, rappel anniversaire |
| **Moteur** | WorkflowService générique |

#### SFD-15 : Modules & Configuration
| Critère | Spécification |
|---------|--------------|
| **Modules** | 38 modules activables indépendamment |
| **Branding** | Nom, logo, couleurs, slogan |
| **Champs custom** | TEXT, NUMBER, DATE, SELECT, BOOLEAN |
| **Langues** | 6 langues configurables |

### 10.9 Notifications Multi-canal

#### SFD-16 : Notifications
| Critère | Spécification |
|---------|--------------|
| **Canaux** | Push (FCM), email, SMS, WhatsApp, USSD, in-app |
| **Templates** | Configurables avec variables |
| **Ciblage** | Par rôle, famille, membre, segment |

### 10.10 Intégrations

#### SFD-17 : WhatsApp Bridge
| Critère | Spécification |
|---------|--------------|
| **Webhook** | Signature HMAC-SHA256 |
| **Cas d'usage** | Demande de prière, RSVP, onboarding, statut |
| **Idempotence** | Gestion des doublons |

#### SFD-18 : Mobile Money
| Critère | Spécification |
|---------|--------------|
| **Opérateurs** | MTN MoMo, Orange Money, M-Pesa |
| **Webhook** | Signature HMAC-SHA256 |
| **Sandbox** | Mode local si `MOBILEMONEY_ENABLED=false` |

### 10.11 Conformité & RGPD

#### SFD-19 : Protection des Données
| Critère | Spécification |
|---------|--------------|
| **Export** | Données exportables (PDF/CSV) |
| **Suppression** | À la demande |
| **Consentement** | Gestion granularisée |

### 10.12 Offline & Sync

#### SFD-20 : Synchronisation Offline
| Critère | Spécification |
|---------|--------------|
| **Local** | Drift SQLite cache local |
| **Sync** | Queue, retry exponentiel |
| **Conflits** | Résolution auto + manuelle |
| **Déduplication** | Idempotence |

---

## 11. API REST — SPÉCIFICATIONS TECHNIQUES

### 11.1 Convention

- **Base URL** : `https://api.discipolat.com/api/v1`
- **Format** : JSON (`application/json`)
- **Auth** : Header `Authorization: Bearer <JWT_RS256>`
- **Multi-tenant** : Claim `tenant_id` dans le JWT
- **Pagination** : `?page=0&size=20&sort=field,asc`

### 11.2 Codes d'erreur

| Code | Description |
|------|-------------|
| 400 | Bad Request (validation) |
| 401 | Non authentifié (token expiré) |
| 403 | Accès refusé (RBAC/IDOR) |
| 404 | Ressource non trouvée |
| 409 | Conflit métier |
| 429 | Rate limit dépassé |
| 503 | Service indisponible (STT non configuré) |

### 11.3 Endpoints clés (164 controllers, ~400+ endpoints)

#### Authentication
| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/auth/login` | Public | Authentification JWT + 2FA |
| POST | `/auth/refresh` | Auth | Rafraîchir le token |
| POST | `/auth/switch-role` | Auth | Changer de rôle |
| POST | `/auth/2fa/enable` | Auth | Activer 2FA |

#### Membres & Âmes
| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| GET | `/members` | Tous | Liste des membres |
| POST | `/members` | FAISEUR+ | Créer un membre |
| GET | `/souls` | Tous | Liste des âmes (filtrable) |
| POST | `/souls` | FAISEUR, CHEF | Créer une âme |
| GET | `/souls/{id}/timeline` | Tous | Historique d'une âme |
| GET | `/souls/{id}/spiritual-score` | Tous | Score spirituel |
| GET | `/souls/{id}/qr-code` | Tous | QR code personnel |

#### Événements & Présences
| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| GET | `/events` | Tous | Liste des événements |
| POST | `/events` | ADMIN, PASTEUR, RESPONSABLE, CHEF | Créer événement |
| POST | `/events/{id}/check-in` | Tous | Pointer présence |
| POST | `/face-checkin` | Tous | Reconnaissance faciale |

#### Rapports & Finances
| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/reports/maker-weekly` | FAISEUR | Soumettre rapport faiseur |
| GET | `/reports/maker-weekly` | CHEF+ | Consulter rapports |
| POST | `/finances/transactions` | ADMIN, PASTEUR | Enregistrer transaction |
| POST | `/giving/donate` | Tous | Faire un don |

#### IA & Communications
| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/ai/chat` | PASTEUR+ | Chat IA |
| POST | `/ai-predictions/generate` | ADMIN, PASTEUR | Générer prédictions |
| POST | `/communications/admin` | ADMIN+ | Broadcast |
| POST | `/messages` | Tous | Message privé |

#### Administrateur
| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| GET | `/platform/modules` | Tous | Liste modules |
| PUT | `/platform/modules/{code}` | ADMIN | Activer/désactiver |
| GET | `/platform/settings` | Tous | Branding |
| PUT | `/platform/settings` | ADMIN | Mettre à jour |

---

## 12. EXIGENCES NON-FONCTIONNELLES (NFR)

### 12.1 Performance

| NFR | Cible |
|-----|-------|
| Temps réponse API | < 500ms (95e percentile) |
| Temps de démarrage app | < 3s (mobile), < 2s (web) |
| Cache offline | 90% des écrans hors ligne |
| Rate limiting | 10 req/min sur login |

### 12.2 Scalabilité

| NFR | Cible |
|-----|-------|
| Tenants simultanés | 1 000+ églises |
| Utilisateurs actifs | 50 000 |
| Requêtes/simultanées | 500 req/s |
| Connexions WS | 1 000 |

### 12.3 Disponibilité & Fiabilité

| NFR | Cible |
|-----|-------|
| Uptime backend | 99.5% |
| Uptime DB | 99.9% |
| Retry | Exponentiel (max 5 essais) |
| Fallback IA | Ollama → moteur déterministe |

### 12.4 Sécurité

| NFR | Cible |
|-----|-------|
| Authentification | JWT RS256, clé RSA 2048 bits |
| Access token | 15 minutes |
| Refresh token | 7 jours, rotation |
| Audit trail | 100% des mutations |
| IDOR | Vérification propriétaire |
| HTTPS | TLS 1.2+ |
| Password | BCrypt coût 12, min 8 chars |

### 12.5 Internationalisation

| NFR | Cible |
|-----|-------|
| Langues | 6 (FR, EN, PT, ES, SW, AR) |
| RTL | Support Arabic |
| Fuseau horaire | Configurable par tenant |
| Devises | Multi-devises |

### 12.6 Qualité de Code & Tests

| NFR | Valeur | Commande |
|-----|--------|----------|
| Backend tests | ~1 034 (JUnit 5 + Mockito) | `mvn test` ✅ |
| Frontend tests | 308 (Vitest) | `vitest run` ✅ |
| Frontend TS | 0 erreur | `tsc -b` ✅ |
| Mobile tests | 325 passés | `flutter test` ✅ |
| Mobile analyze | 0 erreur / 0 warning | `flutter analyze` ✅ |
| **Total** | **1 644** | **100% passants** |

---