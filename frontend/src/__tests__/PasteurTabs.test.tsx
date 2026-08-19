import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// ─── Mocks partagés ───────────────────────────────────────────────

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 'pasteur-1', firstName: 'Jean', lastName: 'Pasteur', role: 'PASTEUR', roles: ['PASTEUR'], activeRole: 'PASTEUR' },
    isAuthenticated: true, isLoading: false,
  }),
}));

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div data-testid="chart">{children}</div>,
  BarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Bar: () => null,
  XAxis: () => null, YAxis: () => null, CartesianGrid: () => null,
  Tooltip: () => null, Cell: () => null,
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Pie: () => null,
  AreaChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Area: () => null,
}));

vi.mock('@/hooks/useDictionaries', () => ({
  useDictionaries: () => ({
    label: (_key: string, code: string) => {
      const map: Record<string, string> = {
        NOUVEL_ARRIVANT: 'Nouvel arrivant', NOUVEAU_CONVERTI: 'Nouveau converti',
        EN_INTEGRATION: 'En intégration', ACTIF: 'Actif', EN_VEILLE: 'En veille', DECROCHE: 'Décroché',
      };
      return map[code] || code;
    },
    options: () => [], selectOptions: () => [], color: () => undefined, isLoading: false,
  }),
}));

vi.mock('@/hooks/useExportReport', () => ({
  useExportReport: () => ({ exportReport: vi.fn(), isExporting: false }),
}));

// ─── Données mock ─────────────────────────────────────────────────

const mockDepartments = {
  content: [
    { id: 'd1', nom: 'Louange', description: 'Équipe de louange', responsableId: 'u1', responsableNom: 'Marie Louange', statut: 'ACTIF', totalAmes: 15, totalFamilles: 3, createdAt: '2024-01-01T00:00:00', updatedAt: '2024-07-01T00:00:00' },
    { id: 'd2', nom: 'Jeunesse', description: 'Jeunes de 15 à 25 ans', responsableId: 'u2', responsableNom: 'Paul Jeunesse', statut: 'ACTIF', totalAmes: 25, totalFamilles: 5, createdAt: '2024-02-01T00:00:00', updatedAt: '2024-07-01T00:00:00' },
  ],
  number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockUsers = {
  content: [
    { id: 'u1', firstName: 'Marie', lastName: 'Louange', role: 'PASTEUR', email: 'marie@test.com', phone: '+243123', statut: 'ACTIVE', twoFactorEnabled: false, createdAt: '2024-01-01', updatedAt: '2024-07-01' },
    { id: 'u2', firstName: 'Paul', lastName: 'Jeunesse', role: 'FAISEUR', email: 'paul@test.com', phone: '+243456', statut: 'ACTIVE', twoFactorEnabled: true, createdAt: '2024-02-01', updatedAt: '2024-07-01' },
    { id: 'u3', firstName: 'Luc', lastName: 'Membre', role: 'MEMBRE', email: 'luc@test.com', phone: '', statut: 'ACTIVE', twoFactorEnabled: false, createdAt: '2024-03-01', updatedAt: '2024-07-01' },
  ],
  number: 0, size: 20, totalElements: 3, totalPages: 1, first: true, last: true,
};

const mockFamilies = {
  content: [
    { id: 'f1', nom: 'Famille Mukendi', chefFamilleId: 'u1', chefFamilleNom: 'Marie Louange', statut: 'ACTIVE', niveauRisque: 'NORMAL', chefAdjointNom: 'Paul Jeunesse', createdAt: '2024-01-01' },
    { id: 'f2', nom: 'Famille Kabongo', chefFamilleId: 'u2', chefFamilleNom: 'Paul Jeunesse', statut: 'ACTIVE', niveauRisque: 'A_RISQUE', chefAdjointNom: '', createdAt: '2024-02-01' },
  ],
  number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockSouls = {
  content: [
    { id: 's1', nom: 'Dupont', prenom: 'Pierre', email: 'pierre@test.com', telephone: '+243111', typeDisciple: 'NOUVEL_ARRIVANT', faiseurId: 'u2', familleId: 'f1', statut: 'EN_INTEGRATION', notesPasteur: 'Bienvenu', dateIntegration: '2024-06-01', dateDernierContact: '2024-07-15' },
    { id: 's2', nom: 'Martin', prenom: 'Marie', email: 'marie@test.com', telephone: '', typeDisciple: 'NOUVEAU_CONVERTI', faiseurId: 'u1', familleId: 'f2', statut: 'ACTIF', dateIntegration: '2024-01-01' },
  ],
  number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockEvaluations = {
  content: [
    { id: 'e1', evalueId: 'u1', evalueNom: 'Marie Louange', evaluateurId: 'u2', evaluateurNom: 'Paul', categorie: 'MEMBRE', note: 4, commentaire: 'Très bien', anonyme: false, createdAt: '2024-07-01' },
    { id: 'e2', evalueId: 'u2', evalueNom: 'Paul Jeunesse', evaluateurId: 'u1', evaluateurNom: 'Marie', categorie: 'FAISEUR', note: 3, commentaire: 'Correct', anonyme: true, createdAt: '2024-06-15' },
  ],
  number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockAlerts = {
  content: [
    { id: 'a1', typeAlerte: 'ABSENCE', message: 'Absence depuis 2 semaines', statut: 'ACTIVE', priorite: 'HAUTE', cible: 'PERSONNE', dateDeclenchement: '2024-07-20T10:00:00', createdAt: '2024-07-20' },
    { id: 'a2', typeAlerte: 'RISQUE', message: 'Famille en risque', statut: 'RESOLUE', priorite: 'MOYENNE', cible: 'FAMILLE', dateDeclenchement: '2024-07-10T08:00:00', createdAt: '2024-07-10' },
  ],
  number: 0, size: 15, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockTransfers = {
  content: [
    { id: 't1', type: 'SOUL_TRANSFERT', statut: 'EN_ATTENTE_VALIDATION', personneNom: 'Pierre Dupont', cible: 'Louange', demandeurNom: 'Paul', dateSoumission: '2024-07-20', priorite: 'HAUTE', motif: 'Besoin de renfort', createdAt: '2024-07-20' },
    { id: 't2', type: 'FAISEUR_TRANSFERT', statut: 'EXECUTE', personneNom: 'Marie Martin', cible: 'Jeunesse', demandeurNom: 'Jean', dateSoumission: '2024-07-15', priorite: 'MOYENNE', motif: 'Réaffectation', createdAt: '2024-07-15' },
  ],
  number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockMakerReports = {
  content: [
    { id: 'r1', semaine: '2024-W29', faiseurId: 'u1', faiseurNom: 'Marie Louange', familleNom: 'Famille Mukendi', statut: 'SOUMIS', nbPresents: 8, nbCultes: 2, createdAt: '2024-07-22' },
    { id: 'r2', semaine: '2024-W28', faiseurId: 'u2', faiseurNom: 'Paul Jeunesse', familleNom: 'Famille Kabongo', statut: 'EN_ATTENTE', nbPresents: 5, nbCultes: 1, createdAt: '2024-07-15' },
  ],
  number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockFamilyReports = {
  content: [
    { id: 'fr1', semaine: '2024-W29', familleId: 'f1', familleNom: 'Famille Mukendi', chefFamilleNom: 'Marie Louange', statut: 'VALIDE', nombreAmes: '10', nombrePresents: '8', themesAbordes: 'Prière', envSpirituel: 'Bon', createdAt: '2024-07-22' },
  ],
  number: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true,
};

const mockVisits = {
  content: [
    { id: 'v1', ameId: 's1', ameNom: 'Pierre Dupont', visiteurId: 'u1', visiteurNom: 'Marie Louange', dateVisite: '2024-07-20', datePrevue: '2024-07-25', typeVisite: 'PASTORALE', statut: 'PLANIFIEE', lieu: 'Domicile', motif: 'Suivi', notes: 'Bien', createdAt: '2024-07-18' },
    { id: 'v2', ameId: 's2', ameNom: 'Marie Martin', visiteurId: 'u2', visiteurNom: 'Paul Jeunesse', dateVisite: '2024-07-10', typeVisite: 'DOMICILIAIRE', statut: 'REALISEE', lieu: 'Chez elle', motif: 'Visite amicale', compteRendu: 'Bon état', createdAt: '2024-07-08' },
  ],
  number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockPrayers = {
  content: [
    { id: 'p1', titre: 'Prière pour la santé', description: 'Guérison de maman', auteurId: 'u1', auteurNom: 'Marie Louange', statut: 'ACTIVE', priorite: 'HAUTE', visibilite: 'EGLISE', categorie: 'SANTE', nbPrieres: 5, createdAt: '2024-07-20' },
    { id: 'p2', titre: 'Action de grâce', description: 'Merci Seigneur', auteurId: 'u2', auteurNom: 'Paul Jeunesse', statut: 'EXAUCEE', priorite: 'MOYENNE', visibilite: 'PRIVE', nbPrieres: 10, temoignage: 'Dieu a répondu', createdAt: '2024-06-15' },
  ],
  number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockEvents = {
  content: [
    { id: 'ev1', titre: 'Culte du dimanche', description: 'Culte principal', typeEvenement: 'CULTE', dateDebut: '2024-08-04T09:00:00', lieu: 'Église', nbInscrits: 50, organisateurNom: 'Marie', createdAt: '2024-07-01' },
    { id: 'ev2', titre: 'Réunion de prière', description: 'Prières intercession', typeEvenement: 'PRIERE', dateDebut: '2024-07-25T18:00:00', lieu: 'Salle B', nbInscrits: 15, organisateurNom: 'Paul', createdAt: '2024-07-15' },
  ],
  number: 0, size: 20, totalElements: 2, totalPages: 1, first: true, last: true,
};

const mockDeptDetail = {
  members: [
    { id: 'm1', nom: 'Dupont', prenom: 'Pierre', soulId: 's1' },
    { id: 'm2', nom: 'Martin', prenom: 'Marie', soulId: 's2' },
  ],
};

const mockKpis = {
  resume: { totalAmes: 42, actifs: 35, totalFaiseurs: 8, alertesActives: 3 },
  health: { score: 78, tauxPresence: 82, tauxRapports: 70, tauxFidelisation: 90, tauxCroissance: 5, nouveauxMois: 4, croissanceNette: 3 },
  workload: [
    { id: 'u1', nom: 'Marie Louange', totalAmes: 8, charge: 60, rapportSoumis: true },
    { id: 'u2', nom: 'Paul Jeunesse', totalAmes: 12, charge: 85, rapportSoumis: false },
  ],
  overdueReports: [{ faiseurId: 'u2', faiseurNom: 'Paul Jeunesse', nbAmes: 12 }],
  upcomingEvents: [{ id: 'ev1', titre: 'Culte', dateDebut: '2024-08-04T09:00:00', lieu: 'Église', inscrits: 50 }],
  overdueReportsCount: 1,
};

const mockDashboard = {
  croissance: { totalAmes: 42, nouveauxConvertis: 3, nouveauxArrivants: 5, actifs: 35, enIntegration: 8, enVeille: 2, decroches: 1, tauxConversion: 12 },
  departements: [{ id: 'd1', nom: 'Louange', responsableNom: 'Marie', totalFamilles: 3, totalAmes: 15 }],
  familles: [{ id: 'f1', nom: 'Mukendi', chefNom: 'Marie', totalAmes: 10, actifs: 8, tauxPresence: 85, aRisque: false }],
  faiseurs: [{ id: 'u1', nom: 'Marie Louange', totalAmes: 8, actifs: 6, estChef: true }],
  presences: { tauxGlobal: 82, tauxNouveauxArrivants: 65, tauxNouveauxConvertis: 90 },
  rapports: { soumis: 5, enAttente: 3, tauxCompletion: 63, totalFaiseurs: 8, faiseursAyantRapporte: 5 },
  transfertsEnAttente: [],
  alertesActives: 3,
  suivisParallelesActifs: 2,
};

// ─── Mock API ─────────────────────────────────────────────────────

vi.mock('@/lib/api', () => {
  const mockApi = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/departments?')) return Promise.resolve({ data: mockDepartments });
      if (url.includes('/departments/') && url.includes('/detail')) return Promise.resolve({ data: mockDeptDetail });
      if (url.includes('/departments/') && url.includes('/kpi')) return Promise.resolve({ data: { totalMembers: 17, totalTeams: 2, totalTasks: 5 } });
      if (url.includes('/departments/') && url.includes('/teams')) return Promise.resolve({ data: [{ id: 't1', nom: 'Équipe A' }] });
      if (url.includes('/departments/') && url.includes('/tasks')) return Promise.resolve({ data: [{ id: 't1', titre: 'Tâche 1', statut: 'EN_COURS' }] });
      if (url.includes('/users?')) return Promise.resolve({ data: mockUsers });
      if (url.includes('/families?')) return Promise.resolve({ data: mockFamilies });
      if (url.includes('/souls?')) return Promise.resolve({ data: mockSouls });
      if (url.includes('/evaluations?')) return Promise.resolve({ data: mockEvaluations });
      if (url.includes('/alerts/stats')) return Promise.resolve({ data: { actives: 3, traitees: 2, resolues: 5, total: 10 } });
      if (url.includes('/alerts?')) return Promise.resolve({ data: mockAlerts });
      if (url.includes('/transfers?')) return Promise.resolve({ data: mockTransfers });
      if (url.includes('/reports/maker-weekly')) return Promise.resolve({ data: mockMakerReports });
      if (url.includes('/reports/family-weekly')) return Promise.resolve({ data: mockFamilyReports });
      if (url.includes('/visits/upcoming')) return Promise.resolve({ data: [] });
      if (url.includes('/visits?')) return Promise.resolve({ data: mockVisits });
      if (url.includes('/prayers/actions-de-grace')) return Promise.resolve({ data: [mockPrayers.content[1]] });
      if (url.includes('/prayers?')) return Promise.resolve({ data: mockPrayers });
      if (url.includes('/events/consolidated')) return Promise.resolve({ data: [] });
      if (url.includes('/events/statistics')) return Promise.resolve({ data: { totalEvents: 10, upcomingEvents: 4, completedEvents: 6, totalRegistrations: 120 } });
      if (url.includes('/events?')) return Promise.resolve({ data: mockEvents });
      if (url.includes('/dashboard/pasteur/kpis')) return Promise.resolve({ data: mockKpis });
      if (url.includes('/dashboard/pasteur/presence-trend')) return Promise.resolve({ data: { tendance: [], tendanceGlobale: 2.5 } });
      if (url.includes('/dashboard/pasteur')) return Promise.resolve({ data: mockDashboard });
      if (url.includes('/audit')) return Promise.resolve({ data: { content: [], number: 0, size: 30, totalElements: 0, totalPages: 0, first: true, last: true } });
      if (url.includes('/dictionaries')) return Promise.resolve({ data: {} });
      return Promise.resolve({ data: {} });
    }),
    post: vi.fn().mockResolvedValue({ data: {} }),
    put: vi.fn().mockResolvedValue({ data: {} }),
    patch: vi.fn().mockResolvedValue({ data: {} }),
    delete: vi.fn().mockResolvedValue({ data: {} }),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  };
  return { default: mockApi, getErrorMessage: vi.fn().mockReturnValue('Erreur') };
});

// ─── Helpers ──────────────────────────────────────────────────────

function createQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: 0 }, mutations: { retry: false } } });
}

function renderWithProviders(ui: React.ReactElement, qc?: QueryClient) {
  const client = qc || createQueryClient();
  return render(
    <QueryClientProvider client={client}>
      <BrowserRouter>{ui}</BrowserRouter>
    </QueryClientProvider>
  );
}

// ═══════════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════════

describe('PasteurDepartmentsTab', () => {
  let qc: QueryClient;
  beforeEach(() => { vi.clearAllMocks(); qc = createQueryClient(); });

  it('renders the title "Départements"', async () => {
    const { default: PasteurDepartmentsTab } = await import('@/components/pasteur/PasteurDepartmentsTab');
    renderWithProviders(<PasteurDepartmentsTab />, qc);
    expect(await screen.findByText('Départements')).toBeInTheDocument();
  });

  it('displays department names in the table', async () => {
    const { default: PasteurDepartmentsTab } = await import('@/components/pasteur/PasteurDepartmentsTab');
    renderWithProviders(<PasteurDepartmentsTab />, qc);
    expect(await screen.findByText('Louange')).toBeInTheDocument();
    expect(screen.getByText('Jeunesse')).toBeInTheDocument();
  });

  it('shows responsable names', async () => {
    const { default: PasteurDepartmentsTab } = await import('@/components/pasteur/PasteurDepartmentsTab');
    renderWithProviders(<PasteurDepartmentsTab />, qc);
    await screen.findByText('Louange');
    expect(screen.getByText('Marie Louange')).toBeInTheDocument();
  });

  it('displays the search input', async () => {
    const { default: PasteurDepartmentsTab } = await import('@/components/pasteur/PasteurDepartmentsTab');
    renderWithProviders(<PasteurDepartmentsTab />, qc);
    expect(await screen.findByPlaceholderText('Rechercher un département...')).toBeInTheDocument();
  });

  it('shows "Nouveau département" button', async () => {
    const { default: PasteurDepartmentsTab } = await import('@/components/pasteur/PasteurDepartmentsTab');
    renderWithProviders(<PasteurDepartmentsTab />, qc);
    expect(await screen.findByText('Nouveau département')).toBeInTheDocument();
  });

  it('shows total âmes count', async () => {
    const { default: PasteurDepartmentsTab } = await import('@/components/pasteur/PasteurDepartmentsTab');
    renderWithProviders(<PasteurDepartmentsTab />, qc);
    await screen.findByText('Louange');
    expect(screen.getByText('15')).toBeInTheDocument();
  });
});

describe('PasteurReportsTab', () => {
  let qc: QueryClient;
  beforeEach(() => { vi.clearAllMocks(); qc = createQueryClient(); });

  it('renders the title "Rapports"', async () => {
    const { default: PasteurReportsTab } = await import('@/components/pasteur/PasteurReportsTab');
    renderWithProviders(<PasteurReportsTab />, qc);
    expect(await screen.findByText('Rapports')).toBeInTheDocument();
  });

  it('shows sub-tabs (Résumé, Faiseur, Famille)', async () => {
    const { default: PasteurReportsTab } = await import('@/components/pasteur/PasteurReportsTab');
    renderWithProviders(<PasteurReportsTab />, qc);
    expect(await screen.findByText('Résumé')).toBeInTheDocument();
    expect(screen.getByText('Rapports Faiseur')).toBeInTheDocument();
    expect(screen.getByText('Rapports Famille')).toBeInTheDocument();
  });

  it('shows completion percentage in progress bar', async () => {
    const { default: PasteurReportsTab } = await import('@/components/pasteur/PasteurReportsTab');
    renderWithProviders(<PasteurReportsTab />, qc);
    await screen.findByText('Rapports');
    await waitFor(() => {
      expect(screen.getByText('Taux de complétion')).toBeInTheDocument();
    });
    // The progress bar text uses a span with the percentage
    const spans = screen.getAllByText(/\d+%/);
    expect(spans.length).toBeGreaterThan(0);
  });

  it('switches to Faiseur tab and shows reports', async () => {
    const { default: PasteurReportsTab } = await import('@/components/pasteur/PasteurReportsTab');
    renderWithProviders(<PasteurReportsTab />, qc);
    await screen.findByText('Résumé');
    fireEvent.click(screen.getByText('Rapports Faiseur'));
    await waitFor(() => {
      expect(screen.getByText(/Marie Louange/)).toBeInTheDocument();
    });
  });

  it('switches to Famille tab', async () => {
    const { default: PasteurReportsTab } = await import('@/components/pasteur/PasteurReportsTab');
    renderWithProviders(<PasteurReportsTab />, qc);
    await screen.findByText('Résumé');
    fireEvent.click(screen.getByText('Rapports Famille'));
    await waitFor(() => {
      expect(screen.getByText(/Famille/)).toBeInTheDocument();
    });
  });
});

describe('PasteurEvaluationsTab', () => {
  let qc: QueryClient;
  beforeEach(() => { vi.clearAllMocks(); qc = createQueryClient(); });

  it('renders the title "Évaluations"', async () => {
    const { default: PasteurEvaluationsTab } = await import('@/components/pasteur/PasteurEvaluationsTab');
    renderWithProviders(<PasteurEvaluationsTab />, qc);
    expect(await screen.findByText('Évaluations')).toBeInTheDocument();
  });

  it('shows "Nouvelle évaluation" button', async () => {
    const { default: PasteurEvaluationsTab } = await import('@/components/pasteur/PasteurEvaluationsTab');
    renderWithProviders(<PasteurEvaluationsTab />, qc);
    expect(await screen.findByText('Nouvelle évaluation')).toBeInTheDocument();
  });

  it('displays average note', async () => {
    const { default: PasteurEvaluationsTab } = await import('@/components/pasteur/PasteurEvaluationsTab');
    renderWithProviders(<PasteurEvaluationsTab />, qc);
    await screen.findByText('Note moyenne');
    expect(screen.getByText('3.5')).toBeInTheDocument();
  });

  it('opens create modal on button click', async () => {
    const { default: PasteurEvaluationsTab } = await import('@/components/pasteur/PasteurEvaluationsTab');
    renderWithProviders(<PasteurEvaluationsTab />, qc);
    fireEvent.click(await screen.findByRole('button', { name: /Nouvelle évaluation/ }));
    await waitFor(() => {
      expect(screen.getByText('Catégorie *')).toBeInTheDocument();
    });
  });

  it('has a filter button', async () => {
    const { default: PasteurEvaluationsTab } = await import('@/components/pasteur/PasteurEvaluationsTab');
    renderWithProviders(<PasteurEvaluationsTab />, qc);
    // The filter button exists
    const filterBtns = screen.getAllByRole('button', { name: /Filtres/ });
    expect(filterBtns.length).toBeGreaterThanOrEqual(1);
  });
});

describe('PasteurUsersTab', () => {
  let qc: QueryClient;
  beforeEach(() => { vi.clearAllMocks(); qc = createQueryClient(); });

  it('renders the title "Utilisateurs"', async () => {
    const { default: PasteurUsersTab } = await import('@/components/pasteur/PasteurUsersTab');
    renderWithProviders(<PasteurUsersTab />, qc);
    expect(await screen.findByText('Utilisateurs')).toBeInTheDocument();
  });

  it('displays user names', async () => {
    const { default: PasteurUsersTab } = await import('@/components/pasteur/PasteurUsersTab');
    renderWithProviders(<PasteurUsersTab />, qc);
    await screen.findByText('Marie Louange');
    expect(screen.getByText('Paul Jeunesse')).toBeInTheDocument();
  });

  it('shows role badges', async () => {
    const { default: PasteurUsersTab } = await import('@/components/pasteur/PasteurUsersTab');
    renderWithProviders(<PasteurUsersTab />, qc);
    await screen.findByText('Marie Louange');
    expect(screen.getAllByText('Pasteur').length).toBeGreaterThanOrEqual(1);
  });

  it('shows search input', async () => {
    const { default: PasteurUsersTab } = await import('@/components/pasteur/PasteurUsersTab');
    renderWithProviders(<PasteurUsersTab />, qc);
    expect(await screen.findByPlaceholderText('Rechercher par nom, email...')).toBeInTheDocument();
  });

  it('opens detail view on click', async () => {
    const { default: PasteurUsersTab } = await import('@/components/pasteur/PasteurUsersTab');
    renderWithProviders(<PasteurUsersTab />, qc);
    fireEvent.click(await screen.findByText('Marie Louange'));
    await waitFor(() => {
      expect(screen.getByText('Retour à la liste')).toBeInTheDocument();
    });
  });
});

describe('PasteurFamiliesTab', () => {
  let qc: QueryClient;
  beforeEach(() => { vi.clearAllMocks(); qc = createQueryClient(); });

  it('renders the title "Familles"', async () => {
    const { default: PasteurFamiliesTab } = await import('@/components/pasteur/PasteurFamiliesTab');
    renderWithProviders(<PasteurFamiliesTab />, qc);
    expect(await screen.findByText('Familles')).toBeInTheDocument();
  });

  it('displays family names', async () => {
    const { default: PasteurFamiliesTab } = await import('@/components/pasteur/PasteurFamiliesTab');
    renderWithProviders(<PasteurFamiliesTab />, qc);
    expect(await screen.findByText('Famille Mukendi')).toBeInTheDocument();
    expect(screen.getByText('Famille Kabongo')).toBeInTheDocument();
  });

  it('shows risk badges', async () => {
    const { default: PasteurFamiliesTab } = await import('@/components/pasteur/PasteurFamiliesTab');
    renderWithProviders(<PasteurFamiliesTab />, qc);
    await screen.findByText('Famille Mukendi');
    expect(screen.getByText('Normal')).toBeInTheDocument();
    expect(screen.getByText('À risque')).toBeInTheDocument();
  });

  it('shows "Nouvelle famille" button', async () => {
    const { default: PasteurFamiliesTab } = await import('@/components/pasteur/PasteurFamiliesTab');
    renderWithProviders(<PasteurFamiliesTab />, qc);
    expect(await screen.findByText('Nouvelle famille')).toBeInTheDocument();
  });

  it('opens detail view on family click', async () => {
    const { default: PasteurFamiliesTab } = await import('@/components/pasteur/PasteurFamiliesTab');
    renderWithProviders(<PasteurFamiliesTab />, qc);
    fireEvent.click(await screen.findByText('Famille Mukendi'));
    await waitFor(() => {
      expect(screen.getByText('Retour à la liste')).toBeInTheDocument();
      expect(screen.getByText('Famille Mukendi')).toBeInTheDocument();
    });
  });
});

describe('PasteurCrmTab', () => {
  let qc: QueryClient;
  beforeEach(() => { vi.clearAllMocks(); qc = createQueryClient(); });

  it('renders the title "CRM Faiseur"', async () => {
    const { default: PasteurCrmTab } = await import('@/components/pasteur/PasteurCrmTab');
    renderWithProviders(<PasteurCrmTab />, qc);
    expect(await screen.findByText('CRM Faiseur')).toBeInTheDocument();
  });

  it('shows KPI cards', async () => {
    const { default: PasteurCrmTab } = await import('@/components/pasteur/PasteurCrmTab');
    renderWithProviders(<PasteurCrmTab />, qc);
    await screen.findByText('Total âmes');
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('35')).toBeInTheDocument(); // actifs
  });

  it('shows faiseur workload list', async () => {
    const { default: PasteurCrmTab } = await import('@/components/pasteur/PasteurCrmTab');
    renderWithProviders(<PasteurCrmTab />, qc);
    await waitFor(() => {
      expect(screen.getByText(/Charge par faiseur/)).toBeInTheDocument();
    });
    expect(screen.getAllByText('Marie Louange').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Paul Jeunesse').length).toBeGreaterThanOrEqual(1);
  });

  it('shows overdue reports section', async () => {
    const { default: PasteurCrmTab } = await import('@/components/pasteur/PasteurCrmTab');
    renderWithProviders(<PasteurCrmTab />, qc);
    await screen.findByText('Rapports en retard');
  });

  it('shows quick links to modules', async () => {
    const { default: PasteurCrmTab } = await import('@/components/pasteur/PasteurCrmTab');
    renderWithProviders(<PasteurCrmTab />, qc);
    await screen.findByText('Total âmes');
    expect(screen.getByText('Rapports')).toBeInTheDocument();
    expect(screen.getByText('Évaluations')).toBeInTheDocument();
    expect(screen.getByText('Familles')).toBeInTheDocument();
  });
});

describe('PasteurSoulsTab', () => {
  let qc: QueryClient;
  beforeEach(() => { vi.clearAllMocks(); qc = createQueryClient(); });

  it('renders the title "Âmes"', async () => {
    const { default: PasteurSoulsTab } = await import('@/components/pasteur/PasteurSoulsTab');
    renderWithProviders(<PasteurSoulsTab />, qc);
    expect(await screen.findByText('Âmes')).toBeInTheDocument();
  });

  it('displays soul names', async () => {
    const { default: PasteurSoulsTab } = await import('@/components/pasteur/PasteurSoulsTab');
    renderWithProviders(<PasteurSoulsTab />, qc);
    expect(await screen.findByText(/Pierre Dupont/)).toBeInTheDocument();
    expect(screen.getByText(/Marie Martin/)).toBeInTheDocument();
  });

  it('shows "Nouvelle âme" button', async () => {
    const { default: PasteurSoulsTab } = await import('@/components/pasteur/PasteurSoulsTab');
    renderWithProviders(<PasteurSoulsTab />, qc);
    expect(await screen.findByText('Nouvelle âme')).toBeInTheDocument();
  });

  it('shows Corbeille button', async () => {
    const { default: PasteurSoulsTab } = await import('@/components/pasteur/PasteurSoulsTab');
    renderWithProviders(<PasteurSoulsTab />, qc);
    expect(await screen.findByText('Corbeille')).toBeInTheDocument();
  });

  it('shows type badges', async () => {
    const { default: PasteurSoulsTab } = await import('@/components/pasteur/PasteurSoulsTab');
    renderWithProviders(<PasteurSoulsTab />, qc);
    await screen.findByText(/Pierre Dupont/);
    expect(screen.getByText('Nouvel arrivant')).toBeInTheDocument();
    expect(screen.getByText('Nouveau converti')).toBeInTheDocument();
  });

  it('opens detail view on soul click', async () => {
    const { default: PasteurSoulsTab } = await import('@/components/pasteur/PasteurSoulsTab');
    renderWithProviders(<PasteurSoulsTab />, qc);
    fireEvent.click(await screen.findByText(/Pierre Dupont/));
    await waitFor(() => {
      expect(screen.getByText('Retour à la liste')).toBeInTheDocument();
    });
  });

  it('switches to corbeille view', async () => {
    const { default: PasteurSoulsTab } = await import('@/components/pasteur/PasteurSoulsTab');
    renderWithProviders(<PasteurSoulsTab />, qc);
    fireEvent.click(await screen.findByText('Corbeille'));
    await waitFor(() => {
      expect(screen.getByText('Corbeille vide')).toBeInTheDocument();
    });
  });
});
