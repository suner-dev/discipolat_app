import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ConfigRevisionHistory from '@/components/ConfigRevisionHistory';

const mockGet = vi.fn();

vi.mock('@/lib/api', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
}));

const REVISIONS = {
  data: {
    content: [
      { id: 'r1', entityType: 'PLATFORM_MODULE', entityKey: 'SOULS', action: 'MODULE_DISABLED', createdAt: '2026-08-17T10:00:00' },
      { id: 'r2', entityType: 'PLATFORM_MODULE', entityKey: 'AUDIT', action: 'MODULE_CREATED', createdAt: '2026-08-16T09:00:00' },
    ],
    totalElements: 2,
  },
};

function renderHistory(entityType = 'PLATFORM_MODULE') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ConfigRevisionHistory entityType={entityType} />
    </QueryClientProvider>
  );
}

describe('ConfigRevisionHistory — versionnage', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockGet.mockImplementation((url: string) => {
      if (url.includes('/platform/revisions')) return Promise.resolve(REVISIONS);
      return Promise.resolve({ data: {} });
    });
  });

  it('affiche le panneau replié sans charger les révisions au départ', () => {
    renderHistory();
    expect(screen.getByText(/historique des modifications/i)).toBeInTheDocument();
    expect(mockGet).not.toHaveBeenCalled();
    expect(screen.queryByText(/MODULE_DISABLED/i)).not.toBeInTheDocument();
  });

  it('charge et affiche les révisions à l’ouverture', async () => {
    const user = userEvent.setup();
    renderHistory();
    await user.click(screen.getByText(/historique des modifications/i));

    expect(await screen.findByText('Module désactivé')).toBeInTheDocument();
    expect(screen.getByText('Module créé')).toBeInTheDocument();
    expect(screen.getByText('SOULS')).toBeInTheDocument();

    await waitFor(() => {
      expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('entityType=PLATFORM_MODULE'));
    });
  });

  it('affiche un état vide quand aucune révision', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url.includes('/platform/revisions')) return Promise.resolve({ data: { content: [] } });
      return Promise.resolve({ data: {} });
    });
    const user = userEvent.setup();
    renderHistory('PLATFORM_MENU');
    await user.click(screen.getByText(/historique des modifications/i));

    expect(await screen.findByText(/aucune modification enregistrée/i)).toBeInTheDocument();
  });
});
