import type { LucideIcon } from 'lucide-react';
import {
  LayoutDashboard,
  Search,
  Map as MapIcon,
  Heart,
  Users,
  UsersRound,
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
  UserCheck,
  Sparkles,
  Church,
  HandHeart,
  ArrowLeftRight,
  Workflow,
  Palette,
  Boxes,
  Menu as MenuList,
  SlidersHorizontal,
  Globe,
  Server,
  ClipboardCheck,
  ClipboardList,
  ListTodo,
  Scale,
  Briefcase,
  TrendingUp,
  ListChecks,
  Zap,
  HeartPulse,
  HandCoins,
  Smartphone,
  GitBranch,
  Mic,
  Compass,
  Eye,
  Webhook as WebhookIcon,
  Plug,
  LifeBuoy,
  Route,
  Flame,
  MessageCircle,
  HelpCircle,
  ClipboardList as ClipboardListIcon,
  CalendarOff,
  Share2,
  GanttChart,
  Award,
  Code,
  ShieldCheck,
  Rocket,
  BookMarked,
  BookHeart,
  Swords,
  Contact,
  Footprints,
  Video,
  Megaphone,
  Package,
  Store,
  Users as UsersIcon,
  Brain,
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
      { name: 'Kingdom Mapping', href: '/kingdom-map', icon: Compass, subtitle: 'Heatmap & secteurs prioritaires' },
      { name: 'Assistant IA', href: '/ai-assistant', icon: Sparkles, subtitle: 'Chat IA pastoral intelligent' },
      { name: 'Observatoire santé', href: '/health-observatory', icon: HeartPulse, subtitle: 'Prédiction de décrochage' },
      { name: 'Jumeau numérique', href: '/digital-twin', icon: GitBranch, subtitle: "Simulateur de croissance" },
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
      { name: 'Rapports vocaux', href: '/voice-reports', icon: Mic, subtitle: 'Dictées terrain analysées par IA' },
      { name: 'Journal prophétique', href: '/prophetic-journal', icon: Eye, subtitle: 'Visions & rêves corrélés par IA' },
      { name: 'Suivis parallèles', href: '/parallel-followups', icon: Activity, subtitle: 'Accompagnements' },
      { name: 'Objectifs', href: '/objectives', icon: Target, subtitle: 'Performances mesurées' },
      { name: 'Visites', href: '/visits', icon: DoorOpen, subtitle: 'Planification & comptes rendus' },
      { name: 'Retraits', href: '/souls/retractions', icon: AlertTriangle, subtitle: 'Demandes de retrait' },
    ],
  },
  {
    title: 'Structures & rapports',
    items: [
      { name: 'Départements', href: '/departments', icon: Building2, subtitle: "Structure de l'église" },
      { name: 'Dashboard Responsable', href: '/dashboard/responsable', icon: Building2, subtitle: 'Mon département' },
      { name: 'Dashboard Chef', href: '/dashboard/chef-famille', icon: Users, subtitle: 'Ma famille' },
      { name: 'Rapports', href: '/reports', icon: FileText, subtitle: 'Hebdomadaires' },
      { name: 'Rapport faiseur', href: '/reports/maker', icon: FileText, subtitle: 'Rapport du faiseur' },
      { name: 'Rapport famille', href: '/reports/family', icon: FileText, subtitle: 'Rapport de famille' },
      { name: 'Aide urgente', href: '/reports/urgent-aid', icon: AlertTriangle, subtitle: "Demandes d'aide" },
    ],
  },
  {
    title: "Vie de l'église",
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
      { name: 'Alertes', href: '/alerts', icon: Bell, subtitle: "Centre d'alertes" },
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
      { name: 'Formations', href: '/trainings', icon: GraduationCap, subtitle: 'Cours, quiz & certificats' },
      { name: 'Badges', href: '/badges', icon: Trophy, subtitle: 'Récompenses & classements' },
      { name: 'Quest (XP)', href: '/quest', icon: Zap, subtitle: 'Gamification & quêtes hebdo' },
      { name: 'Tontines', href: '/tontines', icon: HandCoins, subtitle: 'Épargne solidaire' },
      { name: 'Dîmes & offrandes', href: '/giving', icon: Smartphone, subtitle: 'Mobile Money instantané' },
      { name: 'Prédicateur IA', href: '/sermon-assistant', icon: BookOpen, subtitle: 'Plans de sermons' },
      { name: 'Rendez-vous', href: '/appointments', icon: CalendarClock, subtitle: 'Prises de RDV & validations' },
      { name: 'Tickets & Support', href: '/tickets', icon: LifeBuoy, subtitle: 'Système de tickets interne' },
      { name: 'Sondages', href: '/surveys', icon: ClipboardListIcon, subtitle: 'Sondages rapides' },
      { name: 'Témoignages', href: '/testimonials', icon: Heart, subtitle: 'Galerie de témoignages' },
      { name: 'Demandes d\'absence', href: '/leave-requests', icon: CalendarOff, subtitle: 'Congés & absences' },
      { name: 'Parrainage', href: '/referrals', icon: Share2, subtitle: 'Invitez vos proches' },
      { name: 'Calendrier', href: '/calendar', icon: Calendar, subtitle: 'Sync Google/Outlook/iCal' },
      { name: 'Cercle de faiseurs', href: '/cercle-faiseurs', icon: Users, subtitle: "Espace d'entraide faiseurs" },
      { name: 'Plans de lecture biblique', href: '/bible-reading', icon: BookMarked, subtitle: 'Parcours de lecture partagés' },
      { name: 'Journal de prière', href: '/prayer-journal', icon: BookHeart, subtitle: 'Carnet personnel de prières' },
      { name: 'Défis spirituels', href: '/spiritual-challenges', icon: Swords, subtitle: 'Défis de croissance' },
      { name: 'Annuaire', href: '/directory', icon: Contact, subtitle: 'Répertoire des membres' },
      { name: 'Parcours spirituel', href: '/spiritual-journey', icon: Footprints, subtitle: 'Progression visuelle' },
      { name: 'Streaming & Live', href: '/streaming', icon: Video, subtitle: 'Cultes en direct' },
      { name: 'Diffusion', href: '/broadcast', icon: Megaphone, subtitle: 'Messages massifs' },
      { name: 'Communauté', href: '/community', icon: UsersIcon, subtitle: 'Fil social & témoignages' },
      { name: 'Prédictions IA', href: '/ai-predictions', icon: Sparkles, subtitle: 'Forecasting & analytics' },
      { name: 'Objectifs spirituels', href: '/personal-objectives', icon: Target, subtitle: 'Mes objectifs personnels' },
      { name: 'Cohésion familiale', href: '/family-cohesion', icon: Heart, subtitle: 'Santé de la famille' },
      { name: 'Plan de succession', href: '/succession', icon: GitBranch, subtitle: 'Préparer les futurs leaders' },
      { name: 'Visites pastorales', href: '/pastoral-visits', icon: Compass, subtitle: 'Planification auto' },
      { name: 'Ressources famille', href: '/family-resources', icon: FolderOpen, subtitle: 'Partage de documents' },
      { name: 'Mentorat IA', href: '/mentoring', icon: Brain, subtitle: 'Suggestions d\'accompagnement' },
      { name: 'Drill-down KPI', href: '/kpi-drilldown', icon: BarChart3, subtitle: 'Narration automatique' },
      { name: 'Insights exécutifs IA', href: '/executive-insights', icon: Sparkles, subtitle: 'Recommandations auto' },
      { name: 'Centre intelligence 50+', href: '/intelligence-center', icon: Activity, subtitle: 'KPIs temps réel' },
      { name: 'Prédictions ML', href: '/predictions-ml', icon: Brain, subtitle: 'Forecasting avancé' },
      { name: 'Analytics engagement', href: '/engagement-analytics', icon: BarChart3, subtitle: 'Métriques usage' },
      { name: 'Parcours discipolat IA', href: '/discipleship-path', icon: Route, subtitle: 'Parcours personnalisé' },
      { name: 'Défis hebdomadaires', href: '/weekly-challenges', icon: Flame, subtitle: 'Gamification spirituelle' },
      { name: 'Messages de groupe', href: '/group-messages', icon: MessageCircle, subtitle: 'Conversations d\'équipe' },
      { name: 'Annonces programmées', href: '/scheduled-announcements', icon: Megaphone, subtitle: 'Planification auto' },
      { name: 'Checklists événements', href: '/event-checklists', icon: ClipboardCheck, subtitle: 'Préparation auto' },
      { name: 'Notes IA visites', href: '/ai-visit-notes', icon: FileText, subtitle: 'Transcription auto' },
      { name: 'Mentorat inversé', href: '/reverse-mentoring', icon: HelpCircle, subtitle: 'Demander de l\'aide' },
      { name: 'Réunions famille auto', href: '/family-meetings', icon: Users, subtitle: 'Agenda généré' },
      { name: 'Modération IA', href: '/content-moderation', icon: Shield, subtitle: 'Filtre contenu' },
      { name: 'Multi-devise', href: '/currency-settings', icon: Globe, subtitle: 'Devises & fuseaux' },
      { name: 'Événements à venir', href: '/upcoming-events', icon: Calendar, subtitle: 'Mon calendrier' },
      { name: 'Mon équipe / famille', href: '/my-team', icon: Users, subtitle: 'Mes proches' },
    ],
  },
  {
    title: 'Administration',
    items: [
      { name: "Centre d'administration", href: '/admin', icon: SlidersHorizontal, subtitle: 'Toute la configuration' },
      { name: 'Identité & marque', href: '/admin/settings', icon: Palette, subtitle: 'Nom, logo & couleurs' },
      { name: 'Modules', href: '/admin/modules', icon: Boxes, subtitle: 'Activer / désactiver' },
      { name: 'Menus', href: '/admin/menus', icon: MenuList, subtitle: 'Configurer la navigation' },
      { name: 'Pages', href: '/admin/pages', icon: FileText, subtitle: 'Pages personnalisées' },
      { name: 'Utilisateurs', href: '/users', icon: UserCog, subtitle: 'Gestion des comptes' },
      { name: 'Permissions', href: '/permissions', icon: Shield, subtitle: 'Matrice des rôles' },
      { name: 'Champs personnalisés', href: '/admin/custom-fields', icon: FileText, subtitle: 'Champs des entités' },
      { name: 'Dictionnaires', href: '/admin/dictionaries', icon: BookOpen, subtitle: 'Types, statuts & catégories' },
      { name: 'Notifications', href: '/admin/notifications', icon: Bell, subtitle: 'Modèles & canaux' },
      { name: 'Configuration workflow', href: '/admin/transfers', icon: Workflow, subtitle: 'Circuits de validation' },
      { name: 'Intégrations', href: '/admin/integrations', icon: Globe, subtitle: 'SMTP, stockage, API' },
      { name: 'Webhooks & API', href: '/admin/webhooks', icon: WebhookIcon, subtitle: 'Connecteur écosystème' },
      { name: 'Pont WhatsApp', href: '/admin/whatsapp', icon: MessageCircle, subtitle: 'Diffusion & commandes' },
      { name: 'Connecteurs tiers', href: '/admin/connectors', icon: Plug, subtitle: 'Zapier, agendas, comptabilité' },
      { name: 'API & Documentation', href: '/api-docs', icon: Code, subtitle: 'Clés API & Swagger' },
      { name: 'Conformité RGPD', href: '/admin/compliance', icon: ShieldCheck, subtitle: 'Rétention & portabilité' },
      { name: 'Onboarding wizard', href: '/onboarding-wizard', icon: Rocket, subtitle: 'Configuration église' },
      { name: 'Automatisations', href: '/automations', icon: Zap, subtitle: 'Workflows configurables' },
      { name: 'Mentorat IA', href: '/mentoring', icon: Brain, subtitle: 'Suggestions chefs de famille' },
      { name: 'Drill-down KPI', href: '/kpi-drilldown', icon: BarChart3, subtitle: 'Narration KPI' },
      { name: 'Planning équipes', href: '/team-gantt', icon: GanttChart, subtitle: 'Vue Gantt des tâches' },
      { name: 'Matrice compétences', href: '/skills-matrix', icon: Award, subtitle: 'Évaluation des membres' },
      { name: 'Portail public', href: '/portal', icon: Globe, subtitle: "Page publique de l'église" },
      { name: 'Inventaire', href: '/inventory', icon: Package, subtitle: 'Gestion matérielle' },
      { name: 'KPIs Départements', href: '/department-kpis', icon: BarChart3, subtitle: 'Objectifs & métriques' },
      { name: 'Marketplace', href: '/marketplace', icon: Store, subtitle: 'Échanges communautaires' },
      { name: 'Récompenses', href: '/rewards', icon: Trophy, subtitle: 'Gamification & badges' },
      { name: 'Système', href: '/admin/system', icon: Server, subtitle: 'Santé, cache & performances' },
      { name: 'Églises (tenants)', href: '/admin/tenants', icon: Building2, subtitle: 'Multi-tenant' },
      { name: 'Audit', href: '/audit', icon: Activity, subtitle: 'Journal de bord' },
      { name: 'Retours testeurs', href: '/admin/feedback', icon: MessageSquare, subtitle: 'Bugs & suggestions' },
    ],
  },
];

/* ============================================================================
 * RESPONSABLE — Gestionnaire RH de département
 * Rôle opérationnel : effectifs, présence, absence, tâches, équipes, postes,
 * progression, événements, rapports, discipline, évaluations.
 * Aucun menu discipolat / familles / âmes / faiseurs.
 * ========================================================================== */

const RESPONSABLE_NAV: WorkspaceSection[] = [
  {
    title: 'Tableau de bord',
    items: [
      { name: 'Mon département', href: '/dashboard/responsable', icon: LayoutDashboard, subtitle: 'Effectifs, présence & alertes' },
    ],
  },
  {
    title: 'Effectifs & organisation',
    items: [
      { name: 'Départements', href: '/departments', icon: Building2, subtitle: 'Structure & hiérarchie' },
      { name: 'Équipes', href: '/departments', icon: UsersRound, subtitle: 'Organisation & branches' },
      { name: 'Postes', href: '/departments', icon: Briefcase, subtitle: 'Positions & compétences' },
      { name: 'Membres', href: '/users', icon: UserCog, subtitle: 'Comptes & affectations' },
    ],
  },
  {
    title: 'Présence & activités',
    items: [
      { name: 'Saisie des présences', href: '/dashboard/responsable', icon: ClipboardCheck, subtitle: 'Pointage hebdomadaire' },
      { name: 'Demandes & présences', href: '/members/requests', icon: MessageSquare, subtitle: 'Fiches & suggestions' },
      { name: 'Tâches', href: '/departments', icon: ListTodo, subtitle: 'Suivi des assignments' },
      { name: 'Événements', href: '/events', icon: Calendar, subtitle: 'Calendrier du département' },
    ],
  },
  {
    title: 'Suivi & discipline',
    items: [
      { name: 'Évaluations', href: '/evaluations', icon: StarIcon, subtitle: 'Évaluations anonymes' },
      { name: 'Discipline', href: '/discipline', icon: Scale, subtitle: 'Suivi disciplinaire' },
      { name: 'Progression', href: '/departments', icon: TrendingUp, subtitle: 'Croissance des membres' },
    ],
  },
  {
    title: 'Reporting',
    items: [
      { name: 'Rapports', href: '/reports', icon: FileText, subtitle: 'Hebdomadaires' },
      { name: 'Rapport famille', href: '/reports/family', icon: FileText, subtitle: 'Rapport de famille' },
      { name: 'Aide urgente', href: '/reports/urgent-aid', icon: AlertTriangle, subtitle: "Demandes d'aide" },
      { name: 'Transferts', href: '/transfers', icon: ArrowLeftRight, subtitle: 'Demandes & validations' },
    ],
  },
  {
    title: 'Outils',
    items: [
      { name: 'Recherche', href: '/search', icon: Search, subtitle: 'Recherche intelligente' },
      { name: 'Notifications', href: '/notifications', icon: BellRing, subtitle: 'Centre de notifications' },
      { name: 'Alertes', href: '/alerts', icon: Bell, subtitle: "Centre d'alertes" },
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
      { name: 'Documents', href: '/documents', icon: FolderOpen, subtitle: 'Fichiers & rapports' },
      { name: 'Tickets & Support', href: '/tickets', icon: LifeBuoy, subtitle: 'Système de tickets' },
      { name: 'Sondages', href: '/surveys', icon: ClipboardListIcon, subtitle: 'Sondages rapides' },
      { name: 'Témoignages', href: '/testimonials', icon: Heart, subtitle: 'Galerie' },
      { name: 'Planning équipes', href: '/team-gantt', icon: GanttChart, subtitle: 'Vue Gantt' },
      { name: 'Matrice compétences', href: '/skills-matrix', icon: Award, subtitle: 'Compétences' },
      { name: 'Demandes d\'absence', href: '/leave-requests', icon: CalendarOff, subtitle: 'Congés' },
      { name: 'Parrainage', href: '/referrals', icon: Share2, subtitle: 'Invitez' },
      { name: 'Calendrier', href: '/calendar', icon: Calendar, subtitle: 'Sync' },
      { name: 'Journal de prière', href: '/prayer-journal', icon: BookHeart, subtitle: 'Mon carnet' },
      { name: 'Parcours spirituel', href: '/spiritual-journey', icon: Footprints, subtitle: 'Progression' },
      { name: 'Streaming & Live', href: '/streaming', icon: Video, subtitle: 'Cultes en direct' },
      { name: 'Communauté', href: '/community', icon: UsersIcon, subtitle: 'Fil social' },
      { name: 'KPIs Départements', href: '/department-kpis', icon: BarChart3, subtitle: 'Objectifs' },
      { name: 'Inventaire', href: '/inventory', icon: Package, subtitle: 'Matériel' },
      { name: 'Prédictions IA', href: '/ai-predictions', icon: Sparkles, subtitle: 'Forecasting' },
      { name: 'Objectifs spirituels', href: '/personal-objectives', icon: Target, subtitle: 'Mes objectifs' },
      { name: 'Visites pastorales', href: '/pastoral-visits', icon: Compass, subtitle: 'Mes visites' },
      { name: 'Ressources famille', href: '/family-resources', icon: FolderOpen, subtitle: 'Documents' },
      { name: 'Profil', href: '/profile', icon: User, subtitle: 'Mes informations' },
    ],
  },
];

/* ============================================================================
 * FAISEUR — Accompagneur de disciples
 * Rôle terrain : disciples, visites, prières, rapports, suivi, progression,
 * présence, événements. Pas de gestion RH.
 * ========================================================================== */

const FAISEUR_NAV: WorkspaceSection[] = [
  {
    title: 'Mon terrain',
    items: [
      { name: 'CRM Faiseur', href: '/crm/faiseur', icon: HandHeart, subtitle: 'Tableau de bord terrain' },
      { name: 'Mes disciples', href: '/souls', icon: Heart, subtitle: 'Disciples suivis' },
      { name: 'Évangélisation', href: '/evangelism', icon: Sprout, subtitle: 'Pipeline de croissance' },
    ],
  },
  {
    title: 'Suivi hebdomadaire',
    items: [
      { name: 'Mon rapport', href: '/reports/maker', icon: ClipboardList, subtitle: 'Rapport de la semaine' },
      { name: 'Visites', href: '/visits', icon: DoorOpen, subtitle: 'Planification & comptes rendus' },
      { name: 'Suivis parallèles', href: '/parallel-followups', icon: Activity, subtitle: 'Accompagnements' },
      { name: 'Objectifs', href: '/objectives', icon: Target, subtitle: 'Mes objectifs' },
      { name: 'Mes rapports vocaux', href: '/voice-reports', icon: Mic, subtitle: 'Dictées terrain analysées par IA' },
      { name: 'Journal prophétique', href: '/prophetic-journal', icon: Eye, subtitle: 'Visions & rêves' },
    ],
  },
  {
    title: 'Prières & accompagnement',
    items: [
      { name: 'Prières', href: '/prayers', icon: BookOpen, subtitle: 'Sujets & témoignages' },
      { name: 'Actions de grâce', href: '/prayers/actions-de-grace', icon: Heart, subtitle: 'Prières exaucées' },
    ],
  },
  {
    title: "Vie de l'église",
    items: [
      { name: 'Événements', href: '/events', icon: Calendar, subtitle: 'Calendrier' },
      { name: 'Rapports', href: '/reports', icon: FileText, subtitle: 'Vue globale' },
      { name: 'Transferts', href: '/transfers', icon: ArrowLeftRight, subtitle: 'Demandes & validations' },
    ],
  },
  {
    title: 'Outils',
    items: [
      { name: 'Recherche', href: '/search', icon: Search, subtitle: 'Recherche intelligente' },
      { name: 'Notifications', href: '/notifications', icon: BellRing, subtitle: 'Centre de notifications' },
      { name: 'Alertes', href: '/alerts', icon: Bell, subtitle: "Centre d'alertes" },
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
      { name: 'Documents', href: '/documents', icon: FolderOpen, subtitle: 'Fichiers & rapports' },
      { name: 'Tickets & Support', href: '/tickets', icon: LifeBuoy, subtitle: 'Système de tickets' },
      { name: 'Sondages', href: '/surveys', icon: ClipboardListIcon, subtitle: 'Sondages' },
      { name: 'Témoignages', href: '/testimonials', icon: Heart, subtitle: 'Témoignages' },
      { name: 'Demandes d\'absence', href: '/leave-requests', icon: CalendarOff, subtitle: 'Congés' },
      { name: 'Parrainage', href: '/referrals', icon: Share2, subtitle: 'Invitez' },
      { name: 'Calendrier', href: '/calendar', icon: Calendar, subtitle: 'Sync' },
      { name: 'Cercle de faiseurs', href: '/cercle-faiseurs', icon: Users, subtitle: 'Entraide' },
      { name: 'Objectifs spirituels', href: '/personal-objectives', icon: Target, subtitle: 'Mes objectifs' },
      { name: 'Journal de prière', href: '/prayer-journal', icon: BookHeart, subtitle: 'Mon carnet' },
      { name: 'Défis spirituels', href: '/spiritual-challenges', icon: Swords, subtitle: 'Défis' },
      { name: 'Communauté', href: '/community', icon: UsersIcon, subtitle: 'Fil social' },
      { name: 'Marketplace', href: '/marketplace', icon: Store, subtitle: 'Échanges' },
      { name: 'Récompenses', href: '/rewards', icon: Trophy, subtitle: 'Badges' },
      { name: 'Profil', href: '/profile', icon: User, subtitle: 'Mes informations' },
    ],
  },
];

/* ============================================================================
 * CHEF DE FAMILLE — Gestionnaire pastoral de famille
 * Rôle de supervision : faiseurs, disciples, âmes, familles, rapports,
 * prières, progression, alertes. Pas de gestion RH / départements.
 * ========================================================================== */

const CHEF_FAMILLE_NAV: WorkspaceSection[] = [
  {
    title: 'Ma famille',
    items: [
      { name: 'Ma famille', href: '/dashboard/chef-famille', icon: LayoutDashboard, subtitle: 'Vue pastorale' },
      { name: 'Familles', href: '/families', icon: Users, subtitle: 'Groupes de disciples' },
      { name: 'Disciples', href: '/souls', icon: Heart, subtitle: 'Disciples de la famille' },
    ],
  },
  {
    title: 'Mes faiseurs',
    items: [
      { name: 'Faiseurs & performance', href: '/families', icon: UserCheck, subtitle: 'Charge & résultats' },
      { name: 'Évangélisation', href: '/evangelism', icon: Sprout, subtitle: 'Pipeline de croissance' },
    ],
  },
  {
    title: 'Suivi pastoral',
    items: [
      { name: 'Rapports famille', href: '/reports/family', icon: ClipboardList, subtitle: 'Mon rapport hebdomadaire' },
      { name: 'Journal prophétique', href: '/prophetic-journal', icon: Eye, subtitle: 'Visions & rêves' },
      { name: 'Rapports faiseurs', href: '/reports', icon: FileText, subtitle: 'Rapports reçus' },
      { name: 'Évaluations', href: '/evaluations', icon: StarIcon, subtitle: 'Évaluations anonymes' },
      { name: 'Prières', href: '/prayers', icon: BookOpen, subtitle: 'Sujets & témoignages' },
      { name: 'Progression', href: '/families', icon: TrendingUp, subtitle: 'Croissance des disciples' },
    ],
  },
  {
    title: 'Vie de la famille',
    items: [
      { name: 'Événements', href: '/events', icon: Calendar, subtitle: 'Calendrier' },
      { name: 'Alertes', href: '/alerts', icon: AlertTriangle, subtitle: 'Suivi & vigils' },
      { name: 'Demandes membres', href: '/members/requests', icon: MessageSquare, subtitle: 'Suggestions & présences' },
      { name: 'Transferts', href: '/transfers', icon: ArrowLeftRight, subtitle: 'Demandes & validations' },
      { name: 'Actions de grâce', href: '/prayers/actions-de-grace', icon: Heart, subtitle: 'Prières exaucées' },
    ],
  },
  {
    title: 'Outils',
    items: [
      { name: 'Notifications', href: '/notifications', icon: BellRing, subtitle: 'Centre de notifications' },
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
      { name: 'Documents', href: '/documents', icon: FolderOpen, subtitle: 'Fichiers & rapports' },
      { name: 'Tickets & Support', href: '/tickets', icon: LifeBuoy, subtitle: 'Système de tickets' },
      { name: 'Sondages', href: '/surveys', icon: ClipboardListIcon, subtitle: 'Sondages' },
      { name: 'Témoignages', href: '/testimonials', icon: Heart, subtitle: 'Témoignages' },
      { name: 'Planning équipes', href: '/team-gantt', icon: GanttChart, subtitle: 'Vue Gantt' },
      { name: 'Matrice compétences', href: '/skills-matrix', icon: Award, subtitle: 'Compétences' },
      { name: 'Demandes d\'absence', href: '/leave-requests', icon: CalendarOff, subtitle: 'Congés' },
      { name: 'Parrainage', href: '/referrals', icon: Share2, subtitle: 'Invitez' },
      { name: 'Calendrier', href: '/calendar', icon: Calendar, subtitle: 'Sync' },
      { name: 'Plans de lecture biblique', href: '/bible-reading', icon: BookMarked, subtitle: 'Lecture partagée' },
      { name: 'Journal de prière', href: '/prayer-journal', icon: BookHeart, subtitle: 'Mon carnet' },
      { name: 'Défis spirituels', href: '/spiritual-challenges', icon: Swords, subtitle: 'Défis' },
      { name: 'Annuaire', href: '/directory', icon: Contact, subtitle: 'Connaître' },
      { name: 'Parcours spirituel', href: '/spiritual-journey', icon: Footprints, subtitle: 'Progression' },
      { name: 'Communauté', href: '/community', icon: UsersIcon, subtitle: 'Fil social' },
      { name: 'Récompenses', href: '/rewards', icon: Trophy, subtitle: 'Badges' },
      { name: 'Marketplace', href: '/marketplace', icon: Store, subtitle: 'Échanges' },
      { name: 'Profil', href: '/profile', icon: User, subtitle: 'Mes informations' },
    ],
  },
];

/* ============================================================================
 * MEMBRE — Espace personnel du fidèle
 * Rôle de consommation : profil, présence, progression, événements,
 * prières, formations, activités. Pas de gestion d'autrui.
 * ========================================================================== */

const MEMBRE_NAV: WorkspaceSection[] = [
  {
    title: 'Mon espace',
    items: [
      { name: 'Mon tableau de bord', href: '/dashboard/membre', icon: LayoutDashboard, subtitle: 'Vue personnelle' },
      { name: 'Mon profil', href: '/profile', icon: User, subtitle: 'Mes données personnelles' },
    ],
  },
  {
    title: 'Ma vie spirituelle',
    items: [
      { name: 'Mes présences', href: '/dashboard/membre', icon: ClipboardCheck, subtitle: 'Pointage & historique' },
      { name: 'Ma progression', href: '/dashboard/membre', icon: TrendingUp, subtitle: 'Croissance spirituelle' },
      { name: 'Mes activités', href: '/dashboard/membre/activities', icon: Activity, subtitle: 'Timeline personnelle' },
      { name: 'Prières', href: '/prayers', icon: BookOpen, subtitle: 'Sujets & témoignages' },
      { name: 'Actions de grâce', href: '/prayers/actions-de-grace', icon: Heart, subtitle: 'Prières exaucées' },
    ],
  },
  {
    title: 'Ma communauté',
    items: [
      { name: 'Événements', href: '/events', icon: Calendar, subtitle: 'Calendrier' },
      { name: 'Messagerie', href: '/messages', icon: MessagesSquare, subtitle: 'Conversations privées' },
      { name: 'Notifications', href: '/notifications', icon: BellRing, subtitle: 'Centre de notifications' },
    ],
  },
  {
    title: 'Formation & engagement',
    items: [
      { name: 'Formations', href: '/trainings', icon: GraduationCap, subtitle: 'Cours, quiz & certificats' },
      { name: 'Badges', href: '/badges', icon: Trophy, subtitle: 'Récompenses & classements' },
      { name: 'Quest (XP)', href: '/quest', icon: Zap, subtitle: 'Gagnez de l\'XP en servant' },
      { name: 'Tontines', href: '/tontines', icon: HandCoins, subtitle: 'Épargne solidaire' },
      { name: 'Dîmes & offrandes', href: '/giving', icon: Smartphone, subtitle: 'Mobile Money instantané' },
      { name: 'Rendez-vous', href: '/appointments', icon: CalendarClock, subtitle: 'Prises de RDV' },
      { name: 'Tickets & Support', href: '/tickets', icon: LifeBuoy, subtitle: 'Demander de l\'aide' },
      { name: 'Sondages', href: '/surveys', icon: ClipboardListIcon, subtitle: 'Sondages' },
      { name: 'Témoignages', href: '/testimonials', icon: Heart, subtitle: 'Témoignages' },
      { name: 'Parrainage', href: '/referrals', icon: Share2, subtitle: 'Invitez vos proches' },
      { name: 'Calendrier', href: '/calendar', icon: Calendar, subtitle: 'Sync calendrier' },
      { name: 'Plans de lecture biblique', href: '/bible-reading', icon: BookMarked, subtitle: 'Lecture partagée' },
      { name: 'Journal de prière', href: '/prayer-journal', icon: BookHeart, subtitle: 'Mon carnet' },
      { name: 'Défis spirituels', href: '/spiritual-challenges', icon: Swords, subtitle: 'Défis' },
      { name: 'Annuaire', href: '/directory', icon: Contact, subtitle: 'Connaître' },
      { name: 'Parcours spirituel', href: '/spiritual-journey', icon: Footprints, subtitle: 'Progression' },
      { name: 'Communauté', href: '/community', icon: UsersIcon, subtitle: 'Fil social' },
      { name: 'Récompenses', href: '/rewards', icon: Trophy, subtitle: 'Badges' },
      { name: 'Marketplace', href: '/marketplace', icon: Store, subtitle: 'Échanges' },
    ],
  },
];

/**
 * Routes réservées à l'Administration (rôle ADMIN uniquement).
 * Le Pasteur voit la vue complète SAUF ces écrans : les cliquer le ferait
 * rediriger vers /dashboard sans explication (bouton mort).
 */
const ADMIN_ONLY_HREFS: string[] = [
  // Tous les écrans admin sont désormais accessibles aux ADMIN + PASTEUR.
  // (routes corrigées dans App.tsx pour autoriser PASTEUR)
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
