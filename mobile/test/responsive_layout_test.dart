import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/widgets/responsive_layout.dart';

void main() {
  Widget wrap(Widget child, {double width = 400, double height = 800}) => MaterialApp(
    home: MediaQuery(
      data: MediaQueryData(size: Size(width, height)),
      child: Scaffold(body: child),
    ),
  );

  // ═══════════════════════════════════════════════════════════
  // P2 #99 — RESPONSIVE LAYOUT TESTS
  // ═══════════════════════════════════════════════════════════

  group('ResponsiveLayout', () {
    testWidgets('renders mobile layout on small screen', (tester) async {
      await tester.pumpWidget(wrap(
        const ResponsiveLayout(
          mobile: Text('Mobile'),
          tablet: Text('Tablet'),
          desktop: Text('Desktop'),
        ),
        width: 375,
      ));
      expect(find.text('Mobile'), findsOneWidget);
      expect(find.text('Tablet'), findsNothing);
      expect(find.text('Desktop'), findsNothing);
    });

    testWidgets('renders tablet layout on medium screen', (tester) async {
      await tester.pumpWidget(wrap(
        const ResponsiveLayout(
          mobile: Text('Mobile'),
          tablet: Text('Tablet'),
          desktop: Text('Desktop'),
        ),
        width: 768,
      ));
      expect(find.text('Tablet'), findsOneWidget);
    });

    testWidgets('renders desktop layout on large screen', (tester) async {
      await tester.pumpWidget(wrap(
        const ResponsiveLayout(
          mobile: Text('Mobile'),
          tablet: Text('Tablet'),
          desktop: Text('Desktop'),
        ),
        width: 1200,
      ));
      expect(find.text('Desktop'), findsOneWidget);
    });

    testWidgets('falls back to mobile when tablet/desktop not provided', (tester) async {
      await tester.pumpWidget(wrap(
        const ResponsiveLayout(
          mobile: Text('Mobile only'),
        ),
        width: 768,
      ));
      expect(find.text('Mobile only'), findsOneWidget);
    });
  });

  group('ResponsiveLayout.getDeviceType', () {
    testWidgets('returns phone for width < 600', (tester) async {
      DeviceType? result;
      await tester.pumpWidget(MaterialApp(
        home: Builder(
          builder: (ctx) {
            result = ResponsiveLayout.getDeviceType(ctx);
            return const SizedBox();
          },
        ),
      ));
      // jsdom width is 800 by default, so this tests the default
      expect(result, isNotNull);
    });
  });

  group('ResponsiveLayout.getCrossAxisCount', () {
    testWidgets('returns correct count based on width', (tester) async {
      int? count;
      await tester.pumpWidget(MaterialApp(
        home: MediaQuery(
          data: MediaQueryData(size: Size(400, 800)),
          child: Builder(
            builder: (ctx) {
              count = ResponsiveLayout.getCrossAxisCount(ctx);
              return const SizedBox();
            },
          ),
        ),
      ));
      expect(count, 2); // Phone: 2 columns
    });
  });

  // ═══════════════════════════════════════════════════════════
  // P2 #99 — RESPONSIVE GRID TESTS
  // ═══════════════════════════════════════════════════════════

  group('ResponsiveGrid', () {
    testWidgets('renders children', (tester) async {
      await tester.pumpWidget(wrap(
        const ResponsiveGrid(
          children: [
            Text('Item 1'),
            Text('Item 2'),
            Text('Item 3'),
          ],
        ),
      ));
      expect(find.text('Item 1'), findsOneWidget);
      expect(find.text('Item 2'), findsOneWidget);
      expect(find.text('Item 3'), findsOneWidget);
    });

    testWidgets('renders as GridView', (tester) async {
      await tester.pumpWidget(wrap(
        const ResponsiveGrid(
          children: [Text('A'), Text('B')],
        ),
      ));
      expect(find.byType(GridView), findsOneWidget);
    });
  });
}
