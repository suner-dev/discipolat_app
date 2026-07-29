import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import MainLayout from '@/layouts/MainLayout';
import AuthLayout from '@/layouts/AuthLayout';
import LoginPage from '@/pages/LoginPage';
import ForgotPasswordPage from '@/pages/ForgotPasswordPage';
import ResetPasswordPage from '@/pages/ResetPasswordPage';
import DashboardPage from '@/pages/DashboardPage';
import SoulsPage from '@/pages/SoulsPage';
import SoulDetailPage from '@/pages/SoulDetailPage';
import FamiliesPage from '@/pages/FamiliesPage';
import FamilyDetailPage from '@/pages/FamilyDetailPage';
import DepartmentsPage from '@/pages/DepartmentsPage';
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
import PrayersPage from '@/pages/PrayersPage';
import EventsPage from '@/pages/EventsPage';
import DocumentsPage from '@/pages/DocumentsPage';

function ProtectedRoute({ children, roles }: { children: React.ReactNode; roles?: string[] }) {
  const { isAuthenticated, user, isLoading } = useAuth();

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

  if (roles && user && !roles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}

export default function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      {/* Auth routes */}
      <Route element={<AuthLayout />}>
        <Route
          path="/login"
          element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />}
        />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
      </Route>

      {/* Protected routes */}
      <Route element={<MainLayout />}>
        <Route path="/dashboard" element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        } />
        <Route path="/souls" element={
          <ProtectedRoute>
            <SoulsPage />
          </ProtectedRoute>
        } />
        <Route path="/souls/new" element={
          <ProtectedRoute>
            <SoulCreatePage />
          </ProtectedRoute>
        } />
        <Route path="/souls/:id" element={
          <ProtectedRoute>
            <SoulDetailPage />
          </ProtectedRoute>
        } />
        <Route path="/souls/:id/edit" element={
          <ProtectedRoute>
            <SoulEditPage />
          </ProtectedRoute>
        } />
        <Route path="/families" element={
          <ProtectedRoute>
            <FamiliesPage />
          </ProtectedRoute>
        } />
        <Route path="/families/new" element={
          <ProtectedRoute>
            <FamilyCreatePage />
          </ProtectedRoute>
        } />
        <Route path="/families/:id" element={
          <ProtectedRoute>
            <FamilyDetailPage />
          </ProtectedRoute>
        } />
        <Route path="/departments" element={
          <ProtectedRoute roles={['PASTEUR', 'RESPONSABLE']}>
            <DepartmentsPage />
          </ProtectedRoute>
        } />
        <Route path="/reports" element={
          <ProtectedRoute>
            <ReportsPage />
          </ProtectedRoute>
        } />
        <Route path="/reports/maker" element={
          <ProtectedRoute>
            <MakerReportPage />
          </ProtectedRoute>
        } />
        <Route path="/reports/family" element={
          <ProtectedRoute>
            <FamilyReportPage />
          </ProtectedRoute>
        } />
        <Route path="/prayers" element={
          <ProtectedRoute>
            <PrayersPage />
          </ProtectedRoute>
        } />
        <Route path="/events" element={
          <ProtectedRoute>
            <EventsPage />
          </ProtectedRoute>
        } />
        <Route path="/documents" element={
          <ProtectedRoute>
            <DocumentsPage />
          </ProtectedRoute>
        } />
        <Route path="/parallel-followups" element={
          <ProtectedRoute>
            <ParallelFollowupsPage />
          </ProtectedRoute>
        } />
        <Route path="/alerts" element={
          <ProtectedRoute roles={['PASTEUR', 'RESPONSABLE', 'FAISEUR']}>
            <AlertsPage />
          </ProtectedRoute>
        } />
        <Route path="/users" element={
          <ProtectedRoute roles={['PASTEUR', 'RESPONSABLE']}>
            <UsersPage />
          </ProtectedRoute>
        } />
        <Route path="/profile" element={
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        } />
      </Route>

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
