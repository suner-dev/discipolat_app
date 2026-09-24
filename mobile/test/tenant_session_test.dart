import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:discipolat_mobile/core/tenant_session.dart';
import 'package:discipolat_mobile/tenant_config.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  tearDown(() async {
    await TenantConfig.clearOrgId();
  });

  test('tenant switch clears context from the previous tenant', () async {
    SharedPreferences.setMockInitialValues({});
    final session = TenantSession();
    await session.loadTenants([
      {
        'id': 'tenant-a',
        'name': 'Église A',
        'role': 'ADMIN',
        'scopeType': 'TENANT',
      },
      {
        'id': 'tenant-b',
        'name': 'Église B',
        'role': 'RESPONSABLE',
        'scopeType': 'DEPARTMENT',
        'scopeId': 'department-7',
      },
    ]);
    await session.loadContext({
      'tenantId': 'tenant-a',
      'tenantName': 'Église A',
      'role': 'ADMIN',
      'scopeType': 'TENANT',
      'scopeId': null,
      'permissions': ['dashboard.read'],
      'features': {'analytics': true},
      'settings': {'analyticsEnabled': true},
      'branding': {'primaryColor': '#123456'},
      'accessibleNodes': [
        {'id': 'node-a', 'tenantId': 'tenant-a', 'name': 'Noeud A'},
      ],
    });

    expect(session.hasPermission('dashboard.read'), isTrue);
    expect(session.features['analytics'], isTrue);
    expect(session.accessibleNodes, hasLength(1));

    await session.switchTenant('tenant-b');

    expect(session.activeTenantId, 'tenant-b');
    expect(session.tenantName, 'Église B');
    expect(session.userRole, 'RESPONSABLE');
    expect(session.scopeType, 'DEPARTMENT');
    expect(session.scopeId, 'department-7');
    expect(session.permissions, isNull);
    expect(session.features, isEmpty);
    expect(session.branding, isNull);
    expect(session.settings, isNull);
    expect(session.accessibleNodes, isEmpty);
    expect(session.activeOrgNode, isNull);
    expect(TenantConfig.currentOrgId, 'tenant-b');
    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getString('active_tenant_id'), 'tenant-b');
    expect(prefs.getString('scope_type'), 'DEPARTMENT');
    expect(prefs.getString('scope_id'), 'department-7');
  });

  test('selection-required context removes persisted tenant data', () async {
    SharedPreferences.setMockInitialValues({});
    final session = TenantSession();
    await session.loadContext({
      'tenantId': 'tenant-a',
      'tenantName': 'Église A',
      'role': 'ADMIN',
      'scopeType': 'DEPARTMENT',
      'scopeId': 'department-7',
      'permissions': ['dashboard.read'],
    });

    await session.loadContext({
      'requiresSelection': true,
      'availableTenants': [
        {'id': 'tenant-a', 'name': 'Église A', 'role': 'ADMIN'},
        {'id': 'tenant-b', 'name': 'Église B', 'role': 'MEMBRE'},
      ],
    });

    expect(session.hasMultipleTenants, isTrue);
    expect(session.tenants, hasLength(2));
    expect(session.activeTenantId, isNull);
    expect(session.tenantName, isNull);
    expect(session.userRole, isNull);
    expect(session.scopeType, isNull);
    expect(session.scopeId, isNull);
    expect(session.permissions, isNull);
    expect(TenantConfig.currentOrgId, isNull);
    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getString('active_tenant_id'), isNull);
    expect(prefs.getString('scope_type'), isNull);
    expect(prefs.getString('scope_id'), isNull);
  });

  test('quota context accepts the backend JSON limits contract', () {
    final quotas = Quotas.fromJson(
      '{"max_users":20,"max_storage_mb":100,"ai_credits":50}',
    );

    expect(quotas.maxUsers, 20);
    expect(quotas.maxStorageMb, 100);
    expect(quotas.maxAiRequestsMonth, 50);
    expect(quotas.usagePercent('maxUsers'), isNull);
  });
}
