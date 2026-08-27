import type { LucideIcon } from 'lucide-react';
import {
  Heart, Users, Building2, UsersRound, FileBarChart, UserCheck, CalendarCheck,
  AlertTriangle, PieChart, FileText, Lock, Eye, Fingerprint, KeyRound,
  Church, Sparkles, Settings, Target, Activity, Crown, HandHeart,
  FolderOpen, FileX, MessageCircle, HeartPulse, ShieldCheck, DatabaseBackup,
} from 'lucide-react';

/* ============================================================================
 * LANDING — Données partagées (icônes + styles).
 * Le texte est géré par i18n (t('landing.*')) pour respecter la langue active.
 * ========================================================================== */

export interface LandingIconItem {
  titleKey: string;
  descKey: string;
  icon: LucideIcon;
  gradient: string;
}

/* ---------- Le problème ---------- */
export const PROBLEMS: LandingIconItem[] = [
  { titleKey: 'landing.problem.files', descKey: 'landing.problem.filesDesc', icon: FolderOpen, gradient: 'from-rose-500 to-pink-600' },
  { titleKey: 'landing.problem.manual', descKey: 'landing.problem.manualDesc', icon: UsersRound, gradient: 'from-amber-500 to-orange-600' },
  { titleKey: 'landing.problem.visibility', descKey: 'landing.problem.visibilityDesc', icon: Eye, gradient: 'from-violet-500 to-purple-600' },
  { titleKey: 'landing.problem.lateAlerts', descKey: 'landing.problem.lateAlertsDesc', icon: AlertTriangle, gradient: 'from-red-500 to-rose-600' },
  { titleKey: 'landing.problem.fragmented', descKey: 'landing.problem.fragmentedDesc', icon: MessageCircle, gradient: 'from-sky-500 to-blue-600' },
  { titleKey: 'landing.problem.dataLoss', descKey: 'landing.problem.dataLossDesc', icon: FileX, gradient: 'from-slate-500 to-slate-700' },
];

/* ---------- La solution ---------- */
export const SOLUTION_FEATURES: string[] = [
  'landing.solution.1', 'landing.solution.2', 'landing.solution.3',
  'landing.solution.4', 'landing.solution.5', 'landing.solution.6',
];

export interface ModuleCard {
  nameKey: string;
  descKey: string;
  icon: LucideIcon;
  gradient: string;
}

/* ---------- Une seule plateforme ---------- */
export const MODULES: ModuleCard[] = [
  { nameKey: 'landing.modules.discipolat', descKey: 'landing.modules.discipolatDesc', icon: Heart, gradient: 'from-rose-500 to-pink-600' },
  { nameKey: 'landing.modules.members', descKey: 'landing.modules.membersDesc', icon: Users, gradient: 'from-emerald-500 to-teal-600' },
  { nameKey: 'landing.modules.departments', descKey: 'landing.modules.departmentsDesc', icon: Building2, gradient: 'from-amber-500 to-orange-600' },
  { nameKey: 'landing.modules.families', descKey: 'landing.modules.familiesDesc', icon: UsersRound, gradient: 'from-gold-500 to-amber-600' },
  { nameKey: 'landing.modules.reports', descKey: 'landing.modules.reportsDesc', icon: FileBarChart, gradient: 'from-blue-500 to-indigo-600' },
  { nameKey: 'landing.modules.prayers', descKey: 'landing.modules.prayersDesc', icon: HeartPulse, gradient: 'from-violet-500 to-purple-600' },
  { nameKey: 'landing.modules.attendance', descKey: 'landing.modules.attendanceDesc', icon: UserCheck, gradient: 'from-sky-500 to-cyan-600' },
  { nameKey: 'landing.modules.events', descKey: 'landing.modules.eventsDesc', icon: CalendarCheck, gradient: 'from-green-500 to-emerald-600' },
  { nameKey: 'landing.modules.alerts', descKey: 'landing.modules.alertsDesc', icon: AlertTriangle, gradient: 'from-red-500 to-rose-600' },
  { nameKey: 'landing.modules.stats', descKey: 'landing.modules.statsDesc', icon: PieChart, gradient: 'from-fuchsia-500 to-purple-600' },
];

export interface RoleDef {
  id: string;
  roleKey: string;
  descKey: string;
  icon: LucideIcon;
  gradient: string;
  featKeys: string[];
}

/* ---------- Espaces rôles ---------- */
export const ROLES: RoleDef[] = [
  {
    id: 'pasteur', roleKey: 'landing.roles.pasteur', descKey: 'landing.roles.pasteurDesc',
    icon: Crown, gradient: 'from-primary-500 to-emerald-600',
    featKeys: ['landing.roles.f1', 'landing.roles.f2', 'landing.roles.f3', 'landing.roles.f4'],
  },
  {
    id: 'responsable', roleKey: 'landing.roles.responsable', descKey: 'landing.roles.responsableDesc',
    icon: Building2, gradient: 'from-amber-500 to-orange-600',
    featKeys: ['landing.roles.f5', 'landing.roles.f6', 'landing.roles.f7', 'landing.roles.f8'],
  },
  {
    id: 'chef', roleKey: 'landing.roles.chef', descKey: 'landing.roles.chefDesc',
    icon: Users, gradient: 'from-gold-500 to-amber-600',
    featKeys: ['landing.roles.f9', 'landing.roles.f10', 'landing.roles.f11', 'landing.roles.f12'],
  },
  {
    id: 'faiseur', roleKey: 'landing.roles.faiseur', descKey: 'landing.roles.faiseurDesc',
    icon: HandHeart, gradient: 'from-emerald-500 to-teal-600',
    featKeys: ['landing.roles.f13', 'landing.roles.f14', 'landing.roles.f15', 'landing.roles.f16'],
  },
  {
    id: 'membre', roleKey: 'landing.roles.membre', descKey: 'landing.roles.membreDesc',
    icon: Heart, gradient: 'from-rose-500 to-pink-600',
    featKeys: ['landing.roles.f17', 'landing.roles.f18', 'landing.roles.f19', 'landing.roles.f20'],
  },
];

/* ---------- Comment ça marche ---------- */
export const STEPS: ModuleCard[] = [
  { nameKey: 'landing.how.s1Title', descKey: 'landing.how.s1Desc', icon: Settings, gradient: 'from-primary-500 to-primary-700' },
  { nameKey: 'landing.how.s2Title', descKey: 'landing.how.s2Desc', icon: UsersRound, gradient: 'from-amber-500 to-orange-600' },
  { nameKey: 'landing.how.s3Title', descKey: 'landing.how.s3Desc', icon: Activity, gradient: 'from-emerald-500 to-teal-600' },
  { nameKey: 'landing.how.s4Title', descKey: 'landing.how.s4Desc', icon: Target, gradient: 'from-violet-500 to-purple-600' },
];

/* ---------- Statistiques (démo) ---------- */
export const STATS: { value: number; suffix: string; key: string }[] = [
  { value: 6, suffix: '', key: 'landing.stats.s1' },
  { value: 100, suffix: '+', key: 'landing.stats.s2' },
  { value: 5, suffix: '', key: 'landing.stats.s3' },
  { value: 24, suffix: '/7', key: 'landing.stats.s4' },
];

/* ---------- Avant / Après ---------- */
export const BEFORE: string[] = [
  'landing.beforeAfter.b1', 'landing.beforeAfter.b2', 'landing.beforeAfter.b3',
  'landing.beforeAfter.b4', 'landing.beforeAfter.b5', 'landing.beforeAfter.b6',
];
export const AFTER: string[] = [
  'landing.beforeAfter.a1', 'landing.beforeAfter.a2', 'landing.beforeAfter.a3',
  'landing.beforeAfter.a4', 'landing.beforeAfter.a5', 'landing.beforeAfter.a6',
];

/* ---------- Sécurité ---------- */
export const SECURITY: LandingIconItem[] = [
  { titleKey: 'landing.security.s1', descKey: 'landing.security.s1Desc', icon: Lock, gradient: 'from-violet-500 to-purple-600' },
  { titleKey: 'landing.security.s2', descKey: 'landing.security.s2Desc', icon: Eye, gradient: 'from-sky-500 to-blue-600' },
  { titleKey: 'landing.security.s3', descKey: 'landing.security.s3Desc', icon: Fingerprint, gradient: 'from-emerald-500 to-teal-600' },
  { titleKey: 'landing.security.s4', descKey: 'landing.security.s4Desc', icon: ShieldCheck, gradient: 'from-rose-500 to-pink-600' },
  { titleKey: 'landing.security.s5', descKey: 'landing.security.s5Desc', icon: KeyRound, gradient: 'from-amber-500 to-orange-600' },
  { titleKey: 'landing.security.s6', descKey: 'landing.security.s6Desc', icon: DatabaseBackup, gradient: 'from-indigo-500 to-violet-600' },
];

export interface CustomItem { textKey: string; icon: LucideIcon; }

/* ---------- Personnalisation ---------- */
export const CUSTOMIZATION: CustomItem[] = [
  { textKey: 'landing.custom.c1', icon: Church },
  { textKey: 'landing.custom.c2', icon: Sparkles },
  { textKey: 'landing.custom.c3', icon: Building2 },
  { textKey: 'landing.custom.c4', icon: Users },
  { textKey: 'landing.custom.c5', icon: Settings },
  { textKey: 'landing.custom.c6', icon: FileText },
];

/* ---------- Écosystème ---------- */
export const ECOSYSTEM: { levelKey: string; descKey: string; icon: LucideIcon; color: string }[] = [
  { levelKey: 'landing.ecosystem.pasteur', descKey: 'landing.ecosystem.pasteurDesc', icon: Crown, color: 'from-emerald-500 to-green-600' },
  { levelKey: 'landing.ecosystem.responsable', descKey: 'landing.ecosystem.responsableDesc', icon: Building2, color: 'from-amber-500 to-orange-600' },
  { levelKey: 'landing.ecosystem.chef', descKey: 'landing.ecosystem.chefDesc', icon: Users, color: 'from-yellow-500 to-amber-600' },
  { levelKey: 'landing.ecosystem.faiseur', descKey: 'landing.ecosystem.faiseurDesc', icon: HandHeart, color: 'from-emerald-500 to-teal-600' },
  { levelKey: 'landing.ecosystem.disciple', descKey: 'landing.ecosystem.discipleDesc', icon: Heart, color: 'from-rose-500 to-pink-600' },
];