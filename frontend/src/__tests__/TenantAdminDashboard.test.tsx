import { act, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import TenantAdminDashboard from '@/pages/TenantAdminDashboard';

const { apiGet, getErrorMessage } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  getErrorMessage: vi.fn().mockReturnValue('Accès refusé par le serveur'),
}));

vi.mock('@/lib/api', () => ({
  default: { get: apiGet },
  getErrorMessage,
}));

vi.mock('@/components/admin/QuotaUsageCards', () => ({
  QuotaUsagePanel: () => null,
}));

const overview = {
  tenantId: 'tenant-1',
  tenantName: 'Église Centrale',
  plan: 'growth',
  users: {
    total: 42,
    active: 40,
    inactive: 2,
    totalMemberships: 41,
    activeMemberships: 39,
    pendingMemberships: 2,
    membersByRole: { TENANT_ADMIN: 3, MEMBRE: 36 },
  },
  organizations: {
    churches: 1,
    campuses: 2,
    subChurches: 3,
    departments: 4,
    groups: 5,
  },
  subscription: {
    planKey: 'growth',
    status: 'ACTIVE',
    billingCycle: 'monthly',
    currentPeriodEnd: '2026-10-01T00:00:00Z',
    plan: { name: 'Croissance' },
  },
  quotas: {},
  activity: { auditLogsLast7Days: 8 },
};

describe('TenantAdminDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('charge et interprète le vrai overview imbriqué', async () => {
    let resolveRequest: (value: { data: typeof overview }) => void = () => undefined;
    apiGet.mockReturnValue(new Promise((resolve) => {
      resolveRequest = resolve;
    }));

    render(<TenantAdminDashboard />);

    expect(screen.getByRole('status')).toHaveTextContent('Chargement du tableau de bord');
    expect(apiGet).toHaveBeenCalledWith('/admin/dashboard/overview');

    await act(async () => {
      resolveRequest({ data: overview });
    });

    expect(screen.getByRole('heading', { name: 'Tableau de bord — Église Centrale' })).toBeInTheDocument();
    const usersCard = screen.getByText('Utilisateurs').closest('article');
    const churchesCard = screen.getByText('Églises').closest('article');
    const departmentsCard = screen.getByText('Départements').closest('article');
    const adminRoleCard = screen.getByText('TENANT_ADMIN').parentElement;
    expect(within(usersCard as HTMLElement).getByText('42')).toBeInTheDocument();
    expect(within(churchesCard as HTMLElement).getByText('1')).toBeInTheDocument();
    expect(within(departmentsCard as HTMLElement).getByText('4')).toBeInTheDocument();
    expect(within(adminRoleCard as HTMLElement).getByText('3')).toBeInTheDocument();
    expect(screen.getByText('36')).toBeInTheDocument();
  });

  it('affiche l’erreur autoritaire du backend', async () => {
    apiGet.mockRejectedValue(new Error('forbidden'));

    render(<TenantAdminDashboard />);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Accès refusé par le serveur');
    });
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeInTheDocument();
  });
});
