import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { PlaneTakeoff, Loader2, CheckCircle2, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { useI18n } from '@/i18n';

interface LeaveRequest {
  id: string;
  requesterName?: string;
  type: string;
  startDate: string;
  endDate: string;
  reason?: string;
  status: string;
  createdAt: string;
}

const STATUS_STYLE: Record<string, string> = {
  PENDING: 'text-yellow-400 bg-yellow-500/20',
  APPROVED: 'text-green-400 bg-green-500/20',
  REJECTED: 'text-red-400 bg-red-500/20',
};

export default function LeaveRequestsPage() {
  const { locale } = useI18n();
  const qc = useQueryClient();

  const { data: requests = [], isLoading } = useQuery({
    queryKey: ['leave-requests'],
    queryFn: async () => (await api.get('/leave-requests')).data as LeaveRequest[],
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, status }: { id: string; status: string }) =>
      api.patch(`/leave-requests/${id}/${status === 'APPROVED' ? 'approve' : 'reject'}`),
    onSuccess: () => { toast.success('Demande mise à jour'); qc.invalidateQueries({ queryKey: ['leave-requests'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const pending = requests.filter((r) => r.status === 'PENDING');
  const processed = requests.filter((r) => r.status !== 'PENDING');

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-sky-500 to-blue-600 text-white shadow-lg">
          <PlaneTakeoff className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Demandes de congé</h1>
          <p className="page-subtitle">Gestion des absences et congés du personnel</p>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total', value: requests.length, color: 'text-blue-600' },
          { label: 'En attente', value: pending.length, color: 'text-yellow-600' },
          { label: 'Traitées', value: processed.length, color: 'text-green-600' },
        ].map((s) => (
          <div key={s.label} className="stat-card">
            <p className={`stat-value ${s.color}`}>{s.value}</p>
            <p className="stat-label">{s.label}</p>
          </div>
        ))}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : requests.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">Aucune demande de congé</div>
      ) : (
        <div className="space-y-3">
          {pending.length > 0 && (
            <>
              <h2 className="text-sm font-semibold text-gray-400 mb-2">En attente ({pending.length})</h2>
              {pending.map((r) => (
                <div key={r.id} className="glass-card p-5 border-l-[3px] border-l-yellow-500">
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="px-2 py-0.5 rounded-full text-xs font-medium text-yellow-400 bg-yellow-500/20">En attente</span>
                        <span className="text-xs text-gray-400">{r.type}</span>
                      </div>
                      <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{r.requesterName ?? 'Membre inconnu'}</p>
                      <div className="flex gap-4 mt-1 text-xs text-gray-500">
                        <span>Du {new Date(r.startDate).toLocaleDateString(locale)}</span>
                        <span>Au {new Date(r.endDate).toLocaleDateString(locale)}</span>
                      </div>
                      {r.reason && <p className="text-xs text-gray-500 mt-1">{r.reason}</p>}
                    </div>
                    <div className="flex gap-2 ml-4">
                      <button onClick={() => updateMutation.mutate({ id: r.id, status: 'APPROVED' })} disabled={updateMutation.isPending}
                        className="btn-sm px-3 py-1.5 rounded-lg bg-green-600 text-white text-xs hover:bg-green-700 flex items-center gap-1">
                        {updateMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle2 className="w-3 h-3" />} Approuver
                      </button>
                      <button onClick={() => updateMutation.mutate({ id: r.id, status: 'REJECTED' })} disabled={updateMutation.isPending}
                        className="btn-sm px-3 py-1.5 rounded-lg bg-red-600 text-white text-xs hover:bg-red-700 flex items-center gap-1">
                        <XCircle className="w-3 h-3" /> Rejeter
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </>
          )}

          {processed.length > 0 && (
            <>
              <h2 className="text-sm font-semibold text-gray-400 mt-6 mb-2">Traitées ({processed.length})</h2>
              {processed.map((r) => (
                <div key={r.id} className="glass-card p-5 opacity-70">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLE[r.status]}`}>{r.status}</span>
                        <span className="text-xs text-gray-400">{r.type}</span>
                      </div>
                      <p className="text-sm text-gray-800 dark:text-gray-200">{r.requesterName ?? 'Membre inconnu'}</p>
                      <div className="flex gap-4 mt-1 text-xs text-gray-500">
                        <span>Du {new Date(r.startDate).toLocaleDateString(locale)}</span>
                        <span>Au {new Date(r.endDate).toLocaleDateString(locale)}</span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  );
}
