/// Modèles « organisation modulable » — SPEC_ORGANISATION_MODULABLE_V3 §7.
///
/// Miroir strict des contrats web (§5) : niveaux (A), intitulés (B),
/// affiliations (C), agrégats (E). Le mobile lit la même surface API ; les
/// compteurs sont en LECTURE SEULE (recalcul serveur) — aucune écriture
/// hors-ligne sur `node_aggregate_snapshots` (décision T-M0).

/// Un niveau de hiérarchie propre à une dénomination (A).
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

/// Intitulé AFFICHÉ d'un rôle-capacité (B). Ne change jamais une permission.
class RoleTitle {
  const RoleTitle({
    required this.roleId,
    required this.label,
    this.nodeId,
    this.labelPlural,
    this.effectiveLabel,
  });

  final String roleId;
  final String label;
  final String? nodeId;
  final String? labelPlural;

  /// Libellé résolu renvoyé par l'API (nœud → tenant → global).
  final String? effectiveLabel;

  factory RoleTitle.fromJson(Map<String, dynamic> json) => RoleTitle(
        roleId: json['roleId'] as String? ?? '',
        label: json['label'] as String? ?? '',
        nodeId: json['nodeId'] as String?,
        labelPlural: json['labelPlural'] as String?,
        effectiveLabel: json['effectiveLabel'] as String?,
      );
}

/// Affiliation membre×rôle×nœud (C), découplée de l'appartenance.
class MemberRoleAssignment {
  const MemberRoleAssignment({
    required this.id,
    required this.userId,
    required this.roleId,
    required this.status,
    this.nodeId,
  });

  final String id;
  final String userId;
  final String roleId;
  final String status; // ACTIVE | SUSPENDED | ENDED
  final String? nodeId;

  bool get isActive => status == 'ACTIVE';

  factory MemberRoleAssignment.fromJson(Map<String, dynamic> json) => MemberRoleAssignment(
        id: json['id'] as String,
        userId: json['userId'] as String,
        roleId: json['roleId'] as String,
        status: json['status'] as String? ?? 'ACTIVE',
        nodeId: json['nodeId'] as String?,
      );
}

/// Snapshot agrégé d'un sous-arbre (E) — LECTURE SEULE, sans PII (D7).
class NodeAggregate {
  const NodeAggregate({
    required this.nodeId,
    required this.memberCount,
    required this.churchCount,
    required this.leaderCount,
    this.levelName,
    this.responsibleName,
    this.sermonCount = 0,
    this.prayerTopicCount = 0,
  });

  final String nodeId;
  final int memberCount;
  final int churchCount;
  final int leaderCount;
  final int sermonCount;
  final int prayerTopicCount;
  final String? levelName;
  final String? responsibleName;

  factory NodeAggregate.fromJson(Map<String, dynamic> json) => NodeAggregate(
        nodeId: json['nodeId'] as String? ?? '',
        memberCount: (json['memberCount'] as num?)?.toInt() ?? 0,
        churchCount: (json['churchCount'] as num?)?.toInt() ?? 0,
        leaderCount: (json['leaderCount'] as num?)?.toInt() ?? 0,
        sermonCount: (json['sermonCount'] as num?)?.toInt() ?? 0,
        prayerTopicCount: (json['prayerTopicCount'] as num?)?.toInt() ?? 0,
        levelName: json['levelName'] as String?,
        responsibleName: json['responsibleName'] as String?,
      );
}
