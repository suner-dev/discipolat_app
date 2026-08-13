import type { LucideIcon } from 'lucide-react';
import {
  LayoutDashboard,
  Search,
  Map as MapIcon,
  Heart,
  Users,
  Activity,
  Sprout,
  Target,
  DoorOpen,
  AlertTriangle,
  Building2,
  FileText,
  BookOpen,
  Calendar,
  BarChart3,
  Shield,
  Star as StarIcon,
  MessageSquare,
  MessagesSquare,
  Bell,
  BellRing,
  FolderOpen,
  GraduationCap,
  Trophy,
  CalendarClock,
  UserCog,
  User,
  Sparkles,
  Church,
  HandHeart,
  ArrowLeftRight,
  Workflow,
  Palette,
  Boxes,
  Menu as MenuList,
  SlidersHorizontal,
} from 'lucide-react';
import type { UserRole } from '@/types';

/* ============================================================================
 * ESPACES MÉTIERS — chaque rôle est un environnement de travail indépendant.
 * Le changement de rôle = changement complet de contexte métier.
 * ========================================================================== */

export interface WorkspaceNavItem {
  name: string;
  href: string;
  icon: LucideIcon;
  subtitle: string;
}

export interface WorkspaceSection {
  title: string;
  items: WorkspaceNavItem[];
}

/** Tableau de bord racine de chaque espace métier. */
export const WORKSPACE_HOME: Record<UserRole, string> = {
  ADMIN: '/dashboard',
  PASTEUR: '/dashboard',
  RESPONSABLE: '/dashboard/responsable',
  CHEF_DE_FAMILLE: '/dashboard/chef-famille',
  FAISEUR: '/crm/faiseur',
  MEMBRE: '/dashboard/membre',
};

/** Métadonnées visuelles de chaque espace métier. */
export const ROLE_META: Record<UserRole, { label: string; tagline: string; gradient: string }> = {
  ADMIN: {
    label: 'Admin',
    tagline: 'Administration du système',
    gradient: 'from-red-500 to-rose-600',
  },
  PASTEUR: {
    label: 'Pasteur',
    tagline: 'Centre de commandement',
    gradient: 'from-primary-500 to-emerald-600',
  },
  RESPONSABLE: {
    label: 'Responsable',
    tagline: 'Gestion des départements',
    gradient: 'from-amber-500 to-orange-600',
  },
  CHEF_DE_FAMILLE: {
    label: 'Chef de famille',
    tagline: 'Gestion de la famille',
    gradient: 'from-gold-500 to-amber-600',
  },
  FAISEUR: {
    label: 'Faiseur',
    tagline: 'Suivi des disciples',
    gradient: 'from-emerald-500 to-teal-600',
  },
  MEMBRE: {
    label: 'Membre',
    tagline: 'Espace personnel',
    gradient: 'from-sky-500 to-blue-600',
  },
};

/** Rôles super-utilisateurs : accès à tous les espaces. */
export function isSuperUser(role: string | null | undefined): boolean {
  return role === 'ADMIN' || role === 'PASTEUR';
}

/* ----------------------------------------------------------------------------
 * Navigation des super-utilisateurs (Admin / Pasteur) — vue complète.
 * -------------------------------------------------------------------------- */

const FULL_NAV: WorkspaceSection[] = [
  {
    title: 'Pilotage',
    items: [
      { name: 'Tableau de bord', href: '/dashboard', icon: LayoutDashboard, subtitle: "Vue d'ensemble" },
      { name: 'Pilotage Pasteur', href: '/dashboard/pasteur', icon: Sparkles, subtitle: 'Centre de commandement' },
      { name: 'Recherche', href: '/search', icon: Search, subtitle: 'Recherche intelligente' },
      { name: 'Cartographie', href: '/map', icon: MapIcon, subtitle: 'Carte des disciples' },
    ],
  },
  {
    title: 'Discipolat',
    items: [
      { name: 'Âmes', href: '/souls', icon: Heart, subtitle: 'Disciples suivis' },
      { name: 'Familles', href: '/families', icon: Users, subtitle: 'Groupes de disciples' },
      { name: 'Comparer familles', href: '/families/compare', icon: BarChart3, subtitle: 'Analyse croisée' },
      { name: 'CRM Faiseur', href: '/crm/faiseur', icon: HandHeart, subtitle: 'Suivi des disciples' },
      { name: 'Évangélisation', href: '/evangelism', icon: Sprout, subtitle: 'Pipeline de croissance' },
      { name: 'Suivis parallèles', href: '/parallel-followups', icon: Activity, subtitle: 'Accompagnements' },
      { name: 'Objectifs', href: '/objectives', icon: Target, subtitle: 'Performances mesurées' },
      { name: 'Visites', href: '/visits', icon: DoorOpen, subtitle: 'Planification & comptes rendus' },
      { name: 'Retraits', href: '/souls/retractions', icon: AlertTriangle, subtitle: 'Demandes de retrait' },
    ],
  },
  {
    title: 'Structures & rapports',
    items: [
      { name: 'Départements', href: '/departments', icon: Building2, subtitle: 'Structure de l’église' },
      { name: 'Dashboard Responsable', href: '/dashboard/responsable', icon: Building2, subtitle: 'Mon département' },
      { name: 'Dashboard Chef', href: '/dashboard/chef-famille', icon: Users, subtitle: 'Ma famille' },
      { name: 'Rapports', href: '/reports', icon: FileText, subtitle: 'Hebdomadaires' },
      { name: 'Rapport faiseur', href: '/reports/maker', icon: FileText, subtitle: 'Rapport du faiseur' },
      { name: 'Rapport famille', href: '/reports/family', icon: FileText, subtitle: 'Rapport de famille' },
      { name: 'Aide urgente', href: '/reports/urgent-aid', icon: AlertTriangle, subtitle: "Demandes d'aide" },
    ],
  },
  {
    title: 'Vie de l’église',
    items: [
      { name: 'Prières', href: '/prayers', icon: BookOpen, subtitle: 'Sujets & témoignages' },
      { name: 'Espaces prière', href: '/prayers/spaces', icon: Shield, subtitle: 'Par niveau de visibilité' },
      { name: 'Actions de grâce', href: '/prayers/actions-de-grace', icon: Heart, subtitle: 'Prières exaucées' },
      { name: 'Événements', href: '/events', icon: Calendar, subtitle: 'Calendrier' },
      { name: 'Programme', href: '/events/program', icon: Calendar, subtitle: 'Programme hebdomadaire' },
      { name: 'Statistiques événements', href: '/events/statistics', icon: BarChart3, subtitle: 'Indicateurs' },
      { name: 'Types de programmes', href: '/programs', icon: Calendar, subtitle: 'Configuration des présences' },
    ],
  },
  {
    title: 'Transferts',
    items: [
      { name: 'Demandes de transfert', href: '/transfers', icon: ArrowLeftRight, subtitle: 'Workflow intelligent & validations' },
      { name: 'Configuration workflow', href: '/admin/transfers', icon: Workflow, subtitle: 'Circuits de validation' },
    ],
  },
  {
    title: 'Engagement & outils',
    items: [
      { name: 'Évaluations', href: '/evaluations', icon: StarIcon, subtitle: 'Anonymes & feedback' },
      { name: 'Demandes membres', href: '/members/requests', icon: MessageSquare, subtitle: 'Suggestions & présences' },
      { name: 'Documents', href: '/documents', icon: FolderOpen, subtitle: 'Fichiers & rapports' },
      { name: 'Notifications', href: '/notifications', icon: BellRing, subtitle: 'Centre de notifications' },
      { name: 'Alertes', href: '/alerts', icon: Bell, subtitle: 'Centre d\'alertes' },
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
      { name: 'Formations', href: '/trainings', icon: GraduationCap, subtitle: 'Cours, quiz & certificats' },
      { name: 'Badges', href: '/badges', icon: Trophy, subtitle: 'Récompenses & classements' },
      { name: 'Rendez-vous', href: '/appointments', icon: CalendarClock, subtitle: 'Prises de RDV & validations' },
    ],
  },
  {
    title: 'Administration',
    items: [
      { name: 'Centre d\'administration', href: '/admin', icon: SlidersHorizontal, subtitle: 'Toute la configuration' },
      { name: 'Identité & marque', href: '/admin/settings', icon: Palette, subtitle: 'Nom, logo & couleurs' },
      { name: 'Utilisateurs', href: '/users', icon: UserCog, subtitle: 'Gestion des comptes' },
      { name: 'Audit', href: '/audit', icon: Activity, subtitle: 'Journal de bord' },
      { name: 'Permissions', href: '/permissions', icon: Shield, subtitle: 'Matrice des rôles' },
      { name: 'Modules', href: '/admin/modules', icon: Boxes, subtitle: 'Activer / désactiver' },
      { name: 'Menus', href: '/admin/menus', icon: MenuList, subtitle: 'Configurer la navigation' },
      { name: 'Champs personnalisés', href: '/admin/custom-fields', icon: FileText, subtitle: 'Champs des entités' },
      { name: 'Dictionnaires', href: '/admin/dictionaries', icon: BookOpen, subtitle: 'Types, statuts & catégories' },
    ],
  },
];

/* ----------------------------------------------------------------------------
 * Navigation RESPONSABLE — logiciel de gestion des départements (HRM église).
 * Aucun menu discipolat / familles / âmes / faiseurs.
 * -------------------------------------------------------------------------- */

const RESPONSABLE_NAV: WorkspaceSection[] = [
  {
    title: 'Pilotage',
    items: [
      { name: 'Dashboard Responsable', href: '/dashboard/responsable', icon: LayoutDashboard, subtitle: 'Mes départements' },
    ],
  },
  {
    title: 'Gestion du département',
    items: [
      { name: 'Départements', href: '/departments', icon: Building2, subtitle: 'Structure & membres' },
      { name: 'Membres', href: '/users', icon: UserCog, subtitle: 'Comptes du département' },
      { name: 'Présences & demandes', href: '/members/requests', icon: MessageSquare, subtitle: 'Fiche hebdomadaire' },
      { name: 'Transferts', href: '/transfers', icon: ArrowLeftRight, subtitle: 'Demandes & validations' },
    ],
  },
  {
    title: 'Suivi & reporting',
    items: [
      { name: 'Rapports', href: '/reports', icon: FileText, subtitle: 'Hebdomadaires' },
      { name: 'Aide urgente', href: '/reports/urgent-aid', icon: AlertTriangle, subtitle: "Demandes d'aide" },
      { name: 'Notifications', href: '/notifications', icon: BellRing, subtitle: 'Centre de notifications' },
      { name: 'Alertes', href: '/alerts', icon: Bell, subtitle: 'Centre d\'alertes' },
      { name: 'Événements', href: '/events', icon: Calendar, subtitle: 'Calendrier du département' },
    ],
  },
  {
    title: 'Outils',
    items: [
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
      { name: 'Documents', href: '/documents', icon: FolderOpen, subtitle: 'Fichiers & rapports' },
      { name: 'Profil', href: '/profile', icon: User, subtitle: 'Mes informations' },
    ],
  },
];

/* ----------------------------------------------------------------------------
 * Navigation FAISEUR — uniquement le discipolat (ses disciples, ses rapports).
 * -------------------------------------------------------------------------- */

const FAISEUR_NAV: WorkspaceSection[] = [
  {
    title: 'Mon espace',
    items: [
      { name: 'CRM Faiseur', href: '/crm/faiseur', icon: HandHeart, subtitle: 'Suivi de mes disciples' },
      { name: 'Mes disciples', href: '/souls', icon: Heart, subtitle: 'Disciples suivis' },
    ],
  },
  {
    title: 'Suivi',
    items: [
      { name: 'Rapports', href: '/reports', icon: FileText, subtitle: 'Hebdomadaires' },
      { name: 'Rapport faiseur', href: '/reports/maker', icon: FileText, subtitle: 'Mon rapport de la semaine' },
      { name: 'Prières', href: '/prayers', icon: BookOpen, subtitle: 'Sujets & témoignages' },
      { name: 'Visites', href: '/visits', icon: DoorOpen, subtitle: 'Planification & comptes rendus' },
      { name: 'Évangélisation', href: '/evangelism', icon: Sprout, subtitle: 'Pipeline de croissance' },
      { name: 'Suivis parallèles', href: '/parallel-followups', icon: Activity, subtitle: 'Accompagnements' },
      { name: 'Objectifs', href: '/objectives', icon: Target, subtitle: 'Mes objectifs' },
      { name: 'Recherche', href: '/search', icon: Search, subtitle: 'Recherche intelligente' },
    ],
  },
  {
    title: 'Réseau & outils',
    items: [
      { name: 'Notifications', href: '/notifications', icon: BellRing, subtitle: 'Centre de notifications' },
      { name: 'Alertes', href: '/alerts', icon: Bell, subtitle: 'Centre d\'alertes' },
      { name: 'Événements', href: '/events', icon: Calendar, subtitle: 'Calendrier' },
      { name: 'Transferts', href: '/transfers', icon: ArrowLeftRight, subtitle: 'Demandes & validations' },
      { name: 'Documents', href: '/documents', icon: FolderOpen, subtitle: 'Fichiers & rapports' },
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
      { name: 'Profil', href: '/profile', icon: User, subtitle: 'Mes informations' },
    ],
  },
];

/* ----------------------------------------------------------------------------
 * Navigation CHEF DE FAMILLE — gestion de sa famille de disciples.
 * -------------------------------------------------------------------------- */

const CHEF_FAMILLE_NAV: WorkspaceSection[] = [
  {
    title: 'Ma famille',
    items: [
      { name: 'Dashboard Chef', href: '/dashboard/chef-famille', icon: LayoutDashboard, subtitle: 'Ma famille' },
      { name: 'Familles', href: '/families', icon: Users, subtitle: 'Groupes de disciples' },
      { name: 'Disciples', href: '/souls', icon: Heart, subtitle: 'Disciples de la famille' },
    ],
  },
  {
    title: 'Suivi',
    items: [
      { name: 'Rapports', href: '/reports', icon: FileText, subtitle: 'Hebdomadaires' },
      { name: 'Rapport famille', href: '/reports/family', icon: FileText, subtitle: 'Rapport de famille' },
      { name: 'Évaluations', href: '/evaluations', icon: StarIcon, subtitle: 'Anonymes & feedback' },
      { name: 'Prières', href: '/prayers', icon: BookOpen, subtitle: 'Sujets & témoignages' },
      { name: 'Événements', href: '/events', icon: Calendar, subtitle: 'Calendrier' },
      { name: 'Notifications', href: '/notifications', icon: BellRing, subtitle: 'Centre de notifications' },
      { name: 'Alertes', href: '/alerts', icon: Bell, subtitle: 'Centre d\'alertes' },
      { name: 'Demandes membres', href: '/members/requests', icon: MessageSquare, subtitle: 'Suggestions & présences' },
      { name: 'Transferts', href: '/transfers', icon: ArrowLeftRight, subtitle: 'Demandes & validations' },
    ],
  },
  {
    title: 'Outils',
    items: [
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
      { name: 'Documents', href: '/documents', icon: FolderOpen, subtitle: 'Fichiers & rapports' },
      { name: 'Profil', href: '/profile', icon: User, subtitle: 'Mes informations' },
    ],
  },
];

/* ----------------------------------------------------------------------------
 * Navigation MEMBRE — espace personnel.
 * -------------------------------------------------------------------------- */

const MEMBRE_NAV: WorkspaceSection[] = [
  {
    title: 'Mon espace',
    items: [
      { name: 'Espace Membre', href: '/dashboard/membre', icon: LayoutDashboard, subtitle: 'Mes informations' },
      { name: 'Profil', href: '/profile', icon: User, subtitle: 'Mes données' },
    ],
  },
  {
    title: 'Découverte',
    items: [
      { name: 'Formations', href: '/trainings', icon: GraduationCap, subtitle: 'Cours, quiz & certificats' },
      { name: 'Badges', href: '/badges', icon: Trophy, subtitle: 'Récompenses & classements' },
      { name: 'Rendez-vous', href: '/appointments', icon: CalendarClock, subtitle: 'Prises de RDV' },
    ],
  },
  {
    title: 'Communauté',
    items: [
      { name: 'Notifications', href: '/notifications', icon: BellRing, subtitle: 'Centre de notifications' },
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
    ],
  },
];

/**
 * Routes réservées à l'Administration (rôle ADMIN uniquement).
 * Le Pasteur voit la vue complète SAUF ces écrans : les cliquer le ferait
 * rediriger vers /dashboard sans explication (bouton mort).
 */
const ADMIN_ONLY_HREFS = [
  '/permissions',
  '/admin',
  '/admin/settings',
  '/admin/modules',
  '/admin/menus',
  '/admin/custom-fields',
  '/admin/dictionaries',
];

/** Retourne les menus de l'espace métier correspondant au rôle actif. */
export function navForRole(activeRole: string | null | undefined): WorkspaceSection[] {
  switch (activeRole) {
    case 'RESPONSABLE':
      return RESPONSABLE_NAV;
    case 'FAISEUR':
      return FAISEUR_NAV;
    case 'CHEF_DE_FAMILLE':
      return CHEF_FAMILLE_NAV;
    case 'MEMBRE':
      return MEMBRE_NAV;
    case 'ADMIN':
      return FULL_NAV;
    case 'PASTEUR':
      // Pasteur = vue complète, sans les écrans réservés à l'Admin.
      return FULL_NAV.map((s) => ({
        ...s,
        items: s.items.filter((i) => !ADMIN_ONLY_HREFS.includes(i.href)),
      })).filter((s) => s.items.length > 0);
    default:
      return FULL_NAV;
  }
}

/** Icône de l'espace métier (affichée dans la barre de rôle). */
export function roleIcon(activeRole: string | null | undefined): LucideIcon {
  switch (activeRole) {
    case 'RESPONSABLE':
      return Building2;
    case 'FAISEUR':
      return HandHeart;
    case 'CHEF_DE_FAMILLE':
      return Users;
    case 'MEMBRE':
      return User;
    case 'PASTEUR':
      return Church;
    case 'ADMIN':
      return Shield;
    default:
      return Church;
  }
}
