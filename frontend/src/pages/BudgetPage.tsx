import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Wallet, Plus, Trash2, Loader2, TrendingUp, TrendingDown } from 'lucide-react';
import toast from 'react-hot-toast';

interface Budget { id: string; name: string; category: string; allocated: number; spent: number; period: string; }

export default function BudgetPage() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: '', category: '', allocated: 0, period: 'monthly' });
  const { data: budgets = [], isLoading } = useQuery({ queryKey: ['budgets'], queryFn: async () => (await api.get('/budgets')).data as Budget[] });
  const createMutation = useMutation({ mutationFn: async () => api.post('/budgets', form), onSuccess: () => { toast.success('Budget créé'); setShowForm(false); qc.invalidateQueries({ queryKey: ['budgets'] }); }, onError: (e) => toast.error(getErrorMessage(e)) });
  const deleteMutation = useMutation({ mutationFn: async (id: string) => api.delete(`/budgets/${id}`), onSuccess: () => { toast.success('Supprimé'); qc.invalidateQueries({ queryKey: ['budgets'] }); }, onError: (e) => toast.error(getErrorMessage(e)) });
  const totalAllocated = budgets.reduce((sum, b) => sum + (b.allocated || 0), 0);
  const totalSpent = budgets.reduce((sum, b) => sum + (b.spent || 0), 0);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg"><Wallet className="w-6 h-6" /></div>
        <div><h1 className="page-title">Budgets</h1><p className="page-subtitle">Gestion budgétaire</p></div>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1"><Plus className="w-4 h-4" /> Nouveau budget</button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="stat-card"><TrendingUp className="w-5 h-5 text-emerald-500" /><p className="stat-value">{totalAllocated.toLocaleString('fr-FR')} F</p><p className="stat-label">Total alloué</p></div>
        <div className="stat-card"><TrendingDown className="w-5 h-5 text-red-500" /><p className="stat-value">{totalSpent.toLocaleString('fr-FR')} F</p><p className="stat-label">Total dépensé</p></div>
        <div className="stat-card"><Wallet className="w-5 h-5 text-blue-500" /><p className="stat-value">{(totalAllocated - totalSpent).toLocaleString('fr-FR')} F</p><p className="stat-label">Restant</p></div>
      </div>
      {showForm && (
        <div className="glass-card p-6 mb-6 space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div><label className="text-xs font-medium text-gray-500 mb-1 block">Nom</label><input className="input w-full" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
            <div><label className="text-xs font-medium text-gray-500 mb-1 block">Catégorie</label><input className="input w-full" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} /></div>
            <div><label className="text-xs font-medium text-gray-500 mb-1 block">Montant alloué (XOF)</label><input type="number" className="input w-full" value={form.allocated} onChange={(e) => setForm({ ...form, allocated: Number(e.target.value) })} /></div>
            <div><label className="text-xs font-medium text-gray-500 mb-1 block">Période</label><select className="input w-full" value={form.period} onChange={(e) => setForm({ ...form, period: e.target.value })}><option value="monthly">Mensuel</option><option value="quarterly">Trimestriel</option><option value="yearly">Annuel</option></select></div>
          </div>
          <div className="flex justify-end gap-3"><button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">Annuler</button><button onClick={() => createMutation.mutate()} disabled={!form.name.trim()} className="btn-primary btn-sm">Créer</button></div>
        </div>
      )}
      {isLoading ? <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div> : budgets.length === 0 ? <div className="glass-card p-10 text-center text-gray-500">Aucun budget</div> : (
        <div className="space-y-3">{budgets.map((b) => (
          <div key={b.id} className="glass-card p-5">
            <div className="flex items-center justify-between mb-3"><div><h3 className="font-semibold text-gray-800 dark:text-gray-200">{b.name}</h3><p className="text-xs text-gray-500">{b.category} • {b.period}</p></div><button onClick={() => deleteMutation.mutate(b.id)} className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button></div>
            <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2 mb-2"><div className="bg-emerald-500 h-2 rounded-full" style={{ width: `${Math.min((b.spent / b.allocated) * 100, 100)}%` }} /></div>
            <div className="flex justify-between text-xs text-gray-500"><span>Dépensé: {b.spent?.toLocaleString('fr-FR')} F</span><span>Alloué: {b.allocated?.toLocaleString('fr-FR')} F</span></div>
          </div>
        ))}</div>
      )}
    </div>
  );
}