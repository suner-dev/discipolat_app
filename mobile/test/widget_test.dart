import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/main.dart';

void main() {
  testWidgets('DiscipolatApp renders the login screen', (WidgetTester tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: DiscipolatApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Discipolat'), findsOneWidget);
    expect(find.text('Se connecter'), findsOneWidget);
  });
}
