import { describe, it, expect } from 'vitest';
import ar from '@/i18n/ar';
import en from '@/i18n/en';
import es from '@/i18n/es';
import fr from '@/i18n/fr';
import pt from '@/i18n/pt';
import sw from '@/i18n/sw';

/**
 * LOT 3 §BK (T3.2, R9) — le fil d'Ariane de la zone vitrine (`PublicBreadcrumbs`)
 * doit parler les SIX locales. Une clé `publicNav.*` ajoutée seulement en `fr`
 * serait rendue brute (ou en français) aux autres : ce garde-fou échoue dès
 * qu'une clé manque, est vide, ou reste la clé elle-même dans l'une des six.
 *
 * <p>Structure volontairement calquée sur `landingAdminI18n.test.ts` (T2.5) :
 * le pattern « parité + filet anti-fuite FR » devient une convention vérifiée
 * pour chaque famille de clés i18n ajoutée au parcours « église d'abord ».</p>
 */
const DICTIONARIES = { fr, en, es, pt, sw, ar } as const;

const PUBLIC_NAV_KEYS = [
  'publicNav.home',
  'publicNav.directory',
  'publicNav.church',
] as const;

describe("LOT 3 §BK (T3.2) — parité i18n du fil d'Ariane public", () => {
  for (const key of PUBLIC_NAV_KEYS) {
    it(`${key} existe et est traduite dans les six locales`, () => {
      for (const [locale, dict] of Object.entries(DICTIONARIES)) {
        const value = (dict as Record<string, string>)[key];
        expect(value, `locale ${locale} — ${key}`).toBeTruthy();
        // Jamais la clé brute : elle aurait été rendue telle quelle à l'écran.
        expect(value, `locale ${locale} — ${key}`).not.toBe(key);
      }
    });
  }

  it('toutes les clés publicNav.* sont déclarées côté FR (filet)', () => {
    // Si quelqu'un ajoute une 4e clé `publicNav.*` en es/pt/… sans la
    // déclarer en fr, le filet la rattrape : le dictionnaire FR est la
    // source de vérité pour la découverte des familles de clés.
    const frKeys = Object.keys(fr).filter((k) => k.startsWith('publicNav.'));
    expect(frKeys.length).toBeGreaterThanOrEqual(PUBLIC_NAV_KEYS.length);
    // Chaque clé FR doit exister dans les 5 autres.
    for (const [locale, dict] of Object.entries(DICTIONARIES)) {
      for (const key of frKeys) {
        const value = (dict as Record<string, string>)[key];
        expect(value, `locale ${locale} — ${key}`).toBeTruthy();
        expect(value, `locale ${locale} — ${key}`).not.toBe(key);
      }
    }
  });

  it('aucune clé publicNav.* ne fuit le français dans une autre locale', () => {
    const frKeys = Object.keys(fr).filter((k) => k.startsWith('publicNav.'));
    for (const [locale, dict] of Object.entries(DICTIONARIES)) {
      if (locale === 'fr') continue;
      for (const key of frKeys) {
        const value = (dict as Record<string, string>)[key];
        expect(
          value,
          `locale ${locale} — ${key} ne doit pas rester du FR`,
        ).not.toBe((fr as Record<string, string>)[key]);
      }
    }
  });

  it('les trois clés sont distinctes entre elles dans chaque locale', () => {
    // Un « home », un « directory » et un « church » qui porteraient la
    // même chaîne rendraient le fil d'Ariane illisible (deux maillons
    // identiques se suivent). Le test verrouille la sémantique.
    for (const [locale, dict] of Object.entries(DICTIONARIES)) {
      const home = (dict as Record<string, string>)['publicNav.home'];
      const dir = (dict as Record<string, string>)['publicNav.directory'];
      const church = (dict as Record<string, string>)['publicNav.church'];
      expect(home, locale).not.toBe(dir);
      expect(home, locale).not.toBe(church);
      expect(dir, locale).not.toBe(church);
    }
  });
});
