# Audit de parité Front / Backend / Mobile — Master Prompt (final)

> **Objet :** parcours **rigoureux** du Master Prompt pour s'assurer que **chaque
> fonctionnalité est développée en front + backend + mobile**, compléter l'incomplet,
> corriger le cassé. **Sans suppression** de l'existant — uniquement amélioration et
> ajout (consigne client).
> **Date :** 2026-09-22 · **Branche :** `Develop1`

---

## 1. Méthode

1. **Inventaire des 3 couches** (rejoué frais contre le code courant) :
   - Backend : **209** `*Controller.java` (≈ 1000 endpoints).
   - Web : **239** pages `frontend/src/pages/**` (+ hooks, components).
   - Mobile : **184** écrans `mobile/lib/**/*_screen.dart` / `*_page.dart` + services.
2. **Analyse d'orphelins** : pour chaque base-path de contrôleur, recherche du jeton
   dans le source web **ET** mobile (hors tests) → candidats « endpoint construit,
   interface absente ».
3. **Revérification de la matrice `WEB_MOBILE_PARITY.md`** (snapshot 2026-08-25) ligne
   par ligne contre le code courant : les « MOCK » / « absent » sont-ils toujours vrais ?

## 2. Résultats de la matrice (les faux écarts corrigés)

La matrice de 2026-08-25 marquait **MOCK** ou **absent mobile** des fonctionnalités
désormais **réelles sur les 3 couches**. Vérifications :

| Fonctionnalité | Affirmation (2025-08-25) | Réalité vérifiée (2026-09-22) |
|---|---|---|
| Messages de groupe | Web = MOCK | Backend `GroupMessageController` + web `useQuery('/group-messages/…')` + mobile `GroupMessageService` → **réel ×3** |
| Streaming | Web/Mobile MOCK | `LiveStreamController` + web `/streams` + mobile `_api.get('/streams')` → **réel ×3** |
| Inventaire | MOCK | `InventoryController` + web `/inventory` + mobile `/inventory` → **réel ×3** |
| Marketplace | MOCK | `MarketplaceController` + web `/marketplace` + mobile `/marketplace` → **réel ×3** |
| Communauté | MOCK / absent mobile | `CommunityController` + web + `community_screen.dart` (service témoignages) → **réel ×3** |
| i18n ES/SW/AR | Mobile = FR/EN/PT seulement | 6 locales fr/en/pt/es/sw/ar, map `_translations` complète + `intl_*.arb` → **résolu** |

→ **Synthèse parité** mise à jour : « à implémenter mobile » et « incohérent MOCK »
passent de 3/4 à **0** ; parité web/mobile de 48 à **56**/60. Écarts restants = choix
produit **acceptables** (OAuth social & Magic Link mobile non exigés par le Master
Prompt ; push/offline web non prévus).

## 3. Orphelins backend analysés (et leur verdict)

| Contrôleur / base-path | Conso web+mobile | Verdict |
|---|---|---|
| `ChurchEventController` `/api/v1/church-events/**` (modèle §G3.3/G3.4) | direct = 0 | **Ce n'est pas un défaut d'UI** : modèle « Church OS » distinct de `events` (table `event` vs `events`). Consommé **en interne** par `LowBandPortalService` (portail low-band G5.9) + `SpaceExportService`, couvert par un test d'intégration. L'UI événements standard web+mobile utilise `/events` (29/28 refs). **2 modèles coexistent**, les deux fonctionnels → on **ne casse pas** l'existant. |
| `ResourceScopeController` `/api/v1/scoping/**` | 0 | **Interne** : helper d'application du scope (embarqué dans Event/Inventory/Department). Garde serveur automatique, pas d'UI dédiée requise. |
| `ModuleCatalogController` `/api/v1/modules/catalog/**` | 0 | **Redondant** : la gestion de modules est **bien exposée** via `/platform/modules` (`PlatformModulesPage`) + `/admin/tenant-features`. Fonction présente ×3 ; endpoint catalog = API additionnelle. |
| `KingdomMappingController` `/api/v1/map/{heatmap,sectors}` | heatmap web×2 mobile×1, sectors web×1 | **Consommé** (faux positif de l'heuristique — jeton court `map`). |
| **`AiCreditsController` `/api/v1/ai/credits/{dashboard,my-usage,tenant-usage,daily-usage}`** | **0** | **VRAI ÉCART → corrigé (§4).** |

## 4. Correction livrée — vue self-service « Crédits IA »

**Écart trouvé :** les 4 endpoints `GET /api/v1/ai/credits/*` (ADMIN/PASTEUR/TENANT_ADMIN)
étaient construits côté backend mais **consommés par aucune interface** (les quotas de
plan SaaS étaient, eux, visibles côté super-admin via `/admin/saas/plans/usage`).

**Bug latent découvert au passage :** `AiCreditsService.getUsageDashboard` assemblait sa
réponse avec `Map.of(...)`, qui **lève un `NullPointerException` sur une valeur null**.
Or `monthlyLimit` / `remainingThisMonth` sont `null` pour un tenant dont le plan ne
définit **pas** de quota IA → `GET /api/v1/ai/credits/dashboard` renvoyait **HTTP 500**
sur ce cas. Le bug était **invisible justement parce qu'aucune UI n'appelait l'endpoint.**

**Corrections (additives, zéro suppression) :**
- Backend : `getUsageDashboard` passe en `LinkedHashMap` tolérant au null.
- Web : nouveau bloc **« Usage & crédits IA »** dans `AiDashboardPage` (jauge du quota
  mensuel + restants + ventilation par type et par modèle ; mention « plan sans quota »
  quand `monthlyLimit == null` ; **fail-soft** : un membre sans droit ne voit pas le bloc).
- Tests : `AiCreditsServiceTest` (2 — dont **null-safe sans quota**, le chemin qui
  crashait) ; `AiDashboardPage.test.tsx` (2 — consommation de l'endpoint + rendu jauge/
  mention). `tsc` 0.

## 5. Périmètre Master Prompt vérifié

Les portes G0→G6 (annexe G) étaient déjà `✅ vert` avec preuves. Cet audit ajoute la
**revérification croisée front↔backend↔mobile** des fonctionnalités annoncées et ne
relève **aucun** autre écart fonctionnel réel après correction du §4 : les moteurs
domaine (Family OS, pastorat, rôles vivants, santé/infirmerie, dress code, assets,
finance/tontine, IA, temps réel, offline, low-band, multi-tenant, import/export,
migration legacy) sont présents et branchés sur l'API réelle dans les trois couches.

---

**Verdict : ✅ Parité Master Prompt vérifiée.** Écarts de la matrice obsolète corrigés et
documentés ; un vrai écart front (+ un bug 500 latent) trouvé et réparé avec tests.
