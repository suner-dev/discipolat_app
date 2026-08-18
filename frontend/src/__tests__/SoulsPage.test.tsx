import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import SoulsPage from '@/pages/SoulsPage';

const mockGet = vi.fn();

vi.mock('@/lib/api', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: vi.fn().mockResolvedValue({ data: {} }),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function renderWithRoute(route: string) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <AuthProvider>
          <SoulsPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

beforeEach(() => {
  queryClient.clear();
  mockGet.mockReset();
  mockGet.mockImplementation((url: string) => {
    if (url.startsWith('/favorites/souls')) return Promise.resolve({ data: [] });
    if (url.startsWith('/dictionaries')) return Promise.resolve({ data: {} });
    if (url.startsWith('/souls')) return Promise.resolve({
      data: { content: [], totalElements: 0, totalPages: 1, number: 0, size: 20, first: true, last: true },
    });
    return Promise.resolve({ data: { content: [] } });
  });
});

describe('SoulsPage — query params (KPI cliquables des dashboards)', () => {
  it('applique le filtre statut présent dans l’URL', async () => {
    renderWithRoute('/souls?statut=DECROCHE');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls'));
      expect(String(call?.[0])).toContain('statut=DECROCHE');
    });
    // Les filtres sont pré-ouverts quand l'URL porte un filtre.
    expect(screen.getByDisplayValue('Décroché')).toBeInTheDocument();
  });

  it('applique le filtre typeDisciple présent dans l’URL', async () => {
    renderWithRoute('/souls?typeDisciple=NOUVEAU_CONVERTI');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls'));
      expect(String(call?.[0])).toContain('typeDisciple=NOUVEAU_CONVERTI');
    });
    expect(screen.getByDisplayValue('Nouveau converti')).toBeInTheDocument();
  });

  it('applique la recherche présente dans l’URL', async () => {
    renderWithRoute('/souls?search=jean');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls'));
      expect(String(call?.[0])).toContain('search=jean');
    });
    expect(screen.getByDisplayValue('jean')).toBeInTheDocument();
  });

  it('affiche la corbeille quand l’URL porte view=corbeille', async () => {
    renderWithRoute('/souls?view=corbeille');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls/trash'));
      expect(call).toBeTruthy();
    });
    expect(screen.getByText('Voir les âmes')).toBeInTheDocument();
  });

  it('reste sur la liste quand aucun filtre n’est présent', async () => {
    renderWithRoute('/souls');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls'));
      expect(String(call?.[0])).not.toContain('statut=');
      expect(String(call?.[0])).not.toContain('typeDisciple=');
    });
  });
});
