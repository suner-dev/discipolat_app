// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'health_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

T _$identity<T>(T value) => value;

final _privateConstructorUsedError = UnsupportedError(
    'It seems like you constructed your class using `MyClass._()`. This constructor is only meant to be used by freezed and you are not supposed to need it nor use it.\nPlease check the documentation here for more information: https://github.com/rrousselGit/freezed#adding-getters-and-methods-to-our-models');

Patient _$PatientFromJson(Map<String, dynamic> json) {
  return _Patient.fromJson(json);
}

/// @nodoc
mixin _$Patient {
  int get id => throw _privateConstructorUsedError;
  String get firstName => throw _privateConstructorUsedError;
  String get lastName => throw _privateConstructorUsedError;
  DateTime get dateOfBirth => throw _privateConstructorUsedError;
  String get gender => throw _privateConstructorUsedError;
  String? get phone => throw _privateConstructorUsedError;
  String? get email => throw _privateConstructorUsedError;
  String? get address => throw _privateConstructorUsedError;
  String? get emergencyContactName => throw _privateConstructorUsedError;
  String? get emergencyContactPhone => throw _privateConstructorUsedError;
  String? get bloodType => throw _privateConstructorUsedError;
  List<String>? get allergies => throw _privateConstructorUsedError;
  List<String>? get chronicConditions => throw _privateConstructorUsedError;
  List<String>? get currentMedications => throw _privateConstructorUsedError;
  String? get insuranceProvider => throw _privateConstructorUsedError;
  String? get insuranceNumber => throw _privateConstructorUsedError;
  String? get photoUrl => throw _privateConstructorUsedError;
  PatientStatus get status => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;
  int? get familyId => throw _privateConstructorUsedError;
  String? get familyName => throw _privateConstructorUsedError;

  /// Serializes this Patient to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Patient
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $PatientCopyWith<Patient> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $PatientCopyWith<$Res> {
  factory $PatientCopyWith(Patient value, $Res Function(Patient) then) =
      _$PatientCopyWithImpl<$Res, Patient>;
  @useResult
  $Res call(
      {int id,
      String firstName,
      String lastName,
      DateTime dateOfBirth,
      String gender,
      String? phone,
      String? email,
      String? address,
      String? emergencyContactName,
      String? emergencyContactPhone,
      String? bloodType,
      List<String>? allergies,
      List<String>? chronicConditions,
      List<String>? currentMedications,
      String? insuranceProvider,
      String? insuranceNumber,
      String? photoUrl,
      PatientStatus status,
      String? notes,
      DateTime createdAt,
      DateTime? updatedAt,
      int? familyId,
      String? familyName});
}

/// @nodoc
class _$PatientCopyWithImpl<$Res, $Val extends Patient>
    implements $PatientCopyWith<$Res> {
  _$PatientCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Patient
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? firstName = null,
    Object? lastName = null,
    Object? dateOfBirth = null,
    Object? gender = null,
    Object? phone = freezed,
    Object? email = freezed,
    Object? address = freezed,
    Object? emergencyContactName = freezed,
    Object? emergencyContactPhone = freezed,
    Object? bloodType = freezed,
    Object? allergies = freezed,
    Object? chronicConditions = freezed,
    Object? currentMedications = freezed,
    Object? insuranceProvider = freezed,
    Object? insuranceNumber = freezed,
    Object? photoUrl = freezed,
    Object? status = null,
    Object? notes = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? familyId = freezed,
    Object? familyName = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      firstName: null == firstName
          ? _value.firstName
          : firstName // ignore: cast_nullable_to_non_nullable
              as String,
      lastName: null == lastName
          ? _value.lastName
          : lastName // ignore: cast_nullable_to_non_nullable
              as String,
      dateOfBirth: null == dateOfBirth
          ? _value.dateOfBirth
          : dateOfBirth // ignore: cast_nullable_to_non_nullable
              as DateTime,
      gender: null == gender
          ? _value.gender
          : gender // ignore: cast_nullable_to_non_nullable
              as String,
      phone: freezed == phone
          ? _value.phone
          : phone // ignore: cast_nullable_to_non_nullable
              as String?,
      email: freezed == email
          ? _value.email
          : email // ignore: cast_nullable_to_non_nullable
              as String?,
      address: freezed == address
          ? _value.address
          : address // ignore: cast_nullable_to_non_nullable
              as String?,
      emergencyContactName: freezed == emergencyContactName
          ? _value.emergencyContactName
          : emergencyContactName // ignore: cast_nullable_to_non_nullable
              as String?,
      emergencyContactPhone: freezed == emergencyContactPhone
          ? _value.emergencyContactPhone
          : emergencyContactPhone // ignore: cast_nullable_to_non_nullable
              as String?,
      bloodType: freezed == bloodType
          ? _value.bloodType
          : bloodType // ignore: cast_nullable_to_non_nullable
              as String?,
      allergies: freezed == allergies
          ? _value.allergies
          : allergies // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      chronicConditions: freezed == chronicConditions
          ? _value.chronicConditions
          : chronicConditions // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      currentMedications: freezed == currentMedications
          ? _value.currentMedications
          : currentMedications // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      insuranceProvider: freezed == insuranceProvider
          ? _value.insuranceProvider
          : insuranceProvider // ignore: cast_nullable_to_non_nullable
              as String?,
      insuranceNumber: freezed == insuranceNumber
          ? _value.insuranceNumber
          : insuranceNumber // ignore: cast_nullable_to_non_nullable
              as String?,
      photoUrl: freezed == photoUrl
          ? _value.photoUrl
          : photoUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as PatientStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      familyId: freezed == familyId
          ? _value.familyId
          : familyId // ignore: cast_nullable_to_non_nullable
              as int?,
      familyName: freezed == familyName
          ? _value.familyName
          : familyName // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$PatientImplCopyWith<$Res> implements $PatientCopyWith<$Res> {
  factory _$$PatientImplCopyWith(
          _$PatientImpl value, $Res Function(_$PatientImpl) then) =
      __$$PatientImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String firstName,
      String lastName,
      DateTime dateOfBirth,
      String gender,
      String? phone,
      String? email,
      String? address,
      String? emergencyContactName,
      String? emergencyContactPhone,
      String? bloodType,
      List<String>? allergies,
      List<String>? chronicConditions,
      List<String>? currentMedications,
      String? insuranceProvider,
      String? insuranceNumber,
      String? photoUrl,
      PatientStatus status,
      String? notes,
      DateTime createdAt,
      DateTime? updatedAt,
      int? familyId,
      String? familyName});
}

/// @nodoc
class __$$PatientImplCopyWithImpl<$Res>
    extends _$PatientCopyWithImpl<$Res, _$PatientImpl>
    implements _$$PatientImplCopyWith<$Res> {
  __$$PatientImplCopyWithImpl(
      _$PatientImpl _value, $Res Function(_$PatientImpl) _then)
      : super(_value, _then);

  /// Create a copy of Patient
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? firstName = null,
    Object? lastName = null,
    Object? dateOfBirth = null,
    Object? gender = null,
    Object? phone = freezed,
    Object? email = freezed,
    Object? address = freezed,
    Object? emergencyContactName = freezed,
    Object? emergencyContactPhone = freezed,
    Object? bloodType = freezed,
    Object? allergies = freezed,
    Object? chronicConditions = freezed,
    Object? currentMedications = freezed,
    Object? insuranceProvider = freezed,
    Object? insuranceNumber = freezed,
    Object? photoUrl = freezed,
    Object? status = null,
    Object? notes = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
    Object? familyId = freezed,
    Object? familyName = freezed,
  }) {
    return _then(_$PatientImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      firstName: null == firstName
          ? _value.firstName
          : firstName // ignore: cast_nullable_to_non_nullable
              as String,
      lastName: null == lastName
          ? _value.lastName
          : lastName // ignore: cast_nullable_to_non_nullable
              as String,
      dateOfBirth: null == dateOfBirth
          ? _value.dateOfBirth
          : dateOfBirth // ignore: cast_nullable_to_non_nullable
              as DateTime,
      gender: null == gender
          ? _value.gender
          : gender // ignore: cast_nullable_to_non_nullable
              as String,
      phone: freezed == phone
          ? _value.phone
          : phone // ignore: cast_nullable_to_non_nullable
              as String?,
      email: freezed == email
          ? _value.email
          : email // ignore: cast_nullable_to_non_nullable
              as String?,
      address: freezed == address
          ? _value.address
          : address // ignore: cast_nullable_to_non_nullable
              as String?,
      emergencyContactName: freezed == emergencyContactName
          ? _value.emergencyContactName
          : emergencyContactName // ignore: cast_nullable_to_non_nullable
              as String?,
      emergencyContactPhone: freezed == emergencyContactPhone
          ? _value.emergencyContactPhone
          : emergencyContactPhone // ignore: cast_nullable_to_non_nullable
              as String?,
      bloodType: freezed == bloodType
          ? _value.bloodType
          : bloodType // ignore: cast_nullable_to_non_nullable
              as String?,
      allergies: freezed == allergies
          ? _value._allergies
          : allergies // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      chronicConditions: freezed == chronicConditions
          ? _value._chronicConditions
          : chronicConditions // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      currentMedications: freezed == currentMedications
          ? _value._currentMedications
          : currentMedications // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      insuranceProvider: freezed == insuranceProvider
          ? _value.insuranceProvider
          : insuranceProvider // ignore: cast_nullable_to_non_nullable
              as String?,
      insuranceNumber: freezed == insuranceNumber
          ? _value.insuranceNumber
          : insuranceNumber // ignore: cast_nullable_to_non_nullable
              as String?,
      photoUrl: freezed == photoUrl
          ? _value.photoUrl
          : photoUrl // ignore: cast_nullable_to_non_nullable
              as String?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as PatientStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      createdAt: null == createdAt
          ? _value.createdAt
          : createdAt // ignore: cast_nullable_to_non_nullable
              as DateTime,
      updatedAt: freezed == updatedAt
          ? _value.updatedAt
          : updatedAt // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      familyId: freezed == familyId
          ? _value.familyId
          : familyId // ignore: cast_nullable_to_non_nullable
              as int?,
      familyName: freezed == familyName
          ? _value.familyName
          : familyName // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$PatientImpl extends _Patient {
  const _$PatientImpl(
      {required this.id,
      required this.firstName,
      required this.lastName,
      required this.dateOfBirth,
      required this.gender,
      this.phone,
      this.email,
      this.address,
      this.emergencyContactName,
      this.emergencyContactPhone,
      this.bloodType,
      final List<String>? allergies,
      final List<String>? chronicConditions,
      final List<String>? currentMedications,
      this.insuranceProvider,
      this.insuranceNumber,
      this.photoUrl,
      required this.status,
      this.notes,
      required this.createdAt,
      this.updatedAt,
      this.familyId,
      this.familyName})
      : _allergies = allergies,
        _chronicConditions = chronicConditions,
        _currentMedications = currentMedications,
        super._();

  factory _$PatientImpl.fromJson(Map<String, dynamic> json) =>
      _$$PatientImplFromJson(json);

  @override
  final int id;
  @override
  final String firstName;
  @override
  final String lastName;
  @override
  final DateTime dateOfBirth;
  @override
  final String gender;
  @override
  final String? phone;
  @override
  final String? email;
  @override
  final String? address;
  @override
  final String? emergencyContactName;
  @override
  final String? emergencyContactPhone;
  @override
  final String? bloodType;
  final List<String>? _allergies;
  @override
  List<String>? get allergies {
    final value = _allergies;
    if (value == null) return null;
    if (_allergies is EqualUnmodifiableListView) return _allergies;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  final List<String>? _chronicConditions;
  @override
  List<String>? get chronicConditions {
    final value = _chronicConditions;
    if (value == null) return null;
    if (_chronicConditions is EqualUnmodifiableListView)
      return _chronicConditions;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  final List<String>? _currentMedications;
  @override
  List<String>? get currentMedications {
    final value = _currentMedications;
    if (value == null) return null;
    if (_currentMedications is EqualUnmodifiableListView)
      return _currentMedications;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  @override
  final String? insuranceProvider;
  @override
  final String? insuranceNumber;
  @override
  final String? photoUrl;
  @override
  final PatientStatus status;
  @override
  final String? notes;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;
  @override
  final int? familyId;
  @override
  final String? familyName;

  @override
  String toString() {
    return 'Patient(id: $id, firstName: $firstName, lastName: $lastName, dateOfBirth: $dateOfBirth, gender: $gender, phone: $phone, email: $email, address: $address, emergencyContactName: $emergencyContactName, emergencyContactPhone: $emergencyContactPhone, bloodType: $bloodType, allergies: $allergies, chronicConditions: $chronicConditions, currentMedications: $currentMedications, insuranceProvider: $insuranceProvider, insuranceNumber: $insuranceNumber, photoUrl: $photoUrl, status: $status, notes: $notes, createdAt: $createdAt, updatedAt: $updatedAt, familyId: $familyId, familyName: $familyName)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$PatientImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.firstName, firstName) ||
                other.firstName == firstName) &&
            (identical(other.lastName, lastName) ||
                other.lastName == lastName) &&
            (identical(other.dateOfBirth, dateOfBirth) ||
                other.dateOfBirth == dateOfBirth) &&
            (identical(other.gender, gender) || other.gender == gender) &&
            (identical(other.phone, phone) || other.phone == phone) &&
            (identical(other.email, email) || other.email == email) &&
            (identical(other.address, address) || other.address == address) &&
            (identical(other.emergencyContactName, emergencyContactName) ||
                other.emergencyContactName == emergencyContactName) &&
            (identical(other.emergencyContactPhone, emergencyContactPhone) ||
                other.emergencyContactPhone == emergencyContactPhone) &&
            (identical(other.bloodType, bloodType) ||
                other.bloodType == bloodType) &&
            const DeepCollectionEquality()
                .equals(other._allergies, _allergies) &&
            const DeepCollectionEquality()
                .equals(other._chronicConditions, _chronicConditions) &&
            const DeepCollectionEquality()
                .equals(other._currentMedications, _currentMedications) &&
            (identical(other.insuranceProvider, insuranceProvider) ||
                other.insuranceProvider == insuranceProvider) &&
            (identical(other.insuranceNumber, insuranceNumber) ||
                other.insuranceNumber == insuranceNumber) &&
            (identical(other.photoUrl, photoUrl) ||
                other.photoUrl == photoUrl) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.notes, notes) || other.notes == notes) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt) &&
            (identical(other.familyId, familyId) ||
                other.familyId == familyId) &&
            (identical(other.familyName, familyName) ||
                other.familyName == familyName));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hashAll([
        runtimeType,
        id,
        firstName,
        lastName,
        dateOfBirth,
        gender,
        phone,
        email,
        address,
        emergencyContactName,
        emergencyContactPhone,
        bloodType,
        const DeepCollectionEquality().hash(_allergies),
        const DeepCollectionEquality().hash(_chronicConditions),
        const DeepCollectionEquality().hash(_currentMedications),
        insuranceProvider,
        insuranceNumber,
        photoUrl,
        status,
        notes,
        createdAt,
        updatedAt,
        familyId,
        familyName
      ]);

  /// Create a copy of Patient
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$PatientImplCopyWith<_$PatientImpl> get copyWith =>
      __$$PatientImplCopyWithImpl<_$PatientImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$PatientImplToJson(
      this,
    );
  }
}

abstract class _Patient extends Patient {
  const factory _Patient(
      {required final int id,
      required final String firstName,
      required final String lastName,
      required final DateTime dateOfBirth,
      required final String gender,
      final String? phone,
      final String? email,
      final String? address,
      final String? emergencyContactName,
      final String? emergencyContactPhone,
      final String? bloodType,
      final List<String>? allergies,
      final List<String>? chronicConditions,
      final List<String>? currentMedications,
      final String? insuranceProvider,
      final String? insuranceNumber,
      final String? photoUrl,
      required final PatientStatus status,
      final String? notes,
      required final DateTime createdAt,
      final DateTime? updatedAt,
      final int? familyId,
      final String? familyName}) = _$PatientImpl;
  const _Patient._() : super._();

  factory _Patient.fromJson(Map<String, dynamic> json) = _$PatientImpl.fromJson;

  @override
  int get id;
  @override
  String get firstName;
  @override
  String get lastName;
  @override
  DateTime get dateOfBirth;
  @override
  String get gender;
  @override
  String? get phone;
  @override
  String? get email;
  @override
  String? get address;
  @override
  String? get emergencyContactName;
  @override
  String? get emergencyContactPhone;
  @override
  String? get bloodType;
  @override
  List<String>? get allergies;
  @override
  List<String>? get chronicConditions;
  @override
  List<String>? get currentMedications;
  @override
  String? get insuranceProvider;
  @override
  String? get insuranceNumber;
  @override
  String? get photoUrl;
  @override
  PatientStatus get status;
  @override
  String? get notes;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;
  @override
  int? get familyId;
  @override
  String? get familyName;

  /// Create a copy of Patient
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$PatientImplCopyWith<_$PatientImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Consultation _$ConsultationFromJson(Map<String, dynamic> json) {
  return _Consultation.fromJson(json);
}

/// @nodoc
mixin _$Consultation {
  int get id => throw _privateConstructorUsedError;
  int get patientId => throw _privateConstructorUsedError;
  String get patientName => throw _privateConstructorUsedError;
  int get doctorId => throw _privateConstructorUsedError;
  String get doctorName => throw _privateConstructorUsedError;
  DateTime get dateTime => throw _privateConstructorUsedError;
  ConsultationType get type => throw _privateConstructorUsedError;
  String? get chiefComplaint => throw _privateConstructorUsedError;
  String? get diagnosis => throw _privateConstructorUsedError;
  String? get treatmentPlan => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;
  List<VitalSigns>? get vitalSigns => throw _privateConstructorUsedError;
  List<Prescription>? get prescriptions => throw _privateConstructorUsedError;
  ConsultationStatus get status => throw _privateConstructorUsedError;
  double? get fee => throw _privateConstructorUsedError;
  String? get paymentStatus => throw _privateConstructorUsedError;
  DateTime? get nextAppointment => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this Consultation to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Consultation
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $ConsultationCopyWith<Consultation> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $ConsultationCopyWith<$Res> {
  factory $ConsultationCopyWith(
          Consultation value, $Res Function(Consultation) then) =
      _$ConsultationCopyWithImpl<$Res, Consultation>;
  @useResult
  $Res call(
      {int id,
      int patientId,
      String patientName,
      int doctorId,
      String doctorName,
      DateTime dateTime,
      ConsultationType type,
      String? chiefComplaint,
      String? diagnosis,
      String? treatmentPlan,
      String? notes,
      List<VitalSigns>? vitalSigns,
      List<Prescription>? prescriptions,
      ConsultationStatus status,
      double? fee,
      String? paymentStatus,
      DateTime? nextAppointment,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$ConsultationCopyWithImpl<$Res, $Val extends Consultation>
    implements $ConsultationCopyWith<$Res> {
  _$ConsultationCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Consultation
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? patientId = null,
    Object? patientName = null,
    Object? doctorId = null,
    Object? doctorName = null,
    Object? dateTime = null,
    Object? type = null,
    Object? chiefComplaint = freezed,
    Object? diagnosis = freezed,
    Object? treatmentPlan = freezed,
    Object? notes = freezed,
    Object? vitalSigns = freezed,
    Object? prescriptions = freezed,
    Object? status = null,
    Object? fee = freezed,
    Object? paymentStatus = freezed,
    Object? nextAppointment = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      patientId: null == patientId
          ? _value.patientId
          : patientId // ignore: cast_nullable_to_non_nullable
              as int,
      patientName: null == patientName
          ? _value.patientName
          : patientName // ignore: cast_nullable_to_non_nullable
              as String,
      doctorId: null == doctorId
          ? _value.doctorId
          : doctorId // ignore: cast_nullable_to_non_nullable
              as int,
      doctorName: null == doctorName
          ? _value.doctorName
          : doctorName // ignore: cast_nullable_to_non_nullable
              as String,
      dateTime: null == dateTime
          ? _value.dateTime
          : dateTime // ignore: cast_nullable_to_non_nullable
              as DateTime,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as ConsultationType,
      chiefComplaint: freezed == chiefComplaint
          ? _value.chiefComplaint
          : chiefComplaint // ignore: cast_nullable_to_non_nullable
              as String?,
      diagnosis: freezed == diagnosis
          ? _value.diagnosis
          : diagnosis // ignore: cast_nullable_to_non_nullable
              as String?,
      treatmentPlan: freezed == treatmentPlan
          ? _value.treatmentPlan
          : treatmentPlan // ignore: cast_nullable_to_non_nullable
              as String?,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      vitalSigns: freezed == vitalSigns
          ? _value.vitalSigns
          : vitalSigns // ignore: cast_nullable_to_non_nullable
              as List<VitalSigns>?,
      prescriptions: freezed == prescriptions
          ? _value.prescriptions
          : prescriptions // ignore: cast_nullable_to_non_nullable
              as List<Prescription>?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as ConsultationStatus,
      fee: freezed == fee
          ? _value.fee
          : fee // ignore: cast_nullable_to_non_nullable
              as double?,
      paymentStatus: freezed == paymentStatus
          ? _value.paymentStatus
          : paymentStatus // ignore: cast_nullable_to_non_nullable
              as String?,
      nextAppointment: freezed == nextAppointment
          ? _value.nextAppointment
          : nextAppointment // ignore: cast_nullable_to_non_nullable
              as DateTime?,
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
abstract class _$$ConsultationImplCopyWith<$Res>
    implements $ConsultationCopyWith<$Res> {
  factory _$$ConsultationImplCopyWith(
          _$ConsultationImpl value, $Res Function(_$ConsultationImpl) then) =
      __$$ConsultationImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int patientId,
      String patientName,
      int doctorId,
      String doctorName,
      DateTime dateTime,
      ConsultationType type,
      String? chiefComplaint,
      String? diagnosis,
      String? treatmentPlan,
      String? notes,
      List<VitalSigns>? vitalSigns,
      List<Prescription>? prescriptions,
      ConsultationStatus status,
      double? fee,
      String? paymentStatus,
      DateTime? nextAppointment,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$ConsultationImplCopyWithImpl<$Res>
    extends _$ConsultationCopyWithImpl<$Res, _$ConsultationImpl>
    implements _$$ConsultationImplCopyWith<$Res> {
  __$$ConsultationImplCopyWithImpl(
      _$ConsultationImpl _value, $Res Function(_$ConsultationImpl) _then)
      : super(_value, _then);

  /// Create a copy of Consultation
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? patientId = null,
    Object? patientName = null,
    Object? doctorId = null,
    Object? doctorName = null,
    Object? dateTime = null,
    Object? type = null,
    Object? chiefComplaint = freezed,
    Object? diagnosis = freezed,
    Object? treatmentPlan = freezed,
    Object? notes = freezed,
    Object? vitalSigns = freezed,
    Object? prescriptions = freezed,
    Object? status = null,
    Object? fee = freezed,
    Object? paymentStatus = freezed,
    Object? nextAppointment = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$ConsultationImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      patientId: null == patientId
          ? _value.patientId
          : patientId // ignore: cast_nullable_to_non_nullable
              as int,
      patientName: null == patientName
          ? _value.patientName
          : patientName // ignore: cast_nullable_to_non_nullable
              as String,
      doctorId: null == doctorId
          ? _value.doctorId
          : doctorId // ignore: cast_nullable_to_non_nullable
              as int,
      doctorName: null == doctorName
          ? _value.doctorName
          : doctorName // ignore: cast_nullable_to_non_nullable
              as String,
      dateTime: null == dateTime
          ? _value.dateTime
          : dateTime // ignore: cast_nullable_to_non_nullable
              as DateTime,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as ConsultationType,
      chiefComplaint: freezed == chiefComplaint
          ? _value.chiefComplaint
          : chiefComplaint // ignore: cast_nullable_to_non_nullable
              as String?,
      diagnosis: freezed == diagnosis
          ? _value.diagnosis
          : diagnosis // ignore: cast_nullable_to_non_nullable
              as String?,
      treatmentPlan: freezed == treatmentPlan
          ? _value.treatmentPlan
          : treatmentPlan // ignore: cast_nullable_to_non_nullable
              as String?,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      vitalSigns: freezed == vitalSigns
          ? _value._vitalSigns
          : vitalSigns // ignore: cast_nullable_to_non_nullable
              as List<VitalSigns>?,
      prescriptions: freezed == prescriptions
          ? _value._prescriptions
          : prescriptions // ignore: cast_nullable_to_non_nullable
              as List<Prescription>?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as ConsultationStatus,
      fee: freezed == fee
          ? _value.fee
          : fee // ignore: cast_nullable_to_non_nullable
              as double?,
      paymentStatus: freezed == paymentStatus
          ? _value.paymentStatus
          : paymentStatus // ignore: cast_nullable_to_non_nullable
              as String?,
      nextAppointment: freezed == nextAppointment
          ? _value.nextAppointment
          : nextAppointment // ignore: cast_nullable_to_non_nullable
              as DateTime?,
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
class _$ConsultationImpl implements _Consultation {
  const _$ConsultationImpl(
      {required this.id,
      required this.patientId,
      required this.patientName,
      required this.doctorId,
      required this.doctorName,
      required this.dateTime,
      required this.type,
      this.chiefComplaint,
      this.diagnosis,
      this.treatmentPlan,
      this.notes,
      final List<VitalSigns>? vitalSigns,
      final List<Prescription>? prescriptions,
      required this.status,
      this.fee,
      this.paymentStatus,
      this.nextAppointment,
      required this.createdAt,
      this.updatedAt})
      : _vitalSigns = vitalSigns,
        _prescriptions = prescriptions;

  factory _$ConsultationImpl.fromJson(Map<String, dynamic> json) =>
      _$$ConsultationImplFromJson(json);

  @override
  final int id;
  @override
  final int patientId;
  @override
  final String patientName;
  @override
  final int doctorId;
  @override
  final String doctorName;
  @override
  final DateTime dateTime;
  @override
  final ConsultationType type;
  @override
  final String? chiefComplaint;
  @override
  final String? diagnosis;
  @override
  final String? treatmentPlan;
  @override
  final String? notes;
  final List<VitalSigns>? _vitalSigns;
  @override
  List<VitalSigns>? get vitalSigns {
    final value = _vitalSigns;
    if (value == null) return null;
    if (_vitalSigns is EqualUnmodifiableListView) return _vitalSigns;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  final List<Prescription>? _prescriptions;
  @override
  List<Prescription>? get prescriptions {
    final value = _prescriptions;
    if (value == null) return null;
    if (_prescriptions is EqualUnmodifiableListView) return _prescriptions;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  @override
  final ConsultationStatus status;
  @override
  final double? fee;
  @override
  final String? paymentStatus;
  @override
  final DateTime? nextAppointment;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'Consultation(id: $id, patientId: $patientId, patientName: $patientName, doctorId: $doctorId, doctorName: $doctorName, dateTime: $dateTime, type: $type, chiefComplaint: $chiefComplaint, diagnosis: $diagnosis, treatmentPlan: $treatmentPlan, notes: $notes, vitalSigns: $vitalSigns, prescriptions: $prescriptions, status: $status, fee: $fee, paymentStatus: $paymentStatus, nextAppointment: $nextAppointment, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$ConsultationImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.patientId, patientId) ||
                other.patientId == patientId) &&
            (identical(other.patientName, patientName) ||
                other.patientName == patientName) &&
            (identical(other.doctorId, doctorId) ||
                other.doctorId == doctorId) &&
            (identical(other.doctorName, doctorName) ||
                other.doctorName == doctorName) &&
            (identical(other.dateTime, dateTime) ||
                other.dateTime == dateTime) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.chiefComplaint, chiefComplaint) ||
                other.chiefComplaint == chiefComplaint) &&
            (identical(other.diagnosis, diagnosis) ||
                other.diagnosis == diagnosis) &&
            (identical(other.treatmentPlan, treatmentPlan) ||
                other.treatmentPlan == treatmentPlan) &&
            (identical(other.notes, notes) || other.notes == notes) &&
            const DeepCollectionEquality()
                .equals(other._vitalSigns, _vitalSigns) &&
            const DeepCollectionEquality()
                .equals(other._prescriptions, _prescriptions) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.fee, fee) || other.fee == fee) &&
            (identical(other.paymentStatus, paymentStatus) ||
                other.paymentStatus == paymentStatus) &&
            (identical(other.nextAppointment, nextAppointment) ||
                other.nextAppointment == nextAppointment) &&
            (identical(other.createdAt, createdAt) ||
                other.createdAt == createdAt) &&
            (identical(other.updatedAt, updatedAt) ||
                other.updatedAt == updatedAt));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hashAll([
        runtimeType,
        id,
        patientId,
        patientName,
        doctorId,
        doctorName,
        dateTime,
        type,
        chiefComplaint,
        diagnosis,
        treatmentPlan,
        notes,
        const DeepCollectionEquality().hash(_vitalSigns),
        const DeepCollectionEquality().hash(_prescriptions),
        status,
        fee,
        paymentStatus,
        nextAppointment,
        createdAt,
        updatedAt
      ]);

  /// Create a copy of Consultation
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$ConsultationImplCopyWith<_$ConsultationImpl> get copyWith =>
      __$$ConsultationImplCopyWithImpl<_$ConsultationImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$ConsultationImplToJson(
      this,
    );
  }
}

abstract class _Consultation implements Consultation {
  const factory _Consultation(
      {required final int id,
      required final int patientId,
      required final String patientName,
      required final int doctorId,
      required final String doctorName,
      required final DateTime dateTime,
      required final ConsultationType type,
      final String? chiefComplaint,
      final String? diagnosis,
      final String? treatmentPlan,
      final String? notes,
      final List<VitalSigns>? vitalSigns,
      final List<Prescription>? prescriptions,
      required final ConsultationStatus status,
      final double? fee,
      final String? paymentStatus,
      final DateTime? nextAppointment,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$ConsultationImpl;

  factory _Consultation.fromJson(Map<String, dynamic> json) =
      _$ConsultationImpl.fromJson;

  @override
  int get id;
  @override
  int get patientId;
  @override
  String get patientName;
  @override
  int get doctorId;
  @override
  String get doctorName;
  @override
  DateTime get dateTime;
  @override
  ConsultationType get type;
  @override
  String? get chiefComplaint;
  @override
  String? get diagnosis;
  @override
  String? get treatmentPlan;
  @override
  String? get notes;
  @override
  List<VitalSigns>? get vitalSigns;
  @override
  List<Prescription>? get prescriptions;
  @override
  ConsultationStatus get status;
  @override
  double? get fee;
  @override
  String? get paymentStatus;
  @override
  DateTime? get nextAppointment;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of Consultation
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$ConsultationImplCopyWith<_$ConsultationImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

VitalSigns _$VitalSignsFromJson(Map<String, dynamic> json) {
  return _VitalSigns.fromJson(json);
}

/// @nodoc
mixin _$VitalSigns {
  double? get temperature => throw _privateConstructorUsedError;
  int? get heartRate => throw _privateConstructorUsedError;
  int? get systolicBP => throw _privateConstructorUsedError;
  int? get diastolicBP => throw _privateConstructorUsedError;
  int? get respiratoryRate => throw _privateConstructorUsedError;
  double? get oxygenSaturation => throw _privateConstructorUsedError;
  double? get weight => throw _privateConstructorUsedError;
  double? get height => throw _privateConstructorUsedError;
  double? get bmi => throw _privateConstructorUsedError;

  /// Serializes this VitalSigns to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of VitalSigns
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $VitalSignsCopyWith<VitalSigns> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $VitalSignsCopyWith<$Res> {
  factory $VitalSignsCopyWith(
          VitalSigns value, $Res Function(VitalSigns) then) =
      _$VitalSignsCopyWithImpl<$Res, VitalSigns>;
  @useResult
  $Res call(
      {double? temperature,
      int? heartRate,
      int? systolicBP,
      int? diastolicBP,
      int? respiratoryRate,
      double? oxygenSaturation,
      double? weight,
      double? height,
      double? bmi});
}

/// @nodoc
class _$VitalSignsCopyWithImpl<$Res, $Val extends VitalSigns>
    implements $VitalSignsCopyWith<$Res> {
  _$VitalSignsCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of VitalSigns
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? temperature = freezed,
    Object? heartRate = freezed,
    Object? systolicBP = freezed,
    Object? diastolicBP = freezed,
    Object? respiratoryRate = freezed,
    Object? oxygenSaturation = freezed,
    Object? weight = freezed,
    Object? height = freezed,
    Object? bmi = freezed,
  }) {
    return _then(_value.copyWith(
      temperature: freezed == temperature
          ? _value.temperature
          : temperature // ignore: cast_nullable_to_non_nullable
              as double?,
      heartRate: freezed == heartRate
          ? _value.heartRate
          : heartRate // ignore: cast_nullable_to_non_nullable
              as int?,
      systolicBP: freezed == systolicBP
          ? _value.systolicBP
          : systolicBP // ignore: cast_nullable_to_non_nullable
              as int?,
      diastolicBP: freezed == diastolicBP
          ? _value.diastolicBP
          : diastolicBP // ignore: cast_nullable_to_non_nullable
              as int?,
      respiratoryRate: freezed == respiratoryRate
          ? _value.respiratoryRate
          : respiratoryRate // ignore: cast_nullable_to_non_nullable
              as int?,
      oxygenSaturation: freezed == oxygenSaturation
          ? _value.oxygenSaturation
          : oxygenSaturation // ignore: cast_nullable_to_non_nullable
              as double?,
      weight: freezed == weight
          ? _value.weight
          : weight // ignore: cast_nullable_to_non_nullable
              as double?,
      height: freezed == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as double?,
      bmi: freezed == bmi
          ? _value.bmi
          : bmi // ignore: cast_nullable_to_non_nullable
              as double?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$VitalSignsImplCopyWith<$Res>
    implements $VitalSignsCopyWith<$Res> {
  factory _$$VitalSignsImplCopyWith(
          _$VitalSignsImpl value, $Res Function(_$VitalSignsImpl) then) =
      __$$VitalSignsImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {double? temperature,
      int? heartRate,
      int? systolicBP,
      int? diastolicBP,
      int? respiratoryRate,
      double? oxygenSaturation,
      double? weight,
      double? height,
      double? bmi});
}

/// @nodoc
class __$$VitalSignsImplCopyWithImpl<$Res>
    extends _$VitalSignsCopyWithImpl<$Res, _$VitalSignsImpl>
    implements _$$VitalSignsImplCopyWith<$Res> {
  __$$VitalSignsImplCopyWithImpl(
      _$VitalSignsImpl _value, $Res Function(_$VitalSignsImpl) _then)
      : super(_value, _then);

  /// Create a copy of VitalSigns
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? temperature = freezed,
    Object? heartRate = freezed,
    Object? systolicBP = freezed,
    Object? diastolicBP = freezed,
    Object? respiratoryRate = freezed,
    Object? oxygenSaturation = freezed,
    Object? weight = freezed,
    Object? height = freezed,
    Object? bmi = freezed,
  }) {
    return _then(_$VitalSignsImpl(
      temperature: freezed == temperature
          ? _value.temperature
          : temperature // ignore: cast_nullable_to_non_nullable
              as double?,
      heartRate: freezed == heartRate
          ? _value.heartRate
          : heartRate // ignore: cast_nullable_to_non_nullable
              as int?,
      systolicBP: freezed == systolicBP
          ? _value.systolicBP
          : systolicBP // ignore: cast_nullable_to_non_nullable
              as int?,
      diastolicBP: freezed == diastolicBP
          ? _value.diastolicBP
          : diastolicBP // ignore: cast_nullable_to_non_nullable
              as int?,
      respiratoryRate: freezed == respiratoryRate
          ? _value.respiratoryRate
          : respiratoryRate // ignore: cast_nullable_to_non_nullable
              as int?,
      oxygenSaturation: freezed == oxygenSaturation
          ? _value.oxygenSaturation
          : oxygenSaturation // ignore: cast_nullable_to_non_nullable
              as double?,
      weight: freezed == weight
          ? _value.weight
          : weight // ignore: cast_nullable_to_non_nullable
              as double?,
      height: freezed == height
          ? _value.height
          : height // ignore: cast_nullable_to_non_nullable
              as double?,
      bmi: freezed == bmi
          ? _value.bmi
          : bmi // ignore: cast_nullable_to_non_nullable
              as double?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$VitalSignsImpl implements _VitalSigns {
  const _$VitalSignsImpl(
      {this.temperature,
      this.heartRate,
      this.systolicBP,
      this.diastolicBP,
      this.respiratoryRate,
      this.oxygenSaturation,
      this.weight,
      this.height,
      this.bmi});

  factory _$VitalSignsImpl.fromJson(Map<String, dynamic> json) =>
      _$$VitalSignsImplFromJson(json);

  @override
  final double? temperature;
  @override
  final int? heartRate;
  @override
  final int? systolicBP;
  @override
  final int? diastolicBP;
  @override
  final int? respiratoryRate;
  @override
  final double? oxygenSaturation;
  @override
  final double? weight;
  @override
  final double? height;
  @override
  final double? bmi;

  @override
  String toString() {
    return 'VitalSigns(temperature: $temperature, heartRate: $heartRate, systolicBP: $systolicBP, diastolicBP: $diastolicBP, respiratoryRate: $respiratoryRate, oxygenSaturation: $oxygenSaturation, weight: $weight, height: $height, bmi: $bmi)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$VitalSignsImpl &&
            (identical(other.temperature, temperature) ||
                other.temperature == temperature) &&
            (identical(other.heartRate, heartRate) ||
                other.heartRate == heartRate) &&
            (identical(other.systolicBP, systolicBP) ||
                other.systolicBP == systolicBP) &&
            (identical(other.diastolicBP, diastolicBP) ||
                other.diastolicBP == diastolicBP) &&
            (identical(other.respiratoryRate, respiratoryRate) ||
                other.respiratoryRate == respiratoryRate) &&
            (identical(other.oxygenSaturation, oxygenSaturation) ||
                other.oxygenSaturation == oxygenSaturation) &&
            (identical(other.weight, weight) || other.weight == weight) &&
            (identical(other.height, height) || other.height == height) &&
            (identical(other.bmi, bmi) || other.bmi == bmi));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      temperature,
      heartRate,
      systolicBP,
      diastolicBP,
      respiratoryRate,
      oxygenSaturation,
      weight,
      height,
      bmi);

  /// Create a copy of VitalSigns
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$VitalSignsImplCopyWith<_$VitalSignsImpl> get copyWith =>
      __$$VitalSignsImplCopyWithImpl<_$VitalSignsImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$VitalSignsImplToJson(
      this,
    );
  }
}

abstract class _VitalSigns implements VitalSigns {
  const factory _VitalSigns(
      {final double? temperature,
      final int? heartRate,
      final int? systolicBP,
      final int? diastolicBP,
      final int? respiratoryRate,
      final double? oxygenSaturation,
      final double? weight,
      final double? height,
      final double? bmi}) = _$VitalSignsImpl;

  factory _VitalSigns.fromJson(Map<String, dynamic> json) =
      _$VitalSignsImpl.fromJson;

  @override
  double? get temperature;
  @override
  int? get heartRate;
  @override
  int? get systolicBP;
  @override
  int? get diastolicBP;
  @override
  int? get respiratoryRate;
  @override
  double? get oxygenSaturation;
  @override
  double? get weight;
  @override
  double? get height;
  @override
  double? get bmi;

  /// Create a copy of VitalSigns
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$VitalSignsImplCopyWith<_$VitalSignsImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Prescription _$PrescriptionFromJson(Map<String, dynamic> json) {
  return _Prescription.fromJson(json);
}

/// @nodoc
mixin _$Prescription {
  int get id => throw _privateConstructorUsedError;
  int get consultationId => throw _privateConstructorUsedError;
  int get medicationId => throw _privateConstructorUsedError;
  String get medicationName => throw _privateConstructorUsedError;
  String get dosage => throw _privateConstructorUsedError;
  String get frequency => throw _privateConstructorUsedError;
  String get duration => throw _privateConstructorUsedError;
  String? get instructions => throw _privateConstructorUsedError;
  DateTime get startDate => throw _privateConstructorUsedError;
  DateTime? get endDate => throw _privateConstructorUsedError;
  bool get isActive => throw _privateConstructorUsedError;

  /// Serializes this Prescription to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Prescription
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $PrescriptionCopyWith<Prescription> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $PrescriptionCopyWith<$Res> {
  factory $PrescriptionCopyWith(
          Prescription value, $Res Function(Prescription) then) =
      _$PrescriptionCopyWithImpl<$Res, Prescription>;
  @useResult
  $Res call(
      {int id,
      int consultationId,
      int medicationId,
      String medicationName,
      String dosage,
      String frequency,
      String duration,
      String? instructions,
      DateTime startDate,
      DateTime? endDate,
      bool isActive});
}

/// @nodoc
class _$PrescriptionCopyWithImpl<$Res, $Val extends Prescription>
    implements $PrescriptionCopyWith<$Res> {
  _$PrescriptionCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Prescription
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? consultationId = null,
    Object? medicationId = null,
    Object? medicationName = null,
    Object? dosage = null,
    Object? frequency = null,
    Object? duration = null,
    Object? instructions = freezed,
    Object? startDate = null,
    Object? endDate = freezed,
    Object? isActive = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      consultationId: null == consultationId
          ? _value.consultationId
          : consultationId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationId: null == medicationId
          ? _value.medicationId
          : medicationId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationName: null == medicationName
          ? _value.medicationName
          : medicationName // ignore: cast_nullable_to_non_nullable
              as String,
      dosage: null == dosage
          ? _value.dosage
          : dosage // ignore: cast_nullable_to_non_nullable
              as String,
      frequency: null == frequency
          ? _value.frequency
          : frequency // ignore: cast_nullable_to_non_nullable
              as String,
      duration: null == duration
          ? _value.duration
          : duration // ignore: cast_nullable_to_non_nullable
              as String,
      instructions: freezed == instructions
          ? _value.instructions
          : instructions // ignore: cast_nullable_to_non_nullable
              as String?,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: freezed == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$PrescriptionImplCopyWith<$Res>
    implements $PrescriptionCopyWith<$Res> {
  factory _$$PrescriptionImplCopyWith(
          _$PrescriptionImpl value, $Res Function(_$PrescriptionImpl) then) =
      __$$PrescriptionImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int consultationId,
      int medicationId,
      String medicationName,
      String dosage,
      String frequency,
      String duration,
      String? instructions,
      DateTime startDate,
      DateTime? endDate,
      bool isActive});
}

/// @nodoc
class __$$PrescriptionImplCopyWithImpl<$Res>
    extends _$PrescriptionCopyWithImpl<$Res, _$PrescriptionImpl>
    implements _$$PrescriptionImplCopyWith<$Res> {
  __$$PrescriptionImplCopyWithImpl(
      _$PrescriptionImpl _value, $Res Function(_$PrescriptionImpl) _then)
      : super(_value, _then);

  /// Create a copy of Prescription
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? consultationId = null,
    Object? medicationId = null,
    Object? medicationName = null,
    Object? dosage = null,
    Object? frequency = null,
    Object? duration = null,
    Object? instructions = freezed,
    Object? startDate = null,
    Object? endDate = freezed,
    Object? isActive = null,
  }) {
    return _then(_$PrescriptionImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      consultationId: null == consultationId
          ? _value.consultationId
          : consultationId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationId: null == medicationId
          ? _value.medicationId
          : medicationId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationName: null == medicationName
          ? _value.medicationName
          : medicationName // ignore: cast_nullable_to_non_nullable
              as String,
      dosage: null == dosage
          ? _value.dosage
          : dosage // ignore: cast_nullable_to_non_nullable
              as String,
      frequency: null == frequency
          ? _value.frequency
          : frequency // ignore: cast_nullable_to_non_nullable
              as String,
      duration: null == duration
          ? _value.duration
          : duration // ignore: cast_nullable_to_non_nullable
              as String,
      instructions: freezed == instructions
          ? _value.instructions
          : instructions // ignore: cast_nullable_to_non_nullable
              as String?,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: freezed == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$PrescriptionImpl implements _Prescription {
  const _$PrescriptionImpl(
      {required this.id,
      required this.consultationId,
      required this.medicationId,
      required this.medicationName,
      required this.dosage,
      required this.frequency,
      required this.duration,
      this.instructions,
      required this.startDate,
      this.endDate,
      this.isActive = false});

  factory _$PrescriptionImpl.fromJson(Map<String, dynamic> json) =>
      _$$PrescriptionImplFromJson(json);

  @override
  final int id;
  @override
  final int consultationId;
  @override
  final int medicationId;
  @override
  final String medicationName;
  @override
  final String dosage;
  @override
  final String frequency;
  @override
  final String duration;
  @override
  final String? instructions;
  @override
  final DateTime startDate;
  @override
  final DateTime? endDate;
  @override
  @JsonKey()
  final bool isActive;

  @override
  String toString() {
    return 'Prescription(id: $id, consultationId: $consultationId, medicationId: $medicationId, medicationName: $medicationName, dosage: $dosage, frequency: $frequency, duration: $duration, instructions: $instructions, startDate: $startDate, endDate: $endDate, isActive: $isActive)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$PrescriptionImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.consultationId, consultationId) ||
                other.consultationId == consultationId) &&
            (identical(other.medicationId, medicationId) ||
                other.medicationId == medicationId) &&
            (identical(other.medicationName, medicationName) ||
                other.medicationName == medicationName) &&
            (identical(other.dosage, dosage) || other.dosage == dosage) &&
            (identical(other.frequency, frequency) ||
                other.frequency == frequency) &&
            (identical(other.duration, duration) ||
                other.duration == duration) &&
            (identical(other.instructions, instructions) ||
                other.instructions == instructions) &&
            (identical(other.startDate, startDate) ||
                other.startDate == startDate) &&
            (identical(other.endDate, endDate) || other.endDate == endDate) &&
            (identical(other.isActive, isActive) ||
                other.isActive == isActive));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      consultationId,
      medicationId,
      medicationName,
      dosage,
      frequency,
      duration,
      instructions,
      startDate,
      endDate,
      isActive);

  /// Create a copy of Prescription
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$PrescriptionImplCopyWith<_$PrescriptionImpl> get copyWith =>
      __$$PrescriptionImplCopyWithImpl<_$PrescriptionImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$PrescriptionImplToJson(
      this,
    );
  }
}

abstract class _Prescription implements Prescription {
  const factory _Prescription(
      {required final int id,
      required final int consultationId,
      required final int medicationId,
      required final String medicationName,
      required final String dosage,
      required final String frequency,
      required final String duration,
      final String? instructions,
      required final DateTime startDate,
      final DateTime? endDate,
      final bool isActive}) = _$PrescriptionImpl;

  factory _Prescription.fromJson(Map<String, dynamic> json) =
      _$PrescriptionImpl.fromJson;

  @override
  int get id;
  @override
  int get consultationId;
  @override
  int get medicationId;
  @override
  String get medicationName;
  @override
  String get dosage;
  @override
  String get frequency;
  @override
  String get duration;
  @override
  String? get instructions;
  @override
  DateTime get startDate;
  @override
  DateTime? get endDate;
  @override
  bool get isActive;

  /// Create a copy of Prescription
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$PrescriptionImplCopyWith<_$PrescriptionImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

Medication _$MedicationFromJson(Map<String, dynamic> json) {
  return _Medication.fromJson(json);
}

/// @nodoc
mixin _$Medication {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get genericName => throw _privateConstructorUsedError;
  String? get brandName => throw _privateConstructorUsedError;
  String? get form => throw _privateConstructorUsedError;
  String? get strength => throw _privateConstructorUsedError;
  String? get manufacturer => throw _privateConstructorUsedError;
  String? get category => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  double? get unitPrice => throw _privateConstructorUsedError;
  String? get unit => throw _privateConstructorUsedError;
  bool get requiresPrescription => throw _privateConstructorUsedError;
  bool get isActive => throw _privateConstructorUsedError;

  /// Serializes this Medication to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of Medication
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $MedicationCopyWith<Medication> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $MedicationCopyWith<$Res> {
  factory $MedicationCopyWith(
          Medication value, $Res Function(Medication) then) =
      _$MedicationCopyWithImpl<$Res, Medication>;
  @useResult
  $Res call(
      {int id,
      String name,
      String? genericName,
      String? brandName,
      String? form,
      String? strength,
      String? manufacturer,
      String? category,
      String? description,
      double? unitPrice,
      String? unit,
      bool requiresPrescription,
      bool isActive});
}

/// @nodoc
class _$MedicationCopyWithImpl<$Res, $Val extends Medication>
    implements $MedicationCopyWith<$Res> {
  _$MedicationCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of Medication
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? genericName = freezed,
    Object? brandName = freezed,
    Object? form = freezed,
    Object? strength = freezed,
    Object? manufacturer = freezed,
    Object? category = freezed,
    Object? description = freezed,
    Object? unitPrice = freezed,
    Object? unit = freezed,
    Object? requiresPrescription = null,
    Object? isActive = null,
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
      genericName: freezed == genericName
          ? _value.genericName
          : genericName // ignore: cast_nullable_to_non_nullable
              as String?,
      brandName: freezed == brandName
          ? _value.brandName
          : brandName // ignore: cast_nullable_to_non_nullable
              as String?,
      form: freezed == form
          ? _value.form
          : form // ignore: cast_nullable_to_non_nullable
              as String?,
      strength: freezed == strength
          ? _value.strength
          : strength // ignore: cast_nullable_to_non_nullable
              as String?,
      manufacturer: freezed == manufacturer
          ? _value.manufacturer
          : manufacturer // ignore: cast_nullable_to_non_nullable
              as String?,
      category: freezed == category
          ? _value.category
          : category // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      unitPrice: freezed == unitPrice
          ? _value.unitPrice
          : unitPrice // ignore: cast_nullable_to_non_nullable
              as double?,
      unit: freezed == unit
          ? _value.unit
          : unit // ignore: cast_nullable_to_non_nullable
              as String?,
      requiresPrescription: null == requiresPrescription
          ? _value.requiresPrescription
          : requiresPrescription // ignore: cast_nullable_to_non_nullable
              as bool,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$MedicationImplCopyWith<$Res>
    implements $MedicationCopyWith<$Res> {
  factory _$$MedicationImplCopyWith(
          _$MedicationImpl value, $Res Function(_$MedicationImpl) then) =
      __$$MedicationImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      String? genericName,
      String? brandName,
      String? form,
      String? strength,
      String? manufacturer,
      String? category,
      String? description,
      double? unitPrice,
      String? unit,
      bool requiresPrescription,
      bool isActive});
}

/// @nodoc
class __$$MedicationImplCopyWithImpl<$Res>
    extends _$MedicationCopyWithImpl<$Res, _$MedicationImpl>
    implements _$$MedicationImplCopyWith<$Res> {
  __$$MedicationImplCopyWithImpl(
      _$MedicationImpl _value, $Res Function(_$MedicationImpl) _then)
      : super(_value, _then);

  /// Create a copy of Medication
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? genericName = freezed,
    Object? brandName = freezed,
    Object? form = freezed,
    Object? strength = freezed,
    Object? manufacturer = freezed,
    Object? category = freezed,
    Object? description = freezed,
    Object? unitPrice = freezed,
    Object? unit = freezed,
    Object? requiresPrescription = null,
    Object? isActive = null,
  }) {
    return _then(_$MedicationImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      genericName: freezed == genericName
          ? _value.genericName
          : genericName // ignore: cast_nullable_to_non_nullable
              as String?,
      brandName: freezed == brandName
          ? _value.brandName
          : brandName // ignore: cast_nullable_to_non_nullable
              as String?,
      form: freezed == form
          ? _value.form
          : form // ignore: cast_nullable_to_non_nullable
              as String?,
      strength: freezed == strength
          ? _value.strength
          : strength // ignore: cast_nullable_to_non_nullable
              as String?,
      manufacturer: freezed == manufacturer
          ? _value.manufacturer
          : manufacturer // ignore: cast_nullable_to_non_nullable
              as String?,
      category: freezed == category
          ? _value.category
          : category // ignore: cast_nullable_to_non_nullable
              as String?,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      unitPrice: freezed == unitPrice
          ? _value.unitPrice
          : unitPrice // ignore: cast_nullable_to_non_nullable
              as double?,
      unit: freezed == unit
          ? _value.unit
          : unit // ignore: cast_nullable_to_non_nullable
              as String?,
      requiresPrescription: null == requiresPrescription
          ? _value.requiresPrescription
          : requiresPrescription // ignore: cast_nullable_to_non_nullable
              as bool,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$MedicationImpl implements _Medication {
  const _$MedicationImpl(
      {required this.id,
      required this.name,
      this.genericName,
      this.brandName,
      this.form,
      this.strength,
      this.manufacturer,
      this.category,
      this.description,
      this.unitPrice,
      this.unit,
      this.requiresPrescription = true,
      this.isActive = true});

  factory _$MedicationImpl.fromJson(Map<String, dynamic> json) =>
      _$$MedicationImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final String? genericName;
  @override
  final String? brandName;
  @override
  final String? form;
  @override
  final String? strength;
  @override
  final String? manufacturer;
  @override
  final String? category;
  @override
  final String? description;
  @override
  final double? unitPrice;
  @override
  final String? unit;
  @override
  @JsonKey()
  final bool requiresPrescription;
  @override
  @JsonKey()
  final bool isActive;

  @override
  String toString() {
    return 'Medication(id: $id, name: $name, genericName: $genericName, brandName: $brandName, form: $form, strength: $strength, manufacturer: $manufacturer, category: $category, description: $description, unitPrice: $unitPrice, unit: $unit, requiresPrescription: $requiresPrescription, isActive: $isActive)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$MedicationImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.genericName, genericName) ||
                other.genericName == genericName) &&
            (identical(other.brandName, brandName) ||
                other.brandName == brandName) &&
            (identical(other.form, form) || other.form == form) &&
            (identical(other.strength, strength) ||
                other.strength == strength) &&
            (identical(other.manufacturer, manufacturer) ||
                other.manufacturer == manufacturer) &&
            (identical(other.category, category) ||
                other.category == category) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.unitPrice, unitPrice) ||
                other.unitPrice == unitPrice) &&
            (identical(other.unit, unit) || other.unit == unit) &&
            (identical(other.requiresPrescription, requiresPrescription) ||
                other.requiresPrescription == requiresPrescription) &&
            (identical(other.isActive, isActive) ||
                other.isActive == isActive));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      name,
      genericName,
      brandName,
      form,
      strength,
      manufacturer,
      category,
      description,
      unitPrice,
      unit,
      requiresPrescription,
      isActive);

  /// Create a copy of Medication
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$MedicationImplCopyWith<_$MedicationImpl> get copyWith =>
      __$$MedicationImplCopyWithImpl<_$MedicationImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$MedicationImplToJson(
      this,
    );
  }
}

abstract class _Medication implements Medication {
  const factory _Medication(
      {required final int id,
      required final String name,
      final String? genericName,
      final String? brandName,
      final String? form,
      final String? strength,
      final String? manufacturer,
      final String? category,
      final String? description,
      final double? unitPrice,
      final String? unit,
      final bool requiresPrescription,
      final bool isActive}) = _$MedicationImpl;

  factory _Medication.fromJson(Map<String, dynamic> json) =
      _$MedicationImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  String? get genericName;
  @override
  String? get brandName;
  @override
  String? get form;
  @override
  String? get strength;
  @override
  String? get manufacturer;
  @override
  String? get category;
  @override
  String? get description;
  @override
  double? get unitPrice;
  @override
  String? get unit;
  @override
  bool get requiresPrescription;
  @override
  bool get isActive;

  /// Create a copy of Medication
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$MedicationImplCopyWith<_$MedicationImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

PharmacyStock _$PharmacyStockFromJson(Map<String, dynamic> json) {
  return _PharmacyStock.fromJson(json);
}

/// @nodoc
mixin _$PharmacyStock {
  int get id => throw _privateConstructorUsedError;
  int get medicationId => throw _privateConstructorUsedError;
  String get medicationName => throw _privateConstructorUsedError;
  int get quantity => throw _privateConstructorUsedError;
  int get minStockLevel => throw _privateConstructorUsedError;
  int get maxStockLevel => throw _privateConstructorUsedError;
  String get location => throw _privateConstructorUsedError;
  DateTime get expiryDate => throw _privateConstructorUsedError;
  String? get batchNumber => throw _privateConstructorUsedError;
  double? get unitCost => throw _privateConstructorUsedError;
  double? get sellingPrice => throw _privateConstructorUsedError;
  int get reservedQuantity => throw _privateConstructorUsedError;
  bool get isActive => throw _privateConstructorUsedError;

  /// Serializes this PharmacyStock to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of PharmacyStock
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $PharmacyStockCopyWith<PharmacyStock> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $PharmacyStockCopyWith<$Res> {
  factory $PharmacyStockCopyWith(
          PharmacyStock value, $Res Function(PharmacyStock) then) =
      _$PharmacyStockCopyWithImpl<$Res, PharmacyStock>;
  @useResult
  $Res call(
      {int id,
      int medicationId,
      String medicationName,
      int quantity,
      int minStockLevel,
      int maxStockLevel,
      String location,
      DateTime expiryDate,
      String? batchNumber,
      double? unitCost,
      double? sellingPrice,
      int reservedQuantity,
      bool isActive});
}

/// @nodoc
class _$PharmacyStockCopyWithImpl<$Res, $Val extends PharmacyStock>
    implements $PharmacyStockCopyWith<$Res> {
  _$PharmacyStockCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of PharmacyStock
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? medicationId = null,
    Object? medicationName = null,
    Object? quantity = null,
    Object? minStockLevel = null,
    Object? maxStockLevel = null,
    Object? location = null,
    Object? expiryDate = null,
    Object? batchNumber = freezed,
    Object? unitCost = freezed,
    Object? sellingPrice = freezed,
    Object? reservedQuantity = null,
    Object? isActive = null,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      medicationId: null == medicationId
          ? _value.medicationId
          : medicationId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationName: null == medicationName
          ? _value.medicationName
          : medicationName // ignore: cast_nullable_to_non_nullable
              as String,
      quantity: null == quantity
          ? _value.quantity
          : quantity // ignore: cast_nullable_to_non_nullable
              as int,
      minStockLevel: null == minStockLevel
          ? _value.minStockLevel
          : minStockLevel // ignore: cast_nullable_to_non_nullable
              as int,
      maxStockLevel: null == maxStockLevel
          ? _value.maxStockLevel
          : maxStockLevel // ignore: cast_nullable_to_non_nullable
              as int,
      location: null == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String,
      expiryDate: null == expiryDate
          ? _value.expiryDate
          : expiryDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      batchNumber: freezed == batchNumber
          ? _value.batchNumber
          : batchNumber // ignore: cast_nullable_to_non_nullable
              as String?,
      unitCost: freezed == unitCost
          ? _value.unitCost
          : unitCost // ignore: cast_nullable_to_non_nullable
              as double?,
      sellingPrice: freezed == sellingPrice
          ? _value.sellingPrice
          : sellingPrice // ignore: cast_nullable_to_non_nullable
              as double?,
      reservedQuantity: null == reservedQuantity
          ? _value.reservedQuantity
          : reservedQuantity // ignore: cast_nullable_to_non_nullable
              as int,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$PharmacyStockImplCopyWith<$Res>
    implements $PharmacyStockCopyWith<$Res> {
  factory _$$PharmacyStockImplCopyWith(
          _$PharmacyStockImpl value, $Res Function(_$PharmacyStockImpl) then) =
      __$$PharmacyStockImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int medicationId,
      String medicationName,
      int quantity,
      int minStockLevel,
      int maxStockLevel,
      String location,
      DateTime expiryDate,
      String? batchNumber,
      double? unitCost,
      double? sellingPrice,
      int reservedQuantity,
      bool isActive});
}

/// @nodoc
class __$$PharmacyStockImplCopyWithImpl<$Res>
    extends _$PharmacyStockCopyWithImpl<$Res, _$PharmacyStockImpl>
    implements _$$PharmacyStockImplCopyWith<$Res> {
  __$$PharmacyStockImplCopyWithImpl(
      _$PharmacyStockImpl _value, $Res Function(_$PharmacyStockImpl) _then)
      : super(_value, _then);

  /// Create a copy of PharmacyStock
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? medicationId = null,
    Object? medicationName = null,
    Object? quantity = null,
    Object? minStockLevel = null,
    Object? maxStockLevel = null,
    Object? location = null,
    Object? expiryDate = null,
    Object? batchNumber = freezed,
    Object? unitCost = freezed,
    Object? sellingPrice = freezed,
    Object? reservedQuantity = null,
    Object? isActive = null,
  }) {
    return _then(_$PharmacyStockImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      medicationId: null == medicationId
          ? _value.medicationId
          : medicationId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationName: null == medicationName
          ? _value.medicationName
          : medicationName // ignore: cast_nullable_to_non_nullable
              as String,
      quantity: null == quantity
          ? _value.quantity
          : quantity // ignore: cast_nullable_to_non_nullable
              as int,
      minStockLevel: null == minStockLevel
          ? _value.minStockLevel
          : minStockLevel // ignore: cast_nullable_to_non_nullable
              as int,
      maxStockLevel: null == maxStockLevel
          ? _value.maxStockLevel
          : maxStockLevel // ignore: cast_nullable_to_non_nullable
              as int,
      location: null == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String,
      expiryDate: null == expiryDate
          ? _value.expiryDate
          : expiryDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      batchNumber: freezed == batchNumber
          ? _value.batchNumber
          : batchNumber // ignore: cast_nullable_to_non_nullable
              as String?,
      unitCost: freezed == unitCost
          ? _value.unitCost
          : unitCost // ignore: cast_nullable_to_non_nullable
              as double?,
      sellingPrice: freezed == sellingPrice
          ? _value.sellingPrice
          : sellingPrice // ignore: cast_nullable_to_non_nullable
              as double?,
      reservedQuantity: null == reservedQuantity
          ? _value.reservedQuantity
          : reservedQuantity // ignore: cast_nullable_to_non_nullable
              as int,
      isActive: null == isActive
          ? _value.isActive
          : isActive // ignore: cast_nullable_to_non_nullable
              as bool,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$PharmacyStockImpl extends _PharmacyStock {
  const _$PharmacyStockImpl(
      {required this.id,
      required this.medicationId,
      required this.medicationName,
      required this.quantity,
      required this.minStockLevel,
      required this.maxStockLevel,
      required this.location,
      required this.expiryDate,
      this.batchNumber,
      this.unitCost,
      this.sellingPrice,
      this.reservedQuantity = 0,
      this.isActive = true})
      : super._();

  factory _$PharmacyStockImpl.fromJson(Map<String, dynamic> json) =>
      _$$PharmacyStockImplFromJson(json);

  @override
  final int id;
  @override
  final int medicationId;
  @override
  final String medicationName;
  @override
  final int quantity;
  @override
  final int minStockLevel;
  @override
  final int maxStockLevel;
  @override
  final String location;
  @override
  final DateTime expiryDate;
  @override
  final String? batchNumber;
  @override
  final double? unitCost;
  @override
  final double? sellingPrice;
  @override
  @JsonKey()
  final int reservedQuantity;
  @override
  @JsonKey()
  final bool isActive;

  @override
  String toString() {
    return 'PharmacyStock(id: $id, medicationId: $medicationId, medicationName: $medicationName, quantity: $quantity, minStockLevel: $minStockLevel, maxStockLevel: $maxStockLevel, location: $location, expiryDate: $expiryDate, batchNumber: $batchNumber, unitCost: $unitCost, sellingPrice: $sellingPrice, reservedQuantity: $reservedQuantity, isActive: $isActive)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$PharmacyStockImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.medicationId, medicationId) ||
                other.medicationId == medicationId) &&
            (identical(other.medicationName, medicationName) ||
                other.medicationName == medicationName) &&
            (identical(other.quantity, quantity) ||
                other.quantity == quantity) &&
            (identical(other.minStockLevel, minStockLevel) ||
                other.minStockLevel == minStockLevel) &&
            (identical(other.maxStockLevel, maxStockLevel) ||
                other.maxStockLevel == maxStockLevel) &&
            (identical(other.location, location) ||
                other.location == location) &&
            (identical(other.expiryDate, expiryDate) ||
                other.expiryDate == expiryDate) &&
            (identical(other.batchNumber, batchNumber) ||
                other.batchNumber == batchNumber) &&
            (identical(other.unitCost, unitCost) ||
                other.unitCost == unitCost) &&
            (identical(other.sellingPrice, sellingPrice) ||
                other.sellingPrice == sellingPrice) &&
            (identical(other.reservedQuantity, reservedQuantity) ||
                other.reservedQuantity == reservedQuantity) &&
            (identical(other.isActive, isActive) ||
                other.isActive == isActive));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      medicationId,
      medicationName,
      quantity,
      minStockLevel,
      maxStockLevel,
      location,
      expiryDate,
      batchNumber,
      unitCost,
      sellingPrice,
      reservedQuantity,
      isActive);

  /// Create a copy of PharmacyStock
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$PharmacyStockImplCopyWith<_$PharmacyStockImpl> get copyWith =>
      __$$PharmacyStockImplCopyWithImpl<_$PharmacyStockImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$PharmacyStockImplToJson(
      this,
    );
  }
}

abstract class _PharmacyStock extends PharmacyStock {
  const factory _PharmacyStock(
      {required final int id,
      required final int medicationId,
      required final String medicationName,
      required final int quantity,
      required final int minStockLevel,
      required final int maxStockLevel,
      required final String location,
      required final DateTime expiryDate,
      final String? batchNumber,
      final double? unitCost,
      final double? sellingPrice,
      final int reservedQuantity,
      final bool isActive}) = _$PharmacyStockImpl;
  const _PharmacyStock._() : super._();

  factory _PharmacyStock.fromJson(Map<String, dynamic> json) =
      _$PharmacyStockImpl.fromJson;

  @override
  int get id;
  @override
  int get medicationId;
  @override
  String get medicationName;
  @override
  int get quantity;
  @override
  int get minStockLevel;
  @override
  int get maxStockLevel;
  @override
  String get location;
  @override
  DateTime get expiryDate;
  @override
  String? get batchNumber;
  @override
  double? get unitCost;
  @override
  double? get sellingPrice;
  @override
  int get reservedQuantity;
  @override
  bool get isActive;

  /// Create a copy of PharmacyStock
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$PharmacyStockImplCopyWith<_$PharmacyStockImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

HealthCampaign _$HealthCampaignFromJson(Map<String, dynamic> json) {
  return _HealthCampaign.fromJson(json);
}

/// @nodoc
mixin _$HealthCampaign {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  CampaignType get type => throw _privateConstructorUsedError;
  DateTime get startDate => throw _privateConstructorUsedError;
  DateTime get endDate => throw _privateConstructorUsedError;
  String? get location => throw _privateConstructorUsedError;
  int? get targetPopulation => throw _privateConstructorUsedError;
  int get registeredCount => throw _privateConstructorUsedError;
  int get attendedCount => throw _privateConstructorUsedError;
  CampaignStatus get status => throw _privateConstructorUsedError;
  int? get coordinatorId => throw _privateConstructorUsedError;
  String? get coordinatorName => throw _privateConstructorUsedError;
  List<String>? get targetGroups => throw _privateConstructorUsedError;
  List<String>? get servicesOffered => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this HealthCampaign to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of HealthCampaign
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $HealthCampaignCopyWith<HealthCampaign> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $HealthCampaignCopyWith<$Res> {
  factory $HealthCampaignCopyWith(
          HealthCampaign value, $Res Function(HealthCampaign) then) =
      _$HealthCampaignCopyWithImpl<$Res, HealthCampaign>;
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      CampaignType type,
      DateTime startDate,
      DateTime endDate,
      String? location,
      int? targetPopulation,
      int registeredCount,
      int attendedCount,
      CampaignStatus status,
      int? coordinatorId,
      String? coordinatorName,
      List<String>? targetGroups,
      List<String>? servicesOffered,
      String? notes,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$HealthCampaignCopyWithImpl<$Res, $Val extends HealthCampaign>
    implements $HealthCampaignCopyWith<$Res> {
  _$HealthCampaignCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of HealthCampaign
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? type = null,
    Object? startDate = null,
    Object? endDate = null,
    Object? location = freezed,
    Object? targetPopulation = freezed,
    Object? registeredCount = null,
    Object? attendedCount = null,
    Object? status = null,
    Object? coordinatorId = freezed,
    Object? coordinatorName = freezed,
    Object? targetGroups = freezed,
    Object? servicesOffered = freezed,
    Object? notes = freezed,
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
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as CampaignType,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: null == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
      targetPopulation: freezed == targetPopulation
          ? _value.targetPopulation
          : targetPopulation // ignore: cast_nullable_to_non_nullable
              as int?,
      registeredCount: null == registeredCount
          ? _value.registeredCount
          : registeredCount // ignore: cast_nullable_to_non_nullable
              as int,
      attendedCount: null == attendedCount
          ? _value.attendedCount
          : attendedCount // ignore: cast_nullable_to_non_nullable
              as int,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as CampaignStatus,
      coordinatorId: freezed == coordinatorId
          ? _value.coordinatorId
          : coordinatorId // ignore: cast_nullable_to_non_nullable
              as int?,
      coordinatorName: freezed == coordinatorName
          ? _value.coordinatorName
          : coordinatorName // ignore: cast_nullable_to_non_nullable
              as String?,
      targetGroups: freezed == targetGroups
          ? _value.targetGroups
          : targetGroups // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      servicesOffered: freezed == servicesOffered
          ? _value.servicesOffered
          : servicesOffered // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
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
abstract class _$$HealthCampaignImplCopyWith<$Res>
    implements $HealthCampaignCopyWith<$Res> {
  factory _$$HealthCampaignImplCopyWith(_$HealthCampaignImpl value,
          $Res Function(_$HealthCampaignImpl) then) =
      __$$HealthCampaignImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      CampaignType type,
      DateTime startDate,
      DateTime endDate,
      String? location,
      int? targetPopulation,
      int registeredCount,
      int attendedCount,
      CampaignStatus status,
      int? coordinatorId,
      String? coordinatorName,
      List<String>? targetGroups,
      List<String>? servicesOffered,
      String? notes,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$HealthCampaignImplCopyWithImpl<$Res>
    extends _$HealthCampaignCopyWithImpl<$Res, _$HealthCampaignImpl>
    implements _$$HealthCampaignImplCopyWith<$Res> {
  __$$HealthCampaignImplCopyWithImpl(
      _$HealthCampaignImpl _value, $Res Function(_$HealthCampaignImpl) _then)
      : super(_value, _then);

  /// Create a copy of HealthCampaign
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? type = null,
    Object? startDate = null,
    Object? endDate = null,
    Object? location = freezed,
    Object? targetPopulation = freezed,
    Object? registeredCount = null,
    Object? attendedCount = null,
    Object? status = null,
    Object? coordinatorId = freezed,
    Object? coordinatorName = freezed,
    Object? targetGroups = freezed,
    Object? servicesOffered = freezed,
    Object? notes = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$HealthCampaignImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as CampaignType,
      startDate: null == startDate
          ? _value.startDate
          : startDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endDate: null == endDate
          ? _value.endDate
          : endDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
      targetPopulation: freezed == targetPopulation
          ? _value.targetPopulation
          : targetPopulation // ignore: cast_nullable_to_non_nullable
              as int?,
      registeredCount: null == registeredCount
          ? _value.registeredCount
          : registeredCount // ignore: cast_nullable_to_non_nullable
              as int,
      attendedCount: null == attendedCount
          ? _value.attendedCount
          : attendedCount // ignore: cast_nullable_to_non_nullable
              as int,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as CampaignStatus,
      coordinatorId: freezed == coordinatorId
          ? _value.coordinatorId
          : coordinatorId // ignore: cast_nullable_to_non_nullable
              as int?,
      coordinatorName: freezed == coordinatorName
          ? _value.coordinatorName
          : coordinatorName // ignore: cast_nullable_to_non_nullable
              as String?,
      targetGroups: freezed == targetGroups
          ? _value._targetGroups
          : targetGroups // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      servicesOffered: freezed == servicesOffered
          ? _value._servicesOffered
          : servicesOffered // ignore: cast_nullable_to_non_nullable
              as List<String>?,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
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
class _$HealthCampaignImpl extends _HealthCampaign {
  const _$HealthCampaignImpl(
      {required this.id,
      required this.name,
      this.description,
      required this.type,
      required this.startDate,
      required this.endDate,
      this.location,
      this.targetPopulation,
      this.registeredCount = 0,
      this.attendedCount = 0,
      required this.status,
      this.coordinatorId,
      this.coordinatorName,
      final List<String>? targetGroups,
      final List<String>? servicesOffered,
      this.notes,
      required this.createdAt,
      this.updatedAt})
      : _targetGroups = targetGroups,
        _servicesOffered = servicesOffered,
        super._();

  factory _$HealthCampaignImpl.fromJson(Map<String, dynamic> json) =>
      _$$HealthCampaignImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final String? description;
  @override
  final CampaignType type;
  @override
  final DateTime startDate;
  @override
  final DateTime endDate;
  @override
  final String? location;
  @override
  final int? targetPopulation;
  @override
  @JsonKey()
  final int registeredCount;
  @override
  @JsonKey()
  final int attendedCount;
  @override
  final CampaignStatus status;
  @override
  final int? coordinatorId;
  @override
  final String? coordinatorName;
  final List<String>? _targetGroups;
  @override
  List<String>? get targetGroups {
    final value = _targetGroups;
    if (value == null) return null;
    if (_targetGroups is EqualUnmodifiableListView) return _targetGroups;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  final List<String>? _servicesOffered;
  @override
  List<String>? get servicesOffered {
    final value = _servicesOffered;
    if (value == null) return null;
    if (_servicesOffered is EqualUnmodifiableListView) return _servicesOffered;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(value);
  }

  @override
  final String? notes;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'HealthCampaign(id: $id, name: $name, description: $description, type: $type, startDate: $startDate, endDate: $endDate, location: $location, targetPopulation: $targetPopulation, registeredCount: $registeredCount, attendedCount: $attendedCount, status: $status, coordinatorId: $coordinatorId, coordinatorName: $coordinatorName, targetGroups: $targetGroups, servicesOffered: $servicesOffered, notes: $notes, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$HealthCampaignImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.type, type) || other.type == type) &&
            (identical(other.startDate, startDate) ||
                other.startDate == startDate) &&
            (identical(other.endDate, endDate) || other.endDate == endDate) &&
            (identical(other.location, location) ||
                other.location == location) &&
            (identical(other.targetPopulation, targetPopulation) ||
                other.targetPopulation == targetPopulation) &&
            (identical(other.registeredCount, registeredCount) ||
                other.registeredCount == registeredCount) &&
            (identical(other.attendedCount, attendedCount) ||
                other.attendedCount == attendedCount) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.coordinatorId, coordinatorId) ||
                other.coordinatorId == coordinatorId) &&
            (identical(other.coordinatorName, coordinatorName) ||
                other.coordinatorName == coordinatorName) &&
            const DeepCollectionEquality()
                .equals(other._targetGroups, _targetGroups) &&
            const DeepCollectionEquality()
                .equals(other._servicesOffered, _servicesOffered) &&
            (identical(other.notes, notes) || other.notes == notes) &&
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
      type,
      startDate,
      endDate,
      location,
      targetPopulation,
      registeredCount,
      attendedCount,
      status,
      coordinatorId,
      coordinatorName,
      const DeepCollectionEquality().hash(_targetGroups),
      const DeepCollectionEquality().hash(_servicesOffered),
      notes,
      createdAt,
      updatedAt);

  /// Create a copy of HealthCampaign
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$HealthCampaignImplCopyWith<_$HealthCampaignImpl> get copyWith =>
      __$$HealthCampaignImplCopyWithImpl<_$HealthCampaignImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$HealthCampaignImplToJson(
      this,
    );
  }
}

abstract class _HealthCampaign extends HealthCampaign {
  const factory _HealthCampaign(
      {required final int id,
      required final String name,
      final String? description,
      required final CampaignType type,
      required final DateTime startDate,
      required final DateTime endDate,
      final String? location,
      final int? targetPopulation,
      final int registeredCount,
      final int attendedCount,
      required final CampaignStatus status,
      final int? coordinatorId,
      final String? coordinatorName,
      final List<String>? targetGroups,
      final List<String>? servicesOffered,
      final String? notes,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$HealthCampaignImpl;
  const _HealthCampaign._() : super._();

  factory _HealthCampaign.fromJson(Map<String, dynamic> json) =
      _$HealthCampaignImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  String? get description;
  @override
  CampaignType get type;
  @override
  DateTime get startDate;
  @override
  DateTime get endDate;
  @override
  String? get location;
  @override
  int? get targetPopulation;
  @override
  int get registeredCount;
  @override
  int get attendedCount;
  @override
  CampaignStatus get status;
  @override
  int? get coordinatorId;
  @override
  String? get coordinatorName;
  @override
  List<String>? get targetGroups;
  @override
  List<String>? get servicesOffered;
  @override
  String? get notes;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of HealthCampaign
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$HealthCampaignImplCopyWith<_$HealthCampaignImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

CampaignParticipant _$CampaignParticipantFromJson(Map<String, dynamic> json) {
  return _CampaignParticipant.fromJson(json);
}

/// @nodoc
mixin _$CampaignParticipant {
  int get id => throw _privateConstructorUsedError;
  int get campaignId => throw _privateConstructorUsedError;
  int get patientId => throw _privateConstructorUsedError;
  String get patientName => throw _privateConstructorUsedError;
  DateTime get registrationDate => throw _privateConstructorUsedError;
  DateTime? get attendanceDate => throw _privateConstructorUsedError;
  ParticipationStatus get status => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;

  /// Serializes this CampaignParticipant to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of CampaignParticipant
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $CampaignParticipantCopyWith<CampaignParticipant> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $CampaignParticipantCopyWith<$Res> {
  factory $CampaignParticipantCopyWith(
          CampaignParticipant value, $Res Function(CampaignParticipant) then) =
      _$CampaignParticipantCopyWithImpl<$Res, CampaignParticipant>;
  @useResult
  $Res call(
      {int id,
      int campaignId,
      int patientId,
      String patientName,
      DateTime registrationDate,
      DateTime? attendanceDate,
      ParticipationStatus status,
      String? notes});
}

/// @nodoc
class _$CampaignParticipantCopyWithImpl<$Res, $Val extends CampaignParticipant>
    implements $CampaignParticipantCopyWith<$Res> {
  _$CampaignParticipantCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of CampaignParticipant
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? campaignId = null,
    Object? patientId = null,
    Object? patientName = null,
    Object? registrationDate = null,
    Object? attendanceDate = freezed,
    Object? status = null,
    Object? notes = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      campaignId: null == campaignId
          ? _value.campaignId
          : campaignId // ignore: cast_nullable_to_non_nullable
              as int,
      patientId: null == patientId
          ? _value.patientId
          : patientId // ignore: cast_nullable_to_non_nullable
              as int,
      patientName: null == patientName
          ? _value.patientName
          : patientName // ignore: cast_nullable_to_non_nullable
              as String,
      registrationDate: null == registrationDate
          ? _value.registrationDate
          : registrationDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      attendanceDate: freezed == attendanceDate
          ? _value.attendanceDate
          : attendanceDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as ParticipationStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$CampaignParticipantImplCopyWith<$Res>
    implements $CampaignParticipantCopyWith<$Res> {
  factory _$$CampaignParticipantImplCopyWith(_$CampaignParticipantImpl value,
          $Res Function(_$CampaignParticipantImpl) then) =
      __$$CampaignParticipantImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int campaignId,
      int patientId,
      String patientName,
      DateTime registrationDate,
      DateTime? attendanceDate,
      ParticipationStatus status,
      String? notes});
}

/// @nodoc
class __$$CampaignParticipantImplCopyWithImpl<$Res>
    extends _$CampaignParticipantCopyWithImpl<$Res, _$CampaignParticipantImpl>
    implements _$$CampaignParticipantImplCopyWith<$Res> {
  __$$CampaignParticipantImplCopyWithImpl(_$CampaignParticipantImpl _value,
      $Res Function(_$CampaignParticipantImpl) _then)
      : super(_value, _then);

  /// Create a copy of CampaignParticipant
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? campaignId = null,
    Object? patientId = null,
    Object? patientName = null,
    Object? registrationDate = null,
    Object? attendanceDate = freezed,
    Object? status = null,
    Object? notes = freezed,
  }) {
    return _then(_$CampaignParticipantImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      campaignId: null == campaignId
          ? _value.campaignId
          : campaignId // ignore: cast_nullable_to_non_nullable
              as int,
      patientId: null == patientId
          ? _value.patientId
          : patientId // ignore: cast_nullable_to_non_nullable
              as int,
      patientName: null == patientName
          ? _value.patientName
          : patientName // ignore: cast_nullable_to_non_nullable
              as String,
      registrationDate: null == registrationDate
          ? _value.registrationDate
          : registrationDate // ignore: cast_nullable_to_non_nullable
              as DateTime,
      attendanceDate: freezed == attendanceDate
          ? _value.attendanceDate
          : attendanceDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as ParticipationStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$CampaignParticipantImpl implements _CampaignParticipant {
  const _$CampaignParticipantImpl(
      {required this.id,
      required this.campaignId,
      required this.patientId,
      required this.patientName,
      required this.registrationDate,
      this.attendanceDate,
      required this.status,
      this.notes});

  factory _$CampaignParticipantImpl.fromJson(Map<String, dynamic> json) =>
      _$$CampaignParticipantImplFromJson(json);

  @override
  final int id;
  @override
  final int campaignId;
  @override
  final int patientId;
  @override
  final String patientName;
  @override
  final DateTime registrationDate;
  @override
  final DateTime? attendanceDate;
  @override
  final ParticipationStatus status;
  @override
  final String? notes;

  @override
  String toString() {
    return 'CampaignParticipant(id: $id, campaignId: $campaignId, patientId: $patientId, patientName: $patientName, registrationDate: $registrationDate, attendanceDate: $attendanceDate, status: $status, notes: $notes)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$CampaignParticipantImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.campaignId, campaignId) ||
                other.campaignId == campaignId) &&
            (identical(other.patientId, patientId) ||
                other.patientId == patientId) &&
            (identical(other.patientName, patientName) ||
                other.patientName == patientName) &&
            (identical(other.registrationDate, registrationDate) ||
                other.registrationDate == registrationDate) &&
            (identical(other.attendanceDate, attendanceDate) ||
                other.attendanceDate == attendanceDate) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.notes, notes) || other.notes == notes));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, campaignId, patientId,
      patientName, registrationDate, attendanceDate, status, notes);

  /// Create a copy of CampaignParticipant
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$CampaignParticipantImplCopyWith<_$CampaignParticipantImpl> get copyWith =>
      __$$CampaignParticipantImplCopyWithImpl<_$CampaignParticipantImpl>(
          this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$CampaignParticipantImplToJson(
      this,
    );
  }
}

abstract class _CampaignParticipant implements CampaignParticipant {
  const factory _CampaignParticipant(
      {required final int id,
      required final int campaignId,
      required final int patientId,
      required final String patientName,
      required final DateTime registrationDate,
      final DateTime? attendanceDate,
      required final ParticipationStatus status,
      final String? notes}) = _$CampaignParticipantImpl;

  factory _CampaignParticipant.fromJson(Map<String, dynamic> json) =
      _$CampaignParticipantImpl.fromJson;

  @override
  int get id;
  @override
  int get campaignId;
  @override
  int get patientId;
  @override
  String get patientName;
  @override
  DateTime get registrationDate;
  @override
  DateTime? get attendanceDate;
  @override
  ParticipationStatus get status;
  @override
  String? get notes;

  /// Create a copy of CampaignParticipant
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$CampaignParticipantImplCopyWith<_$CampaignParticipantImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

MedicalKit _$MedicalKitFromJson(Map<String, dynamic> json) {
  return _MedicalKit.fromJson(json);
}

/// @nodoc
mixin _$MedicalKit {
  int get id => throw _privateConstructorUsedError;
  String get name => throw _privateConstructorUsedError;
  String? get description => throw _privateConstructorUsedError;
  KitType get type => throw _privateConstructorUsedError;
  List<KitItem> get items => throw _privateConstructorUsedError;
  String? get location => throw _privateConstructorUsedError;
  int? get assignedToId => throw _privateConstructorUsedError;
  String? get assignedToName => throw _privateConstructorUsedError;
  DateTime? get lastInspectionDate => throw _privateConstructorUsedError;
  DateTime? get nextInspectionDate => throw _privateConstructorUsedError;
  KitStatus get status => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;
  DateTime get createdAt => throw _privateConstructorUsedError;
  DateTime? get updatedAt => throw _privateConstructorUsedError;

  /// Serializes this MedicalKit to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of MedicalKit
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $MedicalKitCopyWith<MedicalKit> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $MedicalKitCopyWith<$Res> {
  factory $MedicalKitCopyWith(
          MedicalKit value, $Res Function(MedicalKit) then) =
      _$MedicalKitCopyWithImpl<$Res, MedicalKit>;
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      KitType type,
      List<KitItem> items,
      String? location,
      int? assignedToId,
      String? assignedToName,
      DateTime? lastInspectionDate,
      DateTime? nextInspectionDate,
      KitStatus status,
      String? notes,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class _$MedicalKitCopyWithImpl<$Res, $Val extends MedicalKit>
    implements $MedicalKitCopyWith<$Res> {
  _$MedicalKitCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of MedicalKit
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? type = null,
    Object? items = null,
    Object? location = freezed,
    Object? assignedToId = freezed,
    Object? assignedToName = freezed,
    Object? lastInspectionDate = freezed,
    Object? nextInspectionDate = freezed,
    Object? status = null,
    Object? notes = freezed,
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
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as KitType,
      items: null == items
          ? _value.items
          : items // ignore: cast_nullable_to_non_nullable
              as List<KitItem>,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
      assignedToId: freezed == assignedToId
          ? _value.assignedToId
          : assignedToId // ignore: cast_nullable_to_non_nullable
              as int?,
      assignedToName: freezed == assignedToName
          ? _value.assignedToName
          : assignedToName // ignore: cast_nullable_to_non_nullable
              as String?,
      lastInspectionDate: freezed == lastInspectionDate
          ? _value.lastInspectionDate
          : lastInspectionDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      nextInspectionDate: freezed == nextInspectionDate
          ? _value.nextInspectionDate
          : nextInspectionDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as KitStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
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
abstract class _$$MedicalKitImplCopyWith<$Res>
    implements $MedicalKitCopyWith<$Res> {
  factory _$$MedicalKitImplCopyWith(
          _$MedicalKitImpl value, $Res Function(_$MedicalKitImpl) then) =
      __$$MedicalKitImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      String name,
      String? description,
      KitType type,
      List<KitItem> items,
      String? location,
      int? assignedToId,
      String? assignedToName,
      DateTime? lastInspectionDate,
      DateTime? nextInspectionDate,
      KitStatus status,
      String? notes,
      DateTime createdAt,
      DateTime? updatedAt});
}

/// @nodoc
class __$$MedicalKitImplCopyWithImpl<$Res>
    extends _$MedicalKitCopyWithImpl<$Res, _$MedicalKitImpl>
    implements _$$MedicalKitImplCopyWith<$Res> {
  __$$MedicalKitImplCopyWithImpl(
      _$MedicalKitImpl _value, $Res Function(_$MedicalKitImpl) _then)
      : super(_value, _then);

  /// Create a copy of MedicalKit
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? name = null,
    Object? description = freezed,
    Object? type = null,
    Object? items = null,
    Object? location = freezed,
    Object? assignedToId = freezed,
    Object? assignedToName = freezed,
    Object? lastInspectionDate = freezed,
    Object? nextInspectionDate = freezed,
    Object? status = null,
    Object? notes = freezed,
    Object? createdAt = null,
    Object? updatedAt = freezed,
  }) {
    return _then(_$MedicalKitImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      name: null == name
          ? _value.name
          : name // ignore: cast_nullable_to_non_nullable
              as String,
      description: freezed == description
          ? _value.description
          : description // ignore: cast_nullable_to_non_nullable
              as String?,
      type: null == type
          ? _value.type
          : type // ignore: cast_nullable_to_non_nullable
              as KitType,
      items: null == items
          ? _value._items
          : items // ignore: cast_nullable_to_non_nullable
              as List<KitItem>,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
      assignedToId: freezed == assignedToId
          ? _value.assignedToId
          : assignedToId // ignore: cast_nullable_to_non_nullable
              as int?,
      assignedToName: freezed == assignedToName
          ? _value.assignedToName
          : assignedToName // ignore: cast_nullable_to_non_nullable
              as String?,
      lastInspectionDate: freezed == lastInspectionDate
          ? _value.lastInspectionDate
          : lastInspectionDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      nextInspectionDate: freezed == nextInspectionDate
          ? _value.nextInspectionDate
          : nextInspectionDate // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as KitStatus,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
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
class _$MedicalKitImpl extends _MedicalKit {
  const _$MedicalKitImpl(
      {required this.id,
      required this.name,
      this.description,
      required this.type,
      required final List<KitItem> items,
      this.location,
      this.assignedToId,
      this.assignedToName,
      this.lastInspectionDate,
      this.nextInspectionDate,
      required this.status,
      this.notes,
      required this.createdAt,
      this.updatedAt})
      : _items = items,
        super._();

  factory _$MedicalKitImpl.fromJson(Map<String, dynamic> json) =>
      _$$MedicalKitImplFromJson(json);

  @override
  final int id;
  @override
  final String name;
  @override
  final String? description;
  @override
  final KitType type;
  final List<KitItem> _items;
  @override
  List<KitItem> get items {
    if (_items is EqualUnmodifiableListView) return _items;
    // ignore: implicit_dynamic_type
    return EqualUnmodifiableListView(_items);
  }

  @override
  final String? location;
  @override
  final int? assignedToId;
  @override
  final String? assignedToName;
  @override
  final DateTime? lastInspectionDate;
  @override
  final DateTime? nextInspectionDate;
  @override
  final KitStatus status;
  @override
  final String? notes;
  @override
  final DateTime createdAt;
  @override
  final DateTime? updatedAt;

  @override
  String toString() {
    return 'MedicalKit(id: $id, name: $name, description: $description, type: $type, items: $items, location: $location, assignedToId: $assignedToId, assignedToName: $assignedToName, lastInspectionDate: $lastInspectionDate, nextInspectionDate: $nextInspectionDate, status: $status, notes: $notes, createdAt: $createdAt, updatedAt: $updatedAt)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$MedicalKitImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.name, name) || other.name == name) &&
            (identical(other.description, description) ||
                other.description == description) &&
            (identical(other.type, type) || other.type == type) &&
            const DeepCollectionEquality().equals(other._items, _items) &&
            (identical(other.location, location) ||
                other.location == location) &&
            (identical(other.assignedToId, assignedToId) ||
                other.assignedToId == assignedToId) &&
            (identical(other.assignedToName, assignedToName) ||
                other.assignedToName == assignedToName) &&
            (identical(other.lastInspectionDate, lastInspectionDate) ||
                other.lastInspectionDate == lastInspectionDate) &&
            (identical(other.nextInspectionDate, nextInspectionDate) ||
                other.nextInspectionDate == nextInspectionDate) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.notes, notes) || other.notes == notes) &&
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
      type,
      const DeepCollectionEquality().hash(_items),
      location,
      assignedToId,
      assignedToName,
      lastInspectionDate,
      nextInspectionDate,
      status,
      notes,
      createdAt,
      updatedAt);

  /// Create a copy of MedicalKit
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$MedicalKitImplCopyWith<_$MedicalKitImpl> get copyWith =>
      __$$MedicalKitImplCopyWithImpl<_$MedicalKitImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$MedicalKitImplToJson(
      this,
    );
  }
}

abstract class _MedicalKit extends MedicalKit {
  const factory _MedicalKit(
      {required final int id,
      required final String name,
      final String? description,
      required final KitType type,
      required final List<KitItem> items,
      final String? location,
      final int? assignedToId,
      final String? assignedToName,
      final DateTime? lastInspectionDate,
      final DateTime? nextInspectionDate,
      required final KitStatus status,
      final String? notes,
      required final DateTime createdAt,
      final DateTime? updatedAt}) = _$MedicalKitImpl;
  const _MedicalKit._() : super._();

  factory _MedicalKit.fromJson(Map<String, dynamic> json) =
      _$MedicalKitImpl.fromJson;

  @override
  int get id;
  @override
  String get name;
  @override
  String? get description;
  @override
  KitType get type;
  @override
  List<KitItem> get items;
  @override
  String? get location;
  @override
  int? get assignedToId;
  @override
  String? get assignedToName;
  @override
  DateTime? get lastInspectionDate;
  @override
  DateTime? get nextInspectionDate;
  @override
  KitStatus get status;
  @override
  String? get notes;
  @override
  DateTime get createdAt;
  @override
  DateTime? get updatedAt;

  /// Create a copy of MedicalKit
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$MedicalKitImplCopyWith<_$MedicalKitImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

KitItem _$KitItemFromJson(Map<String, dynamic> json) {
  return _KitItem.fromJson(json);
}

/// @nodoc
mixin _$KitItem {
  int get id => throw _privateConstructorUsedError;
  int get kitId => throw _privateConstructorUsedError;
  int get medicationId => throw _privateConstructorUsedError;
  String get medicationName => throw _privateConstructorUsedError;
  int get requiredQuantity => throw _privateConstructorUsedError;
  int get currentQuantity => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;

  /// Serializes this KitItem to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of KitItem
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $KitItemCopyWith<KitItem> get copyWith => throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $KitItemCopyWith<$Res> {
  factory $KitItemCopyWith(KitItem value, $Res Function(KitItem) then) =
      _$KitItemCopyWithImpl<$Res, KitItem>;
  @useResult
  $Res call(
      {int id,
      int kitId,
      int medicationId,
      String medicationName,
      int requiredQuantity,
      int currentQuantity,
      String? notes});
}

/// @nodoc
class _$KitItemCopyWithImpl<$Res, $Val extends KitItem>
    implements $KitItemCopyWith<$Res> {
  _$KitItemCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of KitItem
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? kitId = null,
    Object? medicationId = null,
    Object? medicationName = null,
    Object? requiredQuantity = null,
    Object? currentQuantity = null,
    Object? notes = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      kitId: null == kitId
          ? _value.kitId
          : kitId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationId: null == medicationId
          ? _value.medicationId
          : medicationId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationName: null == medicationName
          ? _value.medicationName
          : medicationName // ignore: cast_nullable_to_non_nullable
              as String,
      requiredQuantity: null == requiredQuantity
          ? _value.requiredQuantity
          : requiredQuantity // ignore: cast_nullable_to_non_nullable
              as int,
      currentQuantity: null == currentQuantity
          ? _value.currentQuantity
          : currentQuantity // ignore: cast_nullable_to_non_nullable
              as int,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$KitItemImplCopyWith<$Res> implements $KitItemCopyWith<$Res> {
  factory _$$KitItemImplCopyWith(
          _$KitItemImpl value, $Res Function(_$KitItemImpl) then) =
      __$$KitItemImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int kitId,
      int medicationId,
      String medicationName,
      int requiredQuantity,
      int currentQuantity,
      String? notes});
}

/// @nodoc
class __$$KitItemImplCopyWithImpl<$Res>
    extends _$KitItemCopyWithImpl<$Res, _$KitItemImpl>
    implements _$$KitItemImplCopyWith<$Res> {
  __$$KitItemImplCopyWithImpl(
      _$KitItemImpl _value, $Res Function(_$KitItemImpl) _then)
      : super(_value, _then);

  /// Create a copy of KitItem
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? kitId = null,
    Object? medicationId = null,
    Object? medicationName = null,
    Object? requiredQuantity = null,
    Object? currentQuantity = null,
    Object? notes = freezed,
  }) {
    return _then(_$KitItemImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      kitId: null == kitId
          ? _value.kitId
          : kitId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationId: null == medicationId
          ? _value.medicationId
          : medicationId // ignore: cast_nullable_to_non_nullable
              as int,
      medicationName: null == medicationName
          ? _value.medicationName
          : medicationName // ignore: cast_nullable_to_non_nullable
              as String,
      requiredQuantity: null == requiredQuantity
          ? _value.requiredQuantity
          : requiredQuantity // ignore: cast_nullable_to_non_nullable
              as int,
      currentQuantity: null == currentQuantity
          ? _value.currentQuantity
          : currentQuantity // ignore: cast_nullable_to_non_nullable
              as int,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$KitItemImpl extends _KitItem {
  const _$KitItemImpl(
      {required this.id,
      required this.kitId,
      required this.medicationId,
      required this.medicationName,
      required this.requiredQuantity,
      required this.currentQuantity,
      this.notes})
      : super._();

  factory _$KitItemImpl.fromJson(Map<String, dynamic> json) =>
      _$$KitItemImplFromJson(json);

  @override
  final int id;
  @override
  final int kitId;
  @override
  final int medicationId;
  @override
  final String medicationName;
  @override
  final int requiredQuantity;
  @override
  final int currentQuantity;
  @override
  final String? notes;

  @override
  String toString() {
    return 'KitItem(id: $id, kitId: $kitId, medicationId: $medicationId, medicationName: $medicationName, requiredQuantity: $requiredQuantity, currentQuantity: $currentQuantity, notes: $notes)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$KitItemImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.kitId, kitId) || other.kitId == kitId) &&
            (identical(other.medicationId, medicationId) ||
                other.medicationId == medicationId) &&
            (identical(other.medicationName, medicationName) ||
                other.medicationName == medicationName) &&
            (identical(other.requiredQuantity, requiredQuantity) ||
                other.requiredQuantity == requiredQuantity) &&
            (identical(other.currentQuantity, currentQuantity) ||
                other.currentQuantity == currentQuantity) &&
            (identical(other.notes, notes) || other.notes == notes));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(runtimeType, id, kitId, medicationId,
      medicationName, requiredQuantity, currentQuantity, notes);

  /// Create a copy of KitItem
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$KitItemImplCopyWith<_$KitItemImpl> get copyWith =>
      __$$KitItemImplCopyWithImpl<_$KitItemImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$KitItemImplToJson(
      this,
    );
  }
}

abstract class _KitItem extends KitItem {
  const factory _KitItem(
      {required final int id,
      required final int kitId,
      required final int medicationId,
      required final String medicationName,
      required final int requiredQuantity,
      required final int currentQuantity,
      final String? notes}) = _$KitItemImpl;
  const _KitItem._() : super._();

  factory _KitItem.fromJson(Map<String, dynamic> json) = _$KitItemImpl.fromJson;

  @override
  int get id;
  @override
  int get kitId;
  @override
  int get medicationId;
  @override
  String get medicationName;
  @override
  int get requiredQuantity;
  @override
  int get currentQuantity;
  @override
  String? get notes;

  /// Create a copy of KitItem
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$KitItemImplCopyWith<_$KitItemImpl> get copyWith =>
      throw _privateConstructorUsedError;
}

StaffDuty _$StaffDutyFromJson(Map<String, dynamic> json) {
  return _StaffDuty.fromJson(json);
}

/// @nodoc
mixin _$StaffDuty {
  int get id => throw _privateConstructorUsedError;
  int get staffId => throw _privateConstructorUsedError;
  String get staffName => throw _privateConstructorUsedError;
  String get role => throw _privateConstructorUsedError;
  DateTime get startTime => throw _privateConstructorUsedError;
  DateTime get endTime => throw _privateConstructorUsedError;
  DutyStatus get status => throw _privateConstructorUsedError;
  String? get location => throw _privateConstructorUsedError;
  String? get notes => throw _privateConstructorUsedError;
  DateTime? get actualStartTime => throw _privateConstructorUsedError;
  DateTime? get actualEndTime => throw _privateConstructorUsedError;

  /// Serializes this StaffDuty to a JSON map.
  Map<String, dynamic> toJson() => throw _privateConstructorUsedError;

  /// Create a copy of StaffDuty
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  $StaffDutyCopyWith<StaffDuty> get copyWith =>
      throw _privateConstructorUsedError;
}

/// @nodoc
abstract class $StaffDutyCopyWith<$Res> {
  factory $StaffDutyCopyWith(StaffDuty value, $Res Function(StaffDuty) then) =
      _$StaffDutyCopyWithImpl<$Res, StaffDuty>;
  @useResult
  $Res call(
      {int id,
      int staffId,
      String staffName,
      String role,
      DateTime startTime,
      DateTime endTime,
      DutyStatus status,
      String? location,
      String? notes,
      DateTime? actualStartTime,
      DateTime? actualEndTime});
}

/// @nodoc
class _$StaffDutyCopyWithImpl<$Res, $Val extends StaffDuty>
    implements $StaffDutyCopyWith<$Res> {
  _$StaffDutyCopyWithImpl(this._value, this._then);

  // ignore: unused_field
  final $Val _value;
  // ignore: unused_field
  final $Res Function($Val) _then;

  /// Create a copy of StaffDuty
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? staffId = null,
    Object? staffName = null,
    Object? role = null,
    Object? startTime = null,
    Object? endTime = null,
    Object? status = null,
    Object? location = freezed,
    Object? notes = freezed,
    Object? actualStartTime = freezed,
    Object? actualEndTime = freezed,
  }) {
    return _then(_value.copyWith(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      staffId: null == staffId
          ? _value.staffId
          : staffId // ignore: cast_nullable_to_non_nullable
              as int,
      staffName: null == staffName
          ? _value.staffName
          : staffName // ignore: cast_nullable_to_non_nullable
              as String,
      role: null == role
          ? _value.role
          : role // ignore: cast_nullable_to_non_nullable
              as String,
      startTime: null == startTime
          ? _value.startTime
          : startTime // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endTime: null == endTime
          ? _value.endTime
          : endTime // ignore: cast_nullable_to_non_nullable
              as DateTime,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as DutyStatus,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      actualStartTime: freezed == actualStartTime
          ? _value.actualStartTime
          : actualStartTime // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      actualEndTime: freezed == actualEndTime
          ? _value.actualEndTime
          : actualEndTime // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ) as $Val);
  }
}

/// @nodoc
abstract class _$$StaffDutyImplCopyWith<$Res>
    implements $StaffDutyCopyWith<$Res> {
  factory _$$StaffDutyImplCopyWith(
          _$StaffDutyImpl value, $Res Function(_$StaffDutyImpl) then) =
      __$$StaffDutyImplCopyWithImpl<$Res>;
  @override
  @useResult
  $Res call(
      {int id,
      int staffId,
      String staffName,
      String role,
      DateTime startTime,
      DateTime endTime,
      DutyStatus status,
      String? location,
      String? notes,
      DateTime? actualStartTime,
      DateTime? actualEndTime});
}

/// @nodoc
class __$$StaffDutyImplCopyWithImpl<$Res>
    extends _$StaffDutyCopyWithImpl<$Res, _$StaffDutyImpl>
    implements _$$StaffDutyImplCopyWith<$Res> {
  __$$StaffDutyImplCopyWithImpl(
      _$StaffDutyImpl _value, $Res Function(_$StaffDutyImpl) _then)
      : super(_value, _then);

  /// Create a copy of StaffDuty
  /// with the given fields replaced by the non-null parameter values.
  @pragma('vm:prefer-inline')
  @override
  $Res call({
    Object? id = null,
    Object? staffId = null,
    Object? staffName = null,
    Object? role = null,
    Object? startTime = null,
    Object? endTime = null,
    Object? status = null,
    Object? location = freezed,
    Object? notes = freezed,
    Object? actualStartTime = freezed,
    Object? actualEndTime = freezed,
  }) {
    return _then(_$StaffDutyImpl(
      id: null == id
          ? _value.id
          : id // ignore: cast_nullable_to_non_nullable
              as int,
      staffId: null == staffId
          ? _value.staffId
          : staffId // ignore: cast_nullable_to_non_nullable
              as int,
      staffName: null == staffName
          ? _value.staffName
          : staffName // ignore: cast_nullable_to_non_nullable
              as String,
      role: null == role
          ? _value.role
          : role // ignore: cast_nullable_to_non_nullable
              as String,
      startTime: null == startTime
          ? _value.startTime
          : startTime // ignore: cast_nullable_to_non_nullable
              as DateTime,
      endTime: null == endTime
          ? _value.endTime
          : endTime // ignore: cast_nullable_to_non_nullable
              as DateTime,
      status: null == status
          ? _value.status
          : status // ignore: cast_nullable_to_non_nullable
              as DutyStatus,
      location: freezed == location
          ? _value.location
          : location // ignore: cast_nullable_to_non_nullable
              as String?,
      notes: freezed == notes
          ? _value.notes
          : notes // ignore: cast_nullable_to_non_nullable
              as String?,
      actualStartTime: freezed == actualStartTime
          ? _value.actualStartTime
          : actualStartTime // ignore: cast_nullable_to_non_nullable
              as DateTime?,
      actualEndTime: freezed == actualEndTime
          ? _value.actualEndTime
          : actualEndTime // ignore: cast_nullable_to_non_nullable
              as DateTime?,
    ));
  }
}

/// @nodoc
@JsonSerializable()
class _$StaffDutyImpl implements _StaffDuty {
  const _$StaffDutyImpl(
      {required this.id,
      required this.staffId,
      required this.staffName,
      required this.role,
      required this.startTime,
      required this.endTime,
      required this.status,
      this.location,
      this.notes,
      this.actualStartTime,
      this.actualEndTime});

  factory _$StaffDutyImpl.fromJson(Map<String, dynamic> json) =>
      _$$StaffDutyImplFromJson(json);

  @override
  final int id;
  @override
  final int staffId;
  @override
  final String staffName;
  @override
  final String role;
  @override
  final DateTime startTime;
  @override
  final DateTime endTime;
  @override
  final DutyStatus status;
  @override
  final String? location;
  @override
  final String? notes;
  @override
  final DateTime? actualStartTime;
  @override
  final DateTime? actualEndTime;

  @override
  String toString() {
    return 'StaffDuty(id: $id, staffId: $staffId, staffName: $staffName, role: $role, startTime: $startTime, endTime: $endTime, status: $status, location: $location, notes: $notes, actualStartTime: $actualStartTime, actualEndTime: $actualEndTime)';
  }

  @override
  bool operator ==(Object other) {
    return identical(this, other) ||
        (other.runtimeType == runtimeType &&
            other is _$StaffDutyImpl &&
            (identical(other.id, id) || other.id == id) &&
            (identical(other.staffId, staffId) || other.staffId == staffId) &&
            (identical(other.staffName, staffName) ||
                other.staffName == staffName) &&
            (identical(other.role, role) || other.role == role) &&
            (identical(other.startTime, startTime) ||
                other.startTime == startTime) &&
            (identical(other.endTime, endTime) || other.endTime == endTime) &&
            (identical(other.status, status) || other.status == status) &&
            (identical(other.location, location) ||
                other.location == location) &&
            (identical(other.notes, notes) || other.notes == notes) &&
            (identical(other.actualStartTime, actualStartTime) ||
                other.actualStartTime == actualStartTime) &&
            (identical(other.actualEndTime, actualEndTime) ||
                other.actualEndTime == actualEndTime));
  }

  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  int get hashCode => Object.hash(
      runtimeType,
      id,
      staffId,
      staffName,
      role,
      startTime,
      endTime,
      status,
      location,
      notes,
      actualStartTime,
      actualEndTime);

  /// Create a copy of StaffDuty
  /// with the given fields replaced by the non-null parameter values.
  @JsonKey(includeFromJson: false, includeToJson: false)
  @override
  @pragma('vm:prefer-inline')
  _$$StaffDutyImplCopyWith<_$StaffDutyImpl> get copyWith =>
      __$$StaffDutyImplCopyWithImpl<_$StaffDutyImpl>(this, _$identity);

  @override
  Map<String, dynamic> toJson() {
    return _$$StaffDutyImplToJson(
      this,
    );
  }
}

abstract class _StaffDuty implements StaffDuty {
  const factory _StaffDuty(
      {required final int id,
      required final int staffId,
      required final String staffName,
      required final String role,
      required final DateTime startTime,
      required final DateTime endTime,
      required final DutyStatus status,
      final String? location,
      final String? notes,
      final DateTime? actualStartTime,
      final DateTime? actualEndTime}) = _$StaffDutyImpl;

  factory _StaffDuty.fromJson(Map<String, dynamic> json) =
      _$StaffDutyImpl.fromJson;

  @override
  int get id;
  @override
  int get staffId;
  @override
  String get staffName;
  @override
  String get role;
  @override
  DateTime get startTime;
  @override
  DateTime get endTime;
  @override
  DutyStatus get status;
  @override
  String? get location;
  @override
  String? get notes;
  @override
  DateTime? get actualStartTime;
  @override
  DateTime? get actualEndTime;

  /// Create a copy of StaffDuty
  /// with the given fields replaced by the non-null parameter values.
  @override
  @JsonKey(includeFromJson: false, includeToJson: false)
  _$$StaffDutyImplCopyWith<_$StaffDutyImpl> get copyWith =>
      throw _privateConstructorUsedError;
}
