import 'package:discipolat_mobile/data/services/pricing_service.dart';
import 'package:flutter_test/flutter_test.dart';

/// L'espace fine insécable du formatage mobile (voir `_groupSeparator`).
const groupSep = '\u202f';

/// Contrat d'unités monétaires côté mobile — TODO_TARIFS_PAIEMENTS_INTERNATIONAL §2.1.
///
/// Le piège du mobile est le même que partout ailleurs : afficher 15 000,00 FCFA
/// (la devise n'a pas de centimes) ou 0,29 € pour un plan à 29 € (montant majeur
/// lu comme mineur). Ces tests verrouillent les deux sens.
void main() {
  group('formatMinor — unités mineures ISO-4217', () {
    test("le franc CFA n'affiche jamais de décimale", () {
      expect(formatMinor(15000, 'XAF', 0), '15${groupSep}000 FCFA');
      expect(formatMinor(15000, 'XAF', 0), isNot(contains('.')));
      expect(formatMinor(15000, 'XAF', 0), isNot(contains(',')));
    });

    test('XOF utilise son propre symbole', () {
      // XOF et XAF sont deux devises distinctes, pas deux libellés du même.
      expect(formatMinor(15000, 'XOF', 0), '15${groupSep}000 F CFA');
    });

    test("l'euro affiche deux décimales", () {
      expect(formatMinor(2900, 'EUR', 2), '29.00 €');
      expect(formatMinor(2999, 'EUR', 2), '29.99 €');
    });

    test('un montant à six chiffres reste lisible', () {
      expect(formatMinor(150000, 'XAF', 0), '150${groupSep}000 FCFA');
      expect(formatMinor(100000, 'EUR', 2), '1${groupSep}000.00 €');
    });

    test('une devise inconnue est rendue sans symbole inventé', () {
      // Mieux vaut « 1 234 » qu'un symbole sorti de nulle part.
      final formatted = formatMinor(123456, 'ZZZ', 2);
      expect(formatted, '1${groupSep}234.56');
      expect(formatted, isNot(contains('€')));
    });
  });

  group('Décodage défensif du catalogue', () {
    test('un payload absent ne fait pas planter', () {
      expect(PricingConfigMobile.tryParse(null), isNull);
      expect(PricingCatalog.tryParse('pas un objet'), isNull);
      expect(PricingPlan.tryParse(<String, dynamic>{}), isNull);
    });

    test('un plan sans clé ni nom est écarté', () {
      expect(PricingPlan.tryParse({'name': 'Orphelin'}), isNull);
      expect(PricingPlan.tryParse({'key': 'STARTUP'}), isNull);
    });

    test('un montant en devise sans centimes reste un entier', () {
      final price = PlanPrice.tryParse({
        'currency': 'XAF',
        'cycle': 'MONTHLY',
        'amountMinor': 15000,
        'decimals': 0,
      });
      expect(price, isNotNull);
      expect(price!.amountMinor, 15000);
      expect(price.decimals, 0);
    });

    test('un display indisponible ne vaut pas zéro', () {
      final display = PlanDisplay.tryParse({
        'available': false,
        'amountMinor': null,
        'currency': 'EUR',
        'decimals': 2,
        'channels': <String>[],
      });
      expect(display, isNotNull);
      expect(display!.available, isFalse);
      expect(display.amountMinor, isNull);
    });

    test('le débit en devise forte est détecté', () {
      final display = PlanDisplay.tryParse({
        'available': true,
        'amountMinor': 15000,
        'currency': 'XAF',
        'decimals': 0,
        'checkoutCurrency': 'EUR',
      });
      expect(display!.chargedInDifferentCurrency, isTrue);
    });

    test('un cycle inconnu retombe sur le mensuel', () {
      expect(PlanPrice.tryParse({
        'currency': 'EUR',
        'cycle': 'TRIMESTRIEL',
        'amountMinor': 2900,
      })!.cycle, BillingCycleMobile.monthly);
    });
  });
}