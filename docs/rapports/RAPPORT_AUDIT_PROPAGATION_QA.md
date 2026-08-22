# RAPPORT D'AUDIT — Propagation centralisée des entités (refactor en cours)

> Date : 2026-08-21 — Branche `main` (HEAD `b8c5499`, arbre de travail NON commité).
> Nature : audit + QA + corrections accompagnées d'un refactor actif « UNE ENTITÉ = UNE SOURCE DE VÉRITÉ ».
> ⚠️ Le dépôt est en **écriture concurrente** (une session parallèle applique en temps réel les mêmes corrections). Ce rapport est un point de vérité à un instant t, validé par compilation+réexécution réelles.

---

## 1. État des transactions

| Indicateur | Résultat |
|---|---|
| Compile main (538 fichiers) | ✅ SUCCESS |
| Compile tests (`-o test-compile`) | ✅ SUCCESS — 0 erreur |
| `PropagationConsistencyTest` (Spring H2, chaîne réelle) | ✅ 8/8 tests |
| Jeu ciblé des 13 services touchés + intégration propagation | ✅ **171 tests — 0 échec, 0 erreur** |
| Suite complète `mvn test` | ⚠️ SIGNE — bloque sur les tests intégration profil `docker` (voir §5) |

GIT (working tree, non commité) : **35 fichiers modifiés** + **3 nouveaux** sous
`backend/.../common/infrastructure/propagation/`. Soit ~637 insertions / 220 suppressions.

---

## 2. Architecture auditée (la refonte)

**Nouveaux types** (non suivis / untracked) :
- `common/infrastructure/propagation/EntityChangedEvent.java` — événement de domaine, porteur de `entityType`, `entityId`, `changeType (CREATED|UPDATED|DELETED|SOFT_DELETED|RESTORED|STATUS_CHANGED|REASSIGNED)`, `oldValues`, `newValues`, `actorId`, `description` (+ `fieldChanged/previousValue/currentValue`).
- `common/infrastructure/propagation/EntityPropagationPublisher.java` — `@Service` ; méthodes `publishCreated/Updated/Deleted/SoftDeleted/Restored/StatusChanged/Reassigned` ; résout l'acteur via `SecurityUtils` ; publication `ApplicationEventPublisher` (synchrone, dans la même transaction).
- `common/infrastructure/propagation/EntityPropagationListener.java` — `@Component` + `@EventListener @Transactional`; centralise : (1) **AUDIT** (`AuditService.log` avec old/new), (2) **HISTORIQUE** `soul_history`, (3) **NOTIFICATIONS** (chef/faiseur/responsable/statut/état/réaffectation/…), (4) cohérence recherche/statistiques (recalcul à la lecture).

**Design validé** : suppression de la multiplication des effets (les services mutateurs ne créent plus directement `auditService.logSimple`, `notifyMaker`, `logHistory`…) au profit d'une publication unique. La chaîne réelle est **prouvée par `PropagationConsistencyTest`** (8 scénarios H2 : création/modif âme, transferts, historique, notifications, recherche, statistiques).

---

## 3. Corrections apportées (par moi, validées par réexécution)

1. **SoulServiceTest / DepartmentServiceTest / EvaluationServiceTest / EventServiceTest / UserServiceTest**
   - Constructeurs audités : injection des 2 dépendances `EntityPropagationPublisher` + `EntityPropagationListener` du service sous test (synchronisées avec les signatures actuelles des services).
   - SoulServiceTest : suppression des assertions obsolètes `verify(soulHistoryRepository).save(...)` (devenues centralisées) → vérification des `publishCreated(...)` / `publishStatusChanged(...)`.
2. **Dedup UserServiceTest** : champs `propagation*` présents deux fois (session my + concurrente) → doublon supprimé.
3. **EventServiceTest** : import manquant `com.discipolat.modules.users.domain.User` ajouté (usage introduit par la refonte de `EventService.create`).
4. **EventService.create — CORRECTION DE BUG RUNTIME (product) : NPE « Map.of »**
   `Map.of("typeEvenement", saved.getTypeEvenement(), …)` → `Map.of` rejette les valeurs null ⇒ NPE à `EventService:93` quand `typeEvenement`/`dateDebut` null. Payload remplacé par un `LinkedHashMap` défensif (valeur vide par défaut).
5. **InventoryService.delete** — après investigation j'ai rétabli le comportement **jetant** (`findById` → `EntityNotFoundException`) pour correspondre au contrat testé par `delete_NonExistingItem_ThrowsEntityNotFound` (renommage intentionnel du comportement). J'avais d'abord opté pour l'idempotence, j'ai fait marche arrière.

---
## 4. Constats d'audit (résiduels, à trancher / à sécuriser)

### 4.1 RISQUE SYSTÉMIQUE : `Map.of(...)` non null-safe dans les payloads d'événements
La refonte publie des payloads via `Map.of(...)` ; **`Map.of` lève `NullPointerException` si une valeur est null**. J'en ai trouvé + corrigé un (`EVENT`). Il reste un risque du même genre partout où un getter peut renvoyer null :
`FinanceService`, `UserService` (`Map.of("role", saved.getRole())`, `roles`), `TenantService`, `AppointmentService`, `VisitService`, `PlatformConfigService`, `PlatformMenu`, `DictionaryService`, `FeedbackService`, `FamilyService`, `SoulService` (protégié pour `familleId`), `EvangelismService`…
**Recommandation :** centraliser la défense — soit des petites fabriques « payload null-safe », soit un `Map.copyOf` filtré dans `EntityPropagationPublisher`/`EntityChangedEvent`, soit des tests unitaires par service sur les valeurs null (au moins pour les champs métier nullables).

### 4.2 Changements de contrat API induits par la refonte
- `InventoryService.delete(UUID)` : **jette désormais `EntityNotFoundException`** pour un id inexistant (via `findById`), là où auparavant il n'opérait pas (idempotent). Le test concurrent documente l'intention. **Décision produit à acter + casse API à documenter.**
- `CommunicationService` / `DictionaryService` / `ChurchSettingsService` : l'audit est désormais assuré par la chaîne événementielle (le `auditService.logSimple` direct a été supprimé). **Conséquence tests :** les assertions `verify(auditService).logSimple(...)` sont obsolètes → à réécrire pour vérifier `propagationPublisher.publish*` (fait ponctuellement côté âme ; souhaitable d'uniformiser).

### 4.3 `EntityChangedEvent` : `Map.copyOf(...)` rejette tout element null
Le constructeur fait `Map.copyOf(...)` ⇒ un payload contenant une valeur null via call-site planterait aussi ici. Cohérent avec 4.1 : la défense doit être en amont (payload null-safe à la source).

---

## 5. Environ de tests (mention importante)

La suite complète `mvn test` **bloque sur les profils « docker »** (`WorkspaceIsolationIntegrationTest`, `TenantIsolationIntegrationTest`, …) car ils pointent `jdbc:postgresql://db:5432/discipolat` — hôte `db` non joignable hors du réseau docker ⇒ `Hikari` se fige à l'init. **Ce n'est PAS une régression de la refonte**, c'est un pré-requis d'environnement (lancer `docker compose up` — port 5433). Pour valider rapidement : `@ActiveProfiles("test")` (H2 embarqué) ou un run ciblé `-Dtest=Classe1,Classe2,...`.

---

## 6. Recommandations à court terme
1. **Centraliser la null-safety** des payloads (helper défensif unique, cohérent avec la correction EVENT).
2. **Uniformiser les tests unitaires** des services de la refonte : vérifier `propagationPublisher.publish*` (et non `audit logSimple`/`soulHistoryRepository` disparus) dans : Finance, Tenant, Appointment, Visit, Feedback, Evangelism, Alert, Platform, Soul, Family, Department, User, Inventory, Dictionary, ChurchSettings, PageBuilder, PlatformConfig.
3. **Réintroduire un test d'intégration H2 plus large** (profil « test ») pour couvrir les nouveaux services (Event, Finance, Inventory, Tenant, Visit, Appointment) — mêmes garanties que `PropagationConsistencyTest`.
4. **Décision produit à acter et documenter** : comportement `delete` (jet ou idempotent) et audit centralisé data l'absence d'`auditService.logSimple`.

---

## 7. Résultat final vérifié (cette session)
- `mvn -B -o test-compile` → **BUILD SUCCESS**.
- `mvn test -Dtest=<14 classes incl. PropagationConsistencyTest>` → **171 tests, 0 failure, 0 error** (H2).