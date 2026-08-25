import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/group_message_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/models/group_message.dart';
import 'package:discipolat_mobile/models/group_thread.dart';
import 'package:discipolat_mobile/presentation/screens/group_messages/group_messages_screen.dart';

/// Service factice : renvoie des groupes et messages en mémoire.
class _FakeGroupMessageService implements GroupMessageService {
  @override
  Future<List<GroupThread>> fetchGroups() async => [
        GroupThread(id: '1', nom: 'Département Louange', groupType: 'DEPARTMENT'),
        GroupThread(id: '2', nom: 'Famille Grâce', groupType: 'FAMILY'),
        GroupThread(id: '3', nom: 'Équipe Technique', groupType: 'TEAM'),
      ];

  @override
  Future<List<GroupMessage>> fetchMessages(String groupId) async => const [];

  @override
  Future<GroupMessage> sendMessage({
    required String groupId,
    required String senderId,
    required String groupType,
    required String content,
  }) =>
      throw UnimplementedError();

  @override
  Future<List<GroupMessage>> search(String groupId, String q) async => const [];

  @override
  Future<GroupMessage> react(String messageId) async => throw UnimplementedError();
}

void main() {
  group('GroupMessagesScreen', () {
    Widget wrap() => ProviderScope(
          overrides: [
            groupMessageServiceProvider.overrideWithValue(_FakeGroupMessageService()),
          ],
          child: const MaterialApp(home: GroupMessagesScreen()),
        );

    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(wrap());
      expect(find.text('Messagerie Groupe'), findsOneWidget);
    });

    testWidgets('shows my groups from API', (tester) async {
      await tester.pumpWidget(wrap());
      await tester.pumpAndSettle();
      expect(find.text('Département Louange'), findsOneWidget);
      expect(find.text('Famille Grâce'), findsOneWidget);
      expect(find.text('Équipe Technique'), findsOneWidget);
    });

    testWidgets('opens a conversation when tapping a group', (tester) async {
      await tester.pumpWidget(wrap());
      await tester.pumpAndSettle();
      await tester.tap(find.text('Département Louange'));
      await tester.pumpAndSettle();
      // La barre de recherche du groupe apparaît dans l'appBar.
      expect(find.text('Rechercher dans le groupe...'), findsOneWidget);
    });
  });
}
