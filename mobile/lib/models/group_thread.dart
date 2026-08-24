/// P10 — Modèle fil de discussion de groupe (mobile).
///
/// Représente un groupe (département/famille/équipe) exposé via
/// `DepartmentController` /GroupMessageController.
class GroupThread {
  final String id;
  final String nom;
  final String groupType;
  final String? description;

  GroupThread({
    required this.id,
    required this.nom,
    required this.groupType,
    this.description,
  });

  factory GroupThread.fromJson(Map<String, dynamic> json) => GroupThread(
        id: (json['id'] ?? json['uuid'])?.toString() ?? '',
        nom: (json['nom'] ?? json['name'] ?? json['nom'])?.toString() ?? 'Groupe',
        groupType: json['groupType']?.toString() ?? 'DEPARTMENT',
        description: json['description']?.toString(),
      );
}
