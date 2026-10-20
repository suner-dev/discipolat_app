# Changelog — Discipolat

> Journal des versions notables. L'historique **complet et détaillé** (par
> version, par couche, avec preuves de tests) est tenu dans
> [docs/CHANGELOG.md](docs/CHANGELOG.md). Ce fichier racine résume les versions
> publiquement significatives et suit le format [Keep a Changelog](https://keepachangelog.com/fr/).

Le projet respecte le [Semantic Versioning](https://semver.org/lang/fr).

---

## [Non publié] — Parcours « Archives » complet (V272)

Lecture et clôture d'archives enfin utilisables partout, **additif strict**
(aucun endpoint supprimé, aucun comportement legacy modifié) :

- **Backend** : migration `V272` (parité UI_ARCHIVES_ACCESS ≡ R6A2 sur 6
  rôles, unicité `(event_id, version)`, colonne `attendance_count`) ;
  `GET /church-events/archives/search` (paginé, sans snapshot) et
  `GET /church-events/archives/facets` ; `eventId` et `status` effectifs ;
  verrou pessimiste à la clôture ; archives dans la rétention conformité
  (opt-in). 28 tests dédiés.
- **Web** : bouton « Clôturer et archiver » dans `EventsPage` ; `ArchivesPage`
  refondue (filtres serveur, pagination, détail par version, Échap + focus
  trap, mois localisés) ; i18n 6 locales ; tests de parité UI ≡ matrice servie.
- **Mobile** : écran `ArchivesScreen` + détail versionné, action « Clôturer et
  archiver » dans `EventDetailScreen`, route `/archives` gardée par rôle et
  par clé servie, drawer et i18n 6 locales, test de contrat dédié.

Détail complet : [docs/CHANGELOG.md](docs/CHANGELOG.md).

---

## [1.0.0] — 2026-10-09 — « Church OS » commercial release candidate

### Ajouté
- **Cycle « église d'abord »** : sélecteur d'église (`ChurchPicker` web + mobile),
  landing publique par église (`/e/:slug`, double opt-in, `noindex`), bouton retour
  systématique (web + conversion `go`→`push` mobile).
- **Push notifications réelles** via Firebase Cloud Messaging
  (`FirebaseAdminPushGateway`, `sendEachForMulticast`).
- **Module Backup** (api/domain/infrastructure) — `POST /backups/{id}/verify`.
- **Payouts universels** : `StripePayoutProvider`, `SepaDirectDebitProvider`,
  `PayPalPayoutProvider` derrière une interface `PayoutProvider` pluggable.
- **Devises ISO-4217** par tenant (`Iso4217CurrencyValidator`, `TenantCurrencyResolver`).
- **Fondations de sharding** : `ShardedTenantDataSource`, `TenantShardExecutor`,
  `ShardRouting`.
- **2FA TOTP** (`TwoFactorService`, `TwoFactorController`).
- **Gate de non-régression Flyway sur PostgreSQL réel** (Testcontainers,
  `FlywayMigrationChainPostgreSqlTest`) : applique toute la chaîne de migrations,
  `validate()` propre, aucune dérive entité/schéma.
- **Mesure de couverture** (JaCoCo) et **SCA Java** (OWASP `dependency-check`).
- **Gouvernance** : `LICENSE`, `NOTICE`, `SECURITY.md`, `CONTRIBUTING.md`,
  `CHANGELOG.md`, `KNOWN_ISSUES.md`, ADR-001→004.

### Corrigé
- **H8** : `organization_nodes.path` (ltree → text, migration `V187`) — le
  provisionnement d'un nouveau tenant n'échoue plus.
- **Schema drift events** (V194, V203, V204, V205) : tables/colonnes réalignées
  sur le schéma migré réel.
- Notifications câblées dans l'outbox (`NotificationService` + `PushNotificationService`).
- Configuration IA fail-closed (Ollama/STT : 503 honnête plutôt qu'un faux repli).

### Sécurité
- Audit offensif sans IDOR / cross-tenant / élévation ; JWT RS256 clés hors dépôt ;
  isolation multi-tenant prouvée bout-en-bout (`TenantModuleIsolationEndToEndHttpTest`).

---

## Références
- Détail complet et daté : [docs/CHANGELOG.md](docs/CHANGELOG.md)
- Points ouverts et assumés : [KNOWN_ISSUES.md](KNOWN_ISSUES.md)
