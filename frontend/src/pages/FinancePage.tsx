import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import { usePlatformConfig } from '@/contexts/PlatformContext';
import {
  Wallet, ArrowDownCircle, ArrowUpCircle, PiggyBank, Plus, Pencil, Trash2, Loader2,
  Download, TrendingUp, TrendingDown, Scale, X, BarChart3,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import type {
  FinanceTransaction, FinanceTransactionType, FinanceBudget, FinanceStats,
  CreateFinanceTransactionRequest, CreateFinanceBudgetRequest,
} from '@/types';

const fmt = (v: number | null | undefined) =>
  new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 }).format(v ?? 0);

const money = (v: number | null | undefined) => `${fmt(v)} FCFA`;

const TYPE_LABELS: Record<string, string> = { RECETTE: 'Recette', DEPENSE: 'Dépense' };

export default function FinancePage() {
  const { moduleEnabled } = usePlatformConfig();
  const queryClient = useQueryClient();
  const year = new Date().getFullYear();
  const [annee, setAnnee] = useState(year);
  const [typeFilter, setTypeFilter] = useState('');
  const [categorieFilter, setCategorieFilter] = useState('');
  const [modal, setModal] = useState<null | { edit?: FinanceTransaction }>(null);

  if (!moduleEnabled('FINANCES')) {
    return (
      <div className="page-container flex flex-col items-center justify-center min-h-[60vh] text-center">
        <Wallet className="w-10 h-10 text-gray-300 mb-3" />
        <h1 className="text-lg font-semibold text-gray-800 dark:text-gray-100">Module Finances désactivé</h1>
        <p className="text-sm text-gray-400 mt-1">
          L'administrateur a désactivé ce module. Réactivez-le depuis l'espace d'administration.
        </p>
        <Link to="/dashboard" className="btn-ghost btn-sm mt-4">Retour au tableau de bord</Link>
      </div>
    );
  }

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['finances'] });
  };

  const { data: transactions = [], isLoading: loadingTx } = useQuery({
    queryKey: ['finances', 'transactions', typeFilter, categorieFilter],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (typeFilter) params.set('type', typeFilter);
      if (categorieFilter) params.set('categorie', categorieFilter);
      const res = await api.get(`/finances/transactions?${params.toString()}`);
      return res.data as FinanceTransaction[];
    },
  });

  const { data: stats } = useQuery({
    queryKey: ['finances', 'stats', annee],
    queryFn: async () => (await api.get(`/finances/stats?annee=${annee}`)).data as FinanceStats,
  });

  const { data: budgets = [] } = useQuery({
    queryKey: ['finances', 'budgets', annee],
    queryFn: async () => (await api.get(`/finances/budgets?annee=${annee}`)).data as FinanceBudget[],
  });

  const categories = useMemo(() => {
    const set = new Set<string>();
    for (const t of transactions) if (t.categorie) set.add(t.categorie);
    return [...set].sort();
  }, [transactions]);

  const saveTxMutation = useMutation({
    mutationFn: async (payload: CreateFinanceTransactionRequest) => {
      if (modal?.edit) await api.put(`/finances/transactions/${modal.edit.id}`, payload);
      else await api.post('/finances/transactions', payload);
    },
    onSuccess: () => { invalidate(); setModal(null); toast.success(modal?.edit ? 'Transaction modifiée' : 'Transaction enregistrée'); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const deleteTxMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/finances/transactions/${id}`),
    onSuccess: () => { invalidate(); toast.success('Transaction supprimée'); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const saveBudgetMutation = useMutation({
    mutationFn: async (payload: CreateFinanceBudgetRequest) => api.post('/finances/budgets', payload),
    onSuccess: () => { invalidate(); toast.success('Budget enregistré'); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const deleteBudgetMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/finances/budgets/${id}`),
    onSuccess: () => { invalidate(); toast.success('Budget supprimé'); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const exportCsv = () => {
    const lines = [
      '\uFEFFDate;Type;Catégorie;Description;Montant',
      ...transactions.map((t) =>
        [t.dateTransaction, TYPE_LABELS[t.type] || t.type, t.categorie,
          (t.description || '').replace(/;/g, ','), fmt(t.montant)].join(';')),
    ];
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `finances-${annee}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
    toast.success('Export CSV téléchargé 📥');
  };

  const chartData = stats?.parMois || [];

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <Wallet className="w-5 h-5 text-primary-500" /> Finances
          </h1>
          <p className="page-subtitle">
            Recettes, dépenses et budget de l'église — toutes les statistiques sont calculées
            sur les transactions réelles.
          </p>
        </div>
        <div className="page-header-actions">
          <button className="btn-ghost btn-sm" onClick={exportCsv} disabled={transactions.length === 0}>
            <Download className="w-4 h-4" /> Export CSV
          </button>
          <button className="btn-primary btn-sm" onClick={() => setModal({})}>
            <Plus className="w-4 h-4" /> Nouvelle transaction
          </button>
        </div>
      </div>

      {/* Sélecteur d'année */}
      <div className="flex items-center gap-2 mb-5">
        <label className="label !mb-0">Année</label>
        <select className="input w-36" value={annee} onChange={(e) => setAnnee(Number(e.target.value))}>
          {[year, year - 1, year - 2].map((y) => <option key={y} value={y}>{y}</option>)}
        </select>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Recettes', value: stats ? money(stats.totalRecettes) : '—', icon: TrendingUp, color: 'from-emerald-500 to-green-600', sub: `${stats?.nbTransactions ?? 0} transaction(s)` },
          { label: 'Dépenses', value: stats ? money(stats.totalDepenses) : '—', icon: TrendingDown, color: 'from-rose-500 to-red-600' },
          { label: 'Solde', value: stats ? money(stats.solde) : '—', icon: Scale, color: (stats?.solde ?? 0) >= 0 ? 'from-blue-500 to-indigo-600' : 'from-rose-500 to-red-600' },
          { label: 'Budgets', value: `${budgets.length}`, icon: PiggyBank, color: 'from-amber-500 to-orange-600', sub: budgets.length > 0 ? `${budgets.filter((b) => b.statut === 'DEPASSE').length} dépassé(s)` : '' },
        ].map((kpi, i) => (
          <div key={kpi.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
            <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${kpi.color} opacity-60`} />
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{kpi.label}</span>
              <div className={`p-1.5 rounded-lg bg-gradient-to-br ${kpi.color} text-white shadow-sm`}>
                <kpi.icon className="w-3.5 h-3.5" />
              </div>
            </div>
            <p className="stat-value text-xl">{kpi.value}</p>
            {kpi.sub && <p className="text-[10px] text-gray-400 mt-0.5">{kpi.sub}</p>}
          </div>
        ))}
      </div>

      {/* Graphique recettes / dépenses par mois */}
      {chartData.length > 0 && (
        <div className="glass-card p-5 mb-6 animate-slide-up" style={{ animationDelay: '150ms' }}>
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-3 flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-primary-500" /> Recettes / dépenses par mois ({annee})
          </h3>
          <div className="h-60">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#333" opacity={0.2} />
                <XAxis dataKey="mois" tick={{ fontSize: 10 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
                <Tooltip formatter={(v) => fmt(Number(v))} />
                <Legend />
                <Bar dataKey="recettes" name="Recettes" fill="#22c55e" radius={[4, 4, 0, 0]} />
                <Bar dataKey="depenses" name="Dépenses" fill="#ef4444" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* Budgets */}
      <div className="glass-card p-5 mb-6">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <PiggyBank className="w-4 h-4 text-primary-500" /> Budget {annee} par catégorie
          </h3>
          <button className="btn-ghost btn-sm" onClick={() => saveBudgetMutation.mutate({ categorie: '', annee, montant: 0 })}>
            <Plus className="w-4 h-4" /> Nouveau budget
          </button>
        </div>
        {budgets.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-4">Aucun budget défini pour {annee}.</p>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {budgets.map((b) => (
              <div key={b.id} className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-sm font-semibold text-gray-800 dark:text-gray-100">{b.categorie}</span>
                  <div className="flex items-center gap-1">
                    <button className="p-1 rounded text-gray-400 hover:text-amber-500" onClick={() => saveBudgetMutation.mutate({ categorie: b.categorie, annee, montant: b.montant })} title="Modifier">
                      <Pencil className="w-3.5 h-3.5" />
                    </button>
                    <button className="p-1 rounded text-gray-400 hover:text-red-500" onClick={() => { if (confirm(`Supprimer le budget « ${b.categorie} » ?`)) deleteBudgetMutation.mutate(b.id); }} title="Supprimer">
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
                <p className="text-xs text-gray-500">Prévu : {money(b.montant)}</p>
                <p className="text-xs text-gray-500 mb-2">Dépensé : {money(b.depenseReelle)}</p>
                <div className="h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                  <div
                    className={`h-full rounded-full ${b.statut === 'DEPASSE' ? 'bg-red-500' : b.statut === 'ALERTE' ? 'bg-amber-500' : 'bg-emerald-500'}`}
                    style={{ width: `${Math.min(100, b.consommationPct)}%` }}
                  />
                </div>
                <p className={`text-[10px] mt-1 font-medium ${b.statut === 'DEPASSE' ? 'text-red-500' : b.statut === 'ALERTE' ? 'text-amber-500' : 'text-emerald-500'}`}>
                  {fmt(b.consommationPct)} % {b.statut === 'DEPASSE' ? '— budget dépassé' : b.statut === 'ALERTE' ? '— alerte' : ''}
                </p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Transactions */}
      <div className="glass-card overflow-hidden">
        <div className="card-header flex flex-wrap gap-3">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">Transactions</h3>
          <div className="flex-1" />
          <select className="input !w-auto !py-1.5 text-xs" value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)} aria-label="Filtrer par type">
            <option value="">Tous les types</option>
            <option value="RECETTE">Recettes</option>
            <option value="DEPENSE">Dépenses</option>
          </select>
          <select className="input !w-auto !py-1.5 text-xs" value={categorieFilter} onChange={(e) => setCategorieFilter(e.target.value)} aria-label="Filtrer par catégorie">
            <option value="">Toutes les catégories</option>
            {categories.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
        {loadingTx ? (
          <div className="flex justify-center py-8"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
        ) : transactions.length === 0 ? (
          <p className="px-5 py-8 text-sm text-gray-400 text-center">Aucune transaction. Ajoutez la première ci-dessus.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-[11px] uppercase tracking-wider text-gray-400 border-b border-gray-100/60 dark:border-gray-800/40">
                  <th className="px-5 py-2.5 font-semibold">Date</th>
                  <th className="px-5 py-2.5 font-semibold">Type</th>
                  <th className="px-5 py-2.5 font-semibold">Catégorie</th>
                  <th className="px-5 py-2.5 font-semibold">Description</th>
                  <th className="px-5 py-2.5 font-semibold text-right">Montant</th>
                  <th className="px-5 py-2.5 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100/50 dark:divide-gray-800/30">
                {transactions.map((t) => (
                  <tr key={t.id} className="text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800/30">
                    <td className="px-5 py-2.5">{t.dateTransaction}</td>
                    <td className="px-5 py-2.5">
                      <span className={`badge ${t.type === 'RECETTE' ? 'badge-success' : 'badge-error'}`}>
                        {t.type === 'RECETTE' ? <ArrowDownCircle className="w-3 h-3" /> : <ArrowUpCircle className="w-3 h-3" />}
                        {TYPE_LABELS[t.type] || t.type}
                      </span>
                    </td>
                    <td className="px-5 py-2.5 font-medium">{t.categorie}</td>
                    <td className="px-5 py-2.5 text-gray-500">{t.description || '—'}</td>
                    <td className={`px-5 py-2.5 text-right font-semibold ${t.type === 'RECETTE' ? 'text-emerald-600' : 'text-rose-600'}`}>
                      {t.type === 'RECETTE' ? '+' : '−'} {money(t.montant)}
                    </td>
                    <td className="px-5 py-2.5">
                      <div className="flex items-center justify-end gap-1">
                        <button className="p-1.5 rounded-lg text-gray-400 hover:text-amber-500 hover:bg-amber-500/10" onClick={() => setModal({ edit: t })} title="Modifier">
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-500/10" onClick={() => { if (confirm(`Supprimer cette transaction (${money(t.montant)}) ?`)) deleteTxMutation.mutate(t.id); }} title="Supprimer">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modal && (
        <TransactionModal
          edit={modal.edit}
          onClose={() => setModal(null)}
          onSave={(payload) => saveTxMutation.mutate(payload)}
          pending={saveTxMutation.isPending}
        />
      )}
    </div>
  );
}

function TransactionModal({ edit, onClose, onSave, pending }: {
  edit?: FinanceTransaction;
  onClose: () => void;
  onSave: (p: CreateFinanceTransactionRequest) => void;
  pending: boolean;
}) {
  const today = new Date().toISOString().split('T')[0];
  const [type, setType] = useState<FinanceTransactionType>(edit?.type || 'RECETTE');
  const [categorie, setCategorie] = useState(edit?.categorie || '');
  const [montant, setMontant] = useState(edit ? String(edit.montant) : '');
  const [description, setDescription] = useState(edit?.description || '');
  const [dateTransaction, setDateTransaction] = useState(edit?.dateTransaction || today);

  const submit = () => {
    const value = Number(montant.replace(',', '.'));
    if (!categorie.trim() || !Number.isFinite(value) || value <= 0 || !dateTransaction) return;
    onSave({ type, categorie: categorie.trim().toUpperCase(), montant: value, description: description.trim() || undefined, dateTransaction });
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div className="glass-card p-5 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
            {edit ? 'Modifier la transaction' : 'Nouvelle transaction'}
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-gray-400 hover:text-gray-600 cursor-pointer">
            <X className="w-4 h-4" />
          </button>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label" htmlFor="tx-type">Type</label>
            <select id="tx-type" className="input" value={type} onChange={(e) => setType(e.target.value as FinanceTransactionType)}>
              <option value="RECETTE">Recette</option>
              <option value="DEPENSE">Dépense</option>
            </select>
          </div>
          <div>
            <label className="label" htmlFor="tx-date">Date</label>
            <input id="tx-date" type="date" className="input" value={dateTransaction} onChange={(e) => setDateTransaction(e.target.value)} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3 mt-3">
          <div>
            <label className="label" htmlFor="tx-categorie">Catégorie</label>
            <input id="tx-categorie" className="input" value={categorie} onChange={(e) => setCategorie(e.target.value)} placeholder="DÎME, LOYER…" />
          </div>
          <div>
            <label className="label" htmlFor="tx-montant">Montant</label>
            <input id="tx-montant" type="number" min="0" step="0.01" className="input" value={montant} onChange={(e) => setMontant(e.target.value)} placeholder="0" />
          </div>
        </div>
        <div className="mt-3">
          <label className="label" htmlFor="tx-description">Description</label>
          <input id="tx-description" className="input" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <button
          onClick={submit}
          disabled={pending || !categorie.trim() || !(Number(montant.replace(',', '.')) > 0)}
          className="btn-primary btn-sm mt-4 w-full justify-center cursor-pointer"
        >
          {pending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
          {edit ? 'Enregistrer' : 'Ajouter'}
        </button>
      </div>
    </div>
  );
}
