import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { QuotaUsageCards, QuotaUsagePanel } from '@/components/admin/QuotaUsageCards';
import { normalizeQuotaUsage } from '@/types/quota';

const { apiGet, getErrorMessage } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  getErrorMessage: vi.fn().mockReturnValue('Le chargement a échoué.'),
}));

vi.mock('@/lib/api', () => ({
  default: { get: apiGet },
  getErrorMessage,
}));

function renderWithQuery(element: React.ReactNode) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      {element}
    </QueryClientProvider>
  );
}

describe('QuotaUsage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('affiche un état de chargement pendant la requête', () => {
    let resolveRequest: (value: { data: unknown }) => void = () => undefined;
    apiGet.mockReturnValue(new Promise((resolve) => {
      resolveRequest = resolve;
    }));

    renderWithQuery(<QuotaUsagePanel />);

    expect(screen.getByRole('status')).toHaveTextContent('Chargement des quotas');
    expect(apiGet).toHaveBeenCalledWith('/admin/quotas/usage');
    resolveRequest({ data: { users: { used: 1, limit: 10, percent: 10 } } });
  });

  it('normalise les réponses anciennes et nouvelles', () => {
    const legacy = normalizeQuotaUsage({
      tenantId: 'tenant-1',
      planKey: 'free',
      users: { used: 3, limit: 10, percent: 30 },
      storage: { usedMb: 0, limitMb: 100, percent: 0 },
      courses: { used: 0, limit: 5, percent: 0 },
    });
    const current = normalizeQuotaUsage({
      tenantId: 'tenant-2',
      metrics: [
        { key: 'users', usage: 4, limit: 10, source: 'user_repository', enforced: true, available: true },
        { key: 'ai_requests', usage: 0, limit: null, unlimited: true, source: 'usage_events', enforced: false, available: true },
      ],
      limits: { max_departments: 12 },
    });
    const platform = normalizeQuotaUsage({
      snapshot: {
        tenantId: 'tenant-3',
        plan: { canonicalKey: 'GROWTH', name: 'Growth' },
        storageBytes: { used: 1048576, limit: 10485760, utilizationPercent: 10, unit: 'BYTES', source: 'PERSISTED_FILES', serverEnforced: true },
        aiCredits: { used: 0, limit: 100, utilizationPercent: 0, unit: 'AI_CREDITS', source: 'PERSISTED_AI_USAGE', serverEnforced: true },
      },
    });

    expect(legacy.tenantId).toBe('tenant-1');
    expect(legacy.metrics.find((metric) => metric.key === 'users')).toMatchObject({ usage: 3, limit: 10, percent: 30 });
    expect(legacy.metrics.find((metric) => metric.key === 'storage')).toMatchObject({ usage: 0, available: false, percent: null });
    expect(legacy.metrics.find((metric) => metric.key === 'courses')?.available).toBe(false);
    expect(current.metrics.find((metric) => metric.key === 'users')).toMatchObject({ usage: 4, limit: 10, source: 'user_repository', enforced: true, available: true });
    expect(current.metrics.find((metric) => metric.key === 'aiRequests')).toMatchObject({ unlimited: true, source: 'usage_events', enforced: false, available: true });
    expect(current.metrics.find((metric) => metric.key === 'departments')).toMatchObject({ limit: 12, usage: null, available: false });
    expect(platform.tenantId).toBe('tenant-3');
    expect(platform.planKey).toBe('GROWTH');
    expect(platform.metrics.find((metric) => metric.key === 'storage')).toMatchObject({ usage: 1048576, limit: 10485760, percent: 10, unit: 'BYTES', source: 'PERSISTED_FILES', enforced: true, available: true });
    expect(platform.metrics.find((metric) => metric.key === 'aiRequests')).toMatchObject({ usage: 0, source: 'PERSISTED_AI_USAGE', enforced: true, available: true });
  });

  it('nomme les limites illimitées et les quotas non appliqués', () => {
    const usage = normalizeQuotaUsage({
      metrics: [
        { key: 'users', usage: 12, limit: null, unlimited: true, source: 'user_repository', enforced: false },
        { key: 'churches', usage: null, limit: 1, enforced: false },
      ],
    });

    render(<QuotaUsageCards metrics={usage.metrics} />);

    expect(screen.getByText(/Illimité/)).toBeInTheDocument();
    expect(screen.getAllByText('Limite non appliquée')).toHaveLength(2);
    expect(screen.getByText('Indisponible')).toBeInTheDocument();
  });

  it('affiche une erreur lorsque la requête échoue', async () => {
    apiGet.mockRejectedValue(new Error('network'));

    renderWithQuery(<QuotaUsagePanel />);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Le chargement a échoué.');
    });
  });

  it('ne lance pas de requête lorsque le panneau est désactivé', () => {
    renderWithQuery(<QuotaUsagePanel enabled={false} />);

    expect(apiGet).not.toHaveBeenCalled();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});
