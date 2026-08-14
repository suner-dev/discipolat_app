import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AnnouncementsAlertsSection from '@/components/departments/AnnouncementsAlertsSection';

const mockAnnouncements = [
  { id: 'a1', titre: 'Rappel répétition', message: 'Samedi 9h', cible: 'TOUS', auteurNom: 'Jean', createdAt: '2026-08-01T10:00:00' },
];
const mockAlerts: any[] = [];
const mockManagement = {
  teams: [], positions: [],
  org: {}, taskStats: {}, activity: [],
};
const mockMembers = { content: [{ id: 'm1', nomComplet: 'Jean Dupont' }, { id: 'm2', nomComplet: 'Marie Curie' }] };

vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/announcements')) return Promise.resolve({ data: mockAnnouncements });
      if (url.includes('/alerts/smart')) return Promise.resolve({ data: mockAlerts });
      if (url.includes('/management')) return Promise.resolve({ data: mockManagement });
      if (url.includes('/members')) return Promise.resolve({ data: mockMembers });
      return Promise.resolve({ data: {} });
    }),
    post: vi.fn().mockResolvedValue({ data: {} }),
    delete: vi.fn().mockResolvedValue({ data: {} }),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  };
  return {
    default: mockApiInstance,
    getErrorMessage: vi.fn().mockReturnValue('Erreur'),
  };
});

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

function renderSection() {
  return render(
    <QueryClientProvider client={queryClient}>
      <AnnouncementsAlertsSection deptId="dept-1" />
    </QueryClientProvider>
  );
}

describe('AnnouncementsAlertsSection — cible certains membres', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('publie une annonce ciblée sur des membres sélectionnés', async () => {
    const { default: api } = await import('@/lib/api');
    renderSection();

    fireEvent.change(await screen.findByPlaceholderText(/Réunion d'équipe samedi/), { target: { value: 'Rappel' } });
    fireEvent.change(screen.getByPlaceholderText(/Contenu de l'annonce/), { target: { value: 'Présence obligatoire' } });

    fireEvent.change(screen.getByLabelText('Cible'), { target: { value: 'MEMBRES' } });

    // La liste des membres se charge et permet la sélection
    expect(await screen.findByText('Jean Dupont')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Jean Dupont'));
    fireEvent.click(screen.getByText('Marie Curie'));

    fireEvent.click(screen.getByText('Publier'));

    await waitFor(() => expect(api.post).toHaveBeenCalled());
    const payload = (api.post as any).mock.calls[0][1];
    expect(payload.cible).toBe('MEMBRES');
    expect(payload.memberIds).toEqual(['m1', 'm2']);
  });

  it('désactive la publication tant que aucun membre n est sélectionné', async () => {
    renderSection();

    fireEvent.change(await screen.findByPlaceholderText(/Réunion d'équipe samedi/), { target: { value: 'Rappel' } });
    fireEvent.change(screen.getByPlaceholderText(/Contenu de l'annonce/), { target: { value: 'Présence' } });
    fireEvent.change(screen.getByLabelText('Cible'), { target: { value: 'MEMBRES' } });

    await screen.findByText('Jean Dupont');
    expect(screen.getByText('Publier').closest('button')).toBeDisabled();
  });
});
