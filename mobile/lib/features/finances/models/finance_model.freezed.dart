// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'finance_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

Transaction _$TransactionFromJson(Map<String, dynamic> json) {
  return _Transaction.fromJson(json);
}

/// @nodoc
mixin _$Transaction {
  int get id => throw _privateConstructorUsedError;
  String get reference => throw _privateConstructorUsedError;
  TransactionType get type => throw _privateConstructorUsedError;
  TransactionCategory get category => throw _privateConstructorUsedError;
  double get amount => throw _privateConstructorUsedError;
  String get currency => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  DateTime get date => throw _privateConstructorUsedError;
  String? get paymentMethod => throw _privateConstructorUsedError;
  String? get paymentReference => throw _privateConstructorUsedError;
  TransactionStatus get status => throw _privateConstructorUsedError;
  int? get accountId => throw _privateConstructorUsedError;
  String? get accountName => throw _privateConstructorUsedError;
  int? get budgetId => throw _privateConstructorUsedError;
  String? get budgetName => throw _privateConstructorUsedError;
  int? get projectId => throw _privateConstructorUsedError;
  String? get projectName => throw _privateConstructorUsedError;
  int? get donorId => throw _privateConstructorUsedError;
  String? get donorName => throw _privateConstructorUsedError;
  bool? get isAnonymous => throw _privateConstructorUsedError;
  String? get receiptUrl => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;
  bool get isReconciled => throw _privateConstructorUsedError;
  DateTime? get reconciledAt => throw _privateConstructorUsedError;
  int? get reconciledBy => throw _privateConstructorUsedError;

  /// Serializes this Transaction to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Transaction
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TransactionCopyWith<Transaction> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TransactionCopyWith<$Res> {
  factory $TransactionCopyWith(
          Transaction value, $Res Function(Transaction) then) =
      _$TransactionCopyWithImpl<$Res, Transaction>;
  @useResult
  $Res call(
      {int id,
      String reference,
      TransactionType type,
      TransactionCategory category,
      double amount,
      String currency,
      String? description,
      DateTime date,
      String? paymentMethod,
      String? paymentReference,
      TransactionStatus status,
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
      DateTime createdAt,
      DateTime? updatedAt,
      bool isReconciled,
      DateTime? reconciledAt,
      int? reconciledBy});
}

/// @nodoc
class _$TransactionCopyWithImpl<$Res, $Val extends Transaction>
    implements $TransactionCopyWith<$Res> {
  _$TransactionCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Transaction
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? reference = null,
    Object? type = null,
    Object? category = null,
    Object? amount = null,
    Object? currency = null,
    Object? description = freezed,
    Object? date = null,
    Object? paymentMethod = freezed,
    Object? paymentReference = freezed,
    Object? status = null,
    Object? accountId = freezed,
    Object? accountName = freezed,
    Object? budgetId = freezed,
    Object? budgetName = freezed,
    Object? projectId = freezed,
    Object? projectName = freezed,
    Object? donorId = freezed,
    Object? donorName = freezed,
    Object? isAnonymous = freezed,
    Object? receiptUrl = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? isReconciled = null,
    Object? reconciledAt = freezed,
    Object? reconciledBy = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      reference: null == reference
          ? _value.reference
          : reference // ignore: cast_nullable_to_non_nullable
              as String,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as TransactionType,
      category: null == category
          ? _value.category
          : category // ignore: cast_nullable_to_non_nullable
              as TransactionCategory,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as double,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      date: null == date
          ? _value.date
          : date // ignore: cast_nullable_to_non_nullable
              as DateTime,
      paymentMethod: freezed == paymentMethod
          ? _value.paymentMethod
          : paymentMethod // ignore: cast_nullable_to_non_nullable
              as String?,
      paymentReference: freezed == paymentReference
          ? _value.paymentReference
          : paymentReference // ignore: cast_nullable_to_non_nullable
              as String?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TransactionStatus,
      accountId: freezed == accountId
          ? _value.accountId
          : accountId // ignore: cast_nullable_to_non_nullable
              as int?,
      accountName: freezed == accountName
          ? _value.accountName
          : accountName // ignore: cast_nullable_to_non_nullable
              as String?,
      budgetId: freezed == budgetId
          ? _value.budgetId
          : budgetId // ignore: cast_nullable_to_non_nullable
              as int?,
      budgetName: freezed == budgetName
          ? _value.budgetName
          : budgetName // ignore: cast_nullable_to_non_nullable
              as String?,
      projectId: freezed == projectId
          ? _value.projectId
          : projectId // ignore: cast_nullable_to_non_nullable
              as int?,
      projectName: freezed == projectName
          ? _value.projectName
          : projectName // ignore: cast_nullable_to_non_nullable
              as String?,
      donorId: freezed == donorId
          ? _value.donorId
          : donorId // ignore: cast_nullable_to_non_nullable
              as int?,
      donorName: freezed == donorName
          ? _value.donorName
          : donorName // ignore: cast_nullable_to_non_nullable
              as String?,
      isAnonymous: freezed == isAnonymous
          ? _value.isAnonymous
          : isAnonymous // ignore: cast_nullable_to_non_nullable
              as bool?,
      receiptUrl: freezed == receiptUrl
          ? _value.receiptUrl
          : receiptUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      isReconciled: null == isReconciled
          ? _value.isReconciled
          : isReconciled // ignore: cast_nullable_to_non_nullable
              as bool,
      reconciledAt: freezed == reconciledAt
          ? _value.reconciledAt
          : reconciledAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      reconciledBy: freezed == reconciledBy
          ? _value.reconciledBy
          : reconciledBy // ignore: cast_nullable_to_non_nullable
              as int?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TransactionImplCopyWith<$Res>
    implements $TransactionCopyWith<$Res> {
  factory _$$TransactionImplCopyWith(
          _$TransactionImpl value, $Res Function(_$TransactionImpl) then) =
      __$$TransactionImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String reference,
      TransactionType type,
      TransactionCategory category,
      double amount,
      String currency,
      String? description,
      DateTime date,
      String? paymentMethod,
      String? paymentReference,
      TransactionStatus status,
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
      DateTime createdAt,
      DateTime? updatedAt,
      bool isReconciled,
      DateTime? reconciledAt,
      int? reconciledBy});
}

/// @nodoc
class __$$TransactionImplCopyWithImpl<$Res>
    extends _$TransactionCopyWithImpl<$Res, _$TransactionImpl>
    implements _$$TransactionImplCopyWith<$Res> {
  __$$TransactionImplCopyWithImpl(
      _$TransactionImpl _value, $Res Function(_$TransactionImpl) _then)
      : super(_value, _then);

  /// Create a copy of Transaction
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? reference = null,
    Object? type = null,
    Object? category = null,
    Object? amount = null,
    Object? currency = null,
    Object? description = freezed,
    Object? date = null,
    Object? paymentMethod = freezed,
    Object? paymentReference = freezed,
    Object? status = null,
    Object? accountId = freezed,
    Object? accountName = freezed,
    Object? budgetId = freezed,
    Object? budgetName = freezed,
    Object? projectId = freezed,
    Object? projectName = freezed,
    Object? donorId = freezed,
    Object? donorName = freezed,
    Object? isAnonymous = freezed,
    Object? receiptUrl = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? isReconciled = null,
    Object? reconciledAt = freezed,
    Object? reconciledBy = freezed,
  }) {
    return _then(_$TransactionImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      reference: null == reference
          ? _value.reference
          : reference // ignore: cast_nullable_to_non_nullable
              as String,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as TransactionType,
      category: null == category
          ? _value.category
          : category // ignore: cast_nullable_to_non_nullable
              as TransactionCategory,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as double,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      date: null == date
          ? _value.date
          : date // ignore: cast_nullable_to_non_nullable
              as DateTime,
      paymentMethod: freezed == paymentMethod
          ? _value.paymentMethod
          : paymentMethod // ignore: cast_nullable_to_non_nullable
              as String?,
      paymentReference: freezed == paymentReference
          ? _value.paymentReference
          : paymentReference // ignore: cast_nullable_to_non_nullable
              as String?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TransactionStatus,
      accountId: freezed == accountId
          ? _value.accountId
          : accountId // ignore: cast_nullable_to_non_nullable
              as int?,
      accountName: freezed == accountName
          ? _value.accountName
          : accountName // ignore: cast_nullable_to_non_nullable
              as String?,
      budgetId: freezed == budgetId
          ? _value.budgetId
          : budgetId // ignore: cast_nullable_to_non_nullable
              as int?,
      budgetName: freezed == budgetName
          ? _value.budgetName
          : budgetName // ignore: cast_nullable_to_non_nullable
              as String?,
      projectId: freezed == projectId
          ? _value.projectId
          : projectId // ignore: cast_nullable_to_non_nullable
              as int?,
      projectName: freezed == projectName
          ? _value.projectName
          : projectName // ignore: cast_nullable_to_non_nullable
              as String?,
      donorId: freezed == donorId
          ? _value.donorId
          : donorId // ignore: cast_nullable_to_non_nullable
              as int?,
      donorName: freezed == donorName
          ? _value.donorName
          : donorName // ignore: cast_nullable_to_non_nullable
              as String?,
      isAnonymous: freezed == isAnonymous
          ? _value.isAnonymous
          : isAnonymous // ignore: cast_nullable_to_non_nullable
              as bool?,
      receiptUrl: freezed == receiptUrl
          ? _value.receiptUrl
          : receiptUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      isReconciled: null == isReconciled
          ? _value.isReconciled
          : isReconciled // ignore: cast_nullable_to_non_nullable
              as bool,
      reconciledAt: freezed == reconciledAt
          ? _value.reconciledAt
          : reconciledAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      reconciledBy: freezed == reconciledBy
          ? _value.reconciledBy
          : reconciledBy // ignore: cast_nullable_to_non_nullable
              as int?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TransactionImpl implements _Transaction {
  const _$TransactionImpl(
      {required this.id,
      required this.reference,
      required this.type,
      required this.category,
      required this.amount,
      required this.currency,
      this.description,
      required this.date,
      this.paymentMethod,
      this.paymentReference,
      required this.status,
      this.accountId,
      this.accountName,
      this.budgetId,
      this.budgetName,
      this.projectId,
      this.projectName,
      this.donorId,
      this.donorName,
      this.isAnonymous,
      this.receiptUrl,
      required this.createdAt,
      this.updatedAt,
      this.isReconciled = false,
      this.reconciledAt,
      this.reconciledBy});

  factory _$TransactionImpl.fromJson(Map<String, dynamic> json) =>
      _$$TransactionImplFromJson(json);

  @override
  final int id;
  @override
  final String reference;
  @override
  final TransactionType type;
  @override
  final TransactionCategory category;
  @override
  final double amount;
  @override
  final String currency;
  @override
  final String? description;
  @override
  final DateTime date;
  @override
  final String? paymentMethod;
  @override
  final String? paymentReference;
  @override
  final TransactionStatus status;
  @override
  final int? accountId;
  @override
  final String? accountName;
  @override
  final int? budgetId;
  @override
  final String? budgetName;
  @override
  final int? projectId;
  @override
  final String? projectName;
  @override
  final int? donorId;
  @override
  final String? donorName;
  @override
  final bool? isAnonymous;
  @override
  final String? receiptUrl;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;
  @override
  @JsonKey()
  final bool isReconciled;
  @override
  final DateTime? reconciledAt;
  @override
  final int? reconciledBy;

  @override
  String toString() {
    return 'Transaction(id: $id, reference: $reference, type: $type, category: $category, amount: $amount, currency: $currency, description: $description, date: $date, paymentMethod: $paymentMethod, paymentReference: $paymentReference, status: $status, accountId: $accountId, accountName: $accountName, budgetId: $budgetId, budgetName: $budgetName, projectId: $projectId, projectName: $projectName, donorId: $donorId, donorName: $donorName, isAnonymous: $isAnonymous, receiptUrl: $receiptUrl, createdAt: $createdAt, updatedAt: $updatedAt, isReconciled: $isReconciled, reconciledAt: $reconciledAt, reconciledBy: $reconciledBy)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TransactionImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.reference, reference) ||
                other.reference == reference) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.category, category) ||
                other.category == category) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.currency, currency) ||
                other.currency == currency) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.date, date) || other.date == date) &&
            (identical(other.paymentMethod, paymentMethod) ||
                other.paymentMethod == paymentMethod) &&
            (identical(other.paymentReference, paymentReference) ||
                other.paymentReference == paymentReference) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.accountId, accountId) ||
                other.accountId == accountId) &&
            (identical(other.accountName, accountName) ||
                other.accountName == accountName) &&
            (identical(other.budgetId, budgetId) ||
                other.budgetId == budgetId) &&
            (identical(other.budgetName, budgetName) ||
                other.budgetName == budgetName) &&
            (identical(other.projectId, projectId) ||
                other.projectId == projectId) &&
            (identical(other.projectName, projectName) ||
                other.projectName == projectName) &&
            (identical(other.donorId, donorId) || other.donorId == donorId) &&
            (identical(other.donorName, donorName) ||
                other.donorName == donorName) &&
            (identical(other.isAnonymous, isAnonymous) ||
                other.isAnonymous == isAnonymous) &&
            (identical(other.receiptUrl, receiptUrl) ||
                other.receiptUrl == receiptUrl) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt) &&
            (identical(other.isReconciled, isReconciled) ||
                other.isReconciled == isReconciled) &&
            (identical(other.reconciledAt, reconciledAt) ||
                other.reconciledAt == reconciledAt) &&
            (identical(other.reconciledBy, reconciledBy) ||
                other.reconciledBy == reconciledBy));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hashAll([
        runtimeType,
        id,
        reference,
        type,
        category,
        amount,
        currency,
        description,
        date,
        paymentMethod,
        paymentReference,
        status,
        accountId,
        accountName,
        budgetId,
        budgetName,
        projectId,
        projectName,
        donorId,
        donorName,
        isAnonymous,
        receiptUrl,
        createdAt,
        updatedAt,
        isReconciled,
        reconciledAt,
        reconciledBy
      ]);

  /// Create a copy of Transaction
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TransactionImplCopyWith<_$TransactionImpl> get copyWith =>
      __$$TransactionImplCopyWithImpl<_$TransactionImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TransactionImplToJson(
      this,
    );
  }
}

abstract class _Transaction implements Transaction {
  const factory _Transaction(
      {required final int id,
      required final String reference,
      required final TransactionType type,
      required final TransactionCategory category,
      required final double amount,
      required final String currency,
      final String? description,
      required final DateTime date,
      final String? paymentMethod,
      final String? paymentReference,
      required final TransactionStatus status,
      final int? accountId,
      final String? accountName,
      final int? budgetId,
      final String? budgetName,
      final int? projectId,
      final String? projectName,
      final int? donorId,
      final String? donorName,
      final bool? isAnonymous,
      final String? receiptUrl,
      required final DateTime createdAt,
      final DateTime? updatedAt,
      final bool isReconciled,
      final DateTime? reconciledAt,
      final int? reconciledBy}) = _$TransactionImpl;

  factory _Transaction.fromJson(Map<String, dynamic> json) =
      _$TransactionImpl.fromJson;

  @override
  int get id;
  @override
  String get reference;
  @override
  TransactionType get type;
  @override
  TransactionCategory get category;
  @override
  double get amount;
  @override
  String get currency;
  @override
  String? get description;
  @override
  DateTime get date;
  @override
  String? get paymentMethod;
  @override
  String? get paymentReference;
  @override
  TransactionStatus get status;
  @override
  int? get accountId;
  @override
  String? get accountName;
  @override
  int? get budgetId;
  @override
  String? get budgetName;
  @override
  int? get projectId;
  @override
  String? get projectName;
  @override
  int? get donorId;
  @override
  String? get donorName;
  @override
  bool? get isAnonymous;
  @override
  String? get receiptUrl;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;
  @override
  bool get isReconciled;
  @override
  DateTime? get reconciledAt;
  @override
  int? get reconciledBy;

  /// Create a copy of Transaction
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TransactionImplCopyWith<_$TransactionImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Account _$AccountFromJson(Map<String, dynamic> json) {
  return _Account.fromJson(json);
}

/// @nodoc
mixin _$Account {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get code => throw _privateConstructorUsedError;
  AccountType get type => throw _privateConstructorUsedError;
  String get currency => throw _privateConstructorUsedError;
  double get balance => throw _privateConstructorUsedError;
  double get initialBalance => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  bool get isActive => throw _privateConstructorUsedError;
  int? get parentId => throw _privateConstructorUsedError;
  String? get parentName => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this Account to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Account
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $AccountCopyWith<Account> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $AccountCopyWith<$Res> {
  factory $AccountCopyWith(Account value, $Res Function(Account) then) =
      _$AccountCopyWithImpl<$Res, Account>;
  @useResult
  $Res call(
      {int id,
      String name,
      String code,
      AccountType type,
      String currency,
      double balance,
      double initialBalance,
      String? description,
      bool isActive,
      int? parentId,
      String? parentName,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$AccountCopyWithImpl<$Res, $Val extends Account>
    implements $AccountCopyWith<$Res> {
  _$AccountCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Account
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? code = null,
    Object? type = null,
    Object? currency = null,
    Object? balance = null,
    Object? initialBalance = null,
    Object? description = freezed,
    Object? isActive = null,
    Object? parentId = freezed,
    Object? parentName = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      code: null == code
          ? _value.code
          : code // ignore: cast_nullable_to_non_nullable
              as String,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as AccountType,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      balance: null == balance
          ? _value.balance
          : balance // ignore: cast_nullable_to_non_nullable
              as double,
      initialBalance: null == initialBalance
          ? _value.initialBalance
          : initialBalance // ignore: cast_nullable_to_non_nullable
              as double,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      parentId: freezed == parentId
          ? _value.parentId
          : parentId // ignore: cast_nullable_to_non_nullable
              as int?,
      parentName: freezed == parentName
          ? _value.parentName
          : parentName // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$AccountImplCopyWith<$Res> implements $AccountCopyWith<$Res> {
  factory _$$AccountImplCopyWith(
          _$AccountImpl value, $Res Function(_$AccountImpl) then) =
      __$$AccountImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      String code,
      AccountType type,
      String currency,
      double balance,
      double initialBalance,
      String? description,
      bool isActive,
      int? parentId,
      String? parentName,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$AccountImplCopyWithImpl<$Res>
    extends _$AccountCopyWithImpl<$Res, _$AccountImpl>
    implements _$$AccountImplCopyWith<$Res> {
  __$$AccountImplCopyWithImpl(
      _$AccountImpl _value, $Res Function(_$AccountImpl) _then)
      : super(_value, _then);

  /// Create a copy of Account
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? code = null,
    Object? type = null,
    Object? currency = null,
    Object? balance = null,
    Object? initialBalance = null,
    Object? description = freezed,
    Object? isActive = null,
    Object? parentId = freezed,
    Object? parentName = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$AccountImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      code: null == code
          ? _value.code
          : code // ignore: cast_nullable_to_non_nullable
              as String,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as AccountType,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      balance: null == balance
          ? _value.balance
          : balance // ignore: cast_nullable_to_non_nullable
              as double,
      initialBalance: null == initialBalance
          ? _value.initialBalance
          : initialBalance // ignore: cast_nullable_to_non_nullable
              as double,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      parentId: freezed == parentId
          ? _value.parentId
          : parentId // ignore: cast_nullable_to_non_nullable
              as int?,
      parentName: freezed == parentName
          ? _value.parentName
          : parentName // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$AccountImpl implements _Account {
  const _$AccountImpl(
      {required this.id,
      required this.name,
      required this.code,
      required this.type,
      required this.currency,
      this.balance = 0.0,
      this.initialBalance = 0.0,
      this.description,
      this.isActive = true,
      this.parentId,
      this.parentName,
      required this.createdAt,
      this.updatedAt});

  factory _$AccountImpl.fromJson(Map<String, dynamic> json) =>
      _$$AccountImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final String code;
  @override
  final AccountType type;
  @override
  final String currency;
  @override
  @JsonKey()
  final double balance;
  @override
  @JsonKey()
  final double initialBalance;
  @override
  final String? description;
  @override
  @JsonKey()
  final bool isActive;
  @override
  final int? parentId;
  @override
  final String? parentName;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'Account(id: $id, name: $name, code: $code, type: $type, currency: $currency, balance: $balance, initialBalance: $initialBalance, description: $description, isActive: $isActive, parentId: $parentId, parentName: $parentName, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$AccountImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.code, code) || other.code == code) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.currency, currency) ||
                other.currency == currency) &&
            (identical(other.balance, balance) || other.balance == balance) &&
            (identical(other.initialBalance, initialBalance) ||
                other.initialBalance == initialBalance) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.isActive, isActive) ||
                other.isActive == isActive) &&
            (identical(other.parentId, parentId) ||
                other.parentId == parentId) &&
            (identical(other.parentName, parentName) ||
                other.parentName == parentName) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      name,
      code,
      type,
      currency,
      balance,
      initialBalance,
      description,
      isActive,
      parentId,
      parentName,
      createdAt,
      updatedAt);

  /// Create a copy of Account
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$AccountImplCopyWith<_$AccountImpl> get copyWith =>
      __$$AccountImplCopyWithImpl<_$AccountImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$AccountImplToJson(
      this,
    );
  }
}

abstract class _Account implements Account {
  const factory _Account(
      {required final int id,
      required final String name,
      required final String code,
      required final AccountType type,
      required final String currency,
      final double balance,
      final double initialBalance,
      final String? description,
      final bool isActive,
      final int? parentId,
      final String? parentName,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$AccountImpl;

  factory _Account.fromJson(Map<String, dynamic> json) = _$AccountImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  String get code;
  @override
  AccountType get type;
  @override
  String get currency;
  @override
  double get balance;
  @override
  double get initialBalance;
  @override
  String? get description;
  @override
  bool get isActive;
  @override
  int? get parentId;
  @override
  String? get parentName;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of Account
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$AccountImplCopyWith<_$AccountImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Budget _$BudgetFromJson(Map<String, dynamic> json) {
  return _Budget.fromJson(json);
}

/// @nodoc
mixin _$Budget {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get code => throw _privateConstructorUsedError;
  double get allocatedAmount => throw _privateConstructorUsedError;
  double get spentAmount => throw _privateConstructorUsedError;
  String get currency => throw _privateConstructorUsedError;
  DateTime get startDate => throw _privateConstructorUsedError;
  DateTime get endDate => throw _privateConstructorUsedError;
  bool get isActive => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  int? get departmentId => throw _privateConstructorUsedError;
  String? get departmentName => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this Budget to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Budget
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $BudgetCopyWith<Budget> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $BudgetCopyWith<$Res> {
  factory $BudgetCopyWith(Budget value, $Res Function(Budget) then) =
      _$BudgetCopyWithImpl<$Res, Budget>;
  @useResult
  $Res call(
      {int id,
      String name,
      String code,
      double allocatedAmount,
      double spentAmount,
      String currency,
      DateTime startDate,
      DateTime endDate,
      bool isActive,
      String? description,
      int? departmentId,
      String? departmentName,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$BudgetCopyWithImpl<$Res, $Val extends Budget>
    implements $BudgetCopyWith<$Res> {
  _$BudgetCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Budget
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? code = null,
    Object? allocatedAmount = null,
    Object? spentAmount = null,
    Object? currency = null,
    Object? startDate = null,
    Object? endDate = null,
    Object? isActive = null,
    Object? description = freezed,
    Object? departmentId = freezed,
    Object? departmentName = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      code: null == code
          ? _value.code
          : code // ignore: cast_nullable_to_non_nullable
              as String,
      allocatedAmount: null == allocatedAmount
          ? _value.allocatedAmount
          : allocatedAmount // ignore: cast_nullable_to_non_nullable
              as double,
      spentAmount: null == spentAmount
          ? _value.spentAmount
          : spentAmount // ignore: cast_nullable_to_non_nullable
              as double,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: null == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      departmentId: freezed == departmentId
          ? _value.departmentId
          : departmentId // ignore: cast_nullable_to_non_nullable
              as int?,
      departmentName: freezed == departmentName
          ? _value.departmentName
          : departmentName // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$BudgetImplCopyWith<$Res> implements $BudgetCopyWith<$Res> {
  factory _$$BudgetImplCopyWith(
          _$BudgetImpl value, $Res Function(_$BudgetImpl) then) =
      __$$BudgetImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      String code,
      double allocatedAmount,
      double spentAmount,
      String currency,
      DateTime startDate,
      DateTime endDate,
      bool isActive,
      String? description,
      int? departmentId,
      String? departmentName,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$BudgetImplCopyWithImpl<$Res>
    extends _$BudgetCopyWithImpl<$Res, _$BudgetImpl>
    implements _$$BudgetImplCopyWith<$Res> {
  __$$BudgetImplCopyWithImpl(
      _$BudgetImpl _value, $Res Function(_$BudgetImpl) _then)
      : super(_value, _then);

  /// Create a copy of Budget
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? code = null,
    Object? allocatedAmount = null,
    Object? spentAmount = null,
    Object? currency = null,
    Object? startDate = null,
    Object? endDate = null,
    Object? isActive = null,
    Object? description = freezed,
    Object? departmentId = freezed,
    Object? departmentName = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$BudgetImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      code: null == code
          ? _value.code
          : code // ignore: cast_nullable_to_non_nullable
              as String,
      allocatedAmount: null == allocatedAmount
          ? _value.allocatedAmount
          : allocatedAmount // ignore: cast_nullable_to_non_nullable
              as double,
      spentAmount: null == spentAmount
          ? _value.spentAmount
          : spentAmount // ignore: cast_nullable_to_non_nullable
              as double,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: null == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      departmentId: freezed == departmentId
          ? _value.departmentId
          : departmentId // ignore: cast_nullable_to_non_nullable
              as int?,
      departmentName: freezed == departmentName
          ? _value.departmentName
          : departmentName // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$BudgetImpl implements _Budget {
  const _$BudgetImpl(
      {required this.id,
      required this.name,
      required this.code,
      required this.allocatedAmount,
      this.spentAmount = 0.0,
      required this.currency,
      required this.startDate,
      required this.endDate,
      this.isActive = true,
      this.description,
      this.departmentId,
      this.departmentName,
      required this.createdAt,
      this.updatedAt});

  factory _$BudgetImpl.fromJson(Map<String, dynamic> json) =>
      _$$BudgetImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final String code;
  @override
  final double allocatedAmount;
  @override
  @JsonKey()
  final double spentAmount;
  @override
  final String currency;
  @override
  final DateTime startDate;
  @override
  final DateTime endDate;
  @override
  @JsonKey()
  final bool isActive;
  @override
  final String? description;
  @override
  final int? departmentId;
  @override
  final String? departmentName;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'Budget(id: $id, name: $name, code: $code, allocatedAmount: $allocatedAmount, spentAmount: $spentAmount, currency: $currency, startDate: $startDate, endDate: $endDate, isActive: $isActive, description: $description, departmentId: $departmentId, departmentName: $departmentName, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$BudgetImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.code, code) || other.code == code) &&
            (identical(other.allocatedAmount, allocatedAmount) ||
                other.allocatedAmount == allocatedAmount) &&
            (identical(other.spentAmount, spentAmount) ||
                other.spentAmount == spentAmount) &&
            (identical(other.currency, currency) ||
                other.currency == currency) &&
            (identical(other.startDate, startDate) ||
                other.startDate == startDate) &&
            (identical(other.endDate, endDate) || other.endDate == endDate) &&
            (identical(other.isActive, isActive) ||
                other.isActive == isActive) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.departmentId, departmentId) ||
                other.departmentId == departmentId) &&
            (identical(other.departmentName, departmentName) ||
                other.departmentName == departmentName) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      name,
      code,
      allocatedAmount,
      spentAmount,
      currency,
      startDate,
      endDate,
      isActive,
      description,
      departmentId,
      departmentName,
      createdAt,
      updatedAt);

  /// Create a copy of Budget
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$BudgetImplCopyWith<_$BudgetImpl> get copyWith =>
      __$$BudgetImplCopyWithImpl<_$BudgetImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$BudgetImplToJson(
      this,
    );
  }
}

abstract class _Budget implements Budget {
  const factory _Budget(
      {required final int id,
      required final String name,
      required final String code,
      required final double allocatedAmount,
      final double spentAmount,
      required final String currency,
      required final DateTime startDate,
      required final DateTime endDate,
      final bool isActive,
      final String? description,
      final int? departmentId,
      final String? departmentName,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$BudgetImpl;

  factory _Budget.fromJson(Map<String, dynamic> json) = _$BudgetImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  String get code;
  @override
  double get allocatedAmount;
  @override
  double get spentAmount;
  @override
  String get currency;
  @override
  DateTime get startDate;
  @override
  DateTime get endDate;
  @override
  bool get isActive;
  @override
  String? get description;
  @override
  int? get departmentId;
  @override
  String? get departmentName;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of Budget
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$BudgetImplCopyWith<_$BudgetImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Tontine _$TontineFromJson(Map<String, dynamic> json) {
  return _Tontine.fromJson(json);
}

/// @nodoc
mixin _$Tontine {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String get description => throw _privateConstructorUsedError;
  double get contributionAmount => throw _privateConstructorUsedError;
  TontineFrequency get frequency => throw _privateConstructorUsedError;
  int get maxMembers => throw _privateConstructorUsedError;
  int get currentMembers => throw _privateConstructorUsedError;
  DateTime get startDate => throw _privateConstructorUsedError;
  DateTime? get endDate => throw _privateConstructorUsedError;
  TontineStatus get status => throw _privateConstructorUsedError;
  String get currency => throw _privateConstructorUsedError;
  int? get managerId => throw _privateConstructorUsedError;
  String? get managerName => throw _privateConstructorUsedError;
  String? get payoutMethod => throw _privateConstructorUsedError;
  bool get isPublic => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this Tontine to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Tontine
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TontineCopyWith<Tontine> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TontineCopyWith<$Res> {
  factory $TontineCopyWith(Tontine value, $Res Function(Tontine) then) =
      _$TontineCopyWithImpl<$Res, Tontine>;
  @useResult
  $Res call(
      {int id,
      String name,
      String description,
      double contributionAmount,
      TontineFrequency frequency,
      int maxMembers,
      int currentMembers,
      DateTime startDate,
      DateTime? endDate,
      TontineStatus status,
      String currency,
      int? managerId,
      String? managerName,
      String? payoutMethod,
      bool isPublic,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$TontineCopyWithImpl<$Res, $Val extends Tontine>
    implements $TontineCopyWith<$Res> {
  _$TontineCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Tontine
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = null,
    Object? contributionAmount = null,
    Object? frequency = null,
    Object? maxMembers = null,
    Object? currentMembers = null,
    Object? startDate = null,
    Object? endDate = freezed,
    Object? status = null,
    Object? currency = null,
    Object? managerId = freezed,
    Object? managerName = freezed,
    Object? payoutMethod = freezed,
    Object? isPublic = null,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      contributionAmount: null == contributionAmount
          ? _value.contributionAmount
          : contributionAmount // ignore: cast_nullable_to_non_nullable
              as double,
      frequency: null == frequency
          ? _value.frequency
          : frequency // ignore: cast_nullable_to_non_nullable
              as TontineFrequency,
      maxMembers: null == maxMembers
          ? _value.maxMembers
          : maxMembers // ignore: cast_nullable_to_non_nullable
              as int,
      currentMembers: null == currentMembers
          ? _value.currentMembers
          : currentMembers // ignore: cast_nullable_to_non_nullable
              as int,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: freezed == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TontineStatus,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      managerId: freezed == managerId
          ? _value.managerId
          : managerId // ignore: cast_nullable_to_non_nullable
              as int?,
      managerName: freezed == managerName
          ? _value.managerName
          : managerName // ignore: cast_nullable_to_non_nullable
              as String?,
      payoutMethod: freezed == payoutMethod
          ? _value.payoutMethod
          : payoutMethod // ignore: cast_nullable_to_non_nullable
              as String?,
      isPublic: null == isPublic
          ? _value.isPublic
          : isPublic // ignore: cast_nullable_to_non_nullable
              as bool,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TontineImplCopyWith<$Res> implements $TontineCopyWith<$Res> {
  factory _$$TontineImplCopyWith(
          _$TontineImpl value, $Res Function(_$TontineImpl) then) =
      __$$TontineImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      String description,
      double contributionAmount,
      TontineFrequency frequency,
      int maxMembers,
      int currentMembers,
      DateTime startDate,
      DateTime? endDate,
      TontineStatus status,
      String currency,
      int? managerId,
      String? managerName,
      String? payoutMethod,
      bool isPublic,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$TontineImplCopyWithImpl<$Res>
    extends _$TontineCopyWithImpl<$Res, _$TontineImpl>
    implements _$$TontineImplCopyWith<$Res> {
  __$$TontineImplCopyWithImpl(
      _$TontineImpl _value, $Res Function(_$TontineImpl) _then)
      : super(_value, _then);

  /// Create a copy of Tontine
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = null,
    Object? contributionAmount = null,
    Object? frequency = null,
    Object? maxMembers = null,
    Object? currentMembers = null,
    Object? startDate = null,
    Object? endDate = freezed,
    Object? status = null,
    Object? currency = null,
    Object? managerId = freezed,
    Object? managerName = freezed,
    Object? payoutMethod = freezed,
    Object? isPublic = null,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$TontineImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: null == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String,
      contributionAmount: null == contributionAmount
          ? _value.contributionAmount
          : contributionAmount // ignore: cast_nullable_to_non_nullable
              as double,
      frequency: null == frequency
          ? _value.frequency
          : frequency // ignore: cast_nullable_to_non_nullable
              as TontineFrequency,
      maxMembers: null == maxMembers
          ? _value.maxMembers
          : maxMembers // ignore: cast_nullable_to_non_nullable
              as int,
      currentMembers: null == currentMembers
          ? _value.currentMembers
          : currentMembers // ignore: cast_nullable_to_non_nullable
              as int,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: freezed == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TontineStatus,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      managerId: freezed == managerId
          ? _value.managerId
          : managerId // ignore: cast_nullable_to_non_nullable
              as int?,
      managerName: freezed == managerName
          ? _value.managerName
          : managerName // ignore: cast_nullable_to_non_nullable
              as String?,
      payoutMethod: freezed == payoutMethod
          ? _value.payoutMethod
          : payoutMethod // ignore: cast_nullable_to_non_nullable
              as String?,
      isPublic: null == isPublic
          ? _value.isPublic
          : isPublic // ignore: cast_nullable_to_non_nullable
              as bool,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TontineImpl implements _Tontine {
  const _$TontineImpl(
      {required this.id,
      required this.name,
      required this.description,
      required this.contributionAmount,
      required this.frequency,
      required this.maxMembers,
      this.currentMembers = 0,
      required this.startDate,
      this.endDate,
      required this.status,
      required this.currency,
      this.managerId,
      this.managerName,
      this.payoutMethod,
      this.isPublic = false,
      required this.createdAt,
      this.updatedAt});

  factory _$TontineImpl.fromJson(Map<String, dynamic> json) =>
      _$$TontineImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final String description;
  @override
  final double contributionAmount;
  @override
  final TontineFrequency frequency;
  @override
  final int maxMembers;
  @override
  @JsonKey()
  final int currentMembers;
  @override
  final DateTime startDate;
  @override
  final DateTime? endDate;
  @override
  final TontineStatus status;
  @override
  final String currency;
  @override
  final int? managerId;
  @override
  final String? managerName;
  @override
  final String? payoutMethod;
  @override
  @JsonKey()
  final bool isPublic;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'Tontine(id: $id, name: $name, description: $description, contributionAmount: $contributionAmount, frequency: $frequency, maxMembers: $maxMembers, currentMembers: $currentMembers, startDate: $startDate, endDate: $endDate, status: $status, currency: $currency, managerId: $managerId, managerName: $managerName, payoutMethod: $payoutMethod, isPublic: $isPublic, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TontineImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.contributionAmount, contributionAmount) ||
                other.contributionAmount == contributionAmount) &&
            (identical(other.frequency, frequency) ||
                other.frequency == frequency) &&
            (identical(other.maxMembers, maxMembers) ||
                other.maxMembers == maxMembers) &&
            (identical(other.currentMembers, currentMembers) ||
                other.currentMembers == currentMembers) &&
            (identical(other.startDate, startDate) ||
                other.startDate == startDate) &&
            (identical(other.endDate, endDate) || other.endDate == endDate) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.currency, currency) ||
                other.currency == currency) &&
            (identical(other.managerId, managerId) ||
                other.managerId == managerId) &&
            (identical(other.managerName, managerName) ||
                other.managerName == managerName) &&
            (identical(other.payoutMethod, payoutMethod) ||
                other.payoutMethod == payoutMethod) &&
            (identical(other.isPublic, isPublic) ||
                other.isPublic == isPublic) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      name,
      description,
      contributionAmount,
      frequency,
      maxMembers,
      currentMembers,
      startDate,
      endDate,
      status,
      currency,
      managerId,
      managerName,
      payoutMethod,
      isPublic,
      createdAt,
      updatedAt);

  /// Create a copy of Tontine
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TontineImplCopyWith<_$TontineImpl> get copyWith =>
      __$$TontineImplCopyWithImpl<_$TontineImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TontineImplToJson(
      this,
    );
  }
}

abstract class _Tontine implements Tontine {
  const factory _Tontine(
      {required final int id,
      required final String name,
      required final String description,
      required final double contributionAmount,
      required final TontineFrequency frequency,
      required final int maxMembers,
      final int currentMembers,
      required final DateTime startDate,
      final DateTime? endDate,
      required final TontineStatus status,
      required final String currency,
      final int? managerId,
      final String? managerName,
      final String? payoutMethod,
      final bool isPublic,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$TontineImpl;

  factory _Tontine.fromJson(Map<String, dynamic> json) = _$TontineImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  String get description;
  @override
  double get contributionAmount;
  @override
  TontineFrequency get frequency;
  @override
  int get maxMembers;
  @override
  int get currentMembers;
  @override
  DateTime get startDate;
  @override
  DateTime? get endDate;
  @override
  TontineStatus get status;
  @override
  String get currency;
  @override
  int? get managerId;
  @override
  String? get managerName;
  @override
  String? get payoutMethod;
  @override
  bool get isPublic;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of Tontine
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TontineImplCopyWith<_$TontineImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

TontineMember _$TontineMemberFromJson(Map<String, dynamic> json) {
  return _TontineMember.fromJson(json);
}

/// @nodoc
mixin _$TontineMember {
  int get id => throw _privateConstructorUsedError;
  int get tontineId => throw _privateConstructorUsedError;
  int get personId => throw _privateConstructorUsedError;
  String get personName => throw _privateConstructorUsedError;
  TontineMemberRole get role => throw _privateConstructorUsedError;
  TontineMemberStatus get status => throw _privateConstructorUsedError;
  int get position => throw _privateConstructorUsedError;
  DateTime? get joinedAt => throw _privateConstructorUsedError;
  DateTime? get lastPaymentAt => throw _privateConstructorUsedError;
  double get totalContributed => throw _privateConstructorUsedError;
  int get paymentsMade => throw _privateConstructorUsedError;
  int get paymentsMissed => throw _privateConstructorUsedError;

  /// Serializes this TontineMember to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of TontineMember
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TontineMemberCopyWith<TontineMember> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TontineMemberCopyWith<$Res> {
  factory $TontineMemberCopyWith(
          TontineMember value, $Res Function(TontineMember) then) =
      _$TontineMemberCopyWithImpl<$Res, TontineMember>;
  @useResult
  $Res call(
      {int id,
      int tontineId,
      int personId,
      String personName,
      TontineMemberRole role,
      TontineMemberStatus status,
      int position,
      DateTime? joinedAt,
      DateTime? lastPaymentAt,
      double totalContributed,
      int paymentsMade,
      int paymentsMissed});
}

/// @nodoc
class _$TontineMemberCopyWithImpl<$Res, $Val extends TontineMember>
    implements $TontineMemberCopyWith<$Res> {
  _$TontineMemberCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of TontineMember
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? tontineId = null,
    Object? personId = null,
    Object? personName = null,
    Object? role = null,
    Object? status = null,
    Object? position = null,
    Object? joinedAt = freezed,
    Object? lastPaymentAt = freezed,
    Object? totalContributed = null,
    Object? paymentsMade = null,
    Object? paymentsMissed = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      tontineId: null == tontineId
          ? _value.tontineId
          : tontineId // ignore: cast_nullable_to_non_nullable
              as int,
      personId: null == personId
          ? _value.personId
          : personId // ignore: cast_nullable_to_non_nullable
              as int,
      personName: null == personName
          ? _value.personName
          : personName // ignore: cast_nullable_to_non_nullable
              as String,
      role: null == role
          ? _value.role
          : role // ignore: cast_nullable_to_non_nullable
              as TontineMemberRole,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TontineMemberStatus,
      position: null == position
          ? _value.position
          : position // ignore: cast_nullable_to_non_nullable
              as int,
      joinedAt: freezed == joinedAt
          ? _value.joinedAt
          : joinedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      lastPaymentAt: freezed == lastPaymentAt
          ? _value.lastPaymentAt
          : lastPaymentAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      totalContributed: null == totalContributed
          ? _value.totalContributed
          : totalContributed // ignore: cast_nullable_to_non_nullable
              as double,
      paymentsMade: null == paymentsMade
          ? _value.paymentsMade
          : paymentsMade // ignore: cast_nullable_to_non_nullable
              as int,
      paymentsMissed: null == paymentsMissed
          ? _value.paymentsMissed
          : paymentsMissed // ignore: cast_nullable_to_non_nullable
              as int,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TontineMemberImplCopyWith<$Res>
    implements $TontineMemberCopyWith<$Res> {
  factory _$$TontineMemberImplCopyWith(
          _$TontineMemberImpl value, $Res Function(_$TontineMemberImpl) then) =
      __$$TontineMemberImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int tontineId,
      int personId,
      String personName,
      TontineMemberRole role,
      TontineMemberStatus status,
      int position,
      DateTime? joinedAt,
      DateTime? lastPaymentAt,
      double totalContributed,
      int paymentsMade,
      int paymentsMissed});
}

/// @nodoc
class __$$TontineMemberImplCopyWithImpl<$Res>
    extends _$TontineMemberCopyWithImpl<$Res, _$TontineMemberImpl>
    implements _$$TontineMemberImplCopyWith<$Res> {
  __$$TontineMemberImplCopyWithImpl(
      _$TontineMemberImpl _value, $Res Function(_$TontineMemberImpl) _then)
      : super(_value, _then);

  /// Create a copy of TontineMember
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? tontineId = null,
    Object? personId = null,
    Object? personName = null,
    Object? role = null,
    Object? status = null,
    Object? position = null,
    Object? joinedAt = freezed,
    Object? lastPaymentAt = freezed,
    Object? totalContributed = null,
    Object? paymentsMade = null,
    Object? paymentsMissed = null,
  }) {
    return _then(_$TontineMemberImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      tontineId: null == tontineId
          ? _value.tontineId
          : tontineId // ignore: cast_nullable_to_non_nullable
              as int,
      personId: null == personId
          ? _value.personId
          : personId // ignore: cast_nullable_to_non_nullable
              as int,
      personName: null == personName
          ? _value.personName
          : personName // ignore: cast_nullable_to_non_nullable
              as String,
      role: null == role
          ? _value.role
          : role // ignore: cast_nullable_to_non_nullable
              as TontineMemberRole,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TontineMemberStatus,
      position: null == position
          ? _value.position
          : position // ignore: cast_nullable_to_non_nullable
              as int,
      joinedAt: freezed == joinedAt
          ? _value.joinedAt
          : joinedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      lastPaymentAt: freezed == lastPaymentAt
          ? _value.lastPaymentAt
          : lastPaymentAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      totalContributed: null == totalContributed
          ? _value.totalContributed
          : totalContributed // ignore: cast_nullable_to_non_nullable
              as double,
      paymentsMade: null == paymentsMade
          ? _value.paymentsMade
          : paymentsMade // ignore: cast_nullable_to_non_nullable
              as int,
      paymentsMissed: null == paymentsMissed
          ? _value.paymentsMissed
          : paymentsMissed // ignore: cast_nullable_to_non_nullable
              as int,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TontineMemberImpl implements _TontineMember {
  const _$TontineMemberImpl(
      {required this.id,
      required this.tontineId,
      required this.personId,
      required this.personName,
      required this.role,
      required this.status,
      required this.position,
      this.joinedAt,
      this.lastPaymentAt,
      this.totalContributed = 0.0,
      this.paymentsMade = 0,
      this.paymentsMissed = 0});

  factory _$TontineMemberImpl.fromJson(Map<String, dynamic> json) =>
      _$$TontineMemberImplFromJson(json);

  @override
  final int id;
  @override
  final int tontineId;
  @override
  final int personId;
  @override
  final String personName;
  @override
  final TontineMemberRole role;
  @override
  final TontineMemberStatus status;
  @override
  final int position;
  @override
  final DateTime? joinedAt;
  @override
  final DateTime? lastPaymentAt;
  @override
  @JsonKey()
  final double totalContributed;
  @override
  @JsonKey()
  final int paymentsMade;
  @override
  @JsonKey()
  final int paymentsMissed;

  @override
  String toString() {
    return 'TontineMember(id: $id, tontineId: $tontineId, personId: $personId, personName: $personName, role: $role, status: $status, position: $position, joinedAt: $joinedAt, lastPaymentAt: $lastPaymentAt, totalContributed: $totalContributed, paymentsMade: $paymentsMade, paymentsMissed: $paymentsMissed)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TontineMemberImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.tontineId, tontineId) ||
                other.tontineId == tontineId) &&
            (identical(other.personId, personId) ||
                other.personId == personId) &&
            (identical(other.personName, personName) ||
                other.personName == personName) &&
            (identical(other.role, role) || other.role == role) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.position, position) ||
                other.position == position) &&
            (identical(other.joinedAt, joinedAt) ||
                other.joinedAt == joinedAt) &&
            (identical(other.lastPaymentAt, lastPaymentAt) ||
                other.lastPaymentAt == lastPaymentAt) &&
            (identical(other.totalContributed, totalContributed) ||
                other.totalContributed == totalContributed) &&
            (identical(other.paymentsMade, paymentsMade) ||
                other.paymentsMade == paymentsMade) &&
            (identical(other.paymentsMissed, paymentsMissed) ||
                other.paymentsMissed == paymentsMissed));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      tontineId,
      personId,
      personName,
      role,
      status,
      position,
      joinedAt,
      lastPaymentAt,
      totalContributed,
      paymentsMade,
      paymentsMissed);

  /// Create a copy of TontineMember
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TontineMemberImplCopyWith<_$TontineMemberImpl> get copyWith =>
      __$$TontineMemberImplCopyWithImpl<_$TontineMemberImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TontineMemberImplToJson(
      this,
    );
  }
}

abstract class _TontineMember implements TontineMember {
  const factory _TontineMember(
      {required final int id,
      required final int tontineId,
      required final int personId,
      required final String personName,
      required final TontineMemberRole role,
      required final TontineMemberStatus status,
      required final int position,
      final DateTime? joinedAt,
      final DateTime? lastPaymentAt,
      final double totalContributed,
      final int paymentsMade,
      final int paymentsMissed}) = _$TontineMemberImpl;

  factory _TontineMember.fromJson(Map<String, dynamic> json) =
      _$TontineMemberImpl.fromJson;

  @override
  int get id;
  @override
  int get tontineId;
  @override
  int get personId;
  @override
  String get personName;
  @override
  TontineMemberRole get role;
  @override
  TontineMemberStatus get status;
  @override
  int get position;
  @override
  DateTime? get joinedAt;
  @override
  DateTime? get lastPaymentAt;
  @override
  double get totalContributed;
  @override
  int get paymentsMade;
  @override
  int get paymentsMissed;

  /// Create a copy of TontineMember
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TontineMemberImplCopyWith<_$TontineMemberImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

TontinePayout _$TontinePayoutFromJson(Map<String, dynamic> json) {
  return _TontinePayout.fromJson(json);
}

/// @nodoc
mixin _$TontinePayout {
  int get id => throw _privateConstructorUsedError;
  int get tontineId => throw _privateConstructorUsedError;
  int get memberId => throw _privateConstructorUsedError;
  String get memberName => throw _privateConstructorUsedError;
  double get amount => throw _privateConstructorUsedError;
  DateTime get scheduledDate => throw _privateConstructorUsedError;
  DateTime? get paidDate => throw _privateConstructorUsedError;
  TontinePayoutStatus get status => throw _privateConstructorUsedError;
  String? get paymentReference => throw _privateConstructorUsedError;

  /// Serializes this TontinePayout to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of TontinePayout
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $TontinePayoutCopyWith<TontinePayout> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $TontinePayoutCopyWith<$Res> {
  factory $TontinePayoutCopyWith(
          TontinePayout value, $Res Function(TontinePayout) then) =
      _$TontinePayoutCopyWithImpl<$Res, TontinePayout>;
  @useResult
  $Res call(
      {int id,
      int tontineId,
      int memberId,
      String memberName,
      double amount,
      DateTime scheduledDate,
      DateTime? paidDate,
      TontinePayoutStatus status,
      String? paymentReference});
}

/// @nodoc
class _$TontinePayoutCopyWithImpl<$Res, $Val extends TontinePayout>
    implements $TontinePayoutCopyWith<$Res> {
  _$TontinePayoutCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of TontinePayout
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? tontineId = null,
    Object? memberId = null,
    Object? memberName = null,
    Object? amount = null,
    Object? scheduledDate = null,
    Object? paidDate = freezed,
    Object? status = null,
    Object? paymentReference = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      tontineId: null == tontineId
          ? _value.tontineId
          : tontineId // ignore: cast_nullable_to_non_nullable
              as int,
      memberId: null == memberId
          ? _value.memberId
          : memberId // ignore: cast_nullable_to_non_nullable
              as int,
      memberName: null == memberName
          ? _value.memberName
          : memberName // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as double,
      scheduledDate: null == scheduledDate
          ? _value.scheduledDate
          : scheduledDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      paidDate: freezed == paidDate
          ? _value.paidDate
          : paidDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TontinePayoutStatus,
      paymentReference: freezed == paymentReference
          ? _value.paymentReference
          : paymentReference // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$TontinePayoutImplCopyWith<$Res>
    implements $TontinePayoutCopyWith<$Res> {
  factory _$$TontinePayoutImplCopyWith(
          _$TontinePayoutImpl value, $Res Function(_$TontinePayoutImpl) then) =
      __$$TontinePayoutImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int tontineId,
      int memberId,
      String memberName,
      double amount,
      DateTime scheduledDate,
      DateTime? paidDate,
      TontinePayoutStatus status,
      String? paymentReference});
}

/// @nodoc
class __$$TontinePayoutImplCopyWithImpl<$Res>
    extends _$TontinePayoutCopyWithImpl<$Res, _$TontinePayoutImpl>
    implements _$$TontinePayoutImplCopyWith<$Res> {
  __$$TontinePayoutImplCopyWithImpl(
      _$TontinePayoutImpl _value, $Res Function(_$TontinePayoutImpl) _then)
      : super(_value, _then);

  /// Create a copy of TontinePayout
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? tontineId = null,
    Object? memberId = null,
    Object? memberName = null,
    Object? amount = null,
    Object? scheduledDate = null,
    Object? paidDate = freezed,
    Object? status = null,
    Object? paymentReference = freezed,
  }) {
    return _then(_$TontinePayoutImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      tontineId: null == tontineId
          ? _value.tontineId
          : tontineId // ignore: cast_nullable_to_non_nullable
              as int,
      memberId: null == memberId
          ? _value.memberId
          : memberId // ignore: cast_nullable_to_non_nullable
              as int,
      memberName: null == memberName
          ? _value.memberName
          : memberName // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as double,
      scheduledDate: null == scheduledDate
          ? _value.scheduledDate
          : scheduledDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      paidDate: freezed == paidDate
          ? _value.paidDate
          : paidDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as TontinePayoutStatus,
      paymentReference: freezed == paymentReference
          ? _value.paymentReference
          : paymentReference // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$TontinePayoutImpl implements _TontinePayout {
  const _$TontinePayoutImpl(
      {required this.id,
      required this.tontineId,
      required this.memberId,
      required this.memberName,
      required this.amount,
      required this.scheduledDate,
      this.paidDate,
      required this.status,
      this.paymentReference});

  factory _$TontinePayoutImpl.fromJson(Map<String, dynamic> json) =>
      _$$TontinePayoutImplFromJson(json);

  @override
  final int id;
  @override
  final int tontineId;
  @override
  final int memberId;
  @override
  final String memberName;
  @override
  final double amount;
  @override
  final DateTime scheduledDate;
  @override
  final DateTime? paidDate;
  @override
  final TontinePayoutStatus status;
  @override
  final String? paymentReference;

  @override
  String toString() {
    return 'TontinePayout(id: $id, tontineId: $tontineId, memberId: $memberId, memberName: $memberName, amount: $amount, scheduledDate: $scheduledDate, paidDate: $paidDate, status: $status, paymentReference: $paymentReference)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$TontinePayoutImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.tontineId, tontineId) ||
                other.tontineId == tontineId) &&
            (identical(other.memberId, memberId) ||
                other.memberId == memberId) &&
            (identical(other.memberName, memberName) ||
                other.memberName == memberName) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.scheduledDate, scheduledDate) ||
                other.scheduledDate == scheduledDate) &&
            (identical(other.paidDate, paidDate) ||
                other.paidDate == paidDate) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.paymentReference, paymentReference) ||
                other.paymentReference == paymentReference));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, tontineId, memberId,
      memberName, amount, scheduledDate, paidDate, status, paymentReference);

  /// Create a copy of TontinePayout
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$TontinePayoutImplCopyWith<_$TontinePayoutImpl> get copyWith =>
      __$$TontinePayoutImplCopyWithImpl<_$TontinePayoutImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$TontinePayoutImplToJson(
      this,
    );
  }
}

abstract class _TontinePayout implements TontinePayout {
  const factory _TontinePayout(
      {required final int id,
      required final int tontineId,
      required final int memberId,
      required final String memberName,
      required final double amount,
      required final DateTime scheduledDate,
      final DateTime? paidDate,
      required final TontinePayoutStatus status,
      final String? paymentReference}) = _$TontinePayoutImpl;

  factory _TontinePayout.fromJson(Map<String, dynamic> json) =
      _$TontinePayoutImpl.fromJson;

  @override
  int get id;
  @override
  int get tontineId;
  @override
  int get memberId;
  @override
  String get memberName;
  @override
  double get amount;
  @override
  DateTime get scheduledDate;
  @override
  DateTime? get paidDate;
  @override
  TontinePayoutStatus get status;
  @override
  String? get paymentReference;

  /// Create a copy of TontinePayout
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$TontinePayoutImplCopyWith<_$TontinePayoutImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Donation _$DonationFromJson(Map<String, dynamic> json) {
  return _Donation.fromJson(json);
}

/// @nodoc
mixin _$Donation {
  int get id => throw _privateConstructorUsedError;
  String get reference => throw _privateConstructorUsedError;
  double get amount => throw _privateConstructorUsedError;
  String get currency => throw _privateConstructorUsedError;
  String? get donorName => throw _privateConstructorUsedError;
  String? get donorEmail => throw _privateConstructorUsedError;
  String? get donorPhone => throw _privateConstructorUsedError;
  bool get isAnonymous => throw _privateConstructorUsedError;
  String? get message => throw _privateConstructorUsedError;
  DateTime get date => throw _privateConstructorUsedError;
  DonationSource get source => throw _privateConstructorUsedError;
  String? get paymentReference => throw _privateConstructorUsedError;
  DonationStatus get status => throw _privateConstructorUsedError;
  String? get campaignId => throw _privateConstructorUsedError;
  String? get campaignName => throw _privateConstructorUsedError;
  DateTime? get receiptSentAt => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;

  /// Serializes this Donation to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Donation
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $DonationCopyWith<Donation> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $DonationCopyWith<$Res> {
  factory $DonationCopyWith(Donation value, $Res Function(Donation) then) =
      _$DonationCopyWithImpl<$Res, Donation>;
  @useResult
  $Res call(
      {int id,
      String reference,
      double amount,
      String currency,
      String? donorName,
      String? donorEmail,
      String? donorPhone,
      bool isAnonymous,
      String? message,
      DateTime date,
      DonationSource source,
      String? paymentReference,
      DonationStatus status,
      String? campaignId,
      String? campaignName,
      DateTime? receiptSentAt,
      DateTime createdAt});
}

/// @nodoc
class _$DonationCopyWithImpl<$Res, $Val extends Donation>
    implements $DonationCopyWith<$Res> {
  _$DonationCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Donation
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? reference = null,
    Object? amount = null,
    Object? currency = null,
    Object? donorName = freezed,
    Object? donorEmail = freezed,
    Object? donorPhone = freezed,
    Object? isAnonymous = null,
    Object? message = freezed,
    Object? date = null,
    Object? source = null,
    Object? paymentReference = freezed,
    Object? status = null,
    Object? campaignId = freezed,
    Object? campaignName = freezed,
    Object? receiptSentAt = freezed,
    Object? createdAt = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      reference: null == reference
          ? _value.reference
          : reference // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as double,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      donorName: freezed == donorName
          ? _value.donorName
          : donorName // ignore: cast_nullable_to_non_nullable
              as String?,
      donorEmail: freezed == donorEmail
          ? _value.donorEmail
          : donorEmail // ignore: cast_nullable_to_non_nullable
              as String?,
      donorPhone: freezed == donorPhone
          ? _value.donorPhone
          : donorPhone // ignore: cast_nullable_to_non_nullable
              as String?,
      isAnonymous: null == isAnonymous
          ? _value.isAnonymous
          : isAnonymous // ignore: cast_nullable_to_non_nullable
              as bool,
      message: freezed == message
          ? _value.message
          : message // ignore: cast_nullable_to_non_nullable
              as String?,
      date: null == date
          ? _value.date
          : date // ignore: cast_nullable_to_non_nullable
              as DateTime,
      source: null == source
          ? _value.source
          : source // ignore: cast_nullable_to_non_nullable
              as DonationSource,
      paymentReference: freezed == paymentReference
          ? _value.paymentReference
          : paymentReference // ignore: cast_nullable_to_non_nullable
              as String?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as DonationStatus,
      campaignId: freezed == campaignId
          ? _value.campaignId
          : campaignId // ignore: cast_nullable_to_non_nullable
              as String?,
      campaignName: freezed == campaignName
          ? _value.campaignName
          : campaignName // ignore: cast_nullable_to_non_nullable
              as String?,
      receiptSentAt: freezed == receiptSentAt
          ? _value.receiptSentAt
          : receiptSentAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$DonationImplCopyWith<$Res>
    implements $DonationCopyWith<$Res> {
  factory _$$DonationImplCopyWith(
          _$DonationImpl value, $Res Function(_$DonationImpl) then) =
      __$$DonationImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String reference,
      double amount,
      String currency,
      String? donorName,
      String? donorEmail,
      String? donorPhone,
      bool isAnonymous,
      String? message,
      DateTime date,
      DonationSource source,
      String? paymentReference,
      DonationStatus status,
      String? campaignId,
      String? campaignName,
      DateTime? receiptSentAt,
      DateTime createdAt});
}

/// @nodoc
class __$$DonationImplCopyWithImpl<$Res>
    extends _$DonationCopyWithImpl<$Res, _$DonationImpl>
    implements _$$DonationImplCopyWith<$Res> {
  __$$DonationImplCopyWithImpl(
      _$DonationImpl _value, $Res Function(_$DonationImpl) _then)
      : super(_value, _then);

  /// Create a copy of Donation
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? reference = null,
    Object? amount = null,
    Object? currency = null,
    Object? donorName = freezed,
    Object? donorEmail = freezed,
    Object? donorPhone = freezed,
    Object? isAnonymous = null,
    Object? message = freezed,
    Object? date = null,
    Object? source = null,
    Object? paymentReference = freezed,
    Object? status = null,
    Object? campaignId = freezed,
    Object? campaignName = freezed,
    Object? receiptSentAt = freezed,
    Object? createdAt = null,
  }) {
    return _then(_$DonationImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      reference: null == reference
          ? _value.reference
          : reference // ignore: cast_nullable_to_non_nullable
              as String,
      amount: null == amount
          ? _value.amount
          : amount // ignore: cast_nullable_to_non_nullable
              as double,
      currency: null == currency
          ? _value.currency
          : currency // ignore: cast_nullable_to_non_nullable
              as String,
      donorName: freezed == donorName
          ? _value.donorName
          : donorName // ignore: cast_nullable_to_non_nullable
              as String?,
      donorEmail: freezed == donorEmail
          ? _value.donorEmail
          : donorEmail // ignore: cast_nullable_to_non_nullable
              as String?,
      donorPhone: freezed == donorPhone
          ? _value.donorPhone
          : donorPhone // ignore: cast_nullable_to_non_nullable
              as String?,
      isAnonymous: null == isAnonymous
          ? _value.isAnonymous
          : isAnonymous // ignore: cast_nullable_to_non_nullable
              as bool,
      message: freezed == message
          ? _value.message
          : message // ignore: cast_nullable_to_non_nullable
              as String?,
      date: null == date
          ? _value.date
          : date // ignore: cast_nullable_to_non_nullable
              as DateTime,
      source: null == source
          ? _value.source
          : source // ignore: cast_nullable_to_non_nullable
              as DonationSource,
      paymentReference: freezed == paymentReference
          ? _value.paymentReference
          : paymentReference // ignore: cast_nullable_to_non_nullable
              as String?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as DonationStatus,
      campaignId: freezed == campaignId
          ? _value.campaignId
          : campaignId // ignore: cast_nullable_to_non_nullable
              as String?,
      campaignName: freezed == campaignName
          ? _value.campaignName
          : campaignName // ignore: cast_nullable_to_non_nullable
              as String?,
      receiptSentAt: freezed == receiptSentAt
          ? _value.receiptSentAt
          : receiptSentAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$DonationImpl implements _Donation {
  const _$DonationImpl(
      {required this.id,
      required this.reference,
      required this.amount,
      required this.currency,
      this.donorName,
      this.donorEmail,
      this.donorPhone,
      this.isAnonymous = false,
      this.message,
      required this.date,
      required this.source,
      this.paymentReference,
      required this.status,
      this.campaignId,
      this.campaignName,
      this.receiptSentAt,
      required this.createdAt});

  factory _$DonationImpl.fromJson(Map<String, dynamic> json) =>
      _$$DonationImplFromJson(json);

  @override
  final int id;
  @override
  final String reference;
  @override
  final double amount;
  @override
  final String currency;
  @override
  final String? donorName;
  @override
  final String? donorEmail;
  @override
  final String? donorPhone;
  @override
  @JsonKey()
  final bool isAnonymous;
  @override
  final String? message;
  @override
  final DateTime date;
  @override
  final DonationSource source;
  @override
  final String? paymentReference;
  @override
  final DonationStatus status;
  @override
  final String? campaignId;
  @override
  final String? campaignName;
  @override
  final DateTime? receiptSentAt;
  @override
  final DateTime createdAt;

  @override
  String toString() {
    return 'Donation(id: $id, reference: $reference, amount: $amount, currency: $currency, donorName: $donorName, donorEmail: $donorEmail, donorPhone: $donorPhone, isAnonymous: $isAnonymous, message: $message, date: $date, source: $source, paymentReference: $paymentReference, status: $status, campaignId: $campaignId, campaignName: $campaignName, receiptSentAt: $receiptSentAt, createdAt: $createdAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$DonationImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.reference, reference) ||
                other.reference == reference) &&
            (identical(other.amount, amount) || other.amount == amount) &&
            (identical(other.currency, currency) ||
                other.currency == currency) &&
            (identical(other.donorName, donorName) ||
                other.donorName == donorName) &&
            (identical(other.donorEmail, donorEmail) ||
                other.donorEmail == donorEmail) &&
            (identical(other.donorPhone, donorPhone) ||
                other.donorPhone == donorPhone) &&
            (identical(other.isAnonymous, isAnonymous) ||
                other.isAnonymous == isAnonymous) &&
            (identical(other.message, message) || other.message == message) &&
            (identical(other.date, date) || other.date == date) &&
            (identical(other.source, source) || other.source == source) &&
            (identical(other.paymentReference, paymentReference) ||
                other.paymentReference == paymentReference) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.campaignId, campaignId) ||
                other.campaignId == campaignId) &&
            (identical(other.campaignName, campaignName) ||
                other.campaignName == campaignName) &&
            (identical(other.receiptSentAt, receiptSentAt) ||
                other.receiptSentAt == receiptSentAt) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      reference,
      amount,
      currency,
      donorName,
      donorEmail,
      donorPhone,
      isAnonymous,
      message,
      date,
      source,
      paymentReference,
      status,
      campaignId,
      campaignName,
      receiptSentAt,
      createdAt);

  /// Create a copy of Donation
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$DonationImplCopyWith<_$DonationImpl> get copyWith =>
      __$$DonationImplCopyWithImpl<_$DonationImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$DonationImplToJson(
      this,
    );
  }
}

abstract class _Donation implements Donation {
  const factory _Donation(
      {required final int id,
      required final String reference,
      required final double amount,
      required final String currency,
      final String? donorName,
      final String? donorEmail,
      final String? donorPhone,
      final bool isAnonymous,
      final String? message,
      required final DateTime date,
      required final DonationSource source,
      final String? paymentReference,
      required final DonationStatus status,
      final String? campaignId,
      final String? campaignName,
      final DateTime? receiptSentAt,
      required final DateTime createdAt}) = _$DonationImpl;

  factory _Donation.fromJson(Map<String, dynamic> json) =
      _$DonationImpl.fromJson;

  @override
  int get id;
  @override
  String get reference;
  @override
  double get amount;
  @override
  String get currency;
  @override
  String? get donorName;
  @override
  String? get donorEmail;
  @override
  String? get donorPhone;
  @override
  bool get isAnonymous;
  @override
  String? get message;
  @override
  DateTime get date;
  @override
  DonationSource get source;
  @override
  String? get paymentReference;
  @override
  DonationStatus get status;
  @override
  String? get campaignId;
  @override
  String? get campaignName;
  @override
  DateTime? get receiptSentAt;
  @override
  DateTime get createdAt;

  /// Create a copy of Donation
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$DonationImplCopyWith<_$DonationImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
