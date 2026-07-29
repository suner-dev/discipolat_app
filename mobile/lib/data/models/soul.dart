class Soul {
  final String id;
  final String nom;
  final String? prenom;
  final String? email;
  final String? telephone;
  final String typeDisciple;
  final String statut;
  final String dateIntegration;
  final String faiseurId;
  final String? familleId;
  final String? dateDernierContact;

  Soul({
    required this.id,
    required this.nom,
    this.prenom,
    this.email,
    this.telephone,
    required this.typeDisciple,
    required this.statut,
    required this.dateIntegration,
    required this.faiseurId,
    this.familleId,
    this.dateDernierContact,
  });

  String get nomComplet => prenom != null ? '$prenom $nom' : nom;

  factory Soul.fromJson(Map<String, dynamic> json) => Soul(
    id: json['id'] as String,
    nom: json['nom'] as String,
    prenom: json['prenom'] as String?,
    email: json['email'] as String?,
    telephone: json['telephone'] as String?,
    typeDisciple: json['typeDisciple'] as String,
    statut: json['statut'] as String,
    dateIntegration: json['dateIntegration'] as String,
    faiseurId: json['faiseurId'] as String,
    familleId: json['familleId'] as String?,
    dateDernierContact: json['dateDernierContact'] as String?,
  );
}
