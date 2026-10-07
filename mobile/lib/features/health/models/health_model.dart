// Modèle health — aligné sur le contrat serveur RÉEL (vérifié entité par
// entité dans backend/…/modules/health, migrations V141 et V235).
//
// RÈGLE D'IDENTIFIANTS — à ne pas confondre avec le reste de l'app :
//   TOUTES les entités de santé sont `GenerationType.UUID` côté serveur :
//     PatientRecord, MedicalConsultation, Prescription, PharmacyItem,
//     PharmacyStock, HealthCampaign, HealthMedication, HealthKit, HealthDuty,
//     CampaignParticipant, Family.
//   → `String` côté Dart. Aucun `int id` ici : le `(json['id'] as num).toInt()`
//   de l'ancien modèle levait un TypeError sur la chaîne UUID renvoyée par le
//   serveur, cassant l'écran Santé dès le premier chargement.
//
// Les enums portent la valeur EXACTE du serveur (@Enumerated(EnumType.STRING)).
// Une valeur inconnue retombe sur une valeur par défaut : une liste ne doit
// jamais faire exploser le fromJson.
//
// `fromJson` est TOLÉRANT : le serveur sérialise des entités JPA dont
// certaines clés sont absentes (relations LAZY nonchargées, `deleted`, dates
// nulles). Une clé manquante vaut null, jamais un cast dur.
//
// Les classes sont en Dart simple : le modèle précédent était Freezed, donc
// son `.freezed.dart`/`.g.dart` pouvait rester PÉRIMÉ après une modification du
// modèle — l'app compilait alors avec l'ancien contrat, silencieusement.
// Un seul fichier supprime ce piège et rend `build_runner` inutile.

/// Identifiant UUID serveur : toujours une chaîne, jamais un nombre.
String? _uuid(Object? v) {
  if (v == null) return null;
  final s = v.toString();
  return s.isEmpty ? null : s;
}

String _str(Object? v, [String fallback = '']) => v?.toString() ?? fallback;

int? _i(Object? v) {
  if (v == null) return null;
  if (v is num) return v.toInt();
  return int.tryParse(v.toString());
}

int _int(Object? v, [int fallback = 0]) => _i(v) ?? fallback;

double? _f(Object? v) {
  if (v == null) return null;
  if (v is num) return v.toDouble();
  return double.tryParse(v.toString());
}

double _dbl(Object? v, [double fallback = 0]) => _f(v) ?? fallback;

bool _b(Object? v, {bool fallback = false}) {
  if (v == null) return fallback;
  if (v is bool) return v;
  return v.toString() == 'true' || v == 1;
}

/// Date sérialisée par Jackson pour un `Instant` / `LocalDateTime` :
/// ISO-8601, éventuellement en Z, parfois `LocalDate` seul.
DateTime? _dt(Object? v) {
  if (v == null) return null;
  if (v is DateTime) return v;
  return DateTime.tryParse(v.toString());
}

/// Repli pour une date obligatoire manquante : une date absente ne doit pas
/// devenir « maintenant », qui ferait croire qu'une campagne démarre aujourd'hui.
final DateTime _epoch = DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);

/// Liste de chaînes ; le serveur peut renvoyer `null` ou un tableau vide.
List<String>? _strList(Object? v) {
  if (v is List) return v.map((e) => e.toString()).toList();
  return null;
}

List<T>? _list<T>(Object? v, T Function(Map<String, dynamic>) build) {
  if (v is! List) return null;
  return v.whereType<Map>().map((e) => build(Map<String, dynamic>.from(e))).toList();
}

/// Liste d'objets simple (`Map<String, dynamic>` côté Dart).
Map<String, dynamic>? _map(Object? v) =>
    v is Map ? Map<String, dynamic>.from(v) : null;

// ── Enums (valeurs = noms serveur, vérifiés sur les entités) ────────────────

enum PatientStatus {
  active('ACTIVE'),
  inactive('INACTIVE'),
  discharged('DISCHARGED'),
  deceased('DECEDE');

  const PatientStatus(this.wire);
  final String wire;

  /// Le serveur `PatientRecord` ne porte pas de statut d'énumération : le
  /// mobile applique son propre statut d'affichage. On accepte donc aussi les
  /// formes Dart historiques.
  static PatientStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in PatientStatus.values) {
      if (e.wire == v) return e;
    }
    return PatientStatus.active;
  }

  String get displayName {
    switch (this) {
      case PatientStatus.active:
        return 'Actif';
      case PatientStatus.inactive:
        return 'Inactif';
      case PatientStatus.discharged:
        return 'Sorti';
      case PatientStatus.deceased:
        return 'Décédé';
    }
  }
}

enum ConsultationType {
  general('GENERAL'),
  specialist('SPECIALIST'),
  emergency('EMERGENCY'),
  followUp('FOLLOW_UP'),
  preventive('PREVENTIVE'),
  prenatal('PRENATAL'),
  vaccination('VACCINATION');

  const ConsultationType(this.wire);
  final String wire;

  static ConsultationType fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in ConsultationType.values) {
      if (e.wire == v) return e;
    }
    // `MedicalConsultation.typeConsultation` est un String libre côté serveur
    // (TRIAGE, CONSULTATION, SUIVI…) : on mappe les synonymes documentés.
    switch (v) {
      case 'CONSULTATION':
        return ConsultationType.general;
      case 'TRIAGE':
        return ConsultationType.emergency;
      case 'SUIVI':
        return ConsultationType.followUp;
    }
    return ConsultationType.general;
  }

  String get displayName {
    switch (this) {
      case ConsultationType.general:
        return 'Générale';
      case ConsultationType.specialist:
        return 'Spécialiste';
      case ConsultationType.emergency:
        return 'Urgence';
      case ConsultationType.followUp:
        return 'Suivi';
      case ConsultationType.preventive:
        return 'Préventive';
      case ConsultationType.prenatal:
        return 'Prénatale';
      case ConsultationType.vaccination:
        return 'Vaccination';
    }
  }
}

enum ConsultationStatus {
  scheduled('SCHEDULED'),
  inProgress('IN_PROGRESS'),
  completed('COMPLETED'),
  cancelled('CANCELLED'),
  noShow('NO_SHOW');

  const ConsultationStatus(this.wire);
  final String wire;

  static ConsultationStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in ConsultationStatus.values) {
      if (e.wire == v) return e;
    }
    return ConsultationStatus.scheduled;
  }

  String get displayName {
    switch (this) {
      case ConsultationStatus.scheduled:
        return 'Planifiée';
      case ConsultationStatus.inProgress:
        return 'En cours';
      case ConsultationStatus.completed:
        return 'Terminée';
      case ConsultationStatus.cancelled:
        return 'Annulée';
      case ConsultationStatus.noShow:
        return 'Absent';
    }
  }
}

enum CampaignType {
  vaccination('VACCINATION'),
  screening('SCREENING'),
  awareness('AWERENESS'),
  bloodDonation('BLOOD_DONATION'),
  healthCheck('HEALTH_CHECK'),
  nutrition('NUTRITION'),
  maternalChild('MATERNAL_CHILD');

  const CampaignType(this.wire);
  final String wire;

  static CampaignType fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in CampaignType.values) {
      if (e.wire == v) return e;
    }
    return CampaignType.screening;
  }

  String get displayName {
    switch (this) {
      case CampaignType.vaccination:
        return 'Vaccination';
      case CampaignType.screening:
        return 'Dépistage';
      case CampaignType.awareness:
        return 'Sensibilisation';
      case CampaignType.bloodDonation:
        return 'Don de sang';
      case CampaignType.healthCheck:
        return 'Bilan de santé';
      case CampaignType.nutrition:
        return 'Nutrition';
      case CampaignType.maternalChild:
        return 'Mère-enfant';
    }
  }
}

enum CampaignStatus {
  planned('PLANNED'),
  active('ACTIVE'),
  completed('COMPLETED'),
  cancelled('CANCELLED');

  const CampaignStatus(this.wire);
  final String wire;

  static CampaignStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in CampaignStatus.values) {
      if (e.wire == v) return e;
    }
    return CampaignStatus.planned;
  }

  String get displayName {
    switch (this) {
      case CampaignStatus.planned:
        return 'Planifiée';
      case CampaignStatus.active:
        return 'Active';
      case CampaignStatus.completed:
        return 'Terminée';
      case CampaignStatus.cancelled:
        return 'Annulée';
    }
  }
}

enum ParticipationStatus {
  registered('REGISTERED'),
  attended('ATTENDED'),
  absent('ABSENT'),
  cancelled('CANCELLED');

  const ParticipationStatus(this.wire);
  final String wire;

  static ParticipationStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in ParticipationStatus.values) {
      if (e.wire == v) return e;
    }
    return ParticipationStatus.registered;
  }
}

enum KitType {
  firstAid('FIRST_AID'),
  emergency('EMERGENCY'),
  trauma('TRAUMA'),
  delivery('DELIVERY'),
  custom('CUSTOM');

  const KitType(this.wire);
  final String wire;

  static KitType fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in KitType.values) {
      if (e.wire == v) return e;
    }
    return KitType.custom;
  }
}

enum KitStatus {
  ready('READY'),
  inUse('IN_USE'),
  needsRestock('NEEDS_RESTOCK'),
  expired('EXPIRED');

  const KitStatus(this.wire);
  final String wire;

  static KitStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in KitStatus.values) {
      if (e.wire == v) return e;
    }
    return KitStatus.ready;
  }
}

enum DutyStatus {
  planned('PLANNED'),
  inProgress('IN_PROGRESS'),
  completed('COMPLETED'),
  cancelled('CANCELLED');

  const DutyStatus(this.wire);
  final String wire;

  static DutyStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in DutyStatus.values) {
      if (e.wire == v) return e;
    }
    return DutyStatus.planned;
  }
}

/// `PharmacyStock.StockStatus` côté serveur (voir PharmacyStock.java).
enum StockStatus {
  enStock('EN_STOCK'),
  lowStock('LOW_STOCK'),
  outOfStock('OUT_OF_STOCK'),
  expired('EXPIRED');

  const StockStatus(this.wire);
  final String wire;

  static StockStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in StockStatus.values) {
      if (e.wire == v) return e;
    }
    return StockStatus.enStock;
  }
}

/// `Prescription.PrescriptionStatus` côté serveur.
enum PrescriptionStatus {
  active('ACTIVE'),
  completed('COMPLETED'),
  cancelled('CANCELLED');

  const PrescriptionStatus(this.wire);
  final String wire;

  static PrescriptionStatus fromWire(Object? raw) {
    final v = raw?.toString();
    for (final e in PrescriptionStatus.values) {
      if (e.wire == v) return e;
    }
    return PrescriptionStatus.active;
  }
}

// ── Entités ────────────────────────────────────────────────────────────────

/// `PatientRecord` — lesDefaults/état civil sont portés par `User.person`.
class Patient {
  const Patient({
    required this.id,
    this.personId,
    this.firstName = '',
    this.lastName = '',
    this.familyId,
    this.familyName,
    this.bloodType,
    this.allergies,
    this.chronicConditions,
    this.notes,
    this.deleted = false,
    required this.createdAt,
    this.updatedAt,
    this.status = PatientStatus.active,
    this.dateOfBirth,
    this.gender = '',
    this.photoUrl,
    this.phone,
    this.email,
    this.address,
  });

  final String id;
  final String? personId;
  final String firstName;
  final String lastName;
  final String? familyId;
  final String? familyName;
  final String? bloodType;
  final List<String>? allergies;
  final List<String>? chronicConditions;
  final String? notes;
  final bool deleted;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final PatientStatus status;

  /// Ces champs ne sont pas portés par `PatientRecord` : ils viennent de la
  /// `User` associée, incluse ou non par le contrôleur. Ils restent exposés
  /// pour que les cartes patient continuent de s'afficher, avec des valeurs
  /// neutres quand le serveur ne les envoie pas.
  final DateTime? dateOfBirth;
  final String gender;
  final String? photoUrl;
  final String? phone;
  final String? email;
  final String? address;

  String get fullName {
    final n = '$firstName $lastName'.trim();
    return n.isEmpty ? 'Patient' : n;
  }

  bool get isActive => !deleted;

  /// Âge calculé ; 0 si la date de naissance est absente (le serveur ne
  /// l'expose pas systématiquement). Jamais négatif.
  int get age {
    final d = dateOfBirth;
    if (d == null) return 0;
    final now = DateTime.now();
    var a = now.year - d.year;
    if (now.month < d.month || (now.month == d.month && now.day < d.day)) a--;
    return a < 0 ? 0 : a;
  }

  factory Patient.fromJson(Map<String, dynamic> json) {
    // `PatientRecord` ne porte pas l'état civil : on lit `person` si le
    // contrôleur l'a inclu, sinon on retombe sur des valeurs vides.
    final person = _map(json['person']) ?? const {};
    return Patient(
      id: _str(json['id']),
      personId: _uuid(json['personId'] ?? person['id']),
      firstName: _str(json['firstName'] ?? person['firstName']),
      lastName: _str(json['lastName'] ?? person['lastName']),
      familyId: _uuid(json['familyId'] ?? _map(json['family'])?['id']),
      familyName: json['familyName']?.toString(),
      bloodType: json['bloodType']?.toString() ?? json['groupeSanguin']?.toString(),
      allergies: _strList(json['allergies']),
      chronicConditions: _strList(json['chronicConditions'] ?? json['antecedents']),
      notes: json['notes']?.toString() ?? json['notesSensibles']?.toString(),
      deleted: _b(json['deleted']),
      createdAt: _dt(json['createdAt']),
      updatedAt: _dt(json['updatedAt']),
      status: PatientStatus.fromWire(json['status']),
      dateOfBirth: _dt(json['dateOfBirth'] ?? person['dateOfBirth']),
      gender: _str(json['gender'] ?? person['gender']),
      photoUrl: json['photoUrl']?.toString() ?? person['photoUrl']?.toString(),
      phone: json['phone']?.toString() ?? person['phone']?.toString(),
      email: json['email']?.toString() ?? person['email']?.toString(),
      address: json['address']?.toString() ?? person['address']?.toString(),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'personId': personId,
        'familyId': familyId,
        'bloodType': bloodType,
        'allergies': allergies,
        'chronicConditions': chronicConditions,
        'notes': notes,
      };
}

class VitalSigns {
  const VitalSigns({
    this.temperature,
    this.heartRate,
    this.systolicBP,
    this.diastolicBP,
    this.respiratoryRate,
    this.oxygenSaturation,
    this.weight,
    this.height,
    this.bmi,
  });

  final double? temperature;
  final int? heartRate;
  final int? systolicBP;
  final int? diastolicBP;
  final int? respiratoryRate;
  final double? oxygenSaturation;
  final double? weight;
  final double? height;
  final double? bmi;

  factory VitalSigns.fromJson(Map<String, dynamic> json) => VitalSigns(
        temperature: _f(json['temperature']),
        heartRate: _i(json['heartRate']),
        systolicBP: _i(json['systolicBP']),
        diastolicBP: _i(json['diastolicBP']),
        respiratoryRate: _i(json['respiratoryRate']),
        oxygenSaturation: _f(json['oxygenSaturation']),
        weight: _f(json['weight']),
        height: _f(json['height']),
        bmi: _f(json['bmi']),
      );

  Map<String, dynamic> toJson() => {
        'temperature': temperature,
        'heartRate': heartRate,
        'systolicBP': systolicBP,
        'diastolicBP': diastolicBP,
        'respiratoryRate': respiratoryRate,
        'oxygenSaturation': oxygenSaturation,
        'weight': weight,
        'height': height,
        'bmi': bmi,
      };
}

/// `MedicalConsultation` — `typeConsultation` est un String côté serveur.
class Consultation {
  Consultation({
    required this.id,
    required this.patientId,
    this.patientName,
    this.practitionerId,
    this.doctorName,
    required this.consultationDate,

    this.type = ConsultationType.general,
    this.chiefComplaint,
    this.diagnosis,
    this.treatmentPlan,
    this.notes,
    this.vitalSigns,
    this.prescriptions,
    this.status = ConsultationStatus.scheduled,
    this.familyId,
    this.resultat,
    this.orientation,
    this.deleted = false,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String patientId;
  final String? patientName;
  final String? practitionerId;
  final String? doctorName;

  /// `consultation_date` est NOT NULL en base ; `fromJson` applique un repli
  /// explicite pour qu'un payload incomplet ne casse pas l'affichage.
  final DateTime consultationDate;
  final ConsultationType type;
  final String? chiefComplaint;
  final String? diagnosis;
  final String? treatmentPlan;
  final String? notes;
  final VitalSigns? vitalSigns;
  final List<Prescription>? prescriptions;
  final ConsultationStatus status;
  final String? familyId;
  final String? resultat;
  final String? orientation;
  final bool deleted;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  /// `dateTime` : nom historique conservé (écran + widgets).
  DateTime get dateTime => consultationDate;

  factory Consultation.fromJson(Map<String, dynamic> json) => Consultation(
        id: _str(json['id']),
        patientId: _str(json['patientId'] ?? _map(json['patient'])?['id']),
        patientName: json['patientName']?.toString(),
        practitionerId: _uuid(json['practitionerId'] ?? _map(json['practitioner'])?['id']),
        doctorName: json['doctorName']?.toString(),
        consultationDate: _dt(json['consultationDate'] ?? json['dateTime']) ?? _epoch,
        type: ConsultationType.fromWire(json['type'] ?? json['typeConsultation']),
        chiefComplaint: json['chiefComplaint']?.toString() ?? json['motif']?.toString(),
        diagnosis: json['diagnosis']?.toString(),
        treatmentPlan: json['treatmentPlan']?.toString() ?? json['traitement']?.toString(),
        notes: json['notes']?.toString(),
        vitalSigns: _map(json['vitalSigns']) == null
            ? null
            : VitalSigns.fromJson(Map<String, dynamic>.from(json['vitalSigns'] as Map)),
        prescriptions: _list(json['prescriptions'], Prescription.fromJson),
        status: ConsultationStatus.fromWire(json['status']),
        familyId: _uuid(json['familyId'] ?? _map(json['family'])?['id']),
        resultat: json['resultat']?.toString(),
        orientation: json['orientation']?.toString(),
        deleted: _b(json['deleted']),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'patientId': patientId,
        'practitionerId': practitionerId,
        'familyId': familyId,
        'typeConsultation': type.wire,
        'motif': chiefComplaint,
        'diagnostic': diagnosis,
        'traitement': treatmentPlan,
        'resultat': resultat,
        'orientation': orientation,
        'status': status.wire,
      };
}

/// `Prescription` — le médicament est un String (`medicament`), pas un UUID.
class Prescription {
  const Prescription({
    required this.id,
    this.consultationId,
    this.patientId,
    this.medicament = '',
    this.dosage = '',
    this.posologie = '',
    this.duree = '',
    this.notes,
    this.status = PrescriptionStatus.active,
    this.createdAt,
  });

  final String id;
  final String? consultationId;
  final String? patientId;
  final String medicament;
  final String dosage;
  final String posologie;
  final String duree;
  final String? notes;
  final PrescriptionStatus status;
  final DateTime? createdAt;

  /// `medicationName` : le serveur stocke le nom du produit dans `medicament`.
  String get medicationName => medicament;
  String get medicationId => medicament;
  String get frequency => posologie;

  bool get isActive => status == PrescriptionStatus.active;

  factory Prescription.fromJson(Map<String, dynamic> json) => Prescription(
        id: _str(json['id']),
        consultationId: _uuid(json['consultationId'] ?? _map(json['consultation'])?['id']),
        patientId: _uuid(json['patientId'] ?? _map(json['patient'])?['id']),
        medicament: _str(json['medicament'] ?? json['medicationName']),
        dosage: _str(json['dosage']),
        posologie: _str(json['posologie'] ?? json['frequency']),
        duree: _str(json['duree'] ?? json['duration']),
        notes: json['notes']?.toString(),
        status: PrescriptionStatus.fromWire(json['status']),
        createdAt: _dt(json['createdAt']),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'consultationId': consultationId,
        'patientId': patientId,
        'medicament': medicament,
        'dosage': dosage,
        'posologie': posologie,
        'duree': duree,
        'notes': notes,
        'status': status.wire,
      };
}

/// `HealthMedication` (V235) —Attention : ce n'est PAS le référentiel des
/// ordonnances ; `Prescription.medicament` est un texte libre.
class Medication {
  const Medication({
    required this.id,
    required this.name,
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
  final String name;
  final String? description;
  final String? dosage;
  final String? frequency;
  final int stockQuantity;
  final String? unit;
  final DateTime? expiryDate;
  final bool isActive;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  factory Medication.fromJson(Map<String, dynamic> json) => Medication(
        id: _str(json['id']),
        name: _str(json['name']),
        description: json['description']?.toString(),
        dosage: json['dosage']?.toString(),
        frequency: json['frequency']?.toString(),
        stockQuantity: _int(json['stockQuantity']),
        unit: json['unit']?.toString(),
        expiryDate: _dt(json['expiryDate']),
        isActive: _b(json['isActive'], fallback: true),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'description': description,
        'dosage': dosage,
        'frequency': frequency,
        'stockQuantity': stockQuantity,
        'unit': unit,
        'expiryDate': expiryDate?.toUtc().toIso8601String(),
        'isActive': isActive,
      };
}

/// `PharmacyStock` — stock d'un `PharmacyItem` (médicaments physiques).
class PharmacyStock {
  PharmacyStock({
    required this.id,
    this.pharmacyItemId,
    this.itemId,
    this.medicationId,
    this.medicationName = 'Article de pharmacie',
    this.lotNumber = '',
    this.quantite = 0,
    this.seuilAlerte = 0,
    this.dateExpiration,
    this.prixUnitaire,
    this.status = StockStatus.enStock,
    this.deleted = false,
    this.createdAt,
    this.updatedAt,
    this.location,
    this.reservedQuantity = 0,
  });

  final String id;
  final String? pharmacyItemId;
  final String? itemId;
  final String? medicationId;

  /// Le nom vient du `PharmacyItem` parent (colonne `nom` NOT NULL). Quand le
  /// contrôleur ne l'expose pas, on affiche un libellé explicite plutôt qu'une
  /// chaîne vide : une carte de stock sans nom est illisible.
  final String medicationName;
  final String lotNumber;
  final int quantite;
  final int seuilAlerte;
  final DateTime? dateExpiration;
  final double? prixUnitaire;
  final StockStatus status;
  final bool deleted;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  /// Champs d'affichage non portés par `PharmacyStock` : la localisation vient
  /// du `PharmacyItem` parent, la quantité réservée n'est pas stockée en base.
  final String? location;
  final int reservedQuantity;

  /// Le serveur ne stocke qu'un seuil d'alerte (unique) : on l'expose aussi
  /// sous le nom historique `minStockLevel` utilisé par les widgets.
  int get quantity => quantite;
  int get minStockLevel => seuilAlerte;
  int get maxStockLevel => seuilAlerte;

  /// Alias historiques utilisés par les cartes de stock. `batchNumber` et
  /// `expiryDate` sont non nuls : `date_expiration` est NOT NULL en base, et
  /// un lot sans date exploitable doit quand même s'afficher.
  String get batchNumber => lotNumber;
  double? get unitCost => prixUnitaire;
  double? get sellingPrice => prixUnitaire;

  DateTime get expiryDate => dateExpiration ?? _epoch;

  int get availableQuantity => (quantite - reservedQuantity).clamp(0, quantite);

  bool get isLowStock => quantite <= seuilAlerte;

  bool get isExpired {
    final e = dateExpiration;
    return e != null && DateTime.now().isAfter(e);
  }

  factory PharmacyStock.fromJson(Map<String, dynamic> json) => PharmacyStock(
        id: _str(json['id']),
        pharmacyItemId: _uuid(json['pharmacyItemId'] ?? _map(json['pharmacyItem'])?['id']),
        itemId: _uuid(json['itemId'] ?? _map(json['pharmacyItem'])?['id']),
        medicationId: _uuid(json['medicationId'] ?? _map(json['pharmacyItem'])?['id']),
        medicationName: _str(
          json['medicationName'] ?? _map(json['pharmacyItem'])?['nom'],
          'Article de pharmacie',
        ),
        lotNumber: _str(json['lotNumber']),
        quantite: _int(json['quantite'] ?? json['quantity']),
        seuilAlerte: _int(json['seuilAlerte'] ?? json['minStockLevel']),
        dateExpiration: _dt(json['dateExpiration'] ?? json['expiryDate']),
        prixUnitaire: _f(json['prixUnitaire'] ?? json['unitCost']),
        status: StockStatus.fromWire(json['status']),
        deleted: _b(json['deleted']),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
        // `PharmacyItem` ne porte pas de champ d'emplacement : la localisation
        // n'est lue que si le contrôleur l'expose explicitement.
        location: json['location']?.toString(),
        reservedQuantity: _int(json['reservedQuantity']),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'lotNumber': lotNumber,
        'quantite': quantite,
        'seuilAlerte': seuilAlerte,
        'dateExpiration': dateExpiration?.toIso8601String().split('T').first,
        'prixUnitaire': prixUnitaire,
        'status': status.wire,
      };
}

/// `HealthCampaign`.
class HealthCampaign {
  const HealthCampaign({
    required this.id,
    required this.name,
    this.description,
    this.type = CampaignType.screening,
    required this.startDate,
    required this.endDate,
    this.location,
    this.targetPopulation,
    this.registeredCount = 0,
    this.attendedCount = 0,
    this.status = CampaignStatus.planned,
    this.targetGroups,
    this.servicesOffered,
    this.notes,
    this.coordinatorId,
    this.coordinatorName,
    this.deleted = false,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String name;
  final String? description;
  final CampaignType type;
  /// `HealthCampaign.startDate`/`endDate` sont `NOT NULL` en base, mais le
  /// contrôleur peut les omettre. `fromJson` applique alors un repli explicite
  /// plutôt que d'exposer des nullables que chaque écran devrait gérer.
  final DateTime startDate;
  final DateTime endDate;
  final String? location;
  final int? targetPopulation;
  final int registeredCount;
  final int attendedCount;
  final CampaignStatus status;
  final List<String>? targetGroups;
  final List<String>? servicesOffered;
  final String? notes;
  /// `HealthCampaign` ne stocke pas de coordinateur : lu s'il est exposé.
  final String? coordinatorId;
  final String? coordinatorName;
  final bool deleted;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  bool get isActive =>
      status == CampaignStatus.active && DateTime.now().isBefore(endDate) && DateTime.now().isAfter(startDate);

  bool get isUpcoming => status == CampaignStatus.planned && DateTime.now().isBefore(startDate);

  bool get isCompleted => status == CampaignStatus.completed || DateTime.now().isAfter(endDate);

  factory HealthCampaign.fromJson(Map<String, dynamic> json) => HealthCampaign(
        id: _str(json['id']),
        name: _str(json['name']),
        description: json['description']?.toString(),
        type: CampaignType.fromWire(json['type']),
        startDate: _dt(json['startDate']) ?? _epoch,
        endDate: _dt(json['endDate']) ?? _dt(json['startDate']) ?? _epoch,
        location: json['location']?.toString(),
        targetPopulation: _i(json['targetPopulation']),
        registeredCount: _int(json['registeredCount']),
        attendedCount: _int(json['attendedCount']),
        status: CampaignStatus.fromWire(json['status']),
        targetGroups: _strList(json['targetGroups']),
        servicesOffered: _strList(json['servicesOffered']),
        notes: json['notes']?.toString(),
        coordinatorId: _uuid(json['coordinatorId']),
        coordinatorName: json['coordinatorName']?.toString(),
        deleted: _b(json['deleted']),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'description': description,
        'type': type.wire,
        'startDate': startDate.toIso8601String().split('T').first,
        'endDate': endDate.toIso8601String().split('T').first,
        'location': location,
        'targetPopulation': targetPopulation,
        'status': status.wire,
        'notes': notes,
      };
}

/// `CampaignParticipant` (V235) — la référence personne est `userId` (UUID).
class CampaignParticipant {
  const CampaignParticipant({
    required this.id,
    required this.campaignId,
    this.userId,
    this.patientId,
    this.patientName,
    this.registrationDate,
    this.attendanceDate,
    this.status = ParticipationStatus.registered,
    this.notes,
  });

  final String id;
  final String campaignId;
  final String? userId;
  final String? patientId;
  final String? patientName;
  final DateTime? registrationDate;
  final DateTime? attendanceDate;
  final ParticipationStatus status;
  final String? notes;

  factory CampaignParticipant.fromJson(Map<String, dynamic> json) => CampaignParticipant(
        id: _str(json['id']),
        campaignId: _str(json['campaignId']),
        userId: _uuid(json['userId'] ?? json['patientId']),
        patientId: _uuid(json['patientId'] ?? json['userId']),
        patientName: json['patientName']?.toString(),
        registrationDate: _dt(json['registeredAt'] ?? json['registrationDate']),
        attendanceDate: _dt(json['attendanceDate']),
        status: ParticipationStatus.fromWire(json['status']),
        notes: json['notes']?.toString(),
      );

  Map<String, dynamic> toJson() => {
        'campaignId': campaignId,
        'userId': userId ?? patientId,
      };
}

/// `HealthKit` (V235) — inventaire de kit, sans lignes d'inventaire en base.
class MedicalKit {
  const MedicalKit({
    required this.id,
    required this.name,
    this.description,
    this.category,
    this.type = KitType.custom,
    this.quantity = 0,
    this.location,
    this.isActive = true,
    this.status = KitStatus.ready,
    this.notes,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String name;
  final String? description;
  final String? category;
  final KitType type;
  final int quantity;
  final String? location;
  final bool isActive;
  final KitStatus status;
  final String? notes;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  /// Le serveur ne stocke pas d'inventaire détaillé par kit : la collection est
  /// vide plutôt qu'inventée, et `isComplete` vaut donc `true` par vacuité.
  List<KitItem> get items => const [];

  bool get isComplete => items.every((i) => i.currentQuantity >= i.requiredQuantity);

  factory MedicalKit.fromJson(Map<String, dynamic> json) => MedicalKit(
        id: _str(json['id']),
        name: _str(json['name']),
        description: json['description']?.toString(),
        category: json['category']?.toString(),
        type: KitType.fromWire(json['type'] ?? json['category']),
        quantity: _int(json['quantity']),
        location: json['location']?.toString(),
        isActive: _b(json['isActive'], fallback: true),
        status: KitStatus.fromWire(json['status']),
        notes: json['notes']?.toString(),
        createdAt: _dt(json['createdAt']),
        updatedAt: _dt(json['updatedAt']),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'description': description,
        'category': category,
        'quantity': quantity,
        'isActive': isActive,
      };
}

/// Ligne d'inventaire de kit : conservée pour l'affichage, alimentée par un
/// endpoint dédié le jour où la table existera (hors périmètre V235).
class KitItem {
  const KitItem({
    required this.id,
    required this.kitId,
    required this.medicationId,
    this.medicationName = '',
    this.requiredQuantity = 0,
    this.currentQuantity = 0,
    this.notes,
  });

  final String id;
  final String kitId;
  final String medicationId;
  final String medicationName;
  final int requiredQuantity;
  final int currentQuantity;
  final String? notes;

  bool get isSufficient => currentQuantity >= requiredQuantity;

  factory KitItem.fromJson(Map<String, dynamic> json) => KitItem(
        id: _str(json['id']),
        kitId: _str(json['kitId']),
        medicationId: _str(json['medicationId']),
        medicationName: _str(json['medicationName']),
        requiredQuantity: _int(json['requiredQuantity']),
        currentQuantity: _int(json['currentQuantity']),
        notes: json['notes']?.toString(),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'kitId': kitId,
        'medicationId': medicationId,
        'requiredQuantity': requiredQuantity,
        'currentQuantity': currentQuantity,
        'notes': notes,
      };
}

/// `HealthDuty` (V235) — garde/responsabilité du personnel de santé.
class StaffDuty {
  const StaffDuty({
    required this.id,
    this.staffId,
    this.staffName,
    this.name,
    this.description,
    this.role = 'STAFF',
    this.scheduledAt,
    this.completedAt,
    this.startTime,
    this.endTime,
    this.status = DutyStatus.planned,
    this.isActive = true,
  });

  final String id;
  final String? staffId;
  final String? staffName;
  final String? name;
  final String? description;
  final String role;
  final DateTime? scheduledAt;
  final DateTime? completedAt;
  final DateTime? startTime;
  final DateTime? endTime;
  final DutyStatus status;
  final bool isActive;

  /// `HealthDuty` n'a qu'un horodatage de début : `endTime` vaut `startTime`
  /// tant que le serveur n'expose pas de fin de vacation.
  DateTime? get begin => startTime ?? scheduledAt;

  factory StaffDuty.fromJson(Map<String, dynamic> json) => StaffDuty(
        id: _str(json['id']),
        staffId: _uuid(json['staffId'] ?? json['userId']),
        staffName: json['staffName']?.toString(),
        name: json['name']?.toString(),
        description: json['description']?.toString(),
        role: _str(json['role'] ?? json['dutyType'], 'STAFF'),
        scheduledAt: _dt(json['scheduledAt']),
        completedAt: _dt(json['completedAt']),
        startTime: _dt(json['startTime'] ?? json['scheduledAt']),
        endTime: _dt(json['endTime'] ?? json['scheduledAt']),
        status: DutyStatus.fromWire(json['status']),
        isActive: _b(json['isActive'], fallback: true),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'description': description,
        'dutyType': role,
        'scheduledAt': (startTime ?? scheduledAt)?.toUtc().toIso8601String(),
        'isActive': isActive,
      };
}

/// Agrégat de rapport santé (`GET /health/reports/statistics`).
class HealthStatistics {
  const HealthStatistics({
    this.totalPatients = 0,
    this.totalConsultations = 0,
    this.totalPrescriptions = 0,
    this.activePatients = 0,
    this.newPatientsThisMonth = 0,
    this.consultationsThisMonth = 0,
    this.activePrescriptions = 0,
    this.totalMedications = 0,
    this.totalPharmacyItems = 0,
    this.totalStockItems = 0,
    this.lowStockItems = 0,
    this.expiringItems = 0,
    this.totalCampaigns = 0,
    this.activeCampaigns = 0,
    this.raw = const {},
  });

  final int totalPatients;
  final int totalConsultations;
  final int totalPrescriptions;
  final int activePatients;
  final int newPatientsThisMonth;
  final int consultationsThisMonth;
  final int activePrescriptions;
  final int totalMedications;
  final int totalPharmacyItems;
  final int totalStockItems;
  final int lowStockItems;
  final int expiringItems;
  final int totalCampaigns;
  final int activeCampaigns;

  /// Les clés du serveur sont conservées telles quelles : le rapport évolue
  /// vite et un écran ne doit pas casser sur une clé supplémentaire.
  final Map<String, dynamic> raw;

  factory HealthStatistics.fromJson(Map<String, dynamic> json) => HealthStatistics(
        totalPatients: _int(json['totalPatients'] ?? json['patients']),
        totalConsultations: _int(json['totalConsultations'] ?? json['consultations']),
        totalPrescriptions: _int(json['totalPrescriptions'] ?? json['prescriptions']),
        activePatients: _int(json['activePatients']),
        newPatientsThisMonth: _int(json['newPatientsThisMonth']),
        consultationsThisMonth: _int(json['consultationsThisMonth']),
        activePrescriptions: _int(json['activePrescriptions']),
        totalMedications: _int(json['totalMedications'] ?? json['medications']),
        totalPharmacyItems: _int(json['totalPharmacyItems']),
        totalStockItems: _int(json['totalStockItems']),
        lowStockItems: _int(json['lowStockItems']),
        expiringItems: _int(json['expiringItems']),
        totalCampaigns: _int(json['totalCampaigns'] ?? json['campaigns']),
        activeCampaigns: _int(json['activeCampaigns']),
        raw: Map<String, dynamic>.from(json),
      );

  Map<String, dynamic> toJson() => raw;
}
