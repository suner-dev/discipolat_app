import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Provider pour la session multi-tenant
class TenantSession extends ChangeNotifier {
  String? _activeTenantId;
  String? _activeOrgNodeId;
  String? _tenantName;
  String? _orgName;
  String? _userRole;
  Map<String, dynamic>? _permissions;
  bool _isLoaded = false;

  String? get activeTenantId => _activeTenantId;
  String? get activeOrgNodeId => _activeOrgNodeId;
  String? get tenantName => _tenantName;
  String? get orgName => _orgName;
  String? get userRole => _userRole;
  Map<String, dynamic>? get permissions => _permissions;
  bool get isLoaded => _isLoaded;
  bool get hasMultipleTenants => _multipleTenants;

  bool _multipleTenants = false;
  List<Map<String, dynamic>> _tenants = [];

  List<Map<String, dynamic>> get tenants => _tenants;

  /// Initialiser la session depuis les préférences
  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _activeTenantId = prefs.getString('active_tenant_id');
    _activeOrgNodeId = prefs.getString('active_org_node_id');
    _tenantName = prefs.getString('tenant_name');
    _orgName = prefs.getString('org_name');
    _userRole = prefs.getString('user_role');
    _isLoaded = true;
    notifyListeners();
  }

  /// Définir le tenant actif
  Future<void> setActiveTenant(String tenantId, String tenantName, String role) async {
    _activeTenantId = tenantId;
    _tenantName = tenantName;
    _userRole = role;
    
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('active_tenant_id', tenantId);
    await prefs.setString('tenant_name', tenantName);
    await prefs.setString('user_role', role);
    
    // Vérifier si l'utilisateur a plusieurs tenants
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
    
    // Réinitialiser le node organisationnel
    _activeOrgNodeId = null;
    _orgName = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('active_org_node_id');
    await prefs.remove('org_name');
    
    notifyListeners();
  }

  /// Effacer la session (logout)
  Future<void> clear() async {
    _activeTenantId = null;
    _activeOrgNodeId = null;
    _tenantName = null;
    _orgName = null;
    _userRole = null;
    _permissions = null;
    _isLoaded = false;
    _tenants = [];
    
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('active_tenant_id');
    await prefs.remove('active_org_node_id');
    await prefs.remove('tenant_name');
    await prefs.remove('org_name');
    await prefs.remove('user_role');
    
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
