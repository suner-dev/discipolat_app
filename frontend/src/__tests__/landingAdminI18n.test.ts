import { describe, it, expect } from 'vitest';
import ar from '@/i18n/ar';
import en from '@/i18n/en';
import es from '@/i18n/es';
import fr from '@/i18n/fr';
import pt from '@/i18n/pt';
import sw from '@/i18n/sw';

/**
 * LOT 2 §GLISE-D'ABORD (T2.5, R9) — l'onglet admin « Page publique » doit parler
 * les SIX langues. Une clé `landingAdmin.*` ajoutée dans la seule locale `fr`
 * serait rendue brute (ou en français) aux autres : ce garde-fou échoue dès
 * qu'une clé manque, est vide, ou reste la clé elle-même dans l'une des six.
 */
const DICTIONARIES = { fr, en, es, pt, sw, ar } as const;

const LANDING_ADMIN_KEYS = [
  'landingAdmin.tab',
  'landingAdmin.title',
  'landingAdmin.subtitle',
  'landingAdmin.enabledLabel',
  'landingAdmin.enabledHint',
  'landingAdmin.requiresDirectory',
  'landingAdmin.noindexNote',
  'landingAdmin.shareLabel',
  'landingAdmin.copy',
  'landingAdmin.copied',
  'landingAdmin.copyErr',
  'landingAdmin.preview',
  'landingAdmin.noSlug',
  'landingAdmin.previewTitle',
  'landingAdmin.unnamed',
  'landingAdmin.joinCta',
] as const;

describe("LOT 2 §GLISE-D'ABORD (T2.5) — parité i18n de l'onglet « Page publique »", () => {
  for (const key of LANDING_ADMIN_KEYS) {
    it(`${key} existe et est traduite dans les six locales`, () => {
      for (const [locale, dict] of Object.entries(DICTIONARIES)) {
        const value = (dict as Record<string, string>)[key];
        expect(value, `locale ${locale} — ${key}`).toBeTruthy();
        // Jamais la clé brute : elle aurait été rendue telle quelle à l'écran.
        expect(value, `locale ${locale} — ${key}`).not.toBe(key);
      }
    });
  }

  it('aucune clé landingAdmin.* ne fuit le français dans une autre locale (filet)', () => {
    const frKeys = Object.keys(fr).filter((k) => k.startsWith('landingAdmin.'));
    expect(frKeys.length).toBeGreaterThanOrEqual(LANDING_ADMIN_KEYS.length);
    for (const [locale, dict] of Object.entries(DICTIONARIES)) {
      for (const key of frKeys) {
        const value = (dict as Record<string, string>)[key];
        expect(value, `locale ${locale} — ${key}`).toBeTruthy();
        expect(value, `locale ${locale} — ${key}`).not.toBe(key);
        // La valeur FR ne doit pas être recopiée telle quelle hors FR.
        if (locale !== 'fr') {
          expect(value, `locale ${locale} — ${key} ne doit pas rester du FR`).not.toBe(
            (fr as Record<string, string>)[key],
          );
        }
      }
    }
  });
});
