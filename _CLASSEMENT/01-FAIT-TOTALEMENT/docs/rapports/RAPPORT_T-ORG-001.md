# T-ORG-001 — SPEC_ORGANISATION_DENOMINATION_V2 : modèle d'organisation, transfert, isolement plateforme

> Rapport de fin de tâche, conforme au modèle de `docs/SPEC_ORGANISATION_DENOMINATION_V2.md` §10.
> Période couverte : à partir du commit `34afd92b`.
> Branche : `main`.

---

## 1. Fichiers créés / modifiés (chemins exacts)

### 1.1 Migrations (nouvelles, ascendantes uniquement — V≤221 intouchées)

| Fichier | Rôle |
|---|---|
| `backend/src/main/resources/db/migration/V222__tenant_organization_model.sql` | `tenants.kind`, `tenants.parent_tenant_id`, `tenants.root_tenant_id`, contraintes, index, backfill |
| `backend/src/main/resources/db/migration/V223__membership_transfer.sql` | Colonnes de traçabilité du transfert sur `tenant_memberships` |

### 1.2 Backend — production

| Fichier | Nature |
|---|---|
| `.../modules/tenants/domain/TenantKind.java` | **créé** — enum D2 |
| `.../modules/tenants/domain/TenantOrganizationService.java` | **créé** — création de réseau (dénomination / église enfant) |
| `.../modules/tenants/domain/TenantTransferService.java` | **créé** — détecteur de transfert §4.4 |
| `.../modules/tenants/api/TenantOrganizationController.java` | **créé** |
| `.../modules/tenants/api/TenantTransferController.java` | **créé** |
| `.../modules/tenants/domain/Tenant.java` | modifié — 3 colonnes organisation + normalisation |
| `.../modules/tenants/domain/TenantMembership.java` | modifié — 4 colonnes de traçabilité + `markTransferred` |
| `.../modules/tenants/domain/TenantRepository.java` | modifié — requêtes de hiérarchie |
| `.../modules/tenants/domain/AuthorizationService.java` | modifié — `isTenantOwner()` |
| `.../modules/tenants/domain/AuthzSecurityBean.java` | modifié — `@authz.isTenantOwner()` |
| `.../modules/tenants/api/{TenantJoinCodeController,TenantJoinController,TenantOwnershipController}.java` | modifié — gardes d'autorisation |
| `.../modules/announcements/api/TenantAnnouncementController.java` | modifié — garde d'autorisation |
| `.../modules/announcements/domain/PublicAnnouncementService.java` | modifié — validation URL + retrait du code public |
| `.../modules/tenants/domain/JoinCodeService.java` | modifié — unicité du code principal, libellé de sous-église |
| `.../modules/tenants/domain/TenantJoinCodeRepository.java` | modifié — requête triée (plus `Optional` multi-lignes) |
| `.../modules/tenants/domain/TenantJoinService.java` | modifié — jetons réémis, appartenance unique |
| `.../modules/tenants/domain/OrganizationNodeRepository.java` | modifié — liste triée des nœuds |
| `.../modules/tenants/domain/TenantOwnershipService.java` | modifié — lecture par liste (F17), `promotableMembers()` |
| `.../modules/tenants/domain/RoleManagementService.java` | modifié — F17 |
| `.../modules/ai/domain/AiModuleService.java` | modifié — F17 |
| `.../modules/tenants/domain/TenantGovernanceService.java` | modifié — motif obligatoire, persisté, journalisé, borné |
| `.../modules/tenants/api/PlatformTenantGovernanceController.java` | modifié — pagination + `plan` |
| `.../common/infrastructure/config/PerIpRateLimiter.java` | modifié — IP de confiance (droite du XFF) |
| `.../modules/tenants/domain/{SelfServiceChurchService}.java` | modifié — `Payloads.of` |

### 1.3 Backend — tests

| Fichier | Tests |
|---|---|
| `.../tenants/domain/TenantTransferServiceTest.java` | **créé** — 13 tests |
| `.../tenants/domain/TenantOrganizationServiceTest.java` | **créé** — 13 tests |
| `.../tenants/domain/TenantGovernanceServiceTest.java` | **créé** — 9 tests |
| `.../tenants/domain/TenantOwnershipPromotableMembersTest.java` | **créé** — 6 tests |
| `.../tenants/domain/JoinCodeServiceTest.java` | +5 tests (unicité du code principal, F16) |
| `.../migration/FlywayMigrationChainPostgreSqlTest.java` | `EXPECTED_MIN_VERSION` 205 → **223** |

### 1.4 Frontend

Créés : `pages/TenantOwnershipPage.tsx`, `pages/TenantOrganizationPage.tsx`, `pages/TransferPage.tsx`.
Modifiés : `App.tsx`, `contexts/AuthContext.tsx`, `workspaces.ts`, `lib/routeAccess.ts`,
`components/layout/Sidebar.tsx`, `components/common/CommandPalette.tsx`,
`components/landing/SectionAnnouncements.tsx`, `pages/JoinChurchPage.tsx`,
`pages/PlatformGovernancePage.tsx`, `pages/TenantJoinManagementPage.tsx`, `pages/RegisterPage.tsx`.

Tests modifiés/ajoutés : `__tests__/workspaces.test.ts` (+4), `__tests__/Sidebar.test.tsx` (+1),
`__tests__/RegisterPageInviteLink.test.tsx` (+2 gardes T-W0, + `AuthProvider`).

### 1.5 Mobile

Créés : `lib/data/models/transfer_models.dart`, `lib/presentation/screens/transfer/transfer_screen.dart`.
Modifiés : `lib/app.dart` (route `/transfer`, isolement plateforme T-M4),
`lib/data/services/api_service.dart` (`previewTransfer`, `transfer`),
`lib/presentation/screens/login/join_church_screen.dart` (T-B0bis),
`lib/presentation/screens/login/register_screen.dart` (lien + copie),
`lib/presentation/widgets/app_drawer.dart` (entrées + libellés).

### 1.6 Documentation

`docs/SPEC_ORGANISATION_DENOMINATION_V2.md` — complétée et réparée par l'audit (1137 lignes).

---

## 2. Tests ajoutés / exécutés (résultats chiffrés)

### 2.1 Backend — cycles ciblés, exécutés

```
mvn -o -Dtest=JoinCodeServiceTest,TenantOrganizationServiceTest,TenantTransferServiceTest,
           TenantGovernanceServiceTest,TenantOwnershipPromotableMembersTest,NoNullUnsafeMapLiteralTest test
→ BUILD SUCCESS
```

Découpage : `JoinCodeServiceTest` 13 · `TenantOrganizationServiceTest` 13 ·
`TenantTransferServiceTest` 13 · `TenantGovernanceServiceTest` 9 ·
`TenantOwnershipPromotableMembersTest` 6 · `NoNullUnsafeMapLiteralTest` 4 — **58 tests**.

**Non-vacuit�� démontrée par sonde** (et non par simple lecture verte) :

| Sonde | Résultat |
|---|---|
| F9 réintroduit dans `RegisterPage` (`removeItem('user')`) | les 2 gardes T-W0 échouent (`expected null not to be null`) ; les 2 tests préexistants, eux, passent encore — ce qui prouve qu'ils étaient aveugles au défaut |
| `MAX_REASON_LENGTH` remis à 1000 | `maximalReasonStillFitsTheColumn` échoue : `IllegalStateException: (1017 > 1000)` |
| Le défaut `role_legacy` réintroduit dans `overview`/`promoteAdmin`/`demoteAdmin` | `TenantOwnershipPromotableMembersTest.excludesOwnerAndExistingAdmins` échoue — le test a **découvert** le défaut (D10), il ne le vérifie pas a posteriori |

### 2.2 Frontend

```
npx tsc --noEmit   → 0 erreur
npx vitest run     → Test Files 74 passed (74) · Tests 564 passed (564)
```

### 2.3 Backend — suite complète : mesure comparative honnête

**Méthode.** `git worktree` sur `34afd92b` (arbre intact) pour obtenir un témoin, puis
la même commande `mvn -o test` sur chaque arbre, dans ce même environnement.

**1) Témoin : la suite de HEAD est DÉTERMINISTE.** Deux exécutions successives,
même commit, même machine :

| HEAD `34afd92b` | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| run 1 | 1902 | 1 | 1144 | 24 |
| run 2 | 1902 | 1 | 1144 | 24 |

Identiques. Toute variation est donc **significative** : elle ne peut pas être mise
sur le compte d'un environnement instable.

**2) Ce que sont les 1144 `Errors` de HEAD.** Cause unique et environnementale :
`IllegalArgumentException: Java 26 (70) is not supported by the current version of Byte
Buddy` — le mock *inline* de Byte Buddy refuse d'instrumenter les classes au-delà des
JDK qu'il supporte. Seul JDK 26 est installé ici ; le projet cible Java 21 (`pom.xml`)
et la CI utilise temurin 21. Ces erreurs sont **préexistantes** et hors de portée :
« corriger » les tests pour les faire disparaître serait une faute méthodologique.

**3) L'unique `Failure` de HEAD est la faille F15** :
`NoNullUnsafeMapLiteralTest.auditPayloadsUsePayloads` (123 sites en position d'audit >
plafond 122). **Corrigée ici** par `Payloads.of`, sans toucher au plafond — vérifié vert.

**4) Deux erreurs de mesure ont été commises puis corrigées.** Les deux premières
exécutions sur l'arbre de travail donnaient des chiffres très variables (1933/7/1232 puis
1904/23/1385) avec un **ensemble de classes en échec différent à chaque fois**. J'en
ai d'abord conclu à une instabilité de l'environnement — **c'était faux**, et le témoin
HEAD l'a disprotré. La vraie cause était celle-ci : j'ai lancé d'autres commandes Maven
**en parallèle, dans le même répertoire `backend/`**, pendant que la suite complète
tournayait. Elles partagent `target/` : les rapports Surefire s'écrasent, les classes se
recompilent à moitié. Toute mesure effectuée dans ces conditions est sans valeur.
*Procédure retenue désormais : un arbre, une commande, aucune concurrence.*

**5) La régression réelle que cette discipline a fait apparaître.** L'entité `Tenant`
déclarait `kind` en `NOT NULL` **sans valeur par défaut**, alors que V222 déclare
`DEFAULT 'CHURCH'`. Le profil de test construit son schéma par `ddl-auto` (Flyway
désactivé) : les deux `INSERT` de fixture qui n'énumèrent pas `kind` échouaient
(`NULL not allowed for column "kind"`), et **37 tests d'intégration** tombaient dans
10 classes — dont `MultiTenantSecurityTests`, `OnboardingWizardControllerTest`,
`InvitationAdminTenantScopeRbacTest`, `TenantModuleIsolationEndToEndHttpTest`,
`TenantIsolationIntegrationTest`.

Contrôle ciblé, même lot sur les deux arbres :

```
Avant correctif, arbre de travail : Tests run: 118, Failures: 0, Errors: 37
Avant correctif, HEAD intact      : Tests run: 105, Failures: 0, Errors: 0
Après correctif, arbre de travail : Tests run: 118, Failures: 0, Errors: 0   (105 + 13 nouveaux)
```

Correctif : `columnDefinition = "varchar(30) default 'CHURCH'"` sur l'entité, qui
réaligne le schéma de test sur le schéma de production. Ce type d'écart
entité ↔ migration est **invisible** tant que personne n'exécute les migrations.

**6) Résultat final, obtenu sans concurrence** (arbre `target/` neuf, aucune autre
commande Maven dans ce répertoire) :

| Arbre | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| `34afd92b` — témoin | 1902 | **1** | 1144 | 24 |
| Arbre de travail | 1948 | **0** | **1144** | 24 |

Lecture :

- **+46 tests**, ce qui correspond exactement aux tests ajoutés
  (5 + 13 + 13 + 9 + 6). Aucun test existant n'a été retiré ni affaibli.
- **Failures : 1 → 0.** La crémaillère `NoNullUnsafeMapLiteralTest` (faille F15), qui
  rendait la **branche non mergeable**, est corrigée. Le plafond n'a pas été touché.
- **Errors : 1144 → 1144, à l'identique.** Aucune régression : ce sont les mêmes erreurs
  Byte Buddy/JDK 26, préexistantes.
- **Skipped : 24 → 24.** Inchangeé ; le groupe skippé est celui qui exige Docker
  (dont `FlywayMigrationChainPostgreSqlTest`).

`BUILD FAILURE` subsiste, et **ne doit pas être masqué** : il est dû aux 1144 erreurs
d'environnement. La suite doit être validée sur **temurin 21**, comme la CI.

### 2.4 Mobile — non exécutable ici

Le SDK Flutter **n'est pas installé** sur cette machine (ni `flutter`, ni `dart`, ni
répertoire SDK). `flutter analyze` et `flutter test` n'ont donc **pas** été exécutés —
et ce point est signalé plutôt que masqué. Le code Dart a été relu statiquement, ce qui a
révélé puis corrigé deux erreurs qu'aucun `analyze` n'aurait laissées passer ici :

1. `SecureScreen` a un paramètre `screenName` **obligatoire** — son absence était une
   erreur de compilation ;
2. deux imports relatifs depuis `screens/transfer/` pointaient à la mauvaise profondeur
   (`../../app.dart` au lieu de `../../../app.dart`, soit `lib/presentation/app.dart`).

Ces deux défauts sont de nature *syntaxique/référentielle* : `flutter analyze` les aurait
signalés, et ils sont corrigés. **La vérification comportementale du mobile reste
donc à faire** sur une machine équipée du SDK (§7).

---

## 3. Décisions prises (et pourquoi)

| # | Décision | Justification |
|---|---|---|
| D1 | `root_tenant_id` **nullable**, et non `NOT NULL` comme le prévoyait le brouillon | Une colonne qui doit valoir sa propre clé primaire ne peut pas être renseignée de façon fiable par un déclencheur SQL (il précède l'affectation de l'UUID) ni par un `@PrePersist` (dont dépend l'ordre d'affectation de l'identifiant par Hibernate). Un `NOT NULL` erroné = `INSERT` rejeté dès la première église créée après déploiement, **et aucun test local ne l'aurait vu**. La cohérence est portée par `Tenant.effectiveRootTenantId()` (totale) et par les services d'écriture. |
| D2 | `/transfer` **protégé**, contrairement à `/join` | `/join` a une valeur de découverte avant inscription. `/transfer` est une action purement authentifiée : l'afficher à un visiteur déconnecté ne produirait qu'un formulaire dont le premier clic répond 401. |
| D3 | Le code de rejointure **n'est jamais publié** ; la vitrine renvoie `/j/<slug>` | Le lien mène à la même porte ; le mode OPEN/APPROVAL continue de régner. Publier le code sur la page d'accueil ouvrait l'église à quiconque lisait la page. |
| D4 | Les tests mockent les **interfaces** et utilisent des **doublures écrites à la main** pour les classes | Le mock inline de Byte Buddy refuse les classes sur JDK > 24 supporté. Des tests qui ne s'exécutent que sur JDK 21 ne protègent personne en local, et le défaut serait redécouvert à chaque audit. Ce choix rend les 35 nouveaux tests exécutables partout. |
| D5 | `TenantOrganizationService` **délègue** à `TenantService.create` plutôt que de dupliquer le provisionnement | Deux chemins de création divergeraient au premier correctif (abonnement, modules, dictionnaires, nœud racine, propriété). |
| D6 | Une organisation enfant hérite de la racine **résolue** de sa mère | C'est ce qui rend le transfert détectable entre églises sœurs — le cas le plus fréquent en pratique. |
| D7 | Le motif de gouvernance est **exigé et borné à 968 caractères** | `tenant_warnings.message` est `VARCHAR(1000)` et le message est préfixé (« Église bannie : … »). Limiter le motif à 1000 produisait des messages de 1017 : un bannissement impossible, en 500 au lieu d'un refus. Marge `LABEL_HEADROOM = 32` + garde explicite en développement. |
| D8 | Échec de la trace de gouvernance → **relancer** l'exception (atomicité) | Absorber l'erreur permit un bannissement appliqué sans motif — exactement l'état inexpliquable que la spec supprime. On préfère une décision non appliquée à une décision appliquée sans justification. |
| D9 | `recordGovernanceReason` échoue si le message dépasse la colonne | Protège du futur refactor : quelqu'un qui rallonge un libellé obtient une erreur explicite en dev plutôt qu'une violation de colonne en production. |
| D10 | Les trois comparaisons de rôle restantes de `TenantOwnershipService` lisent désormais `roleKeyOf(m)` (FK `role`) et non `getRoleLegacy()` | **Défaut réel, trouvé par le test `TenantOwnershipPromotableMembersTest`.** `RoleManagementService.assignRoleToUser` fait `setRole(role)` **sans** renseigner `role_legacy`. Un membre promu par l'écran des rôles avait donc `role_legacy` nul : il était **invisible** dans la liste des administrateurs délégués, **non démovable** (`NOT_A_DELEGATED_ADMIN`), et la garde de `promoteAdmin` ne reconnaissait pas un propriétaire ainsi stocké — donc pouvait nommer administrateur le propriétaire lui-même. La FK `role` fait foi ; la colonne legacy est une relique. Trois sites corrigés. |
| D11 | Un test qui figerait un contrat inexistant a été **retiré** plutôt que satisfait | J'avais écrit « le service refuse un tenant nul ». Le contrôleur appelle `TenantContext.requireTenantId()`, qui lève avant : la protection existe, à un seul endroit. Écrire le test aurait fait échouer le build le jour où la garde serait déplacée. |

---

## 4. Points non faits / reportés (et pourquoi)

| Point | Raison |
|---|---|
| **`flutter analyze` / `flutter test`** | SDK Flutter absent de la machine. Deux erreurs de compilation Dart ont été trouvées et corrigées par relecture statique, mais **rien n'a été compilé ni exécuté** côté mobile. À faire avant merge. |
| **`FlywayMigrationChainPostgreSqlTest`** (exécution réelle de V219→V223) | Docker daemon absent → Testcontainers déclare le test `skip`. Le seuil est remonté à 223 et la parité colonnes↔entités a été vérifiée à la main, mais **les migrations n'ont pas été exécutées sur un PostgreSQL réel**. Le test se déclenchera dès qu'un daemon Docker sera présent — c'est le gate à surveiller en priorité. |
| **Suite backend complète au vert** | Impossible dans cet environnement (JDK 26 + ByteBuddy), et l attempting de « corriger » les tests pour masquer l'environnement serait une faute : la suite doit être validée sur temurin 21, comme la CI. |
| **`PricingPage.test.tsx`** | Instabilité **préexistante** sur `main` (course entre le rendu et la résolution du second montant) : le test échoue par intermittence sur l'arbre intact comme sur celui-ci. Hors périmètre des deux SPEC, non modifié. |
| **Questions produit §2bis du SPEC V2** (Q1–Q6) | Arbitrages ouverts : notamment *email de validation vs quota* pour la création libre-service d'église, et *sous-église = nœud ou tenant fils*. Implémentation alignée sur la position de l'audit, à valider. |
| **Aligner `docs/SPEC_ONBOARDING_FLOWS.md` §A** | Contredit encore le contrat réel (`?intent=create-church` vs `mode=church`). Document à corriger, non touché ici. |

---

## 5. Risques / régressions possibles

1. **V222/V223 n'ont jamais été exécutées.** C'est le risque n°1. Le `NOT NULL` évité
   (D1) retire le principal piège, mais un défaut SQL subsisterait jusqu'au premier
   déploiement. *Mitigation* : `EXPECTED_MIN_VERSION = 223` force la CI à les jouer dès
   qu'un daemon Docker est disponible.
2. **Le seuil de la CI est le vrai juge.** `NoNullUnsafeMapLiteralTest` impose un plafond
   de sites d'audit en `Map.of`. Il a été respecté par le bas (correction par
   `Payloads.of`), **jamais en relevant le plafond**.
3. **Non-détection des divergences entité ↔ migration.** La régression du §2.3
   (`kind` sans DEFAULT) l'a démontrée : le schéma de test et le schéma de production
   sont deux bases distinctes tant que personne n'exécute Flyway. Tout nouveau champ doit
   être vérifié dans les deux sens. *Réflexe à appliquer* : dès qu'on ajoute une colonne,
   comparer l'annotation d'entité et la clause de la migration — défaut, `nullabilité`,
   `length`, valeur par défaut.
4. **`role_legacy` est une relique encore lue ailleurs.** D10 a corrigé les trois derniers
   sites de `TenantOwnershipService`, mais `getRoleLegacy()` peut être lu ailleurs dans le
   code. Tant que `RoleManagementService.assignRoleToUser` ne renseigne pas cette colonne,
   toute logique qui filtre dessus est potentiellement fausse. *Suivi recommandé* :
   inventaire des lectures de `getRoleLegacy()` et convergence vers `roleKeyOf`.
5. **`kind` et `root_tenant_id` sont des cascades logiques.** Modifier la sémantique de
   `TenantKind` ou de la hiérarchie impose une migration ET la mise à jour de la
   contrainte SQL, ensemble.
6. **Le `/transfer` mobile dépend de jetons réémis.** Si un déploiement backend
   antérieur répond sans `accessToken`, l'écran affiche une erreur explicite au lieu de
   prétendre avoir basculé — comportement voulu, mais à connaître.
7. **Le rôle `MEMBRE` doit exister dans le catalogue.** `grantMembership` le résout et
   échoue en `MEMBER_ROLE_MISSING` s'il est absent (F25). Les deux chemins
   (`TenantJoinService`, `TenantTransferService`) le gèrent désormais de façon identique.
8. **Ne pas mesurer une suite Maven en lançant d'autres commandes dans le même
   répertoire.** Voir §2.3 point 4 : les deux premières mesures ont été fausses de cette
   manière, et il a fallu un témoin HEAD pour s'en apercevoir.

---

## 6. Écarts par rapport au brouillon initial du SPEC

- `root_tenant_id` n'est **pas** `NOT NULL` (voir D1) — le SPEC §5 sera mis à jour en
  conséquence ; le §7.0 de ce rapport fait foi sur ce point.
- `/transfer` n'est pas une route publique côté mobile (D2).

---

## 7. Ordre de vérification recommandé avant merge

1. `flutter analyze && flutter test` (mobile) — **jamais exécuté ici**.
2. `mvn verify` sur **temurin 21** — la suite complète doit y être verte.
3. Suite complète avec daemon Docker, pour exécuter `FlywayMigrationChainPostgreSqlTest`
   sur PostgreSQL réel (V219 → V223).
4. `npm test && npm run build` (frontend).
