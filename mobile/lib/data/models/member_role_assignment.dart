/// Affiliation membre×rôle×nœud (C) — §7.2.
///
/// Découplée de l'appartenance : un même membre peut porter plusieurs rôles
/// sur plusieurs nœuds (multi-assignations V3-C). Miroir de
/// `GET /tenant/members/{id}/assignments` (§5.2).
library;

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

/// Membre d'une « équipe » de nœud (pasteurs/anciens — §7.1 fiche campus).
///
/// Projection enrichie de `GET /tenant/organization/nodes/{id}/team` : une
/// assignation ACTIVE portée sur CE nœud, avec l'intitulé du rôle (B) et le
/// nom du membre (PII — réservé admin tenant, jamais la plateforme, D7).
class NodeTeamMember {
  const NodeTeamMember({
    required this.assignmentId,
    required this.userId,
    required this.roleId,
    required this.status,
    this.roleLabel,
    this.memberName,
  });

  final String assignmentId;
  final String userId;
  final String roleId;
  final String status; // ACTIVE | SUSPENDED | ENDED
  final String? roleLabel;
  final String? memberName;

  bool get isActive => status == 'ACTIVE';

  factory NodeTeamMember.fromJson(Map<String, dynamic> json) => NodeTeamMember(
        assignmentId: json['assignmentId'] as String? ?? '',
        userId: json['userId'] as String? ?? '',
        roleId: json['roleId'] as String? ?? '',
        status: json['status'] as String? ?? 'ACTIVE',
        roleLabel: json['roleLabel'] as String?,
        memberName: json['memberName'] as String?,
      );
}
