/// Modèles du module santé — contrat exact V240 du backend
/// (`HealthController` + vues aplaties `HealthService`, monture `/api/v1/health`).
///
/// Règles vérifiées dans le code serveur (jamais supposées) :
/// 1. Tous les ids et références de personnes sont des **UUID** côté serveur
///    (`GenerationType.UUID`) → typés `String` ici, jamais `int`.
/// 2. Les endpoints à relation renvoient des **vues Map aplaties**
///    ({personId, personName}, {patientId, patientName}, {itemId, itemName},
///    {responsibleName, participantsCount}) : plus d'objet embarqué, plus de
///    `tenantId` ni `deleted` sur le fil.
/// 3. Les endpoints paginés renvoient `PageResponse` = `{content, page, size,
///    totalElements, totalPages}` — jamais une liste brute ni `{items}`.
/// 4. Les champs sont **français** côté serveur (`groupeSanguin`, `motif`,
///    `quantite`, `lieu`…) : le modèle les reprend tels quels.
/// 5. Les écritures doivent envoyer exactement la forme lue par le serveur :
///    associations sous forme imbriquée `{"person": {"id": "<uuid>"}}`
///    (désérialisation Jackson d'entité), inscription campagne sous forme
///    `{"userId": "<uuid>"}` (`Map<String, UUID>`).
/// 6. Dates : `LocalDate` → `yyyy-MM-dd`, `LocalDateTime`/`Instant` → ISO-8601
///    (configuration Jackson `write-dates-as-timestamps: false`).
library;

// ---------------------------------------------------------------------------
// Helpers de parsing tolérants — le serveur omet parfois des champs selon
// l'appel (création vs liste) ; un champ absent ne doit jamais faire craquer
// le parsing de toute la page.
// ---------------------------------------------------------------------------

String? _sNonEmpty(Object? v) {
  final s = v?.toString();
  return (s == null || s.isEmpty) ? null : s;
}

double? _d(Object? v) {
  if (v is num) return v.toDouble();
  if (v is String) return double.tryParse(v);
  return null;
}

int? _i(Object? v) {
  if (v is num) return v.toInt();
  if (v is String) return int.tryParse(v);
  return null;
}

bool _b(Object? v, {bool fallback = false}) {
  if (v is bool) return v;
  if (v == null) return fallback;
  final s = v.toString().toLowerCase();
  if (s == 'true') return true;
  if (s == 'false') return false;
  return fallback;
}

DateTime? _dt(Object? v) {
  if (v == null) return null;
  return DateTime.tryParse(v.toString());
}

// ---------------------------------------------------------------------------
// Énumérations wire (valeurs serveur exactes, parsing tolérant).
// ---------------------------------------------------------------------------

/// `PatientRecord.ConfidentialityLevel`
enum ConfidentialityLevel {
  strict('STRICT', 'Strict'),
  internal('INTERNAL', 'Interne'),
  general('GENERAL', 'Général');

  const ConfidentialityLevel(this.wire, this.displayName);
  final String wire;
  final String displayName;

  static ConfidentialityLevel fromWire(Object? v) {
    final s = v?.toString().toUpperCase();
    for (final e in values) {
      if (e.wire == s) return e;
    }
    return strict;
  }
}

/// `MedicalConsultation.ConsultationStatus`
enum ConsultationStatus {
  scheduled('SCHEDULED', 'Planifiée'),
  inProgress('IN_PROGRESS', 'En cours'),
  completed('COMPLETED', 'Terminée'),
  cancelled('CANCELLED', 'Annulée');

  const ConsultationStatus(this.wire, this.displayName);
  final String wire;
  final String displayName;

  static ConsultationStatus fromWire(Object? v) {
    final s = v?.toString().toUpperCase();
    for (final e in values) {
      if (e.wire == s) return e;
    }
    return scheduled;
  }
}

/// `Prescription.PrescriptionStatus`
enum PrescriptionStatus {
  active('ACTIVE', 'Active'),
  completed('COMPLETED', 'Terminée'),
  cancelled('CANCELLED', 'Annulée');

  const PrescriptionStatus(this.wire, this.displayName);
  final String wire;
  final String displayName;

  static PrescriptionStatus fromWire(Object? v) {
    final s = v?.toString().toUpperCase();
    for (final e in values) {
      if (e.wire == s) return e;
    }
    return active;
  }
}

/// `PharmacyStock.StockStatus` — accents inclus côté serveur.
enum StockStatus {
  enStock('EN_STOCK', 'En stock'),
  stockFaible('STOCK_FAIBLE', 'Stock faible'),
  expirant('EXPIRANT', 'Expirant'),
  expire('EXPIRÉ', 'Expiré'),
  epuise('ÉPUISÉ', 'Épuisé');

  const StockStatus(this.wire, this.displayName);
  final String wire;
  final String displayName;

  static StockStatus fromWire(Object? v) {
    final s = v?.toString();
    for (final e in values) {
      if (e.wire == s) return e;
    }
    return enStock;
  }
}

/// `HealthCampaign.CampaignType`
enum CampaignType {
  vaccination('VACCINATION', 'Vaccination'),
  depistage('DEPISTAGE', 'Dépistage'),
  sensibilisation('SENSIBILISATION', 'Sensibilisation'),
  donSang('DON_SANG', 'Don de sang'),
  atelier('ATELIER', 'Atelier'),
  autre('AUTRE', 'Autre');

  const CampaignType(this.wire, this.displayName);
  final String wire;
  final String displayName;

  static CampaignType fromWire(Object? v) {
    final s = v?.toString().toUpperCase();
    for (final e in values) {
      if (e.wire == s) return e;
    }
    return autre;
  }
}

/// `HealthCampaign.CampaignStatus`
enum CampaignStatus {
  planned('PLANNED', 'Planifiée'),
  inProgress('IN_PROGRESS', 'En cours'),
  completed('COMPLETED', 'Terminée'),
  cancelled('CANCELLED', 'Annulée');

  const CampaignStatus(this.wire, this.displayName);
  final String wire;
  final String displayName;

  static CampaignStatus fromWire(Object? v) {
    final s = v?.toString().toUpperCase();
    for (final e in values) {
      if (e.wire == s) return e;
    }
    return planned;
  }
}

/// `CampaignParticipant.Status`
enum ParticipantStatus {
  registered('REGISTERED', 'Inscrit'),
  attended('ATTENDED', 'Présent'),
  cancelled('CANCELLED', 'Annulé');

  const ParticipantStatus(this.wire, this.displayName);
  final String wire;
  final String displayName;

  static ParticipantStatus fromWire(Object? v) {
    final s = v?.toString().toUpperCase();
    for (final e in values) {
      if (e.wire == s) return e;
    }
    return registered;
  }
}

// ---------------------------------------------------------------------------
// PatientRecord — vue aplatie {personId, personName, familyId, familyName}.
// ---------------------------------------------------------------------------

class PatientRecord {
  const PatientRecord({
    required this.id,
    this.personId,
    this.personName,
    this.familyId,
    this.familyName,
    this.groupeSanguin,
    this.allergies,
    this.antecedents,
    this.medecinTraitant,
    this.medecinTel,
    this.mesures,
    this.notesSensibles,
    this.numeroAssurance,
    this.poidsKg,
    this.tailleCm,
    this.tensionArterielle,
    this.glycemie,
    this.packYear,
    this.abouchement,
    this.confidentialityLevel = ConfidentialityLevel.strict,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String? personId;
  final String? personName;
  final String? familyId;
  final String? familyName;
  final String? groupeSanguin;
  final String? allergies;
  final String? antecedents;
  final String? medecinTraitant;
  final String? medecinTel;
  final String? mesures;
  final String? notesSensibles;
  final String? numeroAssurance;
  final double? poidsKg;
  final double? tailleCm;
  final String? tensionArterielle;
  final String? glycemie;
  final String? packYear;
  final String? abouchement;
  final ConfidentialityLevel confidentialityLevel;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory PatientRecord.fromJson(Map<String, dynamic> json) => PatientRecord(
        id: json['id']?.toString() ?? '',
        personId: _sNonEmpty(json['personId']),
        personName: _sNonEmpty(json['personName']),
        familyId: _sNonEmpty(json['familyId']),
        familyName: _sNonEmpty(json['familyName']),
        groupeSanguin: _sNonEmpty(json['groupeSanguin']),
        allergies: _sNonEmpty(json['allergies']),
        antecedents: _sNonEmpty(json['antecedents']),
        medecinTraitant: _sNonEmpty(json['medecinTraitant']),
        medecinTel: _sNonEmpty(json['medecinTel']),
        mesures: _sNonEmpty(json['mesures']),
        notesSensibles: _sNonEmpty(json['notesSensibles']),
        numeroAssurance: _sNonEmpty(json['numeroAssurance']),
        poidsKg: _d(json['poidsKg']),
        tailleCm: _d(json['tailleCm']),
        tensionArterielle: _sNonEmpty(json['tensionArterielle']),
        glycemie: _sNonEmpty(json['glycemie']),
        packYear: _sNonEmpty(json['packYear']),
        abouchement: _sNonEmpty(json['abouchement']),
        confidentialityLevel: ConfidentialityLevel.fromWire(json['confidentialityLevel']),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  /// Corps de création — le serveur désérialise une entité : la personne est
  /// une association imbriquée `{"person": {"id": …}}`, PAS une clé plate.
  static Map<String, dynamic> createBody({
    required String personId,
    String? familyId,
    String? groupeSanguin,
    String? allergies,
    String? antecedents,
    String? medecinTraitant,
    String? medecinTel,
    String? numeroAssurance,
    double? poidsKg,
    double? tailleCm,
    String? tensionArterielle,
    String? glycemie,
    ConfidentialityLevel? confidentialityLevel,
  }) {
    return <String, dynamic>{
      'person': <String, dynamic>{'id': personId},
      if (familyId != null) 'family': <String, dynamic>{'id': familyId},
      if (groupeSanguin != null) 'groupeSanguin': groupeSanguin,
      if (allergies != null) 'allergies': allergies,
      if (antecedents != null) 'antecedents': antecedents,
      if (medecinTraitant != null) 'medecinTraitant': medecinTraitant,
      if (medecinTel != null) 'medecinTel': medecinTel,
      if (numeroAssurance != null) 'numeroAssurance': numeroAssurance,
      if (poidsKg != null) 'poidsKg': poidsKg,
      if (tailleCm != null) 'tailleCm': tailleCm,
      if (tensionArterielle != null) 'tensionArterielle': tensionArterielle,
      if (glycemie != null) 'glycemie': glycemie,
      if (confidentialityLevel != null) 'confidentialityLevel': confidentialityLevel.wire,
    };
  }

  /// Corps de mise à jour — seuls les champs scalaires appliqués par
  /// `HealthService.updatePatientRecord` (les champs absents sont conservés).
  Map<String, dynamic> updateBody({
    String? groupeSanguin,
    String? allergies,
    String? antecedents,
    String? medecinTraitant,
    String? medecinTel,
    String? mesures,
    String? notesSensibles,
    String? numeroAssurance,
    double? poidsKg,
    double? tailleCm,
    String? tensionArterielle,
    String? glycemie,
    String? packYear,
    String? abouchement,
    ConfidentialityLevel? confidentialityLevel,
  }) {
    return <String, dynamic>{
      if (groupeSanguin != null) 'groupeSanguin': groupeSanguin,
      if (allergies != null) 'allergies': allergies,
      if (antecedents != null) 'antecedents': antecedents,
      if (medecinTraitant != null) 'medecinTraitant': medecinTraitant,
      if (medecinTel != null) 'medecinTel': medecinTel,
      if (mesures != null) 'mesures': mesures,
      if (notesSensibles != null) 'notesSensibles': notesSensibles,
      if (numeroAssurance != null) 'numeroAssurance': numeroAssurance,
      if (poidsKg != null) 'poidsKg': poidsKg,
      if (tailleCm != null) 'tailleCm': tailleCm,
      if (tensionArterielle != null) 'tensionArterielle': tensionArterielle,
      if (glycemie != null) 'glycemie': glycemie,
      if (packYear != null) 'packYear': packYear,
      if (abouchement != null) 'abouchement': abouchement,
      if (confidentialityLevel != null) 'confidentialityLevel': confidentialityLevel.wire,
    };
  }
}

// ---------------------------------------------------------------------------
// MedicalConsultation — vue {patientId, patientName, practitionerId,
// practitionerName}. `typeConsultation` est une String serveur bornée
// TRIAGE / CONSULTATION / SUIVI (pas un enum Java).
// ---------------------------------------------------------------------------

class MedicalConsultation {
  const MedicalConsultation({
    required this.id,
    this.patientId,
    this.patientName,
    this.practitionerId,
    this.practitionerName,
    this.familyId,
    this.familyName,
    this.consultationDate,
    this.typeConsultation,
    this.motif,
    this.constantes,
    this.diagnostic,
    this.traitement,
    this.resultat,
    this.orientation,
    this.status = ConsultationStatus.scheduled,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String? patientId;
  final String? patientName;
  final String? practitionerId;
  final String? practitionerName;
  final String? familyId;
  final String? familyName;

  /// `LocalDate` serveur → `yyyy-MM-dd`.
  final String? consultationDate;
  final String? typeConsultation;
  final String? motif;
  final String? constantes;
  final String? diagnostic;
  final String? traitement;
  final String? resultat;
  final String? orientation;
  final ConsultationStatus status;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory MedicalConsultation.fromJson(Map<String, dynamic> json) => MedicalConsultation(
        id: json['id']?.toString() ?? '',
        patientId: _sNonEmpty(json['patientId']),
        patientName: _sNonEmpty(json['patientName']),
        practitionerId: _sNonEmpty(json['practitionerId']),
        practitionerName: _sNonEmpty(json['practitionerName']),
        familyId: _sNonEmpty(json['familyId']),
        familyName: _sNonEmpty(json['familyName']),
        consultationDate: _sNonEmpty(json['consultationDate']),
        typeConsultation: _sNonEmpty(json['typeConsultation']),
        motif: _sNonEmpty(json['motif']),
        constantes: _sNonEmpty(json['constantes']),
        diagnostic: _sNonEmpty(json['diagnostic']),
        traitement: _sNonEmpty(json['traitement']),
        resultat: _sNonEmpty(json['resultat']),
        orientation: _sNonEmpty(json['orientation']),
        status: ConsultationStatus.fromWire(json['status']),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  /// Création : le serveur exige les associations imbriquées `patient` et
  /// `practitioner` (nullable = false). Praticien par défaut = acteur courant,
  /// résolu côté serveur par l'appelant qui passe son propre UUID.
  static Map<String, dynamic> createBody({
    required String patientId,
    required String practitionerId,
    String? familyId,
    required String consultationDate,
    String typeConsultation = 'CONSULTATION',
    String? motif,
    String? constantes,
    String? diagnostic,
    String? traitement,
    String? resultat,
    String? orientation,
    ConsultationStatus? status,
  }) {
    return <String, dynamic>{
      'patient': <String, dynamic>{'id': patientId},
      'practitioner': <String, dynamic>{'id': practitionerId},
      if (familyId != null) 'family': <String, dynamic>{'id': familyId},
      'consultationDate': consultationDate,
      'typeConsultation': typeConsultation,
      if (motif != null) 'motif': motif,
      if (constantes != null) 'constantes': constantes,
      if (diagnostic != null) 'diagnostic': diagnostic,
      if (traitement != null) 'traitement': traitement,
      if (resultat != null) 'resultat': resultat,
      if (orientation != null) 'orientation': orientation,
      if (status != null) 'status': status.wire,
    };
  }

  /// Mise à jour : champs scalaires uniquement (`updateConsultation` ignore
  /// les associations ; absence de clé = valeur conservée).
  static Map<String, dynamic> updateBody({
    String? consultationDate,
    String? typeConsultation,
    String? motif,
    String? constantes,
    String? diagnostic,
    String? traitement,
    String? resultat,
    String? orientation,
    ConsultationStatus? status,
  }) {
    return <String, dynamic>{
      if (consultationDate != null) 'consultationDate': consultationDate,
      if (typeConsultation != null) 'typeConsultation': typeConsultation,
      if (motif != null) 'motif': motif,
      if (constantes != null) 'constantes': constantes,
      if (diagnostic != null) 'diagnostic': diagnostic,
      if (traitement != null) 'traitement': traitement,
      if (resultat != null) 'resultat': resultat,
      if (orientation != null) 'orientation': orientation,
      if (status != null) 'status': status.wire,
    };
  }
}

// ---------------------------------------------------------------------------
// Prescription — vue {consultationId, patientId, patientName}.
// Le serveur n'expose PAS de PUT sur les prescriptions : création + lecture.
// ---------------------------------------------------------------------------

class Prescription {
  const Prescription({
    required this.id,
    this.consultationId,
    this.patientId,
    this.patientName,
    this.medicament,
    this.dosage,
    this.posologie,
    this.duree,
    this.status = PrescriptionStatus.active,
    this.notes,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String? consultationId;
  final String? patientId;
  final String? patientName;
  final String? medicament;
  final String? dosage;
  final String? posologie;
  final String? duree;
  final PrescriptionStatus status;
  final String? notes;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory Prescription.fromJson(Map<String, dynamic> json) => Prescription(
        id: json['id']?.toString() ?? '',
        consultationId: _sNonEmpty(json['consultationId']),
        patientId: _sNonEmpty(json['patientId']),
        patientName: _sNonEmpty(json['patientName']),
        medicament: _sNonEmpty(json['medicament']),
        dosage: _sNonEmpty(json['dosage']),
        posologie: _sNonEmpty(json['posologie']),
        duree: _sNonEmpty(json['duree']),
        status: PrescriptionStatus.fromWire(json['status']),
        notes: _sNonEmpty(json['notes']),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  static Map<String, dynamic> createBody({
    required String consultationId,
    required String patientId,
    required String medicament,
    String? dosage,
    String? posologie,
    String? duree,
    String? notes,
    PrescriptionStatus? status,
  }) {
    return <String, dynamic>{
      'consultation': <String, dynamic>{'id': consultationId},
      'patient': <String, dynamic>{'id': patientId},
      'medicament': medicament,
      if (dosage != null) 'dosage': dosage,
      if (posologie != null) 'posologie': posologie,
      if (duree != null) 'duree': duree,
      if (notes != null) 'notes': notes,
      if (status != null) 'status': status.wire,
    };
  }
}

// ---------------------------------------------------------------------------
// PharmacyItem — entité sans relation LAZY : contrat brut conservé.
// ---------------------------------------------------------------------------

class PharmacyItem {
  const PharmacyItem({
    required this.id,
    this.code,
    this.nom,
    this.description,
    this.categorie,
    this.unite,
    this.fournisseur,
    this.prixAchat,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String? code;
  final String? nom;
  final String? description;
  final String? categorie;
  final String? unite;
  final String? fournisseur;
  final double? prixAchat;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory PharmacyItem.fromJson(Map<String, dynamic> json) => PharmacyItem(
        id: json['id']?.toString() ?? '',
        code: _sNonEmpty(json['code']),
        nom: _sNonEmpty(json['nom']),
        description: _sNonEmpty(json['description']),
        categorie: _sNonEmpty(json['categorie']),
        unite: _sNonEmpty(json['unite']),
        fournisseur: _sNonEmpty(json['fournisseur']),
        prixAchat: _d(json['prixAchat']),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  static Map<String, dynamic> createBody({
    required String nom,
    String? code,
    String? description,
    String? categorie,
    String? unite,
    String? fournisseur,
    double? prixAchat,
  }) {
    return <String, dynamic>{
      'nom': nom,
      if (code != null) 'code': code,
      if (description != null) 'description': description,
      if (categorie != null) 'categorie': categorie,
      if (unite != null) 'unite': unite,
      if (fournisseur != null) 'fournisseur': fournisseur,
      if (prixAchat != null) 'prixAchat': prixAchat,
    };
  }
}

// ---------------------------------------------------------------------------
// PharmacyStock — vue {itemId, itemName} + isExpired calculé serveur.
// ---------------------------------------------------------------------------

class PharmacyStock {
  const PharmacyStock({
    required this.id,
    this.itemId,
    this.itemName,
    this.lotNumber,
    this.quantite,
    this.seuilAlerte,
    this.dateExpiration,
    this.prixUnitaire,
    this.status = StockStatus.enStock,
    this.isExpired = false,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String? itemId;
  final String? itemName;
  final String? lotNumber;
  final int? quantite;
  final int? seuilAlerte;

  /// `LocalDate` serveur → `yyyy-MM-dd`.
  final String? dateExpiration;
  final double? prixUnitaire;
  final StockStatus status;
  final bool isExpired;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory PharmacyStock.fromJson(Map<String, dynamic> json) => PharmacyStock(
        id: json['id']?.toString() ?? '',
        itemId: _sNonEmpty(json['itemId']),
        itemName: _sNonEmpty(json['itemName']),
        lotNumber: _sNonEmpty(json['lotNumber']),
        quantite: _i(json['quantite']),
        seuilAlerte: _i(json['seuilAlerte']),
        dateExpiration: _sNonEmpty(json['dateExpiration']),
        prixUnitaire: _d(json['prixUnitaire']),
        status: StockStatus.fromWire(json['status']),
        isExpired: _b(json['isExpired']),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  bool get isLowStock {
    final q = quantite;
    final s = seuilAlerte;
    if (q == null || s == null) return false;
    return q <= s;
  }

  static Map<String, dynamic> updateBody({
    int? quantite,
    int? seuilAlerte,
    double? prixUnitaire,
    String? dateExpiration,
    StockStatus? status,
  }) {
    return <String, dynamic>{
      if (quantite != null) 'quantite': quantite,
      if (seuilAlerte != null) 'seuilAlerte': seuilAlerte,
      if (prixUnitaire != null) 'prixUnitaire': prixUnitaire,
      if (dateExpiration != null) 'dateExpiration': dateExpiration,
      if (status != null) 'status': status.wire,
    };
  }
}

// ---------------------------------------------------------------------------
// HealthCampaign — vue {responsibleId, responsibleName, participantsCount}.
// ---------------------------------------------------------------------------

class HealthCampaign {
  const HealthCampaign({
    required this.id,
    this.title,
    this.description,
    this.campaignType = CampaignType.autre,
    this.startDate,
    this.endDate,
    this.lieu,
    this.responsibleId,
    this.responsibleName,
    this.familyId,
    this.familyName,
    this.objectif,
    this.cibles,
    this.status = CampaignStatus.planned,
    this.participantsCount = 0,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String? title;
  final String? description;
  final CampaignType campaignType;
  final String? startDate;
  final String? endDate;
  final String? lieu;
  final String? responsibleId;
  final String? responsibleName;
  final String? familyId;
  final String? familyName;
  final String? objectif;
  final String? cibles;
  final CampaignStatus status;
  final int participantsCount;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory HealthCampaign.fromJson(Map<String, dynamic> json) => HealthCampaign(
        id: json['id']?.toString() ?? '',
        title: _sNonEmpty(json['title']),
        description: _sNonEmpty(json['description']),
        campaignType: CampaignType.fromWire(json['campaignType']),
        startDate: _sNonEmpty(json['startDate']),
        endDate: _sNonEmpty(json['endDate']),
        lieu: _sNonEmpty(json['lieu']),
        responsibleId: _sNonEmpty(json['responsibleId']),
        responsibleName: _sNonEmpty(json['responsibleName']),
        familyId: _sNonEmpty(json['familyId']),
        familyName: _sNonEmpty(json['familyName']),
        objectif: _sNonEmpty(json['objectif']),
        cibles: _sNonEmpty(json['cibles']),
        status: CampaignStatus.fromWire(json['status']),
        participantsCount: _i(json['participantsCount']) ?? 0,
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  /// Création : `createCampaign` force le `responsible` à l'acteur courant —
  /// le corps ne doit donc PAS envoyer d'association responsible. `lieu` et
  /// `startDate` sont nullable=false côté serveur : obligatoires ici.
  static Map<String, dynamic> createBody({
    required String title,
    required String startDate,
    required String lieu,
    String? description,
    CampaignType campaignType = CampaignType.vaccination,
    String? endDate,
    String? objectif,
    String? cibles,
  }) {
    return <String, dynamic>{
      'title': title,
      'startDate': startDate,
      'lieu': lieu,
      'campaignType': campaignType.wire,
      if (description != null) 'description': description,
      if (endDate != null) 'endDate': endDate,
      if (objectif != null) 'objectif': objectif,
      if (cibles != null) 'cibles': cibles,
    };
  }
}

// ---------------------------------------------------------------------------
// CampaignParticipant — entité plate (pas de LAZY).
// ---------------------------------------------------------------------------

class CampaignParticipant {
  const CampaignParticipant({
    required this.id,
    this.campaignId,
    this.userId,
    this.status = ParticipantStatus.registered,
    this.registeredAt,
  });

  final String id;
  final String? campaignId;
  final String? userId;
  final ParticipantStatus status;
  final DateTime? registeredAt;

  factory CampaignParticipant.fromJson(Map<String, dynamic> json) => CampaignParticipant(
        id: json['id']?.toString() ?? '',
        campaignId: _sNonEmpty(json['campaignId']),
        userId: _sNonEmpty(json['userId']),
        status: ParticipantStatus.fromWire(json['status']),
        registeredAt: _dt(json['registeredAt']),
      );
}

// ---------------------------------------------------------------------------
// HealthMedication / HealthKit / HealthDuty — entités plates (dates `Instant`
// sérialisées par Jackson au format ISO UTC configuré).
// ---------------------------------------------------------------------------

class HealthMedication {
  const HealthMedication({
    required this.id,
    this.name,
    this.description,
    this.dosage,
    this.frequency,
    this.stockQuantity = 0,
    this.unit,
    this.expiryDate,
    this.isActive = true,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String? name;
  final String? description;
  final String? dosage;
  final String? frequency;
  final int stockQuantity;
  final String? unit;
  final DateTime? expiryDate;
  final bool isActive;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory HealthMedication.fromJson(Map<String, dynamic> json) => HealthMedication(
        id: json['id']?.toString() ?? '',
        name: _sNonEmpty(json['name']),
        description: _sNonEmpty(json['description']),
        dosage: _sNonEmpty(json['dosage']),
        frequency: _sNonEmpty(json['frequency']),
        stockQuantity: _i(json['stockQuantity']) ?? 0,
        unit: _sNonEmpty(json['unit']),
        expiryDate: _dt(json['expiryDate']),
        isActive: _b(json['isActive'], fallback: true),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  static Map<String, dynamic> createBody({
    required String name,
    String? description,
    String? dosage,
    String? frequency,
    int? stockQuantity,
    String? unit,
    DateTime? expiryDate,
  }) {
    return <String, dynamic>{
      'name': name,
      if (description != null) 'description': description,
      if (dosage != null) 'dosage': dosage,
      if (frequency != null) 'frequency': frequency,
      if (stockQuantity != null) 'stockQuantity': stockQuantity,
      if (unit != null) 'unit': unit,
      if (expiryDate != null) 'expiryDate': expiryDate.toUtc().toIso8601String(),
    };
  }
}

class HealthKit {
  const HealthKit({
    required this.id,
    this.name,
    this.description,
    this.category,
    this.quantity = 0,
    this.isActive = true,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String? name;
  final String? description;
  final String? category;
  final int quantity;
  final bool isActive;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory HealthKit.fromJson(Map<String, dynamic> json) => HealthKit(
        id: json['id']?.toString() ?? '',
        name: _sNonEmpty(json['name']),
        description: _sNonEmpty(json['description']),
        category: _sNonEmpty(json['category']),
        quantity: _i(json['quantity']) ?? 0,
        isActive: _b(json['isActive'], fallback: true),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );
}

class HealthDuty {
  const HealthDuty({
    required this.id,
    this.name,
    this.description,
    this.dutyType,
    this.scheduledAt,
    this.completedAt,
    this.isActive = true,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String? name;
  final String? description;
  final String? dutyType;
  final DateTime? scheduledAt;
  final DateTime? completedAt;
  final bool isActive;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory HealthDuty.fromJson(Map<String, dynamic> json) => HealthDuty(
        id: json['id']?.toString() ?? '',
        name: _sNonEmpty(json['name']),
        description: _sNonEmpty(json['description']),
        dutyType: _sNonEmpty(json['dutyType']),
        scheduledAt: _dt(json['scheduledAt']),
        completedAt: _dt(json['completedAt']),
        isActive: _b(json['isActive'], fallback: true),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );
}
