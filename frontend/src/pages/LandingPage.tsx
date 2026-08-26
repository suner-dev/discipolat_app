import { useState, useEffect, useRef } from 'react';
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
  ChevronRight,
  HandHeart,
  Building2,
  Activity,
  Target,
  Zap,
  Lock,
  Settings,
  Smartphone,
  Tablet,
  Monitor,
  Eye,
  EyeOff,
  UserCheck,
  GitBranch,
  MessageCircle,
  CalendarCheck,
  FileBarChart,
  AlertCircle,
  PieChart,
  KeyRound,
  Fingerprint,
  ShieldCheck,
  ServerCrash,
  FolderOpen,
  ClipboardList,
  UsersRound,
  ArrowUpRight,
} from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useSettings } from '@/contexts/SettingsContext';
import BetaBadge from '@/components/beta/BetaBadge';
import Reveal from '@/components/shared/Reveal';
import HeroSection from '@/components/landing/HeroSection';

/* ============================================================
   Data
   ============================================================ */

const PROBLEMS = [
  {
    icon: FolderOpen,
    title: 'Fiches dispersées',
    desc: 'Les suivis s\'accumulent dans des fichiers Excel, des cahiers et des chats WhatsApp — sans vue d\'ensemble.',
  },
  {
    icon: UsersRound,
    title: 'Suivi manuel',
    desc: 'Les responsables perdent des heures à consolider des rapports manuels, à chercher des informations et à relancer.',
  },
  {
    icon: EyeOff,
    title: 'Aucune visibilité',
    desc: 'Le pasteur ne dispose pas d\'une vision claire de la santé spirituelle de chaque membre et de chaque famille.',
  },
  {
    icon: AlertCircle,
    title: 'Alertes en retard',
    desc: 'Les absences prolongées, les décrochages et les situations critiques ne sont détectés qu\'après coup.',
  },
  {
    icon: MessageCircle,
    title: 'Communicationfragmentée',
    desc: 'Les messages se perdent entre WhatsApp, SMS, appels et réunions. Rien n\'est centralisé.',
  },
  {
    icon: ServerCrash,
    title: 'Données perdues',
    desc: 'L\'historique, les rapports et les contacts se perdent quand un responsable quitte ou change de rôle.',
  },
];

const SOLUTIONS = [
  { icon: LayoutDashboard, text: 'Un tableau de bord clair pour chaque rôle' },
  { icon: GitBranch, text: 'Une hiérarchie Pasteur → Responsable → Chef → Faiseur → Disciple' },
  { icon: BarChart3, text: 'Des statistiques et KPIs en temps réel' },
  { icon: Bell, text: 'Des alertes automatiques intelligentes' },
  { icon: ClipboardList, text: 'Des rapports hebdomadaires consolidés' },
  { icon: ShieldCheck, text: 'Une sécurité et confidentialité totales' },
];

const MODULES = [
  { icon: Heart, name: 'Discipolat', desc: 'Suivi des âmes, disciples et faiseurs' },
  { icon: Users, name: 'Membres', desc: 'Gestion complète des membres' },
  { icon: Building2, name: 'Départements', desc: 'Organisation des départements' },
  { icon: UsersRound, name: 'Familles', desc: 'Familles de disciples' },
  { icon: FileBarChart, name: 'Rapports', desc: 'Centralisation des rapports' },
  { icon: Heart, name: 'Prières', desc: 'Suivi des sujets de prière' },
  { icon: UserCheck, name: 'Présences', desc: 'Suivi des participations' },
  { icon: CalendarCheck, name: 'Événements', desc: 'Organisation des événements' },
  { icon: AlertTriangle, name: 'Alertes', desc: 'Détection automatique' },
  { icon: PieChart, name: 'Statistiques', desc: 'Pilotage de l\'église' },
];

const ROLES_DATA = [
  {
    icon: Crown,
    role: 'Pasteur',
    desc: 'Vue complète de toute l\'église. Validation finale. Centre de commandement stratégique.',
    features: ['Tableau de bord global', 'Alertes et notifications', 'Rapports consolidés', 'Gestion des départements'],
    gradient: 'from-emerald-500 to-green-600',
    chip: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-600 dark:text-emerald-400',
  },
  {
    icon: Building2,
    role: 'Responsable',
    desc: 'Gestion de son département, suivi des familles et des membres, coordination.',
    features: ['Vue départementale', 'Gestion des familles', 'Rapports de famille', 'Suivi des présences'],
    gradient: 'from-amber-500 to-orange-600',
    chip: 'bg-amber-500/10 border-amber-500/20 text-amber-600 dark:text-amber-400',
  },
  {
    icon: Users,
    role: 'Chef de famille',
    desc: 'Création de familles, suivi des disciples, rapports hebdomadaires consolidés.',
    features: ['Gestion de famille', 'Assignation des disciples', 'Rapports hebdo', 'Suivi spirituel'],
    gradient: 'from-yellow-500 to-amber-600',
    chip: 'bg-yellow-500/10 border-yellow-500/20 text-yellow-600 dark:text-yellow-400',
  },
  {
    icon: HandHeart,
    role: 'Faiseur',
    desc: 'Accompagnement personnalisé de chaque disciple. Prise de notes et rapports.',
    features: ['Liste de disciples', 'Prise de notes', 'Rapport hebdomadaire', 'Historique de suivi'],
    gradient: 'from-emerald-500 to-teal-600',
    chip: 'bg-teal-500/10 border-teal-500/20 text-teal-600 dark:text-teal-400',
  },
  {
    icon: Heart,
    role: 'Membre',
    desc: 'Espace personnel. Activités, prières, événements et parcours spirituel.',
    features: ['Mon parcours', 'Activités personnelles', 'Prières et louanges', 'Événements à venir'],
    gradient: 'from-rose-500 to-pink-600',
    chip: 'bg-rose-500/10 border-rose-500/20 text-rose-600 dark:text-rose-400',
  },
];

const STEPS_DATA = [
  { step: '01', icon: Settings, title: 'Configurez', desc: 'Définissez votre organisation, vos départements et les rôles de votre église.', color: 'from-primary-500 to-primary-700' },
  { step: '02', icon: UsersRound, title: 'Organisez', desc: 'Créez vos familles de disciples, désignez chefs et faiseurs en quelques clics.', color: 'from-amber-500 to-orange-600' },
  { step: '03', icon: Activity, title: 'Accompagnez', desc: 'Suivez chaque âme, saisissez rapports et présences. Les alertes travaillent pour vous.', color: 'from-emerald-500 to-teal-600' },
  { step: '04', icon: Target, title: 'Pilotez', desc: 'KPIs, tendances et objectifs mesurables pour une croissance spirituelle guidée.', color: 'from-violet-500 to-purple-600' },
];

const BEFORE_AFTER = {
  before: [
    'Fichiers Excel éparpillés',
    'Suivi manuel chronophage',
    'Rapports papier perdus',
    'Absence de visibilité',
    'Alertes en retard',
    'Données dispersées',
  ],
  after: [
    'Plateforme centralisée',
    'Automatisation intelligente',
    'Rapports consolidés en temps réel',
    'Visibilité globale instantanée',
    'Alertes automatiques proactive',
    'Historique sécurisé et traçable',
  ],
};

const STATS_DATA = [
  { value: 6, suffix: '', label: 'Espaces métier dédiés' },
  { value: 100, suffix: '+', label: 'Modules configurables' },
  { value: 5, suffix: '', label: 'Niveaux hiérarchiques' },
  { value: 24, suffix: '/7', label: 'Disponibilité' },
];

const SECURITY_FEATURES = [
  { icon: Lock, title: 'Isolation des données', desc: 'Chaque rôle ne voit que les données qui le concernent.' },
  { icon: Eye, title: 'Journal d\'audit', desc: 'Chaque action est tracée, horodatée et infalsifiable.' },
  { icon: Fingerprint, title: 'Contrôle d\'accès', desc: 'Permissions granulaires par rôle, département et famille.' },
  { icon: ShieldCheck, title: 'Confidentialité', desc: 'Les données sensibles sont isolées et chiffrées.' },
  { icon: KeyRound, title: 'Authentification', desc: 'JWT sécurisé, 2FA, magic link et mots de passe temporaires.' },
  { icon: ServerCrash, title: 'Sauvegardes', desc: 'Sauvegardes automatiques et restauration en un clic.' },
];

const CUSTOMIZATION_ITEMS = [
  { icon: Church, text: 'Logo et identité visuelle' },
  { icon: Sparkles, text: 'Couleurs de la marque' },
  { icon: Building2, text: 'Départements personnalisés' },
  { icon: Users, text: 'Rôles et permissions' },
  { icon: Settings, text: 'Modules activables' },
  { icon: FileText, text: 'Champs personnalisés' },
];

export default function LandingPage() {
  const { darkMode, toggleTheme } = useTheme();
  const { branding } = useSettings();
  const [activeRole, setActiveRole] = useState(0);

  const scrollTo = (id: string) => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const tagline = branding.slogan || branding.description || 'Gestion du Discipolat';

  return (
    <div className="relative min-h-screen overflow-x-hidden bg-gradient-to-br from-gray-50 via-white to-gray-50 dark:from-gray-950 dark:via-gray-900 dark:to-gray-950 transition-colors duration-700">
      {/* ── Sticky Navbar ── */}
      <header className="fixed top-0 inset-x-0 z-50 glass-strong border-b border-white/10 dark:border-white/5">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16 sm:h-18">
            <div className="flex items-center gap-3 group cursor-pointer" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500 via-primary-600 to-primary-700 flex items-center justify-center shadow-glow transition-all duration-300 group-hover:scale-110 group-hover:rotate-3">
                {branding.logoUrl ? (
                  <img src={branding.logoUrl} alt="" className="w-4.5 h-4.5 object-contain" />
                ) : (
                  <Church className="w-4.5 h-4.5 text-white" />
                )}
              </div>
              <span className="text-base font-bold text-gray-900 dark:text-white font-display hidden sm:inline">{branding.platformName}</span>
            </div>

            <nav className="hidden lg:flex items-center gap-1">
              {[
                { label: 'Fonctionnalités', id: 'features' },
                { label: 'Écosystème', id: 'ecosystem' },
                { label: 'Modules', id: 'modules' },
                { label: 'Rôles', id: 'roles' },
                { label: 'Comment ça marche', id: 'how-it-works' },
                { label: 'Sécurité', id: 'security' },
              ].map((item) => (
                <button key={item.id} onClick={() => scrollTo(item.id)} className="px-3 py-2 text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-400 rounded-xl hover:bg-primary-500/[0.06] transition-all duration-200">
                  {item.label}
                </button>
              ))}
            </nav>

            <div className="flex items-center gap-2">
              <BetaBadge />
              <button onClick={toggleTheme} className="p-2 rounded-xl glass-strong text-gray-500 dark:text-gray-300 hover:scale-110 active:scale-95 transition-all duration-300" aria-label={darkMode ? 'Passer en mode clair' : 'Passer en mode sombre'}>
                {darkMode ? <Sun className="w-[16px] h-[16px]" /> : <Moon className="w-[16px] h-[16px]" />}
              </button>
              <Link to="/login" className="btn-glow btn-sm hidden sm:inline-flex">Connexion</Link>
            </div>
          </div>
        </div>
      </header>

      <main className="relative z-10">
        {/* ── Hero ── */}
        <HeroSection onNavigate={scrollTo} />

        {/* ── Section 02: Le Problème ── */}
        <section id="problem" className="relative py-24 sm:py-32">
          <div className="absolute inset-0 bg-gray-900 dark:bg-gray-950" />
          <div className="absolute inset-0 bg-gradient-to-br from-gray-900 via-gray-900 to-gray-950 dark:from-gray-950 dark:via-gray-900 dark:to-gray-950" />
          <div className="absolute -top-32 -right-32 w-[400px] h-[400px] rounded-full bg-red-500/[0.04] blur-[100px]" />
          <div className="absolute -bottom-32 -left-32 w-[350px] h-[350px] rounded-full bg-amber-500/[0.03] blur-[100px]" />

          <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="text-center mb-16">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-red-500/10 border border-red-500/20 text-red-400 text-xs font-medium mb-4">
                  <AlertTriangle className="w-3 h-3" /> Le défi
                </span>
                <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-white font-display tracking-tight">
                  Les églises méritent <span className="text-gradient">mieux</span>
                </h2>
                <p className="mt-4 text-sm sm:text-base text-gray-400 max-w-xl mx-auto">
                  La majorité des églises gèrent leur discipolat avec des outils fragmentés, manuels et inefficient.
                </p>
              </div>
            </Reveal>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {PROBLEMS.map((p, i) => (
                <Reveal key={p.title} delay={i * 60}>
                  <div className="group relative p-6 rounded-2xl border border-white/[0.06] bg-white/[0.03] backdrop-blur-sm hover:border-red-500/20 hover:bg-white/[0.05] transition-all duration-300">
                    <div className="absolute top-0 left-0 w-full h-0.5 bg-gradient-to-r from-red-500/50 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 rounded-t-2xl" />
                    <div className="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center mb-4">
                      <p.icon className="w-5 h-5 text-red-400" />
                    </div>
                    <h3 className="text-base font-semibold text-white mb-2">{p.title}</h3>
                    <p className="text-sm text-gray-400 leading-relaxed">{p.desc}</p>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </section>

        {/* ── Section 03: La Solution ── */}
        <section id="solution" className="py-24 sm:py-32">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid lg:grid-cols-2 gap-16 items-center">
              <Reveal>
                <div>
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-600 dark:text-primary-400 text-xs font-medium mb-4">
                    <Sparkles className="w-3 h-3" /> La solution
                  </span>
                  <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight leading-tight">
                    Une seule plateforme.{' '}
                    <span className="text-gradient">Tout centralisé.</span>
                  </h2>
                  <p className="mt-5 text-base text-gray-500 dark:text-gray-400 leading-relaxed">
                    Discipolat remplace vos fichiers Excel, vos groupes WhatsApp et vos cahiers par une plateforme
                    pensée spécifiquement pour le discipolat d'église. Chaque rôle dispose de son espace dédié.
                  </p>
                </div>
              </Reveal>

              <Reveal delay={120}>
                <div className="space-y-3">
                  {SOLUTIONS.map((s, i) => (
                    <div key={i} className="flex items-start gap-4 p-4 rounded-xl glass-card hover:-translate-y-0.5 transition-all duration-200">
                      <div className="w-10 h-10 rounded-xl bg-primary-500/10 flex items-center justify-center flex-shrink-0">
                        <s.icon className="w-5 h-5 text-primary-600 dark:text-primary-400" />
                      </div>
                      <p className="text-sm font-medium text-gray-700 dark:text-gray-200 pt-2">{s.text}</p>
                    </div>
                  ))}
                </div>
              </Reveal>
            </div>
          </div>
        </section>

        {/* ── Section 04: Écosystème ── */}
        <section id="ecosystem" className="relative py-24 sm:py-32 overflow-hidden">
          <div className="absolute inset-0 bg-white/40 dark:bg-gray-900/40 backdrop-blur-sm" />
          <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="text-center mb-16">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-violet-500/10 border border-violet-500/20 text-violet-600 dark:text-violet-400 text-xs font-medium mb-4">
                  <GitBranch className="w-3 h-3" /> Écosystème
                </span>
                <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                  De la vision du pasteur au <span className="text-gradient">quotidien du disciple</span>
                </h2>
                <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-2xl mx-auto">
                  Une hiérarchie claire, des connexions fluides, une responsabilité à chaque niveau.
                </p>
              </div>
            </Reveal>

            {/* Ecosystem hierarchy visual */}
            <Reveal delay={100}>
              <div className="max-w-3xl mx-auto">
                {[
                  { level: 'Pasteur', icon: Crown, color: 'from-emerald-500 to-green-600', desc: 'Vision globale, validation finale' },
                  { level: 'Responsable', icon: Building2, color: 'from-amber-500 to-orange-600', desc: 'Gestion des départements' },
                  { level: 'Chef de famille', icon: Users, color: 'from-yellow-500 to-amber-600', desc: 'Création et suivi des familles' },
                  { level: 'Faiseur', icon: HandHeart, color: 'from-emerald-500 to-teal-600', desc: 'Accompagnement personnalisé' },
                  { level: 'Disciple', icon: Heart, color: 'from-rose-500 to-pink-600', desc: 'Croissance spirituelle' },
                ].map((item, i, arr) => (
                  <div key={item.level} className="relative">
                    <div className="flex items-center gap-4 sm:gap-6 p-4 sm:p-5 rounded-2xl glass-card hover:-translate-y-0.5 transition-all duration-200 group">
                      <div className={`w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-gradient-to-br ${item.color} text-white flex items-center justify-center shadow-lg transition-transform duration-300 group-hover:scale-110 flex-shrink-0`}>
                        <item.icon className="w-6 h-6" />
                      </div>
                      <div className="flex-1">
                        <h4 className="text-base sm:text-lg font-semibold text-gray-900 dark:text-white">{item.level}</h4>
                        <p className="text-sm text-gray-500 dark:text-gray-400">{item.desc}</p>
                      </div>
                      <ChevronRight className="w-5 h-5 text-gray-300 dark:text-gray-600 hidden sm:block" />
                    </div>
                    {i < arr.length - 1 && (
                      <div className="flex justify-center py-1">
                        <div className="w-0.5 h-6 bg-gradient-to-b from-primary-500/30 to-primary-500/10 rounded-full" />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </Reveal>
          </div>
        </section>

        {/* ── Section 05: Fonctionnalités ── */}
        <section id="features" className="py-24 sm:py-32">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="text-center mb-16">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-600 dark:text-primary-400 text-xs font-medium mb-4">
                  <Zap className="w-3 h-3" /> Fonctionnalités
                </span>
                <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                  Tout pour <span className="text-gradient">accompagner</span>
                </h2>
                <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
                  Un outil complet pensé pour les pasteurs, responsables, chefs de famille et faiseurs de disciples.
                </p>
              </div>
            </Reveal>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {[
                { icon: Heart, title: 'Suivi des disciples', desc: 'Familles de disciples, faiseurs et âmes : un accompagnement structuré, doux et humain, à chaque niveau.', gradient: 'from-rose-500 to-pink-500' },
                { icon: FileText, title: 'Reporting hebdomadaire', desc: 'Rapports du faiseur et de famille consolidés, validés à chaque niveau de responsabilité.', gradient: 'from-emerald-500 to-teal-500' },
                { icon: BarChart3, title: 'Tableaux de bord', desc: 'KPIs de santé spirituelle, indices intelligents et tendances pour piloter la croissance.', gradient: 'from-blue-500 to-indigo-500' },
                { icon: Building2, title: 'Départements & familles', desc: 'Département → Famille → Faiseur → Âme, avec des responsables dédiés à chaque étape.', gradient: 'from-amber-500 to-orange-500' },
                { icon: AlertTriangle, title: 'Alertes intelligentes', desc: 'Absences, décrochages et délais détectés automatiquement pour intervenir au bon moment.', gradient: 'from-red-500 to-rose-500' },
                { icon: Shield, title: 'Sécurité & audit', desc: 'Espaces isolés par rôle, journal d\'audit immuable, confidentialité totale de chaque donnée.', gradient: 'from-violet-500 to-purple-500' },
              ].map((f, i) => (
                <Reveal key={f.title} delay={i * 70}>
                  <div className="group glass-card-interactive p-7 h-full transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl">
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
          </div>
        </section>

        {/* ── Section 06: Une seule plateforme (Modules) ── */}
        <section id="modules" className="py-24 sm:py-32 bg-gradient-to-b from-gray-50/50 to-white dark:from-gray-900/50 dark:to-gray-950">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="text-center mb-16">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-gold-500/10 border border-gold-500/20 text-gold-600 dark:text-gold-400 text-xs font-medium mb-4">
                  <LayoutDashboard className="w-3 h-3" /> Plateforme
                </span>
                <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                  Une <span className="text-gradient-gold">seule plateforme</span> pour tout
                </h2>
                <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
                  Tous les modules dont votre église a besoin, dans un seul écosystème cohérent.
                </p>
              </div>
            </Reveal>

            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
              {MODULES.map((m, i) => (
                <Reveal key={m.name} delay={i * 50}>
                  <div className="glass-card p-5 h-full text-center group hover:-translate-y-1 transition-all duration-300 cursor-default">
                    <div className="w-11 h-11 rounded-xl bg-primary-500/[0.08] dark:bg-primary-500/[0.12] flex items-center justify-center mx-auto mb-3 transition-transform duration-300 group-hover:scale-110">
                      <m.icon className="w-5 h-5 text-primary-600 dark:text-primary-400" />
                    </div>
                    <h4 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-1">{m.name}</h4>
                    <p className="text-[11px] text-gray-500 dark:text-gray-400 leading-relaxed">{m.desc}</p>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </section>

        {/* ── Section 07: Espaces Rôles ── */}
        <section id="roles" className="py-24 sm:py-32">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="text-center mb-16">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs font-medium mb-4">
                  <Crown className="w-3 h-3" /> Rôles
                </span>
                <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                  Chacun son <span className="text-gradient">espace</span>
                </h2>
                <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
                  Sélectionnez votre rôle pour découvrir l'expérience qui vous est dédiée.
                </p>
              </div>
            </Reveal>

            {/* Role selector tabs */}
            <Reveal delay={80}>
              <div className="flex flex-wrap justify-center gap-2 mb-10">
                {ROLES_DATA.map((r, i) => (
                  <button
                    key={r.role}
                    onClick={() => setActiveRole(i)}
                    className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-300 ${
                      activeRole === i
                        ? 'glass-strong text-primary-700 dark:text-primary-400 shadow-glow'
                        : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-white/50 dark:hover:bg-gray-800/50'
                    }`}
                  >
                    <r.icon className="w-4 h-4" />
                    {r.role}
                  </button>
                ))}
              </div>
            </Reveal>

            {/* Active role detail */}
            <Reveal delay={120}>
              <div className="glass-strong rounded-3xl p-8 sm:p-10 lg:p-12 relative overflow-hidden">
                <div className="absolute -top-20 -right-20 w-64 h-64 rounded-full bg-primary-500/[0.05] blur-3xl" />

                <div className="relative grid lg:grid-cols-2 gap-10 items-center">
                  <div>
                    <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${ROLES_DATA[activeRole].gradient} text-white flex items-center justify-center shadow-xl mb-6 transition-all duration-500`}>
                      {(() => { const Icon = ROLES_DATA[activeRole].icon; return <Icon className="w-8 h-8" />; })()}
                    </div>
                    <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border ${ROLES_DATA[activeRole].chip} mb-4`}>
                      {ROLES_DATA[activeRole].role}
                    </span>
                    <h3 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white font-display mb-4">
                      Espace {ROLES_DATA[activeRole].role}
                    </h3>
                    <p className="text-base text-gray-500 dark:text-gray-400 leading-relaxed mb-8">
                      {ROLES_DATA[activeRole].desc}
                    </p>
                    <Link to="/login" className="btn-primary btn-lg group">
                      Accéder à cet espace
                      <ArrowRight className="w-4 h-4 transition-transform duration-300 group-hover:translate-x-1" />
                    </Link>
                  </div>

                  <div className="space-y-3">
                    {ROLES_DATA[activeRole].features.map((feat, fi) => (
                      <div key={fi} className="flex items-center gap-3 p-3 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-white/50 dark:border-white/[0.05]">
                        <CheckCircle2 className="w-5 h-5 text-primary-500 flex-shrink-0" />
                        <span className="text-sm font-medium text-gray-700 dark:text-gray-200">{feat}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </Reveal>
          </div>
        </section>

        {/* ── Section 08: Avant / Après ── */}
        <section id="before-after" className="py-24 sm:py-32 bg-gradient-to-b from-white to-gray-50/50 dark:from-gray-950 dark:to-gray-900/50">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="text-center mb-16">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-500/10 border border-primary-500/20 text-primary-600 dark:text-primary-400 text-xs font-medium mb-4">
                  <ArrowUpRight className="w-3 h-3" /> Transformation
                </span>
                <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                  Avant <span className="text-gradient">Discipolat</span>
                </h2>
              </div>
            </Reveal>

            <div className="grid lg:grid-cols-2 gap-6 max-w-5xl mx-auto">
              <Reveal delay={80}>
                <div className="rounded-3xl p-8 bg-red-500/[0.04] dark:bg-red-500/[0.06] border border-red-500/10">
                  <div className="flex items-center gap-3 mb-6">
                    <div className="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center">
                      <XIcon className="w-5 h-5 text-red-500" />
                    </div>
                    <h3 className="text-lg font-bold text-gray-900 dark:text-white">Avant</h3>
                  </div>
                  <div className="space-y-3">
                    {BEFORE_AFTER.before.map((item, i) => (
                      <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-white/60 dark:bg-gray-800/40">
                        <XIcon className="w-4 h-4 text-red-400 flex-shrink-0" />
                        <span className="text-sm text-gray-600 dark:text-gray-300">{item}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </Reveal>

              <Reveal delay={160}>
                <div className="rounded-3xl p-8 bg-primary-500/[0.04] dark:bg-primary-500/[0.06] border border-primary-500/10">
                  <div className="flex items-center gap-3 mb-6">
                    <div className="w-10 h-10 rounded-xl bg-primary-500/10 flex items-center justify-center">
                      <CheckCircle2 className="w-5 h-5 text-primary-500" />
                    </div>
                    <h3 className="text-lg font-bold text-gray-900 dark:text-white">Après</h3>
                  </div>
                  <div className="space-y-3">
                    {BEFORE_AFTER.after.map((item, i) => (
                      <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-white/60 dark:bg-gray-800/40">
                        <CheckCircle2 className="w-4 h-4 text-primary-500 flex-shrink-0" />
                        <span className="text-sm text-gray-600 dark:text-gray-300">{item}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </Reveal>
            </div>
          </div>
        </section>

        {/* ── Section 09: Statistiques ── */}
        <section className="py-20 sm:py-28">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-6">
              {STATS_DATA.map((s, i) => (
                <Reveal key={s.label} delay={i * 80}>
                  <div className="glass-card p-6 sm:p-8 text-center group hover:-translate-y-1 transition-all duration-300">
                    <div className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-mono mb-2">
                      <StaticCountUp end={s.value} suffix={s.suffix} />
                    </div>
                    <p className="text-sm text-gray-500 dark:text-gray-400">{s.label}</p>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </section>

        {/* ── Section 10: Comment ça marche ── */}
        <section id="how-it-works" className="py-24 sm:py-32 bg-gradient-to-b from-gray-50/50 to-white dark:from-gray-900/50 dark:to-gray-950">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="text-center mb-16">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-gold-500/10 border border-gold-500/20 text-gold-600 dark:text-gold-400 text-xs font-medium mb-4">
                  <Sprout className="w-3 h-3" /> Parcours
                </span>
                <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                  En 4 étapes vers la <span className="text-gradient-gold">croissance</span>
                </h2>
              </div>
            </Reveal>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              {STEPS_DATA.map((s, i) => (
                <Reveal key={s.step} delay={i * 90}>
                  <div className="relative glass-card p-7 h-full group hover:-translate-y-1 transition-all duration-300">
                    <div className="absolute -top-3 right-5 text-5xl font-bold text-gray-100 dark:text-gray-800/50 font-display transition-colors duration-500 group-hover:text-primary-100 dark:group-hover:text-primary-900/20">
                      {s.step}
                    </div>
                    <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${s.color} text-white flex items-center justify-center shadow-lg mb-5 transition-transform duration-300 group-hover:scale-110`}>
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

        {/* ── Section 11: Personnalisation ── */}
        <section id="customization" className="py-24 sm:py-32">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid lg:grid-cols-2 gap-16 items-center">
              <Reveal>
                <div>
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-gold-500/10 border border-gold-500/20 text-gold-600 dark:text-gold-400 text-xs font-medium mb-4">
                    <Settings className="w-3 h-3" /> Personnalisation
                  </span>
                  <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight leading-tight">
                    Chaque église est <span className="text-gradient-gold">unique</span>.
                  </h2>
                  <p className="mt-5 text-base text-gray-500 dark:text-gray-400 leading-relaxed">
                    Discipolat s'adapte à votre organisation. Configurez couleurs, logos, départements,
                    rôles, permissions et fonctionnalités sans écrire une seule ligne de code.
                  </p>
                  <p className="mt-3 text-sm font-medium text-primary-600 dark:text-primary-400">
                    Chaque église fonctionne différemment. Discipolat s'adapte à la vôtre.
                  </p>
                </div>
              </Reveal>

              <Reveal delay={120}>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {CUSTOMIZATION_ITEMS.map((c, i) => (
                    <div key={i} className="flex items-center gap-3 p-4 rounded-xl glass-card hover:-translate-y-0.5 transition-all duration-200">
                      <div className="w-10 h-10 rounded-xl bg-gold-500/[0.08] flex items-center justify-center flex-shrink-0">
                        <c.icon className="w-5 h-5 text-gold-600 dark:text-gold-400" />
                      </div>
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-200">{c.text}</span>
                    </div>
                  ))}
                </div>
              </Reveal>
            </div>
          </div>
        </section>

        {/* ── Section 12: Sécurité ── */}
        <section id="security" className="py-24 sm:py-32 bg-gradient-to-b from-gray-50/50 to-white dark:from-gray-900/50 dark:to-gray-950">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="text-center mb-16">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-violet-500/10 border border-violet-500/20 text-violet-600 dark:text-violet-400 text-xs font-medium mb-4">
                  <Shield className="w-3 h-3" /> Sécurité
                </span>
                <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight">
                  Vos données, <span className="text-gradient">protégées</span>
                </h2>
                <p className="mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
                  Sécurité de niveau entreprise, pensée pour rassurer les responsables d'églises.
                </p>
              </div>
            </Reveal>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
              {SECURITY_FEATURES.map((s, i) => (
                <Reveal key={s.title} delay={i * 70}>
                  <div className="glass-card p-6 h-full group hover:-translate-y-1 transition-all duration-300">
                    <div className="w-11 h-11 rounded-xl bg-violet-500/10 flex items-center justify-center mb-4 transition-transform duration-300 group-hover:scale-110">
                      <s.icon className="w-5 h-5 text-violet-600 dark:text-violet-400" />
                    </div>
                    <h3 className="text-base font-semibold text-gray-900 dark:text-gray-100 mb-2">{s.title}</h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{s.desc}</p>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </section>

        {/* ── Section 13: Multi-device ── */}
        <section className="py-20 sm:py-28">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="glass-strong rounded-3xl p-10 sm:p-16 text-center relative overflow-hidden">
                <div className="absolute -top-20 -right-20 w-64 h-64 rounded-full bg-primary-500/[0.05] blur-3xl" />
                <div className="absolute -bottom-24 -left-16 w-72 h-72 rounded-full bg-gold-500/[0.05] blur-3xl" />

                <div className="relative">
                  <div className="flex items-center justify-center gap-6 mb-8">
                    <div className="w-14 h-14 rounded-2xl bg-primary-500/10 flex items-center justify-center animate-float">
                      <Smartphone className="w-7 h-7 text-primary-600 dark:text-primary-400" />
                    </div>
                    <div className="w-18 h-14 rounded-2xl bg-primary-500/10 flex items-center justify-center animate-float" style={{ animationDelay: '0.5s' }}>
                      <Tablet className="w-7 h-7 text-primary-600 dark:text-primary-400" />
                    </div>
                    <div className="w-22 h-14 rounded-2xl bg-primary-500/10 flex items-center justify-center animate-float" style={{ animationDelay: '1s' }}>
                      <Monitor className="w-7 h-7 text-primary-600 dark:text-primary-400" />
                    </div>
                  </div>
                  <h2 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white font-display mb-3">
                    Votre église vous accompagne partout
                  </h2>
                  <p className="text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-lg mx-auto">
                    Accédez à votre espace depuis votre téléphone, tablette ou ordinateur. L'expérience est optimale sur tous les écrans.
                  </p>
                </div>
              </div>
            </Reveal>
          </div>
        </section>

        {/* ── Section 14: CTA Final ── */}
        <section className="py-20 sm:py-28">
          <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
            <Reveal>
              <div className="relative">
                <div className="absolute -inset-1 rounded-[2rem] bg-gradient-to-br from-primary-500/30 via-transparent to-gold-500/30 opacity-60 blur-xl animate-pulse-soft" />
                <div className="relative glass-strong rounded-[2rem] p-10 sm:p-16 text-center overflow-hidden">
                  <div className="absolute -top-20 -right-20 w-64 h-64 rounded-full bg-primary-500/[0.06] blur-3xl" />
                  <div className="absolute -bottom-24 -left-16 w-72 h-72 rounded-full bg-gold-500/[0.06] blur-3xl" />

                  <div className="relative">
                    <div className="w-16 h-16 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-glow animate-float">
                      <CheckCircle2 className="w-8 h-8 text-white" />
                    </div>
                    <h2 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight text-balance">
                      Votre église mérite mieux qu'une multitude d'outils dispersés.
                    </h2>
                    <p className="mt-5 text-sm sm:text-base text-gray-500 dark:text-gray-400 max-w-xl mx-auto">
                      Commencez dès aujourd'hui ou demandez une démonstration personnalisée.
                    </p>
                    <div className="mt-8 flex flex-wrap items-center justify-center gap-3.5">
                      <Link to="/login" className="btn-primary btn-lg group">
                        Commencer maintenant
                        <ArrowRight className="w-4 h-4 transition-transform duration-300 group-hover:translate-x-1" />
                      </Link>
                      <Link to="/login" className="btn-secondary btn-lg">
                        Demander une démonstration
                      </Link>
                    </div>
                  </div>
                </div>
              </div>
            </Reveal>
          </div>
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

/* ============================================================
   Sub-components
   ============================================================ */

function XIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}

function StaticCountUp({ end, suffix = '' }: { end: number; suffix?: string }) {
  const [value, setValue] = useState(0);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) { setValue(end); return; }
    if (typeof IntersectionObserver === 'undefined') { setValue(end); return; }
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          observer.disconnect();
          const start = performance.now();
          const tick = (now: number) => {
            const progress = Math.min((now - start) / 1800, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            setValue(Math.round(eased * end));
            if (progress < 1) requestAnimationFrame(tick);
          };
          requestAnimationFrame(tick);
        }
      },
      { threshold: 0.3 },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [end]);

  return <span ref={ref}>{value}{suffix}</span>;
}
