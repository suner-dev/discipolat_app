import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import FinanceModulesPage from '@/pages/FinanceModulesPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

vi.mock('@/lib/api', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getErrorMessage: vi.fn((e: unknown) =>
    (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail || 'Erreur'),
}));

const svc = vi.hoisted(() => ({
  listAccounts: vi.fn(),
  listDonations: vi.fn(),
  listTontines: vi.fn(),
  listTontineMembers: vi.fn(),
  listTontinePayouts: vi.fn(),
  listBudgets: vi.fn(),
  unmatched: vi.fn(),
  ledger: vi.fn(),
  reportSummary: vi.fn(),
  reportByCategory: vi.fn(),
  reportCashFlow: vi.fn(),
  createAccount: vi.fn(),
  createDonation: vi.fn(),
  createTontine: vi.fn(),
  createTontineMember: vi.fn(),
  upsertBudget: vi.fn(),
  deleteBudget: vi.fn(),
  autoMatch: vi.fn(),
}));

vi.mock('@/services/financeService', () => svc);

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

const ACCOUNT = { id: 'a-1', name: 'Caisse centrale', accountNumber: '00042', bankName: 'BCI', balance: 150000, devise: 'XOF', isActive: true };
const DONATION = { id: 'd-1', donorName: 'Anonyme', amount: 5000, devise: 'XOF', donationDate: '2026-10-01T00:00:00Z', purpose: 'Construction', isAnonymous: true };
const TONTINE = { id: 'to-1', name: 'Tontine des femmes', description: null, amountPerTurn: 10000, frequency: 'MONTHLY', startDate: '2026-01-01T00:00:00Z', endDate: null, isActive: true };
const BUDGET = { id: 'b-1', categorie: 'LOYER', annee: 2026, montant: 600000, depenseReelle: 120000, consommationPct: 20, statut: 'EN_COURS' };

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <FinanceModulesPage />
    </QueryClientProvider>
  );
}

describe('FinanceModulesPage — câblée sur financeService (V236)', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    svc.listAccounts.mockResolvedValue([ACCOUNT]);
    svc.listDonations.mockResolvedValue([DONATION]);
    svc.listTontines.mockResolvedValue([TONTINE]);
    svc.listTontineMembers.mockResolvedValue([{ id: 'm-1', tontineId: 'to-1', userId: 'u-1', turnOrder: 1, isActive: true }]);
    svc.listTontinePayouts.mockResolvedValue([{ id: 'pa-1', tontineId: 'to-1', memberId: 'm-1', amount: 30000, turnNumber: 1, payoutDate: '2026-02-01T00:00:00Z' }]);
    svc.listBudgets.mockResolvedValue([BUDGET]);
    svc.unmatched.mockResolvedValue({ lines: [{ id: 'l-1', amount: 250 }] });
    svc.ledger.mockResolvedValue({ balance: 9000 });
    svc.reportSummary.mockResolvedValue({ recettes: 1000, depenses: 400, solde: 600 });
    svc.reportByCategory.mockResolvedValue([{ categorie: 'DIME', total: 1000 }]);
    svc.reportCashFlow.mockResolvedValue([{ mois: '2026-09', entrees: 1000, sorties: 400 }]);
    svc.createAccount.mockResolvedValue(ACCOUNT);
    svc.createDonation.mockResolvedValue(DONATION);
    svc.createTontine.mockResolvedValue(TONTINE);
    svc.createTontineMember.mockResolvedValue({ id: 'm-2' });
    svc.upsertBudget.mockResolvedValue(BUDGET);
    svc.autoMatch.mockResolvedValue({ matched: 2 });
  });

  it('comptes : listAccounts consommé et vues serveur rendues', async () => {
    renderPage();
    await waitFor(() => {
      expect(svc.listAccounts).toHaveBeenCalled();
      expect(screen.getByText('Caisse centrale')).toBeInTheDocument();
    });
    expect(screen.getByText(/BCI/)).toBeInTheDocument();
  });

  it('création compte : uniquement les clés lues par FinanceService.createAccount', async () => {
    const { container } = renderPage();
    await screen.findByText('Caisse centrale');
    fireEvent.click(screen.getByRole('button', { name: /nouveau/i }));
    const inputs = container.querySelectorAll('input');
    fireEvent.change(inputs[0], { target: { value: 'Compte missions' } });
    fireEvent.click(screen.getByRole('button', { name: /^créer$/i }));
    await waitFor(() => {
      expect(svc.createAccount).toHaveBeenCalledWith(expect.objectContaining({ name: 'Compte missions', devise: 'XOF' }));
    });
    const body = svc.createAccount.mock.calls[0][0];
    for (const key of Object.keys(body)) {
      expect(['name', 'accountNumber', 'bankName', 'balance', 'devise']).toContain(key);
    }
  });

  it('dons : liste + création avec amount requis', async () => {
    renderPage();
    await screen.findByText('Caisse centrale');
    fireEvent.click(screen.getByRole('button', { name: /^donations$/i }));
    await waitFor(() => {
      expect(svc.listDonations).toHaveBeenCalled();
      expect(screen.getAllByText(/Anonyme/).length).toBeGreaterThan(0);
    });
    fireEvent.click(screen.getByRole('button', { name: /nouveau/i }));
    const amountInput = document.querySelectorAll('.glass-card input')[1];
    fireEvent.change(amountInput, { target: { value: '2500' } });
    fireEvent.click(screen.getByRole('button', { name: /enregistrer/i }));
    await waitFor(() => {
      expect(svc.createDonation).toHaveBeenCalledWith(expect.objectContaining({ amount: 2500 }));
    });
  });

  it('tontines : détails membres/versements + ajout membre (userId, turnOrder)', async () => {
    renderPage();
    await screen.findByText('Caisse centrale');
    fireEvent.click(screen.getByRole('button', { name: /^tontines$/i }));
    await waitFor(() => {
      expect(svc.listTontines).toHaveBeenCalled();
      // Le nom apparaît aussi dans l'option du select d'ajout de membre → cibler le span de la carte.
      expect(screen.getByText('Tontine des femmes', { selector: 'span' })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /détails/i }));
    await waitFor(() => {
      expect(svc.listTontineMembers).toHaveBeenCalledWith('to-1');
      expect(svc.listTontinePayouts).toHaveBeenCalledWith('to-1');
    });
    const selects = document.querySelectorAll('select');
    const memberSelect = selects[selects.length - 1];
    fireEvent.change(memberSelect, { target: { value: 'to-1' } });
    const uuidInput = screen.getByPlaceholderText(/UUID utilisateur/i) as HTMLInputElement;
    fireEvent.change(uuidInput, { target: { value: 'u-99' } });
    fireEvent.click(screen.getByRole('button', { name: /ajouter/i }));
    await waitFor(() => {
      expect(svc.createTontineMember).toHaveBeenCalledWith('to-1', 'u-99', undefined);
    });
  });

  it('budgets : listBudgets(année) + suppression', async () => {
    renderPage();
    await screen.findByText('Caisse centrale');
    fireEvent.click(screen.getByRole('button', { name: /^budgets$/i }));
    await waitFor(() => {
      expect(svc.listBudgets).toHaveBeenCalledWith(new Date().getFullYear());
      expect(screen.getByText('LOYER')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /supprimer/i }));
    await waitFor(() => expect(svc.deleteBudget).toHaveBeenCalledWith('b-1'));
  });

  it('rapprochement : unmatched/ledger rendus en JSON générique + autoMatch', async () => {
    renderPage();
    await screen.findByText('Caisse centrale');
    fireEvent.click(screen.getByRole('button', { name: /^reconciliation$/i }));
    await waitFor(() => {
      expect(svc.unmatched).toHaveBeenCalled();
      expect(svc.ledger).toHaveBeenCalled();
      expect(screen.getByText(/"amount": 250/)).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /rapprochement automatique/i }));
    await waitFor(() => expect(svc.autoMatch).toHaveBeenCalled());
  });

  it('rapports : summary, par catégorie et flux de trésorerie', async () => {
    renderPage();
    await screen.findByText('Caisse centrale');
    fireEvent.click(screen.getByRole('button', { name: /^reports$/i }));
    await waitFor(() => {
      expect(svc.reportSummary).toHaveBeenCalled();
      expect(svc.reportByCategory).toHaveBeenCalled();
      expect(svc.reportCashFlow).toHaveBeenCalled();
      expect(screen.getByText(/categorie: DIME/)).toBeInTheDocument();
    });
  });
});
