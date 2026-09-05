import React, { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import fr from './fr';
import en from './en';
import pt from './pt';
import es from './es';
import sw from './sw';
import ar from './ar';

export type Locale = 'fr' | 'en' | 'pt' | 'es' | 'sw' | 'ar';

const dictionaries: Record<Locale, Record<string, string>> = { fr, en, pt, es, sw, ar };

const frDict: Record<string, string> = fr;

interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: string, params?: Record<string, string>) => string;
  availableLocales: { code: Locale; label: string }[];
}

const I18nContext = createContext<I18nContextValue | null>(null);

/** Valeur de secours (locale FR par défaut) quand aucun provider n'est monté —
 * évite un crash des composants rendus hors provider (tests, micro-vues). */
const FALLBACK: I18nContextValue = {
  locale: 'fr',
  setLocale: () => undefined,
  t: (key, params) => {
    let value = frDict[key] ?? key;
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        value = value.replace(new RegExp(`\\{${k}\\}`, 'g'), v);
      });
    }
    return value;
  },
  availableLocales: [
    { code: 'fr' as Locale, label: 'Français' },
    { code: 'en' as Locale, label: 'English' },
    { code: 'pt' as Locale, label: 'Português' },
  ],
};

const STORAGE_KEY = 'discipolat-locale';

function getInitialLocale(): Locale {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored && (stored === 'fr' || stored === 'en' || stored === 'pt' || stored === 'es' || stored === 'sw' || stored === 'ar')) return stored;
  } catch { /* SSR / private browsing */ }
  return 'fr';
}

const RTL_LOCALES: Locale[] = ['ar'];

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(getInitialLocale);

  // Initialize document direction on mount
  useEffect(() => {
    document.documentElement.dir = RTL_LOCALES.includes(locale) ? 'rtl' : 'ltr';
    document.documentElement.lang = locale;
  }, [locale]);

  const setLocale = useCallback((newLocale: Locale) => {
    setLocaleState(newLocale);
    try {
      localStorage.setItem(STORAGE_KEY, newLocale);
      // Update document direction for RTL languages
      document.documentElement.dir = RTL_LOCALES.includes(newLocale) ? 'rtl' : 'ltr';
      document.documentElement.lang = newLocale;
    } catch { /* ignore */ }
  }, []);

  const t = useCallback((key: string, params?: Record<string, string>): string => {
    let value = dictionaries[locale]?.[key] ?? dictionaries.fr[key] ?? key;
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        value = value.replace(new RegExp(`\\{${k}\\}`, 'g'), v);
      });
    }
    return value;
  }, [locale]);

  const availableLocales = [
    { code: 'fr' as Locale, label: 'Français' },
    { code: 'en' as Locale, label: 'English' },
    { code: 'pt' as Locale, label: 'Português' },
    { code: 'es' as Locale, label: 'Español' },
    { code: 'sw' as Locale, label: 'Kiswahili' },
    { code: 'ar' as Locale, label: 'العربية' },
  ];

  return (
    <I18nContext.Provider value={{ locale, setLocale, t, availableLocales }}>
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  // Dégradation gracieuse : hors provider (tests isolés, rendus partiels),
  // on retombe sur la locale par défaut au lieu de faire planter le rendu.
  return useContext(I18nContext) ?? FALLBACK;
}
