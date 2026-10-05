import 'dart:convert';

import 'package:dio/dio.dart';

import '../models/navigation_group.dart';
import 'api_service.dart';

/// Client des groupes de navigation — LOT 2 §GR.
///
/// Reprend exactement le contrat de la web (`GET /tenant/navigation/groups`) :
/// même résolution côté serveur (globaux + église, l'église écrasant), donc le
/// mobile et le web ne peuvent pas afficher deux menus différents pour la même
/// personne.
class NavigationApi {
  NavigationApi(this._api);
  final ApiService _api;

  /// `res.data` peut être une chaîne selon la config Dio ; on décode ici, jamais
  /// dans les écrans (même règle que `OrganizationV3Api._payload`).
  Object? _payload(Response res) {
    final raw = res.data;
    if (raw is String) return raw.isEmpty ? null : jsonDecode(raw);
    return raw;
  }

  /// Groupes + affectations pour l'utilisateur courant.
  ///
  /// Ne lève jamais : un échec réseau renvoie [NavigationShape.empty], ce qui
  /// laisse le drawer retomber sur son affichage d'origine plutôt que de
  /// laisser l'utilisateur sans menu.
  Future<NavigationShape> shape() async {
    try {
      final res = await _api.get('/tenant/navigation/groups');
      final data = _payload(res);
      if (data is! Map<String, dynamic>) return NavigationShape.empty;

      final rawGroups = data['groups'];
      final groups = <NavigationGroup>[];
      if (rawGroups is List) {
        for (final item in rawGroups.whereType<Map<String, dynamic>>()) {
          groups.add(NavigationGroup.fromJson(item));
        }
      }

      final assignments = <String, List<String>>{};
      final rawAssignments = data['assignments'];
      if (rawAssignments is Map<String, dynamic>) {
        rawAssignments.forEach((href, value) {
          if (value is List) {
            assignments[href] = value.map((e) => e.toString()).toList();
          }
        });
      }

      // Nombre d'entrées par groupe, affiché si `showCount` est actif.
      final counts = <String, int>{};
      assignments.forEach((_, ids) {
        for (final id in ids) {
          counts[id] = (counts[id] ?? 0) + 1;
        }
      });
      return NavigationShape(
        groups: groups
            .map((group) => NavigationGroup(
                  id: group.id,
                  tenantId: group.tenantId,
                  key: group.key,
                  label: group.label,
                  description: group.description,
                  icon: group.icon,
                  parentGroupId: group.parentGroupId,
                  displayOrder: group.displayOrder,
                  collapsedByDefault: group.collapsedByDefault,
                  showCount: group.showCount,
                  hrefCount: counts[group.id] ?? 0,
                ))
            .toList(),
        assignments: assignments,
      );
    } on DioException {
      return NavigationShape.empty;
    } catch (_) {
      return NavigationShape.empty;
    }
  }
}