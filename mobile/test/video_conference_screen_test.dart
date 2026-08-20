import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/presentation/screens/messages/video_conference_screen.dart';
import 'helpers/fake_api_service.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  testWidgets('VideoConferenceScreen renders title', (tester) async {
    await tester.pumpWidget(wrap(VideoConferenceScreen(apiService: FakeApiService())));
    expect(find.text('📹 Visioconférence'), findsOneWidget);
  });

  testWidgets('VideoConferenceScreen shows start meeting', (tester) async {
    await tester.pumpWidget(wrap(VideoConferenceScreen(apiService: FakeApiService())));
    await tester.pumpAndSettle(const Duration(seconds: 2));
    expect(find.text('Démarrer une réunion'), findsOneWidget);
    expect(find.text('Démarrer maintenant'), findsOneWidget);
  });

  testWidgets('VideoConferenceScreen shows quick join', (tester) async {
    await tester.pumpWidget(wrap(VideoConferenceScreen(apiService: FakeApiService())));
    await tester.pumpAndSettle(const Duration(seconds: 2));
    expect(find.text('Rejoindre rapidement'), findsOneWidget);
  });

  testWidgets('VideoConferenceScreen shows scheduled meetings section', (tester) async {
    await tester.pumpWidget(wrap(VideoConferenceScreen(apiService: FakeApiService())));
    await tester.pumpAndSettle(const Duration(seconds: 2));
    expect(find.text('Réunions à venir'), findsOneWidget);
    expect(find.text('Aucune réunion planifiée'), findsOneWidget);
  });
}
