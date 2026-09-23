// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'finance_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$TransactionImpl _$$TransactionImplFromJson(Map<String, dynamic> json) =>
    _$TransactionImpl(
      id: (json['id'] as num).toInt(),
      reference: json['reference'] as String,
      type: $enumDecode(_$TransactionTypeEnumMap, json['type']),
      category: $enumDecode(_$TransactionCategoryEnumMap, json['category']),
      amount: (json['amount'] as num).toDouble(),
      currency: json['currency'] as String,
      description: json['description'] as String?,
      date: DateTime.parse(json['date'] as String),
      paymentMethod: json['paymentMethod'] as String?,
      paymentReference: json['paymentReference'] as String?,
      status: $enumDecode(_$TransactionStatusEnumMap, json['status']),
      accountId: (json['accountId'] as num?)?.toInt(),
      accountName: json['accountName'] as String?,
      budgetId: (json['budgetId'] as num?)?.toInt(),
      budgetName: json['budgetName'] as String?,
      projectId: (json['projectId'] as num?)?.toInt(),
      projectName: json['projectName'] as String?,
      donorId: (json['donorId'] as num?)?.toInt(),
      donorName: json['donorName'] as String?,
      isAnonymous: json['isAnonymous'] as bool?,
      receiptUrl: json['receiptUrl'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
      isReconciled: json['isReconciled'] as bool? ?? false,
      reconciledAt: json['reconciledAt'] == null
          ? null
          : DateTime.parse(json['reconciledAt'] as String),
      reconciledBy: (json['reconciledBy'] as num?)?.toInt(),
    );

Map<String, dynamic> _$$TransactionImplToJson(_$TransactionImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'reference': instance.reference,
      'type': _$TransactionTypeEnumMap[instance.type]!,
      'category': _$TransactionCategoryEnumMap[instance.category]!,
      'amount': instance.amount,
      'currency': instance.currency,
      'description': instance.description,
      'date': instance.date.toIso8601String(),
      'paymentMethod': instance.paymentMethod,
      'paymentReference': instance.paymentReference,
      'status': _$TransactionStatusEnumMap[instance.status]!,
      'accountId': instance.accountId,
      'accountName': instance.accountName,
      'budgetId': instance.budgetId,
      'budgetName': instance.budgetName,
      'projectId': instance.projectId,
      'projectName': instance.projectName,
      'donorId': instance.donorId,
      'donorName': instance.donorName,
      'isAnonymous': instance.isAnonymous,
      'receiptUrl': instance.receiptUrl,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
      'isReconciled': instance.isReconciled,
      'reconciledAt': instance.reconciledAt?.toIso8601String(),
      'reconciledBy': instance.reconciledBy,
    };

const _$TransactionTypeEnumMap = {
  TransactionType.income: 'INCOME',
  TransactionType.expense: 'EXPENSE',
  TransactionType.transfer: 'TRANSFER',
  TransactionType.donation: 'DONATION',
  TransactionType.tontineContribution: 'TONTINE_CONTRIBUTION',
  TransactionType.tontinePayout: 'TONTINE_PAYOUT',
};

const _$TransactionCategoryEnumMap = {
  TransactionCategory.donations: 'DONATIONS',
  TransactionCategory.tithes: 'TITHES',
  TransactionCategory.offerings: 'OFFERINGS',
  TransactionCategory.events: 'EVENTS',
  TransactionCategory.building: 'BUILDING',
  TransactionCategory.missions: 'MISSIONS',
  TransactionCategory.benevolence: 'BENEVOLENCE',
  TransactionCategory.salaries: 'SALARIES',
  TransactionCategory.utilities: 'UTILITIES',
  TransactionCategory.maintenance: 'MAINTENANCE',
  TransactionCategory.supplies: 'SUPPLIES',
  TransactionCategory.transport: 'TRANSPORT',
  TransactionCategory.meals: 'MEALS',
  TransactionCategory.training: 'TRAINING',
  TransactionCategory.other: 'OTHER',
};

const _$TransactionStatusEnumMap = {
  TransactionStatus.pending: 'PENDING',
  TransactionStatus.completed: 'COMPLETED',
  TransactionStatus.failed: 'FAILED',
  TransactionStatus.cancelled: 'CANCELLED',
  TransactionStatus.refunded: 'REFUNDED',
};

_$AccountImpl _$$AccountImplFromJson(Map<String, dynamic> json) =>
    _$AccountImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      code: json['code'] as String,
      type: $enumDecode(_$AccountTypeEnumMap, json['type']),
      currency: json['currency'] as String,
      balance: (json['balance'] as num?)?.toDouble() ?? 0.0,
      initialBalance: (json['initialBalance'] as num?)?.toDouble() ?? 0.0,
      description: json['description'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      parentId: (json['parentId'] as num?)?.toInt(),
      parentName: json['parentName'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$AccountImplToJson(_$AccountImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'code': instance.code,
      'type': _$AccountTypeEnumMap[instance.type]!,
      'currency': instance.currency,
      'balance': instance.balance,
      'initialBalance': instance.initialBalance,
      'description': instance.description,
      'isActive': instance.isActive,
      'parentId': instance.parentId,
      'parentName': instance.parentName,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$AccountTypeEnumMap = {
  AccountType.asset: 'ASSET',
  AccountType.liability: 'LIABILITY',
  AccountType.equity: 'EQUITY',
  AccountType.income: 'INCOME',
  AccountType.expense: 'EXPENSE',
};

_$BudgetImpl _$$BudgetImplFromJson(Map<String, dynamic> json) => _$BudgetImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      code: json['code'] as String,
      allocatedAmount: (json['allocatedAmount'] as num).toDouble(),
      spentAmount: (json['spentAmount'] as num?)?.toDouble() ?? 0.0,
      currency: json['currency'] as String,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: DateTime.parse(json['endDate'] as String),
      isActive: json['isActive'] as bool? ?? true,
      description: json['description'] as String?,
      departmentId: (json['departmentId'] as num?)?.toInt(),
      departmentName: json['departmentName'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$BudgetImplToJson(_$BudgetImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'code': instance.code,
      'allocatedAmount': instance.allocatedAmount,
      'spentAmount': instance.spentAmount,
      'currency': instance.currency,
      'startDate': instance.startDate.toIso8601String(),
      'endDate': instance.endDate.toIso8601String(),
      'isActive': instance.isActive,
      'description': instance.description,
      'departmentId': instance.departmentId,
      'departmentName': instance.departmentName,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

_$TontineImpl _$$TontineImplFromJson(Map<String, dynamic> json) =>
    _$TontineImpl(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      description: json['description'] as String,
      contributionAmount: (json['contributionAmount'] as num).toDouble(),
      frequency: $enumDecode(_$TontineFrequencyEnumMap, json['frequency']),
      maxMembers: (json['maxMembers'] as num).toInt(),
      currentMembers: (json['currentMembers'] as num?)?.toInt() ?? 0,
      startDate: DateTime.parse(json['startDate'] as String),
      endDate: json['endDate'] == null
          ? null
          : DateTime.parse(json['endDate'] as String),
      status: $enumDecode(_$TontineStatusEnumMap, json['status']),
      currency: json['currency'] as String,
      managerId: (json['managerId'] as num?)?.toInt(),
      managerName: json['managerName'] as String?,
      payoutMethod: json['payoutMethod'] as String?,
      isPublic: json['isPublic'] as bool? ?? false,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$TontineImplToJson(_$TontineImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'contributionAmount': instance.contributionAmount,
      'frequency': _$TontineFrequencyEnumMap[instance.frequency]!,
      'maxMembers': instance.maxMembers,
      'currentMembers': instance.currentMembers,
      'startDate': instance.startDate.toIso8601String(),
      'endDate': instance.endDate?.toIso8601String(),
      'status': _$TontineStatusEnumMap[instance.status]!,
      'currency': instance.currency,
      'managerId': instance.managerId,
      'managerName': instance.managerName,
      'payoutMethod': instance.payoutMethod,
      'isPublic': instance.isPublic,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt?.toIso8601String(),
    };

const _$TontineFrequencyEnumMap = {
  TontineFrequency.weekly: 'WEEKLY',
  TontineFrequency.biweekly: 'BIWEEKLY',
  TontineFrequency.monthly: 'MONTHLY',
  TontineFrequency.quarterly: 'QUARTERLY',
};

const _$TontineStatusEnumMap = {
  TontineStatus.draft: 'DRAFT',
  TontineStatus.recruiting: 'RECRUITING',
  TontineStatus.active: 'ACTIVE',
  TontineStatus.completed: 'COMPLETED',
  TontineStatus.cancelled: 'CANCELLED',
};

_$TontineMemberImpl _$$TontineMemberImplFromJson(Map<String, dynamic> json) =>
    _$TontineMemberImpl(
      id: (json['id'] as num).toInt(),
      tontineId: (json['tontineId'] as num).toInt(),
      personId: (json['personId'] as num).toInt(),
      personName: json['personName'] as String,
      role: $enumDecode(_$TontineMemberRoleEnumMap, json['role']),
      status: $enumDecode(_$TontineMemberStatusEnumMap, json['status']),
      position: (json['position'] as num).toInt(),
      joinedAt: json['joinedAt'] == null
          ? null
          : DateTime.parse(json['joinedAt'] as String),
      lastPaymentAt: json['lastPaymentAt'] == null
          ? null
          : DateTime.parse(json['lastPaymentAt'] as String),
      totalContributed: (json['totalContributed'] as num?)?.toDouble() ?? 0.0,
      paymentsMade: (json['paymentsMade'] as num?)?.toInt() ?? 0,
      paymentsMissed: (json['paymentsMissed'] as num?)?.toInt() ?? 0,
    );

Map<String, dynamic> _$$TontineMemberImplToJson(_$TontineMemberImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'tontineId': instance.tontineId,
      'personId': instance.personId,
      'personName': instance.personName,
      'role': _$TontineMemberRoleEnumMap[instance.role]!,
      'status': _$TontineMemberStatusEnumMap[instance.status]!,
      'position': instance.position,
      'joinedAt': instance.joinedAt?.toIso8601String(),
      'lastPaymentAt': instance.lastPaymentAt?.toIso8601String(),
      'totalContributed': instance.totalContributed,
      'paymentsMade': instance.paymentsMade,
      'paymentsMissed': instance.paymentsMissed,
    };

const _$TontineMemberRoleEnumMap = {
  TontineMemberRole.manager: 'MANAGER',
  TontineMemberRole.secretary: 'SECRETARY',
  TontineMemberRole.treasurer: 'TREASURER',
  TontineMemberRole.member: 'MEMBER',
};

const _$TontineMemberStatusEnumMap = {
  TontineMemberStatus.pending: 'PENDING',
  TontineMemberStatus.active: 'ACTIVE',
  TontineMemberStatus.suspended: 'SUSPENDED',
  TontineMemberStatus.excluded: 'EXCLUDED',
  TontineMemberStatus.completed: 'COMPLETED',
};

_$TontinePayoutImpl _$$TontinePayoutImplFromJson(Map<String, dynamic> json) =>
    _$TontinePayoutImpl(
      id: (json['id'] as num).toInt(),
      tontineId: (json['tontineId'] as num).toInt(),
      memberId: (json['memberId'] as num).toInt(),
      memberName: json['memberName'] as String,
      amount: (json['amount'] as num).toDouble(),
      scheduledDate: DateTime.parse(json['scheduledDate'] as String),
      paidDate: json['paidDate'] == null
          ? null
          : DateTime.parse(json['paidDate'] as String),
      status: $enumDecode(_$TontinePayoutStatusEnumMap, json['status']),
      paymentReference: json['paymentReference'] as String?,
    );

Map<String, dynamic> _$$TontinePayoutImplToJson(_$TontinePayoutImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'tontineId': instance.tontineId,
      'memberId': instance.memberId,
      'memberName': instance.memberName,
      'amount': instance.amount,
      'scheduledDate': instance.scheduledDate.toIso8601String(),
      'paidDate': instance.paidDate?.toIso8601String(),
      'status': _$TontinePayoutStatusEnumMap[instance.status]!,
      'paymentReference': instance.paymentReference,
    };

const _$TontinePayoutStatusEnumMap = {
  TontinePayoutStatus.pending: 'PENDING',
  TontinePayoutStatus.scheduled: 'SCHEDULED',
  TontinePayoutStatus.paid: 'PAID',
  TontinePayoutStatus.failed: 'FAILED',
};

_$DonationImpl _$$DonationImplFromJson(Map<String, dynamic> json) =>
    _$DonationImpl(
      id: (json['id'] as num).toInt(),
      reference: json['reference'] as String,
      amount: (json['amount'] as num).toDouble(),
      currency: json['currency'] as String,
      donorName: json['donorName'] as String?,
      donorEmail: json['donorEmail'] as String?,
      donorPhone: json['donorPhone'] as String?,
      isAnonymous: json['isAnonymous'] as bool? ?? false,
      message: json['message'] as String?,
      date: DateTime.parse(json['date'] as String),
      source: $enumDecode(_$DonationSourceEnumMap, json['source']),
      paymentReference: json['paymentReference'] as String?,
      status: $enumDecode(_$DonationStatusEnumMap, json['status']),
      campaignId: json['campaignId'] as String?,
      campaignName: json['campaignName'] as String?,
      receiptSentAt: json['receiptSentAt'] == null
          ? null
          : DateTime.parse(json['receiptSentAt'] as String),
      createdAt: DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$$DonationImplToJson(_$DonationImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'reference': instance.reference,
      'amount': instance.amount,
      'currency': instance.currency,
      'donorName': instance.donorName,
      'donorEmail': instance.donorEmail,
      'donorPhone': instance.donorPhone,
      'isAnonymous': instance.isAnonymous,
      'message': instance.message,
      'date': instance.date.toIso8601String(),
      'source': _$DonationSourceEnumMap[instance.source]!,
      'paymentReference': instance.paymentReference,
      'status': _$DonationStatusEnumMap[instance.status]!,
      'campaignId': instance.campaignId,
      'campaignName': instance.campaignName,
      'receiptSentAt': instance.receiptSentAt?.toIso8601String(),
      'createdAt': instance.createdAt.toIso8601String(),
    };

const _$DonationSourceEnumMap = {
  DonationSource.online: 'ONLINE',
  DonationSource.cash: 'CASH',
  DonationSource.mobileMoney: 'MOBILE_MONEY',
  DonationSource.bankTransfer: 'BANK_TRANSFER',
  DonationSource.check: 'CHECK',
};

const _$DonationStatusEnumMap = {
  DonationStatus.pending: 'PENDING',
  DonationStatus.completed: 'COMPLETED',
  DonationStatus.failed: 'FAILED',
  DonationStatus.refunded: 'REFUNDED',
};
