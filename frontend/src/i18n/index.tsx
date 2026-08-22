import React, { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import fr from './fr';
import en from './en';
import pt from './pt';

export type Locale = 'fr' | 'en' | 'pt';

const dictionaries: Record<Locale, Record<string, string>> = { fr, en, pt };

interface I18nContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: string, params?: Record<string, string>) => string;
  availableLocales: { code: Locale; label: string }[];
}

const I18nContext = createContext<I18nContextValue | null>(null);

const STORAGE_KEY = 'discipolat-locale';

function getInitialLocale(): Locale {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored && (stored === 'fr' || stored === 'en' || stored === 'pt')) return stored;
  } catch { /* SSR / private browsing */ }
  return 'fr';
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(getInitialLocale);

  const setLocale = useCallback((newLocale: Locale) => {
    setLocaleState(newLocale);
    try { localStorage.setItem(STORAGE_KEY, newLocale); } catch { /* ignore */ }
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
  ];

  return (
    <I18nContext.Provider value={{ locale, setLocale, t, availableLocales }}>
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error('useI18n must be used within an I18nProvider');
  return ctx;
}
