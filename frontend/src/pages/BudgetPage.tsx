import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Wallet, Plus, Trash2, Loader2, TrendingUp, TrendingDown } from 'lucide-react';
import toast from 'react-hot-toast';
import { useI18n } from '@/i18n';

import { tText } from '@/i18n';
// Contrat réel : GET /api/v1/finances/budgets renvoie une liste de cartes
// { id, categorie, annee, montant, depenseReelle, consommationPct, statut }.
interface Budget {
  id: string;
  categorie: string;
  annee: number;
  montant: number;
  depenseReelle: number;
  consommationPct: number;
  statut: 'OK' | 'ALERTE' | 'DEPASSE';
}

const STATUT_STYLE: Record<Budget['statut'], string> = {
  OK: 'bg-emerald-500',
  ALERTE: 'bg-amber-500',
  DEPASSE: 'bg-red-500',
};

export default function BudgetPage() {
  const { locale } = useI18n();
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const currentYear = new Date().getFullYear();
  const [form, setForm] = useState({ categorie: '', annee: currentYear, montant: 0 });

  const { data: budgets = [], isLoading } = useQuery({
    queryKey: ['budgets'],
    queryFn: async () => (await api.get('/finances/budgets')).data as Budget[],
  });
  const createMutation = useMutation({
    mutationFn: async () => api.post('/finances/budgets', form),
    onSuccess: () => { toast.success(tText('Budget créé')); setShowForm(false); setForm({ categorie: '', annee: currentYear, montant: 0 }); qc.invalidateQueries({ queryKey: ['budgets'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });
  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/finances/budgets/${id}`),
    onSuccess: () => { toast.success(tText('Supprimé')); qc.invalidateQueries({ queryKey: ['budgets'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const totalMontant = budgets.reduce((sum, b) => sum + (b.montant || 0), 0);
  const totalDepense = budgets.reduce((sum, b) => sum + (b.depenseReelle || 0), 0);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg"><Wallet className="w-6 h-6" /></div>
        <div><h1 className="page-title">Budgets</h1><p className="page-subtitle">{tText('Gestion budgétaire')}</p></div>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1"><Plus className="w-4 h-4" /> {tText('Nouveau budget')}</button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="stat-card"><TrendingUp className="w-5 h-5 text-emerald-500" /><p className="stat-value">{totalMontant.toLocaleString(locale)} F</p><p className="stat-label">{tText('Total alloué')}</p></div>
        <div className="stat-card"><TrendingDown className="w-5 h-5 text-red-500" /><p className="stat-value">{totalDepense.toLocaleString(locale)} F</p><p className="stat-label">{tText('Total dépensé')}</p></div>
        <div className="stat-card"><Wallet className="w-5 h-5 text-blue-500" /><p className="stat-value">{(totalMontant - totalDepense).toLocaleString(locale)} F</p><p className="stat-label">Restant</p></div>
      </div>
      {showForm && (
        <div className="glass-card p-6 mb-6 space-y-4">
          <div className="grid gap-4 md:grid-cols-3">
            <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Catégorie')}</label><input className="input w-full" value={form.categorie} onChange={(e) => setForm({ ...form, categorie: e.target.value })} /></div>
            <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Année')}</label><input type="number" className="input w-full" value={form.annee} onChange={(e) => setForm({ ...form, annee: Number(e.target.value) })} /></div>
            <div><label className="text-xs font-medium text-gray-500 mb-1 block">{tText('Montant alloué')} (XOF)</label><input type="number" className="input w-full" value={form.montant} onChange={(e) => setForm({ ...form, montant: Number(e.target.value) })} /></div>
          </div>
          <div className="flex justify-end gap-3"><button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">{tText('Annuler')}</button><button onClick={() => createMutation.mutate()} disabled={!form.categorie.trim()} className="btn-primary btn-sm">{tText('Créer')}</button></div>
        </div>
      )}
      {isLoading ? <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div> : budgets.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">{tText('Aucun budget')}</div> : (
        <div className="space-y-3">{budgets.map((b) => (
          <div key={b.id} className="glass-card p-5">
            <div className="flex items-center justify-between mb-3">
              <div>
                <h3 className="font-semibold text-gray-800 dark:text-gray-200">{b.categorie}</h3>
                <p className="text-xs text-gray-500">{b.annee} • {tText('Statut')}: {b.statut}</p>
              </div>
              <button onClick={() => deleteMutation.mutate(b.id)} className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button>
            </div>
            <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2 mb-2"><div className={`h-2 rounded-full ${STATUT_STYLE[b.statut] ?? 'bg-emerald-500'}`} style={{ width: `${Math.min(Number(b.consommationPct) || 0, 100)}%` }} /></div>
            <div className="flex justify-between text-xs text-gray-500"><span>Dépensé: {b.depenseReelle?.toLocaleString(locale)} F</span><span>Alloué: {b.montant?.toLocaleString(locale)} F</span></div>
          </div>
        ))}</div>
      )}
    </div>
  );
}
