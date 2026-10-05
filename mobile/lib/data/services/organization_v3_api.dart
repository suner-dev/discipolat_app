/// Service API « organisation modulable » — SPEC_ORGANISATION_MODULABLE_V3 §7.
///
/// Enveloppe les endpoints web (§5) pour le mobile. Décision **T-M0** : les
/// agrégats (`node_aggregate_snapshots`) et les données de config sont lus en
/// LECTURE SEULE via ce service ; AUCUNE écriture hors-ligne n'est mise en
/// file dans l'un ou l'autre moteur de sync (`sync_service` / `offline_sync_manager`).
/// Les mutations (niveaux, intitulés, modules, thème, assignations) passent
/// par l'API en ligne, le mobile privilégiant les écrans terrain, pas l'admin
/// lourd. Cela évite les conflits du double moteur (dette connue).
///
/// Le modèle V3 n'est donc JAMAIS écrit dans un seul moteur : source unique =
/// le serveur.
import 'dart:convert';
import 'package:dio/dio.dart';
import 'api_service.dart';
import '../models/organization_v3_models.dart';

class OrganizationV3Api {
  OrganizationV3Api(this._api);
  final ApiService _api;

  /// `ApiService` renvoie la `Response` dio : `res.data` est soit la liste /
  /// l'objet déja sérialisé par JSON, soit une chaine brute selon les
  /// intercepteurs. On normalise ici, jamais dans les écrans.
  Object? _payload(Response res) {
    final raw = res.data;
    if (raw is String) return raw.isEmpty ? null : jsonDecode(raw);
    return raw;
  }

  List<Map<String, dynamic>> _list(Response res) {
    final data = _payload(res);
    if (data is List) {
      return data.whereType<Map<String, dynamic>>().toList();
    }
    return const [];
  }

  /// GET /tenant/organization/levels (A).
  Future<List<OrganizationLevel>> levels() async {
    final res = await _api.get('/tenant/organization/levels');
    return _list(res).map(OrganizationLevel.fromJson).toList();
  }

  /// GET /tenant/organization/tree (E) — nœuds avec levelName/responsibleName.
  Future<List<Map<String, dynamic>>> tree() async {
    final res = await _api.get('/tenant/organization/tree');
    return _list(res);
  }

  /// GET /tenant/organization/nodes/{id}/aggregate (E) — lecture seule, sans PII.
  Future<NodeAggregate> nodeAggregate(String nodeId) async {
    final res = await _api.get('/tenant/organization/nodes/$nodeId/aggregate');
    final data = _payload(res);
    if (data is Map<String, dynamic>) return NodeAggregate.fromJson(data);
    return NodeAggregate(
      nodeId: nodeId,
      memberCount: 0,
      churchCount: 0,
      leaderCount: 0,
    );
  }

  /// GET /tenant/organization/nodes/{id}/children (E) — drill-down.
  Future<List<Map<String, dynamic>>> nodeChildren(String nodeId) async {
    final res = await _api.get('/tenant/organization/nodes/$nodeId/children');
    return _list(res);
  }

  /// GET /tenant/roles/{id}/titles (B).
  Future<List<RoleTitle>> roleTitles(String roleId) async {
    final res = await _api.get('/tenant/roles/$roleId/titles');
    return _list(res).map(RoleTitle.fromJson).toList();
  }

  /// GET /tenant/members/{id}/assignments (C).
  Future<List<MemberRoleAssignment>> memberAssignments(String userId) async {
    final res = await _api.get('/tenant/members/$userId/assignments');
    return _list(res).map(MemberRoleAssignment.fromJson).toList();
  }
}
