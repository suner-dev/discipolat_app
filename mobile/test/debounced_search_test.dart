import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/presentation/widgets/debounced_search.dart';

void main() {
  testWidgets('DebouncedSearchField shows hint text', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: DebouncedSearchField(
          hintText: 'Rechercher...',
          onDebounced: (_) {},
        ),
      ),
    ));

    expect(find.text('Rechercher...'), findsOneWidget);
  });

  testWidgets('DebouncedSearchField shows search icon', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: DebouncedSearchField(
          hintText: 'Search...',
          onDebounced: (_) {},
        ),
      ),
    ));

    expect(find.byIcon(Icons.search), findsOneWidget);
  });

  testWidgets('DebouncedSearchField debounce delays callback', (tester) async {
    int callCount = 0;
    String lastValue = '';

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: DebouncedSearchField(
          hintText: 'Search...',
          onDebounced: (v) {
            callCount++;
            lastValue = v;
          },
        ),
      ),
    ));

    // Type characters
    await tester.enterText(find.byType(TextField), 'test');
    await tester.pump(const Duration(milliseconds: 100));

    // Should not have triggered yet
    expect(callCount, 0);

    // Wait for debounce (400ms default)
    await tester.pump(const Duration(milliseconds: 500));

    expect(callCount, 1);
    expect(lastValue, 'test');
  });

  testWidgets('DebouncedSearchField only fires once for rapid typing', (tester) async {
    int callCount = 0;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: DebouncedSearchField(
          hintText: 'Search...',
          onDebounced: (_) => callCount++,
        ),
      ),
    ));

    // Rapid typing
    await tester.enterText(find.byType(TextField), 'a');
    await tester.pump(const Duration(milliseconds: 100));
    await tester.enterText(find.byType(TextField), 'ab');
    await tester.pump(const Duration(milliseconds: 100));
    await tester.enterText(find.byType(TextField), 'abc');
    await tester.pump(const Duration(milliseconds: 100));

    // Wait for debounce
    await tester.pump(const Duration(milliseconds: 500));

    // Only the last value should have triggered
    expect(callCount, 1);
  });

  testWidgets('DebouncedSearchField uses custom icon', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: DebouncedSearchField(
          hintText: 'Search...',
          onDebounced: (_) {},
          icon: Icons.filter_list,
        ),
      ),
    ));

    expect(find.byIcon(Icons.filter_list), findsOneWidget);
  });

  testWidgets('DebouncedSearchField uses external controller', (tester) async {
    final controller = TextEditingController();

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: DebouncedSearchField(
          hintText: 'Search...',
          onDebounced: (_) {},
          controller: controller,
        ),
      ),
    ));

    controller.text = 'hello';
    await tester.pump();

    expect(find.text('hello'), findsOneWidget);
    controller.dispose();
  });
}
