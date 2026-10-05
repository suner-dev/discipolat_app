import 'package:discipolat/data/models/navigation_group.dart';
import 'package:flutter_test/flutter_test.dart';

/// LOT 2 §GR — modèle de groupes de navigation (parité web).
///
/// Ces tests verrouillent la garantie centrale du module : **rien ne disparaît**.
/// Une entrée sans groupe affecté n'est jamais perdue — c'est ce qui permet à
/// une église de réorganiser son menu sans jamais casser une fonctionnalité.
void main() {
  NavigationGroup group({
    required String id,
    required String label,
    String? tenantId,
    int displayOrder = 0,
    bool collapsedByDefault = true,
    bool showCount = false,
  }) =>
      NavigationGroup(
        id: id,
        label: label,
        key: id,
        tenantId: tenantId,
        displayOrder: displayOrder,
        collapsedByDefault: collapsedByDefault,
        showCount: showCount,
      );

  NavigationShape shapeOf(List<NavigationGroup> groups,
          {Map<String, List<String>> assignments = const {}}) =>
      NavigationShape(groups: groups, assignments: assignments);

  group('NavigationGroup.fromJson', () {
    test('lit les champs fournis par le backend', () {
      final parsed = NavigationGroup.fromJson({
        'id': 'g1',
        'key': 'zones',
        'label': 'Zones',
        'displayOrder': 2,
        'collapsedByDefault': false,
        'showCount': true,
      });

      expect(parsed.id, 'g1');
      expect(parsed.label, 'Zones');
      expect(parsed.displayOrder, 2);
      expect(parsed.collapsedByDefault, isFalse);
      expect(parsed.showCount, isTrue);
    });

    test('tolère une réponse partielle sans planter', () {
      final parsed = NavigationGroup.fromJson({'id': 'g2'});

      expect(parsed.label, '');
      expect(parsed.displayOrder, 0);
      // Repli sûr : replié par défaut = moins de bruit visuel.
      expect(parsed.collapsedByDefault, isTrue);
      expect(parsed.tenantId, isNull);
    });

    test('distingue un groupe global d’un groupe d’église', () {
      expect(NavigationGroup.fromJson({'id': 'a', 'tenantId': null}).isGlobal, isTrue);
      expect(
        NavigationGroup.fromJson({'id': 'a', 'tenantId': 't1'}).isGlobal,
        isFalse,
      );
    });
  });

  group('NavigationShape.groupOf', () {
    test('retrouve le groupe affecté à une route', () {
      final shape = shapeOf(
        [group(id: 'g1', label: 'Zones')],
        assignments: {
          '/zones': ['g1'],
        },
      );

      expect(shape.groupOf('/zones')?.label, 'Zones');
    });

    test('renvoie null pour une route non regroupée : elle reste visible', () {
      final shape = shapeOf(
        [group(id: 'g1', label: 'Zones')],
        assignments: {
          '/zones': ['g1'],
        },
      );

      expect(shape.groupOf('/dashboard'), isNull);
    });

    test('renvoie null quand une affectation pointe vers un groupe absent', () {
      // Le groupe a été supprimé côté serveur : l'entrée ne doit pas disparaître.
      final shape = shapeOf(
        [group(id: 'g1', label: 'Zones')],
        assignments: {
          '/zones': ['groupe-supprime'],
        },
      );

      expect(shape.groupOf('/zones'), isNull);
    });

    test('renvoie null sur une affectation vide', () {
      final shape = shapeOf([group(id: 'g1', label: 'Zones')], assignments: {'/zones': []});
      expect(shape.groupOf('/zones'), isNull);
    });
  });

  group('NavigationShape.empty', () {
    test('est le repli tant que l’API n’a pas répondu', () {
      expect(NavigationShape.empty.isEmpty, isTrue);
      expect(NavigationShape.empty.groups, isEmpty);
      expect(NavigationShape.empty.assignments, isEmpty);
      // Le drawer doit rester utilisable : aucune entrée ne lève.
      expect(NavigationShape.empty.groupOf('/dashboard'), isNull);
    });
  });
}