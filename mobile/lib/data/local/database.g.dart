// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'database.dart';

// ignore_for_file: type=lint
class $SoulsTableTable extends SoulsTable
    with TableInfo<$SoulsTableTable, SoulLocal> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $SoulsTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _tenantIdMeta =
      const VerificationMeta('tenantId');
  @override
  late final GeneratedColumn<String> tenantId = GeneratedColumn<String>(
      'tenant_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _nomMeta = const VerificationMeta('nom');
  @override
  late final GeneratedColumn<String> nom = GeneratedColumn<String>(
      'nom', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _prenomMeta = const VerificationMeta('prenom');
  @override
  late final GeneratedColumn<String> prenom = GeneratedColumn<String>(
      'prenom', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _emailMeta = const VerificationMeta('email');
  @override
  late final GeneratedColumn<String> email = GeneratedColumn<String>(
      'email', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _telephoneMeta =
      const VerificationMeta('telephone');
  @override
  late final GeneratedColumn<String> telephone = GeneratedColumn<String>(
      'telephone', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _typeDiscipleMeta =
      const VerificationMeta('typeDisciple');
  @override
  late final GeneratedColumn<String> typeDisciple = GeneratedColumn<String>(
      'type_disciple', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _statutMeta = const VerificationMeta('statut');
  @override
  late final GeneratedColumn<String> statut = GeneratedColumn<String>(
      'statut', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _dateIntegrationMeta =
      const VerificationMeta('dateIntegration');
  @override
  late final GeneratedColumn<String> dateIntegration = GeneratedColumn<String>(
      'date_integration', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _faiseurIdMeta =
      const VerificationMeta('faiseurId');
  @override
  late final GeneratedColumn<String> faiseurId = GeneratedColumn<String>(
      'faiseur_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _familleIdMeta =
      const VerificationMeta('familleId');
  @override
  late final GeneratedColumn<String> familleId = GeneratedColumn<String>(
      'famille_id', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _dateDernierContactMeta =
      const VerificationMeta('dateDernierContact');
  @override
  late final GeneratedColumn<String> dateDernierContact =
      GeneratedColumn<String>('date_dernier_contact', aliasedName, true,
          type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _lastSyncAtMeta =
      const VerificationMeta('lastSyncAt');
  @override
  late final GeneratedColumn<String> lastSyncAt = GeneratedColumn<String>(
      'last_sync_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  @override
  List<GeneratedColumn> get $columns => [
        id,
        tenantId,
        nom,
        prenom,
        email,
        telephone,
        typeDisciple,
        statut,
        dateIntegration,
        faiseurId,
        familleId,
        dateDernierContact,
        lastSyncAt
      ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'souls_table';
  @override
  VerificationContext validateIntegrity(Insertable<SoulLocal> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('tenant_id')) {
      context.handle(_tenantIdMeta,
          tenantId.isAcceptableOrUnknown(data['tenant_id']!, _tenantIdMeta));
    } else if (isInserting) {
      context.missing(_tenantIdMeta);
    }
    if (data.containsKey('nom')) {
      context.handle(
          _nomMeta, nom.isAcceptableOrUnknown(data['nom']!, _nomMeta));
    } else if (isInserting) {
      context.missing(_nomMeta);
    }
    if (data.containsKey('prenom')) {
      context.handle(_prenomMeta,
          prenom.isAcceptableOrUnknown(data['prenom']!, _prenomMeta));
    }
    if (data.containsKey('email')) {
      context.handle(
          _emailMeta, email.isAcceptableOrUnknown(data['email']!, _emailMeta));
    }
    if (data.containsKey('telephone')) {
      context.handle(_telephoneMeta,
          telephone.isAcceptableOrUnknown(data['telephone']!, _telephoneMeta));
    }
    if (data.containsKey('type_disciple')) {
      context.handle(
          _typeDiscipleMeta,
          typeDisciple.isAcceptableOrUnknown(
              data['type_disciple']!, _typeDiscipleMeta));
    } else if (isInserting) {
      context.missing(_typeDiscipleMeta);
    }
    if (data.containsKey('statut')) {
      context.handle(_statutMeta,
          statut.isAcceptableOrUnknown(data['statut']!, _statutMeta));
    } else if (isInserting) {
      context.missing(_statutMeta);
    }
    if (data.containsKey('date_integration')) {
      context.handle(
          _dateIntegrationMeta,
          dateIntegration.isAcceptableOrUnknown(
              data['date_integration']!, _dateIntegrationMeta));
    } else if (isInserting) {
      context.missing(_dateIntegrationMeta);
    }
    if (data.containsKey('faiseur_id')) {
      context.handle(_faiseurIdMeta,
          faiseurId.isAcceptableOrUnknown(data['faiseur_id']!, _faiseurIdMeta));
    } else if (isInserting) {
      context.missing(_faiseurIdMeta);
    }
    if (data.containsKey('famille_id')) {
      context.handle(_familleIdMeta,
          familleId.isAcceptableOrUnknown(data['famille_id']!, _familleIdMeta));
    }
    if (data.containsKey('date_dernier_contact')) {
      context.handle(
          _dateDernierContactMeta,
          dateDernierContact.isAcceptableOrUnknown(
              data['date_dernier_contact']!, _dateDernierContactMeta));
    }
    if (data.containsKey('last_sync_at')) {
      context.handle(
          _lastSyncAtMeta,
          lastSyncAt.isAcceptableOrUnknown(
              data['last_sync_at']!, _lastSyncAtMeta));
    } else if (isInserting) {
      context.missing(_lastSyncAtMeta);
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id, tenantId};
  @override
  SoulLocal map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return SoulLocal(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      tenantId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}tenant_id'])!,
      nom: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}nom'])!,
      prenom: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}prenom']),
      email: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}email']),
      telephone: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}telephone']),
      typeDisciple: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}type_disciple'])!,
      statut: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}statut'])!,
      dateIntegration: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}date_integration'])!,
      faiseurId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}faiseur_id'])!,
      familleId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}famille_id']),
      dateDernierContact: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}date_dernier_contact']),
      lastSyncAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}last_sync_at'])!,
    );
  }

  @override
  $SoulsTableTable createAlias(String alias) {
    return $SoulsTableTable(attachedDatabase, alias);
  }
}

class SoulLocal extends DataClass implements Insertable<SoulLocal> {
  final String id;
  final String tenantId;
  final String nom;
  final String? prenom;
  final String? email;
  final String? telephone;
  final String typeDisciple;
  final String statut;
  final String dateIntegration;
  final String faiseurId;
  final String? familleId;
  final String? dateDernierContact;
  final String lastSyncAt;
  const SoulLocal(
      {required this.id,
      required this.tenantId,
      required this.nom,
      this.prenom,
      this.email,
      this.telephone,
      required this.typeDisciple,
      required this.statut,
      required this.dateIntegration,
      required this.faiseurId,
      this.familleId,
      this.dateDernierContact,
      required this.lastSyncAt});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['tenant_id'] = Variable<String>(tenantId);
    map['nom'] = Variable<String>(nom);
    if (!nullToAbsent || prenom != null) {
      map['prenom'] = Variable<String>(prenom);
    }
    if (!nullToAbsent || email != null) {
      map['email'] = Variable<String>(email);
    }
    if (!nullToAbsent || telephone != null) {
      map['telephone'] = Variable<String>(telephone);
    }
    map['type_disciple'] = Variable<String>(typeDisciple);
    map['statut'] = Variable<String>(statut);
    map['date_integration'] = Variable<String>(dateIntegration);
    map['faiseur_id'] = Variable<String>(faiseurId);
    if (!nullToAbsent || familleId != null) {
      map['famille_id'] = Variable<String>(familleId);
    }
    if (!nullToAbsent || dateDernierContact != null) {
      map['date_dernier_contact'] = Variable<String>(dateDernierContact);
    }
    map['last_sync_at'] = Variable<String>(lastSyncAt);
    return map;
  }

  SoulsTableCompanion toCompanion(bool nullToAbsent) {
    return SoulsTableCompanion(
      id: Value(id),
      tenantId: Value(tenantId),
      nom: Value(nom),
      prenom:
          prenom == null && nullToAbsent ? const Value.absent() : Value(prenom),
      email:
          email == null && nullToAbsent ? const Value.absent() : Value(email),
      telephone: telephone == null && nullToAbsent
          ? const Value.absent()
          : Value(telephone),
      typeDisciple: Value(typeDisciple),
      statut: Value(statut),
      dateIntegration: Value(dateIntegration),
      faiseurId: Value(faiseurId),
      familleId: familleId == null && nullToAbsent
          ? const Value.absent()
          : Value(familleId),
      dateDernierContact: dateDernierContact == null && nullToAbsent
          ? const Value.absent()
          : Value(dateDernierContact),
      lastSyncAt: Value(lastSyncAt),
    );
  }

  factory SoulLocal.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return SoulLocal(
      id: serializer.fromJson<String>(json['id']),
      tenantId: serializer.fromJson<String>(json['tenantId']),
      nom: serializer.fromJson<String>(json['nom']),
      prenom: serializer.fromJson<String?>(json['prenom']),
      email: serializer.fromJson<String?>(json['email']),
      telephone: serializer.fromJson<String?>(json['telephone']),
      typeDisciple: serializer.fromJson<String>(json['typeDisciple']),
      statut: serializer.fromJson<String>(json['statut']),
      dateIntegration: serializer.fromJson<String>(json['dateIntegration']),
      faiseurId: serializer.fromJson<String>(json['faiseurId']),
      familleId: serializer.fromJson<String?>(json['familleId']),
      dateDernierContact:
          serializer.fromJson<String?>(json['dateDernierContact']),
      lastSyncAt: serializer.fromJson<String>(json['lastSyncAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'tenantId': serializer.toJson<String>(tenantId),
      'nom': serializer.toJson<String>(nom),
      'prenom': serializer.toJson<String?>(prenom),
      'email': serializer.toJson<String?>(email),
      'telephone': serializer.toJson<String?>(telephone),
      'typeDisciple': serializer.toJson<String>(typeDisciple),
      'statut': serializer.toJson<String>(statut),
      'dateIntegration': serializer.toJson<String>(dateIntegration),
      'faiseurId': serializer.toJson<String>(faiseurId),
      'familleId': serializer.toJson<String?>(familleId),
      'dateDernierContact': serializer.toJson<String?>(dateDernierContact),
      'lastSyncAt': serializer.toJson<String>(lastSyncAt),
    };
  }

  SoulLocal copyWith(
          {String? id,
          String? tenantId,
          String? nom,
          Value<String?> prenom = const Value.absent(),
          Value<String?> email = const Value.absent(),
          Value<String?> telephone = const Value.absent(),
          String? typeDisciple,
          String? statut,
          String? dateIntegration,
          String? faiseurId,
          Value<String?> familleId = const Value.absent(),
          Value<String?> dateDernierContact = const Value.absent(),
          String? lastSyncAt}) =>
      SoulLocal(
        id: id ?? this.id,
        tenantId: tenantId ?? this.tenantId,
        nom: nom ?? this.nom,
        prenom: prenom.present ? prenom.value : this.prenom,
        email: email.present ? email.value : this.email,
        telephone: telephone.present ? telephone.value : this.telephone,
        typeDisciple: typeDisciple ?? this.typeDisciple,
        statut: statut ?? this.statut,
        dateIntegration: dateIntegration ?? this.dateIntegration,
        faiseurId: faiseurId ?? this.faiseurId,
        familleId: familleId.present ? familleId.value : this.familleId,
        dateDernierContact: dateDernierContact.present
            ? dateDernierContact.value
            : this.dateDernierContact,
        lastSyncAt: lastSyncAt ?? this.lastSyncAt,
      );
  SoulLocal copyWithCompanion(SoulsTableCompanion data) {
    return SoulLocal(
      id: data.id.present ? data.id.value : this.id,
      tenantId: data.tenantId.present ? data.tenantId.value : this.tenantId,
      nom: data.nom.present ? data.nom.value : this.nom,
      prenom: data.prenom.present ? data.prenom.value : this.prenom,
      email: data.email.present ? data.email.value : this.email,
      telephone: data.telephone.present ? data.telephone.value : this.telephone,
      typeDisciple: data.typeDisciple.present
          ? data.typeDisciple.value
          : this.typeDisciple,
      statut: data.statut.present ? data.statut.value : this.statut,
      dateIntegration: data.dateIntegration.present
          ? data.dateIntegration.value
          : this.dateIntegration,
      faiseurId: data.faiseurId.present ? data.faiseurId.value : this.faiseurId,
      familleId: data.familleId.present ? data.familleId.value : this.familleId,
      dateDernierContact: data.dateDernierContact.present
          ? data.dateDernierContact.value
          : this.dateDernierContact,
      lastSyncAt:
          data.lastSyncAt.present ? data.lastSyncAt.value : this.lastSyncAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('SoulLocal(')
          ..write('id: $id, ')
          ..write('tenantId: $tenantId, ')
          ..write('nom: $nom, ')
          ..write('prenom: $prenom, ')
          ..write('email: $email, ')
          ..write('telephone: $telephone, ')
          ..write('typeDisciple: $typeDisciple, ')
          ..write('statut: $statut, ')
          ..write('dateIntegration: $dateIntegration, ')
          ..write('faiseurId: $faiseurId, ')
          ..write('familleId: $familleId, ')
          ..write('dateDernierContact: $dateDernierContact, ')
          ..write('lastSyncAt: $lastSyncAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id,
      tenantId,
      nom,
      prenom,
      email,
      telephone,
      typeDisciple,
      statut,
      dateIntegration,
      faiseurId,
      familleId,
      dateDernierContact,
      lastSyncAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is SoulLocal &&
          other.id == this.id &&
          other.tenantId == this.tenantId &&
          other.nom == this.nom &&
          other.prenom == this.prenom &&
          other.email == this.email &&
          other.telephone == this.telephone &&
          other.typeDisciple == this.typeDisciple &&
          other.statut == this.statut &&
          other.dateIntegration == this.dateIntegration &&
          other.faiseurId == this.faiseurId &&
          other.familleId == this.familleId &&
          other.dateDernierContact == this.dateDernierContact &&
          other.lastSyncAt == this.lastSyncAt);
}

class SoulsTableCompanion extends UpdateCompanion<SoulLocal> {
  final Value<String> id;
  final Value<String> tenantId;
  final Value<String> nom;
  final Value<String?> prenom;
  final Value<String?> email;
  final Value<String?> telephone;
  final Value<String> typeDisciple;
  final Value<String> statut;
  final Value<String> dateIntegration;
  final Value<String> faiseurId;
  final Value<String?> familleId;
  final Value<String?> dateDernierContact;
  final Value<String> lastSyncAt;
  final Value<int> rowid;
  const SoulsTableCompanion({
    this.id = const Value.absent(),
    this.tenantId = const Value.absent(),
    this.nom = const Value.absent(),
    this.prenom = const Value.absent(),
    this.email = const Value.absent(),
    this.telephone = const Value.absent(),
    this.typeDisciple = const Value.absent(),
    this.statut = const Value.absent(),
    this.dateIntegration = const Value.absent(),
    this.faiseurId = const Value.absent(),
    this.familleId = const Value.absent(),
    this.dateDernierContact = const Value.absent(),
    this.lastSyncAt = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  SoulsTableCompanion.insert({
    required String id,
    required String tenantId,
    required String nom,
    this.prenom = const Value.absent(),
    this.email = const Value.absent(),
    this.telephone = const Value.absent(),
    required String typeDisciple,
    required String statut,
    required String dateIntegration,
    required String faiseurId,
    this.familleId = const Value.absent(),
    this.dateDernierContact = const Value.absent(),
    required String lastSyncAt,
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        tenantId = Value(tenantId),
        nom = Value(nom),
        typeDisciple = Value(typeDisciple),
        statut = Value(statut),
        dateIntegration = Value(dateIntegration),
        faiseurId = Value(faiseurId),
        lastSyncAt = Value(lastSyncAt);
  static Insertable<SoulLocal> custom({
    Expression<String>? id,
    Expression<String>? tenantId,
    Expression<String>? nom,
    Expression<String>? prenom,
    Expression<String>? email,
    Expression<String>? telephone,
    Expression<String>? typeDisciple,
    Expression<String>? statut,
    Expression<String>? dateIntegration,
    Expression<String>? faiseurId,
    Expression<String>? familleId,
    Expression<String>? dateDernierContact,
    Expression<String>? lastSyncAt,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (tenantId != null) 'tenant_id': tenantId,
      if (nom != null) 'nom': nom,
      if (prenom != null) 'prenom': prenom,
      if (email != null) 'email': email,
      if (telephone != null) 'telephone': telephone,
      if (typeDisciple != null) 'type_disciple': typeDisciple,
      if (statut != null) 'statut': statut,
      if (dateIntegration != null) 'date_integration': dateIntegration,
      if (faiseurId != null) 'faiseur_id': faiseurId,
      if (familleId != null) 'famille_id': familleId,
      if (dateDernierContact != null)
        'date_dernier_contact': dateDernierContact,
      if (lastSyncAt != null) 'last_sync_at': lastSyncAt,
      if (rowid != null) 'rowid': rowid,
    });
  }

  SoulsTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? tenantId,
      Value<String>? nom,
      Value<String?>? prenom,
      Value<String?>? email,
      Value<String?>? telephone,
      Value<String>? typeDisciple,
      Value<String>? statut,
      Value<String>? dateIntegration,
      Value<String>? faiseurId,
      Value<String?>? familleId,
      Value<String?>? dateDernierContact,
      Value<String>? lastSyncAt,
      Value<int>? rowid}) {
    return SoulsTableCompanion(
      id: id ?? this.id,
      tenantId: tenantId ?? this.tenantId,
      nom: nom ?? this.nom,
      prenom: prenom ?? this.prenom,
      email: email ?? this.email,
      telephone: telephone ?? this.telephone,
      typeDisciple: typeDisciple ?? this.typeDisciple,
      statut: statut ?? this.statut,
      dateIntegration: dateIntegration ?? this.dateIntegration,
      faiseurId: faiseurId ?? this.faiseurId,
      familleId: familleId ?? this.familleId,
      dateDernierContact: dateDernierContact ?? this.dateDernierContact,
      lastSyncAt: lastSyncAt ?? this.lastSyncAt,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (tenantId.present) {
      map['tenant_id'] = Variable<String>(tenantId.value);
    }
    if (nom.present) {
      map['nom'] = Variable<String>(nom.value);
    }
    if (prenom.present) {
      map['prenom'] = Variable<String>(prenom.value);
    }
    if (email.present) {
      map['email'] = Variable<String>(email.value);
    }
    if (telephone.present) {
      map['telephone'] = Variable<String>(telephone.value);
    }
    if (typeDisciple.present) {
      map['type_disciple'] = Variable<String>(typeDisciple.value);
    }
    if (statut.present) {
      map['statut'] = Variable<String>(statut.value);
    }
    if (dateIntegration.present) {
      map['date_integration'] = Variable<String>(dateIntegration.value);
    }
    if (faiseurId.present) {
      map['faiseur_id'] = Variable<String>(faiseurId.value);
    }
    if (familleId.present) {
      map['famille_id'] = Variable<String>(familleId.value);
    }
    if (dateDernierContact.present) {
      map['date_dernier_contact'] = Variable<String>(dateDernierContact.value);
    }
    if (lastSyncAt.present) {
      map['last_sync_at'] = Variable<String>(lastSyncAt.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('SoulsTableCompanion(')
          ..write('id: $id, ')
          ..write('tenantId: $tenantId, ')
          ..write('nom: $nom, ')
          ..write('prenom: $prenom, ')
          ..write('email: $email, ')
          ..write('telephone: $telephone, ')
          ..write('typeDisciple: $typeDisciple, ')
          ..write('statut: $statut, ')
          ..write('dateIntegration: $dateIntegration, ')
          ..write('faiseurId: $faiseurId, ')
          ..write('familleId: $familleId, ')
          ..write('dateDernierContact: $dateDernierContact, ')
          ..write('lastSyncAt: $lastSyncAt, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $ReportDraftsTableTable extends ReportDraftsTable
    with TableInfo<$ReportDraftsTableTable, ReportDraft> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $ReportDraftsTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _tenantIdMeta =
      const VerificationMeta('tenantId');
  @override
  late final GeneratedColumn<String> tenantId = GeneratedColumn<String>(
      'tenant_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _ameIdMeta = const VerificationMeta('ameId');
  @override
  late final GeneratedColumn<String> ameId = GeneratedColumn<String>(
      'ame_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _semaineMeta =
      const VerificationMeta('semaine');
  @override
  late final GeneratedColumn<String> semaine = GeneratedColumn<String>(
      'semaine', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _presencesParCulteMeta =
      const VerificationMeta('presencesParCulte');
  @override
  late final GeneratedColumn<String> presencesParCulte =
      GeneratedColumn<String>('presences_par_culte', aliasedName, false,
          type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _absenceRaisonMeta =
      const VerificationMeta('absenceRaison');
  @override
  late final GeneratedColumn<String> absenceRaison = GeneratedColumn<String>(
      'absence_raison', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _absenceCommentaireMeta =
      const VerificationMeta('absenceCommentaire');
  @override
  late final GeneratedColumn<String> absenceCommentaire =
      GeneratedColumn<String>('absence_commentaire', aliasedName, true,
          type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _difficultesMeta =
      const VerificationMeta('difficultes');
  @override
  late final GeneratedColumn<String> difficultes = GeneratedColumn<String>(
      'difficultes', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _notesComplementairesMeta =
      const VerificationMeta('notesComplementaires');
  @override
  late final GeneratedColumn<String> notesComplementaires =
      GeneratedColumn<String>('notes_complementaires', aliasedName, true,
          type: DriftSqlType.string, requiredDuringInsert: false);
  static const VerificationMeta _nbSortiesMeta =
      const VerificationMeta('nbSorties');
  @override
  late final GeneratedColumn<int> nbSorties = GeneratedColumn<int>(
      'nb_sorties', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _nbMaintenusMeta =
      const VerificationMeta('nbMaintenus');
  @override
  late final GeneratedColumn<int> nbMaintenus = GeneratedColumn<int>(
      'nb_maintenus', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _updatedAtMeta =
      const VerificationMeta('updatedAt');
  @override
  late final GeneratedColumn<String> updatedAt = GeneratedColumn<String>(
      'updated_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _syncedMeta = const VerificationMeta('synced');
  @override
  late final GeneratedColumn<bool> synced = GeneratedColumn<bool>(
      'synced', aliasedName, false,
      type: DriftSqlType.bool,
      requiredDuringInsert: false,
      defaultConstraints:
          GeneratedColumn.constraintIsAlways('CHECK ("synced" IN (0, 1))'),
      defaultValue: const Constant(false));
  @override
  List<GeneratedColumn> get $columns => [
        id,
        tenantId,
        ameId,
        semaine,
        presencesParCulte,
        absenceRaison,
        absenceCommentaire,
        difficultes,
        notesComplementaires,
        nbSorties,
        nbMaintenus,
        updatedAt,
        synced
      ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'report_drafts_table';
  @override
  VerificationContext validateIntegrity(Insertable<ReportDraft> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('tenant_id')) {
      context.handle(_tenantIdMeta,
          tenantId.isAcceptableOrUnknown(data['tenant_id']!, _tenantIdMeta));
    } else if (isInserting) {
      context.missing(_tenantIdMeta);
    }
    if (data.containsKey('ame_id')) {
      context.handle(
          _ameIdMeta, ameId.isAcceptableOrUnknown(data['ame_id']!, _ameIdMeta));
    } else if (isInserting) {
      context.missing(_ameIdMeta);
    }
    if (data.containsKey('semaine')) {
      context.handle(_semaineMeta,
          semaine.isAcceptableOrUnknown(data['semaine']!, _semaineMeta));
    } else if (isInserting) {
      context.missing(_semaineMeta);
    }
    if (data.containsKey('presences_par_culte')) {
      context.handle(
          _presencesParCulteMeta,
          presencesParCulte.isAcceptableOrUnknown(
              data['presences_par_culte']!, _presencesParCulteMeta));
    } else if (isInserting) {
      context.missing(_presencesParCulteMeta);
    }
    if (data.containsKey('absence_raison')) {
      context.handle(
          _absenceRaisonMeta,
          absenceRaison.isAcceptableOrUnknown(
              data['absence_raison']!, _absenceRaisonMeta));
    }
    if (data.containsKey('absence_commentaire')) {
      context.handle(
          _absenceCommentaireMeta,
          absenceCommentaire.isAcceptableOrUnknown(
              data['absence_commentaire']!, _absenceCommentaireMeta));
    }
    if (data.containsKey('difficultes')) {
      context.handle(
          _difficultesMeta,
          difficultes.isAcceptableOrUnknown(
              data['difficultes']!, _difficultesMeta));
    }
    if (data.containsKey('notes_complementaires')) {
      context.handle(
          _notesComplementairesMeta,
          notesComplementaires.isAcceptableOrUnknown(
              data['notes_complementaires']!, _notesComplementairesMeta));
    }
    if (data.containsKey('nb_sorties')) {
      context.handle(_nbSortiesMeta,
          nbSorties.isAcceptableOrUnknown(data['nb_sorties']!, _nbSortiesMeta));
    }
    if (data.containsKey('nb_maintenus')) {
      context.handle(
          _nbMaintenusMeta,
          nbMaintenus.isAcceptableOrUnknown(
              data['nb_maintenus']!, _nbMaintenusMeta));
    }
    if (data.containsKey('updated_at')) {
      context.handle(_updatedAtMeta,
          updatedAt.isAcceptableOrUnknown(data['updated_at']!, _updatedAtMeta));
    } else if (isInserting) {
      context.missing(_updatedAtMeta);
    }
    if (data.containsKey('synced')) {
      context.handle(_syncedMeta,
          synced.isAcceptableOrUnknown(data['synced']!, _syncedMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  ReportDraft map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return ReportDraft(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      tenantId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}tenant_id'])!,
      ameId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}ame_id'])!,
      semaine: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}semaine'])!,
      presencesParCulte: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}presences_par_culte'])!,
      absenceRaison: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}absence_raison']),
      absenceCommentaire: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}absence_commentaire']),
      difficultes: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}difficultes']),
      notesComplementaires: attachedDatabase.typeMapping.read(
          DriftSqlType.string, data['${effectivePrefix}notes_complementaires']),
      nbSorties: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}nb_sorties'])!,
      nbMaintenus: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}nb_maintenus'])!,
      updatedAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}updated_at'])!,
      synced: attachedDatabase.typeMapping
          .read(DriftSqlType.bool, data['${effectivePrefix}synced'])!,
    );
  }

  @override
  $ReportDraftsTableTable createAlias(String alias) {
    return $ReportDraftsTableTable(attachedDatabase, alias);
  }
}

class ReportDraft extends DataClass implements Insertable<ReportDraft> {
  final String id;
  final String tenantId;
  final String ameId;
  final String semaine;
  final String presencesParCulte;
  final String? absenceRaison;
  final String? absenceCommentaire;
  final String? difficultes;
  final String? notesComplementaires;
  final int nbSorties;
  final int nbMaintenus;
  final String updatedAt;
  final bool synced;
  const ReportDraft(
      {required this.id,
      required this.tenantId,
      required this.ameId,
      required this.semaine,
      required this.presencesParCulte,
      this.absenceRaison,
      this.absenceCommentaire,
      this.difficultes,
      this.notesComplementaires,
      required this.nbSorties,
      required this.nbMaintenus,
      required this.updatedAt,
      required this.synced});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['tenant_id'] = Variable<String>(tenantId);
    map['ame_id'] = Variable<String>(ameId);
    map['semaine'] = Variable<String>(semaine);
    map['presences_par_culte'] = Variable<String>(presencesParCulte);
    if (!nullToAbsent || absenceRaison != null) {
      map['absence_raison'] = Variable<String>(absenceRaison);
    }
    if (!nullToAbsent || absenceCommentaire != null) {
      map['absence_commentaire'] = Variable<String>(absenceCommentaire);
    }
    if (!nullToAbsent || difficultes != null) {
      map['difficultes'] = Variable<String>(difficultes);
    }
    if (!nullToAbsent || notesComplementaires != null) {
      map['notes_complementaires'] = Variable<String>(notesComplementaires);
    }
    map['nb_sorties'] = Variable<int>(nbSorties);
    map['nb_maintenus'] = Variable<int>(nbMaintenus);
    map['updated_at'] = Variable<String>(updatedAt);
    map['synced'] = Variable<bool>(synced);
    return map;
  }

  ReportDraftsTableCompanion toCompanion(bool nullToAbsent) {
    return ReportDraftsTableCompanion(
      id: Value(id),
      tenantId: Value(tenantId),
      ameId: Value(ameId),
      semaine: Value(semaine),
      presencesParCulte: Value(presencesParCulte),
      absenceRaison: absenceRaison == null && nullToAbsent
          ? const Value.absent()
          : Value(absenceRaison),
      absenceCommentaire: absenceCommentaire == null && nullToAbsent
          ? const Value.absent()
          : Value(absenceCommentaire),
      difficultes: difficultes == null && nullToAbsent
          ? const Value.absent()
          : Value(difficultes),
      notesComplementaires: notesComplementaires == null && nullToAbsent
          ? const Value.absent()
          : Value(notesComplementaires),
      nbSorties: Value(nbSorties),
      nbMaintenus: Value(nbMaintenus),
      updatedAt: Value(updatedAt),
      synced: Value(synced),
    );
  }

  factory ReportDraft.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return ReportDraft(
      id: serializer.fromJson<String>(json['id']),
      tenantId: serializer.fromJson<String>(json['tenantId']),
      ameId: serializer.fromJson<String>(json['ameId']),
      semaine: serializer.fromJson<String>(json['semaine']),
      presencesParCulte: serializer.fromJson<String>(json['presencesParCulte']),
      absenceRaison: serializer.fromJson<String?>(json['absenceRaison']),
      absenceCommentaire:
          serializer.fromJson<String?>(json['absenceCommentaire']),
      difficultes: serializer.fromJson<String?>(json['difficultes']),
      notesComplementaires:
          serializer.fromJson<String?>(json['notesComplementaires']),
      nbSorties: serializer.fromJson<int>(json['nbSorties']),
      nbMaintenus: serializer.fromJson<int>(json['nbMaintenus']),
      updatedAt: serializer.fromJson<String>(json['updatedAt']),
      synced: serializer.fromJson<bool>(json['synced']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'tenantId': serializer.toJson<String>(tenantId),
      'ameId': serializer.toJson<String>(ameId),
      'semaine': serializer.toJson<String>(semaine),
      'presencesParCulte': serializer.toJson<String>(presencesParCulte),
      'absenceRaison': serializer.toJson<String?>(absenceRaison),
      'absenceCommentaire': serializer.toJson<String?>(absenceCommentaire),
      'difficultes': serializer.toJson<String?>(difficultes),
      'notesComplementaires': serializer.toJson<String?>(notesComplementaires),
      'nbSorties': serializer.toJson<int>(nbSorties),
      'nbMaintenus': serializer.toJson<int>(nbMaintenus),
      'updatedAt': serializer.toJson<String>(updatedAt),
      'synced': serializer.toJson<bool>(synced),
    };
  }

  ReportDraft copyWith(
          {String? id,
          String? tenantId,
          String? ameId,
          String? semaine,
          String? presencesParCulte,
          Value<String?> absenceRaison = const Value.absent(),
          Value<String?> absenceCommentaire = const Value.absent(),
          Value<String?> difficultes = const Value.absent(),
          Value<String?> notesComplementaires = const Value.absent(),
          int? nbSorties,
          int? nbMaintenus,
          String? updatedAt,
          bool? synced}) =>
      ReportDraft(
        id: id ?? this.id,
        tenantId: tenantId ?? this.tenantId,
        ameId: ameId ?? this.ameId,
        semaine: semaine ?? this.semaine,
        presencesParCulte: presencesParCulte ?? this.presencesParCulte,
        absenceRaison:
            absenceRaison.present ? absenceRaison.value : this.absenceRaison,
        absenceCommentaire: absenceCommentaire.present
            ? absenceCommentaire.value
            : this.absenceCommentaire,
        difficultes: difficultes.present ? difficultes.value : this.difficultes,
        notesComplementaires: notesComplementaires.present
            ? notesComplementaires.value
            : this.notesComplementaires,
        nbSorties: nbSorties ?? this.nbSorties,
        nbMaintenus: nbMaintenus ?? this.nbMaintenus,
        updatedAt: updatedAt ?? this.updatedAt,
        synced: synced ?? this.synced,
      );
  ReportDraft copyWithCompanion(ReportDraftsTableCompanion data) {
    return ReportDraft(
      id: data.id.present ? data.id.value : this.id,
      tenantId: data.tenantId.present ? data.tenantId.value : this.tenantId,
      ameId: data.ameId.present ? data.ameId.value : this.ameId,
      semaine: data.semaine.present ? data.semaine.value : this.semaine,
      presencesParCulte: data.presencesParCulte.present
          ? data.presencesParCulte.value
          : this.presencesParCulte,
      absenceRaison: data.absenceRaison.present
          ? data.absenceRaison.value
          : this.absenceRaison,
      absenceCommentaire: data.absenceCommentaire.present
          ? data.absenceCommentaire.value
          : this.absenceCommentaire,
      difficultes:
          data.difficultes.present ? data.difficultes.value : this.difficultes,
      notesComplementaires: data.notesComplementaires.present
          ? data.notesComplementaires.value
          : this.notesComplementaires,
      nbSorties: data.nbSorties.present ? data.nbSorties.value : this.nbSorties,
      nbMaintenus:
          data.nbMaintenus.present ? data.nbMaintenus.value : this.nbMaintenus,
      updatedAt: data.updatedAt.present ? data.updatedAt.value : this.updatedAt,
      synced: data.synced.present ? data.synced.value : this.synced,
    );
  }

  @override
  String toString() {
    return (StringBuffer('ReportDraft(')
          ..write('id: $id, ')
          ..write('tenantId: $tenantId, ')
          ..write('ameId: $ameId, ')
          ..write('semaine: $semaine, ')
          ..write('presencesParCulte: $presencesParCulte, ')
          ..write('absenceRaison: $absenceRaison, ')
          ..write('absenceCommentaire: $absenceCommentaire, ')
          ..write('difficultes: $difficultes, ')
          ..write('notesComplementaires: $notesComplementaires, ')
          ..write('nbSorties: $nbSorties, ')
          ..write('nbMaintenus: $nbMaintenus, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('synced: $synced')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(
      id,
      tenantId,
      ameId,
      semaine,
      presencesParCulte,
      absenceRaison,
      absenceCommentaire,
      difficultes,
      notesComplementaires,
      nbSorties,
      nbMaintenus,
      updatedAt,
      synced);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is ReportDraft &&
          other.id == this.id &&
          other.tenantId == this.tenantId &&
          other.ameId == this.ameId &&
          other.semaine == this.semaine &&
          other.presencesParCulte == this.presencesParCulte &&
          other.absenceRaison == this.absenceRaison &&
          other.absenceCommentaire == this.absenceCommentaire &&
          other.difficultes == this.difficultes &&
          other.notesComplementaires == this.notesComplementaires &&
          other.nbSorties == this.nbSorties &&
          other.nbMaintenus == this.nbMaintenus &&
          other.updatedAt == this.updatedAt &&
          other.synced == this.synced);
}

class ReportDraftsTableCompanion extends UpdateCompanion<ReportDraft> {
  final Value<String> id;
  final Value<String> tenantId;
  final Value<String> ameId;
  final Value<String> semaine;
  final Value<String> presencesParCulte;
  final Value<String?> absenceRaison;
  final Value<String?> absenceCommentaire;
  final Value<String?> difficultes;
  final Value<String?> notesComplementaires;
  final Value<int> nbSorties;
  final Value<int> nbMaintenus;
  final Value<String> updatedAt;
  final Value<bool> synced;
  final Value<int> rowid;
  const ReportDraftsTableCompanion({
    this.id = const Value.absent(),
    this.tenantId = const Value.absent(),
    this.ameId = const Value.absent(),
    this.semaine = const Value.absent(),
    this.presencesParCulte = const Value.absent(),
    this.absenceRaison = const Value.absent(),
    this.absenceCommentaire = const Value.absent(),
    this.difficultes = const Value.absent(),
    this.notesComplementaires = const Value.absent(),
    this.nbSorties = const Value.absent(),
    this.nbMaintenus = const Value.absent(),
    this.updatedAt = const Value.absent(),
    this.synced = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  ReportDraftsTableCompanion.insert({
    required String id,
    required String tenantId,
    required String ameId,
    required String semaine,
    required String presencesParCulte,
    this.absenceRaison = const Value.absent(),
    this.absenceCommentaire = const Value.absent(),
    this.difficultes = const Value.absent(),
    this.notesComplementaires = const Value.absent(),
    this.nbSorties = const Value.absent(),
    this.nbMaintenus = const Value.absent(),
    required String updatedAt,
    this.synced = const Value.absent(),
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        tenantId = Value(tenantId),
        ameId = Value(ameId),
        semaine = Value(semaine),
        presencesParCulte = Value(presencesParCulte),
        updatedAt = Value(updatedAt);
  static Insertable<ReportDraft> custom({
    Expression<String>? id,
    Expression<String>? tenantId,
    Expression<String>? ameId,
    Expression<String>? semaine,
    Expression<String>? presencesParCulte,
    Expression<String>? absenceRaison,
    Expression<String>? absenceCommentaire,
    Expression<String>? difficultes,
    Expression<String>? notesComplementaires,
    Expression<int>? nbSorties,
    Expression<int>? nbMaintenus,
    Expression<String>? updatedAt,
    Expression<bool>? synced,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (tenantId != null) 'tenant_id': tenantId,
      if (ameId != null) 'ame_id': ameId,
      if (semaine != null) 'semaine': semaine,
      if (presencesParCulte != null) 'presences_par_culte': presencesParCulte,
      if (absenceRaison != null) 'absence_raison': absenceRaison,
      if (absenceCommentaire != null) 'absence_commentaire': absenceCommentaire,
      if (difficultes != null) 'difficultes': difficultes,
      if (notesComplementaires != null)
        'notes_complementaires': notesComplementaires,
      if (nbSorties != null) 'nb_sorties': nbSorties,
      if (nbMaintenus != null) 'nb_maintenus': nbMaintenus,
      if (updatedAt != null) 'updated_at': updatedAt,
      if (synced != null) 'synced': synced,
      if (rowid != null) 'rowid': rowid,
    });
  }

  ReportDraftsTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? tenantId,
      Value<String>? ameId,
      Value<String>? semaine,
      Value<String>? presencesParCulte,
      Value<String?>? absenceRaison,
      Value<String?>? absenceCommentaire,
      Value<String?>? difficultes,
      Value<String?>? notesComplementaires,
      Value<int>? nbSorties,
      Value<int>? nbMaintenus,
      Value<String>? updatedAt,
      Value<bool>? synced,
      Value<int>? rowid}) {
    return ReportDraftsTableCompanion(
      id: id ?? this.id,
      tenantId: tenantId ?? this.tenantId,
      ameId: ameId ?? this.ameId,
      semaine: semaine ?? this.semaine,
      presencesParCulte: presencesParCulte ?? this.presencesParCulte,
      absenceRaison: absenceRaison ?? this.absenceRaison,
      absenceCommentaire: absenceCommentaire ?? this.absenceCommentaire,
      difficultes: difficultes ?? this.difficultes,
      notesComplementaires: notesComplementaires ?? this.notesComplementaires,
      nbSorties: nbSorties ?? this.nbSorties,
      nbMaintenus: nbMaintenus ?? this.nbMaintenus,
      updatedAt: updatedAt ?? this.updatedAt,
      synced: synced ?? this.synced,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (tenantId.present) {
      map['tenant_id'] = Variable<String>(tenantId.value);
    }
    if (ameId.present) {
      map['ame_id'] = Variable<String>(ameId.value);
    }
    if (semaine.present) {
      map['semaine'] = Variable<String>(semaine.value);
    }
    if (presencesParCulte.present) {
      map['presences_par_culte'] = Variable<String>(presencesParCulte.value);
    }
    if (absenceRaison.present) {
      map['absence_raison'] = Variable<String>(absenceRaison.value);
    }
    if (absenceCommentaire.present) {
      map['absence_commentaire'] = Variable<String>(absenceCommentaire.value);
    }
    if (difficultes.present) {
      map['difficultes'] = Variable<String>(difficultes.value);
    }
    if (notesComplementaires.present) {
      map['notes_complementaires'] =
          Variable<String>(notesComplementaires.value);
    }
    if (nbSorties.present) {
      map['nb_sorties'] = Variable<int>(nbSorties.value);
    }
    if (nbMaintenus.present) {
      map['nb_maintenus'] = Variable<int>(nbMaintenus.value);
    }
    if (updatedAt.present) {
      map['updated_at'] = Variable<String>(updatedAt.value);
    }
    if (synced.present) {
      map['synced'] = Variable<bool>(synced.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('ReportDraftsTableCompanion(')
          ..write('id: $id, ')
          ..write('tenantId: $tenantId, ')
          ..write('ameId: $ameId, ')
          ..write('semaine: $semaine, ')
          ..write('presencesParCulte: $presencesParCulte, ')
          ..write('absenceRaison: $absenceRaison, ')
          ..write('absenceCommentaire: $absenceCommentaire, ')
          ..write('difficultes: $difficultes, ')
          ..write('notesComplementaires: $notesComplementaires, ')
          ..write('nbSorties: $nbSorties, ')
          ..write('nbMaintenus: $nbMaintenus, ')
          ..write('updatedAt: $updatedAt, ')
          ..write('synced: $synced, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

class $SyncQueueTableTable extends SyncQueueTable
    with TableInfo<$SyncQueueTableTable, SyncQueueItem> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $SyncQueueTableTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<String> id = GeneratedColumn<String>(
      'id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _tenantIdMeta =
      const VerificationMeta('tenantId');
  @override
  late final GeneratedColumn<String> tenantId = GeneratedColumn<String>(
      'tenant_id', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _operationMeta =
      const VerificationMeta('operation');
  @override
  late final GeneratedColumn<String> operation = GeneratedColumn<String>(
      'operation', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _endpointMeta =
      const VerificationMeta('endpoint');
  @override
  late final GeneratedColumn<String> endpoint = GeneratedColumn<String>(
      'endpoint', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _payloadMeta =
      const VerificationMeta('payload');
  @override
  late final GeneratedColumn<String> payload = GeneratedColumn<String>(
      'payload', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _createdAtMeta =
      const VerificationMeta('createdAt');
  @override
  late final GeneratedColumn<String> createdAt = GeneratedColumn<String>(
      'created_at', aliasedName, false,
      type: DriftSqlType.string, requiredDuringInsert: true);
  static const VerificationMeta _retryCountMeta =
      const VerificationMeta('retryCount');
  @override
  late final GeneratedColumn<int> retryCount = GeneratedColumn<int>(
      'retry_count', aliasedName, false,
      type: DriftSqlType.int,
      requiredDuringInsert: false,
      defaultValue: const Constant(0));
  static const VerificationMeta _lastErrorMeta =
      const VerificationMeta('lastError');
  @override
  late final GeneratedColumn<String> lastError = GeneratedColumn<String>(
      'last_error', aliasedName, true,
      type: DriftSqlType.string, requiredDuringInsert: false);
  @override
  List<GeneratedColumn> get $columns => [
        id,
        tenantId,
        operation,
        endpoint,
        payload,
        createdAt,
        retryCount,
        lastError
      ];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'sync_queue_table';
  @override
  VerificationContext validateIntegrity(Insertable<SyncQueueItem> instance,
      {bool isInserting = false}) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    } else if (isInserting) {
      context.missing(_idMeta);
    }
    if (data.containsKey('tenant_id')) {
      context.handle(_tenantIdMeta,
          tenantId.isAcceptableOrUnknown(data['tenant_id']!, _tenantIdMeta));
    } else if (isInserting) {
      context.missing(_tenantIdMeta);
    }
    if (data.containsKey('operation')) {
      context.handle(_operationMeta,
          operation.isAcceptableOrUnknown(data['operation']!, _operationMeta));
    } else if (isInserting) {
      context.missing(_operationMeta);
    }
    if (data.containsKey('endpoint')) {
      context.handle(_endpointMeta,
          endpoint.isAcceptableOrUnknown(data['endpoint']!, _endpointMeta));
    } else if (isInserting) {
      context.missing(_endpointMeta);
    }
    if (data.containsKey('payload')) {
      context.handle(_payloadMeta,
          payload.isAcceptableOrUnknown(data['payload']!, _payloadMeta));
    } else if (isInserting) {
      context.missing(_payloadMeta);
    }
    if (data.containsKey('created_at')) {
      context.handle(_createdAtMeta,
          createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta));
    } else if (isInserting) {
      context.missing(_createdAtMeta);
    }
    if (data.containsKey('retry_count')) {
      context.handle(
          _retryCountMeta,
          retryCount.isAcceptableOrUnknown(
              data['retry_count']!, _retryCountMeta));
    }
    if (data.containsKey('last_error')) {
      context.handle(_lastErrorMeta,
          lastError.isAcceptableOrUnknown(data['last_error']!, _lastErrorMeta));
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  SyncQueueItem map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return SyncQueueItem(
      id: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}id'])!,
      tenantId: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}tenant_id'])!,
      operation: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}operation'])!,
      endpoint: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}endpoint'])!,
      payload: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}payload'])!,
      createdAt: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}created_at'])!,
      retryCount: attachedDatabase.typeMapping
          .read(DriftSqlType.int, data['${effectivePrefix}retry_count'])!,
      lastError: attachedDatabase.typeMapping
          .read(DriftSqlType.string, data['${effectivePrefix}last_error']),
    );
  }

  @override
  $SyncQueueTableTable createAlias(String alias) {
    return $SyncQueueTableTable(attachedDatabase, alias);
  }
}

class SyncQueueItem extends DataClass implements Insertable<SyncQueueItem> {
  final String id;
  final String tenantId;
  final String operation;
  final String endpoint;
  final String payload;
  final String createdAt;
  final int retryCount;
  final String? lastError;
  const SyncQueueItem(
      {required this.id,
      required this.tenantId,
      required this.operation,
      required this.endpoint,
      required this.payload,
      required this.createdAt,
      required this.retryCount,
      this.lastError});
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<String>(id);
    map['tenant_id'] = Variable<String>(tenantId);
    map['operation'] = Variable<String>(operation);
    map['endpoint'] = Variable<String>(endpoint);
    map['payload'] = Variable<String>(payload);
    map['created_at'] = Variable<String>(createdAt);
    map['retry_count'] = Variable<int>(retryCount);
    if (!nullToAbsent || lastError != null) {
      map['last_error'] = Variable<String>(lastError);
    }
    return map;
  }

  SyncQueueTableCompanion toCompanion(bool nullToAbsent) {
    return SyncQueueTableCompanion(
      id: Value(id),
      tenantId: Value(tenantId),
      operation: Value(operation),
      endpoint: Value(endpoint),
      payload: Value(payload),
      createdAt: Value(createdAt),
      retryCount: Value(retryCount),
      lastError: lastError == null && nullToAbsent
          ? const Value.absent()
          : Value(lastError),
    );
  }

  factory SyncQueueItem.fromJson(Map<String, dynamic> json,
      {ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return SyncQueueItem(
      id: serializer.fromJson<String>(json['id']),
      tenantId: serializer.fromJson<String>(json['tenantId']),
      operation: serializer.fromJson<String>(json['operation']),
      endpoint: serializer.fromJson<String>(json['endpoint']),
      payload: serializer.fromJson<String>(json['payload']),
      createdAt: serializer.fromJson<String>(json['createdAt']),
      retryCount: serializer.fromJson<int>(json['retryCount']),
      lastError: serializer.fromJson<String?>(json['lastError']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<String>(id),
      'tenantId': serializer.toJson<String>(tenantId),
      'operation': serializer.toJson<String>(operation),
      'endpoint': serializer.toJson<String>(endpoint),
      'payload': serializer.toJson<String>(payload),
      'createdAt': serializer.toJson<String>(createdAt),
      'retryCount': serializer.toJson<int>(retryCount),
      'lastError': serializer.toJson<String?>(lastError),
    };
  }

  SyncQueueItem copyWith(
          {String? id,
          String? tenantId,
          String? operation,
          String? endpoint,
          String? payload,
          String? createdAt,
          int? retryCount,
          Value<String?> lastError = const Value.absent()}) =>
      SyncQueueItem(
        id: id ?? this.id,
        tenantId: tenantId ?? this.tenantId,
        operation: operation ?? this.operation,
        endpoint: endpoint ?? this.endpoint,
        payload: payload ?? this.payload,
        createdAt: createdAt ?? this.createdAt,
        retryCount: retryCount ?? this.retryCount,
        lastError: lastError.present ? lastError.value : this.lastError,
      );
  SyncQueueItem copyWithCompanion(SyncQueueTableCompanion data) {
    return SyncQueueItem(
      id: data.id.present ? data.id.value : this.id,
      tenantId: data.tenantId.present ? data.tenantId.value : this.tenantId,
      operation: data.operation.present ? data.operation.value : this.operation,
      endpoint: data.endpoint.present ? data.endpoint.value : this.endpoint,
      payload: data.payload.present ? data.payload.value : this.payload,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
      retryCount:
          data.retryCount.present ? data.retryCount.value : this.retryCount,
      lastError: data.lastError.present ? data.lastError.value : this.lastError,
    );
  }

  @override
  String toString() {
    return (StringBuffer('SyncQueueItem(')
          ..write('id: $id, ')
          ..write('tenantId: $tenantId, ')
          ..write('operation: $operation, ')
          ..write('endpoint: $endpoint, ')
          ..write('payload: $payload, ')
          ..write('createdAt: $createdAt, ')
          ..write('retryCount: $retryCount, ')
          ..write('lastError: $lastError')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, tenantId, operation, endpoint, payload,
      createdAt, retryCount, lastError);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is SyncQueueItem &&
          other.id == this.id &&
          other.tenantId == this.tenantId &&
          other.operation == this.operation &&
          other.endpoint == this.endpoint &&
          other.payload == this.payload &&
          other.createdAt == this.createdAt &&
          other.retryCount == this.retryCount &&
          other.lastError == this.lastError);
}

class SyncQueueTableCompanion extends UpdateCompanion<SyncQueueItem> {
  final Value<String> id;
  final Value<String> tenantId;
  final Value<String> operation;
  final Value<String> endpoint;
  final Value<String> payload;
  final Value<String> createdAt;
  final Value<int> retryCount;
  final Value<String?> lastError;
  final Value<int> rowid;
  const SyncQueueTableCompanion({
    this.id = const Value.absent(),
    this.tenantId = const Value.absent(),
    this.operation = const Value.absent(),
    this.endpoint = const Value.absent(),
    this.payload = const Value.absent(),
    this.createdAt = const Value.absent(),
    this.retryCount = const Value.absent(),
    this.lastError = const Value.absent(),
    this.rowid = const Value.absent(),
  });
  SyncQueueTableCompanion.insert({
    required String id,
    required String tenantId,
    required String operation,
    required String endpoint,
    required String payload,
    required String createdAt,
    this.retryCount = const Value.absent(),
    this.lastError = const Value.absent(),
    this.rowid = const Value.absent(),
  })  : id = Value(id),
        tenantId = Value(tenantId),
        operation = Value(operation),
        endpoint = Value(endpoint),
        payload = Value(payload),
        createdAt = Value(createdAt);
  static Insertable<SyncQueueItem> custom({
    Expression<String>? id,
    Expression<String>? tenantId,
    Expression<String>? operation,
    Expression<String>? endpoint,
    Expression<String>? payload,
    Expression<String>? createdAt,
    Expression<int>? retryCount,
    Expression<String>? lastError,
    Expression<int>? rowid,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (tenantId != null) 'tenant_id': tenantId,
      if (operation != null) 'operation': operation,
      if (endpoint != null) 'endpoint': endpoint,
      if (payload != null) 'payload': payload,
      if (createdAt != null) 'created_at': createdAt,
      if (retryCount != null) 'retry_count': retryCount,
      if (lastError != null) 'last_error': lastError,
      if (rowid != null) 'rowid': rowid,
    });
  }

  SyncQueueTableCompanion copyWith(
      {Value<String>? id,
      Value<String>? tenantId,
      Value<String>? operation,
      Value<String>? endpoint,
      Value<String>? payload,
      Value<String>? createdAt,
      Value<int>? retryCount,
      Value<String?>? lastError,
      Value<int>? rowid}) {
    return SyncQueueTableCompanion(
      id: id ?? this.id,
      tenantId: tenantId ?? this.tenantId,
      operation: operation ?? this.operation,
      endpoint: endpoint ?? this.endpoint,
      payload: payload ?? this.payload,
      createdAt: createdAt ?? this.createdAt,
      retryCount: retryCount ?? this.retryCount,
      lastError: lastError ?? this.lastError,
      rowid: rowid ?? this.rowid,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<String>(id.value);
    }
    if (tenantId.present) {
      map['tenant_id'] = Variable<String>(tenantId.value);
    }
    if (operation.present) {
      map['operation'] = Variable<String>(operation.value);
    }
    if (endpoint.present) {
      map['endpoint'] = Variable<String>(endpoint.value);
    }
    if (payload.present) {
      map['payload'] = Variable<String>(payload.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<String>(createdAt.value);
    }
    if (retryCount.present) {
      map['retry_count'] = Variable<int>(retryCount.value);
    }
    if (lastError.present) {
      map['last_error'] = Variable<String>(lastError.value);
    }
    if (rowid.present) {
      map['rowid'] = Variable<int>(rowid.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('SyncQueueTableCompanion(')
          ..write('id: $id, ')
          ..write('tenantId: $tenantId, ')
          ..write('operation: $operation, ')
          ..write('endpoint: $endpoint, ')
          ..write('payload: $payload, ')
          ..write('createdAt: $createdAt, ')
          ..write('retryCount: $retryCount, ')
          ..write('lastError: $lastError, ')
          ..write('rowid: $rowid')
          ..write(')'))
        .toString();
  }
}

abstract class _$AppDatabase extends GeneratedDatabase {
  _$AppDatabase(QueryExecutor e) : super(e);
  $AppDatabaseManager get managers => $AppDatabaseManager(this);
  late final $SoulsTableTable soulsTable = $SoulsTableTable(this);
  late final $ReportDraftsTableTable reportDraftsTable =
      $ReportDraftsTableTable(this);
  late final $SyncQueueTableTable syncQueueTable = $SyncQueueTableTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities =>
      [soulsTable, reportDraftsTable, syncQueueTable];
}

typedef $$SoulsTableTableCreateCompanionBuilder = SoulsTableCompanion Function({
  required String id,
  required String tenantId,
  required String nom,
  Value<String?> prenom,
  Value<String?> email,
  Value<String?> telephone,
  required String typeDisciple,
  required String statut,
  required String dateIntegration,
  required String faiseurId,
  Value<String?> familleId,
  Value<String?> dateDernierContact,
  required String lastSyncAt,
  Value<int> rowid,
});
typedef $$SoulsTableTableUpdateCompanionBuilder = SoulsTableCompanion Function({
  Value<String> id,
  Value<String> tenantId,
  Value<String> nom,
  Value<String?> prenom,
  Value<String?> email,
  Value<String?> telephone,
  Value<String> typeDisciple,
  Value<String> statut,
  Value<String> dateIntegration,
  Value<String> faiseurId,
  Value<String?> familleId,
  Value<String?> dateDernierContact,
  Value<String> lastSyncAt,
  Value<int> rowid,
});

class $$SoulsTableTableFilterComposer
    extends Composer<_$AppDatabase, $SoulsTableTable> {
  $$SoulsTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get tenantId => $composableBuilder(
      column: $table.tenantId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get nom => $composableBuilder(
      column: $table.nom, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get prenom => $composableBuilder(
      column: $table.prenom, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get email => $composableBuilder(
      column: $table.email, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get telephone => $composableBuilder(
      column: $table.telephone, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get typeDisciple => $composableBuilder(
      column: $table.typeDisciple, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get statut => $composableBuilder(
      column: $table.statut, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get dateIntegration => $composableBuilder(
      column: $table.dateIntegration,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get faiseurId => $composableBuilder(
      column: $table.faiseurId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get familleId => $composableBuilder(
      column: $table.familleId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get dateDernierContact => $composableBuilder(
      column: $table.dateDernierContact,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get lastSyncAt => $composableBuilder(
      column: $table.lastSyncAt, builder: (column) => ColumnFilters(column));
}

class $$SoulsTableTableOrderingComposer
    extends Composer<_$AppDatabase, $SoulsTableTable> {
  $$SoulsTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get tenantId => $composableBuilder(
      column: $table.tenantId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get nom => $composableBuilder(
      column: $table.nom, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get prenom => $composableBuilder(
      column: $table.prenom, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get email => $composableBuilder(
      column: $table.email, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get telephone => $composableBuilder(
      column: $table.telephone, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get typeDisciple => $composableBuilder(
      column: $table.typeDisciple,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get statut => $composableBuilder(
      column: $table.statut, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get dateIntegration => $composableBuilder(
      column: $table.dateIntegration,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get faiseurId => $composableBuilder(
      column: $table.faiseurId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get familleId => $composableBuilder(
      column: $table.familleId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get dateDernierContact => $composableBuilder(
      column: $table.dateDernierContact,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get lastSyncAt => $composableBuilder(
      column: $table.lastSyncAt, builder: (column) => ColumnOrderings(column));
}

class $$SoulsTableTableAnnotationComposer
    extends Composer<_$AppDatabase, $SoulsTableTable> {
  $$SoulsTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get tenantId =>
      $composableBuilder(column: $table.tenantId, builder: (column) => column);

  GeneratedColumn<String> get nom =>
      $composableBuilder(column: $table.nom, builder: (column) => column);

  GeneratedColumn<String> get prenom =>
      $composableBuilder(column: $table.prenom, builder: (column) => column);

  GeneratedColumn<String> get email =>
      $composableBuilder(column: $table.email, builder: (column) => column);

  GeneratedColumn<String> get telephone =>
      $composableBuilder(column: $table.telephone, builder: (column) => column);

  GeneratedColumn<String> get typeDisciple => $composableBuilder(
      column: $table.typeDisciple, builder: (column) => column);

  GeneratedColumn<String> get statut =>
      $composableBuilder(column: $table.statut, builder: (column) => column);

  GeneratedColumn<String> get dateIntegration => $composableBuilder(
      column: $table.dateIntegration, builder: (column) => column);

  GeneratedColumn<String> get faiseurId =>
      $composableBuilder(column: $table.faiseurId, builder: (column) => column);

  GeneratedColumn<String> get familleId =>
      $composableBuilder(column: $table.familleId, builder: (column) => column);

  GeneratedColumn<String> get dateDernierContact => $composableBuilder(
      column: $table.dateDernierContact, builder: (column) => column);

  GeneratedColumn<String> get lastSyncAt => $composableBuilder(
      column: $table.lastSyncAt, builder: (column) => column);
}

class $$SoulsTableTableTableManager extends RootTableManager<
    _$AppDatabase,
    $SoulsTableTable,
    SoulLocal,
    $$SoulsTableTableFilterComposer,
    $$SoulsTableTableOrderingComposer,
    $$SoulsTableTableAnnotationComposer,
    $$SoulsTableTableCreateCompanionBuilder,
    $$SoulsTableTableUpdateCompanionBuilder,
    (SoulLocal, BaseReferences<_$AppDatabase, $SoulsTableTable, SoulLocal>),
    SoulLocal,
    PrefetchHooks Function()> {
  $$SoulsTableTableTableManager(_$AppDatabase db, $SoulsTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$SoulsTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$SoulsTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$SoulsTableTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> tenantId = const Value.absent(),
            Value<String> nom = const Value.absent(),
            Value<String?> prenom = const Value.absent(),
            Value<String?> email = const Value.absent(),
            Value<String?> telephone = const Value.absent(),
            Value<String> typeDisciple = const Value.absent(),
            Value<String> statut = const Value.absent(),
            Value<String> dateIntegration = const Value.absent(),
            Value<String> faiseurId = const Value.absent(),
            Value<String?> familleId = const Value.absent(),
            Value<String?> dateDernierContact = const Value.absent(),
            Value<String> lastSyncAt = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              SoulsTableCompanion(
            id: id,
            tenantId: tenantId,
            nom: nom,
            prenom: prenom,
            email: email,
            telephone: telephone,
            typeDisciple: typeDisciple,
            statut: statut,
            dateIntegration: dateIntegration,
            faiseurId: faiseurId,
            familleId: familleId,
            dateDernierContact: dateDernierContact,
            lastSyncAt: lastSyncAt,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String tenantId,
            required String nom,
            Value<String?> prenom = const Value.absent(),
            Value<String?> email = const Value.absent(),
            Value<String?> telephone = const Value.absent(),
            required String typeDisciple,
            required String statut,
            required String dateIntegration,
            required String faiseurId,
            Value<String?> familleId = const Value.absent(),
            Value<String?> dateDernierContact = const Value.absent(),
            required String lastSyncAt,
            Value<int> rowid = const Value.absent(),
          }) =>
              SoulsTableCompanion.insert(
            id: id,
            tenantId: tenantId,
            nom: nom,
            prenom: prenom,
            email: email,
            telephone: telephone,
            typeDisciple: typeDisciple,
            statut: statut,
            dateIntegration: dateIntegration,
            faiseurId: faiseurId,
            familleId: familleId,
            dateDernierContact: dateDernierContact,
            lastSyncAt: lastSyncAt,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$SoulsTableTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $SoulsTableTable,
    SoulLocal,
    $$SoulsTableTableFilterComposer,
    $$SoulsTableTableOrderingComposer,
    $$SoulsTableTableAnnotationComposer,
    $$SoulsTableTableCreateCompanionBuilder,
    $$SoulsTableTableUpdateCompanionBuilder,
    (SoulLocal, BaseReferences<_$AppDatabase, $SoulsTableTable, SoulLocal>),
    SoulLocal,
    PrefetchHooks Function()>;
typedef $$ReportDraftsTableTableCreateCompanionBuilder
    = ReportDraftsTableCompanion Function({
  required String id,
  required String tenantId,
  required String ameId,
  required String semaine,
  required String presencesParCulte,
  Value<String?> absenceRaison,
  Value<String?> absenceCommentaire,
  Value<String?> difficultes,
  Value<String?> notesComplementaires,
  Value<int> nbSorties,
  Value<int> nbMaintenus,
  required String updatedAt,
  Value<bool> synced,
  Value<int> rowid,
});
typedef $$ReportDraftsTableTableUpdateCompanionBuilder
    = ReportDraftsTableCompanion Function({
  Value<String> id,
  Value<String> tenantId,
  Value<String> ameId,
  Value<String> semaine,
  Value<String> presencesParCulte,
  Value<String?> absenceRaison,
  Value<String?> absenceCommentaire,
  Value<String?> difficultes,
  Value<String?> notesComplementaires,
  Value<int> nbSorties,
  Value<int> nbMaintenus,
  Value<String> updatedAt,
  Value<bool> synced,
  Value<int> rowid,
});

class $$ReportDraftsTableTableFilterComposer
    extends Composer<_$AppDatabase, $ReportDraftsTableTable> {
  $$ReportDraftsTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get tenantId => $composableBuilder(
      column: $table.tenantId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get ameId => $composableBuilder(
      column: $table.ameId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get semaine => $composableBuilder(
      column: $table.semaine, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get presencesParCulte => $composableBuilder(
      column: $table.presencesParCulte,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get absenceRaison => $composableBuilder(
      column: $table.absenceRaison, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get absenceCommentaire => $composableBuilder(
      column: $table.absenceCommentaire,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get difficultes => $composableBuilder(
      column: $table.difficultes, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get notesComplementaires => $composableBuilder(
      column: $table.notesComplementaires,
      builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get nbSorties => $composableBuilder(
      column: $table.nbSorties, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get nbMaintenus => $composableBuilder(
      column: $table.nbMaintenus, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<bool> get synced => $composableBuilder(
      column: $table.synced, builder: (column) => ColumnFilters(column));
}

class $$ReportDraftsTableTableOrderingComposer
    extends Composer<_$AppDatabase, $ReportDraftsTableTable> {
  $$ReportDraftsTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get tenantId => $composableBuilder(
      column: $table.tenantId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get ameId => $composableBuilder(
      column: $table.ameId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get semaine => $composableBuilder(
      column: $table.semaine, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get presencesParCulte => $composableBuilder(
      column: $table.presencesParCulte,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get absenceRaison => $composableBuilder(
      column: $table.absenceRaison,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get absenceCommentaire => $composableBuilder(
      column: $table.absenceCommentaire,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get difficultes => $composableBuilder(
      column: $table.difficultes, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get notesComplementaires => $composableBuilder(
      column: $table.notesComplementaires,
      builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get nbSorties => $composableBuilder(
      column: $table.nbSorties, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get nbMaintenus => $composableBuilder(
      column: $table.nbMaintenus, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get updatedAt => $composableBuilder(
      column: $table.updatedAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<bool> get synced => $composableBuilder(
      column: $table.synced, builder: (column) => ColumnOrderings(column));
}

class $$ReportDraftsTableTableAnnotationComposer
    extends Composer<_$AppDatabase, $ReportDraftsTableTable> {
  $$ReportDraftsTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get tenantId =>
      $composableBuilder(column: $table.tenantId, builder: (column) => column);

  GeneratedColumn<String> get ameId =>
      $composableBuilder(column: $table.ameId, builder: (column) => column);

  GeneratedColumn<String> get semaine =>
      $composableBuilder(column: $table.semaine, builder: (column) => column);

  GeneratedColumn<String> get presencesParCulte => $composableBuilder(
      column: $table.presencesParCulte, builder: (column) => column);

  GeneratedColumn<String> get absenceRaison => $composableBuilder(
      column: $table.absenceRaison, builder: (column) => column);

  GeneratedColumn<String> get absenceCommentaire => $composableBuilder(
      column: $table.absenceCommentaire, builder: (column) => column);

  GeneratedColumn<String> get difficultes => $composableBuilder(
      column: $table.difficultes, builder: (column) => column);

  GeneratedColumn<String> get notesComplementaires => $composableBuilder(
      column: $table.notesComplementaires, builder: (column) => column);

  GeneratedColumn<int> get nbSorties =>
      $composableBuilder(column: $table.nbSorties, builder: (column) => column);

  GeneratedColumn<int> get nbMaintenus => $composableBuilder(
      column: $table.nbMaintenus, builder: (column) => column);

  GeneratedColumn<String> get updatedAt =>
      $composableBuilder(column: $table.updatedAt, builder: (column) => column);

  GeneratedColumn<bool> get synced =>
      $composableBuilder(column: $table.synced, builder: (column) => column);
}

class $$ReportDraftsTableTableTableManager extends RootTableManager<
    _$AppDatabase,
    $ReportDraftsTableTable,
    ReportDraft,
    $$ReportDraftsTableTableFilterComposer,
    $$ReportDraftsTableTableOrderingComposer,
    $$ReportDraftsTableTableAnnotationComposer,
    $$ReportDraftsTableTableCreateCompanionBuilder,
    $$ReportDraftsTableTableUpdateCompanionBuilder,
    (
      ReportDraft,
      BaseReferences<_$AppDatabase, $ReportDraftsTableTable, ReportDraft>
    ),
    ReportDraft,
    PrefetchHooks Function()> {
  $$ReportDraftsTableTableTableManager(
      _$AppDatabase db, $ReportDraftsTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$ReportDraftsTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$ReportDraftsTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$ReportDraftsTableTableAnnotationComposer(
                  $db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> tenantId = const Value.absent(),
            Value<String> ameId = const Value.absent(),
            Value<String> semaine = const Value.absent(),
            Value<String> presencesParCulte = const Value.absent(),
            Value<String?> absenceRaison = const Value.absent(),
            Value<String?> absenceCommentaire = const Value.absent(),
            Value<String?> difficultes = const Value.absent(),
            Value<String?> notesComplementaires = const Value.absent(),
            Value<int> nbSorties = const Value.absent(),
            Value<int> nbMaintenus = const Value.absent(),
            Value<String> updatedAt = const Value.absent(),
            Value<bool> synced = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              ReportDraftsTableCompanion(
            id: id,
            tenantId: tenantId,
            ameId: ameId,
            semaine: semaine,
            presencesParCulte: presencesParCulte,
            absenceRaison: absenceRaison,
            absenceCommentaire: absenceCommentaire,
            difficultes: difficultes,
            notesComplementaires: notesComplementaires,
            nbSorties: nbSorties,
            nbMaintenus: nbMaintenus,
            updatedAt: updatedAt,
            synced: synced,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String tenantId,
            required String ameId,
            required String semaine,
            required String presencesParCulte,
            Value<String?> absenceRaison = const Value.absent(),
            Value<String?> absenceCommentaire = const Value.absent(),
            Value<String?> difficultes = const Value.absent(),
            Value<String?> notesComplementaires = const Value.absent(),
            Value<int> nbSorties = const Value.absent(),
            Value<int> nbMaintenus = const Value.absent(),
            required String updatedAt,
            Value<bool> synced = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              ReportDraftsTableCompanion.insert(
            id: id,
            tenantId: tenantId,
            ameId: ameId,
            semaine: semaine,
            presencesParCulte: presencesParCulte,
            absenceRaison: absenceRaison,
            absenceCommentaire: absenceCommentaire,
            difficultes: difficultes,
            notesComplementaires: notesComplementaires,
            nbSorties: nbSorties,
            nbMaintenus: nbMaintenus,
            updatedAt: updatedAt,
            synced: synced,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$ReportDraftsTableTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $ReportDraftsTableTable,
    ReportDraft,
    $$ReportDraftsTableTableFilterComposer,
    $$ReportDraftsTableTableOrderingComposer,
    $$ReportDraftsTableTableAnnotationComposer,
    $$ReportDraftsTableTableCreateCompanionBuilder,
    $$ReportDraftsTableTableUpdateCompanionBuilder,
    (
      ReportDraft,
      BaseReferences<_$AppDatabase, $ReportDraftsTableTable, ReportDraft>
    ),
    ReportDraft,
    PrefetchHooks Function()>;
typedef $$SyncQueueTableTableCreateCompanionBuilder = SyncQueueTableCompanion
    Function({
  required String id,
  required String tenantId,
  required String operation,
  required String endpoint,
  required String payload,
  required String createdAt,
  Value<int> retryCount,
  Value<String?> lastError,
  Value<int> rowid,
});
typedef $$SyncQueueTableTableUpdateCompanionBuilder = SyncQueueTableCompanion
    Function({
  Value<String> id,
  Value<String> tenantId,
  Value<String> operation,
  Value<String> endpoint,
  Value<String> payload,
  Value<String> createdAt,
  Value<int> retryCount,
  Value<String?> lastError,
  Value<int> rowid,
});

class $$SyncQueueTableTableFilterComposer
    extends Composer<_$AppDatabase, $SyncQueueTableTable> {
  $$SyncQueueTableTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get tenantId => $composableBuilder(
      column: $table.tenantId, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get operation => $composableBuilder(
      column: $table.operation, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get endpoint => $composableBuilder(
      column: $table.endpoint, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get payload => $composableBuilder(
      column: $table.payload, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get createdAt => $composableBuilder(
      column: $table.createdAt, builder: (column) => ColumnFilters(column));

  ColumnFilters<int> get retryCount => $composableBuilder(
      column: $table.retryCount, builder: (column) => ColumnFilters(column));

  ColumnFilters<String> get lastError => $composableBuilder(
      column: $table.lastError, builder: (column) => ColumnFilters(column));
}

class $$SyncQueueTableTableOrderingComposer
    extends Composer<_$AppDatabase, $SyncQueueTableTable> {
  $$SyncQueueTableTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<String> get id => $composableBuilder(
      column: $table.id, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get tenantId => $composableBuilder(
      column: $table.tenantId, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get operation => $composableBuilder(
      column: $table.operation, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get endpoint => $composableBuilder(
      column: $table.endpoint, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get payload => $composableBuilder(
      column: $table.payload, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get createdAt => $composableBuilder(
      column: $table.createdAt, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<int> get retryCount => $composableBuilder(
      column: $table.retryCount, builder: (column) => ColumnOrderings(column));

  ColumnOrderings<String> get lastError => $composableBuilder(
      column: $table.lastError, builder: (column) => ColumnOrderings(column));
}

class $$SyncQueueTableTableAnnotationComposer
    extends Composer<_$AppDatabase, $SyncQueueTableTable> {
  $$SyncQueueTableTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<String> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<String> get tenantId =>
      $composableBuilder(column: $table.tenantId, builder: (column) => column);

  GeneratedColumn<String> get operation =>
      $composableBuilder(column: $table.operation, builder: (column) => column);

  GeneratedColumn<String> get endpoint =>
      $composableBuilder(column: $table.endpoint, builder: (column) => column);

  GeneratedColumn<String> get payload =>
      $composableBuilder(column: $table.payload, builder: (column) => column);

  GeneratedColumn<String> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);

  GeneratedColumn<int> get retryCount => $composableBuilder(
      column: $table.retryCount, builder: (column) => column);

  GeneratedColumn<String> get lastError =>
      $composableBuilder(column: $table.lastError, builder: (column) => column);
}

class $$SyncQueueTableTableTableManager extends RootTableManager<
    _$AppDatabase,
    $SyncQueueTableTable,
    SyncQueueItem,
    $$SyncQueueTableTableFilterComposer,
    $$SyncQueueTableTableOrderingComposer,
    $$SyncQueueTableTableAnnotationComposer,
    $$SyncQueueTableTableCreateCompanionBuilder,
    $$SyncQueueTableTableUpdateCompanionBuilder,
    (
      SyncQueueItem,
      BaseReferences<_$AppDatabase, $SyncQueueTableTable, SyncQueueItem>
    ),
    SyncQueueItem,
    PrefetchHooks Function()> {
  $$SyncQueueTableTableTableManager(
      _$AppDatabase db, $SyncQueueTableTable table)
      : super(TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$SyncQueueTableTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$SyncQueueTableTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$SyncQueueTableTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback: ({
            Value<String> id = const Value.absent(),
            Value<String> tenantId = const Value.absent(),
            Value<String> operation = const Value.absent(),
            Value<String> endpoint = const Value.absent(),
            Value<String> payload = const Value.absent(),
            Value<String> createdAt = const Value.absent(),
            Value<int> retryCount = const Value.absent(),
            Value<String?> lastError = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              SyncQueueTableCompanion(
            id: id,
            tenantId: tenantId,
            operation: operation,
            endpoint: endpoint,
            payload: payload,
            createdAt: createdAt,
            retryCount: retryCount,
            lastError: lastError,
            rowid: rowid,
          ),
          createCompanionCallback: ({
            required String id,
            required String tenantId,
            required String operation,
            required String endpoint,
            required String payload,
            required String createdAt,
            Value<int> retryCount = const Value.absent(),
            Value<String?> lastError = const Value.absent(),
            Value<int> rowid = const Value.absent(),
          }) =>
              SyncQueueTableCompanion.insert(
            id: id,
            tenantId: tenantId,
            operation: operation,
            endpoint: endpoint,
            payload: payload,
            createdAt: createdAt,
            retryCount: retryCount,
            lastError: lastError,
            rowid: rowid,
          ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ));
}

typedef $$SyncQueueTableTableProcessedTableManager = ProcessedTableManager<
    _$AppDatabase,
    $SyncQueueTableTable,
    SyncQueueItem,
    $$SyncQueueTableTableFilterComposer,
    $$SyncQueueTableTableOrderingComposer,
    $$SyncQueueTableTableAnnotationComposer,
    $$SyncQueueTableTableCreateCompanionBuilder,
    $$SyncQueueTableTableUpdateCompanionBuilder,
    (
      SyncQueueItem,
      BaseReferences<_$AppDatabase, $SyncQueueTableTable, SyncQueueItem>
    ),
    SyncQueueItem,
    PrefetchHooks Function()>;

class $AppDatabaseManager {
  final _$AppDatabase _db;
  $AppDatabaseManager(this._db);
  $$SoulsTableTableTableManager get soulsTable =>
      $$SoulsTableTableTableManager(_db, _db.soulsTable);
  $$ReportDraftsTableTableTableManager get reportDraftsTable =>
      $$ReportDraftsTableTableTableManager(_db, _db.reportDraftsTable);
  $$SyncQueueTableTableTableManager get syncQueueTable =>
      $$SyncQueueTableTableTableManager(_db, _db.syncQueueTable);
}
