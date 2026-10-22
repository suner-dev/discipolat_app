/// Roster des membres d'un nœud — hiérarchie ecclésiastique (§7, miroir BE
/// `GET /tenant/organization/nodes/{id}/members`).
///
/// Contrairement à l'agrégat (compteurs seuls, D7), cette vue est NOMINATIVE :
/// elle aligne sa visibilité sur l'annuaire `/users` déjà ouvert à tout membre
/// authentifié du tenant. Le serveur scoping/déduplique/borne la pagination ;
/// le mobile n'invente jamais de rattachement (chaîne partielle ⇒ badge posé,
/// jamais de chaînon fabriqué).
library;

class NodeMemberRow {
  const NodeMemberRow({
    required this.userId,
    required this.direct,
    required this.origine,
    this.firstName,
    this.lastName,
    this.email,
    this.photoUrl,
    this.statut,
    this.attachedNodeId,
    this.attachedNodeName,
    this.attachedNodeType,
    this.roles = const [],
    this.depuis,
  });

  final String userId;
  final String? firstName;
  final String? lastName;
  final String? email;
  final String? photoUrl;
  final String? statut;
  final String? attachedNodeId;
  final String? attachedNodeName;
  final String? attachedNodeType;

  /// true = rattaché au nœud consulté ; false = via une sous-structure.
  final bool direct;

  /// Origine du rattachement (V231) : ASSIGNATION_V3 | ADHESION_NOEUD | RESPONSABLE_NOEUD.
  final String origine;
  final List<String> roles;
  final String? depuis;

  String get displayName {
    final name = <String?>[firstName, lastName]
        .where((e) => e != null && e.isNotEmpty)
        .join(' ')
        .trim();
    return name.isNotEmpty ? name : userId;
  }

  static String? _str(dynamic v) => v is String ? v : (v == null ? null : v.toString());

  factory NodeMemberRow.fromJson(Map<String, dynamic> json) => NodeMemberRow(
        userId: _str(json['userId']) ?? '',
        firstName: _str(json['firstName']),
        lastName: _str(json['lastName']),
        email: _str(json['email']),
        photoUrl: _str(json['photoUrl']),
        statut: _str(json['statut']),
        attachedNodeId: _str(json['attachedNodeId']),
        attachedNodeName: _str(json['attachedNodeName']),
        attachedNodeType: _str(json['attachedNodeType']),
        direct: json['direct'] == true,
        origine: _str(json['origine']) ?? 'ADHESION_NOEUD',
        roles: (json['roles'] as List?)?.map((e) => e.toString()).toList() ?? const [],
        depuis: _str(json['depuis']),
      );
}

/// Page de roster (pagination serveur) + compteurs direct/sous-arbre.
class NodeMembersPage {
  const NodeMembersPage({
    required this.nodeId,
    required this.scope,
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
    required this.memberCountDirect,
    required this.memberCountSousArbre,
    this.nodeName,
    this.nodeType,
  });

  final String nodeId;
  final String? nodeName;
  final String? nodeType;
  final String scope; // DIRECT | SUBTREE
  final List<NodeMemberRow> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final int memberCountDirect;
  final int memberCountSousArbre;

  bool get hasNext => page + 1 < totalPages;

  factory NodeMembersPage.fromJson(Map<String, dynamic> json) => NodeMembersPage(
        nodeId: (json['nodeId'] ?? '').toString(),
        nodeName: json['nodeName'] is String ? json['nodeName'] as String : null,
        nodeType: json['nodeType'] is String ? json['nodeType'] as String : null,
        scope: (json['scope'] ?? 'SUBTREE').toString(),
        content: (json['content'] as List?)
                ?.whereType<Map<String, dynamic>>()
                .map(NodeMemberRow.fromJson)
                .toList() ??
            const [],
        page: (json['page'] as num?)?.toInt() ?? 0,
        size: (json['size'] as num?)?.toInt() ?? 20,
        totalElements: (json['totalElements'] as num?)?.toInt() ?? 0,
        totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
        memberCountDirect: (json['memberCountDirect'] as num?)?.toInt() ?? 0,
        memberCountSousArbre: (json['memberCountSousArbre'] as num?)?.toInt() ?? 0,
      );

  static NodeMembersPage empty(String nodeId) => NodeMembersPage(
        nodeId: nodeId,
        scope: 'SUBTREE',
        content: const [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        memberCountDirect: 0,
        memberCountSousArbre: 0,
      );
}
