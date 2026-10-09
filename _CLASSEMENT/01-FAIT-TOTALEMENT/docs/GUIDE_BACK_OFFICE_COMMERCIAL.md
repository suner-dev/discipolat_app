# Guide Back-Office & Commercial — Discipolat Church OS

> Version 1.0 — 2026-09-22 · §72 / G6.8 (tache 3 : Super Admin / Commercial). Voir [ARCHITECTURE.md](ARCHITECTURE.md) · [MULTI_TENANT_ARCHITECTURE.md](MULTI_TENANT_ARCHITECTURE.md) · [ADMINISTRATION_MODEL.md](ADMINISTRATION_MODEL.md) · [RBAC.md](RBAC.md) · [API.md](API.md) · [DEPLOYMENT.md](DEPLOYMENT.md) · [ENV_TEMPLATE.md](ENV_TEMPLATE.md) · [GUIDE_UTILISATEUR.md](GUIDE_UTILISATEUR.md) · [RUNBOOK.md](RUNBOOK.md).

## 1. Roles back-office

| Role | Perimetre | Pouvoirs |
|---|---|---|
| `PLATFORM_SUPER_ADMIN` | Plateforme | Tenants, plans SaaS, quotas, feature flags, impersonation (motif requis, TTL 30 min, journalisee), sante plateforme |
| `TENANT_OWNER` (1/tenant) | Eglise/Reseau | Facturation, branding, modules, utilisateurs, exports |
| `TENANT_ADMIN` | Eglise/Reseau | Operations quotidiennes, espaces, membres, evenements, finances, rapports |

Impersonation : `POST /platform/admin/impersonation` (jamais vers un super admin ; cible conforme au tenant ; fin journalisee avec duree/IP/UA). Matrice : [security/SECURITY_MATRIX.md](security/SECURITY_MATRIX.md).

## 2. Procedure de vente (de la prospection a l'onboarding)

1. **Qualification** : taille (5 vs 150 espaces), regions (Afrique/Europe/Ameriques ; Asie/Oceanie v1.1), langues (FR/EN + PT/ES/SW/AR), devises (EUR/FCFA/USD), modules (finances, WhatsApp, IA).
2. **Plan** : DECOUVERTE (50 membres, 3 espaces, 1 Go, 500 credits IA) · DEMARRAGE (200, 10, 5 Go, 2 000) · CROISSANCE (1 000, 50, 50 Go, 10 000) · RESEAU & CAMPUS (10 000, illimite, 500 Go, 50 000). Page `/pricing` (4 plans, prix EUR/FCFA, badge IA).
3. **Creation eglise** : Super Admin → `POST /tenants` (nom, slug unique, legal_name, logo/cover, devise, fuseau, langue) → `POST /admin/invitations` (TENANT_OWNER) → acceptee → wizard 6 etapes (profil → eglise → campus → departements → familles → lancement) → hierarchie `organization_node` + templates d'espaces appliques.
4. **Donnees cliente** : import CSV (`/import`) ou moteur legacy dry-run + replay (`/data-migration`, V164 ; rapport par table ; source jamais effacee avant validation) ; branding (couleurs, CSS, modules par espace) ; connecteurs (Zapier/Make, Google Calendar, QuickBooks/Xero) ; WhatsApp (Phone ID + token → test → activation) ; Mobile Money (cles Orange/MTN/M-Pesa, `PAYMENTS_WEBHOOK_SECRET`, simulation a 0 en prod).
5. **Recette** : 4 parcours demo (5 espaces / 150 espaces / famille / campus) de bout en bout ; annuaire public opt-in (`public_directory_enabled`) → vitrine + lien souscription.
6. **Go-live** : quotas verifies (`/admin/quotas`, `/ai/credits`) · monitoring actif · sauvegarde OK · `GO_NO_GO_REPORT` (G6.10).

## 3. Operations courantes (back-office)

- Tenants : `GET /tenants`, `/platform/admin/dashboard`, `/admin/dashboard`, `/admin/system-health`, `/admin/cache-stats`.
- Abonnements : `/admin/saas/plans`, `/admin/subscription`, `/admin/quotas` ; depassement IA → file + degradation gracieuse.
- Utilisateurs : invitations (create/resend/revoke, token public rate-limite 5/min/IP, anti-rejeu), roles (`/admin/roles`, `/permissions`), switch tenant (`/tenant-switcher`).
- Contenus : `/pages` (V65), dictionnaires (V42-47), `/config` + revisions (V64), statuts (V150), custom fields (V153), workflows (V151, `/admin/transfers/workflows`).
- Conformite : exports (`/exports`, journalises), GDPR (`/gdpr`), audit hash chain (`/audit`), impersonation audit.

## 4. Tarification & arguments

Dual-market EUR/FCFA/USD ; IA incluse des DECOUVERTE ; offline-first + USSD/SMS (connectivite limitee) ; multi-tenant isole (IDOR-safe) ; migration legacy sans perte ; monitoring + sauvegardes inclus.
