import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/presentation/screens/trainings/sermon_transcription_screen.dart';
import 'helpers/fake_api_service.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  testWidgets('SermonTranscriptionScreen renders title', (tester) async {
    await tester.pumpWidget(wrap(SermonTranscriptionScreen(apiService: FakeApiService())));
    expect(find.text('🎙️ Transcriptions'), findsOneWidget);
  });

  testWidgets('SermonTranscriptionScreen has search bar', (tester) async {
    await tester.pumpWidget(wrap(SermonTranscriptionScreen(apiService: FakeApiService())));
    expect(find.byIcon(Icons.search), findsOneWidget);
  });

  testWidgets('SermonTranscriptionScreen loads data and shows empty state', (tester) async {
    final fakeApi = FakeApiService();
    await tester.pumpWidget(wrap(SermonTranscriptionScreen(apiService: fakeApi)));
    await tester.pumpAndSettle(const Duration(seconds: 3));

    expect(find.text('Aucune transcription'), findsOneWidget);
    expect(fakeApi.getPaths, isNotEmpty);
  });

  testWidgets('SermonTranscriptionScreen has refresh button', (tester) async {
    await tester.pumpWidget(wrap(SermonTranscriptionScreen(apiService: FakeApiService())));
    expect(find.byIcon(Icons.refresh), findsOneWidget);
  });
}
