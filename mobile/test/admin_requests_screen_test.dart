import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/admin_requests/admin_requests_screen.dart';

void main() {
  group('AdminRequestsScreen', () {
    testWidgets('renders app bar with title', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AdminRequestsScreen()));
      expect(find.text('📋 Demandes administratives'), findsOneWidget);
    });

    testWidgets('shows request types', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AdminRequestsScreen()));
      expect(find.text('Baptême'), findsOneWidget);
      expect(find.text('Dédicace'), findsOneWidget);
      expect(find.text('Accueil nouveau'), findsOneWidget);
    });

    testWidgets('shows my requests', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AdminRequestsScreen()));
      expect(find.text('Mes demandes'), findsOneWidget);
      expect(find.text('Approuvée'), findsOneWidget);
      expect(find.text('En examen'), findsOneWidget);
    });

    testWidgets('FAB opens new request sheet', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AdminRequestsScreen()));
      await tester.tap(find.byIcon(Icons.add));
      await tester.pumpAndSettle();
      expect(find.text('Nouvelle demande'), findsOneWidget);
    });
  });
}
