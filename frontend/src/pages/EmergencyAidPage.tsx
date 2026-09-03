import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { AlertTriangle, Loader2, Plus, CheckCircle2, Package } from 'lucide-react';
import toast from 'react-hot-toast';

interface Emergency {
  id: string;
  title: string;
  description: string;
  severity: string;
  status: string;
  location?: string;
  contactName?: string;
  contactPhone?: string;
  createdAt: string;
}

const STATUS_STYLE: Record<string, string> = {
  OPEN: 'text-red-400 bg-red-500/20',
  COLLECTING: 'text-yellow-400 bg-yellow-500/20',
  RESOLVED: 'text-green-400 bg-green-500/20',
};

export default function EmergencyAidPage() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ title: '', description: '', severity: 'HIGH', location: '', contactName: '', contactPhone: '' });

  const { data: emergencies = [], isLoading } = useQuery({
    queryKey: ['emergency-aid'],
    queryFn: async () => (await api.get('/aid/emergency/open')).data as Emergency[],
  });

  const createMutation = useMutation({
    mutationFn: async () => api.post('/aid/emergency', form),
    onSuccess: () => { toast.success('Urgence créée'); setShowForm(false); setForm({ title: '', description: '', severity: 'HIGH', location: '', contactName: '', contactPhone: '' }); qc.invalidateQueries({ queryKey: ['emergency-aid'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const collectMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/aid/emergency/${id}/collect`),
    onSuccess: () => { toast.success('Collecte lancée'); qc.invalidateQueries({ queryKey: ['emergency-aid'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const resolveMutation = useMutation({
    mutationFn: async (id: string) => api.post(`/aid/emergency/${id}/resolve`),
    onSuccess: () => { toast.success('Urgence résolue'); qc.invalidateQueries({ queryKey: ['emergency-aid'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-red-500 to-orange-600 text-white shadow-lg">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Aide d'urgence</h1>
          <p className="page-subtitle">Gestion des situations d'urgence et collectes</p>
        </div>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary btn-sm ml-auto inline-flex items-center gap-1">
          <Plus className="w-4 h-4" /> Nouvelle urgence
        </button>
      </div>

      {showForm && (
        <div className="glass-card p-6 mb-6 space-y-4 animate-slide-up">
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Titre</label>
              <input className="input w-full" placeholder="Titre de l'urgence" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Sévérité</label>
              <select className="input w-full" value={form.severity} onChange={(e) => setForm({ ...form, severity: e.target.value })}>
                <option value="LOW">Faible</option>
                <option value="MEDIUM">Moyen</option>
                <option value="HIGH">Élevé</option>
                <option value="CRITICAL">Critique</option>
              </select>
            </div>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Description</label>
            <textarea className="input w-full min-h-[80px]" placeholder="Décrivez la situation..." value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          </div>
          <div className="grid gap-4 md:grid-cols-3">
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Lieu</label>
              <input className="input w-full" placeholder="Lieu" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Contact</label>
              <input className="input w-full" placeholder="Nom du contact" value={form.contactName} onChange={(e) => setForm({ ...form, contactName: e.target.value })} />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Téléphone</label>
              <input className="input w-full" placeholder="+225..." value={form.contactPhone} onChange={(e) => setForm({ ...form, contactPhone: e.target.value })} />
            </div>
          </div>
          <div className="flex justify-end gap-3">
            <button onClick={() => setShowForm(false)} className="btn-sm px-4 py-2 rounded-lg glass-card">Annuler</button>
            <button onClick={() => createMutation.mutate()} disabled={!form.title.trim() || createMutation.isPending}
              className="btn-primary btn-sm inline-flex items-center gap-1">
              {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />} Créer
            </button>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : emergencies.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucune urgence ouverte</div>
      ) : (
        <div className="space-y-3">
          {emergencies.map((e) => (
            <div key={e.id} className={`glass-card p-5 ${e.status === 'OPEN' ? 'border-l-[3px] border-l-red-500' : ''}`}>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[e.status] ?? 'text-gray-400 bg-gray-500/20'}`}>{e.status}</span>
                    <span className="text-xs text-gray-400">{e.severity}</span>
                  </div>
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{e.title}</p>
                  <p className="text-xs text-gray-500 mt-1">{e.description}</p>
                  <div className="flex gap-4 mt-2 text-[11px] text-gray-500">
                    {e.location && <span>{e.location}</span>}
                    {e.contactName && <span>Contact: {e.contactName}</span>}
                    <span>{new Date(e.createdAt).toLocaleDateString('fr-FR')}</span>
                  </div>
                </div>
                <div className="flex gap-2 ml-4">
                  {e.status === 'OPEN' && (
                    <button onClick={() => collectMutation.mutate(e.id)} disabled={collectMutation.isPending}
                      className="btn-sm px-3 py-1.5 rounded-lg bg-yellow-600 text-white text-xs hover:bg-yellow-700 flex items-center gap-1">
                      {collectMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <Package className="w-3 h-3" />} Collecter
                    </button>
                  )}
                  {e.status !== 'RESOLVED' && (
                    <button onClick={() => resolveMutation.mutate(e.id)} disabled={resolveMutation.isPending}
                      className="btn-sm px-3 py-1.5 rounded-lg bg-green-600 text-white text-xs hover:bg-green-700 flex items-center gap-1">
                      {resolveMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle2 className="w-3 h-3" />} Résoudre
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
