import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

import api from '@/lib/api';
import * as svc from './financeService';

const mock = api as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  put: ReturnType<typeof vi.fn>;
  patch: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
};

describe('financeService — contrat exact FinanceController', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mock.get.mockResolvedValue({ data: [] });
    mock.post.mockResolvedValue({ data: {} });
    mock.put.mockResolvedValue({ data: {} });
    mock.delete.mockResolvedValue({ data: {} });
  });

  it('transactions: list, create, update, delete, get, unreconciled, reconcile', async () => {
    await svc.listTransactions({ type: 'RECETTE', categorie: 'DIME' });
    expect(mock.get).toHaveBeenCalledWith('/finances/transactions', {
      params: { type: 'RECETTE', categorie: 'DIME' },
    });
    await svc.createTransaction({ type: 'RECETTE', montant: 100 });
    expect(mock.post).toHaveBeenCalledWith('/finances/transactions', { type: 'RECETTE', montant: 100 });
    await svc.updateTransaction('t-1', { type: 'DEPENSE', montant: 50 });
    expect(mock.put).toHaveBeenCalledWith('/finances/transactions/t-1', { type: 'DEPENSE', montant: 50 });
    await svc.deleteTransaction('t-1');
    expect(mock.delete).toHaveBeenCalledWith('/finances/transactions/t-1');
    await svc.getTransaction('t-1');
    expect(mock.get).toHaveBeenCalledWith('/finances/transactions/t-1');
    await svc.listUnreconciled();
    expect(mock.get).toHaveBeenCalledWith('/finances/transactions/unreconciled');
    await svc.reconcileTransaction('t-1');
    expect(mock.post).toHaveBeenCalledWith('/finances/transactions/t-1/reconcile');
  });

  it('stats + budgets (dont GET /budgets/{id} ajouté)', async () => {
    await svc.stats(2026);
    expect(mock.get).toHaveBeenCalledWith('/finances/stats', { params: { annee: 2026 } });
    await svc.statsWithCurrency(2026, 'EUR');
    expect(mock.get).toHaveBeenCalledWith('/finances/stats/currency', { params: { annee: 2026, currency: 'EUR' } });
    await svc.listBudgets(2026);
    expect(mock.get).toHaveBeenCalledWith('/finances/budgets', { params: { annee: 2026 } });
    await svc.getBudget('b-1');
    expect(mock.get).toHaveBeenCalledWith('/finances/budgets/b-1');
    await svc.upsertBudget({ annee: 2026, categorie: 'LOYER', montant: 1000 });
    expect(mock.post).toHaveBeenCalledWith('/finances/budgets', { annee: 2026, categorie: 'LOYER', montant: 1000 });
    await svc.deleteBudget('b-1');
    expect(mock.delete).toHaveBeenCalledWith('/finances/budgets/b-1');
  });

  it('rapprochement: import, auto, unmatched, match, ledger', async () => {
    await svc.importStatement([{ externalKey: 'k', amount: 10 }]);
    expect(mock.post).toHaveBeenCalledWith('/finances/reconciliation/import', {
      lines: [{ externalKey: 'k', amount: 10 }],
    });
    await svc.autoMatch();
    expect(mock.post).toHaveBeenCalledWith('/finances/reconciliation/auto');
    await svc.unmatched();
    expect(mock.get).toHaveBeenCalledWith('/finances/reconciliation/unmatched');
    await svc.manualMatch('l-1', 't-1');
    expect(mock.post).toHaveBeenCalledWith('/finances/reconciliation/match', { lineId: 'l-1', transactionId: 't-1' });
    await svc.ledger();
    expect(mock.get).toHaveBeenCalledWith('/finances/reconciliation/ledger');
  });

  it('comptes + dons', async () => {
    await svc.listAccounts();
    expect(mock.get).toHaveBeenCalledWith('/finances/accounts');
    await svc.getAccount('a-1');
    expect(mock.get).toHaveBeenCalledWith('/finances/accounts/a-1');
    await svc.createAccount({ name: 'Caisse' });
    expect(mock.post).toHaveBeenCalledWith('/finances/accounts', { name: 'Caisse' });
    await svc.listDonations();
    expect(mock.get).toHaveBeenCalledWith('/finances/donations');
    await svc.createDonation({ amount: 500 });
    expect(mock.post).toHaveBeenCalledWith('/finances/donations', { amount: 500 });
  });

  it('tontines (dont POST /tontines/{id}/members ajouté)', async () => {
    await svc.listTontines();
    expect(mock.get).toHaveBeenCalledWith('/finances/tontines');
    await svc.getTontine('to-1');
    expect(mock.get).toHaveBeenCalledWith('/finances/tontines/to-1');
    await svc.createTontine({ name: 'T1' });
    expect(mock.post).toHaveBeenCalledWith('/finances/tontines', { name: 'T1' });
    await svc.listTontineMembers('to-1');
    expect(mock.get).toHaveBeenCalledWith('/finances/tontines/to-1/members');
    await svc.createTontineMember('to-1', 'u-1', 2);
    expect(mock.post).toHaveBeenCalledWith('/finances/tontines/to-1/members', { userId: 'u-1', turnOrder: 2 });
    await svc.createTontineMember('to-1', 'u-2');
    expect(mock.post).toHaveBeenCalledWith('/finances/tontines/to-1/members', { userId: 'u-2' });
    await svc.listTontinePayouts('to-1');
    expect(mock.get).toHaveBeenCalledWith('/finances/tontines/to-1/payouts');
  });

  it('rapports: summary, by-category, cash-flow', async () => {
    await svc.reportSummary();
    expect(mock.get).toHaveBeenCalledWith('/finances/reports/summary');
    await svc.reportByCategory();
    expect(mock.get).toHaveBeenCalledWith('/finances/reports/by-category');
    await svc.reportCashFlow();
    expect(mock.get).toHaveBeenCalledWith('/finances/reports/cash-flow');
  });
});
