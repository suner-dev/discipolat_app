import React, { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { WORKSPACE_HOME, isSuperUser } from '@/workspaces';
import type { UserRole } from '@/types';
import MainLayout from '@/layouts/MainLayout';
import AuthLayout from '@/layouts/AuthLayout';
import ErrorBoundary from '@/components/shared/ErrorBoundary';

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
const PermissionsPage = lazy(() => import('@/pages/PermissionsPage'));
const CompareFamiliesPage = lazy(() => import('@/pages/CompareFamiliesPage'));
const SoulRetractionsPage = lazy(() => import('@/pages/SoulRetractionsPage'));
const ActionsDeGracePage = lazy(() => import('@/pages/ActionsDeGracePage'));
const UrgentAidPage = lazy(() => import('@/pages/UrgentAidPage'));
const IntelligentSearchPage = lazy(() => import('@/pages/IntelligentSearchPage'));
const Pastoral360Page = lazy(() => import('@/pages/Pastoral360Page'));
const CrmFaiseurPage = lazy(() => import('@/pages/CrmFaiseurPage'));
const FinancePage = lazy(() => import('@/pages/FinancePage'));
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
const TransferAdminPage = lazy(() => import('@/pages/TransferAdminPage'));
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
    return <Navigate to="/dashboard" replace />;
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
      <Suspense fallback={<RouteFallback />}>
        <Routes>
        {/* Auth routes */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
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
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
              <PrayersPage />
            </ProtectedRoute>
          } />
          <Route path="/events" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
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
          } />
          <Route path="/permissions" element={
            <ProtectedRoute roles={['ADMIN']}>
              <PermissionsPage />
            </ProtectedRoute>
          } />
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
            <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
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
              <TransferAdminPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/settings" element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminSettingsPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/modules" element={
            <ProtectedRoute roles={['ADMIN']}>
              <PlatformModulesPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/menus" element={
            <ProtectedRoute roles={['ADMIN']}>
              <PlatformMenusPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/pages" element={
            <ProtectedRoute roles={['ADMIN']}>
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
            <ProtectedRoute roles={['ADMIN']}>
              <AdminDashboardPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/custom-fields" element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminCustomFieldsPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/feedback" element={
            <ProtectedRoute roles={['ADMIN', 'PASTEUR']}>
              <AdminFeedbackPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/dictionaries" element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminDictionariesPage />
            </ProtectedRoute>
          } />
        </Route>

        {/* Page d'accueil publique (landing) — redirige vers /dashboard si connecté */}
        <Route path="/" element={<HomeGate />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      </Suspense>
    </ErrorBoundary>
  );
}
