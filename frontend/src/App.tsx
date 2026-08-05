import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import MainLayout from '@/layouts/MainLayout';
import AuthLayout from '@/layouts/AuthLayout';
import LoginPage from '@/pages/LoginPage';
import TwoFactorChallengePage from '@/pages/TwoFactorChallengePage';
import ForgotPasswordPage from '@/pages/ForgotPasswordPage';
import ResetPasswordPage from '@/pages/ResetPasswordPage';
import DashboardPage from '@/pages/DashboardPage';
import PasteurDashboardPage from '@/pages/PasteurDashboardPage';
import ChefFamilleDashboardPage from '@/pages/ChefFamilleDashboardPage';
import ResponsableDashboardPage from '@/pages/ResponsableDashboardPage';
import SoulsPage from '@/pages/SoulsPage';
import SoulDetailPage from '@/pages/SoulDetailPage';
import FamiliesPage from '@/pages/FamiliesPage';
import FamilyDetailPage from '@/pages/FamilyDetailPage';
import DepartmentsPage from '@/pages/DepartmentsPage';
import DepartmentDetailPage from '@/pages/DepartmentDetailPage';
import DepartmentReportPage from '@/pages/DepartmentReportPage';
import ReportsPage from '@/pages/ReportsPage';
import MakerReportPage from '@/pages/MakerReportPage';
import FamilyReportPage from '@/pages/FamilyReportPage';
import ParallelFollowupsPage from '@/pages/ParallelFollowupsPage';
import AlertsPage from '@/pages/AlertsPage';
import UsersPage from '@/pages/UsersPage';
import ProfilePage from '@/pages/ProfilePage';
import SoulCreatePage from '@/pages/SoulCreatePage';
import SoulEditPage from '@/pages/SoulEditPage';
import FamilyCreatePage from '@/pages/FamilyCreatePage';
import FamilyFaiseurPerformancePage from '@/pages/FamilyFaiseurPerformancePage';
import PrayersPage from '@/pages/PrayersPage';
import EventsPage from '@/pages/EventsPage';
import DocumentsPage from '@/pages/DocumentsPage';
import AuditPage from '@/pages/AuditPage';
import PermissionsPage from '@/pages/PermissionsPage';
import CompareFamiliesPage from '@/pages/CompareFamiliesPage';
import SoulRetractionsPage from '@/pages/SoulRetractionsPage';
import ActionsDeGracePage from '@/pages/ActionsDeGracePage';
import UrgentAidPage from '@/pages/UrgentAidPage';
import IntelligentSearchPage from '@/pages/IntelligentSearchPage';
import Pastoral360Page from '@/pages/Pastoral360Page';
import CrmFaiseurPage from '@/pages/CrmFaiseurPage';
import NotFoundPage from '@/pages/NotFoundPage';
import WeeklyProgramPage from '@/pages/WeeklyProgramPage';
import EventStatisticsPage from '@/pages/EventStatisticsPage';
import PrayerSpacesPage from '@/pages/PrayerSpacesPage';
import EvaluationsPage from '@/pages/EvaluationsPage';
import MemberDashboardPage from '@/pages/MemberDashboardPage';
import MemberRequestsPage from '@/pages/MemberRequestsPage';
import MessagesPage from '@/pages/MessagesPage';
import EvangelismPage from '@/pages/EvangelismPage';
import ObjectivesPage from '@/pages/ObjectivesPage';
import VisitsPage from '@/pages/VisitsPage';
import BadgesPage from '@/pages/BadgesPage';
import TrainingsPage from '@/pages/TrainingsPage';

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
  if (roles && currentRole && !roles.includes(currentRole)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}

/**
 * Tableau de bord racine : un membre pur (sans autre rôle) est redirigé
 * vers son Espace Membre ; les autres rôles gardent le dashboard général.
 */
function DashboardGate() {
  const { hasRole } = useAuth();
  const isPureMembre =
    hasRole('MEMBRE') &&
    !hasRole('FAISEUR') &&
    !hasRole('RESPONSABLE') &&
    !hasRole('CHEF_DE_FAMILLE') &&
    !hasRole('PASTEUR') &&
    !hasRole('ADMIN');
  if (isPureMembre) {
    return <Navigate to="/dashboard/membre" replace />;
  }
  return <DashboardPage />;
}

export default function App() {
  return (
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
          <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
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
          <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE']}>
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
        <Route path="/reports" element={
          <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
            <ReportsPage />
          </ProtectedRoute>
        } />
        <Route path="/reports/maker" element={
          <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
            <MakerReportPage />
          </ProtectedRoute>
        } />
        <Route path="/reports/family" element={
          <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
            <FamilyReportPage />
          </ProtectedRoute>
        } />
        <Route path="/families/:id/faiseur-performance" element={
          <ProtectedRoute roles={['PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
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
        <Route path="/members/requests" element={
          <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE']}>
            <MemberRequestsPage />
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
          <ProtectedRoute roles={['PASTEUR']}>
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
          <ProtectedRoute roles={['PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
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
        <Route path="/souls/:id/pastoral-360" element={
          <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
            <Pastoral360Page />
          </ProtectedRoute>
        } />
        <Route path="/crm/faiseur" element={
          <ProtectedRoute roles={['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']}>
            <CrmFaiseurPage />
          </ProtectedRoute>
        } />
        <Route path="/events/statistics" element={
          <ProtectedRoute roles={['PASTEUR']}>
            <EventStatisticsPage />
          </ProtectedRoute>
        } />
      </Route>

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
