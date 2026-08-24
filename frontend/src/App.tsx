import React, { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { WORKSPACE_HOME, isSuperUser } from '@/workspaces';
import type { UserRole } from '@/types';
import MainLayout from '@/layouts/MainLayout';
import AuthLayout from '@/layouts/AuthLayout';
import ErrorBoundary from '@/components/shared/ErrorBoundary';
import { OfflineIndicator } from '@/components/pwa/OfflineIndicator';

/* ============================================================================
 * CHARGEMENT PAR ROUTE (code splitting)
 * Chaque écran n'est téléchargé qu'à sa première visite → chargement initial
 * minimal (critique pour les testeurs sur réseaux lents, cf. mission §16/§22).
 * ========================================================================== */
const LandingPage = lazy(() => import('@/pages/LandingPage'));
const LoginPage = lazy(() => import('@/pages/LoginPage'));
const TwoFactorChallengePage = lazy(() => import('@/pages/TwoFactorChallengePage'));
const ForgotPasswordPage = lazy(() => import('@/pages/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('@/pages/ResetPasswordPage'));
const MagicLinkVerifyPage = lazy(() => import('@/pages/MagicLinkVerifyPage'));
const DashboardPage = lazy(() => import('@/pages/DashboardPage'));
const PasteurDashboardPage = lazy(() => import('@/pages/PasteurDashboardPage'));
const ChefFamilleDashboardPage = lazy(() => import('@/pages/ChefFamilleDashboardPage'));
const ResponsableDashboardPage = lazy(() => import('@/pages/ResponsableDashboardPage'));
const SoulsPage = lazy(() => import('@/pages/SoulsPage'));
const SoulDetailPage = lazy(() => import('@/pages/SoulDetailPage'));
const FamiliesPage = lazy(() => import('@/pages/FamiliesPage'));
const FamilyDetailPage = lazy(() => import('@/pages/FamilyDetailPage'));
const DepartmentsPage = lazy(() => import('@/pages/DepartmentsPage'));
const DepartmentDetailPage = lazy(() => import('@/pages/DepartmentDetailPage'));
const DepartmentManagementPage = lazy(() => import('@/pages/DepartmentManagementPage'));
const DepartmentReportPage = lazy(() => import('@/pages/DepartmentReportPage'));
const DepartmentMemberDossierPage = lazy(() => import('@/pages/DepartmentMemberDossierPage'));
const DepartmentStatsPage = lazy(() => import('@/pages/DepartmentStatsPage'));
const DepartmentToolsPage = lazy(() => import('@/pages/DepartmentToolsPage'));
const ReportsPage = lazy(() => import('@/pages/ReportsPage'));
const MakerReportPage = lazy(() => import('@/pages/MakerReportPage'));
const FamilyReportPage = lazy(() => import('@/pages/FamilyReportPage'));
const ParallelFollowupsPage = lazy(() => import('@/pages/ParallelFollowupsPage'));
const AlertsPage = lazy(() => import('@/pages/AlertsPage'));
const NotificationsPage = lazy(() => import('@/pages/NotificationsPage'));
const UsersPage = lazy(() => import('@/pages/UsersPage'));
const ProfilePage = lazy(() => import('@/pages/ProfilePage'));
const SoulCreatePage = lazy(() => import('@/pages/SoulCreatePage'));
const SoulEditPage = lazy(() => import('@/pages/SoulEditPage'));
const FamilyCreatePage = lazy(() => import('@/pages/FamilyCreatePage'));
const FamilyFaiseurPerformancePage = lazy(() => import('@/pages/FamilyFaiseurPerformancePage'));
const PrayersPage = lazy(() => import('@/pages/PrayersPage'));
const EventsPage = lazy(() => import('@/pages/EventsPage'));
const DocumentsPage = lazy(() => import('@/pages/DocumentsPage'));
const AuditPage = lazy(() => import('@/pages/AuditPage'));
const MemberActivitiesPage = lazy(() => import('@/pages/MemberActivitiesPage'));
const DisciplinePage = lazy(() => import('@/pages/DisciplinePage'));
const PermissionsPage = lazy(() => import('@/pages/PermissionsPage'));
const CompareFamiliesPage = lazy(() => import('@/pages/CompareFamiliesPage'));
const SoulRetractionsPage = lazy(() => import('@/pages/SoulRetractionsPage'));
const ActionsDeGracePage = lazy(() => import('@/pages/ActionsDeGracePage'));
const UrgentAidPage = lazy(() => import('@/pages/UrgentAidPage'));
const IntelligentSearchPage = lazy(() => import('@/pages/IntelligentSearchPage'));
const Pastoral360Page = lazy(() => import('@/pages/Pastoral360Page'));
const CrmFaiseurPage = lazy(() => import('@/pages/CrmFaiseurPage'));
const FinancePage = lazy(() => import('@/pages/FinancePage'));
const CommunicationsPage = lazy(() => import('@/pages/CommunicationsPage'));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'));
const WeeklyProgramPage = lazy(() => import('@/pages/WeeklyProgramPage'));
const ProgramTypesPage = lazy(() => import('@/pages/ProgramTypesPage'));
const EventStatisticsPage = lazy(() => import('@/pages/EventStatisticsPage'));
const PrayerSpacesPage = lazy(() => import('@/pages/PrayerSpacesPage'));
const EvaluationsPage = lazy(() => import('@/pages/EvaluationsPage'));
const MemberDashboardPage = lazy(() => import('@/pages/MemberDashboardPage'));
const MemberRequestsPage = lazy(() => import('@/pages/MemberRequestsPage'));
const MapPage = lazy(() => import('@/pages/MapPage'));
const MessagesPage = lazy(() => import('@/pages/MessagesPage'));
const EvangelismPage = lazy(() => import('@/pages/EvangelismPage'));
const ObjectivesPage = lazy(() => import('@/pages/ObjectivesPage'));
const VisitsPage = lazy(() => import('@/pages/VisitsPage'));
const BadgesPage = lazy(() => import('@/pages/BadgesPage'));
const TrainingsPage = lazy(() => import('@/pages/TrainingsPage'));
const AppointmentsPage = lazy(() => import('@/pages/AppointmentsPage'));
const TransfersPage = lazy(() => import('@/pages/TransfersPage'));
const TransferDetailPage = lazy(() => import('@/pages/TransferDetailPage'));
const TransferCreatePage = lazy(() => import('@/pages/TransferCreatePage'));
const AdminWorkflowBuilderPage = lazy(() => import('@/pages/AdminWorkflowBuilderPage'));
const AdminSettingsPage = lazy(() => import('@/pages/AdminSettingsPage'));
const PlatformModulesPage = lazy(() => import('@/pages/PlatformModulesPage'));
const PlatformMenusPage = lazy(() => import('@/pages/PlatformMenusPage'));
const PlatformPagesPage = lazy(() => import('@/pages/PlatformPagesPage'));
const CustomPageView = lazy(() => import('@/pages/CustomPageView'));
const ModuleUnavailablePage = lazy(() => import('@/pages/ModuleUnavailablePage'));
const AdminDashboardPage = lazy(() => import('@/pages/AdminDashboardPage'));
const AdminCustomFieldsPage = lazy(() => import('@/pages/AdminCustomFieldsPage'));
const AdminFeedbackPage = lazy(() => import('@/pages/AdminFeedbackPage'));
const AdminDictionariesPage = lazy(() => import('@/pages/AdminDictionariesPage'));
const AdminTenantsPage = lazy(() => import('@/pages/AdminTenantsPage'));
const AdminNotificationTemplatesPage = lazy(() => import('@/pages/AdminNotificationTemplatesPage'));
const AdminSystemPage = lazy(() => import('@/pages/AdminSystemPage'));
const AdminIntegrationsPage = lazy(() => import('@/pages/AdminIntegrationsPage'));
const AdminGdprPage = lazy(() => import('@/pages/AdminGdprPage'));
const QuestPage = lazy(() => import('@/pages/QuestPage'));
const HealthObservatoryPage = lazy(() => import('@/pages/HealthObservatoryPage'));
const TontinePage = lazy(() => import('@/pages/TontinePage'));
const GivingPage = lazy(() => import('@/pages/GivingPage'));
const AdminWebhooksPage = lazy(() => import('@/pages/AdminWebhooksPage'));
const AdminWhatsappPage = lazy(() => import('@/pages/AdminWhatsappPage'));
const DigitalTwinPage = lazy(() => import('@/pages/DigitalTwinPage'));
const PropheticJournalPage = lazy(() => import('@/pages/PropheticJournalPage'));
const SermonAssistantPage = lazy(() => import('@/pages/SermonAssistantPage'));
const VoiceReportsPage = lazy(() => import('@/pages/VoiceReportsPage'));
const KingdomMappingPage = lazy(() => import('@/pages/KingdomMappingPage'));
const AiAssistantPage = lazy(() => import('@/pages/AiAssistantPage'));
const TicketsPage = lazy(() => import('@/pages/TicketsPage'));
const OnboardingWizardPage = lazy(() => import('@/pages/OnboardingWizardPage'));
const SurveysPage = lazy(() => import('@/pages/SurveysPage'));
const LeaveRequestsPage = lazy(() => import('@/pages/LeaveRequestsPage'));
const PublicPortalPage = lazy(() => import('@/pages/PublicPortalPage'));
const SkillsMatrixPage = lazy(() => import('@/pages/SkillsMatrixPage'));
const ReferralPage = lazy(() => import('@/pages/ReferralPage'));
const CalendarIntegrationPage = lazy(() => import('@/pages/CalendarIntegrationPage'));
const TeamGanttPage = lazy(() => import('@/pages/TeamGanttPage'));
const TestimonialsPage = lazy(() => import('@/pages/TestimonialsPage'));
const ComplianceDashboardPage = lazy(() => import('@/pages/ComplianceDashboardPage'));
const ApiDocsPage = lazy(() => import('@/pages/ApiDocsPage'));
const CercleFaiseursPage = lazy(() => import('@/pages/CercleFaiseursPage'));
const BibleReadingPlanPage = lazy(() => import('@/pages/BibleReadingPlanPage'));
const PrayerJournalPage = lazy(() => import('@/pages/PrayerJournalPage'));
const SpiritualChallengesPage = lazy(() => import('@/pages/SpiritualChallengesPage'));
const ChurchDirectoryPage = lazy(() => import('@/pages/ChurchDirectoryPage'));
const SpiritualJourneyPage = lazy(() => import('@/pages/SpiritualJourneyPage'));
const StreamingPage = lazy(() => import('@/pages/StreamingPage'));
const BroadcastPage = lazy(() => import('@/pages/BroadcastPage'));
const InventoryPage = lazy(() => import('@/pages/InventoryPage'));
const DepartmentKPIsPage = lazy(() => import('@/pages/DepartmentKPIsPage'));
const RewardsPage = lazy(() => import('@/pages/RewardsPage'));
const MarketplacePage = lazy(() => import('@/pages/MarketplacePage'));
const CommunityPage = lazy(() => import('@/pages/CommunityPage'));
const AiPredictionsPage = lazy(() => import('@/pages/AiPredictionsPage'));
const PersonalObjectivesPage = lazy(() => import('@/pages/PersonalObjectivesPage'));
const FamilyCohesionPage = lazy(() => import('@/pages/FamilyCohesionPage'));
const SuccessionPage = lazy(() => import('@/pages/SuccessionPage'));
const PastoralVisitsPage = lazy(() => import('@/pages/PastoralVisitsPage'));
const FamilyResourcesPage = lazy(() => import('@/pages/FamilyResourcesPage'));
const AutomationsPage = lazy(() => import('@/pages/AutomationsPage'));
const MentoratIAPage = lazy(() => import('@/pages/MentoratIAPage'));
const KpiDrillDownPage = lazy(() => import('@/pages/KpiDrillDownPage'));
const CurrencySettingsPage = lazy(() => import('@/pages/CurrencySettingsPage'));
const ContentModerationPage = lazy(() => import('@/pages/ContentModerationPage'));
const PredictionsMLPage = lazy(() => import('@/pages/PredictionsMLPage'));
const IntelligenceCenterPage = lazy(() => import('@/pages/IntelligenceCenterPage'));
const EngagementAnalyticsPage = lazy(() => import('@/pages/EngagementAnalyticsPage'));
const ScheduledAnnouncementsPage = lazy(() => import('@/pages/ScheduledAnnouncementsPage'));
const EventChecklistsPage = lazy(() => import('@/pages/EventChecklistsPage'));
const GroupMessagesPage = lazy(() => import('@/pages/GroupMessagesPage'));
const WeeklyChallengesPage = lazy(() => import('@/pages/WeeklyChallengesPage'));
const DiscipleshipPathPage = lazy(() => import('@/pages/DiscipleshipPathPage'));
const AiVisitNotesPage = lazy(() => import('@/pages/AiVisitNotesPage'));
const ReverseMentoringPage = lazy(() => import('@/pages/ReverseMentoringPage'));
const FamilyMeetingPage = lazy(() => import('@/pages/FamilyMeetingPage'));
const ExecutiveInsightsPage = lazy(() => import('@/pages/ExecutiveInsightsPage'));
const UpcomingEventsMemberPage = lazy(() => import('@/pages/UpcomingEventsMemberPage'));
const MyTeamFamilyPage = lazy(() => import('@/pages/MyTeamFamilyPage'));

/** Fallback de chargement des routes (squelette léger, cohérent avec le thème). */
function RouteFallback() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-950">
      <div className="flex flex-col items-center gap-4">
        <div className="spinner h-8 w-8" />
        <p className="text-xs text-gray-400 dark:text-gray-500 font-medium">Chargement…</p>
      </div>
    </div>
  );
}

function ProtectedRoute({ children, roles }: { children: React.ReactNode; roles?: string[] }) {
  const { isAuthenticated, user, isLoading, activeRole } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Check against activeRole; fallback to user.role for backward compatibility
  const currentRole = activeRole || user?.role;
  // Super-utilisateur : un Admin accède aux capacités Pasteur (cf. mobile).
  const allowed = !roles
    || (currentRole && (roles.includes(currentRole)
        || (currentRole === 'ADMIN' && roles.includes('PASTEUR'))));
  if (!allowed) {
    // Redirection directe vers l'espace métier du rôle actif (au lieu de
    // /dashboard qui re-redirige) : un menu jamais accessible au rôle actif
    // ne doit pas donner l'impression d'un bouton mort vers le CRM Faiseur.
    return <Navigate to={WORKSPACE_HOME[(currentRole || 'FAISEUR') as UserRole] || '/dashboard'} replace />;
  }

  return <>{children}</>;
}

/**
 * Tableau de bord racine : chaque rôle est redirigé vers SON espace métier.
 * Un changement de rôle = un changement complet de contexte de travail.
 */
function DashboardGate() {
  const { activeRole, user } = useAuth();
  const currentRole = (activeRole || user?.role || 'FAISEUR') as UserRole;
  // Super-utilisateurs (Admin / Pasteur) : dashboard général.
  // Rôles opérationnels : redirection vers leur espace métier dédié.
  if (!isSuperUser(currentRole)) {
    return <Navigate to={WORKSPACE_HOME[currentRole] || '/dashboard'} replace />;
  }
  return <DashboardPage />;
}

/**
 * Page d'accueil publique : landing glassmorphism si visiteur, dashboard si
 * déjà authentifié.
 */
function HomeGate() {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }
  return <LandingPage />;
}

export default function App() {
  return (
    <ErrorBoundary>
      <OfflineIndicator />
      <Suspense fallback={<RouteFallback />}>
        <Routes>
        {/* Auth routes */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/auth/magic-link" element={<MagicLinkVerifyPage />} />
        </Route>

        <Route path="/verify-2fa" element={
          <ProtectedRoute>
            <TwoFactorChallengePage />
          </ProtectedRoute>
        } />

        {/* Protected routes */}
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <DashboardGate />
            </ProtectedRoute>
          } />
          <Route path="/dashboard/membre" element={
            <ProtectedRoute roles={['MEMBRE']}>
              <MemberDashboardPage />
            </ProtectedRoute>
          } />
          <Route path="/dashboard/membre/activities" element={
            <ProtectedRoute roles={['MEMBRE']}>
              <MemberActivitiesPage />
            </ProtectedRoute>
          } />
          <Route path="/discipline" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <DisciplinePage />
            </ProtectedRoute>
          } />
          <Route path="/dashboard/pasteur" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <PasteurDashboardPage />
            </ProtectedRoute>
          } />
          <Route path="/dashboard/chef-famille" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE']}>
              <ChefFamilleDashboardPage />
            </ProtectedRoute>
          } />
          <Route path="/dashboard/responsable" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <ResponsableDashboardPage />
            </ProtectedRoute>
          } />
          <Route path="/search" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <IntelligentSearchPage />
            </ProtectedRoute>
          } />
          <Route path="/souls" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <SoulsPage />
            </ProtectedRoute>
          } />
          <Route path="/souls/new" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <SoulCreatePage />
            </ProtectedRoute>
          } />
          <Route path="/souls/:id" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <SoulDetailPage />
            </ProtectedRoute>
          } />
          <Route path="/souls/:id/edit" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <SoulEditPage />
            </ProtectedRoute>
          } />
          <Route path="/families" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <FamiliesPage />
            </ProtectedRoute>
          } />
          <Route path="/families/new" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE']}>
              <FamilyCreatePage />
            </ProtectedRoute>
          } />
          <Route path="/families/:id" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <FamilyDetailPage />
            </ProtectedRoute>
          } />
          <Route path="/departments" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <DepartmentsPage />
            </ProtectedRoute>
          } />
          <Route path="/departments/:id" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <DepartmentDetailPage />
            </ProtectedRoute>
          } />
          <Route path="/departments/:id/report" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <DepartmentReportPage />
            </ProtectedRoute>
          } />
          <Route path="/departments/:id/manage" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <DepartmentManagementPage />
            </ProtectedRoute>
          } />
          <Route path="/departments/:id/members/:memberId" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <DepartmentMemberDossierPage />
            </ProtectedRoute>
          } />
          <Route path="/departments/:id/stats" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <DepartmentStatsPage />
            </ProtectedRoute>
          } />
          <Route path="/departments/:id/tools" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <DepartmentToolsPage />
            </ProtectedRoute>
          } />
          <Route path="/reports" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <ReportsPage />
            </ProtectedRoute>
          } />
          <Route path="/reports/maker" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'FAISEUR']}>
              <MakerReportPage />
            </ProtectedRoute>
          } />
          <Route path="/reports/family" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <FamilyReportPage />
            </ProtectedRoute>
          } />
          <Route path="/families/:id/faiseur-performance" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <FamilyFaiseurPerformancePage />
            </ProtectedRoute>
          } />
          <Route path="/prayers" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']}>
              <PrayersPage />
            </ProtectedRoute>
          } />
          <Route path="/events" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']}>
              <EventsPage />
            </ProtectedRoute>
          } />
          <Route path="/documents" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <DocumentsPage />
            </ProtectedRoute>
          } />
          <Route path="/parallel-followups" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <ParallelFollowupsPage />
            </ProtectedRoute>
          } />
          <Route path="/alerts" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <AlertsPage />
            </ProtectedRoute>
          } />
          <Route path="/finances" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <FinancePage />
            </ProtectedRoute>
          } />
          <Route path="/communications" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']}>
              <CommunicationsPage />
            </ProtectedRoute>
          } />
          <Route path="/notifications" element={
            <ProtectedRoute>
              <NotificationsPage />
            </ProtectedRoute>
          } />
          <Route path="/members/requests" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE']}>
              <MemberRequestsPage />
            </ProtectedRoute>
          } />
          <Route path="/map" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <MapPage />
            </ProtectedRoute>
          } />
          <Route path="/evangelism" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <EvangelismPage />
            </ProtectedRoute>
          } />
          <Route path="/objectives" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <ObjectivesPage />
            </ProtectedRoute>
          } />
          <Route path="/visits" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <VisitsPage />
            </ProtectedRoute>
          } />
          <Route path="/badges" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']}>
              <BadgesPage />
            </ProtectedRoute>
          } />
          <Route path="/quest" element={
            <ProtectedRoute>
              <QuestPage />
            </ProtectedRoute>
          } />
          <Route path="/giving" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']}>
              <GivingPage />
            </ProtectedRoute>
          } />
          <Route path="/health-observatory" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <HealthObservatoryPage />
            </ProtectedRoute>
          } />
          <Route path="/tontines" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']}>
              <TontinePage />
            </ProtectedRoute>
          } />
          <Route path="/digital-twin" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <DigitalTwinPage />
            </ProtectedRoute>
          } />
          <Route path="/prophetic-journal" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <PropheticJournalPage />
            </ProtectedRoute>
          } />
          <Route path="/sermon-assistant" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <SermonAssistantPage />
            </ProtectedRoute>
          } />
          <Route path="/voice-reports" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <VoiceReportsPage />
            </ProtectedRoute>
          } />
          <Route path="/kingdom-map" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <KingdomMappingPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/whatsapp" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminWhatsappPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/webhooks" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminWebhooksPage />
            </ProtectedRoute>
          } />
          <Route path="/trainings" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']}>
              <TrainingsPage />
            </ProtectedRoute>
          } />
          <Route path="/appointments" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']}>
              <AppointmentsPage />
            </ProtectedRoute>
          } />
          <Route path="/messages" element={
            <ProtectedRoute>
              <MessagesPage />
            </ProtectedRoute>
          } />
          <Route path="/users" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <UsersPage />
            </ProtectedRoute>
          } />
          <Route path="/profile" element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          } />
          <Route path="/audit" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AuditPage />
            </ProtectedRoute>
          } />          <Route path="/permissions" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <PermissionsPage />
            </ProtectedRoute>} />
          <Route path="/families/compare" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <CompareFamiliesPage />
            </ProtectedRoute>
          } />
          <Route path="/souls/retractions" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <SoulRetractionsPage />
            </ProtectedRoute>
          } />
          <Route path="/prayers/spaces" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <PrayerSpacesPage />
            </ProtectedRoute>
          } />
          <Route path="/evaluations" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <EvaluationsPage />
            </ProtectedRoute>
          } />
          <Route path="/prayers/actions-de-grace" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']}>
              <ActionsDeGracePage />
            </ProtectedRoute>
          } />
          <Route path="/reports/urgent-aid" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <UrgentAidPage />
            </ProtectedRoute>
          } />
          <Route path="/events/program" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <WeeklyProgramPage />
            </ProtectedRoute>
          } />
          <Route path="/programs" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <ProgramTypesPage />
            </ProtectedRoute>
          } />
          <Route path="/souls/:id/pastoral-360" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <Pastoral360Page />
            </ProtectedRoute>
          } />
          <Route path="/crm/faiseur" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'FAISEUR']}>
              <CrmFaiseurPage />
            </ProtectedRoute>
          } />
          <Route path="/events/statistics" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <EventStatisticsPage />
            </ProtectedRoute>
          } />
          <Route path="/transfers" element={
            <ProtectedRoute>
              <TransfersPage />
            </ProtectedRoute>
          } />
          <Route path="/transfers/new" element={
            <ProtectedRoute>
              <TransferCreatePage />
            </ProtectedRoute>
          } />
          <Route path="/transfers/:id" element={
            <ProtectedRoute>
              <TransferDetailPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/transfers" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminWorkflowBuilderPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/settings" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminSettingsPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/modules" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <PlatformModulesPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/menus" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <PlatformMenusPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/pages" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <PlatformPagesPage />
            </ProtectedRoute>
          } />
          <Route path="/pages/:slug" element={
            <ProtectedRoute>
              <CustomPageView />
            </ProtectedRoute>
          } />
          <Route path="/module-unavailable" element={
            <ProtectedRoute>
              <ModuleUnavailablePage />
            </ProtectedRoute>
          } />
          <Route path="/admin" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminDashboardPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/custom-fields" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminCustomFieldsPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/feedback" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminFeedbackPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/dictionaries" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminDictionariesPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/tenants" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminTenantsPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/notifications" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminNotificationTemplatesPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/system" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminSystemPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/integrations" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminIntegrationsPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/gdpr" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminGdprPage />
            </ProtectedRoute>
          } />
          <Route path="/ai-assistant" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <AiAssistantPage />
            </ProtectedRoute>
          } />
          <Route path="/tickets" element={
            <ProtectedRoute>
              <TicketsPage />
            </ProtectedRoute>
          } />
          <Route path="/onboarding-wizard" element={
            <ProtectedRoute>
              <OnboardingWizardPage />
            </ProtectedRoute>
          } />
          <Route path="/surveys" element={
            <ProtectedRoute>
              <SurveysPage />
            </ProtectedRoute>
          } />
          <Route path="/leave-requests" element={
            <ProtectedRoute>
              <LeaveRequestsPage />
            </ProtectedRoute>
          } />
          <Route path="/skills-matrix" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <SkillsMatrixPage />
            </ProtectedRoute>
          } />
          <Route path="/referrals" element={
            <ProtectedRoute>
              <ReferralPage />
            </ProtectedRoute>
          } />
          <Route path="/calendar" element={
            <ProtectedRoute>
              <CalendarIntegrationPage />
            </ProtectedRoute>
          } />
          <Route path="/team-gantt" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <TeamGanttPage />
            </ProtectedRoute>
          } />
          <Route path="/testimonials" element={
            <ProtectedRoute>
              <TestimonialsPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/compliance" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <ComplianceDashboardPage />
            </ProtectedRoute>
          } />
          <Route path="/api-docs" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <ApiDocsPage />
            </ProtectedRoute>
          } />
          <Route path="/portal" element={<PublicPortalPage />} />
          <Route path="/cercle-faiseurs" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'FAISEUR']}>
              <CercleFaiseursPage />
            </ProtectedRoute>
          } />
          <Route path="/bible-reading" element={
            <ProtectedRoute>
              <BibleReadingPlanPage />
            </ProtectedRoute>
          } />
          <Route path="/prayer-journal" element={
            <ProtectedRoute>
              <PrayerJournalPage />
            </ProtectedRoute>
          } />
          <Route path="/spiritual-challenges" element={
            <ProtectedRoute>
              <SpiritualChallengesPage />
            </ProtectedRoute>
          } />
          <Route path="/directory" element={
            <ProtectedRoute>
              <ChurchDirectoryPage />
            </ProtectedRoute>
          } />
          <Route path="/spiritual-journey" element={
            <ProtectedRoute>
              <SpiritualJourneyPage />
            </ProtectedRoute>
          } />
          <Route path="/streaming" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <StreamingPage />
            </ProtectedRoute>
          } />
          <Route path="/broadcast" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <BroadcastPage />
            </ProtectedRoute>
          } />
          <Route path="/inventory" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <InventoryPage />
            </ProtectedRoute>
          } />
          <Route path="/department-kpis" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <DepartmentKPIsPage />
            </ProtectedRoute>
          } />
          <Route path="/rewards" element={
            <ProtectedRoute>
              <RewardsPage />
            </ProtectedRoute>
          } />
          <Route path="/marketplace" element={
            <ProtectedRoute>
              <MarketplacePage />
            </ProtectedRoute>
          } />
          <Route path="/community" element={
            <ProtectedRoute>
              <CommunityPage />
            </ProtectedRoute>
          } />
          <Route path="/ai-predictions" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <AiPredictionsPage />
            </ProtectedRoute>
          } />
          <Route path="/personal-objectives" element={
            <ProtectedRoute>
              <PersonalObjectivesPage />
            </ProtectedRoute>
          } />
          <Route path="/family-cohesion" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE']}>
              <FamilyCohesionPage />
            </ProtectedRoute>
          } />
          <Route path="/succession" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <SuccessionPage />
            </ProtectedRoute>
          } />
          <Route path="/pastoral-visits" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'FAISEUR']}>
              <PastoralVisitsPage />
            </ProtectedRoute>
          } />
          <Route path="/family-resources" element={
            <ProtectedRoute>
              <FamilyResourcesPage />
            </ProtectedRoute>
          } />
          <Route path="/automations" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AutomationsPage />
            </ProtectedRoute>
          } />
          <Route path="/mentoring" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE']}>
              <MentoratIAPage />
            </ProtectedRoute>
          } />
          <Route path="/kpi-drilldown" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
              <KpiDrillDownPage />
            </ProtectedRoute>
          } />
          <Route path="/currency-settings" element={<ProtectedRoute roles={['ADMIN']}><CurrencySettingsPage /></ProtectedRoute>} />
          <Route path="/content-moderation" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR']}><ContentModerationPage /></ProtectedRoute>} />
          <Route path="/predictions-ml" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}><PredictionsMLPage /></ProtectedRoute>} />
          <Route path="/intelligence-center" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR']}><IntelligenceCenterPage /></ProtectedRoute>} />
          <Route path="/engagement-analytics" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}><EngagementAnalyticsPage /></ProtectedRoute>} />
          <Route path="/scheduled-announcements" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR']}><ScheduledAnnouncementsPage /></ProtectedRoute>} />
          <Route path="/event-checklists" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}><EventChecklistsPage /></ProtectedRoute>} />
          <Route path="/group-messages" element={<ProtectedRoute><GroupMessagesPage /></ProtectedRoute>} />
          <Route path="/weekly-challenges" element={<ProtectedRoute><WeeklyChallengesPage /></ProtectedRoute>} />
          <Route path="/discipleship-path" element={<ProtectedRoute><DiscipleshipPathPage /></ProtectedRoute>} />
          <Route path="/ai-visit-notes" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR', 'FAISEUR']}><AiVisitNotesPage /></ProtectedRoute>} />
          <Route path="/reverse-mentoring" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR']}><ReverseMentoringPage /></ProtectedRoute>} />
          <Route path="/family-meetings" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE']}><FamilyMeetingPage /></ProtectedRoute>} />
          <Route path="/executive-insights" element={<ProtectedRoute roles={['ADMIN', 'PASTEUR']}><ExecutiveInsightsPage /></ProtectedRoute>} />
          <Route path="/upcoming-events" element={<ProtectedRoute><UpcomingEventsMemberPage /></ProtectedRoute>} />
          <Route path="/my-team" element={<ProtectedRoute><MyTeamFamilyPage /></ProtectedRoute>} />
        </Route>

        {/* Page d'accueil publique (landing) — redirige vers /dashboard si connecté */}
        <Route path="/" element={<HomeGate />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      </Suspense>
    </ErrorBoundary>
  );
}
