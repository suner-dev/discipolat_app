/// Model for FormTemplate entity (backend FormController /api/v1/forms).
class FormTemplate {
  final String id;
  final String titre;
  final String? description;
  final String statut;
  final String? categorie;
  final bool anonyme;
  final String fieldsJson;
  final int nbReponses;
  final DateTime creeLe;

  FormTemplate({
    required this.id,
    required this.titre,
    this.description,
    required this.statut,
    this.categorie,
    this.anonyme = false,
    this.fieldsJson = '[]',
    this.nbReponses = 0,
    required this.creeLe,
  });

  factory FormTemplate.fromJson(Map<String, dynamic> json) => FormTemplate(
        id: (json['id'] ?? '').toString(),
        titre: (json['titre'] ?? json['title'] ?? '').toString(),
        description: json['description']?.toString(),
        statut: (json['statut'] ?? json['status'] ?? 'BROUILLON').toString(),
        categorie: json['categorie']?.toString(),
        anonyme: json['anonyme'] is bool
            ? json['anonyme'] as bool
            : (json['anonyme']?.toString().toLowerCase() == 'true'),
        fieldsJson: (json['fieldsJson'] ?? json['fields_json'] ?? '[]').toString(),
        nbReponses: json['nbReponses'] is int ? json['nbReponses'] as int : int.tryParse('${json['nbReponses']}') ?? 0,
        creeLe: DateTime.tryParse(json['creeLe']?.toString() ?? '') ?? DateTime.now(),
      );
}
