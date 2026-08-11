import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Church,
  Sparkles,
  Heart,
  Users,
  FileText,
  Shield,
  ArrowRight,
  BarChart3,
  AlertTriangle,
  Sun,
  Moon,
  LayoutDashboard,
  Bell,
  CheckCircle2,
  Sprout,
  Crown,
  Star,
  Quote,
  ChevronRight,
  HandHeart,
  Building2,
  Activity,
  Target,
} from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useSettings } from '@/contexts/SettingsContext';
import BetaBadge from '@/components/beta/BetaBadge';
import Reveal from '@/components/shared/Reveal';

/* ============================================================
   Compteur animé (déclenché à l'entrée dans le viewport)
   ============================================================ */
function CountUp({ value, suffix = '' }: { value: number; suffix?: string }) {
  const ref = useRef<HTMLSpanElement>(null);
  const [display, setDisplay] = useState(0);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    // Fallback jsdom / anciens navigateurs : afficher directement la valeur.
    if (typeof IntersectionObserver === 'undefined') {
      setDisplay(value);
      return;
    }

    let raf = 0;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          const duration = 1400;
          const start = performance.now();
          const tick = (now: number) => {
            const p = Math.min((now - start) / duration, 1);
            const eased = 1 - Math.pow(1 - p, 3);
            setDisplay(Math.round(eased * value));
            if (p < 1) raf = requestAnimationFrame(tick);
          };
          raf = requestAnimationFrame(tick);
          observer.unobserve(el);
        });
      },
      { threshold: 0.4 },
    );
    observer.observe(el);
    return () => {
      observer.disconnect();
      cancelAnimationFrame(raf);
    };
  }, [value]);

  return (
    <span ref={ref}>
      {display.toLocaleString('fr-FR')}{suffix}
    </span>
  );
}

/* ============================================================
   Données
   ============================================================ */
const FEATURES = [
  {
    icon: Heart,
    title: 'Suivi des disciples',
    desc: 'Familles de disciples, faiseurs et âmes : un accompagnement spirituel structuré, doux et humain, à chaque niveau.',
    gradient: 'from-rose-500 to-pink-500',
    shadow: 'hover:shadow-rose-500/20',
  },
  {
    icon: FileText,
    title: 'Reporting hebdomadaire',
    desc: 'Rapports du faiseur et de famille consolidés, validés à chaque niveau de responsabilité, sans friction.',
    gradient: 'from-emerald-500 to-teal-500',
    shadow: 'hover:shadow-emerald-500/20',
  },
  {
    icon: BarChart3,
    title: 'Tableaux de bord décisionnels',
    desc: 'KPIs de santé spirituelle, indices intelligents et tendances pour piloter la croissance avec clarté.',
    gradient: 'from-blue-500 to-indigo-500',
    shadow: 'hover:shadow-blue-500/20',
  },
  {
    icon: Building2,
    title: 'Départements & familles',
    desc: 'Une organisation limpide : département → famille → faiseur → âme, avec des responsables dédiés.',
    gradient: 'from-amber-500 to-orange-500',
    shadow: 'hover:shadow-amber-500/20',
  },
  {
    icon: AlertTriangle,
    title: 'Alertes intelligentes',
    desc: 'Absences, décrochages et délais détectés automatiquement pour intervenir au bon moment, en douceur.',
    gradient: 'from-red-500 to-rose-500',
    shadow: 'hover:shadow-red-500/20',
  },
  {
    icon: Shield,
    title: 'Sécurité & audit',
    desc: 'Espaces métiers isolés par rôle, journal d’audit immuable, confidentialité totale de chaque donnée.',
    gradient: 'from-violet-500 to-purple-500',
    shadow: 'hover:shadow-violet-500/20',
  },
];

const STEPS = [
  {
    icon: Users,
    step: '01',
    title: 'Organisez',
    desc: 'Créez vos familles de disciples, désignez vos chefs et faiseurs en quelques clics.',
  },
  {
    icon: Heart,
    step: '02',
    title: 'Accompagnez',
    desc: 'Suivez chaque âme, saisissez les rapports hebdomadaires et les présences en toute simplicité.',
  },
  {
    icon: Activity,
    step: '03',
    title: 'Détectez',
    desc: 'Les alertes intelligentes repèrent absences et décrochages pour agir avant qu’il ne soit trop tard.',
  },
  {
    icon: Target,
    step: '04',
    title: 'Croissez',
    desc: 'Pilotez la croissance spirituelle grâce à des tableaux de bord clairs et des objectifs mesurables.',
  },
];

const ROLES = [
  {
    icon: Crown,
    role: 'Pasteur',
    desc: 'Vue complète, validation finale, centre de commandement.',
    color: 'from-primary-500 to-emerald-600',
    chip: 'bg-primary-500/10 border-primary-500/20 text-primary-400',
  },
  {
    icon: Building2,
    role: 'Responsable',
    desc: 'Gestion des départements, des membres et des présences.',
    color: 'from-amber-500 to-orange-600',
    chip: 'bg-amber-500/10 border-amber-500/20 text-amber-400',
  },
  {
    icon: Users,
    role: 'Chef de famille',
    desc: 'Création de familles, suivi des disciples, rapports consolidés.',
    color: 'from-gold-500 to-amber-600',
    chip: 'bg-gold-500/10 border-gold-500/20 text-gold-400',
  },
  {
    icon: HandHeart,
    role: 'Faiseur',
    desc: 'Suivi personnalisé des disciples au quotidien.',
    color: 'from-emerald-500 to-teal-600',
    chip: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400',
  },
];

const STATS = [
  { icon: Users, value: 6, suffix: '', label: 'Espaces métiers dédiés' },
  { icon: LayoutDashboard, value: 40, suffix: '+', label: 'Fonctionnalités intégrées' },
  { icon: Bell, value: 24, suffix: '/7', label: 'Alertes automatiques' },
  { icon: Shield, value: 100, suffix: '%', label: 'Confidentialité & audit' },
];

const TESTIMONIALS = [
  {
    quote: 'Discipolat a transformé notre manière d’accompagner les familles de disciples. Tout est clair, ordonné, apaisant.',
    name: 'Pasteur Emmanuel',
    role: 'Église Locale',
  },
  {
    quote: 'Je crée mes familles, j’assigne mes disciples, je saisis mes rapports… tout est fluide et intuitif.',
    name: 'Chef de famille',
    role: 'Famille de disciples',
  },
];

export default function LandingPage() {
  const { darkMode, toggleTheme } = useTheme();
  const { branding } = useSettings();

  const scrollTo = (id: string) => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const tagline = branding.slogan || branding.description || 'Gestion du Discipolat';

  return (
    <div className="relative min-h-screen overflow-x-hidden bg-gradient-to-br from-gray-50 via-white to-gray-50 dark:from-gray-950 dark:via-gray-900 dark:to-gray-950 transition-colors duration-700">
      {/* ── Fond animé : halos doux + grille décorative ── */}
      <div className="absolute inset-0 bg-gradient-mesh animate-gradient-shift bg-[length:200%_200%]" />
      <div className="absolute -top-48 -right-40 w-[36rem] h-[36rem] rounded-full bg-primary-500/10 blur-3xl animate-pulse-soft" />
      <div className="absolute top-1/3 -left-48 w-[28rem] h-[28rem] rounded-full bg-gold-500/10 blur-3xl animate-pulse-soft" style={{ animationDelay: '1.2s' }} />
      <div className="absolute bottom-0 right-1/4 w-[24rem] h-[24rem] rounded-full bg-sky-500/10 blur-3xl animate-pulse-soft" style={{ animationDelay: '2.4s' }} />

      {/* Particules flottantes */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden>
        {[...Array(14)].map((_, i) => (
          <div
            key={i}
            className="particle"
            style={{
              width: `${Math.random() * 5 + 2}px`,
              height: `${Math.random() * 5 + 2}px`,
              left: `${Math.random() * 100}%`,
              bottom: '-10px',
              animation: `particleDrift ${Math.random() * 18 + 12}s linear infinite`,
              animationDelay: `${Math.random() * 12}s`,
            }}
          />
        ))}
      </div>

      {/* ── Navigation ── */}
      <header className="relative z-30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-20">
            <div className="flex items-center gap-3 group cursor-pointer" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-primary-500 via-primary-600 to-primary-700 flex items-center justify-center shadow-glow transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3">
                {branding.logoUrl ? (
                  <img src={branding.logoUrl} alt="" className="w-5 h-5 object-contain" />
                ) : (
                  <Church className="w-5 h-5 text-white" />
                )}
              </div>
              <div className="leading-tight">
                <span className="text-lg font-bold text-gray-900 dark:text-white font-display">{branding.platformName}</span>
                <p className="text-[10px] tracking-wide text-gray-400 dark:text-gray-500 font-medium">{branding.churchName}</p>
              </div>
            </div>

            <nav className="hidden md:flex items-center gap-8 text-sm font-medium text-gray-600 dark:text-gray-300">
              <button onClick={() => scrollTo('fonctionnalites')} className="hover:text-primary-600 dark:hover:text-primary-400 transition-colors">Fonctionnalités</button>
              <button onClick={() => scrollTo('parcours')} className="hover:text-primary-600 dark:hover:text-primary-400 transition-colors">Parcours</button>
              <button onClick={() => scrollTo('espaces')} className="hover:text-primary-600 dark:hover:text-primary-400 transition-colors">Espaces</button>
              <button onClick={() => scrollTo('temoignages')} className="hover:text-primary-600 dark:hover:text-primary-400 transition-colors">Témoignages</button>
            </nav>

            <div className="flex items-center gap-2.5">
              <BetaBadge />
              <button
                onClick={toggleTheme}
                className="p-2.5 rounded-xl glass-strong text-gray-500 dark:text-gray-300 hover:scale-110 active:scale-95 transition-all duration-300"
                title={darkMode ? 'Mode clair' : 'Mode sombre'}
                aria-label={darkMode ? 'Passer en mode clair' : 'Passer en mode sombre'}
              >
                {darkMode ? <Sun className="w-[18px] h-[18px]" /> : <Moon className="w-[18px] h-[18px]" />}
              </button>
              <Link to="/login" className="btn-glow btn-sm sm:btn-md">
                Connexion
              </Link>
            </div>
          </div>
        </div>
      </header>

      {/* ── Hero ── */}
      <main className="relative z-10">
        <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-16 sm:pt-24 pb-10 text-center">
          <Reveal>
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full glass-strong text-primary-600 dark:text-primary-400 text-xs font-medium animate-float">
              <Sparkles className="w-3.5 h-3.5" />
              {tagline}
            </div>
          </Reveal>

          <Reveal delay={80}>
            <h1 className="mt-6 text-4xl sm:text-6xl lg:text-7xl font-bold text-gray-900 dark:text-white font-display tracking-tight leading-[1.05] text-balance">
              Cultivez la{' '}
              <span className="relative inline-block">
                <span className="text-gradient">croissance spirituelle</span>
                <span
                  className="absolute -bottom-1.5 left-0 right-0 h-[3px] rounded-full bg-gradient-to-r from-primary-500 via-primary-400 to-gold-500 opacity-70"
                  aria-hidden
                />
              </span>{' '}
              de chaque âme
            </h1>
          </Reveal>

          <Reveal delay={160}>
            <p className="mt-6 max-w-2xl mx-auto text-base sm:text-lg text-gray-500 dark:text-gray-400 leading-relaxed">
              Une solution complète pour accompagner vos membres : familles de disciples,
              reporting hebdomadaire, alertes intelligentes et tableaux de bord décisionnels.
              <span className="text-gray-700 dark:text-gray-200 font-medium"> Simple, élégant, humain.</span>
            </p>
          </Reveal>

          <Reveal delay={240}>
            <div className="mt-9 flex flex-wrap items-center justify-center gap-3.5">
              <Link to="/login" className="btn-primary btn-lg group">
                Accéder à mon espace
                <ArrowRight className="w-4 h-4 transition-transform duration-300 group-hover:translate-x-1" />
              </Link>
              <button onClick={() => scrollTo('fonctionnalites')} className="btn-secondary btn-lg">
                Découvrir les fonctionnalités
              </button>
            </div>
          </Reveal>

          {/* ── Stats animées ── */}
          <Reveal delay={320}>
            <div className="mt-16 grid grid-cols-2 sm:grid-cols-4 gap-4 max-w-3xl mx-auto">
              {STATS.map((s) => (
                <div key={s.label} className="glass-card p-5 relative overflow-hidden group hover:-translate-y-1">
                  <div className="absolute top-0 left-0 w-full h-0.5 bg-gradient-to-r from-primary-500 to-gold-500 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                  <s.icon className="w-5 h-5 text-primary-500 mb-2.5 mx-auto" />
                  <p className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-gray-100 font-mono tracking-tight">
                    <CountUp value={s.value} suffix={s.suffix} />
                  </p>
                  <p className="text-[11px] text-gray-500 dark:text-gray-400 mt-1">{s.label}</p>
                </div>
              ))}
            </div>
          </Reveal>
        </section>

        {/* ── Fonctionnalités ── */}
        <section id="fonctionnalites" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-24 pt-10">
          <Reveal>
            <div className="text-center mb-14">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-600 dark:text-primary-400 text-xs font-medium mb-4">
                <Sparkles className="w-3 h-3" /> Fonctionnalités
              </span>
              <h2 className="text-3xl sm:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                Tout pour <span className="text-gradient">accompagner</span>
              </h2>
              <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
                Un outil pensé pour les pasteurs, responsables, chefs de famille et faiseurs de disciples.
              </p>
            </div>
          </Reveal>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {FEATURES.map((f, i) => (
              <Reveal key={f.title} delay={i * 70}>
                <div className={`group glass-card-interactive p-7 h-full transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl ${f.shadow}`}>
                  <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${f.gradient} text-white flex items-center justify-center shadow-lg mb-5 transition-transform duration-300 group-hover:scale-110 group-hover:-rotate-6`}>
                    <f.icon className="w-6 h-6" />
                  </div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">{f.title}</h3>
                  <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{f.desc}</p>
                  <div className="mt-5 flex items-center gap-1 text-xs font-medium text-primary-600 dark:text-primary-400 opacity-0 group-hover:opacity-100 transition-all duration-300 translate-x-[-6px] group-hover:translate-x-0">
                    En savoir plus <ChevronRight className="w-3.5 h-3.5" />
                  </div>
                </div>
              </Reveal>
            ))}
          </div>
        </section>

        {/* ── Parcours en 4 étapes ── */}
        <section id="parcours" className="relative py-24">
          <div className="absolute inset-0 bg-white/40 dark:bg-gray-900/40 backdrop-blur-sm" />
          <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="text-center mb-14">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-gold-500/10 border border-gold-500/20 text-gold-600 dark:text-gold-400 text-xs font-medium mb-4">
                  <Sprout className="w-3 h-3" /> Parcours
                </span>
                <h2 className="text-3xl sm:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                  Quatre étapes vers la <span className="text-gradient-gold">fidélité</span>
                </h2>
              </div>
            </Reveal>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {STEPS.map((s, i) => (
                <Reveal key={s.step} delay={i * 90}>
                  <div className="relative glass-card p-7 h-full group">
                    <div className="absolute -top-4 right-6 text-5xl font-bold text-gray-100 dark:text-gray-800 font-display group-hover:text-primary-100 dark:group-hover:text-primary-900/40 transition-colors duration-500">
                      {s.step}
                    </div>
                    <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-primary-500 to-primary-700 text-white flex items-center justify-center shadow-lg mb-5 transition-transform duration-300 group-hover:scale-110">
                      <s.icon className="w-6 h-6" />
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">{s.title}</h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{s.desc}</p>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </section>

        {/* ── Espaces métiers ── */}
        <section id="espaces" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24">
          <Reveal>
            <div className="text-center mb-14">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-violet-500/10 border border-violet-500/20 text-violet-600 dark:text-violet-400 text-xs font-medium mb-4">
                <LayoutDashboard className="w-3 h-3" /> Espaces métiers
              </span>
              <h2 className="text-3xl sm:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                Chacun son <span className="text-gradient">environnement</span>
              </h2>
              <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
                Un espace de travail dédié et épuré pour chaque responsabilité dans l’église.
              </p>
            </div>
          </Reveal>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {ROLES.map((r, i) => (
              <Reveal key={r.role} delay={i * 80}>
                <div className="glass-card p-7 h-full flex flex-col group hover:-translate-y-1.5 transition-all duration-300">
                  <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${r.color} text-white flex items-center justify-center shadow-lg mb-5 transition-transform duration-300 group-hover:scale-110`}>
                    <r.icon className="w-6 h-6" />
                  </div>
                  <span className={`self-start inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-medium border ${r.chip} mb-3`}>
                    {r.role}
                  </span>
                  <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{r.desc}</p>
                </div>
              </Reveal>
            ))}
          </div>
        </section>

        {/* ── Témoignages ── */}
        <section id="temoignages" className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-24">
          <Reveal>
            <div className="text-center mb-12">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-medium mb-4">
                <Quote className="w-3 h-3" /> Témoignages
              </span>
              <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                Ils accompagnent avec Discipolat
              </h2>
            </div>
          </Reveal>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {TESTIMONIALS.map((t, i) => (
              <Reveal key={i} delay={i * 100}>
                <div className="glass-strong rounded-3xl p-8 h-full relative overflow-hidden">
                  <Quote className="w-8 h-8 text-primary-500/20 absolute top-6 right-6" />
                  <Star className="w-4 h-4 text-gold-500 fill-gold-500" />
                  <Star className="w-4 h-4 text-gold-500 fill-gold-500 -ml-1" />
                  <Star className="w-4 h-4 text-gold-500 fill-gold-500 -ml-1" />
                  <Star className="w-4 h-4 text-gold-500 fill-gold-500 -ml-1" />
                  <Star className="w-4 h-4 text-gold-500 fill-gold-500 -ml-1" />
                  <p className="mt-5 text-base text-gray-700 dark:text-gray-200 leading-relaxed italic">
                    « {t.quote} »
                  </p>
                  <div className="mt-6 flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center text-white font-bold text-sm shadow-lg">
                      {t.name.charAt(0)}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t.name}</p>
                      <p className="text-xs text-gray-500 dark:text-gray-400">{t.role}</p>
                    </div>
                  </div>
                </div>
              </Reveal>
            ))}
          </div>
        </section>

        {/* ── CTA final ── */}
        <section className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pb-24">
          <Reveal>
            <div className="relative">
              <div className="absolute -inset-1 rounded-[2rem] bg-gradient-to-br from-primary-500/40 via-transparent to-gold-500/40 opacity-70 blur-xl animate-pulse-soft" />
              <div className="relative glass-strong rounded-[2rem] p-10 sm:p-16 text-center overflow-hidden">
                <div className="absolute -top-20 -right-20 w-64 h-64 rounded-full bg-primary-500/10 blur-3xl" />
                <div className="absolute -bottom-24 -left-16 w-72 h-72 rounded-full bg-gold-500/10 blur-3xl" />
                <div className="relative">
                  <div className="w-16 h-16 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-glow animate-float">
                    <CheckCircle2 className="w-8 h-8 text-white" />
                  </div>
                  <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white font-display tracking-tight text-balance">
                    Prêt à structurer le suivi de vos membres&nbsp;?
                  </h2>
                  <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
                    Connectez-vous pour accéder à votre espace métier. Le pasteur dispose d’une vue
                    complète, chaque rôle de son propre environnement de travail.
                  </p>
                  <div className="mt-8 flex flex-wrap items-center justify-center gap-3.5">
                    <Link to="/login" className="btn-primary btn-lg group">
                      Se connecter
                      <ArrowRight className="w-4 h-4 transition-transform duration-300 group-hover:translate-x-1" />
                    </Link>
                    <Link to="/forgot-password" className="btn-ghost btn-lg">
                      Mot de passe oublié ?
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </Reveal>
        </section>
      </main>

      {/* ── Footer ── */}
      <footer className="relative z-10 border-t border-gray-200/60 dark:border-gray-800/60 backdrop-blur-xl bg-white/30 dark:bg-gray-950/30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center">
                {branding.logoUrl ? (
                  <img src={branding.logoUrl} alt="" className="w-4 h-4 object-contain" />
                ) : (
                  <Church className="w-4 h-4 text-white" />
                )}
              </div>
              <span className="text-sm font-semibold text-gray-900 dark:text-white font-display">{branding.platformName}</span>
            </div>

            {(branding.phone || branding.email || branding.website) && (
              <div className="flex items-center gap-4 text-xs text-gray-400 dark:text-gray-500 flex-wrap justify-center">
                {branding.phone && <span>{branding.phone}</span>}
                {branding.email && <span>{branding.email}</span>}
                {branding.website && <span className="text-primary-600 dark:text-primary-400">{branding.website}</span>}
              </div>
            )}

            <p className="text-xs text-gray-400 dark:text-gray-500">
              © {new Date().getFullYear()} {branding.platformName}. Tous droits réservés.
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}
