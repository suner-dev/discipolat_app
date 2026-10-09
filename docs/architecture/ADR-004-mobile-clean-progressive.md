# ADR-004 — Mobile : Flutter feature-first, clean architecture miroir du BE, offline-first progressif

> **Statut** : proposé. **Date** : 2026-10-09. **Contexte mesuré** : `mobile/` = Flutter + Dart,
> `go_router` + `flutter_riverpod` (+ `riverpod_generator`), `dio`, **`drift` déjà dans `pubspec.yaml`**,
> `connectivity`, `flutter_secure_storage`. Couches actuelles : `api/`, `core/`, `data/{local,models,
> services,utils}`, `features/`, `models/`, `presentation/`. **~47 fichiers** Riverpod. Suite :
> **652 tests verts**. Deux faits qui décident de tout :
> - **couche `domain/` absente** (la politique métier vit dans `data/services` et les écrans) ;
> - **`ApiService()` instancié en dur 260 fois** → zéro injection, écrans difficilement testables
>   (exactement ce qu'on a dû contourner en ajoutant un `apiService` optionnel pour T4).

---

## Décision

Le mobile adopte la **même grammaire en couches que le backend** (ADR-002), adaptée à Flutter, et
industrialise l'**offline-first de façon progressive** en s'appuyant sur les briques **déjà présentes**
(`drift`, verrou de sync, module `LowBand`) — **sans réécrire** les écrans qui marchent.

**Cible par feature :**
```
features/<x>/
  domain/          ← entités + règles métier pures + ports (repositories abstraits). 0 Flutter, 0 dio.
  data/            ← DTO + implémentations des ports (ApiService, drift local, file d'écriture)
  presentation/    ← écrans + providers Riverpod ; ne connaît QUE domain/
```
On **réconcilie** `presentation/` (ancien) et `features/` (nouveau) déjà cohabitant : la règle devient
*une fonctionnalité = un dossier `features/<x>/` possédant ses 3 couches*, migrée écran par écran.

## Les 5 engagements

1. **Injection de dépendances** : fini les 260 `ApiService()` en dur. Un `provider` unique fournit
   l'`ApiService` (+ un `HttpClient` partageable). Conséquence directe : chaque écran devient testable
   avec un fake — c'est ce qu'on a dû bricoler en T4, systématisé ici.
2. **`domain/` sans framework** : les règles (rôles, droits, validations métier) sont testables sans
   pomper un widget. Le `data/` dépend du `domain/`, **jamais l'inverse**.
3. **Offline-first progressif sur `drift`** : file d'écriture locale idempotente (la clé d'idempotence
   serveur existe déjà) + relecture au retour réseau, **par feature à forte valeur** d'abord (carnet
   de prière/âmes, présence, offrandes), pas un bas-bloc global.
4. **Contrats partagés** : les DTO mobile sont alignés sur le `openapi.json`/`asyncapi.yaml` du BE via
   des **tests de contrat** (on étend `churchesSuggestExistsContract`, déjà vert 12/12).
5. **Parité de navigation** : la règle `go` vs `push` du plan église (D6) devient une **convention
   testée** (une flèche de retour attendue par écran enfant) plutôt qu'une correction au cas par cas.

## Progressif ≠ timide : l'ordre réaliste

| Étape | Charge | Bénéfice |
|---|---|---|
| **M0 — DI** : remplacer `ApiService()` en dur par un provider, écran par écran | faible | tous les écrans testables ; débloque le reste |
| **M1 — domain/** par feature migrée | moyen | règles métier testées, découplage data/écran |
| **M2 — offline drift** sur 1-2 features critiques | moyen | rétention marché connexion faible (**valeur business directe**) |
| **M3 — SDK extrait** (`core_auth`, `core_sync` en packages) | élevé | ouvre l'écosystème / ouvreurs tiers (effet réseau) |

## Options rejetées (avec raison)

- **Réécrire tout `presentation/` → `features/` d'un coup** : des semaines sans valeur visible,
  régressions sur 652 tests ; le strangler (écran par écran) est le seul chemin compatible avec
  « ne rien casser ».
- **Bloquer l'offline sur un nouveau moteur** : `drift` est **déjà** dans `pubspec` + `data/local`
  existe → on capitalise, on n'ajoute pas une dépendance de plus.
- **State management additionnel (Bloc en plus de Riverpod)** : Riverpod + generator est déjà la
  norme ; ajouter un second système crée deux sources de vérité. (Bloc uniquement là où un flux
  d'événements explicite est objectivement plus clair — cas rares.)

## Critères de vérification

- `grep -rn "ApiService()" lib` → **0** (hors composition-root DI).
- Un test d'architecture Dart interdit qu'un `domain/` importe `package:dio` ou `package:flutter`.
- Au moins **une** feature offline-first prouvée par un test « écriture hors-ligne → rejeu à la
  reconnexion → état serveur cohérent ».
- Suite mobile **strictement additive** au-dessus du plancher 652, `flutter analyze` à 0 warning.

*Reproductible : `grep -rn "ApiService()" mobile/lib | wc -l` (260) ; `ls mobile/lib/domain 2>/dev/null`
(absent) ; `grep -E "drift|connectivity" mobile/pubspec.yaml` (déjà déclarés).*
