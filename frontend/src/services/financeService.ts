import api from '@/lib/api';

/**
 * Contrat exact du backend — FinanceController
 * (backend/.../finances/api/FinanceController.java), monture
 * {@code /api/v1/finances}. Tous les identifiants sont des UUID serveur :
 * typés {@code string}.
 *
 * {@link TransactionRequest} et {@link BudgetRequest} ne listent que les
 * champs réellement lus par {@code FinanceService} (request.type()/
 * categorie()/montant()/description()/dateTransaction() ; annee()/categorie()/
 * montant()) — rien d'inventé. Les autres corps (comptes, dons, tontines)
 * restent en {@link Json} car le service serveur les lit clé par clé.
 */
export type Json = Record<string, unknown>;

export type TransactionType = 'RECETTE' | 'DEPENSE';

export interface TransactionRequest {
  type: TransactionType;
  categorie?: string;
  montant: number;
  description?: string;
  dateTransaction?: string;
}

export interface BudgetRequest {
  annee: number;
  categorie: string;
  montant: number;
}

export interface TransactionsQuery {
  type?: TransactionType;
  categorie?: string;
  debut?: string;
  fin?: string;
}

// ---------- Transactions ----------

export function listTransactions(query: TransactionsQuery = {}): Promise<Json[]> {
  return api.get<Json[]>('/finances/transactions', { params: query }).then((r) => r.data);
}

export function createTransaction(body: TransactionRequest): Promise<Json> {
  return api.post<Json>('/finances/transactions', body).then((r) => r.data);
}

export function updateTransaction(id: string, body: TransactionRequest): Promise<Json> {
  return api.put<Json>(`/finances/transactions/${id}`, body).then((r) => r.data);
}

export function deleteTransaction(id: string): Promise<void> {
  return api.delete(`/finances/transactions/${id}`).then(() => undefined);
}

export function getTransaction(id: string): Promise<Json> {
  return api.get<Json>(`/finances/transactions/${id}`).then((r) => r.data);
}

export function listUnreconciled(): Promise<Json[]> {
  return api.get<Json[]>('/finances/transactions/unreconciled').then((r) => r.data);
}

export function reconcileTransaction(id: string): Promise<Json> {
  return api.post<Json>(`/finances/transactions/${id}/reconcile`).then((r) => r.data);
}

// ---------- Statistiques ----------

export function stats(annee?: number): Promise<Json> {
  return api.get<Json>('/finances/stats', { params: annee == null ? {} : { annee } }).then((r) => r.data);
}

export function statsWithCurrency(annee?: number, currency?: string): Promise<Json> {
  const params: Record<string, string | number> = {};
  if (annee != null) params.annee = annee;
  if (currency != null) params.currency = currency;
  return api.get<Json>('/finances/stats/currency', { params }).then((r) => r.data);
}

// ---------- Budgets ----------

export function listBudgets(annee?: number): Promise<Json[]> {
  return api
    .get<Json[]>('/finances/budgets', { params: annee == null ? {} : { annee } })
    .then((r) => r.data);
}

export function getBudget(id: string): Promise<Json> {
  return api.get<Json>(`/finances/budgets/${id}`).then((r) => r.data);
}

export function upsertBudget(body: BudgetRequest): Promise<Json> {
  return api.post<Json>('/finances/budgets', body).then((r) => r.data);
}

export function deleteBudget(id: string): Promise<void> {
  return api.delete(`/finances/budgets/${id}`).then(() => undefined);
}

// ---------- Rapprochement bancaire ----------

export function importStatement(lines: Json[]): Promise<Json> {
  return api.post<Json>('/finances/reconciliation/import', { lines }).then((r) => r.data);
}

export function autoMatch(): Promise<Json> {
  return api.post<Json>('/finances/reconciliation/auto').then((r) => r.data);
}

export function unmatched(): Promise<Json> {
  return api.get<Json>('/finances/reconciliation/unmatched').then((r) => r.data);
}

export function manualMatch(lineId: string, transactionId: string): Promise<Json> {
  return api
    .post<Json>('/finances/reconciliation/match', { lineId, transactionId })
    .then((r) => r.data);
}

export function ledger(): Promise<Json> {
  return api.get<Json>('/finances/reconciliation/ledger').then((r) => r.data);
}

// ---------- Comptes (V236) ----------

export function listAccounts(): Promise<Json[]> {
  return api.get<Json[]>('/finances/accounts').then((r) => r.data);
}

export function getAccount(id: string): Promise<Json> {
  return api.get<Json>(`/finances/accounts/${id}`).then((r) => r.data);
}

export function createAccount(body: Json): Promise<Json> {
  return api.post<Json>('/finances/accounts', body).then((r) => r.data);
}

// ---------- Dons (V236) ----------

export function listDonations(): Promise<Json[]> {
  return api.get<Json[]>('/finances/donations').then((r) => r.data);
}

export function createDonation(body: Json): Promise<Json> {
  return api.post<Json>('/finances/donations', body).then((r) => r.data);
}

// ---------- Tontines (V236) ----------

export function listTontines(): Promise<Json[]> {
  return api.get<Json[]>('/finances/tontines').then((r) => r.data);
}

export function getTontine(id: string): Promise<Json> {
  return api.get<Json>(`/finances/tontines/${id}`).then((r) => r.data);
}

export function createTontine(body: Json): Promise<Json> {
  return api.post<Json>('/finances/tontines', body).then((r) => r.data);
}

export function listTontineMembers(tontineId: string): Promise<Json[]> {
  return api.get<Json[]>(`/finances/tontines/${tontineId}/members`).then((r) => r.data);
}

export function createTontineMember(tontineId: string, userId: string, turnOrder?: number): Promise<Json> {
  const body: Json = { userId };
  if (turnOrder != null) body.turnOrder = turnOrder;
  return api.post<Json>(`/finances/tontines/${tontineId}/members`, body).then((r) => r.data);
}

export function listTontinePayouts(tontineId: string): Promise<Json[]> {
  return api.get<Json[]>(`/finances/tontines/${tontineId}/payouts`).then((r) => r.data);
}

// ---------- Rapports (V236) ----------

export function reportSummary(): Promise<Json> {
  return api.get<Json>('/finances/reports/summary').then((r) => r.data);
}

export function reportByCategory(): Promise<Json[]> {
  return api.get<Json[]>('/finances/reports/by-category').then((r) => r.data);
}

export function reportCashFlow(): Promise<Json[]> {
  return api.get<Json[]>('/finances/reports/cash-flow').then((r) => r.data);
}
