# Guide Super Admin & commercial — §G6.8 (Church OS)

> Console de la **plateforme** (au-dessus des tenants/églises) et procédure de vente de
> bout-en-bout : création d'une église cliente, choix de plan, onboarding, import des
> données, mise en beta. Destinée à l'équipe interne (Super Admin / Sales / Ops).
>
> Rôle : seuls `PLATFORM_SUPER_ADMIN` (contrôlé **en base**, jamais par le rôle porté par le
> client) accèdent aux endpoints `/platform/admin/**`. Toute action sensible est journalisée
> (`audit_event`, chaîne hashée).

---

## 1. Concepts

| Terme | Définition |
|---|---|
| **Tenant** | Une église (ou réseau/campus) = unité d'isolation des données (`tenant_id`). |
| **Plan / abonnement** | Offre commerciale (cf. §3), détermine quotas (stockage, IA, membres, modules). |
| **Super Admin** | Opérateur plateforme : provisionne les tenants, gère plans & quotas, impersonne (journalisé). |
| **Tenant Admin / Pasteur** | Responsable **de l'église** — n'a jamais de vue transverse autres tenants. |

Isolation stricte : le backend résout **toujours** le tenant depuis le JWT (`TenantContext`),
`findById`/requêtes sont filtrés tenant (`TenantAwareSimpleJpaRepository`). Revue :
[`security/SECURITY_MATRIX.md`](./security/SECURITY_MATRIX.md).

---

## 2. Console Super Admin (back-office)

### 2.1 Tenants
- **Lister / rechercher** : `GET /platform/admin/tenants`.
- **Créer** : `POST /platform/admin/tenants` (nom, slug unique, pays, devise, timezone, langue).
- **Fiche tenant** : `GET /platform/admin/tenants/{id}` — membres, abonnement, **usage réel**
  (`GET /admin/saas/plans/usage/{tenantId}`). Lazy à l'ouverture, jamais de chiffre inventé.
- **Cycle de vie** : `POST /platform/admin/tenants/{id}/suspend|reactivate|archive`.
- **Édition plan/params** : `PUT /tenants/{id}`.

### 2.2 Plans & SaaS
- CRUD plans : `POST/PUT/DELETE /admin/saas/plans[/{key}]`, `POST/PUT /platform/admin/plans`.
- Quotas appliqués serveur (stockage, crédits IA, nb membres, modules) — consommation via
  `GET /api/saas/usage` ; quota atteint → **message clair + file d'attente**, jamais de
  blocage silencieux.

### 2.3 Impersonation (§43 — dépannage / support)
- `POST /platform/admin/impersonation` (motif **obligatoire**, TTL 30 min, cible = identité
  portée uniquement, sans élévation). **Interdit** d'impersonner un `PLATFORM_SUPER_ADMIN`.
- Chaque session = lignes `IMPERSONATION_START`/`IMPERSONATION_END` (durée, IP, UA).
- Usage : reproduire un ticket, valider une config, **puis quitter** — l'autorité reste serveur.

### 2.4 Journaux & intégrité
- `GET /audit/events` (recherche par tenant/acteur/entité) ; `GET /audit/events/verify-chain`
  (contrôle d'intégrité de la chaîne hashée) ; `GET /audit/history/{type}/{id}` (history métier).

---

## 3. Plans commerciaux (annexe F)

| Plan | Cible | Points clés |
|---|---|---|
| **DÉCOUVERTE** | petites communautés | essentiel : registre, espaces, événements, dress code ; badges 🤖 IA incluse (quota d'entrée). |
| **DÉMARRAGE** | église structurée | + workflow/approbations, finance & dons Mobile Money, plus de crédits IA. |
| **CROISSANCE** | multi-campus naissant | + pasturat/multi-unités, offline terrain, low-band (WhatsApp/USSD), analytics avancés. |
| **RÉSEAU & CAMPUS** | réseaux/dénominations | + hiérarchie REGION/CHURCH/CAMPUS illimitée, héritage de config, quotas élevés, support prioritaire. |

- **Essai** : 30 jours. **Annuel** : 2 mois offerts. **Géo-pricing** (FCFA/EUR/USD) selon pays.
- **Annuaire public** « Églises sur Discipolat » : opt-in par tenant (`public_directory_enabled`)
  → vitrine + lien de souscription.

---

## 4. Procédure de vente — de la signature à la beta

1. **Prospection / démo** — page vitrine `/pricing` (CTA onboarding). Recueillir : nom église,
   pays, taille, besoins (multi-campus ? finance ? low-band ?).
2. **Provisionner le tenant** — console §2.1 : créer le tenant, choisir le plan §3, devise,
   langue, timezone. Vérifier l'émission du slug unique.
3. **Onboarding (§G1.5)** — envoyer l'invitation au responsable : `POST /admin/invitations`
   → e-mail réel + page d'acceptation (token, expiration, anti-rejeu) → le client définit son
   mot de passe et atterrit sur le wizard (branding, espaces depuis templates).
4. **Héritage de configuration (§G1.7/G1.8)** — choisir `DEFAULT/INHERITED/OVERRIDDEN` par
   unité ; ressources `GLOBAL`/`LOCAL`.
5. **Import des données existantes (§G4.6)** — activer `legacy_migration_enabled` puis
   **dry-run** : `POST /legacy-migration/modules/{code}/dry-run` (0 écriture, rapport par table).
   Valider → **replay** : `POST …/migrate`. Rollback possible ≤ 30 j (`…/jobs/{id}/rollback`).
   **Aucune donnée source n'est effacée avant validation.**
6. **Mise en beta** — suivre la check-list de lancement du
   [`RUNBOOK_OPS.md`](./RUNBOOK_OPS.md) §7 (backup post-import, monitoring, smoke check).
7. **Facturation / cycle de vie** — suivi consommation & renouvellement via usage SaaS ;
   suspension/réactivation/archivage selon paiement.

---

## 5. Garde-fous

- **Principe du moindre privilège** : un Super Admin ne consulte les données transverse que
  pour le support, **journalisé**. Aucune donnée sensible client (pastoral, finance) exportée
  hors procédure documentée.
- **RGPD / confidentialité** : mentions et conditions de traitement (cf. [`SECURITY.md`](./SECURITY.md)).
- **Double confirmation** pour toute suppression physique (réservée Super Admin) + audit.
- **Anti-escalade** : `RESPONSABLE`/rôles tenant ne peuvent jamais atteindre les endpoints
  plateforme ; testés en CI (`MultiTenantSecurityTests`).
