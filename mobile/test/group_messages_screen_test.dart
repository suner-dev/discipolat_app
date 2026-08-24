import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/group_messages/group_messages_screen.dart';

void main() {
  group('GroupMessagesScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const GroupMessagesScreen()));
      expect(find.text('💬 Messagerie Groupe'), findsOneWidget);
    });

    testWidgets('shows my groups', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const GroupMessagesScreen()));
      expect(find.text('Mes groupes'), findsOneWidget);
      expect(find.text('Département Louange'), findsOneWidget);
      expect(find.text('Famille Grâce'), findsOneWidget);
      expect(find.text('Équipe Technique'), findsOneWidget);
    });

    testWidgets('shows suggested groups', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const GroupMessagesScreen()));
      expect(find.text('Groupes suggérés'), findsOneWidget);
      expect(find.text('Département Accueil'), findsOneWidget);
    });

    testWidgets('shows unread badges', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const GroupMessagesScreen()));
      // Should have unread count badges
      expect(find.text('2'), findsWidgets);
    });
  });
}
