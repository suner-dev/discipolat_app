import { describe, it, expect } from 'vitest';
import ar from '@/i18n/ar';
import en from '@/i18n/en';
import es from '@/i18n/es';
import fr from '@/i18n/fr';
import pt from '@/i18n/pt';
import sw from '@/i18n/sw';

/**
 * LOT 1 §GLISE-D'ABORD (T1.5, R9) — le sélecteur d'église doit parler les SIX
 * langues. Une clé ajoutée dans la seule locale `fr` serait rendue brute (ou en
 * français) pour les autres : ce garde-fou échoue dès qu'une clé `churchPicker.*`
 * manque, est vide, ou reste la clé elle-même dans l'une des six locales.
 */
const DICTIONARIES = { fr, en, es, pt, sw, ar } as const;

const PICKER_KEYS = [
  'churchPicker.label',
  'churchPicker.optional',
  'churchPicker.placeholder',
  'churchPicker.clear',
  'churchPicker.results',
  'churchPicker.found',
  'churchPicker.notFound',
  'churchPicker.ctaCode',
  'churchPicker.ctaInvite',
  'churchPicker.ctaCreate',
  'churchPicker.error',
  'churchPicker.loginLabel',
  'churchPicker.joinHint',
  'churchPicker.openJoin',
  'churchPicker.cancel',
] as const;

describe("LOT 1 §GLISE-D'ABORD (T1.5) — parité i18n du sélecteur d'église", () => {
  for (const key of PICKER_KEYS) {
    it(`${key} existe, est traduit et interpolate {name} dans les six locales`, () => {
      const needsName = key === 'churchPicker.found' || key === 'churchPicker.joinHint';
      for (const [locale, dict] of Object.entries(DICTIONARIES)) {
        const value = (dict as Record<string, string>)[key];
        expect(value, `locale ${locale} — ${key}`).toBeTruthy();
        // Jamais la clé brute : elle aurait été rendue telle quelle à l'écran.
        expect(value, `locale ${locale} — ${key}`).not.toBe(key);
        if (needsName) {
          expect(value, `locale ${locale} — ${key} doit porter {name}`).toContain('{name}');
        }
      }
    });
  }

  it('toutes les clés churchPicker.* présentes en FR existent aussi ailleurs (filet)', () => {
    const frKeys = Object.keys(fr).filter((k) => k.startsWith('churchPicker.'));
    expect(frKeys.length).toBeGreaterThanOrEqual(PICKER_KEYS.length);
    for (const [locale, dict] of Object.entries(DICTIONARIES)) {
      for (const key of frKeys) {
        const value = (dict as Record<string, string>)[key];
        expect(value, `locale ${locale} — ${key}`).toBeTruthy();
        expect(value, `locale ${locale} — ${key}`).not.toBe(key);
      }
    }
  });
});
