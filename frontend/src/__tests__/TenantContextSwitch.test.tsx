import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import { StrictMode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import api from '@/lib/api';
import { TenantProvider, useTenant } from '@/contexts/TenantContext';

// -- Mocks -------------------------------------------------------------------
vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), put: vi.fn(), post: vi.fn() },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

function stubApi() {
  (api.get as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
    if (url === '/tenant-switcher/context') {
      return Promise.resolve({
        data: {
          tenantId: 't1', tenantName: 'Église Béréa', tenantSlug: 'berea',
          tenantStatus: 'ACTIVE', userId: 'u1', role: 'ADMIN',
          scopeType: 'TENANT', scopeId: null,
          permissions: ['ORG_NODE_MOVE'],
          accessibleNodes: [], branding: null, features: { FINANCES: true }, settings: null,
        },
      });
    }
    if (url === '/tenant-switcher/my-tenants') {
      return Promise.resolve({ data: [] });
    }
    if (url === '/admin/roles') {
      return Promise.resolve({ data: [] });
    }
    return Promise.resolve({ data: null });
  });
}

function Consumer({ onReady }: { onReady?: (ctx: ReturnType<typeof useTenant>) => void }) {
  const ctx = useTenant();
  if (ctx.isInitialized && onReady) onReady(ctx);
  return (
    <button type="button" onClick={() => void ctx.switchTenant('t2')}>
      Basculer
    </button>
  );
}

function renderProvider(queryClient = new QueryClient()) {
  return render(
    <QueryClientProvider client={queryClient}>
      <TenantProvider>
        <Consumer />
      </TenantProvider>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  stubApi();
  localStorage.clear();
});

describe('TenantContext G5.4 (§55) — provider finalisé', () => {
  it('monté, il résout le contexte tenant et les permissions serveur', async () => {
    renderProvider();
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/tenant-switcher/context');
    });
    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/tenant-switcher/my-tenants');
    });
  });

  it('chargement unique : StrictMode (double montage) ne double pas le fetch du contexte', async () => {
    const queryClient = new QueryClient();
    await act(async () => {
      render(
        <StrictMode>
          <QueryClientProvider client={queryClient}>
            <TenantProvider>
              <Consumer />
            </TenantProvider>
          </QueryClientProvider>
        </StrictMode>,
      );
    });
    await waitFor(() => {
      const calls = (api.get as ReturnType<typeof vi.fn>).mock.calls.filter(
        (c) => c[0] === '/tenant-switcher/context',
      );
      // Une seule vague de fetch en vol (StrictMode) — pas deux requêtes superposées.
      expect(calls.length).toBe(1);
    });
  });

  it('switchTenant : stocke les nouveaux JWT, vide le cache de requêtes, recharge le contexte — sans rechargement de page', async () => {
    localStorage.setItem('accessToken', 'OLD');
    localStorage.setItem('refreshToken', 'OLD-R');
    const queryClient = new QueryClient();
    const clearSpy = vi.spyOn(queryClient, 'clear');
    (api.post as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: { success: true, tenantId: 't2', accessToken: 'NEW', refreshToken: 'NEW-R', role: 'PASTEUR' },
    });

    renderProvider(queryClient);
    await screen.findByText('Basculer');
    const before = (api.get as ReturnType<typeof vi.fn>).mock.calls.filter(
      (c) => c[0] === '/tenant-switcher/context',
    ).length;

    fireEvent.click(screen.getByText('Basculer'));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/tenant-switcher/switch', { tenantId: 't2' });
    });
    await waitFor(() => {
      expect(localStorage.getItem('accessToken')).toBe('NEW');
      expect(localStorage.getItem('refreshToken')).toBe('NEW-R');
    });
    expect(clearSpy).toHaveBeenCalled();
    await waitFor(() => {
      const after = (api.get as ReturnType<typeof vi.fn>).mock.calls.filter(
        (c) => c[0] === '/tenant-switcher/context',
      ).length;
      expect(after).toBeGreaterThan(before);
    });
  });

  it('sans tenant résolu (requiresSelection), les cartes de permissions sont vidées — pas d\'accès fantôme', async () => {
    (api.get as ReturnType<typeof vi.fn>).mockImplementation((url: string) => {
      if (url === '/tenant-switcher/context') {
        return Promise.resolve({
          data: { requiresSelection: true, availableTenants: [{ id: 't1', name: 'A' }, { id: 't2', name: 'B' }] },
        });
      }
      if (url === '/tenant-switcher/my-tenants') return Promise.resolve({ data: [] });
      return Promise.resolve({ data: null });
    });
    let ctx: ReturnType<typeof useTenant> | undefined;
    render(
      <QueryClientProvider client={new QueryClient()}>
        <TenantProvider>
          <Consumer onReady={(c) => { ctx = c; }} />
        </TenantProvider>
      </QueryClientProvider>,
    );
    await waitFor(() => {
      const calls = (api.get as ReturnType<typeof vi.fn>).mock.calls.filter(
        (c) => c[0] === '/tenant-switcher/context',
      );
      expect(calls.length).toBe(1);
    });
    await waitFor(() => expect(ctx?.isInitialized).toBe(true));
    expect(ctx?.currentTenant).toBeNull();
    expect(ctx?.hasPermission('ORG_NODE_MOVE')).toBe(false);
  });
});
