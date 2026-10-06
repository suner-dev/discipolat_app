/// Service API « Mon encadrement » — V231 relations déclaratives entre membres
/// (pasteur, supérieur, mentor, parrain…) + hiérarchie agrégée d'une personne.
///
/// Calqué sur `organization_v3_api.dart` : enveloppe `ApiService` (dio),
/// normalise le payload ici — jamais dans les écrans. Lecture/écriture en
/// ligne uniquement (données spirituelles nominatives, pas de hors-ligne).
///
/// Volumétrie : « ses membres » passe par la version PAGINÉE de l'API
/// (`GET /relations/me/members?page=&size=`). La variante non paginée
/// `/relations/me` sert à la seule liste des encadrants déclarés, qui est
/// bornée par le plafond serveur (10).
library;

import 'dart:convert';

import 'package:dio/dio.dart';

import '../models/member_relation.dart';
import 'api_service.dart';

class RelationService {
  RelationService(this._api);
  final ApiService _api;

  Object? _payload(Response res) {
    final raw = res.data;
    if (raw is String) return raw.isEmpty ? null : jsonDecode(raw);
    return raw;
  }

  List<Map<String, dynamic>> _list(Response res) {
    final data = _payload(res);
    if (data is List) return data.whereType<Map<String, dynamic>>().toList();
    return const [];
  }

  Map<String, dynamic> _map(Response res) {
    final data = _payload(res);
    return data is Map<String, dynamic> ? data : const {};
  }

  /// GET /relations/types — types ACTIFS selon le paramétrage de l'église
  /// (dictionnaire MEMBER_RELATION_TYPE). Un type désactivé par l'église en
  /// est absent : le serveur le refuse aussi à la déclaration.
  Future<List<RelationType>> types() async {
    final res = await _api.get('/relations/types');
    return _list(res)
        .map(RelationType.fromJson)
        .where((t) => t.code.isNotEmpty)
        .toList();
  }

  /// GET /relations/me — {sortantes, entrantes} (mes encadrants déclarés +
  /// mes membres rattachés).
  Future<RelationsSummary> myRelations() async {
    final res = await _api.get('/relations/me');
    return RelationsSummary.fromJson(_map(res));
  }

  /// POST /relations/me — déclarer une autorité enregistrée ; le destinataire
  /// reçoit automatiquement une notification (RELATION_DECLAREE).
  Future<MemberRelation> declare({
    required String toUserId,
    required String relationType,
    String? note,
  }) async {
    final res = await _api.post('/relations/me', data: {
      'toUserId': toUserId,
      'relationType': relationType,
      if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
    });
    return MemberRelation.fromJson(_map(res));
  }

  /// DELETE /relations/me/{id} — révocation souple (REVOKED, jamais de purge).
  /// Autorisé au déclarant, à l'encadrant concerné et aux modérateurs.
  Future<MemberRelation> revoke(String relationId) async {
    final res = await _api.delete('/relations/me/$relationId');
    return MemberRelation.fromJson(_map(res));
  }

  /// GET /relations/me/members — « ses membres », PAGINÉ et borné.
  Future<MemberRelationPage> membersPage({
    int page = 0,
    int size = 25,
  }) async {
    final res = await _api.get('/relations/me/members', params: {
      'page': page,
      'size': size,
    });
    return MemberRelationPage.fromJson(_map(res));
  }

  /// GET /hierarchy/me — arbre multi-branches, chaîne des responsables,
  /// ascendants unifiés, encadrement pastoral.
  Future<Map<String, dynamic>> myHierarchy() async {
    final res = await _api.get('/hierarchy/me');
    return _map(res);
  }

  /// GET /hierarchy/users/{id} — hiérarchie d'UN membre (déclarant/pasteur/
  /// responsable). Consommé par la fiche utilisateur pour isoler une panne de
  /// la brique hiérarchie du reste de la fiche.
  Future<Map<String, dynamic>> hierarchyOf(String userId) async {
    final res = await _api.get('/hierarchy/users/$userId');
    return _map(res);
  }

  /// GET /relations/users/{id} — {sortantes, entrantes} d'un membre.
  /// Utilisé par la fiche pour lister ses encadrants et ses rattachés sans
  /// recharger toute la fiche détail.
  Future<RelationsSummary> relationsOf(String userId) async {
    final res = await _api.get('/relations/users/$userId');
    return RelationsSummary.fromJson(_map(res));
  }

  /// POST /relations/users/{id} — déclarer, pour le membre consulté, un
  /// encadrant (réservé ADMIN/PASTEUR). Notification automatique.
  Future<MemberRelation> declareFor(
    String userId, {
    required String toUserId,
    required String relationType,
    String? note,
  }) async {
    final res = await _api.post('/relations/users/$userId', data: {
      'toUserId': toUserId,
      'relationType': relationType,
      if (note != null && note.trim().isNotEmpty) 'note': note.trim(),
    });
    return MemberRelation.fromJson(_map(res));
  }

  /// GET /users/search?q= — recherche de membres enregistrés (déclaration).
  Future<List<Map<String, dynamic>>> searchMembers(String query) async {
    if (query.trim().length < 2) return const [];
    final res = await _api.get('/users/search', params: {'q': query.trim()});
    return _list(res);
  }

  /// Raccourcis de parsing pour les écrans : transforme l'agrégat brut en
  /// branches typées. Une réponse dégradée (brique en panne côté serveur)
  /// donne une liste vide, jamais une exception.
  static List<HierarchyBranch> branchesOf(Map<String, dynamic> hierarchy) {
    final raw = hierarchy['branches'];
    if (raw is! List) return const [];
    return raw
        .whereType<Map<String, dynamic>>()
        .map(HierarchyBranch.fromJson)
        .toList();
  }

  static List<Ascendant> ascendantsOf(Map<String, dynamic> hierarchy) {
    final raw = hierarchy['ascendants'];
    if (raw is! List) return const [];
    return raw
        .whereType<Map<String, dynamic>>()
        .map(Ascendant.fromJson)
        .where((a) => a.id.isNotEmpty)
        .toList();
  }
}
