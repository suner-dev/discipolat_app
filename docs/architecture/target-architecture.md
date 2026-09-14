# ARCHITECTURE CIBLE — Church OS v1 (G0.1)

> Architecture de référence pour la commercialisation. Source : PRMPT §37 PHASE 0, Annexe A (contrat de données), Annexe D (43 sections).
> Date : 2026-09-14

---

## 1. VISION GLOBALE — "ONE CORE, MANY CHURCHES, INFINITE CONFIGURATIONS"

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        DISCIPOLAT PLATFORM (SaaS)                          │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    SUPER ADMIN (Plateforme)                         │   │
│  │  Tenants · Plans · Quotas · Feature Flags · Branding Global · Audit │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│         ┌──────────────────────────┼──────────────────────────┐           │
│         ▼                          ▼                          ▼           │
│  ┌─────────────┐            ┌─────────────┐            ┌─────────────┐   │
│  │  TENANT A   │            │  TENANT B   │            │  TENANT C   │   │
│  │ (Église)    │            │ (Réseau)    │            │ (Mission)   │   │
│  └──────┬──────┘            └──────┬──────┘            └──────┬──────┘   │
│         │                          │                          │           │
│  ┌──────┴──────────────────────────┴──────────────────────────┴──────┐   │
│  │                    CHURCH OS CORE ENGINES                          │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐      │   │
│  │  │ People  │ │ Org     │ │ Events  │ │ Assets  │ │ Finance │      │   │
│  │  │ Engine  │ │ Engine  │ │ Engine  │ │ Engine  │ │ Engine  │      │   │
│  │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘      │   │
│  │       │           │           │           │           │            │   │
│  │  ┌────┴────┐ ┌────┴────┐ ┌────┴────┐ ┌────┴────┐ ┌────┴────┐      │   │
│  │  │Disciple │ │ Prayer  │ │ Media   │ │ Health  │ │ Workflow│      │   │
│  │  │ Engine  │ │ Engine  │ │ Engine  │ │ Engine  │ │ Engine  │      │   │
│  │  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘      │   │
│  │       │           │           │           │           │            │   │
│  │  ┌────┴───────────┴───────────┴───────────┴───────────┴────┐      │   │
│  │  │           CONFIGURATION ENGINE (transverse)              │      │   │
│  │  │  Custom Fields │ Workflows │ Statuses │ Permissions     │      │   │
│  │  │  Templates     │ Dashboards│ Menus    │ Forms           │      │   │
│  │  └──────────────────────────────────────────────────────────┘      │   │
│  │                                    │                                │   │
│  │  ┌────┴────┐ ┌────┬────┐ ┌────┴────┐ ┌────┴────┐ ┌────┴────┐      │   │
│  │  │ Audit   │ │Bus │R-Time│ │ Notify  │ │ Offline │ │ Low-Band│      │   │
│  │  │ Engine  │ │Event│Engine│ │ Engine  │ │ Engine  │ │(WA/USSD)│      │   │
│  │  └─────────┘ └────┴────┘ └─────────┘ └─────────┘ └─────────┘      │   │
│  └────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. MODÈLE DE DONNÉES CIBLE (Annexe A — Contrat Contractuel)

### 2.1 Fondations (A.1-A.7) — DDL Contractuel

#### A.1 Organisation & Multi-tenant
```sql
-- tenant (existant, enrichir)
tenant(id uuid pk, name, slug unique, legal_name, logo_url, cover_url, description,
       country, city, timezone, currency, language, status, offline_mode, created_at, updated_at)

-- organization_unit (généralisée, hiérarchie infinie)
organization_unit(id uuid pk, tenant_id fk not null, parent_id fk null, name, code,
       type varchar check (type in ('CHURCH','CAMPUS','MINISTRY','DEPARTMENT',
       'SUB_DEPARTMENT','TEAM','CELL','GROUP','FAMILY')), description, status,
       icon, color, sort_order, config_source check ('DEFAULT','INHERITED','OVERRIDDEN'),
       path ltree|text, level int, created_at, updated_at, deleted_at)
-- index (tenant_id, parent_id), (tenant_id, type), unique (tenant_id, code) where code not null

-- space (espace configurable : DEPARTMENT | FAMILY | SUB_TEAM)
space(id uuid pk, tenant_id fk, organization_unit_id fk not null,
       space_type check ('DEPARTMENT','FAMILY','SUB_TEAM'), template_code, name, code,
       icon, color, description, status, visible_people_scope check ('CAMPUS','CHURCH'),
       configuration_json jsonb, created_at, updated_at, deleted_at)

-- module_definition (catalogue unique)
module_definition(id uuid pk, code unique, name, description, category, version,
       enabled, icon, source check ('CORE','EXISTING','ENGINE'),
       features_json jsonb, created_at, updated_at)

-- space_module (ex department_module → space_id polymorphique)
space_module(id uuid pk, space_id fk, module_definition_id fk, enabled bool,
       configuration_json jsonb, display_order int, created_at, updated_at)
-- unique (space_id, module_definition_id)

-- space_template (20 templates seed)
space_template(id uuid pk, code unique, name, description, icon, color, version,
       modules_json jsonb, default_workflows_json jsonb, default_statuses_json jsonb,
       default_dashboards_json jsonb, created_at, updated_at)
```

#### A.2 Identité, Rôles & Permissions
```sql
-- person (identité unique par tenant)
person(id uuid pk, tenant_id fk not null, first_name, last_name, display_name, gender,
       birth_date, phone_normalized, email_normalized, address, photo_url, status,
       visibility_scope, created_at, updated_at, deleted_at)
-- unique (tenant_id, email_normalized) where not null
-- unique (tenant_id, phone_normalized) where not null

-- membership (appartenance à l'église)
membership(id uuid pk, tenant_id fk, person_id fk, membership_status, joined_at,
       left_at, source, notes, created_at, updated_at)

-- space_membership (appartenance à un espace)
space_membership(id uuid pk, tenant_id fk, person_id fk, space_id fk,
       joined_at, left_at null, status, membership_type, responsibility, notes, created_at, updated_at)
-- index (space_id, status), (person_id, status)

-- role + permission + role_permission (catalogue)
role(id uuid pk, tenant_id fk null, name, code, description, scope_type)
permission(id uuid pk, code unique, name, description)
role_permission(role_id fk, permission_id fk, pk(role_id, permission_id))

-- role_assignment (3 dimensions : permanente, espace, événementielle)
role_assignment(id uuid pk, tenant_id fk, person_id fk, organization_unit_id fk null,
       space_id fk null, role_id fk, started_at, ended_at null, status, reason,
       created_at, updated_at)
-- index (person_id, status), (role_id, status)

-- invitation (workflow complet)
invitation(id uuid pk, tenant_id fk, organization_unit_id fk, space_id fk null, email,
       role_code, token_hash, expires_at, status, invited_by, accepted_at, created_at, updated_at)
```

#### A.3 Configuration
```sql
-- custom_field_definition (19 types supportés)
custom_field_definition(id uuid pk, tenant_id fk, entity_type, space_id fk null, field_code,
       label, field_type, required bool, options_json jsonb, validation_json jsonb,
       visibility_scope, display_order, created_at, updated_at)
-- unique (tenant_id, entity_type, space_id nulls_not_distinct, field_code)

-- custom_field_value
custom_field_value(id uuid pk, tenant_id fk, field_id fk, entity_id uuid, value_json jsonb,
       updated_by, updated_at)

-- custom_status_set (statuts configurables par espace)
custom_status_set(id uuid pk, tenant_id fk, entity_type, space_id fk null, code, name, color,
       icon, display_order, initial bool, final bool, allowed_transitions_json jsonb,
       created_at, updated_at)

-- workflow_definition + steps + transitions + instances + tasks
workflow_definition(id uuid pk, tenant_id fk, entity_type, code, name, space_id fk null,
       enabled bool, version, created_at, updated_at)
workflow_step(id uuid pk, workflow_id fk, step_order, step_type
       check ('APPROVAL','AUTO_ACTION','NOTIFY','EXPENSE','ASSET_STATUS','FORM'),
       name, conditions_json jsonb, assignee_role, assignee_scope, timeout_hours,
       escalation_role, auto_action_json jsonb, created_at, updated_at)
workflow_transition(from_step_id fk, to_step_id fk, on_event, pk(from_step_id, to_step_id, on_event))
workflow_instance(id uuid pk, tenant_id, workflow_id fk, entity_id uuid, current_step_id fk,
       status, started_at, updated_at)
workflow_task(id uuid pk, tenant_id, instance_id fk, step_id fk, assignee_id fk, status,
       due_at, resolved_at, comment, created_at, updated_at)
```

#### A.4 Outbox & Événements
```sql
outbox_event(id bigserial pk, tenant_id, aggregate_type, aggregate_id uuid,
       event_type, payload_json jsonb, status check ('PENDING','PUBLISHED','FAILED','DISCARDED'),
       attempts int, available_at timestamptz, created_at, published_at)
-- index (status, available_at), (aggregate_type, aggregate_id)

processed_event(id bigserial pk, consumer varchar, event_id fk, processed_at)
-- unique (consumer, event_id)  -- idempotence
```

#### A.5 Audit & Historique Métier
```sql
-- Audit technique (hash chain)
audit_event(id bigserial pk, tenant_id, actor_id, actor_email, action, entity,
       entity_id uuid, old_value_json jsonb, new_value_json jsonb, ip, user_agent,
       prev_hash char(64), hash char(64), timestamp)
-- index (tenant_id, entity, entity_id, timestamp)
-- chaîne calculée : hash = sha256(prev_hash || action || entity || entity_id || timestamp || actor_id)

-- Historique métier (générique + vues dédiées)
business_history(id bigserial pk, tenant_id, object_type, object_id uuid, event_type,
       summary text, detail_json jsonb, actor_id, actor_role, space_id fk null,
       happened_at)
-- index (object_type, object_id, happened_at), (space_id, happened_at)
```

#### A.6-A.7 Notifications & Fichiers
```sql
notification(id uuid pk, tenant_id, user_id fk, title, body, type, link, read_at,
       channel check ('IN_APP','PUSH','EMAIL'), event_id fk null, created_at)

notification_rule(id uuid pk, tenant_id, space_id fk null, event_type, channel,
       audience_json jsonb, enabled bool, created_at, updated_at)

-- Fichiers : isolation par tenant (storage: tenants/{tenantId}/...)
```

---

### 2.2 Domaines (A.8-A.17) — Contrats Fonctionnels

| Domaine | Tables principales | Spécificités |
|---|---|---|
| **A.8 Events** | `event`, `location`, `event_space`, `event_team`, `event_assignment`, `event_task`, `event_asset`, `event_expense`, `event_attendance`, `event_document`, `event_schedule` | Transversal, N:N espaces, planning visuel par lieu |
| **A.9 Dress Code** | `dress_code`, `dress_code_rule`, `dress_code_audience`, `event_repertoire`, `event_debrief`, `space_internal_role` | Archives versionnées, notifications ciblées |
| **A.10 Assets** | `asset`, `inventory_item`, `inventory_movement`, `asset_checkout`, `maintenance_record`, `asset_transfer`, `asset_expense` | TCO = somme dépenses liées, QR checkout |
| **A.11 Finance** | `financial_account`, `financial_transaction`, `expense`, `income`, `donation`, `contribution`, `budget`, `budget_line`, `payment`, `ledger_entry`, `reconciliation` | Cœur unique, double entrée, Mobile Money, multi-devises |
| **A.12 Discipleship** | `faith_journey`, `journey_definition`, `journey_stage_definition`, `journey_transition`, `journey_event`, `discipleship_group`, `discipleship_group_member`, `mentor_assignment` | Parcours configurable par église |
| **A.13 Pastoral** | `pastoral_case`, `pastoral_interaction`, `pastoral_action`, `follow_up` | Confidentialité STRICT/INTERNAL/GENERAL, audit d'accès |
| **A.14 Prayer** | `prayer_program`, `prayer_session`, `prayer_slot`, `prayer_request`, `fasting_program` | Créneaux sans chevauchement, archives mensuelles |
| **A.15 Media** | `sermon`, `media_asset`, `media_collection`, `document` | Prédication riche (speaker, verses, résumé, URLs) |
| **A.16 Family** | `family_visit`, `family_reception`, `family_meeting` | Liens space FAMILY + person |
| **A.17 Health** | `patient_record`, `medical_consultation`, `prescription`, `pharmacy_item`, `pharmacy_stock`, `pharmacy_movement`, `health_campaign`, `campaign_participant`, `medical_kit`, `kit_distribution`, `health_referral`, `staff_duty` | Confidentialité = modèle pastoral, traçabilité kits, alertes expiration |

---

## 3. MOTEURS TRANSVERSES (Architecture Church OS)

### 3.1 Configuration Engine
- **Module Catalogue** : `module_definition` (CORE/EXISTING/ENGINE) + feature flags par tenant
- **Template Engine** : 20 templates seed → création espace en 1 clic, modifiable post-création
- **Custom Fields** : 19 types, validation backend uniquement, rendu générique réutilisé
- **Workflow Engine** : Éditeur visuel (drag&drop), approbations, escalade, auto-actions, idempotence
- **Custom Statuses** : Kanban visuel, transitions contrôlées, héritage config
- **Space Config** : Feature picker (modules, pages, boutons, widgets), droits par rôle (admin/pasteur/chef), propagation temps réel < 5s

### 3.2 Event Bus & Transactional Outbox (PRMPT §23)
- **Producteurs** : Écrivent `outbox_event` dans MÊME transaction que mutation métier
- **Dispatcher** : Polling DB / Spring Integration → publie avec retry exponentiel + dead-letter
- **Consommateurs** : NOTIFY, AUDIT, BUSINESS_HISTORY, FINANCE, ANALYTICS, REALTIME
- **Idempotence** : `processed_event` (consumer, event_id unique)
- **20+ événements canoniques** (Annexe E)

### 3.3 Audit Engine ≠ Business History (PRMPT §21-22)
- **Audit** : "Qui a modifié quoi" — hash chain immuable, export chiffré, rétention 95j, suppression interdite
- **History** : "Qu'est-il arrivé à l'objet" — timeline par objet, vues dédiées (asset_history, role_history, etc.)

### 3.4 Real-time Engine (PRMPT §24, §41)
- **WebSocket** : Destinations typées tenant/user/space, isolation stricte (test §40)
- **Notifications** : Multi-canal (in-app WS, push Firebase, email async), règles configurables par espace
- **Scopes** : Personne, son espace, ses rôles, tout le tenant (admins)
- **Sync Web↔Mobile** : < 5s démontré par test E2E

### 3.5 Permission Engine (RBAC + Scope + ABAC)
- **AuthorizationService** : Point unique `can(actor, action, resource, scope)` — 0 `if (role==)` ad hoc
- **Scopes** : PLATFORM, TENANT, REGION, CHURCH, SUB_CHURCH, CAMPUS, DEPARTMENT, FAMILY, OWN, ASSIGNED
- **Rôles vivants** : `PermissionResolver` calcule permissions depuis affectations actives → version `permission_version` → push WS `permissions-changed` → re-rendu UI à chaud

---

## 4. UX 2 NIVEAUX (PRMPT §22, §25, §26, §29-30)

### 4.1 Niveau 1 — Church OS (Vue Globale)
- **Route** : `/app` (dashboard global)
- **Contenu** : KPIs réels (membres, espaces, événements, finance, pastoral, intercession), activité récente (feed), tuiles espaces (icône+couleur), événements à venir, alertes (sans espace, quotas, dress codes)
- **Shell** : Sidebar + Command Palette (Cmd+K) + Recherche globale + Centre notifications temps réel + Navigateur organisation (arbre drag&drop admin)
- **Responsive** : Tablette / mobile-web

### 4.2 Niveau 2 — Department/Family OS (Expérience Générée)
- **Route** : `/app/spaces/:id`
- **Bootstrap** : `GET /api/spaces/:id/bootstrap` → config résolue + modules + statuts + champs + permissions + UIConfig
- **Génération dynamique** : Nav, boutons d'action, widgets, pages par module activé
- **Exemple Audiovisuel** : Overview, Équipe, Équipements, Inventaire, Maintenance, Événements, Planning, Tâches, Budget, Dépenses, Documents, Rapports, Archives
- **Exemple Chorale** : Overview, Membres, Répertoire, Répétitions, Événements, Présences, Documents, Archives
- **Customisation live** : Couleurs/icônes/ordre/visibilité appliqués en direct (propagation §3.1)

### 4.3 Design System Premium (G5.1)
- **Tokens** : `--brand-*` (héritage branding dynamique), light/dark complet, échelle typo
- **Composants** : DataTable (virtualisé, tri, filtre, colonnes perso), Kanban, Calendrier, Timeline, Graphiques, Drawer/Modal, Form Builder, Empty States premium, Skeleton, Optimistic UI, Undo
- **Command Palette** : Cmd+K → recherche globale (personnes, espaces, événements, actions)
- **Raccourcis clavier** + **Accessibilité** : Focus visible, contrastes WCAG AA, navigation clavier, labels, screen readers
- **i18n** : Clés dès 1er écran neuf (`fr.json` défaut, fallback `en.json`)

---

## 5. MOBILE — Terrain Connecté + Offline Ciblé (PRMPT §27-28)

### 5.1 Mobile Terrain (Vraies APIs — Zéro Mock)
- **Connecteur** : Dio + endpoints versionnés, refresh token, header tenant, gestion 401/403
- **Écrans terrain** : QR asset checkout/in, présence événement, tâches, planning, notifications, fiche personne, dress code "Ma tenue", approbations workflow, suivi famille
- **Design** : Même design system (tokens via API branding), dark/light, thumb-friendly

### 5.2 Offline Ciblé (Q3 validée)
- **Lecture** : Cache complet (personnes, événements, dress codes, config UI) — refresh plein + delta
- **Écriture** : File `sync_queue` (opération, payload, client_uuid, retry_count, status) — idempotence serveur par `client_uuid`, retry exponentiel
- **Conflits** : LWW horodaté défaut, champs sensibles → réconciliation manuelle web
- **Toggle tenant** : `offline_mode` = LECTURE | FIELD_OPS | FULL

### 5.3 Portail Basse Connexion (WhatsApp / USSD)
- **WhatsApp sortant** : Webhook événements outbox → notifications ciblées (dress code, rappels, désignation, alertes stock santé)
- **WhatsApp entrant** : "Ma tenue", "Planning", "Don 1000" (lien Mobile Money), "Présence" (flash)
- **USSD** : Menu `*code#` configurable (Événements, Ma tenue, Présence, Dons, Notifications)
- **Opt-in** : Toggle `low_band_enabled` (tenant) + opt-in individuel membre, aucune donnée sensible (pastoral/finance)

---

## 6. SÉCURITÉ & QUALITÉ (Non Négociable)

### 6.1 Règles Absolues (§0.3)
1. **Jamais table par département/famille** — espace = configuration
2. **Jamais trust frontend tenantId** — résolution backend (`CurrentTenantResolver`)
3. **Jamais `findById(id)`** sur donnée sensible — toujours `findByIdAndTenantId(...)`
4. **Permissions backend only** — `AuthorizationService` + scope à chaque endpoint
5. **Données sensibles** (pastoral, finances, personnel) jamais exposées à membre simple
6. **Soft delete + historisation** — jamais suppression physique données critiques
7. **Zéro mock / zéro bouton mort / zéro erreur console / zéro erreur compilation**
8. **Propagation temps réel** web↔mobile (couleurs, noms, membres, désignations, config)
9. **Rôle changé ⇒ interface change auto** (web + mobile) prochaine action
10. **Événement métier majeur ⇒ outbox + consumers** (notif, audit, history, finance, analytics)
11. **Département/Famille peut exister sans remplir tous champs template**

### 6.2 Security Matrix (G1.10, G6.6)
- **Matrice** : Ressource × Action × Rôle × Scope → `AUTORISÉ/REFUSÉ`
- **Ressources** : members, departments, events, assets, finance, settings, invitations, impersonation, custom fields, workflow, dress code, pastoral
- **Tests** : 1 test par cellule (`@ParameterizedTest`), CI = `mvn verify`

### 6.3 Validation Complète (DoD §0.5)
```bash
# Backend
cd backend && mvn verify -B

# Frontend
cd frontend && npm run lint && npm run build && npm run test

# Mobile
cd mobile && flutter analyze --no-pub && flutter test --no-pub
```

---

## 7. MODÈLE COMMERCIAL (Annexe F — Validé 12/09/2026)

### 7.1 Plans SaaS — Dual Market (EUR ◈ FCFA ◈ USD)

| Plan | Membres | Espaces | Stockage | Crédits IA/mois | Événements | Europe | Afrique | Amériques | Annuel (17% = 2 mois offerts) |
|---|---|---|---|---|---|---|---|---|---|
| **DÉCOUVERTE** | 50 | 1 | 100 Mo | 10 | 5 | 0 € | 0 FCFA | 0 $ | 0 |
| **DÉMARRAGE** | 250 | 10 | 1 Go | 30 | ∞ | 9 € | 5 000 F | 10 $ | 90 € / 50k F / 100 $ |
| **CROISSANCE** | 2 000 | 50 | 20 Go | 100 | ∞ | 29 € | 15 000 F | 32 $ | 290 € / 150k F / 320 $ |
| **RÉSEAU & CAMPUS** | 10 000 | 150+ | 100 Go | 500 | ∞ | 79 € | 35 000 F | 85 $ | 790 € / 350k F / 850 $ |

**Règles** : Essai 30j, géo-pricing (IP/sélecteur), prix gelé à souscription, paiements (MoMo/Orange Afrique, Stripe/SEPA Europe, Stripe Amériques), crédits IA par plan, module SANTÉ inclus CROISSANCE+RÉSEAU, frais installation (390€/150kF/425$), upgrade/downgrade self-service prorata.

### 7.2 Business Inputs Requis [BUSINESS_INPUT]
- Comptes paiement réels (sandbox + prod)
- SMTP transactionnel + expéditeur
- Domaine public + sous-domaines
- >3 églises pilotes (Afrique + Europe + Amériques)
- CGU + Privacy par région (RGPD Europe, Cameroun/Afrique, US)
- Marque finale (nom, logo, couleurs vitrine)

---

## 8. PLAN D'IMPLÉMENTATION — 7 PORTES (69 étapes)

| Porte | Étapes | Objectif | Verrou de sortie |
|---|---|---|---|
| **G0** | G0.1→G0.6 | Stabilisation (build vert 3 couches) | `mvn verify` ✅ + `npm lint+build+test` ✅ + `flutter analyze+test` ✅ + tag `v0.10-snapshot-pre-church-os` |
| **G1** | G1.1→G1.12 | Contrats Multi-tenant (§27-72) | Settings/Branding + Plans/Quotas + TenantFeature + Impersonation + Security Matrix + AuthorizationService + Onboarding + Invitations + Héritage + GLOBAL/LOCAL |
| **G2** | G2.1→G2.11 | Cœur Church OS (Config + Moteurs) | OrgUnit généralisée + Catalogue Modules + Templates + Custom Fields + Workflows + Espaces Unifiés + Custom Statuses + Outbox + Audit/History + Real-time |
| **G3** | G3.1→G3.12 | Moteurs Domaine | People (inscription auto) + Memberships (3 dim) + Events + Dress Code/Archives + Assets/TCO + Finance Central + Discipleship + Pastoral + Prayer + Media + **Santé/Infirmerie** |
| **G4** | G4.1→G4.7 | Family OS & Rôles Vivants | Family OS + Chef famille recherche/ajout + Pastorate transferts + **Rôles vivants <5s** + Import/Export + Migration Legacy |
| **G5** | G5.1→G5.10 | UX 2 Niveaux & Mobile | Design System + Church OS + Dept/Family OS + Guards + Admins + Mobile Terrain + Offline + Sync <5s + **WhatsApp/USSD** |
| **G6** | G6.1→G6.10 | Qualité, Sécurité & GO/NO-GO | Search/Export/Delete + IA/Payments + Redis + Non-régression + Perf 1K-10K + Audit Sécurité + QA + Docs + Ops + **GO/NO-GO Checklist** |

---

## 9. GO / NO-GO CHECKLIST (Annexe G)

### 9.1 7 Portes Vertes
- [ ] G0 : Build 3 couches + dépôt propre + tag
- [ ] G1 : Contrats multi-tenant complets
- [ ] G2 : Moteurs Church OS + migrations Annexe A
- [ ] G3 : Moteurs domaine complets (incl. Dress Code + Santé)
- [ ] G4 : Family OS + Pastorate + Rôles vivants <5s + Migration legacy
- [ ] G5 : UX 2 niveaux + Mobile terrain + Offline + Sync <5s + **Low-band**
- [ ] G6 : Search/Export/Delete + IA/Payments + Redis + Non-régression + Perf + Sécurité + QA + Docs + Ops

### 9.2 Sécurité & Qualité
- [ ] Security Matrix : 0 cellule ⚠️
- [ ] IDOR / Cross-tenant / Élévation : 0
- [ ] Performance : budgets p95 atteints (1K-10K tenants)
- [ ] Non-régression : parcours critiques verts
- [ ] Offline : 0 perte donnée à la coupure
- [ ] Temps réel : web↔mobile < 5s démontré

### 9.3 Produit & Utilisateurs
- [ ] 4 parcours démo bout-en-bout (5 espaces / 150 espaces / famille / campus)
- [ ] Guide utilisateur + aide intégrée
- [ ] Branding 2 églises pilotes sans code
- [ ] `/pricing` en ligne : 4 plans Dual-Market + badges "🤖 IA incluse"
- [ ] Crédits IA par plan appliqués + testés

### 9.4 Opérations
- [ ] Staging + Beta déployés + sauvegarde restaurée
- [ ] Monitoring alerté et testé
- [ ] Tous `[BUSINESS_INPUT]` reçus et intégrés

### 9.5 Décision
- [ ] `reports/GO_NO_GO_REPORT.md` : chaque critère ✓ + preuve (commit/rapport/capture)
- [ ] 0 ✗ non justifié
- [ ] Tag `v1.0-commercial-release` + CHANGELOG à jour
- [ ] Décision GO/NO-GO formalisée avec l'équipe

---

## 10. PROCHAINES ACTIONS IMMÉDIATES (G0.1 → G0.6)

1. **G0.1** ✅ — Documents produits : `current-state.md`, `gap-analysis.md`, `target-architecture.md`, `ETAT_AVANCEMENT_CHURCH_OS.md`
2. **G0.2** — Commit travaux WIP multi-tenant (PHASE 10) → tag `v0.10-snapshot-pre-church-os`
3. **G0.3** — Corriger ~100 erreurs compilation backend (Annexe B)
4. **G0.4** — Frontend : lint 0 warning + build + tests verts
5. **G0.5** — Mobile : analyze + tests verts
6. **G0.6** — Gate G0 : validation triade + fiche à jour → **Démarrer G1.1**

> **Règle** : Une étape = un commit atomique (§0.6). DoD global à chaque étape (§0.5). Protocole STOP-NEED-HELP si ambiguïté (§0.8).