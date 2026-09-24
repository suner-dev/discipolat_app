import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../tenant_config.dart';

/// Modèle pour la configuration d'abonnement
class Subscription {
  final String id;
  final String tenantId;
  final String planKey;
  final String status;
  final String billingCycle;
  final DateTime currentPeriodStart;
  final DateTime currentPeriodEnd;
  final bool cancelAtPeriodEnd;
  final DateTime? canceledAt;
  final DateTime? trialEndsAt;
  final Quotas quotas;
  final Plan? plan;

  Subscription({
    required this.id,
    required this.tenantId,
    required this.planKey,
    required this.status,
    required this.billingCycle,
    required this.currentPeriodStart,
    required this.currentPeriodEnd,
    required this.cancelAtPeriodEnd,
    this.canceledAt,
    this.trialEndsAt,
    required this.quotas,
    this.plan,
  });

  factory Subscription.fromJson(Map<String, dynamic> json) {
    return Subscription(
      id: json['id'] ?? '',
      tenantId: json['tenantId'] ?? '',
      planKey: json['planKey'] ?? '',
      status: json['status'] ?? 'TRIAL',
      billingCycle: json['billingCycle'] ?? 'monthly',
      currentPeriodStart:
          DateTime.tryParse(json['currentPeriodStart'] ?? '') ?? DateTime.now(),
      currentPeriodEnd:
          DateTime.tryParse(json['currentPeriodEnd'] ?? '') ?? DateTime.now(),
      cancelAtPeriodEnd: json['cancelAtPeriodEnd'] ?? false,
      canceledAt: json['canceledAt'] != null
          ? DateTime.tryParse(json['canceledAt'])
          : null,
      trialEndsAt: json['trialEndsAt'] != null
          ? DateTime.tryParse(json['trialEndsAt'])
          : null,
      quotas: Quotas.fromJson(json['quotas'] ?? json['limits'] ?? {}),
      plan: json['plan'] != null ? Plan.fromJson(json['plan']) : null,
    );
  }

  bool get isActive => status == 'ACTIVE';
  bool get isTrial => status == 'TRIAL';
  bool get isPastDue => status == 'PAST_DUE';
  bool get isCanceled => status == 'CANCELED';
}

/// Modèle pour le plan SaaS
class Plan {
  final String id;
  final String key;
  final String name;
  final String description;
  final int priceMonthly;
  final int priceYearly;
  final String currency;
  final Map<String, bool> features;

  Plan({
    required this.id,
    required this.key,
    required this.name,
    required this.description,
    required this.priceMonthly,
    required this.priceYearly,
    required this.currency,
    required this.features,
  });

  factory Plan.fromJson(Map<String, dynamic> json) {
    dynamic rawFeatures = json['features'];
    if (rawFeatures is String) {
      try {
        rawFeatures = jsonDecode(rawFeatures);
      } catch (_) {
        rawFeatures = null;
      }
    }
    final features = rawFeatures is Map
        ? Map<String, dynamic>.from(rawFeatures)
        : const <String, dynamic>{};
    return Plan(
      id: json['id'] ?? '',
      key: json['key'] ?? '',
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      priceMonthly: json['priceMonthly'] ?? 0,
      priceYearly: json['priceYearly'] ?? 0,
      currency: json['currency'] ?? 'XAF',
      features: {
        for (final entry in features.entries)
          if (entry.value is bool) entry.key: entry.value as bool,
      },
    );
  }
}

/// Modèle pour les quotas
class Quotas {
  final int? maxUsers;
  final int? maxChurches;
  final int? maxDepartments;
  final int? maxStorageMb;
  final int? maxAiRequestsMonth;
  final int? maxCourses;
  final int? maxMessagesMonth;
  final int? currentUsers;
  final int? currentChurches;
  final int? currentDepartments;
  final int? currentStorageMb;
  final int? currentAiRequestsMonth;
  final int? currentCourses;
  final int? currentMessagesMonth;

  Quotas({
    required this.maxUsers,
    required this.maxChurches,
    required this.maxDepartments,
    required this.maxStorageMb,
    required this.maxAiRequestsMonth,
    required this.maxCourses,
    required this.maxMessagesMonth,
    required this.currentUsers,
    required this.currentChurches,
    required this.currentDepartments,
    required this.currentStorageMb,
    required this.currentAiRequestsMonth,
    required this.currentCourses,
    required this.currentMessagesMonth,
  });

  factory Quotas.fromJson(dynamic value) {
    dynamic decoded = value;
    if (value is String) {
      try {
        decoded = jsonDecode(value);
      } catch (_) {
        decoded = null;
      }
    }
    final json = decoded is Map
        ? Map<String, dynamic>.from(decoded)
        : const <String, dynamic>{};
    return Quotas(
      maxUsers:
          _numberForKeys(json, const ['maxUsers', 'max_users', 'members']),
      maxChurches: _numberForKeys(json, const ['maxChurches', 'max_churches']),
      maxDepartments:
          _numberForKeys(json, const ['maxDepartments', 'max_departments']),
      maxStorageMb: _numberForKeys(
          json, const ['maxStorageMb', 'max_storage_mb', 'storage_mb']),
      maxAiRequestsMonth: _numberForKeys(json,
          const ['maxAiRequestsMonth', 'max_ai_requests_month', 'ai_credits']),
      maxCourses: _numberForKeys(json, const ['maxCourses', 'max_courses']),
      maxMessagesMonth: _numberForKeys(
          json, const ['maxMessagesMonth', 'max_messages_month']),
      currentUsers:
          _numberForKeys(json, const ['currentUsers', 'current_users']),
      currentChurches:
          _numberForKeys(json, const ['currentChurches', 'current_churches']),
      currentDepartments: _numberForKeys(
          json, const ['currentDepartments', 'current_departments']),
      currentStorageMb: _numberForKeys(
          json, const ['currentStorageMb', 'current_storage_mb']),
      currentAiRequestsMonth: _numberForKeys(
          json, const ['currentAiRequestsMonth', 'current_ai_requests_month']),
      currentCourses:
          _numberForKeys(json, const ['currentCourses', 'current_courses']),
      currentMessagesMonth: _numberForKeys(
          json, const ['currentMessagesMonth', 'current_messages_month']),
    );
  }

  static int? _numberForKeys(
    Map<String, dynamic> json,
    List<String> keys,
  ) {
    for (final key in keys) {
      if (!json.containsKey(key)) continue;
      final value = json[key];
      if (value is num) return value.toInt();
      if (value is String) return int.tryParse(value);
      return null;
    }
    return null;
  }

  bool isExceeded(String quotaKey) {
    final current = _currentFor(quotaKey);
    final max = _maxFor(quotaKey);
    if (current == null || max == null || max <= 0) return false;
    return current >= max;
  }

  double? usagePercent(String quotaKey) {
    final current = _currentFor(quotaKey);
    final max = _maxFor(quotaKey);
    if (current == null || max == null || max <= 0) return null;
    return current / max;
  }

  int? _currentFor(String quotaKey) {
    switch (quotaKey) {
      case 'maxUsers':
        return currentUsers;
      case 'maxChurches':
        return currentChurches;
      case 'maxDepartments':
        return currentDepartments;
      case 'maxStorageMb':
        return currentStorageMb;
      case 'maxAiRequestsMonth':
        return currentAiRequestsMonth;
      case 'maxCourses':
        return currentCourses;
      case 'maxMessagesMonth':
        return currentMessagesMonth;
      default:
        return null;
    }
  }

  int? _maxFor(String quotaKey) {
    switch (quotaKey) {
      case 'maxUsers':
        return maxUsers;
      case 'maxChurches':
        return maxChurches;
      case 'maxDepartments':
        return maxDepartments;
      case 'maxStorageMb':
        return maxStorageMb;
      case 'maxAiRequestsMonth':
        return maxAiRequestsMonth;
      case 'maxCourses':
        return maxCourses;
      case 'maxMessagesMonth':
        return maxMessagesMonth;
      default:
        return null;
    }
  }
}

/// Modèle pour le nœud organisationnel
class OrganizationNode {
  final String id;
  final String tenantId;
  final String? parentId;
  final String type;
  final String name;
  final String code;
  final String status;
  final String path;
  final int level;
  final String? timezone;
  final String? country;
  final String? city;
  final Map<String, dynamic>? metadata;
  final String? responsibleId;
  final DateTime createdAt;

  OrganizationNode({
    required this.id,
    required this.tenantId,
    this.parentId,
    required this.type,
    required this.name,
    required this.code,
    required this.status,
    required this.path,
    required this.level,
    this.timezone,
    this.country,
    this.city,
    this.metadata,
    this.responsibleId,
    required this.createdAt,
  });

  factory OrganizationNode.fromJson(Map<String, dynamic> json) {
    return OrganizationNode(
      id: json['id'] ?? '',
      tenantId: json['tenantId'] ?? '',
      parentId: json['parentId'],
      type: json['type'] ?? '',
      name: json['name'] ?? '',
      code: json['code'] ?? '',
      status: json['status'] ?? 'ACTIVE',
      path: json['path'] ?? '',
      level: json['level'] ?? 0,
      timezone: json['timezone'],
      country: json['country'],
      city: json['city'],
      metadata: json['metadata'] != null
          ? Map<String, dynamic>.from(json['metadata'])
          : null,
      responsibleId: json['responsibleId'],
      createdAt: DateTime.tryParse(json['createdAt'] ?? '') ?? DateTime.now(),
    );
  }
}

/// Provider pour la session multi-tenant
class TenantSession extends ChangeNotifier {
  String? _activeTenantId;
  String? _activeOrgNodeId;
  String? _tenantName;
  String? _orgName;
  String? _userRole;
  String? _scopeType;
  String? _scopeId;
  Map<String, dynamic>? _permissions;
  bool _isLoaded = false;
  bool _multipleTenants = false;
  List<Map<String, dynamic>> _tenants = [];
  Subscription? _subscription;
  Quotas? _quotas;
  Map<String, bool> _features = {};
  Map<String, dynamic>? _branding;
  Map<String, dynamic>? _settings;
  OrganizationNode? _activeOrgNode;
  List<OrganizationNode> _accessibleNodes = [];

  // Getters
  String? get activeTenantId => _activeTenantId;
  String? get activeOrgNodeId => _activeOrgNodeId;
  String? get tenantName => _tenantName;
  String? get orgName => _orgName;
  String? get userRole => _userRole;
  String? get scopeType => _scopeType;
  String? get scopeId => _scopeId;
  Map<String, dynamic>? get permissions => _permissions;
  bool get isLoaded => _isLoaded;
  bool get hasMultipleTenants => _multipleTenants;
  List<Map<String, dynamic>> get tenants => _tenants;
  Subscription? get subscription => _subscription;
  Quotas? get quotas => _quotas;
  Map<String, bool> get features => _features;
  Map<String, dynamic>? get branding => _branding;
  Map<String, dynamic>? get settings => _settings;
  OrganizationNode? get activeOrgNode => _activeOrgNode;
  List<OrganizationNode> get accessibleNodes => _accessibleNodes;

  bool get isSubscriptionActive => _subscription?.isActive ?? false;
  bool get isTrial => _subscription?.isTrial ?? false;
  String get planKey => _subscription?.planKey ?? 'free';
  String get planName => _subscription?.plan?.name ?? 'Gratuit';

  /// Initialiser la session depuis les préférences
  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _activeTenantId = prefs.getString('active_tenant_id');
    _activeOrgNodeId = prefs.getString('active_org_node_id');
    _tenantName = prefs.getString('tenant_name');
    _orgName = prefs.getString('org_name');
    _userRole = prefs.getString('user_role');
    _scopeType = prefs.getString('scope_type');
    _scopeId = prefs.getString('scope_id');
    _isLoaded = true;
    if (_activeTenantId == null) {
      await TenantConfig.clearOrgId();
    } else {
      await TenantConfig.setOrgId(_activeTenantId!);
    }
    notifyListeners();
  }

  /// Charger le contexte complet depuis l'API
  Future<void> loadContext(Map<String, dynamic> context) async {
    if (context['requiresSelection'] == true) {
      _multipleTenants = true;
      _tenants = _mapList(context['availableTenants']);
      _clearTenantContext();
      await TenantConfig.clearOrgId();
      final prefs = await SharedPreferences.getInstance();
      await _removePersistedTenantContext(prefs);
      _isLoaded = true;
      notifyListeners();
      return;
    }

    if (context['tenantId'] == null) {
      _clearTenantContext();
      await TenantConfig.clearOrgId();
      final prefs = await SharedPreferences.getInstance();
      await _removePersistedTenantContext(prefs);
      _isLoaded = true;
      notifyListeners();
      return;
    }

    final nextTenantId = context['tenantId'].toString();
    final tenantChanged = _activeTenantId != nextTenantId;
    if (tenantChanged) _clearTenantContext();
    _activeTenantId = nextTenantId;
    _tenantName = context['tenantName']?.toString();
    _userRole = context['role']?.toString();
    _scopeType = context['scopeType']?.toString();
    _scopeId = context['scopeId']?.toString();
    _subscription = null;
    _quotas = null;
    if (context['subscription'] is Map) {
      _subscription = Subscription.fromJson(
        Map<String, dynamic>.from(context['subscription'] as Map),
      );
      _quotas = _subscription!.quotas;
    }

    _features = _booleanMap(context['features']);
    _branding = _nullableMap(context['branding']);
    _settings = _nullableMap(context['settings']);
    _accessibleNodes = context['accessibleNodes'] is List
        ? (context['accessibleNodes'] as List)
            .whereType<Map>()
            .map((node) => OrganizationNode.fromJson(
                  Map<String, dynamic>.from(node),
                ))
            .toList()
        : [];
    _permissions = _permissionMap(context['permissions']);
    _multipleTenants = _mapList(context['availableTenants']).length > 1;

    if (tenantChanged) await TenantConfig.clearOrgId();
    await TenantConfig.setOrgId(nextTenantId);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('active_tenant_id', nextTenantId);
    await prefs.setString('tenant_name', _tenantName ?? '');
    await prefs.setString('user_role', _userRole ?? '');
    await prefs.setString('scope_type', _scopeType ?? 'TENANT');
    await prefs.remove('active_org_node_id');
    await prefs.remove('org_name');
    if (_scopeId == null) {
      await prefs.remove('scope_id');
    } else {
      await prefs.setString('scope_id', _scopeId!);
    }

    _isLoaded = true;
    notifyListeners();
  }

  /// Définir le tenant actif
  Future<void> setActiveTenant(
    String tenantId,
    String tenantName,
    String role, {
    String? scopeType,
    String? scopeId,
  }) async {
    _activeTenantId = tenantId;
    _tenantName = tenantName;
    _userRole = role;
    _activeOrgNodeId = null;
    _orgName = null;
    _scopeType = scopeType ?? 'TENANT';
    _scopeId = scopeId;
    _permissions = null;
    _activeOrgNode = null;
    _subscription = null;
    _quotas = null;
    _features = {};
    _branding = null;
    _settings = null;
    _accessibleNodes = [];
    await TenantConfig.setOrgId(tenantId);

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('active_tenant_id', tenantId);
    await prefs.setString('tenant_name', tenantName);
    await prefs.setString('user_role', role);
    await prefs.setString('scope_type', _scopeType!);
    await prefs.remove('active_org_node_id');
    await prefs.remove('org_name');
    if (scopeId == null) {
      await prefs.remove('scope_id');
    } else {
      await prefs.setString('scope_id', scopeId);
    }

    _multipleTenants = _tenants.length > 1;
    notifyListeners();
  }

  /// Définir le node organisationnel actif
  Future<void> setActiveOrgNode(String orgNodeId, String orgName) async {
    _activeOrgNodeId = orgNodeId;
    _orgName = orgName;

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('active_org_node_id', orgNodeId);
    await prefs.setString('org_name', orgName);

    // Find the node in accessible nodes
    _activeOrgNode = _accessibleNodes.firstWhere(
      (n) => n.id == orgNodeId,
      orElse: () => OrganizationNode(
        id: orgNodeId,
        tenantId: _activeTenantId ?? '',
        type: 'UNKNOWN',
        name: orgName,
        code: '',
        status: 'ACTIVE',
        path: '',
        level: 0,
        createdAt: DateTime.now(),
      ),
    );

    notifyListeners();
  }

  /// Charger la liste des tenants accessibles
  Future<void> loadTenants(List<Map<String, dynamic>> tenants) async {
    _tenants = tenants;
    _multipleTenants = tenants.length > 1;
    notifyListeners();
  }

  /// Changer de tenant
  Future<void> switchTenant(String tenantId) async {
    final tenant = _tenants.firstWhere(
      (t) => t['id'] == tenantId,
      orElse: () => throw Exception('Tenant non trouvé'),
    );

    await setActiveTenant(
      tenant['id'].toString(),
      tenant['name']?.toString() ?? 'Organisation',
      tenant['role']?.toString() ?? 'MEMBRE',
      scopeType: tenant['scopeType']?.toString(),
      scopeId: tenant['scopeId']?.toString(),
    );
  }

  /// Mettre à jour les données de subscription/quotas/features
  void updateSubscriptionData(Subscription subscription) {
    _subscription = subscription;
    _quotas = subscription.quotas;
    notifyListeners();
  }

  void updateFeatures(Map<String, bool> features) {
    _features = features;
    notifyListeners();
  }

  void updateBranding(Map<String, dynamic> branding) {
    _branding = branding;
    notifyListeners();
  }

  void updateSettings(Map<String, dynamic> settings) {
    _settings = settings;
    notifyListeners();
  }

  /// Effacer la session (logout)
  Future<void> clear() async {
    _activeTenantId = null;
    _activeOrgNodeId = null;
    _tenantName = null;
    _orgName = null;
    _userRole = null;
    _scopeType = null;
    _scopeId = null;
    _permissions = null;
    _isLoaded = false;
    _multipleTenants = false;
    _tenants = [];
    _subscription = null;
    _quotas = null;
    _features = {};
    _branding = null;
    _settings = null;
    _activeOrgNode = null;
    _accessibleNodes = [];
    TenantConfig.clearOrgId();

    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('active_tenant_id');
    await prefs.remove('active_org_node_id');
    await prefs.remove('tenant_name');
    await prefs.remove('org_name');
    await prefs.remove('user_role');
    await prefs.remove('scope_type');
    await prefs.remove('scope_id');

    notifyListeners();
  }

  /// Vérifier si l'utilisateur a une permission
  bool hasPermission(String permission) {
    if (_permissions == null) return false;
    return _permissions![permission] == true;
  }

  /// Vérifier si l'utilisateur a un rôle
  bool hasRole(String role) {
    if (_userRole == null) return false;
    return _userRole!.toUpperCase() == role.toUpperCase();
  }

  /// Vérifier si une fonctionnalité est activée
  bool hasFeature(String feature) {
    return _features[feature] == true;
  }

  void _clearTenantContext() {
    _activeTenantId = null;
    _activeOrgNodeId = null;
    _tenantName = null;
    _orgName = null;
    _userRole = null;
    _scopeType = null;
    _scopeId = null;
    _permissions = null;
    _subscription = null;
    _quotas = null;
    _features = {};
    _branding = null;
    _settings = null;
    _activeOrgNode = null;
    _accessibleNodes = [];
  }

  Future<void> _removePersistedTenantContext(SharedPreferences prefs) async {
    await prefs.remove('active_tenant_id');
    await prefs.remove('active_org_node_id');
    await prefs.remove('tenant_name');
    await prefs.remove('org_name');
    await prefs.remove('user_role');
    await prefs.remove('scope_type');
    await prefs.remove('scope_id');
  }

  static List<Map<String, dynamic>> _mapList(dynamic value) {
    if (value is! List) return [];
    return value
        .whereType<Map>()
        .map((item) => Map<String, dynamic>.from(item))
        .toList();
  }

  static Map<String, bool> _booleanMap(dynamic value) {
    final map = _nullableMap(value);
    if (map == null) return {};
    return {
      for (final entry in map.entries)
        if (entry.value is bool) entry.key: entry.value as bool,
    };
  }

  static Map<String, dynamic>? _nullableMap(dynamic value) {
    if (value is Map) return Map<String, dynamic>.from(value);
    if (value is String) {
      try {
        final decoded = jsonDecode(value);
        if (decoded is Map) return Map<String, dynamic>.from(decoded);
      } catch (_) {
        return null;
      }
    }
    return null;
  }

  static Map<String, dynamic>? _permissionMap(dynamic value) {
    if (value is Map) {
      return {
        for (final entry in value.entries) entry.key.toString(): entry.value,
      };
    }
    if (value is Iterable) {
      return {
        for (final permission in value)
          if (permission != null) permission.toString(): true,
      };
    }
    return null;
  }

  /// Vérifier si un quota est dépassé
  bool isQuotaExceeded(String quotaKey) {
    return _quotas?.isExceeded(quotaKey) ?? false;
  }

  /// Obtenir le pourcentage d'utilisation d'un quota
  double? getQuotaUsage(String quotaKey) {
    return _quotas?.usagePercent(quotaKey);
  }

  /// Permissions de l'utilisateur dans le tenant courant
  void setPermissions(Map<String, dynamic> permissions) {
    _permissions = _permissionMap(permissions);
    notifyListeners();
  }
}

/// Provider pour accéder à la session
final tenantSessionProvider = ChangeNotifierProvider<TenantSession>((ref) {
  return TenantSession();
});

/// Extension pour utiliser le tenant session dans les widgets
extension TenantSessionX on BuildContext {
  TenantSession get tenantSession =>
      ProviderScope.containerOf(this, listen: false)
          .read(tenantSessionProvider);
}
