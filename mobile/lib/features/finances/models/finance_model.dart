// Modèle FINANCES — contrat exact du backend FinanceController
// (backend/.../finances/api/FinanceController.java, monture /api/v1/finances).
//
// Règles suivies ici :
// - tous les identifiants sont des UUID serveur → typés `String` (plus d'`int`) ;
// - les champs de chaque classe sont UNIQUEMENT les clés réellement renvoyées
//   par FinanceService (toMap / vues V236) — rien d'inventé ;
// - les champs réellement requis côté serveur sont marqués `required` ; les
//   champs absents de certaines vues (détail, non-rapprochées) sont nullables ;
// - classes immuables sans génération de code (ni freezed ni .g.dart) :
//   fromJson/toJson écrits à la main, tolérants (le serveur sérialise
//   BigDecimal en nombre, LocalDate en « yyyy-MM-dd », Instant en ISO-8601).

/// Types de transaction — seul ce que connaît le serveur
/// (FinanceTransaction.TransactionType : RECETTE, DEPENSE).
enum TransactionType {
  recette('RECETTE', 'Recette'),
  depense('DEPENSE', 'Dépense');

  const TransactionType(this.wire, this.label);

  /// Valeur envoyue au serveur / reçue de lui.
  final String wire;
  final String label;

  static TransactionType? fromWire(String? raw) {
    if (raw == null) return null;
    for (final t in values) {
      if (t.wire == raw.toUpperCase()) return t;
    }
    return null;
  }
}

/// Fréquences de tontine — FinanceTontine.Frequency (WEEKLY, MONTHLY,
/// QUARTERLY, YEARLY). Le serveur rejette toute autre valeur (requireFrequency).
enum TontineFrequency {
  weekly('WEEKLY', 'Hebdomadaire'),
  monthly('MONTHLY', 'Mensuel'),
  quarterly('QUARTERLY', 'Trimestriel'),
  yearly('YEARLY', 'Annuel');

  const TontineFrequency(this.wire, this.label);

  final String wire;
  final String label;

  static TontineFrequency? fromWire(String? raw) {
    if (raw == null) return null;
    for (final f in values) {
      if (f.wire == raw.toUpperCase()) return f;
    }
    return null;
  }
}

/// Normalise une date serveur en instant ISO-8601 complet, exigé par
/// `Instant.parse` côté FinanceService (donationDate, startDate, endDate).
String? isoInstant(String? raw) {
  if (raw == null || raw.isEmpty) return null;
  if (raw.contains('T')) return raw;
  // « yyyy-MM-dd » (10 caractères) → minuit UTC.
  if (raw.length == 10) return '${raw}T00:00:00Z';
  return raw;
}

double _d(Object? v) => v is num ? v.toDouble() : double.tryParse('${v ?? ''}') ?? 0;
int? _i(Object? v) => v is num ? v.toInt() : int.tryParse('${v ?? ''}');
bool _b(Object? v) => v == true || v == 'true';
String? _s(Object? v) => v == null ? null : '$v';

/// Transaction — clés de `FinanceService.toMap` (liste, création, mise à jour),
/// `getTransaction` (sous-ensemble) et `listUnreconciledTransactions` (sous-ensemble).
class Transaction {
  const Transaction({
    required this.id,
    this.type,
    this.categorie,
    required this.montant,
    this.devise,
    this.montantMinor,
    this.tauxVersBase,
    this.montantBase,
    this.description = '',
    this.dateTransaction = '',
    this.createdAt = '',
    this.deviseSymbole,
    this.fuseauHoraire,
  });

  factory Transaction.fromJson(Map<String, dynamic> json) => Transaction(
        id: _s(json['id']) ?? '',
        type: TransactionType.fromWire(_s(json['type'])),
        categorie: _s(json['categorie']),
        montant: _d(json['montant']),
        devise: _s(json['devise']),
        montantMinor: json['montantMinor'] is num ? (json['montantMinor'] as num) : null,
        tauxVersBase: json['tauxVersBase'] == null ? null : _d(json['tauxVersBase']),
        montantBase: json['montantBase'] == null ? null : _d(json['montantBase']),
        description: _s(json['description']) ?? '',
        dateTransaction: _s(json['dateTransaction']) ?? '',
        createdAt: _s(json['createdAt']) ?? '',
        deviseSymbole: _s(json['deviseSymbole']),
        fuseauHoraire: _s(json['fuseauHoraire']),
      );

  final String id;
  final TransactionType? type;
  final String? categorie;
  final double montant;
  final String? devise;
  final num? montantMinor;
  final double? tauxVersBase;
  final double? montantBase;
  final String description;

  /// LocalDate serveur, format brut « yyyy-MM-dd » (peut être '' sur certaines vues).
  final String dateTransaction;
  final String createdAt;
  final String? deviseSymbole;
  final String? fuseauHoraire;

  DateTime? get date => DateTime.tryParse(dateTransaction);
  String get symboleOuDevise => deviseSymbole ?? devise ?? '';

  /// Corps de `FinanceTransactionRequest` — uniquement les clés lues par
  /// FinanceService.createTransaction/updateTransaction.
  Map<String, dynamic> toJson() => <String, dynamic>{
        if (type != null) 'type': type!.wire,
        if (categorie != null) 'categorie': categorie,
        'montant': montant,
        'description': description,
        if (dateTransaction.isNotEmpty) 'dateTransaction': dateTransaction,
      };

  Transaction copyWith({
    String? id,
    TransactionType? type,
    String? categorie,
    double? montant,
    String? devise,
    String? description,
    String? dateTransaction,
  }) =>
      Transaction(
        id: id ?? this.id,
        type: type ?? this.type,
        categorie: categorie ?? this.categorie,
        montant: montant ?? this.montant,
        devise: devise ?? this.devise,
        montantMinor: montantMinor,
        tauxVersBase: tauxVersBase,
        montantBase: montantBase,
        description: description ?? this.description,
        dateTransaction: dateTransaction ?? this.dateTransaction,
        createdAt: createdAt,
        deviseSymbole: deviseSymbole,
        fuseauHoraire: fuseauHoraire,
      );
}

/// Compte — clés des vues `listAccounts` / `getAccount` / `createAccount`.
class Account {
  const Account({
    required this.id,
    this.name,
    this.accountNumber,
    this.bankName,
    required this.balance,
    this.devise,
    required this.isActive,
    this.createdAt = '',
    this.updatedAt,
  });

  factory Account.fromJson(Map<String, dynamic> json) => Account(
        id: _s(json['id']) ?? '',
        name: _s(json['name']),
        accountNumber: _s(json['accountNumber']),
        bankName: _s(json['bankName']),
        balance: _d(json['balance']),
        devise: _s(json['devise']),
        isActive: _b(json['isActive']),
        createdAt: _s(json['createdAt']) ?? '',
        updatedAt: _s(json['updatedAt']),
      );

  final String id;
  final String? name;
  final String? accountNumber;
  final String? bankName;
  final double balance;
  final String? devise;
  final bool isActive;
  final String createdAt;
  final String? updatedAt;

  /// Corps lu par `FinanceService.createAccount` (clé par clé).
  Map<String, dynamic> toJson() => <String, dynamic>{
        'name': name,
        if (accountNumber != null) 'accountNumber': accountNumber,
        if (bankName != null) 'bankName': bankName,
        'balance': balance,
        if (devise != null) 'devise': devise,
      };
}

/// Budget — clés des vues `listBudgets` / `getBudget` (consommation calculée
/// serveur) et `upsertBudget` (id/categorie/annee/montant).
class Budget {
  const Budget({
    required this.id,
    this.categorie,
    this.annee,
    required this.montant,
    this.depenseReelle = 0,
    this.consommationPct = 0,
    this.statut = '',
  });

  factory Budget.fromJson(Map<String, dynamic> json) => Budget(
        id: _s(json['id']) ?? '',
        categorie: _s(json['categorie']),
        annee: _i(json['annee']),
        montant: _d(json['montant']),
        depenseReelle: _d(json['depenseReelle']),
        consommationPct: _d(json['consommationPct']),
        statut: _s(json['statut']) ?? '',
      );

  final String id;
  final String? categorie;
  final int? annee;
  final double montant;
  final double depenseReelle;
  final double consommationPct;

  /// Statut calculé côté serveur : OK | ALERTE | DEPASSE (vides sur la réponse
  /// d'upsert).
  final String statut;

  bool get estDepasse => statut == 'DEPASSE';
  bool get estAlerte => statut == 'ALERTE';

  /// Corps de `FinanceBudgetRequest` — les trois seules clés lues
  /// (annee(), categorie(), montant()).
  Map<String, dynamic> toJson() => <String, dynamic>{
        'categorie': categorie,
        'annee': annee,
        'montant': montant,
      };
}

/// Tontine — clés des vues `listTontines` / `getTontine` / `createTontine`.
class Tontine {
  const Tontine({
    required this.id,
    this.name,
    this.description,
    required this.amountPerTurn,
    this.frequency,
    this.startDate,
    this.endDate,
    required this.isActive,
    this.createdAt = '',
    this.updatedAt,
  });

  factory Tontine.fromJson(Map<String, dynamic> json) => Tontine(
        id: _s(json['id']) ?? '',
        name: _s(json['name']),
        description: _s(json['description']),
        amountPerTurn: _d(json['amountPerTurn']),
        frequency: TontineFrequency.fromWire(_s(json['frequency'])),
        startDate: _s(json['startDate']),
        endDate: _s(json['endDate']),
        isActive: _b(json['isActive']),
        createdAt: _s(json['createdAt']) ?? '',
        updatedAt: _s(json['updatedAt']),
      );

  final String id;
  final String? name;
  final String? description;
  final double amountPerTurn;
  final TontineFrequency? frequency;

  /// Instant serveur sérialisé (ISO-8601 avec « T »), conservé brut.
  final String? startDate;
  final String? endDate;
  final bool isActive;
  final String createdAt;
  final String? updatedAt;

  DateTime? get debut => DateTime.tryParse(startDate ?? '');

  /// Corps lu par `FinanceService.createTontine` (frequency obligatoire,
  /// startDate/endDate passés à Instant.parse → ISO complet exigé).
  Map<String, dynamic> toJson() => <String, dynamic>{
        'name': name,
        if (description != null) 'description': description,
        'amountPerTurn': amountPerTurn,
        if (frequency != null) 'frequency': frequency!.wire,
        if (startDate != null) 'startDate': isoInstant(startDate),
        if (endDate != null) 'endDate': isoInstant(endDate),
      };
}

/// Membre de tontine — clés des vues `listTontineMembers` /
/// `createTontineMember`. Les personnes sont désignées par `userId` UUID.
class TontineMember {
  const TontineMember({
    required this.id,
    required this.tontineId,
    required this.userId,
    this.joinedAt = '',
    this.turnOrder = 0,
    required this.isActive,
  });

  factory TontineMember.fromJson(Map<String, dynamic> json) => TontineMember(
        id: _s(json['id']) ?? '',
        tontineId: _s(json['tontineId']) ?? '',
        userId: _s(json['userId']) ?? '',
        joinedAt: _s(json['joinedAt']) ?? '',
        turnOrder: _i(json['turnOrder']) ?? 0,
        isActive: _b(json['isActive']),
      );

  final String id;
  final String tontineId;
  final String userId;
  final String joinedAt;
  final int turnOrder;
  final bool isActive;

  /// Corps lu par `FinanceService.createTontineMember` : userId (UUID,
  /// obligatoire) et turnOrder (facultatif).
  Map<String, dynamic> toJson() => <String, dynamic>{
        'userId': userId,
        'turnOrder': turnOrder,
      };
}

/// Versement de tontine — clés de la vue `listTontinePayouts`.
class TontinePayout {
  const TontinePayout({
    required this.id,
    required this.tontineId,
    required this.memberId,
    required this.amount,
    this.payoutDate = '',
    this.turnNumber,
    this.createdAt = '',
  });

  factory TontinePayout.fromJson(Map<String, dynamic> json) => TontinePayout(
        id: _s(json['id']) ?? '',
        tontineId: _s(json['tontineId']) ?? '',
        memberId: _s(json['memberId']) ?? '',
        amount: _d(json['amount']),
        payoutDate: _s(json['payoutDate']) ?? '',
        turnNumber: _i(json['turnNumber']),
        createdAt: _s(json['createdAt']) ?? '',
      );

  final String id;
  final String tontineId;
  final String memberId;
  final double amount;
  final String payoutDate;
  final int? turnNumber;
  final String createdAt;
}

/// Don — clés des vues `listDonations` / `createDonation`.
class Donation {
  const Donation({
    required this.id,
    this.donorName,
    required this.amount,
    this.devise,
    this.donationDate = '',
    this.purpose,
    required this.isAnonymous,
    this.createdAt = '',
  });

  factory Donation.fromJson(Map<String, dynamic> json) => Donation(
        id: _s(json['id']) ?? '',
        donorName: _s(json['donorName']),
        amount: _d(json['amount']),
        devise: _s(json['devise']),
        donationDate: _s(json['donationDate']) ?? '',
        purpose: _s(json['purpose']),
        isAnonymous: _b(json['isAnonymous']),
        createdAt: _s(json['createdAt']) ?? '',
      );

  final String id;
  final String? donorName;
  final double amount;
  final String? devise;

  /// Instant serveur sérialisé, conservé brut (« amount » est le seul champ
  /// exigé par createDonation — requireDecimal).
  final String donationDate;
  final String? purpose;
  final bool isAnonymous;
  final String createdAt;

  /// Corps lu par `FinanceService.createDonation` (donationDate → Instant.parse).
  Map<String, dynamic> toJson() => <String, dynamic>{
        if (donorName != null) 'donorName': donorName,
        'amount': amount,
        if (devise != null) 'devise': devise,
        if (donationDate.isNotEmpty) 'donationDate': isoInstant(donationDate),
        if (purpose != null) 'purpose': purpose,
        'isAnonymous': isAnonymous,
      };
}
