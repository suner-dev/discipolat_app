# TODO — Parcours complet « Archives » (V272)

> Correctifs issus de l'audit profond « Archives » (oct. 2026).
> Règle : **uniquement des ajouts et améliorations, jamais de suppressions.**
> Ni mocks, ni valeurs codées en dur — vraies fonctionnalités fullstack + mobile.

Légende : `[x]` fait · `[ ]` à faire · `[~]` en cours

---

## A. Backend — schéma & droits (migration V272)

- [x] Créer `V272__archives_parcours_complet.sql`
  - [x] §1 — parité UI_ARCHIVES_ACCESS (6 rôles = détenteurs R6A2) : + DEPARTMENT_LEADER, FAMILY_LEADER (globaux + propagation locale)
  - [x] §1-ter — entrée menu `archives` élargie aux 2 rôles (jsonb idempotent)
  - [x] §2 — index unique `(event_id, version)` + détection de doublons avec message clair (rien supprimé/modifié)
  - [x] §3 — colonne `attendance_count` + backfill depuis snapshot + NOT NULL
  - [x] §4 — décision FK documentée (pas de FK : soft-delete + purge conformité), seule porte de purge = rétention opt-in

## B. Backend — domaine & API (service entité, sans rien casser)

- [x] `ChurchEventArchive` : champ `attendanceCount` + contrainte unique
- [x] `ChurchEventArchiveSummary` (record, sans snapshot) + `ChurchEventArchiveFacet` (record)
- [x] `ChurchEventArchiveRepository` : `searchSummaries` paginé (CAST IS NULL, convention PrayerRepository) + `findYearMonthFacets` + `findByTenantIdAndEventId…` + `findByTenantId`
- [x] `ChurchEventRepository` : `findForUpdate` (verrou pessimiste) + `findByTenantIdAndStatut…` paginé
- [x] `ChurchEventService.archiveEvent` : verrou + `attendanceCount` ; `getArchives` + param `eventId` ; `searchArchives` ; `getArchiveFacets`
- [x] `ChurchEventService.updateChurchEvent` : refuse `ARCHIVED` (400) → renvoie vers POST /archive
- [x] `EventService.update` (chemin /events) : même garde miroir
- [x] `ChurchEventController` : `status` enfin appliqué ; `eventId` sur GET /archives ; nouveau `GET /archives/search` ; nouveau `GET /archives/facets` ; audit lecture (`EVENT_ARCHIVES_LISTED` / `EVENT_ARCHIVE_VIEWED`, émis contrôleur car readOnly inhibe le flush)
- [x] `ComplianceManagerService` : param constructeur, branche `EventArchive` dans `purgePolicy` (export avant suppression ; soft = anonymisation), `case "EventArchive"` dans `countPendingPurge`, entrée par défaut d'affichage (5 ans, non active par défaut) — via le port publié `EventArchivePurgePort` (compliance.api) et son impl `EventArchivePurgeAdapter` (events.service) : la porte de purge UNIQUE reste dans compliance sans pénétrer les internes de events (règle R3/ADR-002 validée par `ArchitectureRulesTest`)

## C. Backend — tests (zéro test dédié existait)

- [x] `ChurchEventArchiveServiceTest` (Mockito) : v1/v2, statut, snapshot, `attendanceCount`, audit+outbox, `EntityNotFound` inter-tenant, filtre eventId/espace, refus ARCHIVED, filtre status
- [x] `ChurchEventArchiveHttpIntegrationTest` (MockMvc, profil test) : POST archive 200 + version ; GET list/search/facets/detail ; 403 sans clé ; isolation 2 tenants ; clôtures concurrentes → versions uniques
- [x] Mettre à jour les outillages : `catalog-migrations.txt` (+V272), `gen_h2_seed.py` (+V272, régénérer `import.sql`), `TestPermissionMatrixSeedGuardTest` (+V272), `servedCatalog.ts` (+V272)
- [x] Lancer : compilation + nouveaux tests + gardes (`PermissionKeyCatalogGuardTest`, seed, Flyway chain)

## D. Frontend web

- [x] `EventsPage` : bouton « Clôturer/Archiver » (garde `EVENTS_EXECUTE_ARCHIVEEVENT` servie + legacy, modale de confirmation, mutation, invalide `['church-event-archives']` + `['events']`, toast) — rend la fonctionnalité enfin utilisable
- [x] `ArchivesPage` : passer sur `/search` paginé + `/facets` (filtres serveur, plus de téléchargement intégral) ; détail via `GET /archives/{id}` ; navigation de versions via `eventId` ; Échap + focus trap ; pagination Précédent/Suivant ; mois via `Intl.DateTimeFormat` (locale active) ; statuts via `tText` (+ dictionnaires quand disponibles)
- [x] `routeAccess` + `App.tsx` : ajouter DEPARTMENT_LEADER/FAMILY_LEADER ; corriger le commentaire « parité exacte » (désormais vrai : 6=6)
- [x] `workspaces.ts` : vérifier la couverture des rôles legacy (repli `default` → FULL_NAV)
- [x] i18n : ajouter TOUTES les chaînes (fr + en + es + pt + ar + sw)
- [x] Tests : `ArchivesPage.test.tsx` (liste, filtres, détail, vide, erreur) ; `archivesParity.test.ts` (UI ≡ R6A2 via servedCatalog)
- [x] Lancer : `vitest run` + build

## E. Mobile (Flutter)

- [x] `models/event_archive_model.dart` : modèles `EventArchive` + résumé + facette (fromJson/toJson, sans codegen)
- [x] `events_service.dart` : `archiveEvent`, `getArchives` (paginé), `getArchive`, `getArchiveFacets`
- [x] `screens/archives_screen.dart` : liste (année/mois), pagination, détail + navigation versions
- [x] `event_detail_screen.dart` : action « Archiver » (garde `canServed('EVENTS_EXECUTE_ARCHIVEEVENT')` + confirmé, snackbar, refresh)
- [x] `app.dart` : route `/archives` + `_routeRoles` + `routePermissions`
- [x] `app_drawer.dart` : entrées Archives par rôle + cas d'icône
- [x] `app_localizations.dart` : nouvelles clés × 6 locales (fr/en/pt/es/sw/ar)
- [x] Test : `test/event_archive_contract_test.dart` (convention `_FakeApi` existante)
- [x] Lancer : `flutter analyze` + `flutter test`

## F. Docs & conformité front

- [x] `docs/API.md` : lignes pour `/archives/search`, `/archives/facets`, param `eventId`, `status` fonctionnel
- [x] `ComplianceDashboardPage` : ajouter le type de données `EventArchive` aux options de politique de rétention (si sélecteur présent)
- [x] `CHANGELOG.md` : entrée V272 (si format existant le permet)

## G. Validation finale

- [x] Backend : `mvn` compile + tests ciblés + gardes verts
- [x] Web : `vitest run` complet vert + `tsc`/build vert
- [x] Mobile : `flutter analyze` propre + tests verts
- [x] Relecture croisée : aucun endpoint supprimé, aucune clé retirée, aucun comportement legacy modifié (compatibilité /archives sans params)
