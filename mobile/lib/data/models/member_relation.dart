library;

/// V231 — modèle du rattachement déclaratif « Mon encadrement ».
///
/// Style : classes immuables écrites à la main, `const` constructor, champs
/// `final`, `factory ... fromJson` — exactement comme
/// `lib/data/models/member_role_assignment.dart` (aucun freezed /
/// json_serializable dans `lib/data/models/`).
///
/// Parsing **défensif** (`as String? ?? ''`) et JAMAIS de cast dur : un code
/// de relation renommé ou une réponse partielle ne doivent pas faire crasher
/// l'écran de profil.
class MemberRelation {
  const MemberRelation({
    required this.id,
    required this.otherUserId,
    required this.otherNom,
    required this.relationType,
    required this.typeLabel,
    this.fromUserId = '',
    this.fromNom = '',
    this.toUserId = '',
    this.toNom = '',
    this.statut = 'ACTIVE',
    this.note,
    this.createdAt,
    this.revocable = true,
  });

  final String id;
  final String fromUserId;
  final String fromNom;
  final String toUserId;
  final String toNom;
  final String otherUserId;
  final String otherNom;
  final String relationType;
  final String typeLabel;
  final String statut;
  final String? note;
  final String? createdAt;

  /// Le serveur indique si l'utilisateur courant peut retirer ce
  /// rattachement : l'interface ne doit pas proposer un bouton doomed.
  final bool revocable;

  bool get isActive => statut == 'ACTIVE';

  factory MemberRelation.fromJson(Map<String, dynamic> json) => MemberRelation(
        id: '${json['id'] ?? ''}',
        fromUserId: '${json['fromUserId'] ?? ''}',
        fromNom: '${json['fromNom'] ?? ''}',
        toUserId: '${json['toUserId'] ?? ''}',
        toNom: '${json['toNom'] ?? ''}',
        otherUserId: '${json['otherUserId'] ?? ''}',
        otherNom: json['otherNom'] == null || '${json['otherNom']}' == ''
            ? '—'
            : '${json['otherNom']}',
        relationType: '${json['relationType'] ?? ''}',
        typeLabel: '${json['typeLabel'] ?? json['relationType'] ?? ''}',
        statut: '${json['statut'] ?? 'ACTIVE'}',
        note: json['note']?.toString(),
        createdAt: json['createdAt']?.toString(),
        revocable: json['revocable'] is bool ? json['revocable'] as bool : true,
      );
}

/// Vue « mes relations » : sortantes (mes encadrants) + entrantes (mes
/// membres), telle que renvoyée par `GET /relations/me`.
class RelationsSummary {
  const RelationsSummary({this.sortantes = const [], this.entrantes = const []});

  final List<MemberRelation> sortantes;
  final List<MemberRelation> entrantes;

  factory RelationsSummary.fromJson(Map<String, dynamic> json) {
    List<MemberRelation> rows(Object? raw) => (raw is List)
        ? raw
            .whereType<Map<String, dynamic>>()
            .map(MemberRelation.fromJson)
            .toList()
        : const [];
    return RelationsSummary(
      sortantes: rows(json['sortantes']),
      entrantes: rows(json['entrantes']),
    );
  }
}

/// Page de « ses membres » — la liste est bornée et paginée côté serveur,
/// ce qui est indispensable : un encadrement peut dépasser le plafond de
/// rattachements entrants et faire exploser la réponse.
class MemberRelationPage {
  const MemberRelationPage({
    this.content = const [],
    this.totalElements = 0,
    this.totalPages = 0,
    this.number = 0,
  });

  final List<MemberRelation> content;
  final int totalElements;
  final int totalPages;
  final int number;

  bool get hasPrevious => number > 0;
  bool get hasNext => number + 1 < totalPages;

  factory MemberRelationPage.fromJson(Map<String, dynamic> json) {
    final raw = json['content'];
    return MemberRelationPage(
      content: raw is List
          ? raw
              .whereType<Map<String, dynamic>>()
              .map(MemberRelation.fromJson)
              .toList()
          : const [],
      totalElements: json['totalElements'] is num
          ? (json['totalElements'] as num).toInt()
          : 0,
      totalPages:
          json['totalPages'] is num ? (json['totalPages'] as num).toInt() : 0,
      number: json['number'] is num ? (json['number'] as num).toInt() : 0,
    );
  }
}

/// Type de rattachement paramétré par l'église (`MEMBER_RELATION_TYPE`).
class RelationType {
  const RelationType({required this.code, required this.label});

  final String code;
  final String label;

  factory RelationType.fromJson(Map<String, dynamic> json) => RelationType(
        code: '${json['code'] ?? ''}',
        label: '${json['label'] ?? json['code'] ?? ''}',
      );
}

/// Nœud de l'arbre de hiérarchie d'une personne (branche de l'organisation).
class HierarchyBranch {
  const HierarchyBranch({
    required this.nodeId,
    required this.nodeNom,
    required this.nodeType,
    required this.origine,
    this.steps = const [],
  });

  final String nodeId;
  final String nodeNom;
  final String nodeType;
  final String? origine;

  /// Chaîne d'ascendance, du nœud le plus proche (feuille) à la racine.
  final List<HierarchyStep> steps;

  factory HierarchyBranch.fromJson(Map<String, dynamic> json) {
    final noeud = json['noeud'] is Map
        ? Map<String, dynamic>.from(json['noeud'] as Map)
        : const <String, dynamic>{};
    final chaine = json['chaine'];
    return HierarchyBranch(
      nodeId: '${noeud['id'] ?? ''}',
      nodeNom: '${noeud['nom'] ?? '—'}',
      nodeType: '${noeud['type'] ?? ''}',
      origine: json['origine']?.toString(),
      steps: chaine is List
          ? chaine
              .whereType<Map<String, dynamic>>()
              .map(HierarchyStep.fromJson)
              .toList()
          : const [],
    );
  }
}

/// Un niveau de la chaîne d'ascendance : le nœud + son responsable.
class HierarchyStep {
  const HierarchyStep({required this.nom, this.responsableId, this.responsableNom});

  final String nom;
  final String? responsableId;
  final String? responsableNom;

  factory HierarchyStep.fromJson(Map<String, dynamic> json) {
    final resp = json['responsable'] is Map
        ? Map<String, dynamic>.from(json['responsable'] as Map)
        : null;
    return HierarchyStep(
      nom: '${json['nom'] ?? '—'}',
      responsableId: resp?['id']?.toString(),
      responsableNom: resp?['nom']?.toString(),
    );
  }
}

/// Ascendant unifié (organisation ∪ déclaratif).
class Ascendant {
  const Ascendant({
    required this.id,
    required this.nom,
    required this.via,
    this.noeud,
    this.typeLabel,
  });

  final String id;
  final String nom;
  final String via;
  final String? noeud;
  final String? typeLabel;

  bool get isDeclared => via == 'DECLARATIF';

  factory Ascendant.fromJson(Map<String, dynamic> json) => Ascendant(
        id: '${json['id'] ?? ''}',
        nom: '${json['nom'] ?? '—'}',
        via: '${json['via'] ?? 'ORGANISATION'}',
        noeud: json['noeud']?.toString(),
        typeLabel: json['typeLabel']?.toString(),
      );
}
