import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Wrench, Plus, Trash2, Loader2, Edit2 } from 'lucide-react';
import toast from 'react-hot-toast';

interface Equipment {
  id: string;
  name: string;
  description?: string;
  category?: string;
  status: string;
  quantity: number;
  location?: string;
  createdAt: string;
}

export default function EquipmentPage() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: '', description: '', category: '', quantity: 1, location: '' });

  const { data: equipment = [], isLoading } = useQuery({
    queryKey: ['equipment'],
    queryFn: async () => (await api.get('/equipment')).data as Equipment[],
  });

  const createMutation = useMutation({
    mutationFn: async () => api.post('/equipment', form),
    onSuccess: () => { toast.success('Équipement ajouté'); setShowForm(false); qc.invalidateQueries({ queryKey: ['equipment'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/equipment/${id}`),
    onSuccess: () => { toast.success('Supprimé'); qc.invalidateQueries({ queryKey: ['equipment'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500 to-amber-600 text-white shadow-lg">
          <Wrench className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Équipement</h1>
          <p className="page-subtitle">Gestion du matériel</p>
        </div>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1">
          <Plus className="w-4 h-4" /> Ajouter
        </button>
      </div>

      {showForm && (
        <div className="glass-card p-6 mb-6 space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Nom</label>
              <input className="input w-full" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Catégorie</label>
              <input className="input w-full" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Quantité</label>
              <input type="number" className="input w-full" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })} />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Emplacement</label>
              <input className="input w-full" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} />
            </div>
          </div>
          <div className="flex justify-end gap-3">
            <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">Annuler</button>
            <button onClick={() => createMutation.mutate()} disabled={!form.name.trim()} className="btn-primary btn-sm">Créer</button>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : equipment.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucun équipement</div>
      ) : (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {equipment.map((item) => (
            <div key={item.id} className="glass-card p-5">
              <div className="flex items-start justify-between mb-2">
                <h3 className="font-semibold text-gray-800 dark:text-gray-200">{item.name}</h3>
                <button onClick={() => deleteMutation.mutate(item.id)} className="text-red-400 hover:text-red-300">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
              <div className="flex gap-2 text-xs text-gray-500">
                {item.category && <span className="px-2 py-0.5 rounded-full bg-orange-500/20 text-orange-400">{item.category}</span>}
                <span>Qté: {item.quantity}</span>
                {item.location && <span>📍 {item.location}</span>}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}