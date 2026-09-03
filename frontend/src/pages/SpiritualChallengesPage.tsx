import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Flame, Loader2, CheckCircle2, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';

interface SpiritualChallenge {
  id: string;
  title: string;
  description?: string;
  category: string;
  status: string;
  soulId?: string;
  soulName?: string;
  points?: number;
  deadline?: string;
  createdAt: string;
}

const STATUS_STYLE: Record<string, string> = {
  ACTIVE: 'text-orange-400 bg-orange-500/20',
  COMPLETED: 'text-green-400 bg-green-500/20',
  FAILED: 'text-red-400 bg-red-500/20',
  EXPIRED: 'text-gray-400 bg-gray-500/20',
};

export default function SpiritualChallengesPage() {
  const qc = useQueryClient();

  const { data: challenges = [], isLoading } = useQuery({
    queryKey: ['spiritual-challenges'],
    queryFn: async () => (await api.get('/spiritual-challenges')).data as SpiritualChallenge[],
  });

  const updateStatusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: string }) =>
      api.post(`/spiritual-challenges/${id}/status`, { status }),
    onSuccess: () => { toast.success('Statut mis à jour'); qc.invalidateQueries({ queryKey: ['spiritual-challenges'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const active = challenges.filter((c) => c.status === 'ACTIVE');
  const completed = challenges.filter((c) => c.status === 'COMPLETED');

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500 to-red-600 text-white shadow-lg">
          <Flame className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Défis spirituels</h1>
          <p className="page-subtitle">Défis de croissance spirituelle et défis communautaires</p>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Total', value: challenges.length, color: 'text-orange-600' },
          { label: 'Actifs', value: active.length, color: 'text-red-600' },
          { label: 'Complétés', value: completed.length, color: 'text-green-600' },
          { label: 'Points', value: challenges.reduce((sum, c) => sum + (c.points ?? 0), 0), color: 'text-yellow-600' },
        ].map((s) => (
          <div key={s.label} className="stat-card">
            <p className={`stat-value ${s.color}`}>{s.value}</p>
            <p className="stat-label">{s.label}</p>
          </div>
        ))}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : challenges.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucun défi spirituel</div>
      ) : (
        <div className="space-y-3">
          {challenges.map((c) => (
            <div key={c.id} className={`glass-card p-5 ${c.status === 'ACTIVE' ? 'border-l-[3px] border-l-orange-500' : ''}`}>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[c.status] ?? 'text-gray-400 bg-gray-500/20'}`}>{c.status}</span>
                    <span className="text-xs text-gray-400">{c.category}</span>
                    {c.points != null && <span className="text-xs text-yellow-400">{c.points} pts</span>}
                  </div>
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{c.title}</p>
                  {c.description && <p className="text-xs text-gray-500 mt-1">{c.description}</p>}
                  <div className="flex gap-3 mt-2 text-[11px] text-gray-500">
                    {c.soulName && <span>Participant: {c.soulName}</span>}
                    {c.deadline && <span>Échéance: {new Date(c.deadline).toLocaleDateString('fr-FR')}</span>}
                  </div>
                </div>
                {c.status === 'ACTIVE' && (
                  <div className="flex gap-2 ml-4">
                    <button onClick={() => updateStatusMutation.mutate({ id: c.id, status: 'COMPLETED' })} disabled={updateStatusMutation.isPending}
                      className="btn-sm px-3 py-1.5 rounded-lg bg-green-600 text-white text-xs hover:bg-green-700 flex items-center gap-1">
                      {updateStatusMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle2 className="w-3 h-3" />} Réussi
                    </button>
                    <button onClick={() => updateStatusMutation.mutate({ id: c.id, status: 'FAILED' })} disabled={updateStatusMutation.isPending}
                      className="btn-sm px-3 py-1.5 rounded-lg bg-red-600 text-white text-xs hover:bg-red-700 flex items-center gap-1">
                      <XCircle className="w-3 h-3" /> Échoué
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
