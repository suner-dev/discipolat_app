import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

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
      currentPeriodStart: DateTime.tryParse(json['currentPeriodStart'] ?? '') ?? DateTime.now(),
      currentPeriodEnd: DateTime.tryParse(json['currentPeriodEnd'] ?? '') ?? DateTime.now(),
      cancelAtPeriodEnd: json['cancelAtPeriodEnd'] ?? false,
      canceledAt: json['canceledAt'] != null ? DateTime.tryParse(json['canceledAt']) : null,
      trialEndsAt: json['trialEndsAt'] != null ? DateTime.tryParse(json['trialEndsAt']) : null,
      quotas: Quotas.fromJson(json['quotas'] ?? {}),
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
    return Plan(
      id: json['id'] ?? '',
      key: json['key'] ?? '',
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      priceMonthly: json['priceMonthly'] ?? 0,
      priceYearly: json['priceYearly'] ?? 0,
      currency: json['currency'] ?? 'XAF',
      features: Map<String, bool>.from(json['features'] ?? {}),
    );
  }
}

/// Modèle pour les quotas
class Quotas {
  final int maxUsers;
  final int maxChurches;
  final int maxDepartments;
  final int maxStorageMb;
  final int maxAiRequestsMonth;
  final int maxCourses;
  final int maxMessagesMonth;
  final int currentUsers;
  final int currentChurches;
  final int currentDepartments;
  final int currentStorageMb;
  final int currentAiRequestsMonth;
  final int currentCourses;
  final int currentMessagesMonth;

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

  factory Quotas.fromJson(Map<String, dynamic> json) {
    return Quotas(
      maxUsers: json['maxUsers'] ?? 50,
      maxChurches: json['maxChurches'] ?? 1,
      maxDepartments: json['maxDepartments'] ?? 10,
      maxStorageMb: json['maxStorageMb'] ?? 100,
      maxAiRequestsMonth: json['maxAiRequestsMonth'] ?? 100,
      maxCourses: json['maxCourses'] ?? 5,
      maxMessagesMonth: json['maxMessagesMonth'] ?? 1000,
      currentUsers: json['currentUsers'] ?? 0,
      currentChurches: json['currentChurches'] ?? 0,
      currentDepartments: json['currentDepartments'] ?? 0,
      currentStorageMb: json['currentStorageMb'] ?? 0,
      currentAiRequestsMonth: json['currentAiRequestsMonth'] ?? 0,
      currentCourses: json['currentCourses'] ?? 0,
      currentMessagesMonth: json['currentMessagesMonth'] ?? 0,
    );
  }

  bool isExceeded(String quotaKey) {
    switch (quotaKey) {
      case 'maxUsers': return currentUsers >= maxUsers;
      case 'maxChurches': return currentChurches >= maxChurches;
      case 'maxDepartments': return currentDepartments >= maxDepartments;
      case 'maxStorageMb': return currentStorageMb >= maxStorageMb;
      case 'maxAiRequestsMonth': return currentAiRequestsMonth >= maxAiRequestsMonth;
      case 'maxCourses': return currentCourses >= maxCourses;
      case 'maxMessagesMonth': return currentMessagesMonth >= maxMessagesMonth;
      default: return false;
    }
  }

  double usagePercent(String quotaKey) {
    switch (quotaKey) {
      case 'maxUsers': return maxUsers > 0 ? currentUsers / maxUsers : 0;
      case 'maxChurches': return maxChurches > 0 ? currentChurches / maxChurches : 0;
      case 'maxDepartments': return maxDepartments > 0 ? currentDepartments / maxDepartments : 0;
      case 'maxStorageMb': return maxStorageMb > 0 ? currentStorageMb / maxStorageMb : 0;
      case 'maxAiRequestsMonth': return maxAiRequestsMonth > 0 ? currentAiRequestsMonth / maxAiRequestsMonth : 0;
      case 'maxCourses': return maxCourses > 0 ? currentCourses / maxCourses : 0;
      case 'maxMessagesMonth': return maxMessagesMonth > 0 ? currentMessagesMonth / maxMessagesMonth : 0;
      default: return 0;
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
      metadata: json['metadata'] != null ? Map<String, dynamic>.from(json['metadata']) : null,
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
    notifyListeners();
  }

  /// Charger le contexte complet depuis l'API
  Future<void> loadContext(Map<String, dynamic> context) async {
    if (context['requiresSelection'] == true) {
      _multipleTenants = true;
      _tenants = List<Map<String, dynamic>>.from(context['availableTenants'] ?? []);
      _activeTenantId = null;
      _activeOrgNodeId = null;
      _subscription = null;
      _quotas = null;
      _features = {};
      _branding = null;
      _settings = null;
      _accessibleNodes = [];
      _activeOrgNode = null;
    } else if (context['tenantId'] != null) {
      _activeTenantId = context['tenantId'];
      _tenantName = context['tenantName'];
      _userRole = context['role'];
      _scopeType = context['scopeType'];
      _scopeId = context['scopeId'];

      // Subscription & quotas
      if (context['subscription'] != null) {
        _subscription = Subscription.fromJson(context['subscription']);
        _quotas = _subscription!.quotas;
      }

      // Features & branding
      _features = Map<String, bool>.from(context['features'] ?? {});
      _branding = context['branding'] != null ? Map<String, dynamic>.from(context['branding']) : null;
      _settings = context['settings'] != null ? Map<String, dynamic>.from(context['settings']) : null;

      // Accessible nodes
      if (context['accessibleNodes'] != null) {
        _accessibleNodes = (context['accessibleNodes'] as List)
            .map((n) => OrganizationNode.fromJson(n))
            .toList();
      }

      // Permissions
      if (context['permissions'] != null) {
        _permissions = Map<String, dynamic>.from(context['permissions']);
      }

      // Save to prefs
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('active_tenant_id', _activeTenantId!);
      await prefs.setString('tenant_name', _tenantName ?? '');
      await prefs.setString('user_role', _userRole ?? '');
      await prefs.setString('scope_type', _scopeType ?? 'TENANT');
      if (_scopeId != null) {
        await prefs.setString('scope_id', _scopeId!);
      }

      _multipleTenants = (context['availableTenants'] as List?)?.length ?? 0 > 1;
    }
    _isLoaded = true;
    notifyListeners();
  }

  /// Définir le tenant actif
  Future<void> setActiveTenant(String tenantId, String tenantName, String role) async {
    _activeTenantId = tenantId;
    _tenantName = tenantName;
    _userRole = role;
    _activeOrgNodeId = null;
    _orgName = null;
    _scopeType = 'TENANT';
    _scopeId = null;
    _activeOrgNode = null;

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('active_tenant_id', tenantId);
    await prefs.setString('tenant_name', tenantName);
    await prefs.setString('user_role', role);
    await prefs.setString('scope_type', 'TENANT');
    await prefs.remove('active_org_node_id');
    await prefs.remove('org_name');
    await prefs.remove('scope_id');

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
      tenant['id'],
      tenant['name'],
      tenant['role'],
    );

    notifyListeners();
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

  /// Vérifier si un quota est dépassé
  bool isQuotaExceeded(String quotaKey) {
    return _quotas?.isExceeded(quotaKey) ?? false;
  }

  /// Obtenir le pourcentage d'utilisation d'un quota
  double getQuotaUsage(String quotaKey) {
    return _quotas?.usagePercent(quotaKey) ?? 0.0;
  }

  /// Permissions de l'utilisateur dans le tenant courant
  void setPermissions(Map<String, dynamic> permissions) {
    _permissions = permissions;
    notifyListeners();
  }
}

/// Provider pour accéder à la session
final tenantSessionProvider = ChangeNotifierProvider<TenantSession>((ref) {
  return TenantSession();
});

/// Extension pour utiliser le tenant session dans les widgets
extension TenantSessionX on BuildContext {
  TenantSession get tenantSession => read(tenantSessionProvider);
}