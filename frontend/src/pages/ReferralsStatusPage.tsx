import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Share2, Loader2, Send } from 'lucide-react';
import toast from 'react-hot-toast';

interface Referral {
  id: string;
  referrerName?: string;
  referredName?: string;
  referredEmail?: string;
  status: string;
  message?: string;
  createdAt: string;
}

const STATUS_STYLE: Record<string, string> = {
  PENDING: 'text-yellow-400 bg-yellow-500/20',
  CONTACTED: 'text-blue-400 bg-blue-500/20',
  JOINED: 'text-green-400 bg-green-500/20',
  DECLINED: 'text-red-400 bg-red-500/20',
};

export default function ReferralsStatusPage() {
  const qc = useQueryClient();
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  const { data: referrals = [], isLoading } = useQuery({
    queryKey: ['referrals'],
    queryFn: async () => (await api.get('/referrals')).data as Referral[],
  });

  const updateStatusMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: string }) =>
      api.patch(`/referrals/${id}/status`, { status }),
    onSuccess: () => { toast.success('Statut mis à jour'); setUpdatingId(null); qc.invalidateQueries({ queryKey: ['referrals'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const statuses = ['PENDING', 'CONTACTED', 'JOINED', 'DECLINED'];

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-pink-500 to-rose-600 text-white shadow-lg">
          <Share2 className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Statut des parrainages</h1>
          <p className="page-subtitle">Suivi des invitations et parrainages</p>
        </div>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        {statuses.map((s) => (
          <div key={s} className="stat-card">
            <p className="stat-value">{referrals.filter((r) => r.status === s).length}</p>
            <p className="stat-label">{s.replace(/_/g, ' ')}</p>
          </div>
        ))}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : referrals.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucun parrainage</div>
      ) : (
        <div className="space-y-3">
          {referrals.map((r) => (
            <div key={r.id} className="glass-card p-5">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[r.status] ?? 'text-gray-400 bg-gray-500/20'}`}>{r.status}</span>
                  </div>
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200">
                    {r.referredName ?? r.referredEmail ?? 'Inconnu'}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    Parrainé par {r.referrerName ?? 'Inconnu'}
                    {r.message && ` — "${r.message}"`}
                  </p>
                  <p className="text-[11px] text-gray-500 mt-1">{new Date(r.createdAt).toLocaleDateString('fr-FR')}</p>
                </div>
                <div className="ml-4">
                  {updatingId === r.id ? (
                    <div className="flex items-center gap-1">
                      {statuses.filter((s) => s !== r.status).map((s) => (
                        <button key={s} onClick={() => updateStatusMutation.mutate({ id: r.id, status: s })} disabled={updateStatusMutation.isPending}
                          className="btn-sm px-2 py-1 rounded bg-white/5 text-[10px] text-gray-400 hover:bg-white/10">
                          {s.replace(/_/g, ' ')}
                        </button>
                      ))}
                      <button onClick={() => setUpdatingId(null)} className="text-[10px] text-gray-400 hover:text-white ml-1">✕</button>
                    </div>
                  ) : (
                    <button onClick={() => setUpdatingId(r.id)}
                      className="btn-sm px-3 py-1.5 rounded-lg bg-white/5 text-gray-400 text-xs hover:bg-white/10 flex items-center gap-1">
                      <Send className="w-3 h-3" /> Modifier
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
