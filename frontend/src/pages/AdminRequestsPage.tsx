import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Shield, Loader2, CheckCircle2, XCircle, Users, BarChart3 } from 'lucide-react';
import toast from 'react-hot-toast';

interface AdminRequest {
  id: string;
  type: string;
  requesterName?: string;
  requesterEmail?: string;
  message?: string;
  status: string;
  createdAt: string;
  processedAt?: string;
}

interface AdminDemoRequest {
  id: string;
  churchName: string;
  contactName: string;
  contactEmail: string;
  status: string;
  createdAt: string;
}

interface Stats {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
}

const STATUS_STYLE: Record<string, string> = {
  PENDING: 'text-yellow-400 bg-yellow-500/20',
  APPROVED: 'text-green-400 bg-green-500/20',
  REJECTED: 'text-red-400 bg-red-500/20',
  PROCESSED: 'text-blue-400 bg-blue-500/20',
};

export default function AdminRequestsPage() {
  const qc = useQueryClient();
  const [tab, setTab] = useState<'requests' | 'demo'>('requests');

  const { data: requests = [], isLoading } = useQuery({
    queryKey: ['admin-requests'],
    queryFn: async () => (await api.get('/admin-requests')).data as AdminRequest[],
  });

  const { data: stats } = useQuery({
    queryKey: ['admin-requests-stats'],
    queryFn: async () => (await api.get('/admin-requests/stats')).data as Stats,
  });

  const { data: demoRequests = [], isLoading: loadingDemo } = useQuery({
    queryKey: ['admin-demo-requests'],
    queryFn: async () => (await api.get('/admin-demo-requests')).data as AdminDemoRequest[],
    enabled: tab === 'demo',
  });

  const processMutation = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: string }) =>
      api.post(`/admin-requests/${id}/process`, { action }),
    onSuccess: () => { toast.success('Demande traitée'); qc.invalidateQueries({ queryKey: ['admin-requests'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-700 text-white shadow-lg">
          <Shield className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Demandes administratives</h1>
          <p className="page-subtitle">Gestion des demandes de démos et d'adhésion</p>
        </div>
      </div>

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Total', value: stats.total, icon: BarChart3 },
            { label: 'En attente', value: stats.pending, icon: Loader2 },
            { label: 'Approuvées', value: stats.approved, icon: CheckCircle2 },
            { label: 'Rejetées', value: stats.rejected, icon: XCircle },
          ].map(({ label, value, icon: Icon }) => (
            <div key={label} className="stat-card">
              <Icon className="w-5 h-5 text-primary-500 opacity-80" />
              <p className="stat-value">{value}</p>
              <p className="stat-label">{label}</p>
            </div>
          ))}
        </div>
      )}

      <div className="flex gap-2 mb-6">
        <button onClick={() => setTab('requests')}
          className={`btn-sm px-4 py-2 rounded-lg ${tab === 'requests' ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-md' : 'glass-card hover:shadow-md'}`}>
          Demandes ({requests.length})
        </button>
        <button onClick={() => setTab('demo')}
          className={`btn-sm px-4 py-2 rounded-lg ${tab === 'demo' ? 'bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-md' : 'glass-card hover:shadow-md'}`}>
          Démos ({demoRequests.length})
        </button>
      </div>

      {tab === 'requests' ? (
        isLoading ? (
          <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
        ) : requests.length === 0 ? (
          <div className="glass-card p-10 text-center text-gray-500">Aucune demande administrative</div>
        ) : (
          <div className="space-y-3">
            {requests.map((r) => (
              <div key={r.id} className="glass-card p-5">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[r.status] ?? 'text-gray-400 bg-gray-500/20'}`}>{r.status}</span>
                      <span className="text-xs text-gray-400">{r.type}</span>
                    </div>
                    <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{r.requesterName ?? 'Utilisateur inconnu'}</p>
                    {r.message && <p className="text-xs text-gray-500 mt-1">{r.message}</p>}
                    <p className="text-[11px] text-gray-500 mt-1">{new Date(r.createdAt).toLocaleDateString('fr-FR')}</p>
                  </div>
                  {r.status === 'PENDING' && (
                    <div className="flex gap-2 ml-4">
                      <button onClick={() => processMutation.mutate({ id: r.id, action: 'APPROVE' })}
                        disabled={processMutation.isPending}
                        className="btn-sm px-3 py-1.5 rounded-lg bg-green-600 text-white text-xs hover:bg-green-700 flex items-center gap-1">
                        {processMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle2 className="w-3 h-3" />} Approuver
                      </button>
                      <button onClick={() => processMutation.mutate({ id: r.id, action: 'REJECT' })}
                        disabled={processMutation.isPending}
                        className="btn-sm px-3 py-1.5 rounded-lg bg-red-600 text-white text-xs hover:bg-red-700 flex items-center gap-1">
                        <XCircle className="w-3 h-3" /> Rejeter
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        loadingDemo ? (
          <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
        ) : demoRequests.length === 0 ? (
          <div className="glass-card p-10 text-center text-gray-500">Aucune demande de démo</div>
        ) : (
          <div className="space-y-3">
            {demoRequests.map((d) => (
              <div key={d.id} className="glass-card p-5">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <Users className="w-4 h-4 text-indigo-400" />
                      <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{d.churchName}</span>
                    </div>
                    <p className="text-xs text-gray-500">{d.contactName} — {d.contactEmail}</p>
                    <p className="text-[11px] text-gray-500 mt-1">{new Date(d.createdAt).toLocaleDateString('fr-FR')}</p>
                  </div>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[d.status] ?? 'text-gray-400 bg-gray-500/20'}`}>{d.status}</span>
                </div>
              </div>
            ))}
          </div>
        )
      )}
    </div>
  );
}
