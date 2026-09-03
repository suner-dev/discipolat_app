import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Users, Plus, Trash2, Loader2, Edit2, Search, Wrench } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';
import SkeletonLoader from '@/components/shared/SkeletonLoader';

interface MemberCompetence {
  id: string;
  memberId: string;
  competenceName: string;
  niveau: number;
  yearsExperience?: number;
  certified: boolean;
  notes?: string;
  createdAt: string;
}

interface CompetenceStats {
  total: number;
  byCompetence: Record<string, number>;
  byLevel: Record<string, number>;
  certifiedCount: number;
}

const COMPETENCES = ['Animation', 'Musique', 'Accueil', 'Prédication', 'Pédagogie', 'Organisation', 'Communication', 'Écoute', 'Leadership', 'Technique', 'Informatique', 'Comptabilité'];

export default function MemberCompetencesPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [showAdd, setShowAdd] = useState(false);
  const [newComp, setNewComp] = useState({ memberId: '', competenceName: 'Animation', niveau: '2', yearsExperience: '', certified: false, notes: '' });

  const { data: competences = [], isLoading } = useQuery({
    queryKey: ['member-competences', searchQuery],
    queryFn: async () => {
      if (searchQuery) return (await api.get(`/members/competences/search?competenceName=${encodeURIComponent(searchQuery)}`)).data;
      return (await api.get('/members/competences/mine')).data;
    },
  });

  const { data: stats } = useQuery({
    queryKey: ['member-competences-stats'],
    queryFn: async () => (await api.get<CompetenceStats>('/members/competences/stats')).data,
  });

  const addMutation = useMutation({
    mutationFn: async () => {
      if (!newComp.memberId.trim()) { toast('Entrez l\'ID du membre', { icon: '⚠️' }); throw new Error('empty'); }
      return api.post('/members/competences', { ...newComp, niveau: parseInt(newComp.niveau) });
    },
    onSuccess: () => {
      toast.success('Compétence ajoutée');
      setShowAdd(false);
      setNewComp({ memberId: '', competenceName: 'Animation', niveau: '2', yearsExperience: '', certified: false, notes: '' });
      queryClient.invalidateQueries({ queryKey: ['member-competences'] });
    },
    onError: (e: unknown) => { if ((e as Error).message !== 'empty') toast.error(getErrorMessage(e)); },
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/members/competences/${id}`),
    onSuccess: () => { toast.success('Supprimé'); queryClient.invalidateQueries({ queryKey: ['member-competences'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-blue-600 text-white shadow-lg">
          <Wrench className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Compétences des Membres</h1>
          <p className="page-subtitle">Gérez les compétences et expertise de vos membres</p>
        </div>
        <div className="ml-auto flex gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
              className="pl-9 pr-3 py-2 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm w-48"
              placeholder="Rechercher..." />
          </div>
          <button onClick={() => setShowAdd(true)}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 text-white text-sm font-medium hover:from-indigo-600 hover:to-blue-600 transition-all shadow-lg flex items-center gap-2">
            <Plus className="w-4 h-4" /> Ajouter
          </button>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-gray-900 dark:text-white">{stats.total}</div>
            <div className="text-xs text-gray-500">Total compétences</div>
          </div>
          <div className="glass-card p-4 text-center">
            <div className="text-2xl font-bold text-green-600">{stats.certifiedCount}</div>
            <div className="text-xs text-gray-500">Certifiées</div>
          </div>
          {Object.entries(stats.byCompetence || {}).slice(0, 2).map(([key, val]) => (
            <div key={key} className="glass-card p-4 text-center">
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{val}</div>
              <div className="text-xs text-gray-500">{key}</div>
            </div>
          ))}
        </div>
      )}

      {isLoading ? <SkeletonLoader lines={4} variant="card" /> :
        competences.length === 0 ? (
          <EmptyState icon={<Wrench className="w-8 h-8 text-gray-400" />}
            title="Aucune compétence"
            message="Ajoutez des compétences à vos membres"
            action={{ label: 'Ajouter une compétence', onClick: () => setShowAdd(true) }} />
        ) : (
          <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
            {competences.map((c: MemberCompetence) => (
              <div key={c.id} className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="px-2 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-500/20 text-indigo-700 dark:text-indigo-400 text-xs font-medium">
                      {c.competenceName}
                    </span>
                    {c.certified && <span className="px-2 py-0.5 rounded-full bg-green-100 dark:bg-green-500/20 text-green-700 dark:text-green-400 text-xs">Certifié</span>}
                  </div>
                  <div className="text-xs text-gray-500">
                    Niveau: {c.niveau}/5 · Membre: {c.memberId.slice(0, 8)}...
                  </div>
                  {c.notes && <div className="text-xs text-gray-400 mt-1">{c.notes}</div>}
                </div>
                <button onClick={() => deleteMutation.mutate(c.id)} className="text-red-400 hover:text-red-600 p-1">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        )}

      {showAdd && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setShowAdd(false)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-2xl shadow-2xl max-w-lg w-full p-6 border border-gray-200 dark:border-white/10">
            <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Ajouter une compétence</h2>
            <div className="space-y-4">
              <input type="text" value={newComp.memberId} onChange={e => setNewComp({ ...newComp, memberId: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
                placeholder="ID du membre" />
              <select value={newComp.competenceName} onChange={e => setNewComp({ ...newComp, competenceName: e.target.value })}
                className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm">
                {COMPETENCES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
              <div className="flex gap-3">
                <div className="flex-1">
                  <label className="block text-xs text-gray-500 mb-1">Niveau (1-5)</label>
                  <input type="number" min="1" max="5" value={newComp.niveau} onChange={e => setNewComp({ ...newComp, niveau: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
                </div>
                <div className="flex-1">
                  <label className="block text-xs text-gray-500 mb-1">Années d'expérience</label>
                  <input type="number" value={newComp.yearsExperience} onChange={e => setNewComp({ ...newComp, yearsExperience: e.target.value })}
                    className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm" />
                </div>
              </div>
              <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                <input type="checkbox" checked={newComp.certified} onChange={e => setNewComp({ ...newComp, certified: e.target.checked })}
                  className="rounded border-gray-300" />
                Certifié
              </label>
              <textarea value={newComp.notes} onChange={e => setNewComp({ ...newComp, notes: e.target.value })}
                rows={2} className="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm resize-none"
                placeholder="Notes (optionnel)" />
            </div>
            <div className="flex justify-end gap-3 mt-6">
              <button onClick={() => setShowAdd(false)} className="px-4 py-2 rounded-xl border text-sm">Annuler</button>
              <button onClick={() => addMutation.mutate()} disabled={addMutation.isPending}
                className="px-4 py-2 rounded-xl bg-indigo-500 text-white text-sm font-medium hover:bg-indigo-600 flex items-center gap-2">
                {addMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                Ajouter
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
