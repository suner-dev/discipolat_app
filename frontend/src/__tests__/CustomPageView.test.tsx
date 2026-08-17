import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import CustomPageView from '@/pages/CustomPageView';

const { apiGet, apiPost } = vi.hoisted(() => ({ apiGet: vi.fn(), apiPost: vi.fn() }));

// Recharts : conteneurs simples pour le rendu jsdom (pattern des autres tests).
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  LineChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Line: () => null,
  BarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Bar: () => null,
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Pie: () => null,
  Cell: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  Legend: () => null,
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    put: vi.fn(),
    post: apiPost,
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
}));

const RESOLVED_PAGE = {
  page: {
    id: 'page-1',
    key: 'APERCU',
    title: 'Vue d’ensemble de l’église',
    description: 'Synthèse en temps réel',
    slug: 'apercu-eglise',
    layout: 'GRID_2',
    blocks: [],
    roles: [],
    enabled: true,
    published: true,
    version: 2,
    createdAt: '2026-08-17T10:00:00',
    updatedAt: '2026-08-17T10:00:00',
  },
  blocks: [
    { type: 'KPI', config: { label: 'Âmes suivies', source: 'SOULS_TOTAL', icon: 'Heart', color: 'primary' }, data: { value: 42 } },
    { type: 'KPI', config: { label: 'Alertes ouvertes', source: 'ALERTS_OPEN', icon: 'Bell', color: 'rose' }, data: { value: 3 } },
    {
      type: 'TABLEAU',
      config: { title: 'Événements à venir', source: 'UPCOMING_EVENTS' },
      data: {
        headers: ['Événement', 'Date', 'Lieu'],
        rows: [['Culte du dimanche', '17/08/2026 09:00', 'Temple'], ['Veillée', '22/08/2026 20:00', '—']],
      },
    },
    { type: 'TEXTE', config: { content: 'Bienvenue dans notre église !' }, data: null },
  ],
};

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderView() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/pages/apercu-eglise']}>
        <Routes>
          <Route path="/pages/:slug" element={<CustomPageView />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('CustomPageView — rendu public des pages personnalisées', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
  });

  it('affiche la page, les KPI résolus et le tableau de données réelles', async () => {
    apiGet.mockResolvedValue({ data: RESOLVED_PAGE });
    renderView();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble de l’église')).toBeInTheDocument();
    });
    // KPI : libellé + valeur réelle (format fr).
    expect(screen.getByText('Âmes suivies')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('Alertes ouvertes')).toBeInTheDocument();
    // Tableau : en-têtes + lignes.
    expect(screen.getByText('Événement')).toBeInTheDocument();
    expect(screen.getByText('Culte du dimanche')).toBeInTheDocument();
    expect(screen.getByText('17/08/2026 09:00')).toBeInTheDocument();
    // Bloc texte.
    expect(screen.getByText('Bienvenue dans notre église !')).toBeInTheDocument();
    // Version affichée.
    expect(screen.getByText('v2')).toBeInTheDocument();
  });

  it('affiche « — » quand un KPI n’a pas de valeur résolue', async () => {
    const page = JSON.parse(JSON.stringify(RESOLVED_PAGE));
    page.blocks[0].data = null;
    apiGet.mockResolvedValue({ data: page });
    renderView();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble de l’église')).toBeInTheDocument();
    });
    // Valeur non résolue → « — » dans la carte KPI (et sa ligne de tableau).
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });

  it('affiche une page vide quand il n’y a aucun bloc', async () => {
    const page = JSON.parse(JSON.stringify(RESOLVED_PAGE));
    page.blocks = [];
    apiGet.mockResolvedValue({ data: page });
    renderView();

    await waitFor(() => {
      expect(screen.getByText('Cette page ne contient aucun bloc.')).toBeInTheDocument();
    });
  });

  it('affiche un message d’accès refusé sur 403', async () => {
    apiGet.mockRejectedValue({ response: { status: 403 } });
    renderView();

    await waitFor(() => {
      expect(screen.getByText(/pas accessible avec votre rôle actuel/i)).toBeInTheDocument();
    });
  });

  it('affiche « page introuvable » quand la page n’existe pas ou n’est pas publiée', async () => {
    apiGet.mockRejectedValue({ response: { status: 404 } });
    renderView();

    await waitFor(() => {
      expect(screen.getByText(/n’existe pas ou n’est pas publiée/i)).toBeInTheDocument();
    });
  });

  it('rend les blocs GRAPHIQUE, CALENDRIER, TIMELINE et CHECKLIST', async () => {
    // Date d'événement toujours dans le mois courant (aujourd'hui + 3 jours).
    const soon = new Date(Date.now() + 3 * 86400000);
    const iso = `${soon.getFullYear()}-${String(soon.getMonth() + 1).padStart(2, '0')}-${String(soon.getDate()).padStart(2, '0')}`;
    const page = JSON.parse(JSON.stringify(RESOLVED_PAGE));
    page.blocks = [
      {
        type: 'GRAPHIQUE',
        config: { title: 'Âmes par statut', source: 'SOULS_BY_STATUT', chartType: 'PIE' },
        data: { data: [{ name: 'Actif', value: 42 }, { name: 'En veille', value: 3 }] },
      },
      {
        type: 'CALENDRIER',
        config: { title: 'Agenda', source: 'CALENDAR_EVENTS' },
        data: { events: [{ date: iso, title: 'Culte du dimanche', lieu: 'Temple' }] },
      },
      {
        type: 'TIMELINE',
        config: { title: 'Nouvelles âmes', source: 'SOULS_TIMELINE' },
        data: { items: [{ date: '17/08/2026', label: 'Aya Kouassi', value: 'Actif' }] },
      },
      {
        type: 'CHECKLIST',
        config: { title: 'Suivi des nouveaux', items: ['Appeler', 'Inviter'] },
        data: null,
      },
    ];
    apiGet.mockResolvedValue({ data: page });
    renderView();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble de l’église')).toBeInTheDocument();
    });
    // Graphique : titre rendu.
    expect(screen.getByText('Âmes par statut')).toBeInTheDocument();
    // Calendrier : titre + mois courant + événement du mois.
    expect(screen.getByText('Agenda')).toBeInTheDocument();
    expect(screen.getByText('Culte du dimanche')).toBeInTheDocument();
    expect(screen.getByText('Temple')).toBeInTheDocument();
    // Timeline : titre + entrée.
    expect(screen.getByText('Nouvelles âmes')).toBeInTheDocument();
    expect(screen.getByText('Aya Kouassi')).toBeInTheDocument();
    // Checklist : titre + éléments + progression.
    expect(screen.getByText('Suivi des nouveaux')).toBeInTheDocument();
    expect(screen.getByText('Appeler')).toBeInTheDocument();
    expect(screen.getByText('Inviter')).toBeInTheDocument();
    expect(screen.getByText('0/2 · 0 %')).toBeInTheDocument();

    // Interagir : cocher le premier élément → 1/2 · 50 %.
    fireEvent.click(screen.getAllByRole('checkbox')[0]);
    expect(screen.getByText('1/2 · 50 %')).toBeInTheDocument();
  });

  it('rend les blocs FICHIERS et TACHES sur des données réelles', async () => {
    const page = JSON.parse(JSON.stringify(RESOLVED_PAGE));
    page.blocks = [
      {
        type: 'FICHIERS',
        config: { title: 'Documents', source: 'RECENT_FILES' },
        data: { items: [{ nom: 'Programme du culte.pdf', categorie: 'Compte Rendu', taille: 2048, date: '17/08/2026' }] },
      },
      {
        type: 'TACHES',
        config: { title: 'Tâches ouvertes', source: 'TACHES_EN_COURS' },
        data: { items: [{ titre: 'Préparer la répétition', departement: 'Jeunesse', echeance: '2026-08-20', priorite: 'HAUTE' }] },
      },
    ];
    apiGet.mockResolvedValue({ data: page });
    renderView();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble de l’église')).toBeInTheDocument();
    });
    // Fichiers : titre + nom + catégorie + taille formatée.
    expect(screen.getByText('Documents')).toBeInTheDocument();
    expect(screen.getByText('Programme du culte.pdf')).toBeInTheDocument();
    expect(screen.getByText(/Compte Rendu · 17\/08\/2026 · 2 Ko/)).toBeInTheDocument();
    // Tâches : titre + tâche + département + badge de priorité.
    expect(screen.getByText('Tâches ouvertes')).toBeInTheDocument();
    expect(screen.getByText('Préparer la répétition')).toBeInTheDocument();
    expect(screen.getByText('Jeunesse')).toBeInTheDocument();
    expect(screen.getByText('Haute')).toBeInTheDocument();
  });

  it('soumet un bloc FORMULAIRE et affiche la confirmation', async () => {
    const page = JSON.parse(JSON.stringify(RESOLVED_PAGE));
    page.blocks = [
      {
        type: 'FORMULAIRE',
        config: { title: 'Suggestion', type: 'SUGGESTION', cible: 'PASTEUR', successMessage: 'Merci ! Votre message a bien été transmis.' },
        data: null,
      },
    ];
    apiGet.mockResolvedValue({ data: page });
    apiPost.mockResolvedValue({ data: {} });
    renderView();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble de l’église')).toBeInTheDocument();
    });
    expect(screen.getByText('Suggestion')).toBeInTheDocument();
    // Le bouton est désactivé tant que le message est vide.
    const button = screen.getByRole('button', { name: /envoyer/i });
    expect(button).toBeDisabled();

    fireEvent.change(screen.getByLabelText('Votre message'), { target: { value: 'J’aimerais proposer une nouvelle activité.' } });
    fireEvent.click(button);

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/members/me/requests', {
        type: 'SUGGESTION',
        cible: 'PASTEUR',
        message: 'J’aimerais proposer une nouvelle activité.',
      });
    });
    expect(screen.getByText('Merci ! Votre message a bien été transmis.')).toBeInTheDocument();
  });
});
