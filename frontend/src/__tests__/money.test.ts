import { describe, expect, it } from 'vitest';
import {
  formatMinor,
  formatMinorCompact,
  isZeroDecimal,
  majorToInput,
  parseMajorToMinor,
  yearlyFromMonthly,
} from '@/lib/money';

/**
 * Contrat d'unités côté client — TODO_TARIFS_PAIEMENTS_INTERNATIONAL §2.1.
 *
 * <p>Le piège du frontend est symétrique de celui du backend : si `formatMinor`
 * receives un montant majeur, tous les prix affichés sont 100 fois trop petits —
 * ou 100 fois trop grands. Ces tests verrouillent les deux sens.</p>
 */
describe('money — unités mineures ISO-4217', () => {
  describe('formatMinor', () => {
    it('formate 2900 centimes comme 29,00 €', () => {
      const formatted = formatMinor(2900, { code: 'EUR', decimals: 2 }, 'fr-FR');
      expect(formatted).toContain('29');
      expect(formatted).toContain('€');
      // 29 centimes s'écrirait « 0,29 € » : c'est exactement la faille F1.
      expect(formatted).not.toContain('0,29');
    });

    it('formate 9900 centimes comme 99,00 $', () => {
      expect(formatMinor(9900, { code: 'USD', decimals: 2 }, 'en-US')).toContain('99.00');
    });

    it('formate 15000 XAF sans décimale', () => {
      const formatted = formatMinor(15000, { code: 'XAF', decimals: 0 }, 'fr-FR');
      expect(formatted).toContain('15');
      expect(formatted).not.toContain(',00');
      expect(formatted).not.toContain('.00');
    });

    it('formate 2900 GBP en livres', () => {
      expect(formatMinor(2900, { code: 'GBP', decimals: 2 }, 'en-GB')).toContain('29');
    });

    it('reste stable sur un montant non fini plutôt que d’afficher NaN', () => {
      expect(formatMinor(Number.NaN, { code: 'EUR', decimals: 2 }, 'fr-FR')).toBe('');
      expect(formatMinor(Number.POSITIVE_INFINITY, { code: 'EUR', decimals: 2 }, 'fr-FR')).toBe('');
    });
  });

  describe('formatMinorCompact', () => {
    it('arrondit à l’euro pour un affichage épuré', () => {
      expect(formatMinorCompact(2900, { code: 'EUR', decimals: 2 }, 'fr-FR')).toContain('29');
    });

    it('n’arrondit pas une devise sans centimes', () => {
      // Arrondir du franc CFA serait faux : 15 001 FCFA n'est pas 15 000.
      expect(formatMinorCompact(15001, { code: 'XAF', decimals: 0 }, 'fr-FR')).toContain('15');
    });
  });

  describe('parseMajorToMinor', () => {
    it('convertit une saisie décimale en unités mineures', () => {
      expect(parseMajorToMinor('29.99', 2)).toBe(2999);
      expect(parseMajorToMinor('29,99', 2)).toBe(2999);
      expect(parseMajorToMinor('15000', 0)).toBe(15000);
      expect(parseMajorToMinor('29', 2)).toBe(2900);
    });

    it('tolère les espaces de saisie', () => {
      expect(parseMajorToMinor(' 29,99 ', 2)).toBe(2999);
    });

    it('refuse plus de décimales que la devise n’en admet', () => {
      // XAF n'a pas de centimes : arrondir à la place de refuser enverrait
      // au client un montant différent de celui qu'il a saisi.
      expect(parseMajorToMinor('15000.5', 0)).toBeNull();
    });

    it('refuse une saisie inexploitable', () => {
      expect(parseMajorToMinor('', 2)).toBeNull();
      expect(parseMajorToMinor('abc', 2)).toBeNull();
      expect(parseMajorToMinor('1e3', 2)).toBeNull();
    });

    it('ne derive pas sur un flottant', () => {
      // 0.1 + 0.2 = 0.30000000000000004 en JavaScript : c'est exactement pour
      // cela qu'on n'utilise jamais de parseFloat * 100.
      expect(parseMajorToMinor('0.1', 2)).toBe(10);
      expect(parseMajorToMinor('0.2', 2)).toBe(20);
      expect(parseMajorToMinor('0.3', 2)).toBe(30);
    });
  });

  describe('majorToInput', () => {
    it('fait l’aller-retour avec parseMajorToMinor', () => {
      expect(parseMajorToMinor(majorToInput(2900, 2), 2)).toBe(2900);
      expect(parseMajorToMinor(majorToInput(15000, 0), 0)).toBe(15000);
    });

    it('reste vide sur une valeur absente', () => {
      expect(majorToInput(null, 2)).toBe('');
    });
  });

  describe('isZeroDecimal', () => {
    it('reconnaît les devises sans centimes', () => {
      expect(isZeroDecimal('XAF')).toBe(true);
      expect(isZeroDecimal('XOF')).toBe(true);
      expect(isZeroDecimal('JPY')).toBe(true);
      expect(isZeroDecimal('EUR')).toBe(false);
    });
  });

  describe('yearlyFromMonthly', () => {
    it('applique une remise de 17 % sur douze mois', () => {
      // 29,00 EUR x 12 = 348,00 - 17 % = 288,84 EUR -> 28884 unites mineures.
      expect(yearlyFromMonthly(2900, 17)).toBe(28884);
    });

    it('additionne le mensuel sur douze mois sans remise', () => {
      expect(yearlyFromMonthly(2900, 0)).toBe(34800);
    });

    it("reste sur la grille d'une devise sans centimes", () => {
      // 15 000 FCFA x 12 x 0,83 = 149 400 FCFA : entier, pas de centime perdu.
      expect(yearlyFromMonthly(15000, 17)).toBe(149400);
    });
  });
});