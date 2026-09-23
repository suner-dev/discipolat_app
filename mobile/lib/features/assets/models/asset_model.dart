import 'package:flutter/material.dart';

enum AssetType {
  equipment,
  furniture,
  vehicle,
  it,
  audioVisual,
  musicalInstrument,
  medical,
  security,
  tool,
  other;

  String get displayName {
    switch (this) {
      case AssetType.equipment: return 'Équipement';
      case AssetType.furniture: return 'Mobilier';
      case AssetType.vehicle: return 'Véhicule';
      case AssetType.it: return 'Informatique';
      case AssetType.audioVisual: return 'Audio/Visuel';
      case AssetType.musicalInstrument: return 'Instrument de musique';
      case AssetType.medical: return 'Médical';
      case AssetType.security: return 'Sécurité';
      case AssetType.tool: return 'Outil';
      case AssetType.other: return 'Autre';
    }
  }

  IconData get icon {
    switch (this) {
      case AssetType.equipment: return Icons.precision_manufacturing_rounded;
      case AssetType.furniture: return Icons.chair_rounded;
      case AssetType.vehicle: return Icons.directions_car_rounded;
      case AssetType.it: return Icons.computer_rounded;
      case AssetType.audioVisual: return Icons.tv_rounded;
      case AssetType.musicalInstrument: return Icons.music_note_rounded;
      case AssetType.medical: return Icons.medical_services_rounded;
      case AssetType.security: return Icons.security_rounded;
      case AssetType.tool: return Icons.build_rounded;
      case AssetType.other: return Icons.category_rounded;
    }
  }

  String get color {
    switch (this) {
      case AssetType.equipment: return '#3B82F6';
      case AssetType.furniture: return '#8B5CF6';
      case AssetType.vehicle: return '#EF4444';
      case AssetType.it: return '#10B981';
      case AssetType.audioVisual: return '#F59E0B';
      case AssetType.musicalInstrument: return '#EC4899';
      case AssetType.medical: return '#06B6D4';
      case AssetType.security: return '#6366F1';
      case AssetType.tool: return '#84CC16';
      case AssetType.other: return '#6B7280';
    }
  }
}

enum AssetStatus {
  available,
  inUse,
  maintenance,
  repair,
  retired,
  lost,
  stolen,
  disposed;

  String get displayName {
    switch (this) {
      case AssetStatus.available: return 'Disponible';
      case AssetStatus.inUse: return 'En utilisation';
      case AssetStatus.maintenance: return 'En maintenance';
      case AssetStatus.repair: return 'En réparation';
      case AssetStatus.retired: return 'Mis au rebut';
      case AssetStatus.lost: return 'Perdu';
      case AssetStatus.stolen: return 'Volé';
      case AssetStatus.disposed: return 'Éliminé';
    }
  }

  Color get color {
    switch (this) {
      case AssetStatus.available: return Colors.green;
      case AssetStatus.inUse: return Colors.blue;
      case AssetStatus.maintenance: return Colors.orange;
      case AssetStatus.repair: return Colors.red;
      case AssetStatus.retired: return Colors.grey;
      case AssetStatus.lost: return Colors.red;
      case AssetStatus.stolen: return Colors.red;
      case AssetStatus.disposed: return Colors.grey;
    }
  }
}

class Asset {
  final int id;
  final String name;
  final String? description;
  final AssetType type;
  final AssetStatus status;
  final String? serialNumber;
  final String? barcode;
  final String? qrCode;
  final String? location;
  final String? roomName;
  final String? departmentName;
  final String? assignedToName;
  final DateTime? purchaseDate;
  final double? purchasePrice;
  final String? currency;
  final String? supplier;
  final int? warrantyMonths;
  final DateTime? warrantyExpiry;
  final String? model;
  final String? brand;
  final String? specifications;
  final String? photoUrl;
  final DateTime? lastMaintenanceDate;
  final DateTime? nextMaintenanceDate;
  final int maintenanceCount;
  final double? totalMaintenanceCost;
  final String? notes;
  final DateTime createdAt;
  final DateTime? updatedAt;

  Asset({
    required this.id,
    required this.name,
    this.description,
    required this.type,
    required this.status,
    this.serialNumber,
    this.barcode,
    this.qrCode,
    this.location,
    this.roomName,
    this.departmentName,
    this.assignedToName,
    this.purchaseDate,
    this.purchasePrice,
    this.currency,
    this.supplier,
    this.warrantyMonths,
    this.warrantyExpiry,
    this.model,
    this.brand,
    this.specifications,
    this.photoUrl,
    this.lastMaintenanceDate,
    this.nextMaintenanceDate,
    this.maintenanceCount = 0,
    this.totalMaintenanceCost,
    this.notes,
    required this.createdAt,
    this.updatedAt,
  });

  factory Asset.fromJson(Map<String, dynamic> json) {
    return Asset(
      id: json['id'] as int,
      name: json['name'] as String,
      description: json['description'] as String?,
      type: AssetType.values.firstWhere(
        (e) => e.name == json['type'],
        orElse: () => AssetType.other,
      ),
      status: AssetStatus.values.firstWhere(
        (e) => e.name == json['status'],
        orElse: () => AssetStatus.available,
      ),
      serialNumber: json['serialNumber'] as String?,
      barcode: json['barcode'] as String?,
      qrCode: json['qrCode'] as String?,
      location: json['location'] as String?,
      roomName: json['roomName'] as String?,
      departmentName: json['departmentName'] as String?,
      assignedToName: json['assignedToName'] as String?,
      purchaseDate: json['purchaseDate'] != null
          ? DateTime.parse(json['purchaseDate'] as String)
          : null,
      purchasePrice: (json['purchasePrice'] as num?)?.toDouble(),
      currency: json['currency'] as String?,
      supplier: json['supplier'] as String?,
      warrantyMonths: json['warrantyMonths'] as int?,
      warrantyExpiry: json['warrantyExpiry'] != null
          ? DateTime.parse(json['warrantyExpiry'] as String)
          : null,
      model: json['model'] as String?,
      brand: json['brand'] as String?,
      specifications: json['specifications'] as String?,
      photoUrl: json['photoUrl'] as String?,
      lastMaintenanceDate: json['lastMaintenanceDate'] != null
          ? DateTime.parse(json['lastMaintenanceDate'] as String)
          : null,
      nextMaintenanceDate: json['nextMaintenanceDate'] != null
          ? DateTime.parse(json['nextMaintenanceDate'] as String)
          : null,
      maintenanceCount: json['maintenanceCount'] as int? ?? 0,
      totalMaintenanceCost: (json['totalMaintenanceCost'] as num?)?.toDouble(),
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
      'type': type.name,
      'status': status.name,
      'serialNumber': serialNumber,
      'barcode': barcode,
      'qrCode': qrCode,
      'location': location,
      'roomName': roomName,
      'departmentName': departmentName,
      'assignedToName': assignedToName,
      'purchaseDate': purchaseDate?.toIso8601String(),
      'purchasePrice': purchasePrice,
      'currency': currency,
      'supplier': supplier,
      'warrantyMonths': warrantyMonths,
      'warrantyExpiry': warrantyExpiry?.toIso8601String(),
      'model': model,
      'brand': brand,
      'specifications': specifications,
      'photoUrl': photoUrl,
      'lastMaintenanceDate': lastMaintenanceDate?.toIso8601String(),
      'nextMaintenanceDate': nextMaintenanceDate?.toIso8601String(),
      'maintenanceCount': maintenanceCount,
      'totalMaintenanceCost': totalMaintenanceCost,
      'notes': notes,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }

  bool get needsMaintenance => nextMaintenanceDate != null && DateTime.now().isAfter(nextMaintenanceDate!);
  bool get isUnderWarranty => warrantyExpiry != null && DateTime.now().isBefore(warrantyExpiry!);
  int get ageInYears => purchaseDate != null ? (DateTime.now().year - purchaseDate!.year) : 0;
}

enum MaintenanceType {
  preventive,
  corrective,
  predictive,
  emergency,
  calibration,
  inspection;

  String get displayName {
    switch (this) {
      case MaintenanceType.preventive: return 'Préventive';
      case MaintenanceType.corrective: return 'Corrective';
      case MaintenanceType.predictive: return 'Prédictive';
      case MaintenanceType.emergency: return 'Urgence';
      case MaintenanceType.calibration: return 'Calibration';
      case MaintenanceType.inspection: return 'Inspection';
    }
  }
}

enum MaintenanceStatus {
  scheduled,
  inProgress,
  completed,
  cancelled,
  overdue;

  String get displayName {
    switch (this) {
      case MaintenanceStatus.scheduled: return 'Planifiée';
      case MaintenanceStatus.inProgress: return 'En cours';
      case MaintenanceStatus.completed: return 'Terminée';
      case MaintenanceStatus.cancelled: return 'Annulée';
      case MaintenanceStatus.overdue: return 'En retard';
    }
  }
}

class AssetMaintenance {
  final int id;
  final int assetId;
  final String assetName;
  final MaintenanceType type;
  final MaintenanceStatus status;
  final String? description;
  final String? workPerformed;
  final double? cost;
  final String? currency;
  final DateTime scheduledDate;
  final DateTime? completedDate;
  final int? technicianId;
  final String? technicianName;
  final String? partsUsed;
  final String? notes;
  final String? nextMaintenanceNotes;
  final DateTime? nextMaintenanceDate;
  final DateTime createdAt;
  final DateTime? updatedAt;

  AssetMaintenance({
    required this.id,
    required this.assetId,
    required this.assetName,
    required this.type,
    required this.status,
    this.description,
    this.workPerformed,
    this.cost,
    this.currency,
    required this.scheduledDate,
    this.completedDate,
    this.technicianId,
    this.technicianName,
    this.partsUsed,
    this.notes,
    this.nextMaintenanceNotes,
    this.nextMaintenanceDate,
    required this.createdAt,
    this.updatedAt,
  });

  factory AssetMaintenance.fromJson(Map<String, dynamic> json) {
    return AssetMaintenance(
      id: json['id'] as int,
      assetId: json['assetId'] as int,
      assetName: json['assetName'] as String,
      type: MaintenanceType.values.firstWhere(
        (e) => e.name == json['type'],
        orElse: () => MaintenanceType.corrective,
      ),
      status: MaintenanceStatus.values.firstWhere(
        (e) => e.name == json['status'],
        orElse: () => MaintenanceStatus.scheduled,
      ),
      description: json['description'] as String?,
      workPerformed: json['workPerformed'] as String?,
      cost: (json['cost'] as num?)?.toDouble(),
      currency: json['currency'] as String?,
      scheduledDate: DateTime.parse(json['scheduledDate'] as String),
      completedDate: json['completedDate'] != null
          ? DateTime.parse(json['completedDate'] as String)
          : null,
      technicianId: json['technicianId'] as int?,
      technicianName: json['technicianName'] as String?,
      partsUsed: json['partsUsed'] as String?,
      notes: json['notes'] as String?,
      nextMaintenanceNotes: json['nextMaintenanceNotes'] as String?,
      nextMaintenanceDate: json['nextMaintenanceDate'] != null
          ? DateTime.parse(json['nextMaintenanceDate'] as String)
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
      'type': type.name,
      'status': status.name,
      'description': description,
      'workPerformed': workPerformed,
      'cost': cost,
      'currency': currency,
      'scheduledDate': scheduledDate.toIso8601String(),
      'completedDate': completedDate?.toIso8601String(),
      'technicianId': technicianId,
      'technicianName': technicianName,
      'partsUsed': partsUsed,
      'notes': notes,
      'nextMaintenanceNotes': nextMaintenanceNotes,
      'nextMaintenanceDate': nextMaintenanceDate?.toIso8601String(),
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }
}

enum CheckoutStatus {
  pending,
  approved,
  checkedOut,
  returned,
  overdue,
  cancelled;

  String get displayName {
    switch (this) {
      case CheckoutStatus.pending: return 'En attente';
      case CheckoutStatus.approved: return 'Approuvé';
      case CheckoutStatus.checkedOut: return 'Emprunté';
      case CheckoutStatus.returned: return 'Retourné';
      case CheckoutStatus.overdue: return 'En retard';
      case CheckoutStatus.cancelled: return 'Annulé';
    }
  }
}

class AssetCheckout {
  final int id;
  final int assetId;
  final String assetName;
  final int checkedOutById;
  final String checkedOutByName;
  final DateTime checkoutDate;
  final DateTime? expectedReturnDate;
  final DateTime? actualReturnDate;
  final CheckoutStatus status;
  final String? purpose;
  final String? location;
  final String? conditionOnCheckout;
  final String? conditionOnReturn;
  final String? notes;
  final String? approvedBy;
  final DateTime createdAt;
  final DateTime? updatedAt;

  AssetCheckout({
    required this.id,
    required this.assetId,
    required this.assetName,
    required this.checkedOutById,
    required this.checkedOutByName,
    required this.checkoutDate,
    this.expectedReturnDate,
    this.actualReturnDate,
    required this.status,
    this.purpose,
    this.location,
    this.conditionOnCheckout,
    this.conditionOnReturn,
    this.notes,
    this.approvedBy,
    required this.createdAt,
    this.updatedAt,
  });

  factory AssetCheckout.fromJson(Map<String, dynamic> json) {
    return AssetCheckout(
      id: json['id'] as int,
      assetId: json['assetId'] as int,
      assetName: json['assetName'] as String,
      checkedOutById: json['checkedOutById'] as int,
      checkedOutByName: json['checkedOutByName'] as String,
      checkoutDate: DateTime.parse(json['checkoutDate'] as String),
      expectedReturnDate: json['expectedReturnDate'] != null
          ? DateTime.parse(json['expectedReturnDate'] as String)
          : null,
      actualReturnDate: json['actualReturnDate'] != null
          ? DateTime.parse(json['actualReturnDate'] as String)
          : null,
      status: CheckoutStatus.values.firstWhere(
        (e) => e.name == json['status'],
        orElse: () => CheckoutStatus.pending,
      ),
      purpose: json['purpose'] as String?,
      location: json['location'] as String?,
      conditionOnCheckout: json['conditionOnCheckout'] as String?,
      conditionOnReturn: json['conditionOnReturn'] as String?,
      notes: json['notes'] as String?,
      approvedBy: json['approvedBy'] as String?,
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
      'checkedOutById': checkedOutById,
      'checkedOutByName': checkedOutByName,
      'checkoutDate': checkoutDate.toIso8601String(),
      'expectedReturnDate': expectedReturnDate?.toIso8601String(),
      'actualReturnDate': actualReturnDate?.toIso8601String(),
      'status': status.name,
      'purpose': purpose,
      'location': location,
      'conditionOnCheckout': conditionOnCheckout,
      'conditionOnReturn': conditionOnReturn,
      'notes': notes,
      'approvedBy': approvedBy,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }

  bool get isOverdue =>
      expectedReturnDate != null &&
      DateTime.now().isAfter(expectedReturnDate!) &&
      status == CheckoutStatus.checkedOut;
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
