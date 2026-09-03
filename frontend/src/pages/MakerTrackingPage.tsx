import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Footprints, Plus, Trash2, Loader2, User, Calendar, FileText } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface MakerEvent {
  id: string;
  faiseurId: string;
  type: string;
  description: string;
  date: string;
  contactNom?: string;
  notes?: string;
  createdAt: string;
}

interface MakerResume {
  totalEvents: number;
  byType: Record<string, number>;
  recentActivity: string;
}

const EVENT_TYPES = ['VISITE', 'APPEL', 'PRIÈRE', 'RÉUNION', 'FORMATION', 'ÉVANGÉLISATION', 'AUTRE'];

export default function MakerTrackingPage() {
  const queryClient = useQueryClient();
  const [faiseurId, setFaiseurId] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [newEvent, setNewEvent] = useState({ type: 'VISITE', description: '', contactNom: '', notes: '' });

  const { data: events = [], isLoading } = useQuery({
    queryKey: ['maker-tracking', faiseurId],
    queryFn: async () => {
      const params = faiseurId ? `?faiseurId=${faiseurId}` : '';
      const res = await api.get(`/maker-tracking${params}`);
      return (res.data?.content || res.data || []) as MakerEvent[];
    },
  });

  const { data: resume } = useQuery({
    queryKey: ['maker-tracking-resume', faiseurId],
    queryFn: async () => {
      const id = faiseurId || 'me';
      return (await api.get<MakerResume>(`/maker-tracking/resume/${id}`)).data;
    },
  });

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!newEvent.description.trim()) { toast('Entrez une description', { icon: '⚠️' }); throw new Error('empty'); }
      return api.post('/maker-tracking', { ...newEvent, faiseurId: faiseurId || undefined });
    },
    onSuccess: () => {
      toast.success('Événement enregistré');
      setShowCreate(false);
      setNewEvent({ type: 'VISITE', description: '', contactNom: '', notes: '' });
      queryClient.invalidateQueries({ queryKey: ['maker-tracking'] });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/maker-tracking/${id}`),
    onSuccess: () => { toast.success('Supprimé'); queryClient.invalidateQueries({ queryKey: ['maker-tracking'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500 to-amber-600 text-white shadow-lg">
          <Footprints className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Suivi des Faiseurs</h1>
          <p className="page-subtitle">Traçabilité des activités d'évangélisation</p>
        </div>
        <div className="ml-auto flex gap-2">
          <input type="text" value={faiseurId} onChange={e => setFaiseurId(e.target.value)}
            className="px-3 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm w-40"
            placeholder="ID faiseur (optionnel)" />
          <button onClick={() => setShowCreate(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-orange-500 to-amber-500 text-white text-sm font-medium hover:from-orange-600 hover:to-amber-600 transition-all shadow-lg flex items-center gap-2">
            <Plus className="w-4 h-4" /> Nouvel événement
          </button>
        </div>
      </div>

      {resume && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">{resume.totalEvents}</div>
            <div className="text-xs text-gray-500">Total événements</div>
          </div>
          {Object.entries(resume.byType || {}).slice(0, 3).map(([key, val]) => (
            <div key={key} className="glass-card p-4 text-center">
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{val}</div>
              <div className="text-xs text-gray-500">{key}</div>
            </div>
          ))}
        </div>
      )}

      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
        events.length === 0 ? (
          <EmptyState icon={<Footprints className="w-8 h-8 text-gray-400" />}
            title="Aucun événement"
            message="Enregistrez les activités de vos faiseurs"
            action={{ label: 'Ajouter un événement', onClick: () => setShowCreate(true) }} />
        ) : (
          <div className="space-y-3">
            {events.map(ev => (
              <div key={ev.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 flex items-center gap-4">
                <div className="w-10 h-10 rounded-xl bg-orange-100 dark:bg-orange-500/20 flex items-center justify-center">
                  <Footprints className="w-5 h-5 text-orange-600 dark:text-orange-400" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="px-2 py-0.5 rounded-full bg-orange-100 dark:bg-orange-500/20 text-orange-700 dark:text-orange-400 text-xs font-medium">{ev.type}</span>
                    {ev.contactNom && <span className="text-xs text-gray-500 flex items-center gap-1"><User className="w-3 h-3" />{ev.contactNom}</span>}
                  </div>
                  <p className="text-sm text-gray-700 dark:text-gray-300">{ev.description}</p>
                  {ev.notes && <p className="text-xs text-gray-400 mt-1">{ev.notes}</p>}
                </div>
                <div className="text-xs text-gray-400 flex flex-col items-end gap-1 flex-shrink-0">
                  <span className="flex items-center gap-1"><Calendar className="w-3 h-3" />{new Date(ev.date || ev.createdAt).toLocaleDateString('fr-FR')}</span>
                  <button onClick={() => deleteMutation.mutate(ev.id)} className="text-red-400 hover:text-red-600"><Trash2 className="w-3 h-3" /></button>
                </div>
              </div>
            ))}
          </div>
        )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Nouvel événement</h2>
            <div className="space-y-4">
              <select value={newEvent.type} onChange={e => setNewEvent({ ...newEvent, type: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                {EVENT_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
              <input type="text" value={newEvent.description} onChange={e => setNewEvent({ ...newEvent, description: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="Description" />
              <input type="text" value={newEvent.contactNom} onChange={e => setNewEvent({ ...newEvent, contactNom: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="Nom du contact" />
              <textarea value={newEvent.notes} onChange={e => setNewEvent({ ...newEvent, notes: e.target.value })}
                rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none"
                placeholder="Notes (optionnel)" />
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowCreate(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => createMutation.mutate()} disabled={createMutation.isPending}
                className="px-4 py-2 rounded-xl bg-orange-500 text-white text-sm font-medium hover:bg-orange-600 flex items-center gap-2">
                {createMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Enregistrer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
