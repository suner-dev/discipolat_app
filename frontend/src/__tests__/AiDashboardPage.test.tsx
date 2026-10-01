import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import AiDashboardPage from '@/pages/ai/AiDashboardPage';

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn(() => 'Erreur'),
}));

// Chaque route renvoie un payload minimal ; /ai/credits/dashboard pilote le bloc testé.
function stub(payloads: Record<string, unknown>) {
  apiGet.mockImplementation((url: string) => {
    if (url in payloads) return Promise.resolve({ data: payloads[url] });
    return Promise.resolve({ data: null });
  });
}

describe('AiDashboardPage — self-service crédits IA (§G6.2, audit parité G6.10)', () => {
  beforeEach(() => apiGet.mockReset());

  it('consomme /ai/credits/dashboard et affiche la jauge de quota mensuel', async () => {
    stub({
      '/ai/credits/dashboard': {
        totalCredits: 340, totalRequests: 52,
        monthlyLimit: 500, usedThisMonth: 120, remainingThisMonth: 380,
        byType: [{ type: 'chat', count: 40, credits: 200 }],
        byModel: [{ model: 'groq', count: 30, credits: 150 }],
      },
    });
    render(<AiDashboardPage />);
    await waitFor(() =>
      expect(apiGet).toHaveBeenCalledWith('/ai/credits/dashboard'),
    );
    expect(await screen.findByText('Usage & crédits IA')).toBeInTheDocument();
    expect(screen.getByText('120 / 500 crédits ce mois')).toBeInTheDocument();
    expect(screen.getByText('380 restants')).toBeInTheDocument();
    expect(screen.getByText('chat')).toBeInTheDocument();
    expect(screen.getByText('groq')).toBeInTheDocument();
  });

  it('affiche la mention « sans quota » quand le plan na pas de limite IA (null-safe)', async () => {
    stub({
      '/ai/credits/dashboard': {
        totalCredits: 10, totalRequests: 3,
        monthlyLimit: null, usedThisMonth: 0, remainingThisMonth: null,
        byType: [], byModel: [],
      },
    });
    render(<AiDashboardPage />);
    expect(await screen.findByText(/Plan sans quota IA mensuel/)).toBeInTheDocument();
  });
});
