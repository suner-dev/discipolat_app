# MIGRATION DU CONTRAT D'IDENTIFIANTS — mobile contre backend

> Constat mesuré le 2026-09-29 par l'Agent B. **Bloquant pour 6 des 10 TODO joignables** :
> les implémenter aujourd'hui produirait des fonctionnalités qui ne peuvent pas
> fonctionner. Ce document explique pourquoi, et l'ordre de travail réel.

## 1. La mesure

| Côté | Type des identifiants |
|---|---|
| **Backend** | **250 entités avec `UUID id`**, 10 avec `Long id` |
| **Mobile** | **7 modèles déclarent encore `required int id`**, 1 en `String` |

Les 7 modules concernés : `events`, `streaming`, `health`, `finances`, `discipleship`,
`tasks`, `messages`. Tous sont **importés dans `app.dart`**, donc atteignables.

## 2. Ce que ça produit concrètement

Un `PUT /events/42` part vers un contrôleur qui attend un `UUID` : le backend répond
**400 Bad Request**, pas 404, et le message ne parle pas d'identifiant. Un
`GET /streams/7` est un 400 de même nature. L'utilisateur voit un écran vide ou une
erreur générique, sans cause.

`EventService.getEvent(int id)` → `_api.get('/events/$id')` : c'est du code
**réel** (pas un mock) qui **ne peut pas aboutir**.

## 3. Le cas particulier du streaming — un défaut de modèle de données

`LiveStream` est l'une des 10 entités restées en `Long` :

```java
// backend/.../streaming/domain/LiveStream.java
private Long id;            private Long tenantId;      private Long createdBy;
```

et la table (V133, époque pré-multi-tenant) :

```sql
id        BIGSERIAL PRIMARY KEY,
tenant_id BIGINT NOT NULL,     -- alors que tenants.id est UUID (V70)
```

→ `live_streams.tenant_id` **ne peut pas référencer** `tenants.id`. C'est une
instance du même thème que H1/H2 (modèle écrit contre un schéma que les migrations
ne produisent pas) et ça justifie de le traiter comme **arbitrage backend**, pas
comme un simple refactor de modèle.

## 4. Le plan de migration (par module, ~1 h de travail effectif chacun)

Pour chaque module, dans cet ordre :

1. `lib/features/<m>/models/<m>_model.dart` : `required int id` → `required String id`.
2. Régénérer les fichiers générés (obligatoire, ils sont versionnés) :
   ```bash
   cd mobile && dart run build_runner build --delete-conflicting-outputs
   ```
3. `lib/features/<m>/services/<m>_service.dart` : signatures `int id` → `String id`,
   et supprimer tout `int.parse(...)` sur un identifiant.
4. Les écrans :_plus aucune comparaison/arithmétique sur l'id.
5. Les tests : fixtures `id: 1` → `id: '11111111-1111-1111-1111-111111111111'`.
6. Retirer le module de la liste d'exceptions du garde-fou
   (`mobile/test/contract/identifier_contract_test.dart`) — le test échouera tant
   que ce n'est pas fait.

**Ordre recommandé** (du plus rentable au plus risqué) :
`messages` (senderId/receiverId — corrige aussi le préfixe « Vous: ») →
`events` (débloque l'annulation, TODO réel) → `finances` → `discipleship` →
`health` → `tasks` (à arbitrer : module orphelin) → `streaming` (**après** arbitrage
backend sur le `tenant_id`).

## 5. Le garde-fou, vérifié

`mobile/test/contract/identifier_contract_test.dart` est un **crémaillon** : il
maintient la liste d'exceptions et échoue si la dette **augmente** ou si une
nouvelle occurrence apparaît hors liste.

Il a été **épreuve** : en ajoutant `required int id` dans
`lib/features/assets/models/asset_model.dart`, le test échoue avec le nom du
fichier ; le fichier restauré, il repasse. Un garde-fou non éprouvé n'est pas un
garde-fou.

## 6. Conséquence sur les 10 TODO

| TODO | Bloqué par quoi |
|---|---|
| Annuler un événement | **Non bloqué** : `UpdateEventRequest` contient déjà `statut` et `EventService.update` est un patch partiel. Il reste à migrer l'id du module `events` |
| Partager (stream, event) | Choix produit + capacité OS (|MethodChannel) : hors périmètre d'un TODO |
| Streaming update / delete | **Endpoint absent** (`LiveStreamController` n'expose que `list`, `live`, `create`, `go-live`, `end`, `viewer`) **et** id `Long` à arbitrer |
| Upload d'image de stream | Aucun endpoint d'upload générique ; `BrandingController` gère le logo uniquement |
| Health kits / duties | **Domaine inexistant** : aucune entité, aucun contrôleur. 6 livrables minimum (entité, migration, repository, service, contrôleur + RBAC, écran mobile) |
| Tasks (create) | Module orphelin : aucun `/api/v1/tasks` (voir `HANDOVER-VERS-AGENT-A.md`) |

**Implémenter les 10 maintenant reviendrait à livrer 10 écrans qui appellent des
routes inexistantes ou des identifiants invalides** — autrement dit, des maquettes
qui ressemblent à des fonctionnalités. C'est précisément ce qu'il ne faut pas.

---

# SPÉCIFICATION EXÉCUTABLE — réécriture du modèle `Event` (module events)

> Établie le 2026-09-29 à partir de la source de vérité : `EventResponse.java` (backend) et
> `Evenement` (web, le client qui fonctionne). Rien n'est inventé : chaque ligne ci-dessous
> correspond à un champ réel du backend.

## 1. Pourquoi ce n'est pas un TODO

Le modèle mobile actuel déclare **31 champs** ; le backend en fournit **16**, et **aucun ne porte
le même nom** :

| Le mobile attend | Le backend renvoie |
|---|---|
| `title` | `titre` |
| `startAt` / `endAt` | `dateDebut` / `dateFin` |
| `location` | `lieu` |
| `type` (enum `EventType`) | `typeEvenement` (String libre) |
| `maxAttendees` / `currentAttendees` | `limitePlaces` / `nbInscrits` |
| `status` | `statut` |
| `organizerId` | `organisateurId` |
| `attachments` | `piecesJointes` |
| `latitude`, `longitude`, `hasGeofencing`, `hasFaceCheckIn`, `checkInQrCode`, `streamUrl`, `thumbnailUrl`, `tags`, `isPublic`, `requiresRegistration`, `hasCheckIn`, `spaceId`, `spaceName`, `dressCode*` | **rien** |

`Event.fromJson` échoue donc sur le premier `null as String` : **le module events ne peut pas
lire un seul événement**.

Volume mesuré : **227 références** aux champs à supprimer (`.title`, `.location`, `.startAt`,
`.isPublic`, `.dressCodeId`, `.streamUrl`…) dans `lib/features` et `lib/presentation`, dont une
partie appartient à d'autres modèles. Avec 4 écrans, 4 widgets, le service et le modèle, c'est un
chantier de **plusieurs jours** qui doit être fait d'un bloc : à moitié migré, l'application est
cassée.

## 2. Le nouveau modèle, champ par champ

```dart
@freezed
class Event with _$Event {
  const factory Event({
    required String id,                    // UUID
    String? organisateurId,               // UUID
    String? familleId,                     // UUID
    String? departmentId,                  // UUID
    String? typeEvenement,                 // String libre côté backend
    required String titre,
    String? description,
    String? lieu,
    required DateTime dateDebut,
    DateTime? dateFin,
    int? limitePlaces,
    @Default(0) int nbInscrits,
    required EventStatus statut,           // enum aligné PLANIFIE/EN_COURS/TERMINE/ANNULE
    String? compteRendu,
    DateTime? createdAt,                  // le backend n'expose PAS updatedAt
    @Default(<EventPieceJointe>[]) List<EventPieceJointe> piecesJointes,
    // --- état DÉRIVÉ côté client, jamais lu dans la réponse ---
    @Default(false) bool isOrganizer,      // calculé : organisateurId == utilisateur courant
    @Default(false) bool isRegistered,     // calculé : /events/{id}/registrations contient l'utilisateur
  }) = _Event;
  factory Event.fromJson(Map<String, dynamic> json) => _$EventFromJson(json);
}

@freezed
class EventPieceJointe with _$EventPieceJointe {
  const factory EventPieceJointe({
    required String id,        // EntityAttachmentService.AttachmentItem.id
    required String fileId,
    required String nom,
    required String url,
  }) = _EventPieceJointe;
  factory EventPieceJointe.fromJson(Map<String, dynamic> json) => _$EventPieceJointeFromJson(json);
}
```

**`typeEvenement`** : le backend le stocke en texte libre. Le web type
`'SORTIE' | 'RETRAITE' | 'EVANGELISATION' | 'REUNION' | 'VISITE' | 'CONFERENCE' | 'FORMATION' |
'ANNIVERSAIRE' | 'CULTE' | 'ETUDE_BIBLIQUE' | 'VEILLEE' | 'PRIERE' | 'AUTRE'`. Deux options :
- conserver un enum + `@JsonValue` sur ces 13 valeurs **et** un décodeur tolérant (comme
  `EventStatusWire`) — recommandé, aligné sur le web ;
- `String` brut — plus simple, mais l'UI perd les libellés typés.

**Point d'attention `createdAt`** : le web déclare `dateCreation` **et** `updatedAt`, que le backend
n'envoie pas. Ce n'est pas une erreur du web (ses champs sont simplement `undefined` à
l'exécution), mais c'est un rappel : le contrat réel n'a qu'un `createdAt`.

## 3. Les 7 endpoints du service qui n'existent pas

| Chemin appelé par le service | Backend | Décision |
|---|---|---|
| `/events/{id}/check-in`, `/check-in/qr` | `POST /events/{id}/attendance` existe, check-in non | **Realigner** sur `/attendance` |
| `/events/{id}/my-registration` | `GET /events/{id}/registrations` + `PUT /rsvp` | **Realigner** |
| `/events/upcoming` | `GET /events/upcoming/mine` | **Realigner** |
| `/events/{id}/stats` | `GET /events/statistics` et `/consolidated` | **Realigner** |
| `/events/{id}/team`, `/team/{id}` | **aucun** | **Supprimer** (modèles `EventTeamMember` inclus) |
| `/events/{id}/checklist`, `/checklist/{id}` | **aucun** | **Supprimer** (modèles `EventChecklist` inclus) |
| `/events/{id}/dress-code` | **aucun** | **Supprimer** (modèles `DressCode`, `DressCodeItem` inclus) |

Supprimer ces trois sous-fonctionnalités est un **arbitrage produit** : on peut les reconstruire
chez l'Agent A, ou les retirer de l'application. Les **garder** en l'état revient à faire appel à
des routes inexistantes — c'est-à-dire à annoncer une fonctionnalité qui n'existe pas.

## 4. Ordre d'exécution (une seule PR, tout ou rien)

1. Nouveau `Event` + `EventPieceJointe` ; suppression des 4 modèles sans backend.
2. `dart run build_runner build --delete-conflicting-outputs`
   ⚠️ **ne committer que les fichiers générés du module `events`** : le générateur réécrit aussi
   12 fichiers d'autres modules (constaté le 2026-09-29), ce qui mélange 5 modules dans une PR.
3. Service : réaligner les 4 chemins, supprimer les 7 méthodes orphelines, ajouter
   `listAttachments` si l'UI en a besoin.
4. Écrans : `event_detail_screen`, `event_create_screen`, `events_screen`,
   `widgets/event_card`, `widgets/event_filter_chips`, `widgets/event_chat_overlay`.
5. Tests : `test/event_contract_and_cancel_test.dart` — le groupe
   « DIVERGENCE DE SCHEMA CONNUE » **doit échouer** à ce stade : c'est lui qui dit que la migration
   est faite. Le remplacer par un test de parsing réel (`Event.fromJson(backendEventJson())` doit
   produire un `Event` correct).
6. `flutter analyze` : 0 erreur **et** aucun `unused_field` nouveau ; suite complète verte.
7. Le crèmeillon `test/contract/identifier_contract_test.dart` passe déjà pour `events` (il porte
   sur les `int`, pas sur les noms de champs) — inchangeable ici.
