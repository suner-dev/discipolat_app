import { useState, useRef, useEffect } from 'react';
import { Globe, ChevronDown } from 'lucide-react';
import { useI18n, type Locale } from '@/i18n';

const FLAG_EMOJI: Record<Locale, string> = {
  fr: '🇫🇷',
  en: '🇬🇧',
  pt: '🇧🇷',
};

export default function LanguageSwitcher() {
  const { locale, setLocale, availableLocales } = useI18n();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const current = availableLocales.find((l) => l.code === locale);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-2 px-3 py-1.5 rounded-xl text-sm font-medium
          text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
      >
        <Globe className="w-4 h-4" />
        <span>{FLAG_EMOJI[locale]}</span>
        <span className="hidden sm:inline">{current?.label}</span>
        <ChevronDown className={`w-3 h-3 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-1 z-50 min-w-[160px] bg-white dark:bg-gray-900 
          border border-gray-200 dark:border-gray-700 rounded-xl shadow-xl overflow-hidden animate-slide-up">
          {availableLocales.map((l) => (
            <button
              key={l.code}
              onClick={() => { setLocale(l.code); setOpen(false); }}
              className={`w-full flex items-center gap-3 px-4 py-2.5 text-sm transition-colors
                ${l.code === locale
                  ? 'bg-primary-50 dark:bg-primary-900/20 text-primary-600 dark:text-primary-400 font-medium'
                  : 'text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800'
                }`}
            >
              <span className="text-lg">{FLAG_EMOJI[l.code]}</span>
              <span>{l.label}</span>
              {l.code === locale && <span className="ml-auto text-primary-500">✓</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
