import { useEffect, useState } from 'react';
import api from '@/lib/api';
import { Plus, Pencil, Power, RefreshCw } from 'lucide-react';

type Plan = {
  key: string;
  name: string;
  description: string;
  priceMonthly: number;
  priceYearly: number;
  isActive: boolean;
  sortOrder?: number;
};

type PlanForm = {
  key: string;
  name: string;
  description: string;
  priceMonthly: number;
  priceYearly: number;
  isActive: boolean;
};

const emptyForm: PlanForm = {
  key: '',
  name: '',
  description: '',
  priceMonthly: 0,
  priceYearly: 0,
  isActive: true,
};

export default function PlatformPlansPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [form, setForm] = useState<PlanForm>(emptyForm);
  const [editing, setEditing] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get('/platform/admin/plans');
      setPlans(response.data);
    } catch {
      setError('Impossible de charger les plans.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const openCreate = () => {
    setForm(emptyForm);
    setEditing(false);
    setModalOpen(true);
  };

  const openEdit = (plan: Plan) => {
    setForm({
      key: plan.key,
      name: plan.name,
      description: plan.description,
      priceMonthly: plan.priceMonthly,
      priceYearly: plan.priceYearly,
      isActive: plan.isActive,
    });
    setEditing(true);
    setModalOpen(true);
  };

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.key.trim() || !form.name.trim()) return;
    setLoading(true);
    try {
      await api.post('/platform/admin/plans', form);
      setModalOpen(false);
      await load();
    } catch {
      setError('Enregistrement impossible.');
    } finally {
      setLoading(false);
    }
  };

  const toggle = async (plan: Plan) => {
    try {
      await api.post('/platform/admin/plans', { key: plan.key, isActive: !plan.isActive });
      await load();
    } catch {
      setError('Modification impossible.');
    }
  };

  return (
    <main className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold">Plans SaaS</h1>
            <p className="text-sm text-slate-500">Gérer les offres et limites de la plateforme.</p>
          </div>
          <div className="flex gap-2">
            <button type="button" className="inline-flex items-center rounded-md border border-slate-300 bg-white px-4 py-2 text-sm hover:bg-slate-50" onClick={() => void load()} disabled={loading}>
              <RefreshCw className="mr-2 h-4 w-4" /> Actualiser
            </button>
            <button type="button" className="inline-flex items-center rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700" onClick={openCreate}>
              <Plus className="mr-2 h-4 w-4" /> Nouveau plan
            </button>
          </div>
        </div>

        {error && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}

        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {plans.map((plan) => (
            <div key={plan.key} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="font-semibold">{plan.name}</h2>
                  <span className="text-xs uppercase text-slate-400">{plan.key}</span>
                </div>
                <span className={`rounded-full px-2 py-1 text-xs ${plan.isActive ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>
                  {plan.isActive ? 'Actif' : 'Inactif'}
                </span>
              </div>
              <p className="mt-3 min-h-10 text-sm text-slate-600">{plan.description || 'Aucune description'}</p>
              <div className="mt-4 flex items-end justify-between">
                <div>
                  <div className="text-2xl font-semibold">{plan.priceMonthly.toLocaleString()} <span className="text-sm font-normal">FCFA/mois</span></div>
                  <div className="text-xs text-slate-500">{plan.priceYearly.toLocaleString()} FCFA/an</div>
                </div>
                <div className="flex gap-1">
                  <button type="button" className="rounded-md p-2 text-slate-600 hover:bg-slate-100" onClick={() => openEdit(plan)} aria-label={`Modifier ${plan.name}`}><Pencil className="h-4 w-4" /></button>
                  <button type="button" className="rounded-md p-2 text-slate-600 hover:bg-slate-100" onClick={() => void toggle(plan)} aria-label={`${plan.isActive ? 'Désactiver' : 'Activer'} ${plan.name}`}><Power className="h-4 w-4" /></button>
                </div>
              </div>
            </div>
          ))}
        </div>

        {modalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
            <form onSubmit={save} className="w-full max-w-lg space-y-4 rounded-xl bg-white p-6 shadow-xl">
              <h2 className="text-lg font-semibold">{editing ? 'Modifier le plan' : 'Créer un plan'}</h2>
              <label className="block text-sm">Clé<input required value={form.key} disabled={editing} onChange={(event) => setForm({ ...form, key: event.target.value.toUpperCase() })} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2" /></label>
              <label className="block text-sm">Nom<input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2" /></label>
              <label className="block text-sm">Description<textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2" /></label>
              <div className="grid grid-cols-2 gap-3">
                <label className="block text-sm">Prix mensuel<input type="number" min="0" value={form.priceMonthly} onChange={(event) => setForm({ ...form, priceMonthly: Number(event.target.value) })} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2" /></label>
                <label className="block text-sm">Prix annuel<input type="number" min="0" value={form.priceYearly} onChange={(event) => setForm({ ...form, priceYearly: Number(event.target.value) })} className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2" /></label>
              </div>
              <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.isActive} onChange={(event) => setForm({ ...form, isActive: event.target.checked })} /> Actif</label>
              <div className="flex justify-end gap-2"><button type="button" className="rounded-md border px-4 py-2" onClick={() => setModalOpen(false)}>Annuler</button><button type="submit" disabled={loading} className="rounded-md bg-indigo-600 px-4 py-2 text-white disabled:opacity-50">{loading ? 'Enregistrement...' : 'Enregistrer'}</button></div>
            </form>
          </div>
        )}
      </div>
    </main>
  );
}
