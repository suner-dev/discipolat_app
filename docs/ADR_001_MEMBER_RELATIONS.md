# ADR-001 — Relations d'encadrement déclaratives (`member_relations`) et non réutilisation de `modules/mentoring`

- **Statut** : accepté
- **Date** : 2026-10-06
- **Migration** : `V231__member_relations.sql`
- **Décideurs** : architecture backend
- **Lie à** : `docs/PLAN_HIERARCHIE_RELATIONS_MEMBRES.md`, `docs/ORGANIZATION_HIERARCHY.md`, `docs/RBAC.md` §4.11

## Contexte

Le besoin exprimé est le suivant : **tout membre doit pouvoir connaître sa
hiérarchie complète** (ses chefs, ses responsables, son pasteur, en arbre et
en plusieurs branches) et **déclarer lui-même, dans son profil, son pasteur ou
ses supérieurs**, selon le paramétrage de son église, avec apparition
automatique et notification chez le supérieur.

Le socle contient déjà plusieurs briques qui semblent couvrir le sujet :

| Brique existante | Ce qu'elle modélise | Pourquoi elle ne suffit pas |
|---|---|---|
| `modules/mentoring` | binômes de suivi entre deux personnes, avec axes et objectifs | Relation **horizontale et temporaire** (un binôme de mentoring). Aucune notion de *pasteur*, de chaîne d'encadrement, ni de subordination. |
| `modules/reverseMentoring` | mentorat inverse (un junior forme un senior) | Sens opposé, même nature horizontale. |
| `modules/network` | relations sociales génériques | Nonordinale, non paramétrable par église, sans notification ni historique. |
| `modules/succession` | plans de succession / relève | Anticipation d'un poste vacant, pas la ligne d'encadrement courante. |
| `OrganizationNode.responsibleId` | le responsable d'un nœud organisationnel | Structurant et **autoritatif**, mais ne dit rien du lien *personne à personne* hors nœud, et ne se laisse pas déclaré par le membre lui-même. |

Ajouter une cinquième notion « mentor/parrain » sans décision documentée
aurait créé **cinq notions concurrentes** du même concept.

## Décision

Créer l'entité **`MemberRelation`** : une arête dirigée
`from_user_id → to_user_id`, typée par le dictionnaire
`MEMBER_RELATION_TYPE`, **déclarée par le membre lui-même**, avec
activation immédiate, notification automatique chez le destinataire et
traçabilité (`REVOKED`, jamais de purge).

L'agrégat `UserHierarchyService` **agrège** les briques existantes sans les
remplacer :

1. **branches organisationnelles** — un nœud peut être atteint par trois
   origines, par ordre de priorité et chacune tracée :
   | Origine | Source | Cas |
   |---|---|---|
   | `ASSIGNATION_V3` | `member_role_assignments.node_id` | capacité posée explicitement |
   | `ADHESION_NOEUD` | `tenant_membership.scope_id` | adhésion à portée de nœud |
   | `RESPONSABLE_NOEUD` | `organization_nodes.responsible_id` | le membre **dirige** un nœud (signal autoritatif du pasteur) |
2. **chaîne d'ascendance** — chaque branche est développée jusqu'à la racine
   avec le responsable de **chaque** niveau ;
3. **relations déclarées** — encadrants et membres rattachés ;
4. **encadrement pastoral** (`suivi`) — `souls.faiseur_id`,
   `users.famille_geree_id`, `departments.responsable_id` ;
5. **`resume`** — ce que l'agrégat sait et d'où il le sait, pour que
   l'interface n'affiche jamais un vide silencieux.

`MemberRelation` est donc **une pièce parmi d'autres** de l'agrégat, pas un
doublon de `MentoringAssignment` : elle porte le lien **vertical et
déclaratif** que les autres ne modélisent pas.

## Origines des branches : pourquoi `RESPONSABLE_NOEUD` a été ajouté

Sans ce troisième signal, `branches` était vide pour la grande majorité des
membres : presque toutes les adhésions sont écrites en
`scope_type = TENANT` (`RoleManagementService` est le seul endroit qui force
`CHURCH`), et les assignations V3 sont rares. Or le cas d'usage principal —
« je suis pasteur de ce campus » — est **déjà** dans la base via
`organization_nodes.responsible_id`, simplement inexploité. L'ajouter rend la
fonctionnalité réellement opérationnelle sans rien inventer.

## Conséquences

**Positives**

- Une seule notion d'encadrement vertical, paramétrable par église,
  auto-déclarée et notifiée — exactement ce que le besoin décrit.
- Le paramétrage d'église est réel : un type `MEMBER_RELATION_TYPE`
  désactivé est **rejeté** à la déclaration, pas seulement masqué.
- Les briques existantes sont réutilisées et non dupliquées ; chaque branche
  est **traçable** jusqu'à sa source.
- Aucun vocabulaire nouveau pour l'utilisateur : ce qu'il voit vient des
  nœuds, rôles et personnes qu'il connaît déjà.

**Négatives / risques assumés**

- **Cinquième notion à comprendre** pour un mainteneur. Accepté : c'est le
  prix d'une fonctionnalité demandée, et cet ADR le documente.
- **Pas de workflow de validation.** L'activation est immédiate, conformément
  à la demande (« automatiquement … avec notif »). Les statuts sont
  extensibles : un flux `PENDING` pourra être ajouté sans migration
  cassable, en ProtectedBranches le cas échéant.
- **Données sensibles nominatives.** Le rattachement est un lien pastoral
  personnel : il est scopé par tenant, tracé en audit
  (`RELATION_DECLARED` / `RELATION_REVOQUED`), et il n'est lisible par un
  tiers que selon les gardes de `docs/RBAC.md` §4.11.

**Neutre**

- `modules/mentoring` et `modules/reverseMentoring` ne sont **pas** modifiés :
  ils restent le bon outil pour un binôme de formation entre pairs.

## Alternatives écartées

| Alternative | Pourquoi écartée |
|---|---|
| Réutiliser `MentoringAssignment` | Aucune notion de subordination ni de pasteur ; le modèle (axes, objectifs, durée) n'a pas d'équivalent. Introduire un champ « type de relation » dans `mentoring` aurait contaminé un module au périmètre différent et forcé à migrer ses données. |
| Ajouter une FK `mentor_id` sur `users` | Ne permet ni le multi-branches (un membre peut avoir un pasteur *et* un mentor *et* un supérieur), ni les types paramétrables, ni l'historique, ni la notification. |
| Ne traiter l'encadrement que par `organization_nodes.responsibleId` | Le membre ne peut alors pas déclarer son pasteur lui-même, et rien ne couvre le membre sans nœud. Le besoin explicite n'est pas satisfait. |
| Un quatrième `node_type` pour chaque pasteur | Un pasteur est une **personne**, pas un nœud ; on dupliquerait les pasteurs dans l'arbre et on perdrait le lien inter-membres. |

## Renvois

- Contrat d'API : §6 de `docs/PLAN_HIERARCHIE_RELATIONS_MEMBRES.md`.
- Gardes et isolation : `docs/RBAC.md` §4.11.
- Index et contraintes : `docs/DATABASE.md` §2 et §4.
