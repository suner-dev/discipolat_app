import 'package:flutter/material.dart';

/// Catégories d'inventaire telles qu'exposées par le serveur
/// (`InventoryItem.categorie`, colonne `categorie`) : MATERIEL, MOBILIER,
/// TECHNIQUE, VESTIMENTAIRE, AUTRE. Les membres Dart portent la chaîne exacte
/// reçue en JSON via [serverValue] ; [fromServer] décode sans crasher une valeur
/// inconnue (repli sur [AssetType.autre]).
enum AssetType {
  materiel('MATERIEL'),
  mobilier('MOBILIER'),
  technique('TECHNIQUE'),
  vestimentaire('VESTIMENTAIRE'),
  autre('AUTRE');

  final String serverValue;
  const AssetType(this.serverValue);

  static AssetType fromServer(Object? raw) => AssetType.values.firstWhere(
        (e) => e.serverValue == raw,
        orElse: () => AssetType.autre,
      );

  String get displayName {
    switch (this) {
      case AssetType.materiel: return 'Matériel';
      case AssetType.mobilier: return 'Mobilier';
      case AssetType.technique: return 'Technique';
      case AssetType.vestimentaire: return 'Vestimentaire';
      case AssetType.autre: return 'Autre';
    }
  }

  IconData get icon {
    switch (this) {
      case AssetType.materiel: return Icons.precision_manufacturing_rounded;
      case AssetType.mobilier: return Icons.chair_rounded;
      case AssetType.technique: return Icons.computer_rounded;
      case AssetType.vestimentaire: return Icons.checkroom_rounded;
      case AssetType.autre: return Icons.category_rounded;
    }
  }

  String get color {
    switch (this) {
      case AssetType.materiel: return '#3B82F6';
      case AssetType.mobilier: return '#8B5CF6';
      case AssetType.technique: return '#10B981';
      case AssetType.vestimentaire: return '#EC4899';
      case AssetType.autre: return '#6B7280';
    }
  }
}

/// Statuts d'inventaire tels qu'exposés par le serveur
/// (`InventoryItem.statut`, colonne `statut`) : DISPONIBLE, AFFECTE,
/// EN_MAINTENANCE, PERDU, RETIRE. Même décodage tolérant que [AssetType].
enum AssetStatus {
  disponible('DISPONIBLE'),
  affecte('AFFECTE'),
  enMaintenance('EN_MAINTENANCE'),
  perdu('PERDU'),
  retire('RETIRE');

  final String serverValue;
  const AssetStatus(this.serverValue);

  static AssetStatus fromServer(Object? raw) => AssetStatus.values.firstWhere(
        (e) => e.serverValue == raw,
        orElse: () => AssetStatus.disponible,
      );

  String get displayName {
    switch (this) {
      case AssetStatus.disponible: return 'Disponible';
      case AssetStatus.affecte: return 'Affecté';
      case AssetStatus.enMaintenance: return 'En maintenance';
      case AssetStatus.perdu: return 'Perdu';
      case AssetStatus.retire: return 'Retiré';
    }
  }

  Color get color {
    switch (this) {
      case AssetStatus.disponible: return Colors.green;
      case AssetStatus.affecte: return Colors.blue;
      case AssetStatus.enMaintenance: return Colors.orange;
      case AssetStatus.perdu: return Colors.red;
      case AssetStatus.retire: return Colors.grey;
    }
  }
}

/// Vue mobile d'un objet d'inventaire — miroir exact de l'entité serveur
/// `InventoryItem` renvoyée par `GET /inventory` (Page `<InventoryItem>`,
/// `.content`) et `GET /inventory/{id}`.
///
/// Points vérifiés sur le serveur (pas supposés) :
/// - `id`/`tenantId`/`departementId`/`affecteAId` sont des **UUID** : le cast
///   Dart doit rester `String` (un `as int` plantait sur la valeur réelle).
/// - les champs sont en français (`nom`, `categorie`, `statut`, `lieuStockage`,
///   `quantiteDisponible`…) et non l'ancien vocabulaire anglais fantôme.
/// - les dates sont des `LocalDateTime` sérialisées ISO‑8601 (`createdAt`
///   always, le reste nullable).
class Asset {
  final String id;
  final String tenantId;
  final String nom;
  final String? description;
  final AssetType categorie;
  final AssetStatus statut;
  final int quantite;
  final int quantiteDisponible;
  final double? valeurUnitaire;
  final String? lieuStockage;
  final String? numeroSerie;
  final DateTime? dateAcquisition;
  final DateTime? derniereMaintenance;
  final DateTime? prochaineMaintenance;
  final String? departementId;
  final String? affecteAId;
  final double totalMaintenanceCost;
  final int totalCheckoutCount;
  final double? purchasePrice;
  final int? expectedLifespanMonths;
  final String? notes;
  final String? qrToken;
  final DateTime createdAt;
  final DateTime? updatedAt;

  const Asset({
    required this.id,
    required this.tenantId,
    required this.nom,
    this.description,
    required this.categorie,
    required this.statut,
    this.quantite = 0,
    this.quantiteDisponible = 0,
    this.valeurUnitaire,
    this.lieuStockage,
    this.numeroSerie,
    this.dateAcquisition,
    this.derniereMaintenance,
    this.prochaineMaintenance,
    this.departementId,
    this.affecteAId,
    this.totalMaintenanceCost = 0,
    this.totalCheckoutCount = 0,
    this.purchasePrice,
    this.expectedLifespanMonths,
    this.notes,
    this.qrToken,
    required this.createdAt,
    this.updatedAt,
  });

  factory Asset.fromJson(Map<String, dynamic> json) {
    return Asset(
      id: json['id'].toString(),
      tenantId: json['tenantId']?.toString() ?? '',
      nom: (json['nom'] ?? '') as String,
      description: json['description'] as String?,
      categorie: AssetType.fromServer(json['categorie']),
      statut: AssetStatus.fromServer(json['statut']),
      quantite: (json['quantite'] as num?)?.toInt() ?? 0,
      quantiteDisponible: (json['quantiteDisponible'] as num?)?.toInt() ?? 0,
      valeurUnitaire: (json['valeurUnitaire'] as num?)?.toDouble(),
      lieuStockage: json['lieuStockage'] as String?,
      numeroSerie: json['numeroSerie'] as String?,
      dateAcquisition: _parseDate(json['dateAcquisition']),
      derniereMaintenance: _parseDate(json['derniereMaintenance']),
      prochaineMaintenance: _parseDate(json['prochaineMaintenance']),
      departementId: json['departementId']?.toString(),
      affecteAId: json['affecteAId']?.toString(),
      totalMaintenanceCost:
          (json['totalMaintenanceCost'] as num?)?.toDouble() ?? 0,
      totalCheckoutCount: (json['totalCheckoutCount'] as num?)?.toInt() ?? 0,
      purchasePrice: (json['purchasePrice'] as num?)?.toDouble(),
      expectedLifespanMonths:
          (json['expectedLifespanMonths'] as num?)?.toInt(),
      notes: json['notes'] as String?,
      qrToken: json['qrToken'] as String?,
      createdAt: _parseDate(json['createdAt']) ?? DateTime.now(),
      updatedAt: _parseDate(json['updatedAt']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'tenantId': tenantId,
      'nom': nom,
      'description': description,
      'categorie': categorie.serverValue,
      'statut': statut.serverValue,
      'quantite': quantite,
      'quantiteDisponible': quantiteDisponible,
      'valeurUnitaire': valeurUnitaire,
      'lieuStockage': lieuStockage,
      'numeroSerie': numeroSerie,
      'dateAcquisition': dateAcquisition?.toIso8601String(),
      'derniereMaintenance': derniereMaintenance?.toIso8601String(),
      'prochaineMaintenance': prochaineMaintenance?.toIso8601String(),
      'departementId': departementId,
      'affecteAId': affecteAId,
      'totalMaintenanceCost': totalMaintenanceCost,
      'totalCheckoutCount': totalCheckoutCount,
      'purchasePrice': purchasePrice,
      'expectedLifespanMonths': expectedLifespanMonths,
      'notes': notes,
      'qrToken': qrToken,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }

  /// Le serveur expose `prochaineMaintenance` : maintenance « requise » dès que
  /// la date prévue est dépassée.
  bool get needsMaintenance =>
      prochaineMaintenance != null &&
      DateTime.now().isAfter(prochaineMaintenance!);

  /// Objet réellement empruntable : statut disponible ET stock résiduel > 0.
  bool get isAvailable =>
      statut == AssetStatus.disponible && quantiteDisponible > 0;

  /// Corps d'écriture (`POST`/`PUT /inventory`) — le serveur force
  /// id/tenantId/createdAt et dérive le tenant du JWT : on ne renvoie que les
  /// champs éditables.
  Map<String, dynamic> writeBody() => {
        'nom': nom,
        'description': description,
        'categorie': categorie.serverValue,
        'statut': statut.serverValue,
        'quantite': quantite,
        'quantiteDisponible': quantiteDisponible,
        'valeurUnitaire': valeurUnitaire,
        'lieuStockage': lieuStockage,
        'numeroSerie': numeroSerie,
        'departementId': departementId,
        'affecteAId': affecteAId,
        'purchasePrice': purchasePrice,
        'expectedLifespanMonths': expectedLifespanMonths,
        'notes': notes,
      };
}

/// Parse tolérant une date serveur (`LocalDateTime` ISO‑8601). Renvoie null si
/// absente ou illisible — jamais de crash sur un champ optionnel.
DateTime? _parseDate(Object? raw) {
  if (raw == null) return null;
  if (raw is DateTime) return raw;
  return DateTime.tryParse(raw.toString());
}

/// Types de maintenance serveur (`AssetMaintenance.maintenanceType`) :
/// PREVENTIVE, CORRECTIVE, INSPECTION, REPAIR, UPGRADE.
enum MaintenanceType {
  preventive('PREVENTIVE'),
  corrective('CORRECTIVE'),
  inspection('INSPECTION'),
  repair('REPAIR'),
  upgrade('UPGRADE');

  final String serverValue;
  const MaintenanceType(this.serverValue);

  static MaintenanceType fromServer(Object? raw) =>
      MaintenanceType.values.firstWhere(
        (e) => e.serverValue == raw,
        orElse: () => MaintenanceType.corrective,
      );

  String get displayName {
    switch (this) {
      case MaintenanceType.preventive: return 'Préventive';
      case MaintenanceType.corrective: return 'Corrective';
      case MaintenanceType.inspection: return 'Inspection';
      case MaintenanceType.repair: return 'Réparation';
      case MaintenanceType.upgrade: return 'Amélioration';
    }
  }
}

/// Statuts de maintenance serveur (`AssetMaintenance.status`) :
/// SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED.
enum MaintenanceStatus {
  scheduled('SCHEDULED'),
  inProgress('IN_PROGRESS'),
  completed('COMPLETED'),
  cancelled('CANCELLED');

  final String serverValue;
  const MaintenanceStatus(this.serverValue);

  static MaintenanceStatus fromServer(Object? raw) =>
      MaintenanceStatus.values.firstWhere(
        (e) => e.serverValue == raw,
        orElse: () => MaintenanceStatus.scheduled,
      );

  String get displayName {
    switch (this) {
      case MaintenanceStatus.scheduled: return 'Planifiée';
      case MaintenanceStatus.inProgress: return 'En cours';
      case MaintenanceStatus.completed: return 'Terminée';
      case MaintenanceStatus.cancelled: return 'Annulée';
    }
  }
}

/// Historique de maintenance d'un objet — miroir de l'entité serveur
/// `AssetMaintenance` renvoyée par `GET /assets/{itemId}/maintenance`.
/// `id`/`itemId` sont des UUID String ; il n'existe ni `assetName` ni
/// `updatedAt` côté serveur (le nom de l'objet vient de la fiche `InventoryItem`).
class AssetMaintenance {
  final String id;
  final String itemId;
  final MaintenanceType maintenanceType;
  final MaintenanceStatus status;
  final String title;
  final String? description;
  final String? performedBy;
  final String? vendorName;
  final String? vendorContact;
  final double? cost;
  final String? currency;
  final DateTime? scheduledFor;
  final DateTime? startedAt;
  final DateTime? completedAt;
  final DateTime? nextMaintenanceDue;
  final String? partsReplaced;
  final String? notes;
  final DateTime createdAt;

  const AssetMaintenance({
    required this.id,
    required this.itemId,
    required this.maintenanceType,
    required this.status,
    required this.title,
    this.description,
    this.performedBy,
    this.vendorName,
    this.vendorContact,
    this.cost,
    this.currency,
    this.scheduledFor,
    this.startedAt,
    this.completedAt,
    this.nextMaintenanceDue,
    this.partsReplaced,
    this.notes,
    required this.createdAt,
  });

  factory AssetMaintenance.fromJson(Map<String, dynamic> json) {
    return AssetMaintenance(
      id: json['id'].toString(),
      itemId: json['itemId'].toString(),
      maintenanceType: MaintenanceType.fromServer(json['maintenanceType']),
      status: MaintenanceStatus.fromServer(json['status']),
      title: (json['title'] ?? '') as String,
      description: json['description'] as String?,
      performedBy: json['performedBy']?.toString(),
      vendorName: json['vendorName'] as String?,
      vendorContact: json['vendorContact'] as String?,
      cost: (json['cost'] as num?)?.toDouble(),
      currency: json['currency'] as String?,
      scheduledFor: _parseDate(json['scheduledFor']),
      startedAt: _parseDate(json['startedAt']),
      completedAt: _parseDate(json['completedAt']),
      nextMaintenanceDue: _parseDate(json['nextMaintenanceDue']),
      partsReplaced: json['partsReplaced'] as String?,
      notes: json['notes'] as String?,
      createdAt: _parseDate(json['createdAt']) ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'itemId': itemId,
      'maintenanceType': maintenanceType.serverValue,
      'status': status.serverValue,
      'title': title,
      'description': description,
      'performedBy': performedBy,
      'vendorName': vendorName,
      'vendorContact': vendorContact,
      'cost': cost,
      'currency': currency,
      'scheduledFor': scheduledFor?.toIso8601String(),
      'startedAt': startedAt?.toIso8601String(),
      'completedAt': completedAt?.toIso8601String(),
      'nextMaintenanceDue': nextMaintenanceDue?.toIso8601String(),
      'partsReplaced': partsReplaced,
      'notes': notes,
      'createdAt': createdAt.toIso8601String(),
    };
  }
}

/// Statuts de prêt serveur (`AssetCheckout.status`) :
/// CHECKED_OUT, RETURNED, OVERDUE, DAMAGED, LOST.
enum CheckoutStatus {
  checkedOut('CHECKED_OUT'),
  returned('RETURNED'),
  overdue('OVERDUE'),
  damaged('DAMAGED'),
  lost('LOST');

  final String serverValue;
  const CheckoutStatus(this.serverValue);

  static CheckoutStatus fromServer(Object? raw) =>
      CheckoutStatus.values.firstWhere(
        (e) => e.serverValue == raw,
        orElse: () => CheckoutStatus.checkedOut,
      );

  String get displayName {
    switch (this) {
      case CheckoutStatus.checkedOut: return 'Emprunté';
      case CheckoutStatus.returned: return 'Retourné';
      case CheckoutStatus.overdue: return 'En retard';
      case CheckoutStatus.damaged: return 'Endommagé';
      case CheckoutStatus.lost: return 'Perdu';
    }
  }
}

/// Prêt d'un objet — miroir de l'entité serveur `AssetCheckout` renvoyée par
/// `GET /assets/{itemId}/checkouts`. UUID String partout ; les dates serveur
/// sont `checkedOutAt`/`dueBackAt`/`returnedAt` (et non « checkoutDate »,
/// `assetName`/`purpose`/`location`/`approvedBy` n'existent pas côté serveur).
class AssetCheckout {
  final String id;
  final String itemId;
  final String memberId;
  final String? spaceId;
  final String? eventId;
  final DateTime checkedOutAt;
  final DateTime? dueBackAt;
  final DateTime? returnedAt;
  final CheckoutStatus status;
  final String? conditionOnCheckout;
  final String? conditionOnReturn;
  final String? checkedOutBy;
  final String? returnedBy;
  final String? notes;
  final String? damagePhotoPath;
  final DateTime createdAt;

  const AssetCheckout({
    required this.id,
    required this.itemId,
    required this.memberId,
    this.spaceId,
    this.eventId,
    required this.checkedOutAt,
    this.dueBackAt,
    this.returnedAt,
    required this.status,
    this.conditionOnCheckout,
    this.conditionOnReturn,
    this.checkedOutBy,
    this.returnedBy,
    this.notes,
    this.damagePhotoPath,
    required this.createdAt,
  });

  factory AssetCheckout.fromJson(Map<String, dynamic> json) {
    return AssetCheckout(
      id: json['id'].toString(),
      itemId: json['itemId'].toString(),
      memberId: json['memberId'].toString(),
      spaceId: json['spaceId']?.toString(),
      eventId: json['eventId']?.toString(),
      checkedOutAt: _parseDate(json['checkedOutAt']) ?? DateTime.now(),
      dueBackAt: _parseDate(json['dueBackAt']),
      returnedAt: _parseDate(json['returnedAt']),
      status: CheckoutStatus.fromServer(json['status']),
      conditionOnCheckout: json['conditionOnCheckout'] as String?,
      conditionOnReturn: json['conditionOnReturn'] as String?,
      checkedOutBy: json['checkedOutBy']?.toString(),
      returnedBy: json['returnedBy']?.toString(),
      notes: json['notes'] as String?,
      damagePhotoPath: json['damagePhotoPath'] as String?,
      createdAt: _parseDate(json['createdAt']) ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'itemId': itemId,
      'memberId': memberId,
      'spaceId': spaceId,
      'eventId': eventId,
      'checkedOutAt': checkedOutAt.toIso8601String(),
      'dueBackAt': dueBackAt?.toIso8601String(),
      'returnedAt': returnedAt?.toIso8601String(),
      'status': status.serverValue,
      'conditionOnCheckout': conditionOnCheckout,
      'conditionOnReturn': conditionOnReturn,
      'checkedOutBy': checkedOutBy,
      'returnedBy': returnedBy,
      'notes': notes,
      'damagePhotoPath': damagePhotoPath,
      'createdAt': createdAt.toIso8601String(),
    };
  }

  /// En retard = toujours sorti ET date de retour attendue dépassée.
  bool get isOverdue =>
      status == CheckoutStatus.checkedOut &&
      dueBackAt != null &&
      DateTime.now().isAfter(dueBackAt!);
}

enum TransferStatus {
  pending,
  approved,
  inTransit,
  completed,
  rejected,
  cancelled;

  String get displayName {
    switch (this) {
      case TransferStatus.pending: return 'En attente';
      case TransferStatus.approved: return 'Approuvé';
      case TransferStatus.inTransit: return 'En transit';
      case TransferStatus.completed: return 'Terminé';
      case TransferStatus.rejected: return 'Rejeté';
      case TransferStatus.cancelled: return 'Annulé';
    }
  }
}

class AssetTransfer {
  final int id;
  final int assetId;
  final String assetName;
  final int fromLocationId;
  final String? fromLocationName;
  final int toLocationId;
  final String? toLocationName;
  final int requestedById;
  final String? requestedByName;
  final DateTime requestedDate;
  final DateTime? transferDate;
  final TransferStatus status;
  final String? reason;
  final String? notes;
  final int? approvedById;
  final String? approvedByName;
  final DateTime? approvedDate;
  final DateTime createdAt;
  final DateTime? updatedAt;

  AssetTransfer({
    required this.id,
    required this.assetId,
    required this.assetName,
    required this.fromLocationId,
    this.fromLocationName,
    required this.toLocationId,
    this.toLocationName,
    required this.requestedById,
    this.requestedByName,
    required this.requestedDate,
    this.transferDate,
    required this.status,
    this.reason,
    this.notes,
    this.approvedById,
    this.approvedByName,
    this.approvedDate,
    required this.createdAt,
    this.updatedAt,
  });

  factory AssetTransfer.fromJson(Map<String, dynamic> json) {
    return AssetTransfer(
      id: json['id'] as int,
      assetId: json['assetId'] as int,
      assetName: json['assetName'] as String,
      fromLocationId: json['fromLocationId'] as int,
      fromLocationName: json['fromLocationName'] as String?,
      toLocationId: json['toLocationId'] as int,
      toLocationName: json['toLocationName'] as String?,
      requestedById: json['requestedById'] as int,
      requestedByName: json['requestedByName'] as String?,
      requestedDate: DateTime.parse(json['requestedDate'] as String),
      transferDate: json['transferDate'] != null
          ? DateTime.parse(json['transferDate'] as String)
          : null,
      status: TransferStatus.values.firstWhere(
        (e) => e.name == json['status'],
        orElse: () => TransferStatus.pending,
      ),
      reason: json['reason'] as String?,
      notes: json['notes'] as String?,
      approvedById: json['approvedById'] as int?,
      approvedByName: json['approvedByName'] as String?,
      approvedDate: json['approvedDate'] != null
          ? DateTime.parse(json['approvedDate'] as String)
          : null,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'assetId': assetId,
      'assetName': assetName,
      'fromLocationId': fromLocationId,
      'fromLocationName': fromLocationName,
      'toLocationId': toLocationId,
      'toLocationName': toLocationName,
      'requestedById': requestedById,
      'requestedByName': requestedByName,
      'requestedDate': requestedDate.toIso8601String(),
      'transferDate': transferDate?.toIso8601String(),
      'status': status.name,
      'reason': reason,
      'notes': notes,
      'approvedById': approvedById,
      'approvedByName': approvedByName,
      'approvedDate': approvedDate?.toIso8601String(),
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }
}

enum ItemCategory {
  consumable,
  officeSupply,
  cleaning,
  foodBeverage,
  medicalSupply,
  safety,
  tool,
  other;

  String get displayName {
    switch (this) {
      case ItemCategory.consumable: return 'Consommable';
      case ItemCategory.officeSupply: return 'Fourniture bureau';
      case ItemCategory.cleaning: return 'Nettoyage';
      case ItemCategory.foodBeverage: return 'Alimentaire';
      case ItemCategory.medicalSupply: return 'Fourniture médicale';
      case ItemCategory.safety: return 'Sécurité';
      case ItemCategory.tool: return 'Outil';
      case ItemCategory.other: return 'Autre';
    }
  }
}

enum ItemUnit {
  unit,
  box,
  pack,
  roll,
  bottle,
  can,
  kg,
  liter,
  meter,
  set;

  String get displayName {
    switch (this) {
      case ItemUnit.unit: return 'Unité';
      case ItemUnit.box: return 'Boîte';
      case ItemUnit.pack: return 'Paquet';
      case ItemUnit.roll: return 'Rouleau';
      case ItemUnit.bottle: return 'Bouteille';
      case ItemUnit.can: return 'Canette';
      case ItemUnit.kg: return 'Kg';
      case ItemUnit.liter: return 'Litre';
      case ItemUnit.meter: return 'Mètre';
      case ItemUnit.set: return 'Set';
    }
  }
}

class InventoryItem {
  final int id;
  final String name;
  final String? description;
  final String sku;
  final String? barcode;
  final ItemCategory category;
  final ItemUnit unit;
  final int currentStock;
  final int minStockLevel;
  final int maxStockLevel;
  final String location;
  final double? unitCost;
  final double? sellingPrice;
  final String? currency;
  final String? supplier;
  final DateTime? lastRestockDate;
  final DateTime? expiryDate;
  final int? batchNumber;
  final int? reservedQuantity;
  final bool isActive;
  final int reorderPoint;
  final String? notes;
  final DateTime createdAt;
  final DateTime? updatedAt;

  InventoryItem({
    required this.id,
    required this.name,
    this.description,
    required this.sku,
    this.barcode,
    required this.category,
    required this.unit,
    required this.currentStock,
    required this.minStockLevel,
    required this.maxStockLevel,
    required this.location,
    this.unitCost,
    this.sellingPrice,
    this.currency,
    this.supplier,
    this.lastRestockDate,
    this.expiryDate,
    this.batchNumber,
    this.reservedQuantity,
    this.isActive = true,
    this.reorderPoint = 0,
    this.notes,
    required this.createdAt,
    this.updatedAt,
  });

  factory InventoryItem.fromJson(Map<String, dynamic> json) {
    return InventoryItem(
      id: json['id'] as int,
      name: json['name'] as String,
      description: json['description'] as String?,
      sku: json['sku'] as String,
      barcode: json['barcode'] as String?,
      category: ItemCategory.values.firstWhere(
        (e) => e.name == json['category'],
        orElse: () => ItemCategory.other,
      ),
      unit: ItemUnit.values.firstWhere(
        (e) => e.name == json['unit'],
        orElse: () => ItemUnit.unit,
      ),
      currentStock: json['currentStock'] as int,
      minStockLevel: json['minStockLevel'] as int,
      maxStockLevel: json['maxStockLevel'] as int,
      location: json['location'] as String,
      unitCost: (json['unitCost'] as num?)?.toDouble(),
      sellingPrice: (json['sellingPrice'] as num?)?.toDouble(),
      currency: json['currency'] as String?,
      supplier: json['supplier'] as String?,
      lastRestockDate: json['lastRestockDate'] != null
          ? DateTime.parse(json['lastRestockDate'] as String)
          : null,
      expiryDate: json['expiryDate'] != null
          ? DateTime.parse(json['expiryDate'] as String)
          : null,
      batchNumber: json['batchNumber'] as int?,
      reservedQuantity: json['reservedQuantity'] as int?,
      isActive: json['isActive'] as bool? ?? true,
      reorderPoint: json['reorderPoint'] as int? ?? 0,
      notes: json['notes'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'sku': sku,
      'barcode': barcode,
      'category': category.name,
      'unit': unit.name,
      'currentStock': currentStock,
      'minStockLevel': minStockLevel,
      'maxStockLevel': maxStockLevel,
      'location': location,
      'unitCost': unitCost,
      'sellingPrice': sellingPrice,
      'currency': currency,
      'supplier': supplier,
      'lastRestockDate': lastRestockDate?.toIso8601String(),
      'expiryDate': expiryDate?.toIso8601String(),
      'batchNumber': batchNumber,
      'reservedQuantity': reservedQuantity,
      'isActive': isActive,
      'reorderPoint': reorderPoint,
      'notes': notes,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }

  bool get isLowStock => currentStock <= minStockLevel;
  bool get isOverstocked => currentStock >= maxStockLevel;
  int get availableQuantity => currentStock - (reservedQuantity ?? 0);
  bool get needsReorder => currentStock <= (reorderPoint ?? minStockLevel);
}

class StockMovement {
  final int id;
  final int itemId;
  final String itemName;
  final String type;
  final int quantity;
  final int? previousStock;
  final int? newStock;
  final String? reason;
  final String? reference;
  final int? performedById;
  final String? performedByName;
  final DateTime date;
  final String status;
  final String? notes;
  final DateTime createdAt;

  StockMovement({
    required this.id,
    required this.itemId,
    required this.itemName,
    required this.type,
    required this.quantity,
    this.previousStock,
    this.newStock,
    this.reason,
    this.reference,
    this.performedById,
    this.performedByName,
    required this.date,
    required this.status,
    this.notes,
    required this.createdAt,
  });

  factory StockMovement.fromJson(Map<String, dynamic> json) {
    return StockMovement(
      id: json['id'] as int,
      itemId: json['itemId'] as int,
      itemName: json['itemName'] as String,
      type: json['type'] as String,
      quantity: json['quantity'] as int,
      previousStock: json['previousStock'] as int?,
      newStock: json['newStock'] as int?,
      reason: json['reason'] as String?,
      reference: json['reference'] as String?,
      performedById: json['performedById'] as int?,
      performedByName: json['performedByName'] as String?,
      date: DateTime.parse(json['date'] as String),
      status: json['status'] as String,
      notes: json['notes'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'itemId': itemId,
      'itemName': itemName,
      'type': type,
      'quantity': quantity,
      'previousStock': previousStock,
      'newStock': newStock,
      'reason': reason,
      'reference': reference,
      'performedById': performedById,
      'performedByName': performedByName,
      'date': date.toIso8601String(),
      'status': status,
      'notes': notes,
      'createdAt': createdAt.toIso8601String(),
    };
  }
}

enum InsuranceStatus {
  active,
  expired,
  cancelled,
  pending;

  String get displayName {
    switch (this) {
      case InsuranceStatus.active: return 'Active';
      case InsuranceStatus.expired: return 'Expirée';
      case InsuranceStatus.cancelled: return 'Annulée';
      case InsuranceStatus.pending: return 'En attente';
    }
  }
}

class AssetInsurance {
  final int id;
  final int assetId;
  final String assetName;
  final String provider;
  final String policyNumber;
  final double coverageAmount;
  final String? currency;
  final DateTime startDate;
  final DateTime endDate;
  final double? premium;
  final String? premiumFrequency;
  final String? coverageDetails;
  final InsuranceStatus status;
  final String? policyDocumentUrl;
  final String? notes;
  final DateTime createdAt;
  final DateTime? updatedAt;

  AssetInsurance({
    required this.id,
    required this.assetId,
    required this.assetName,
    required this.provider,
    required this.policyNumber,
    required this.coverageAmount,
    this.currency,
    required this.startDate,
    required this.endDate,
    this.premium,
    this.premiumFrequency,
    this.coverageDetails,
    required this.status,
    this.policyDocumentUrl,
    this.notes,
    required this.createdAt,
    this.updatedAt,
  });

  factory AssetInsurance.fromJson(Map<String, dynamic> json) {
    return AssetInsurance(
      id: json['id'] as int,
      assetId: json['assetId'] as int,
      assetName: json['assetName'] as String,
      provider: json['provider'] as String,
      policyNumber: json['policyNumber'] as String,
      coverageAmount: (json['coverageAmount'] as num).toDouble(),
      currency: json['currency'] as String?,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      premium: (json['premium'] as num?)?.toDouble(),
      premiumFrequency: json['premiumFrequency'] as String?,
      coverageDetails: json['coverageDetails'] as String?,
      status: InsuranceStatus.values.firstWhere(
        (e) => e.name == json['status'],
        orElse: () => InsuranceStatus.active,
      ),
      policyDocumentUrl: json['policyDocumentUrl'] as String?,
      notes: json['notes'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'assetId': assetId,
      'assetName': assetName,
      'provider': provider,
      'policyNumber': policyNumber,
      'coverageAmount': coverageAmount,
      'currency': currency,
      'startDate': startDate.toIso8601String(),
      'endDate': endDate.toIso8601String(),
      'premium': premium,
      'premiumFrequency': premiumFrequency,
      'coverageDetails': coverageDetails,
      'status': status.name,
      'policyDocumentUrl': policyDocumentUrl,
      'notes': notes,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }
}
