library;

/// Groupe d'onglets du menu mobile — parité LOT 2 §GR avec le web.
///
/// Un groupe est un simple [label] affiché : l'utilisateur n'a jamais à le saisir
/// en dur, c'est l'église qui le définit depuis l'écran
/// « Personnalisation de l'interface ».
class NavigationGroup {
  const NavigationGroup({
    required this.id,
    required this.key,
    required this.label,
    this.description,
    this.icon,
    this.parentGroupId,
    this.displayOrder = 0,
    this.collapsedByDefault = true,
    this.showCount = false,
    this.tenantId,
    this.hrefCount = 0,
  });

  final String id;

  /// `null` = groupe global livré par défaut ; sinon groupe d'une église.
  final String? tenantId;
  final String key;
  final String label;
  final String? description;
  final String? icon;
  final String? parentGroupId;
  final int displayOrder;
  final bool collapsedByDefault;
  final bool showCount;

  /// Nombre d'entrées affectées au groupe (lu dans `assignments`).
  final int hrefCount;

  bool get isGlobal => tenantId == null || tenantId!.isEmpty;

  factory NavigationGroup.fromJson(Map<String, dynamic> json) =>
      NavigationGroup(
        id: json['id'] as String? ?? '',
        tenantId: json['tenantId'] as String?,
        key: json['key'] as String? ?? '',
        label: json['label'] as String? ?? '',
        description: json['description'] as String?,
        icon: json['icon'] as String?,
        parentGroupId: json['parentGroupId'] as String?,
        displayOrder: (json['displayOrder'] as num?)?.toInt() ?? 0,
        collapsedByDefault: json['collapsedByDefault'] as bool? ?? true,
        showCount: json['showCount'] as bool? ?? false,
      );
}

/// Forme résolue du menu : groupes + affectations `href -> [groupId]`.
class NavigationShape {
  const NavigationShape({required this.groups, required this.assignments});

  /// Vide tant que l'API n'a pas répondu — le drawer garde alors son
  /// fonctionnement d'avant, sans jamais être vide.
  static const empty =
      NavigationShape(groups: <NavigationGroup>[], assignments: <String, List<String>>{});

  final List<NavigationGroup> groups;
  final Map<String, List<String>> assignments;

  bool get isEmpty => groups.isEmpty;

  /// Premier groupe contenant cette route, sinon `null`.
  NavigationGroup? groupOf(String route) {
    final ids = assignments[route];
    if (ids == null || ids.isEmpty) return null;
    for (final id in ids) {
      for (final group in groups) {
        if (group.id == id) return group;
      }
    }
    return null;
  }
}