/// Intitulé AFFICHÉ d'un rôle-capacité (B) — §7.2.
///
/// Miroir de `GET /tenant/roles/{id}/titles` (§5.2). Ne change **jamais** une
/// permission (garde-fou §11.4) : c'est purement du libellé, résolu nœud →
/// tenant → global par le serveur.
library;

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
