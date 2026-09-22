import 'package:freezed_annotation/freezed_annotation.dart';

part 'asset_model.freezed.dart';
part 'asset_model.g.dart';

@freezed
class Asset with _$Asset {
  const factory Asset({
    required int id,
    required String name,
    String? description,
    required AssetType type,
    required AssetStatus status,
    String? serialNumber,
    String? barcode,
    String? qrCode,
    String? location,
    int? roomId,
    String? roomName,
    int? departmentId,
    String? departmentName,
    int? assignedToId,
    String? assignedToName,
    DateTime? purchaseDate,
    double? purchasePrice,
    String? currency,
    String? supplier,
    int? warrantyMonths,
    DateTime? warrantyExpiry,
    String? model,
    String? brand,
    String? specifications,
    String? photoUrl,
    DateTime? lastMaintenanceDate,
    DateTime? nextMaintenanceDate,
    @Default(0) int maintenanceCount,
    double? totalMaintenanceCost,
    String? notes,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _Asset;

  factory Asset.fromJson(Map<String, dynamic> json) => _$AssetFromJson(json);

  bool get needsMaintenance => nextMaintenanceDate != null && DateTime.now().isAfter(nextMaintenanceDate!);
  bool get isUnderWarranty => warrantyExpiry != null && DateTime.now().isBefore(warrantyExpiry!);
  int get ageInYears => purchaseDate != null ? DateTime.now().year - purchaseDate!.year : 0;
}

@freezed
class AssetMaintenance with _$AssetMaintenance {
  const factory AssetMaintenance({
    required int id,
    required int assetId,
    required String assetName,
    required MaintenanceType type,
    required MaintenanceStatus status,
    String? description,
    String? workPerformed,
    double? cost,
    String? currency,
    required DateTime scheduledDate,
    DateTime? completedDate,
    int? technicianId,
    String? technicianName,
    String? partsUsed,
    String? notes,
    String? nextMaintenanceNotes,
    DateTime? nextMaintenanceDate,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _AssetMaintenance;

  factory AssetMaintenance.fromJson(Map<String, dynamic> json) => _$AssetMaintenanceFromJson(json);
}

@freezed
class AssetCheckout with _$AssetCheckout {
  const factory AssetCheckout({
    required int id,
    required int assetId,
    required String assetName,
    required int checkedOutById,
    required String checkedOutByName,
    required DateTime checkoutDate,
    DateTime? expectedReturnDate,
    DateTime? actualReturnDate,
    CheckoutStatus status,
    String? purpose,
    String? location,
    String? conditionOnCheckout,
    String? conditionOnReturn,
    String? notes,
    String? approvedBy,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _AssetCheckout;

  factory AssetCheckout.fromJson(Map<String, dynamic> json) => _$AssetCheckoutFromJson(json);

  bool get isOverdue => expectedReturnDate != null && DateTime.now().isAfter(expectedReturnDate!) && status == CheckoutStatus.checkedOut;
}

@freezed
class AssetTransfer with _$AssetTransfer {
  const factory AssetTransfer({
    required int id,
    required int assetId,
    required String assetName,
    required int fromLocationId,
    String? fromLocationName,
    required int toLocationId,
    String? toLocationName,
    required int requestedById,
    String? requestedByName,
    required DateTime requestedDate,
    DateTime? transferDate,
    TransferStatus status,
    String? reason,
    String? notes,
    int? approvedById,
    String? approvedByName,
    DateTime? approvedDate,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _AssetTransfer;

  factory AssetTransfer.fromJson(Map<String, dynamic> json) => _$AssetTransferFromJson(json);
}

@freezed
class InventoryItem with _$InventoryItem {
  const factory InventoryItem({
    required int id,
    required String name,
    String? description,
    required String sku,
    String? barcode,
    required ItemCategory category,
    required ItemUnit unit,
    required int currentStock,
    required int minStockLevel,
    required int maxStockLevel,
    required String location,
    double? unitCost,
    double? sellingPrice,
    String? currency,
    String? supplier,
    DateTime? lastRestockDate,
    DateTime? expiryDate,
    int? batchNumber,
    int? reservedQuantity,
    @Default(true) bool isActive,
    @Default(0) int reorderPoint,
    String? notes,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _InventoryItem;

  factory InventoryItem.fromJson(Map<String, dynamic> json) => _$InventoryItemFromJson(json);

  bool get isLowStock => currentStock <= minStockLevel;
  bool get isOverstocked => currentStock >= maxStockLevel;
  int get availableQuantity => currentStock - (reservedQuantity ?? 0);
  bool get needsReorder => currentStock <= (reorderPoint ?? minStockLevel);
}

@freezed
class StockMovement with _$StockMovement {
  const factory StockMovement({
    required int id,
    required int itemId,
    required String itemName,
    required MovementType type,
    required int quantity,
    int? previousStock,
    int? newStock,
    String? reason,
    String? reference,
    int? performedById,
    String? performedByName,
    required DateTime date,
    MovementStatus status,
    String? notes,
    required DateTime createdAt,
  }) = _StockMovement;

  factory StockMovement.fromJson(Map<String, dynamic> json) => _$StockMovementFromJson(json);
}

@freezed
class AssetInsurance with _$AssetInsurance {
  const factory AssetInsurance({
    required int id,
    required int assetId,
    required String assetName,
    required String provider,
    required String policyNumber,
    required double coverageAmount,
    String? currency,
    required DateTime startDate,
    required DateTime endDate,
    double? premium,
    String? premiumFrequency,
    String? coverageDetails,
    InsuranceStatus status,
    String? policyDocumentUrl,
    String? notes,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _AssetInsurance;

  factory AssetInsurance.fromJson(Map<String, dynamic> json) => _$AssetInsuranceFromJson(json);
}

enum AssetType {
  @JsonValue('EQUIPMENT')
  equipment,
  @JsonValue('FURNITURE')
  furniture,
  @JsonValue('VEHICLE')
  vehicle,
  @JsonValue('IT')
  it,
  @JsonValue('AUDIO_VISUAL')
  audioVisual,
  @JsonValue('MUSICAL_INSTRUMENT')
  musicalInstrument,
  @JsonValue('MEDICAL')
  medical,
  @JsonValue('SECURITY')
  security,
  @JsonValue('TOOL')
  tool,
  @JsonValue('OTHER')
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

  String getColor() {
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

  IconData getIcon() {
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
}

enum AssetStatus {
  @JsonValue('AVAILABLE')
  available,
  @JsonValue('IN_USE')
  inUse,
  @JsonValue('MAINTENANCE')
  maintenance,
  @JsonValue('REPAIR')
  repair,
  @JsonValue('RETIRED')
  retired,
  @JsonValue('LOST')
  lost,
  @JsonValue('STOLEN')
  stolen,
  @JsonValue('DISPOSED')
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

  Color getColor() {
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

enum MaintenanceType {
  @JsonValue('PREVENTIVE')
  preventive,
  @JsonValue('CORRECTIVE')
  corrective,
  @JsonValue('PREDICTIVE')
  predictive,
  @JsonValue('EMERGENCY')
  emergency,
  @JsonValue('CALIBRATION')
  calibration,
  @JsonValue('INSPECTION')
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
  @JsonValue('SCHEDULED')
  scheduled,
  @JsonValue('IN_PROGRESS')
  inProgress,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('CANCELLED')
  cancelled,
  @JsonValue('OVERDUE')
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

  Color getColor() {
    switch (this) {
      case MaintenanceStatus.scheduled: return Colors.blue;
      case MaintenanceStatus.inProgress: return Colors.orange;
      case MaintenanceStatus.completed: return Colors.green;
      case MaintenanceStatus.cancelled: return Colors.red;
      case MaintenanceStatus.overdue: return Colors.red;
    }
  }
}

enum CheckoutStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('APPROVED')
  approved,
  @JsonValue('CHECKED_OUT')
  checkedOut,
  @JsonValue('RETURNED')
  returned,
  @JsonValue('OVERDUE')
  overdue,
  @JsonValue('CANCELLED')
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

  Color getColor() {
    switch (this) {
      case CheckoutStatus.pending: return Colors.orange;
      case CheckoutStatus.approved: return Colors.blue;
      case CheckoutStatus.checkedOut: return Colors.green;
      case CheckoutStatus.returned: return Colors.grey;
      case CheckoutStatus.overdue: return Colors.red;
      case CheckoutStatus.cancelled: return Colors.red;
    }
  }
}

enum TransferStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('APPROVED')
  approved,
  @JsonValue('IN_TRANSIT')
  inTransit,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('REJECTED')
  rejected,
  @JsonValue('CANCELLED')
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

  Color getColor() {
    switch (this) {
      case TransferStatus.pending: return Colors.orange;
      case TransferStatus.approved: return Colors.blue;
      case TransferStatus.inTransit: return Colors.purple;
      case TransferStatus.completed: return Colors.green;
      case TransferStatus.rejected: return Colors.red;
      case TransferStatus.cancelled: return Colors.red;
    }
  }
}

enum ItemCategory {
  @JsonValue('CONSUMABLE')
  consumable,
  @JsonValue('OFFICE_SUPPLY')
  officeSupply,
  @JsonValue('CLEANING')
  cleaning,
  @JsonValue('FOOD_BEVERAGE')
  foodBeverage,
  @JsonValue('MEDICAL_SUPPLY')
  medicalSupply,
  @JsonValue('SAFETY')
  safety,
  @JsonValue('TOOL')
  tool,
  @JsonValue('OTHER')
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
  @JsonValue('UNIT')
  unit,
  @JsonValue('BOX')
  box,
  @JsonValue('PACK')
  pack,
  @JsonValue('ROLL')
  roll,
  @JsonValue('BOTTLE')
  bottle,
  @JsonValue('CAN')
  can,
  @JsonValue('KG')
  kg,
  @JsonValue('LITER')
  liter,
  @JsonValue('METER')
  meter,
  @JsonValue('SET')
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

enum MovementType {
  @JsonValue('IN')
  inMovement,
  @JsonValue('OUT')
  outMovement,
  @JsonValue('TRANSFER')
  transfer,
  @JsonValue('ADJUSTMENT')
  adjustment,
  @JsonValue('RETURN')
  returnMovement,
  @JsonValue('LOSS')
  loss,
  @JsonValue('DAMAGE')
  damage;

  String get displayName {
    switch (this) {
      case MovementType.inMovement: return 'Entrée';
      case MovementType.outMovement: return 'Sortie';
      case MovementType.transfer: return 'Transfert';
      case MovementType.adjustment: return 'Ajustement';
      case MovementType.returnMovement: return 'Retour';
      case MovementType.loss: return 'Perte';
      case MovementType.damage: return 'Dommage';
    }
  }

  Color getColor() {
    switch (this) {
      case MovementType.inMovement: return Colors.green;
      case MovementType.outMovement: return Colors.red;
      case MovementType.transfer: return Colors.blue;
      case MovementType.adjustment: return Colors.orange;
      case MovementType.returnMovement: return Colors.green;
      case MovementType.loss: return Colors.red;
      case MovementType.damage: return Colors.red;
    }
  }
}

enum MovementStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('CANCELLED')
  cancelled;

  String get displayName {
    switch (this) {
      case MovementStatus.pending: return 'En attente';
      case MovementStatus.completed: return 'Terminé';
      case MovementStatus.cancelled: return 'Annulé';
    }
  }
}

enum InsuranceStatus {
  @JsonValue('ACTIVE')
  active,
  @JsonValue('EXPIRED')
  expired,
  @JsonValue('CANCELLED')
  cancelled,
  @JsonValue('PENDING')
  pending;

  String get displayName {
    switch (this) {
      case InsuranceStatus.active: return 'Active';
      case InsuranceStatus.expired: return 'Expirée';
      case InsuranceStatus.cancelled: return 'Annulée';
      case InsuranceStatus.pending: return 'En attente';
    }
  }

  Color getColor() {
    switch (this) {
      case InsuranceStatus.active: return Colors.green;
      case InsuranceStatus.expired: return Colors.red;
      case InsuranceStatus.cancelled: return Colors.grey;
      case InsuranceStatus.pending: return Colors.orange;
    }
  }
}