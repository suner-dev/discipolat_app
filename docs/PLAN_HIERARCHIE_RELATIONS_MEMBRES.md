# PLAN — Hiérarchie & Relations personnelles des membres (« Mon encadrement »)

> Fonctionnalité : tout membre peut **connaître en détail son rôle et toute sa
> hiérarchie** (arbre multi-branches : chefs, responsables, pasteurs…), peut
> **déclarer lui-même** dans son profil son pasteur / ses supérieurs / son
> mentor / son parrain **selon le paramétrage de son église**, et ce membre
> apparaît **automatiquement avec notification chez son supérieur** dans la
> liste « ses membres ». Toute personne affichée est cliquable jusqu'à sa
> fiche complète. Web **et** mobile.
>
> Règle de travail : **ne rien supprimer, que de l'additif** (non-destructif).
> Auteur : agent senior fullstack. Statut : **TERMINÉ — testé et commité**.

---

## 1. AUDIT — ce qui EXISTE DÉJÀ (vérifié par lecture du code)

### Backend (Spring Boot, `com.discipolat`)
| Brique | Fichier(s) | Verdict |
|---|---|---|
| Arbre organisationnel V3 multi-nœuds (path, niveau, responsable de nœud, move/copy/bulk) | `modules/tenants/domain/OrganizationNode.java`, `OrganizationHierarchyService.java`, `OrganizationHierarchyController.java` (`/api/v1/org`) | ✅ existant, **conservé** |
| Assignations membre×rôle-capacité×nœud (0..n = multi-branches) | `MemberRoleAssignment.java` (V226) + `MemberRoleAssignmentService.java` | ✅ existant, **réutilisé** |
| Rôles custom par église + titres localisés par nœud | `tenants/domain/Role.java` (priority), `RoleTitle.java` (label + labelPlural par rôle×nœud, V225), `RoleTitleService.getEffectiveLabel` | ✅ existant |
| **Responsable d'un nœud** | `OrganizationNode.responsibleId` + `OrganizationNodeRepository.findByResponsibleId` | ✅ existant… **mais inexploité** → exploited en §3 |
| Dictionnaires paramétrables par tenant (« paramétrage de l'église ») | `platform/domain/DictionaryEntry`, `DictionaryService.defaultSeedData()` / `seedForTenant` | ✅ existant → **nouvelle clé `MEMBER_RELATION_TYPE`** |
| Adhésion membre×tenant avec portée nœud | `TenantMembership.scopeId/scopeType` — **très utilisé** (`AuthorizationService`, `NodeAggregateService`, `TenantSwitcher`, …) | ✅ existant, réutilisé |
| Parrainage de prospects | `modules/referrals/…` `Referral.parrainId` | ✅ existe mais **autre périmètre** : la cible est un prospect externe sans `userId`, et `Statut.INSCRIT` ne stocke **aucun** compte lié → non raccordé (voir §7) |
| Suivi disciplolat | `Soul.faiseurId`, `User.familleGereeId`, `Department.responsableId` | ✅ existant → exposé dans `suivi` |
| Fiche utilisateur complète cliquable | `GET /api/v1/users/{id}/detail` (`UserService.getUserDetail`) | ✅ existant → **enrichi** |
| Notifications in-app + templates + préférences canal | `NotificationService.create(...)` | ✅ existant → **2 nouveaux types** |
| Règles d'architecture multi-tenant | filtre `tenantFilter`, `TenantContext`, `TenantFilterInterceptor` | ✅ existant → **invariant verrouillé par un test** |

### Frontend (React 19)
| Brique | Fichier(s) | Verdict |
|---|---|---|
| Fiche modal utilisateur | `components/users/UserDetailModal.tsx` (unique consommateur : `UsersPage`) | ✅ existant → **enrichi** |
| Arbre org viz | `components/organization/OrgTreeNav.tsx` | ✅ existant mais **non réutilisable** pour un encadrement personnel (typé `TreeNode`, compteurs en dur) → **nouveau composant générique** |
| Profil self-service (`PUT /users/me`) | `pages/ProfilePage.tsx` | ✅ existant → **section ajoutée** |
| Dictionnaire client | `hooks/useDictionaries.ts` (`label()` renvoie `''` si absent → repli obligatoire côté appelant) | ✅ existant |
| i18n strict fr/en/pt/es/sw/ar | `src/i18n/*.ts`, `t`/`tText` | ✅ existant → **parité vérifiée par script** |

### Mobile (Flutter)
| Brique | Fichier(s) | Verdict |
|---|---|---|
| Client API réel | `lib/data/services/api_service.dart` (dio, refresh 401, `X-Org-Id`) — **pas** `lib/api/api_service.dart` (stub legacy quasi mort) | ✅ existant → **réutilisé** |
| Service org V3 | `lib/data/services/organization_v3_api.dart` | ✅ modèle de style (`ApiService` injecté, `library;`, parsing dans le service) |
| Modèles | `lib/data/models/*.dart` — classes immuables écrites à la main, **aucun** freezed | ✅ style respecté |
| Fiche membre | `lib/presentation/screens/users/user_detail_screen.dart` (lit le **même** `/users/{id}/detail`) | ✅ existant → **section ajoutée** |
| Profil | `lib/presentation/screens/profile/profile_screen.dart` (carte `GlassCard`, `ApiService?` injectable pour les tests) | ✅ existant → **section ajoutée** |

## 2. AUDIT — ce qui MANQUE (le cœur de l'idée)

1. ❌ **Aucune entité de relation inter-membres déclarative.** Le membre ne peut
   pas déclarer son pasteur/supérieur/mentor/parrain enregistré.
2. ❌ **Aucune notification « un membre se rattache à moi »** ni endpoint
   « la liste des membres qui se sont rattachés à ce supérieur ».
3. ❌ **Aucun agrégat « hiérarchie complète d'un utilisateur »** multi-branches.
   `RoleManagementController./hierarchy` est une **liste plate de rôles triée par
   priorité**, pas un arbre, et pas la hiérarchie d'**une personne**.
4. ❌ **Aucun rendu en arbre** de l'encadrement (seulement des listes).
5. ❌ Les branches organisationnelles ne se remplissaient que via assignation V3
   ou adhésion de portée de nœud — **or presque toutes les adhésions sont
   `TENANT`** : `branches` était vide pour la majorité des membres, alors que
   le signal pertinent (`responsible_id` du nœud) existait déjà en base.

## 3. CONCEPTION RETENUE (additive, sans dette nouvelle)

- **`MemberRelation`** = arête dirigée `from` (membre) → `to` (autorité
  déclarée), typée par le dictionnaire `MEMBER_RELATION_TYPE`. Types
  **paramétrables et désactivables** par église, avec précédence
  **ligne du tenant > ligne globale (`tenant_id IS NULL`) > défaut technique**.
- Déclaration **immédiate et automatique** : `ACTIVE` direct + **notif IN_APP
  chez `to`** ; traçabilité = `REVOKED` (jamais de purge).
- **Plafonds** : pas d'auto-rattachement, même tenant, un seul `ACTIVE` par
  (from, to, type), **10 sortants** et **500 entrants** par membre, pagination
  à 200/page.
- Destinataire = utilisateur **enregistré et actif**, cherché par id ou email.
- **Hiérarchie agrégée** (`GET /api/v1/hierarchy/me` et `/users/{id}`) : rôles
  legacy + capacités×nœuds avec `RoleTitle`, **branches multi-branches** avec
  trois origines tracées, chaîne d'ascendance jusqu'à la racine avec le
  responsable de chaque niveau, relations entrantes/sortantes, **encadrement
  pastoral** (`suivi`) et **`resume`** (complétude + origines).
- **Liste « ses membres »** paginée (`GET /api/v1/relations/me/members`).
- `GET /users/{id}/detail` **enrichi** de `relations`, `hierarchie`,
  `hierarchiePartielle` (additif, rétrocompatible).
- Migrations **V231** (table) et **V232** (backfill `NOTIFICATION_TYPE`).
  Dialecte-safe : unicité vérifiée au service, pas d'index partiel.
- Sécurité : `isAuthenticated()` pour le self-service ; gardes de rôle alignées
  sur `/users/{id}/detail` pour la lecture d'autrui ; **tenant issu du seul
  contexte serveur** et **revérifié explicitement** sur le membre chargé.

## 4. TÂCHES — ÉTAT RÉEL (☑ fait et vérifié · ☐ reste)

### Backend
- [x] B1. `V231__member_relations.sql` : table `member_relations`, FK vers
  `tenants`/`users`, `CHECK` de cohérence temporelle, 4 index, seed
  `MEMBER_RELATION_TYPE` par tenant. Conventions V222–V230 (TIMESTAMPTZ,
  `now()`, `uuid_generate_v4()`).
- [x] B2. `TypeNotification` : `RELATION_DECLAREE`, `RELATION_REVOQUEE`.
- [x] B3. Module `modules/relations` : entité, dépôt, `MemberRelationService`
  (invariants, catalogue des types, pagination, notifications défensives,
  audit), `UserHierarchyService` (agrégat), 2 contrôleurs, `MemberRelationView`.
- [x] B4. `DictionaryService.defaultSeedData()` + `NOTIFICATION_TYPE` enrichi.
- [x] B5. `UserController.getUserDetail` : clés `relations`, `hierarchie`,
  `hierarchiePartielle`, `hierarchieIndisponible`.
- [x] B6. Tests : 31 unitaires + 18 HTTP/RBAC/isolation + 2 architecture.
- [x] B7. `mvn compile` + suite complète (H2) — voir §5.
- [x] B8. `V232__notification_type_dictionary_backfill.sql` : 12 types
  manquants (libellé + couleur) pour chaque tenant.
- [x] B9. `TenantFilterDefArchitectureTest` : invariant « un seul
  `@FilterDef(tenantFilter)` ».

### Frontend
- [x] F1. `components/relations/MyRelationsCard.tsx` : liste + ajout (type du
  dictionnaire, recherche membre, note) + **mes membres paginés et cliquables**.
- [x] F2. `components/relations/HierarchyTree.tsx` : **composant d'arbre
  générique** + `useBranchTree`.
- [x] F3. `UserDetailModal.tsx` : arbre, encadrement pastoral, ascendants
  unifiés, membres rattachés, **fil d'Ariane + Retour**, **état d'erreur**, garde
  corrigée.
- [x] F4. `ProfilePage.tsx` : section + ouverture de la fiche complète.
- [x] F5. i18n **parité stricte** vérifiée : 30 clés `relations.*` + 26 clés
  `hierarchy.*` × 6 locales.
- [x] F6. Tests vitest : 9 (`MyRelationsCard`) + 8 (`UserDetailModal`).

### Mobile
- [x] M1. `data/models/member_relation.dart` (modèles, parsing défensif) +
  `data/services/relation_service.dart` (source paginée).
- [x] M2. `presentation/widgets/hierarchy_card.dart` : **l'arbre** +
  ascendants + encadrement pastoral + message de dégradation.
- [x] M3. `my_relations_card.dart` : pagination, nom tapable, détachement
  conditionné, état d'erreur.
- [x] M4. `notifications_screen.dart` : couleurs + icônes des 2 nouveaux types.
- [x] M5. i18n : 34 clés × 6 locales, parité stricte vérifiée.
- [x] M6. Tests : 7 (profil) + 22 (modèles + rendu de l'arbre).

### Documentation
- [x] D1. `docs/ADR_001_MEMBER_RELATIONS.md` (pourquoi `MemberRelation` et pas
  `modules/mentoring`).
- [x] D2. `docs/DATABASE.md` §2 + §4 (migrations, index).
- [x] D3. `docs/RBAC.md` §4.11 (gardes, révocation, isolation).
- [x] D4. `docs/ARCHITECTURE.md` §7 bis (module + invariants).
- [x] D5. `docs/CHANGELOG.md` (entrée « Non publié »).
- [x] D6. `docs/API.md` : **généré** par `scripts/generate-api-docs.sh` — pas
  d'édition manuelle (règle du dépôt).

## 5. PREUVES D'EXÉCUTION (mesurées, pas déclaratives)

| Vérification | Commande | Résultat |
|---|---|---|
| Compilation backend | `mvn -o -DskipTests compile` | **BUILD SUCCESS** |
| Tests unitaires relations | `mvn -o test -Dtest='MemberRelationServiceTest,UserHierarchyServiceTest'` | **31 run, 0 failure, 0 error** |
| Tests HTTP / RBAC / isolation | `mvn -o test -Dtest='RelationApiSecurityTest'` | **18 run, 0 failure, 0 error** |
| Garde d'architecture | `mvn -o test -Dtest='TenantFilterDefArchitectureTest'` | **2 run, 0 failure, 0 error** |
| Contextes Spring complet | `mvn -o test -Dtest='EventTableContractTest,MultiTenantSecurityTests,OnboardingWizardControllerTest,PeopleCriticalPathIntegrationTest,PeopleRoleAssignmentRbacTest,TenantSwitcherMultiMembershipTest,InvitationAdminTenantScopeRbacTest,SpaceCriticalPathIntegrationTest'` | **84 run, 0 failure, 0 error** — c'est exactement la classe de tests que le défaut #1 faisait échouer |
| **Suite backend complète** | `mvn -o test` | **2047 run, 0 failure, 4 errors** (voir §5.1) |
| Chaîne Flyway sur PostgreSQL réel | `mvn -o test -Dtest='FlywayMigrationChainPostgreSqlTest,EventTableContractTest'` | **19 run, 0 failure, 0 error** (dont V231 et V232) |
| Validation SQL déterministe | `./scripts/validate-migrations-v231.sh` | **OK** — table/index/CHECK/FK/CASCADE, seed et backfill **idempotents**, libellé personnalisé **préservé**, base de dev **intacte** |
| Typage frontend | `npx tsc --noEmit` | **0 erreur** |
| Lint frontend | `npx eslint` (relations, modal, profil, notifications) | **0 erreur** (warnings `any` pré-existants) |
| Parité i18n web | script node sur `src/i18n/*.ts` | **toute clé `fr` existe dans les 6 locales** |
| **Tests frontend (suite complète)** | `npx vitest run` | **86 fichiers, 687 tests, 0 failure** |
| Analyse Dart | `flutter analyze --no-pub` | **0 error** (les warnings restants sont antérieurs et hors périmètre) |
| Tests mobile (feature) | `flutter test` (2 fichiers) | **29 run, 0 failure** |
| Parité i18n mobile | script sur `app_localizations.dart` | **34 clés × 6 locales** |

### 5.1 Les 4 « errors » de la suite backend — analyse honnête

**Aucun n'est imputable à cette feature.** Ils sont de deux natures :

| Test | Cause | Preuve |
|---|---|---|
| `FlywayMigrationChainPostgreSqlTest` | **Environnement** : testcontainers n'arrive pas à démarrer `postgres:16-alpine` (démarrage Docker sous charge, nombreux conteneurs périmés). | **Passe** en relance isolée : `Tests run: 11, Failures: 0, Errors: 0`. |
| `EventTableContractTest` | **Environnement** : même symptôme, image `postgres:16`. | **Passe** en relance isolée : `Tests run: 8, Failures: 0, Errors: 0`. |
| `OnboardingStepActionsTest.modulesValidatesAgainstCatalogAndEnables` | **Modification tierce non liée**, déjà présente dans l'arbre de travail avant ce travail : `OnboardingStepActions` appelle désormais `ModuleCatalogService.resolveCanonicalCode(...)`, que le test ne mocke pas → `Module inconnu : people`. | Le fichier concerned n'appartient pas à cette feature et n'a **pas été commité** ici (cf. §9). |

Pour lever le doute sur les migrations malgré l'instabilité de testcontainers,
`scripts/validate-migrations-v231.sh` rejoue V231 et V232 sur le PostgreSQL de
développement dans un **schéma jetable**, et contrôle explicitement : création
de la table et des 4 index, `CHECK` de statut, `CHECK` de cohérence
temporelle, clés étrangères, `ON DELETE CASCADE`, **idempotence** des deux
seeds, **préservation** d'un libellé personnalisé par l'église, et absence
d'impact sur le schéma public.

### 5.2 Défauts corrigés en cours de route (trouvés par vérification stricte)

| # | Défaut | Gravité | Correctif |
|---|---|---|---|
| 1 | Second `@FilterDef(tenantFilter)` sur la nouvelle entité → `EntityManagerFactory` **ne démarre pas** → toute l'application tombe | **Bloquant production** | `@FilterDef` supprimé (une seule définition, sur `User`) + `TenantFilterDefArchitectureTest` |
| 2 | Un type `MEMBER_RELATION_TYPE` **désactivé** par l'église restait proposé **et accepté** | Fonctionnel | Catalogue avec précédence tenant > global > défaut ; `RELATION_TYPE_DISABLED` |
| 3 | Fuite inter-tenant possible : `getHierarchy`/`summarize` ne vérifiaient pas le tenant du membre chargé | Sécurité | `requireTenantUser` explicite (→ 404, jamais 403) |
| 4 | Contrôle de doublon sans `tenantId` (dépendait du seul filtre Hibernate, inactif hors requête) | Sécurité | `tenantId` dans la signature du dépôt |
| 5 | `chaine` sérialisait `null` au lieu de `{nœud, responsable}` | Fonctionnel | Chaîne reconstruite + test |
| 6 | N+1 systématique (2 SELECT par relation, 1 par nœud) + 1 `getAncestors` par branche | Performance | Résolution par lot, catalogue chargé une fois, ancêtres dérivés d'un seul chargement |
| 7 | « Ses membres » ni paginé ni borné (500 entrants → réponse non bornée) | Performance | Pagination + plafond + taille de page clampée |
| 8 | Aucun acteur privilégié ne pouvait révoquer (un membre mal rattaché restait bloqué **définitivement**) | Fonctionnel | Autorisé au déclarant, **à l'encadrant**, aux modérateurs ; `revocable` exposé à l'UI |
| 9 | `POST`/`DELETE` renvoyaient l'**entité JPA brute** (ni `typeLabel`, ni noms ; fuite d'`tenantId`/`declaredBy`) | Contrat | `MemberRelationView` |
| 10 | `getUserDetail` **avaleait** l'anomalie sans marqueur ; la garde du modal affichait une carte vide **pour tout le monde** | UX | `hierarchiePartielle` + garde corrigée + état d'erreur |
| 11 | Navigation interne sans retour → l'utilisateur se perdait dans la pile | UX | Fil d'Ariane + bouton Retour + test |
| 12 | « Mes membres » pas cliquable, et l'endpoint `/members` jamais appelé | Fonctionnel | Source paginée + nom cliquable |
| 13 | 12 types de notification sans ligne de dictionnaire → **ni libellé ni couleur de badge** | Produit | `V232` + `defaultSeedData()` |
| 14 | Libellés `RELATION_*` en français en dur, absents des 6 locales | i18n | Clés ajoutées dans les 6 locales |
| 15 | Mobile : « mes membres » lu depuis une source non bornée ; pas de modèle typé (crash possible sur champ absent) | Robustesse | Modèles à parsing défensif + source paginée |
| 16 | Mobile : les encadrants déclarés disparaissaient si la fusion `ascendants` était absente | Fonctionnel | Fusion défensive dédoublonnée |

## 6. CONTRAT D'API (référence pour tout agent expert)

```
GET    /api/v1/relations/types              → [{code,label}]      (types ACTIFS de l'église)
POST   /api/v1/relations/me                 → 201 + MemberRelationView + notif chez « to »
DELETE /api/v1/relations/me/{id}            → 200 + MemberRelationView (REVOKED + notif)
GET    /api/v1/relations/me                 → { sortantes[], entrantes[], typesDisponibles[] }
GET    /api/v1/relations/me/members?page&size → Spring Page<MemberRelationView>  (borné, paginé)
POST   /api/v1/relations/users/{userId}      → 201 — ADMIN/PASTEUR déclare pour un membre
GET    /api/v1/relations/users/{userId}      → { sortantes, entrantes, … }
GET    /api/v1/hierarchy/me                 → HierarchyView
GET    /api/v1/hierarchy/users/{userId}      → HierarchyView
GET    /api/v1/users/{id}/detail            → + "relations", "hierarchie",
                                             + "hierarchiePartielle", "hierarchieIndisponible"

MemberRelationView { id, fromUserId, fromNom, toUserId, toNom, otherUserId, otherNom,
                     relationType, typeLabel, statut, note, createdAt, endedAt,
                     declaredBy, revocable }

HierarchyView { userId, nomComplet, rolePrincipal, roleActif,
                roles[{code,label,source,nodeId,nodeName}],
                branches[{ noeud{id,nom,type,level}, origine, niveaux,
                           chaine[{id,nom,type,level,responsable{id,nom}|null}] }],
                relations, ascendants[{id,nom,via,noeud?|typeLabel?}],
                suivi{ faiseur, chefDeFamille, familleGeree,
                       departementsDiriges[], noeudsDiriges[], amesSuiviesTotal },
                resume{ branchesOrganisationnelles, encadrantsDeclares,
                        membresRattaches, origines[], hierarchieComplete } }
```

`origine` ∈ `ASSIGNATION_V3` | `ADHESION_NOEUD` | `RESPONSABLE_NOEUD`.
`via` ∈ `ORGANISATION` | `DECLARATIF`.

## 7. HORS PÉRIMÈTRE (assumé et documenté)

- **Validation / approbation par le supérieur** : activation immédiate, conformément
  à la demande (« automatiquement … avec notif »). Un flux `PENDING` pourra suivre ;
  les statuts sont extensibles (`enum` côté Java, `String` côté mobile).
- **`modules/mentoring` / `reverseMentoring`** : non modifiés, non dupliqués —
  ce sont des binômes horizontaux, pas de l'encadrement vertical. Voir
  `docs/ADR_001_MEMBER_RELATIONS.md`.
- **`modules/referrals` (parrainage des prospects)** : la cible est un prospect
  externe **sans compte** et `Statut.INSCRIT` ne conserve **aucun** compte lié.
  Le « parrain » d'un nouveau converti est donc inexistant en base ; ce n'est pas
  dans le périmètre de cette feature et le raccordement exigerait une migration
  (`Referral.converted_user_id`).
- **Temps réel push** de la notification : la notification in-app standard
  (page + badge) couvre l'exigence ; le canal additionnel passe par la
  configuration de templates existante.

## 8. JALONS SUIVANTS (si l'on veut aller plus loin)

1. **Duplication de branches organisationnelles.** Si deux personnes
   déclarent le même pasteur, `ascendants` les produit une seule fois ; un
   compteur « X personnes déclarent ce pasteur » sur la fiche de
   l'encadrant serait l'étape logique suivante.
2. **Rattachement groupé** : rattacher plusieurs membres d'un département à un
   responsable en une action (aujourd'hui déclaratif un par un).
3. **Export de l'organigramme** (PDF/PNG) à partir du même agrégat.
4. **Vue « mon organigramme »** pour les rôles `ADMIN`/`PASTEUR` : l'arbre
   organisationnel annoté de l'encadrement déclaré.

## 9. PÉRIMÈTRE DE CE COMMIT

Conformément à la règle « la machine est partagée : ne pas toucher aux
modifications tierces », **seuls les fichiers de cette feature ont été
stagés**. Restent délibérément **hors commit** (modifications sans rapport,
présentes avant ce travail) :

- `backend/…/onboarding/domain/OnboardingStepActions.java`
- `backend/…/tenants/domain/ModuleCatalogService.java`
- `backend/…/tenants/domain/ModuleDefinitionRepository.java`
- `frontend/vite.config.ts`

`OnboardingStepActionsTest` échoue à cause des deux premiers : ce n'est pas
une régression de cette feature, et le corriger ici reviendrait à empiéter
dans le travail d'un autre agent. À traiter par son auteur
(le mock doit stubber `resolveCanonicalCode`).
