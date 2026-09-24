import { render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PricingPage from '@/pages/PricingPage';

const { apiGet, getErrorMessage } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  getErrorMessage: vi.fn().mockReturnValue('Service indisponible'),
}));

vi.mock('@/lib/api', () => ({
  default: { get: apiGet },
  getErrorMessage,
}));

const renderPage = () => render(
  <MemoryRouter>
    <PricingPage />
  </MemoryRouter>
);

describe('PricingPage — catalogue public', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('discipolat-locale', 'fr');
  });

  it('affiche chaque prix fourni sans supposer une devise unique', async () => {
    apiGet.mockResolvedValue({
      data: [
        {
          key: 'GROWTH',
          name: 'Croissance',
          description: 'Pour les églises en expansion',
          priceMonthly: 0,
          priceEur: 29,
          priceXaf: 17500,
          priceUsd: 32,
          currency: 'EUR',
          seatsLimit: 2000,
          storageLimitMb: 20480,
          aiCreditsLimit: 100,
          billingPeriod: 'monthly',
          sortOrder: 1,
        },
        {
          key: 'CUSTOM',
          name: 'Sur mesure',
          description: null,
          priceMonthly: null,
          priceEur: null,
          priceXaf: null,
          priceUsd: null,
          currency: null,
          seatsLimit: null,
          storageLimitMb: null,
          aiCreditsLimit: null,
          billingPeriod: null,
          sortOrder: 2,
        },
      ],
    });

    renderPage();

    const growthHeading = await screen.findByRole('heading', { name: 'Croissance' });
    const growthCard = growthHeading.closest('article');
    expect(growthCard).not.toBeNull();
    expect(within(growthCard as HTMLElement).getByText(/29/)).toBeInTheDocument();
    expect(within(growthCard as HTMLElement).getByText('EUR')).toBeInTheDocument();
    expect(within(growthCard as HTMLElement).getByText(/17.?500/)).toBeInTheDocument();
    expect(within(growthCard as HTMLElement).getByText('XAF')).toBeInTheDocument();
    expect(within(growthCard as HTMLElement).getByText(/32/)).toBeInTheDocument();
    expect(within(growthCard as HTMLElement).getByText('USD')).toBeInTheDocument();

    const customHeading = screen.getByRole('heading', { name: 'Sur mesure' });
    const customCard = customHeading.closest('article') as HTMLElement;
    expect(within(customCard).getByText('Tarif non communiqué')).toBeInTheDocument();
    expect(within(customCard).queryByText(/^0/)).not.toBeInTheDocument();
    expect(within(customCard).getByText('Utilisateurs non précisés')).toBeInTheDocument();
    expect(within(customCard).getByText('Stockage non précisé')).toBeInTheDocument();
    expect(within(customCard).getByText('Crédits IA non précisés')).toBeInTheDocument();
  });

  it('affiche clairement le catalogue de secours en cas d’erreur', async () => {
    apiGet.mockRejectedValue(new Error('network'));

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Catalogue momentanément indisponible : Service indisponible');
    });
    expect(screen.getByRole('heading', { name: 'Découverte' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Démarrage' })).toBeInTheDocument();
  });
});
