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
