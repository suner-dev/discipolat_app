import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PlatformAdminDashboard from '@/pages/PlatformAdminDashboard';

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }));

vi.mock('@/lib/api', () => ({
  default: { get: apiGet },
  getErrorMessage: vi.fn().mockReturnValue('Erreur de chargement'),
}));

interface Deferred<T> {
  promise: Promise<T>;
  resolve: (value: T) => void;
}

const deferred = <T,>(): Deferred<T> => {
  let resolve: (value: T) => void = () => undefined;
  const promise = new Promise<T>((complete) => {
    resolve = complete;
  });
  return { promise, resolve };
};

const tenant = (id: string, name: string) => ({
  id,
  name,
  slug: id.toLowerCase(),
  status: 'ACTIVE',
  plan: 'free',
  country: 'CM',
  currency: 'XAF',
  timezone: 'Africa/Douala',
  locale: 'fr',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
});

const usageResponse = (tenantId: string, used: number) => ({
  data: {
    tenantId,
    metrics: [{
      key: 'users',
      label: 'Utilisateurs',
      usage: used,
      limit: 100,
      percent: used,
      source: 'user_repository',
      enforced: true,
      available: true,
    }],
  },
});

describe('PlatformAdminDashboard — usage par tenant', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiGet.mockImplementation((url: string) => {
      if (url === '/platform/admin/dashboard') {
        return Promise.resolve({ data: { tenants: {}, users: {}, organizations: {}, activity: {} } });
      }
      if (url === '/platform/admin/plans') return Promise.resolve({ data: [] });
      if (url === '/platform/admin/tenants?size=100') {
        return Promise.resolve({ data: { content: [tenant('tenant-a', 'Église A'), tenant('tenant-b', 'Église B')] } });
      }
      if (url === '/platform/admin/feature-flags') return Promise.resolve({ data: { details: [] } });
      return Promise.reject(new Error(`Requête inattendue : ${url}`));
    });
  });

  it('ignore une réponse tardive appartenant à un tenant précédent', async () => {
    const tenantA = deferred<ReturnType<typeof usageResponse>>();
    const tenantB = deferred<ReturnType<typeof usageResponse>>();
    apiGet.mockImplementation((url: string) => {
      if (url === '/platform/admin/dashboard') {
        return Promise.resolve({ data: { tenants: {}, users: {}, organizations: {}, activity: {} } });
      }
      if (url === '/platform/admin/plans') return Promise.resolve({ data: [] });
      if (url === '/platform/admin/tenants?size=100') {
        return Promise.resolve({ data: { content: [tenant('tenant-a', 'Église A'), tenant('tenant-b', 'Église B')] } });
      }
      if (url === '/platform/admin/feature-flags') return Promise.resolve({ data: { details: [] } });
      if (url === '/platform/admin/quota-usage/tenants/tenant-a') return tenantA.promise;
      if (url === '/platform/admin/quota-usage/tenants/tenant-b') return tenantB.promise;
      return Promise.reject(new Error(`Requête inattendue : ${url}`));
    });

    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <PlatformAdminDashboard />
      </MemoryRouter>
    );

    await screen.findByRole('heading', { name: 'Administration Plateforme' });
    await user.click(screen.getByRole('button', { name: 'Usage par tenant' }));
    const selector = screen.getByRole('combobox', { name: 'Tenant' });

    await user.selectOptions(selector, 'tenant-a');
    await user.click(screen.getByRole('button', { name: "Charger l'usage" }));
    await waitFor(() => expect(apiGet).toHaveBeenCalledWith('/platform/admin/quota-usage/tenants/tenant-a'));

    await user.selectOptions(selector, 'tenant-b');
    await user.click(screen.getByRole('button', { name: "Charger l'usage" }));
    await waitFor(() => expect(apiGet).toHaveBeenCalledWith('/platform/admin/quota-usage/tenants/tenant-b'));

    await act(async () => {
      tenantB.resolve(usageResponse('tenant-b', 222));
      await tenantB.promise;
    });
    expect(await screen.findByText('222')).toBeInTheDocument();

    await act(async () => {
      tenantA.resolve(usageResponse('tenant-a', 111));
      await tenantA.promise;
    });
    expect(screen.getByText('222')).toBeInTheDocument();
    expect(screen.queryByText('111')).not.toBeInTheDocument();
  });
});
