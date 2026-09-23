import 'package:freezed_annotation/freezed_annotation.dart';

part 'finance_model.freezed.dart';
part 'finance_model.g.dart';

@freezed
class Transaction with _$Transaction {
  const factory Transaction({
    required int id,
    required String reference,
    required TransactionType type,
    required TransactionCategory category,
    required double amount,
    required String currency,
    String? description,
    required DateTime date,
    String? paymentMethod,
    String? paymentReference,
    required TransactionStatus status,
    int? accountId,
    String? accountName,
    int? budgetId,
    String? budgetName,
    int? projectId,
    String? projectName,
    int? donorId,
    String? donorName,
    bool? isAnonymous,
    String? receiptUrl,
    required DateTime createdAt,
    DateTime? updatedAt,
    @Default(false) bool isReconciled,
    DateTime? reconciledAt,
    int? reconciledBy,
  }) = _Transaction;

  factory Transaction.fromJson(Map<String, dynamic> json) => _$TransactionFromJson(json);
}

@freezed
class Account with _$Account {
  const factory Account({
    required int id,
    required String name,
    required String code,
    required AccountType type,
    required String currency,
    @Default(0.0) double balance,
    @Default(0.0) double initialBalance,
    String? description,
    @Default(true) bool isActive,
    int? parentId,
    String? parentName,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _Account;

  factory Account.fromJson(Map<String, dynamic> json) => _$AccountFromJson(json);
}

@freezed
class Budget with _$Budget {
  const factory Budget({
    required int id,
    required String name,
    required String code,
    required double allocatedAmount,
    @Default(0.0) double spentAmount,
    required String currency,
    required DateTime startDate,
    required DateTime endDate,
    @Default(true) bool isActive,
    String? description,
    int? departmentId,
    String? departmentName,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _Budget;

  factory Budget.fromJson(Map<String, dynamic> json) => _$BudgetFromJson(json);
}

@freezed
class Tontine with _$Tontine {
  const factory Tontine({
    required int id,
    required String name,
    required String description,
    required double contributionAmount,
    required TontineFrequency frequency,
    required int maxMembers,
    @Default(0) int currentMembers,
    required DateTime startDate,
    DateTime? endDate,
    required TontineStatus status,
    required String currency,
    int? managerId,
    String? managerName,
    String? payoutMethod,
    @Default(false) bool isPublic,
    required DateTime createdAt,
    DateTime? updatedAt,
  }) = _Tontine;

  factory Tontine.fromJson(Map<String, dynamic> json) => _$TontineFromJson(json);
}

@freezed
class TontineMember with _$TontineMember {
  const factory TontineMember({
    required int id,
    required int tontineId,
    required int personId,
    required String personName,
    required TontineMemberRole role,
    required TontineMemberStatus status,
    required int position,
    DateTime? joinedAt,
    DateTime? lastPaymentAt,
    @Default(0.0) double totalContributed,
    @Default(0) int paymentsMade,
    @Default(0) int paymentsMissed,
  }) = _TontineMember;

  factory TontineMember.fromJson(Map<String, dynamic> json) => _$TontineMemberFromJson(json);
}

@freezed
class TontinePayout with _$TontinePayout {
  const factory TontinePayout({
    required int id,
    required int tontineId,
    required int memberId,
    required String memberName,
    required double amount,
    required DateTime scheduledDate,
    DateTime? paidDate,
    required TontinePayoutStatus status,
    String? paymentReference,
  }) = _TontinePayout;

  factory TontinePayout.fromJson(Map<String, dynamic> json) => _$TontinePayoutFromJson(json);
}

@freezed
class Donation with _$Donation {
  const factory Donation({
    required int id,
    required String reference,
    required double amount,
    required String currency,
    String? donorName,
    String? donorEmail,
    String? donorPhone,
    @Default(false) bool isAnonymous,
    String? message,
    required DateTime date,
    required DonationSource source,
    String? paymentReference,
    required DonationStatus status,
    String? campaignId,
    String? campaignName,
    DateTime? receiptSentAt,
    required DateTime createdAt,
  }) = _Donation;

  factory Donation.fromJson(Map<String, dynamic> json) => _$DonationFromJson(json);
}

enum TransactionType {
  @JsonValue('INCOME')
  income,
  @JsonValue('EXPENSE')
  expense,
  @JsonValue('TRANSFER')
  transfer,
  @JsonValue('DONATION')
  donation,
  @JsonValue('TONTINE_CONTRIBUTION')
  tontineContribution,
  @JsonValue('TONTINE_PAYOUT')
  tontinePayout,
}

enum TransactionCategory {
  @JsonValue('DONATIONS')
  donations,
  @JsonValue('TITHES')
  tithes,
  @JsonValue('OFFERINGS')
  offerings,
  @JsonValue('EVENTS')
  events,
  @JsonValue('BUILDING')
  building,
  @JsonValue('MISSIONS')
  missions,
  @JsonValue('BENEVOLENCE')
  benevolence,
  @JsonValue('SALARIES')
  salaries,
  @JsonValue('UTILITIES')
  utilities,
  @JsonValue('MAINTENANCE')
  maintenance,
  @JsonValue('SUPPLIES')
  supplies,
  @JsonValue('TRANSPORT')
  transport,
  @JsonValue('MEALS')
  meals,
  @JsonValue('TRAINING')
  training,
  @JsonValue('OTHER')
  other;

  String get displayName {
    switch (this) {
      case TransactionCategory.donations:
        return 'Dons';
      case TransactionCategory.tithes:
        return 'Dîmes';
      case TransactionCategory.offerings:
        return 'Offrandes';
      case TransactionCategory.events:
        return 'Événements';
      case TransactionCategory.building:
        return 'Bâtiment';
      case TransactionCategory.missions:
        return 'Missions';
      case TransactionCategory.benevolence:
        return 'Bienfaisance';
      case TransactionCategory.salaries:
        return 'Salaires';
      case TransactionCategory.utilities:
        return 'Services publics';
      case TransactionCategory.maintenance:
        return 'Maintenance';
      case TransactionCategory.supplies:
        return 'Fournitures';
      case TransactionCategory.transport:
        return 'Transport';
      case TransactionCategory.meals:
        return 'Repas';
      case TransactionCategory.training:
        return 'Formation';
      case TransactionCategory.other:
        return 'Autre';
    }
  }
}

enum TransactionStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('FAILED')
  failed,
  @JsonValue('CANCELLED')
  cancelled,
  @JsonValue('REFUNDED')
  refunded,
}

enum AccountType {
  @JsonValue('ASSET')
  asset,
  @JsonValue('LIABILITY')
  liability,
  @JsonValue('EQUITY')
  equity,
  @JsonValue('INCOME')
  income,
  @JsonValue('EXPENSE')
  expense,
}

enum BudgetStatus {
  @JsonValue('DRAFT')
  draft,
  @JsonValue('ACTIVE')
  active,
  @JsonValue('EXHAUSTED')
  exhausted,
  @JsonValue('CLOSED')
  closed,
}

enum TontineFrequency {
  @JsonValue('WEEKLY')
  weekly,
  @JsonValue('BIWEEKLY')
  biweekly,
  @JsonValue('MONTHLY')
  monthly,
  @JsonValue('QUARTERLY')
  quarterly;

  String get displayName {
    switch (this) {
      case TontineFrequency.weekly:
        return 'Hebdomadaire';
      case TontineFrequency.biweekly:
        return 'Quinzaine';
      case TontineFrequency.monthly:
        return 'Mensuel';
      case TontineFrequency.quarterly:
        return 'Trimestriel';
    }
  }
}

enum TontineStatus {
  @JsonValue('DRAFT')
  draft,
  @JsonValue('RECRUITING')
  recruiting,
  @JsonValue('ACTIVE')
  active,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('CANCELLED')
  cancelled;

  String get displayName {
    switch (this) {
      case TontineStatus.draft:
        return 'Brouillon';
      case TontineStatus.recruiting:
        return 'Recrutement';
      case TontineStatus.active:
        return 'Active';
      case TontineStatus.completed:
        return 'Terminée';
      case TontineStatus.cancelled:
        return 'Annulée';
    }
  }
}

enum TontineMemberRole {
  @JsonValue('MANAGER')
  manager,
  @JsonValue('SECRETARY')
  secretary,
  @JsonValue('TREASURER')
  treasurer,
  @JsonValue('MEMBER')
  member,
}

enum TontineMemberStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('ACTIVE')
  active,
  @JsonValue('SUSPENDED')
  suspended,
  @JsonValue('EXCLUDED')
  excluded,
  @JsonValue('COMPLETED')
  completed,
}

enum TontinePayoutStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('SCHEDULED')
  scheduled,
  @JsonValue('PAID')
  paid,
  @JsonValue('FAILED')
  failed,
}

enum DonationSource {
  @JsonValue('ONLINE')
  online,
  @JsonValue('CASH')
  cash,
  @JsonValue('MOBILE_MONEY')
  mobileMoney,
  @JsonValue('BANK_TRANSFER')
  bankTransfer,
  @JsonValue('CHECK')
  check,
}

enum DonationStatus {
  @JsonValue('PENDING')
  pending,
  @JsonValue('COMPLETED')
  completed,
  @JsonValue('FAILED')
  failed,
  @JsonValue('REFUNDED')
  refunded,
}