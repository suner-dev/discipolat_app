import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import HealthPortalPage from '@/pages/HealthPortalPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getErrorMessage: vi.fn((e: unknown) =>
    (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail || 'Erreur'),
}));

const svc = vi.hoisted(() => ({
  getDashboardStats: vi.fn(),
  getHealthReportsStatistics: vi.fn(),
  getPatients: vi.fn(),
  getConsultations: vi.fn(),
  getPharmacyItems: vi.fn(),
  getPharmacyStock: vi.fn(),
  getLowStockAlerts: vi.fn(),
  getExpiringSoonAlerts: vi.fn(),
  getCampaigns: vi.fn(),
  updatePatient: vi.fn(),
  updateConsultation: vi.fn(),
  createPharmacyItem: vi.fn(),
  updatePharmacyStock: vi.fn(),
  createCampaign: vi.fn(),
  registerCampaignParticipant: vi.fn(),
}));

vi.mock('@/services/healthService', () => svc);

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

// Fixtures = vues aplaties V240 du serveur (HealthService.patientView etc.) :
// {personId, personName}, {patientId, patientName}, {itemId, itemName},
// {responsibleName, participantsCount} — plus d'objet embarqué, plus de tenantId.
const PATIENT = {
  id: 'p-uuid-1', personId: 'per-uuid-1', personName: 'Grace Kabila',
  groupeSanguin: 'O+', allergies: 'Arachide',
  medecinTraitant: 'Dr. Mbeki', poidsKg: 72.5, tailleCm: 175, numeroAssurance: 'NA-9',
};
const CONSULTATION = {
  id: 'c-uuid-1', patientId: 'per-uuid-1', patientName: 'Grace Kabila',
  practitionerId: 'pra-uuid-1', practitionerName: 'Jean Kalala',
  consultationDate: '2026-10-01', typeConsultation: 'SUIVI',
  motif: 'Contrôle', diagnostic: 'RAS', status: 'COMPLETED',
};
const STOCK = { id: 's-uuid-1', itemId: 'i-1', itemName: 'Paracétamol', lotNumber: 'LOT-42', quantite: 3, seuilAlerte: 10, dateExpiration: '2026-11-30', status: 'STOCK_FAIBLE' };
const CAMPAIGN = { id: 'ca-uuid-1', title: 'Campagne rougeole', campaignType: 'VACCINATION', startDate: '2026-11-01', responsibleName: 'Sarah Mbala', participantsCount: 4, status: 'PLANNED' };
const PAGED = <T,>(content: T[]) => ({ content, page: 0, size: 50, totalElements: content.length, totalPages: 1 });

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <HealthPortalPage />
    </QueryClientProvider>
  );
}

describe('HealthPortalPage — câblée sur healthService (V235)', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    svc.getDashboardStats.mockResolvedValue({ totalPatients: 12, totalConsultations: 30 });
    svc.getHealthReportsStatistics.mockResolvedValue({ medications: 5, kits: 2, duties: 3, patients: 12, consultations: 30, prescriptions: 8 });
    svc.getPatients.mockResolvedValue(PAGED([PATIENT]));
    svc.getConsultations.mockResolvedValue(PAGED([CONSULTATION]));
    svc.getPharmacyItems.mockResolvedValue(PAGED([{ id: 'i-1', nom: 'Paracétamol', categorie: 'MEDICAMENT', unite: 'boîte' }]));
    svc.getPharmacyStock.mockResolvedValue(PAGED([STOCK]));
    svc.getLowStockAlerts.mockResolvedValue([STOCK]);
    svc.getExpiringSoonAlerts.mockResolvedValue([STOCK]);
    svc.getCampaigns.mockResolvedValue(PAGED([CAMPAIGN]));
    svc.updatePatient.mockResolvedValue(PATIENT);
    svc.createPharmacyItem.mockResolvedValue({ id: 'i-2' });
    svc.registerCampaignParticipant.mockResolvedValue({ id: 'cp-1' });
  });

  it('tableau de bord : getDashboardStats + statistiques rapports', async () => {
    renderPage();
    await screen.findAllByText('12');
    expect(svc.getDashboardStats).toHaveBeenCalled();
    expect(svc.getHealthReportsStatistics).toHaveBeenCalled();
  });

  it('onglet patients : liste paginée (PageResponse) consommée, vue aplatie rendue', async () => {
    renderPage();
    await waitFor(() => expect(svc.getDashboardStats).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /^patients$/i }));
    await waitFor(() => {
      expect(svc.getPatients).toHaveBeenCalledWith(expect.objectContaining({ size: 50 }));
      expect(screen.getByText(/Dr\. Mbeki/)).toBeInTheDocument();
      // personName vient de la vue serveur (plus d'objet `person` embarqué).
      expect(screen.getByText('Grace Kabila')).toBeInTheDocument();
    });
  });

  it('onglet consultations : patientName et practitionerName aplaties rendus', async () => {
    renderPage();
    await waitFor(() => expect(svc.getDashboardStats).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /consultations/i }));
    await waitFor(() => {
      expect(svc.getConsultations).toHaveBeenCalled();
      expect(screen.getByText('Grace Kabila')).toBeInTheDocument();
      expect(screen.getByText(/Jean Kalala/)).toBeInTheDocument();
    });
  });

  it('mise à jour patient : uniquement les champs appliqués par HealthService.updatePatientRecord', async () => {
    renderPage();
    await waitFor(() => expect(svc.getDashboardStats).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /^patients$/i }));
    await screen.findByText(/Dr\. Mbeki/);
    fireEvent.click(screen.getByRole('button', { name: /modifier/i }));
    const inputs = document.querySelectorAll('.glass-card input');
    // inputs[0] = Allergies (formulaire d'édition affiché au-dessus de la liste).
    fireEvent.change(inputs[0], { target: { value: 'Pollen' } });
    fireEvent.click(screen.getByRole('button', { name: /enregistrer/i }));
    await waitFor(() => {
      expect(svc.updatePatient).toHaveBeenCalledWith('p-uuid-1', expect.objectContaining({ allergies: 'Pollen' }));
    });
    const body = svc.updatePatient.mock.calls[0][1];
    for (const key of Object.keys(body)) {
      expect(['allergies', 'antecedents', 'medecinTraitant', 'numeroAssurance', 'poidsKg', 'tailleCm']).toContain(key);
    }
  });

  it('onglet pharmacie : articles, stock, alertes basses et expirations', async () => {
    renderPage();
    await waitFor(() => expect(svc.getDashboardStats).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /^pharmacy$/i }));
    await waitFor(() => {
      expect(svc.getPharmacyItems).toHaveBeenCalled();
      expect(svc.getPharmacyStock).toHaveBeenCalled();
      expect(svc.getLowStockAlerts).toHaveBeenCalled();
      expect(svc.getExpiringSoonAlerts).toHaveBeenCalledWith(30);
      expect(screen.getByText('Paracétamol')).toBeInTheDocument();
    });
    expect(screen.getAllByText(/LOT-42/).length).toBeGreaterThan(0);
  });

  it('création article : clés scalaires de l\'entité PharmacyItem', async () => {
    renderPage();
    await waitFor(() => expect(svc.getDashboardStats).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /^pharmacy$/i }));
    await screen.findByText('Paracétamol');
    fireEvent.click(screen.getByRole('button', { name: /nouveau/i }));
    const inputs = document.querySelectorAll('.glass-card input');
    fireEvent.change(inputs[0], { target: { value: 'Ibuprofène' } });
    fireEvent.click(screen.getByRole('button', { name: /^créer$/i }));
    await waitFor(() => {
      expect(svc.createPharmacyItem).toHaveBeenCalledWith(expect.objectContaining({ nom: 'Ibuprofène', categorie: 'MEDICAMENT' }));
    });
    expect(toast.success).toHaveBeenCalled();
  });

  it('onglet campagnes : liste + inscription participant (userId UUID)', async () => {
    renderPage();
    await waitFor(() => expect(svc.getDashboardStats).toHaveBeenCalled());
    fireEvent.click(screen.getByRole('button', { name: /^campaigns$/i }));
    await waitFor(() => {
      expect(svc.getCampaigns).toHaveBeenCalled();
      expect(screen.getByText('Campagne rougeole')).toBeInTheDocument();
      // Vue aplatie V240 : responsable et participants viennent de la Map serveur.
      expect(screen.getByText(/Sarah Mbala/)).toBeInTheDocument();
      expect(screen.getByText(/4 inscrit/)).toBeInTheDocument();
    });
    const input = screen.getByPlaceholderText(/UUID utilisateur/i) as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'u-uuid-1' } });
    fireEvent.click(screen.getByRole('button', { name: /inscrire/i }));
    await waitFor(() => {
      expect(svc.registerCampaignParticipant).toHaveBeenCalledWith('ca-uuid-1', 'u-uuid-1');
    });
  });
});
