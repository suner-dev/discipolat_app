import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/presentation/widgets/app_drawer.dart';

void main() {
  tearDown(AuthState().logout);

  test('tenant membership roles are not exposed as auth active roles', () {
    AuthState().setAuthenticated(true, userData: {
      'userId': 'tenant-user',
      'email': 'admin@tenant.test',
      'role': 'TENANT_ADMIN',
      'roles': ['TENANT_ADMIN', 'MEMBRE'],
      'activeRole': 'TENANT_ADMIN',
    });

    expect(AuthState().isAuthenticated, isFalse);
    expect(AuthState().activeRole, isEmpty);
    expect(AuthState().roles, isEmpty);
    expect(roleHome('TENANT_ADMIN'), '/dashboard');
  });

  test('contract active roles remain available', () {
    AuthState().setAuthenticated(true, userData: {
      'userId': 'user-1',
      'email': 'admin@test',
      'role': 'PASTEUR',
      'roles': ['PASTEUR'],
      'activeRole': 'PASTEUR',
    });

    expect(AuthState().activeRole, 'PASTEUR');
    expect(AuthState().roles, ['PASTEUR']);
    expect(roleHome('PASTEUR'), '/dashboard');
  });

  testWidgets('pasteur role exposes the authenticated tenant dashboard', (
    tester,
  ) async {
    AuthState().setAuthenticated(true, userData: {
      'userId': 'tenant-owner',
      'email': 'owner@tenant.test',
      'role': 'PASTEUR',
      'roles': ['PASTEUR'],
      'activeRole': 'PASTEUR',
    });
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          home: Scaffold(drawer: const AppDrawer(), body: const SizedBox()),
        ),
      ),
    );
    tester.state<ScaffoldState>(find.byType(Scaffold)).openDrawer();
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Dashboard tenant'), 240,
        scrollable: find.byType(Scrollable).first);

    expect(find.text('Dashboard tenant'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Paramètres église'), 240,
        scrollable: find.byType(Scrollable).first);
    expect(find.text('Paramètres église'), findsOneWidget);
    expect(find.text('Modules'), findsNothing);
    expect(find.text('Utilisateurs'), findsNothing);
    expect(find.text('Organisations'), findsNothing);
  });

  testWidgets('unsupported tenant roles expose no legacy drawer routes', (
    tester,
  ) async {
    AuthState().setAuthenticated(true, userData: {
      'userId': 'tenant-user',
      'email': 'admin@tenant.test',
      'role': 'TENANT_ADMIN',
      'roles': ['TENANT_ADMIN'],
      'activeRole': 'TENANT_ADMIN',
    });
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          home: Scaffold(drawer: const AppDrawer(), body: const SizedBox()),
        ),
      ),
    );
    tester.state<ScaffoldState>(find.byType(Scaffold)).openDrawer();
    await tester.pumpAndSettle();

    expect(find.text('Paramètres église'), findsNothing);
    expect(find.text('Modules'), findsNothing);
    expect(find.text('Utilisateurs'), findsNothing);
    expect(find.text('Organisations'), findsNothing);
  });
}
