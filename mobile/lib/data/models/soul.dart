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
  final String? dateNaissance;
  final String? dateBapteme;
  final String? familleNom;
  final String? faiseurNom;
  final String? departementNom;

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
    this.dateNaissance,
    this.dateBapteme,
    this.familleNom,
    this.faiseurNom,
    this.departementNom,
  });

  String get nomComplet => prenom != null ? '$prenom $nom' : nom;

  /// Copie avec les infos d'encadrement issues de la fiche 360° (pastoral-360).
  Soul withEncadrement({
    String? familleNom,
    String? faiseurNom,
    String? departementNom,
    String? dateBapteme,
  }) {
    return Soul(
      id: id,
      nom: nom,
      prenom: prenom,
      email: email,
      telephone: telephone,
      typeDisciple: typeDisciple,
      statut: statut,
      dateIntegration: dateIntegration,
      faiseurId: faiseurId,
      familleId: familleId,
      dateDernierContact: dateDernierContact,
      dateNaissance: dateNaissance,
      dateBapteme: dateBapteme ?? this.dateBapteme,
      familleNom: familleNom ?? this.familleNom,
      faiseurNom: faiseurNom ?? this.faiseurNom,
      departementNom: departementNom ?? this.departementNom,
    );
  }

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
    dateNaissance: json['dateNaissance'] as String?,
    dateBapteme: json['dateBapteme'] as String?,
    familleNom: json['familleNom'] as String?,
    faiseurNom: json['faiseurNom'] as String?,
    departementNom: json['departementNom'] as String?,
  );
}
