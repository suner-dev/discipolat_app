import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Menu, X, Sun, Moon, Church, ArrowRight, ChevronDown } from 'lucide-react';
import { useI18n } from '@/i18n';
import { useTheme } from '@/hooks/useTheme';
import { useSettings } from '@/contexts/SettingsContext';
import LanguageSwitcher from '@/components/shared/LanguageSwitcher';

const NAV_ITEMS: { key: string; id: string }[] = [
  { key: 'landing.nav.product', id: 'problem' },
  { key: 'landing.nav.features', id: 'features' },
  { key: 'landing.nav.solutions', id: 'solutions' },
  { key: 'landing.nav.platform', id: 'modules' },
  { key: 'landing.nav.roles', id: 'roles' },
  { key: 'landing.nav.pricing', id: 'pricing' },
  { key: 'landing.nav.about', id: 'about' },
];

interface LandingNavbarProps { onNavigate: (id: string) => void; onDemo: () => void; }

export default function LandingNavbar({ onNavigate, onDemo }: LandingNavbarProps) {
  const { t } = useI18n();
  const { darkMode, toggleTheme } = useTheme();
  const { branding } = useSettings();
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const [expanded, setExpanded] = useState<string | null>(null);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, []);

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : '';
    return () => { document.body.style.overflow = ''; };
  }, [open]);

  const go = (id: string) => { setOpen(false); onNavigate(id); };

  return (
    <header className={`fixed top-0 inset-x-0 z-50 transition-all duration-300 ${scrolled ? 'backdrop-blur-xl bg-white/75 dark:bg-gray-950/70 shadow-lg shadow-gray-950/5 border-b border-gray-200/50 dark:border-white/10' : 'bg-transparent'}`}>
      <a href="#main-content" className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-[60] focus:px-4 focus:py-2 focus:rounded-xl focus:bg-primary-600 focus:text-white text-sm">
        {t('landing.skipToContent')}
      </a>

      <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8" aria-label="Principal">
        <div className="flex items-center justify-between h-16 sm:h-[72px]">
          <button onClick={() => go('hero')} className="flex items-center gap-2.5 group" aria-label="Discipolat">
            <span className="relative">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-md shadow-primary-500/30 transition-transform duration-300 group-hover:scale-105">
                {branding.logoUrl ? <img src={branding.logoUrl} alt="" className="w-6 h-6 object-contain" /> : <Church className="w-5 h-5 text-white" />}
              </div>
              <span className="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full bg-gold-400 animate-ping" />
            </span>
            <span className="text-base sm:text-lg font-bold font-display text-gray-900 dark:text-white">{branding.platformName || 'Discipolat'}</span>
          </button>

          <div className="hidden lg:flex items-center gap-1">
            {NAV_ITEMS.map((item) => (
              <button key={item.id} onClick={() => go(item.id)} className="px-3.5 py-2 rounded-xl text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-100/70 dark:hover:bg-gray-800/50 transition-colors">
                {t(item.key)}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <LanguageSwitcher />
            <ThemeToggle darkMode={darkMode} toggleTheme={toggleTheme} />
            <Link to="/login" className="hidden sm:inline-flex items-center px-4 py-2 rounded-xl text-sm font-medium text-gray-700 dark:text-gray-200 hover:bg-gray-100/70 dark:hover:bg-gray-800/50 transition-colors">
              {t('landing.nav.login')}
            </Link>
            <div className="hidden sm:flex items-center gap-2 ml-1">
              <Link to="/login" className="btn-primary btn-md flex items-center gap-1.5 group">
                {t('landing.nav.start')}
                <ArrowRight className="w-3.5 h-3.5 transition-transform duration-300 group-hover:translate-x-0.5" />
              </Link>
            </div>
            <button
              onClick={() => setOpen(!open)}
              aria-label={open ? t('landing.closeMenu') : t('landing.openMenu')}
              aria-expanded={open}
              className="lg:hidden p-2 rounded-xl text-gray-600 dark:text-gray-300 hover:bg-gray-100/70 dark:hover:bg-gray-800/50 transition-colors"
            >
              {open ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </nav>
      {/* Menu mobile */}
      {open && (
        <div className="lg:hidden fixed inset-0 top-16 bg-white/95 dark:bg-gray-950/95 backdrop-blur-xl z-40 overflow-y-auto animate-fade-in">
          <div className="px-6 py-6 space-y-1.5">
            {NAV_ITEMS.map((item) => (
              <div key={item.id} className="border-b border-gray-100 dark:border-gray-800/60">
                <button
                  onClick={() => go(item.id)}
                  className="w-full flex items-center justify-between py-3.5 text-base font-medium text-gray-800 dark:text-gray-100 text-left"
                >
                  {t(item.key)}
                  <ChevronDown className={`w-4 h-4 text-gray-400 transition-transform ${expanded === item.id ? 'rotate-180' : ''}`} onClick={(e) => { e.stopPropagation(); setExpanded(expanded === item.id ? null : item.id); }} />
                </button>
              </div>
            ))}

            <div className="pt-4 space-y-3">
              <Link to="/login" onClick={() => setOpen(false)} className="w-full btn-secondary btn-lg flex items-center justify-center">
                {t('landing.nav.login')}
              </Link>
              <button onClick={() => { setOpen(false); onDemo(); }} className="w-full btn-ghost btn-lg">
                {t('landing.hero.ctaDemo')}
              </button>
              <Link to="/login" onClick={() => setOpen(false)} className="w-full btn-primary btn-lg flex items-center justify-center gap-2">
                {t('landing.nav.start')}
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>
        </div>
      )}
    </header>
  );
}

function ThemeToggle({ darkMode, toggleTheme }: { darkMode: boolean; toggleTheme: () => void }) {
  const { t } = useI18n();
  return (
    <button
      onClick={toggleTheme}
      aria-label={darkMode ? t('landing.themeLight') : t('landing.themeDark')}
      title={darkMode ? t('landing.themeLight') : t('landing.themeDark')}
      className="p-2 rounded-xl text-gray-600 dark:text-gray-300 hover:bg-gray-100/70 dark:hover:bg-gray-800/50 transition-colors"
    >
      {darkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
    </button>
  );
}