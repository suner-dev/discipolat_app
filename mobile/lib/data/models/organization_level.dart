/// Niveau de hiérarchie propre à une dénomination (A) — §7.2.
///
/// Miroir strict de `GET /tenant/organization/levels` (§5.1). Le type
/// sémantique (`semanticType`) porte la LOGIQUE ; `name` n'est que l'AFFICHAGE
/// (garde-fou §11.4 : capacité ≠ intitulé).
library;

class OrganizationLevel {
  const OrganizationLevel({
    required this.id,
    required this.rootTenantId,
    required this.name,
    required this.depthOrder,
    required this.semanticType,
    this.pluralName,
    this.parentLevelId,
    this.icon,
    this.color,
    this.branching = true,
    this.active = true,
  });

  final String id;
  final String rootTenantId;
  final String name;
  final int depthOrder;
  final String semanticType;
  final String? pluralName;
  final String? parentLevelId;
  final String? icon;
  final String? color;
  final bool branching;
  final bool active;

  factory OrganizationLevel.fromJson(Map<String, dynamic> json) => OrganizationLevel(
        id: json['id'] as String,
        rootTenantId: json['rootTenantId'] as String,
        name: json['name'] as String,
        depthOrder: (json['depthOrder'] as num?)?.toInt() ?? 0,
        semanticType: json['semanticType'] as String? ?? 'CUSTOM',
        pluralName: json['pluralName'] as String?,
        parentLevelId: json['parentLevelId'] as String?,
        icon: json['icon'] as String?,
        color: json['color'] as String?,
        branching: json['branching'] as bool? ?? true,
        active: json['active'] as bool? ?? true,
      );
}
