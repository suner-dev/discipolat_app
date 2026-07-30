import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Loader2, AlertTriangle, CheckCircle2, Search, Clock, MessageSquare } from 'lucide-react';
import toast from 'react-hot-toast';

interface AidRequest {
  reportId: string;
  faiseurId: string;
  ameId: string;
  semaine: string;
  demande: string;
  dateSoumission?: string;
  traite: boolean;
}

export default function UrgentAidPage() {
  const queryClient = useQueryClient();
  const [showTreated, setShowTreated] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['urgent-aid', showTreated],
    queryFn: async () => {
      const res = await api.get(`/reports/maker-weekly/urgent-aid?traite=${showTreated}`);
      return res.data as AidRequest[];
    },
  });

  const markTreatedMutation = useMutation({
    mutationFn: async (reportId: string) => {
      await api.patch(`/reports/maker-weekly/urgent-aid/${reportId}/mark-treated`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['urgent-aid'] });
      toast.success('Demande marquée comme traitée');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const untreated = (data || []).filter(r => !r.traite);
  const untreatCount = untreated.length;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-red-500 to-rose-500 text-white shadow-lg">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Demandes d'aide urgentes</h1>
            <p className="page-subtitle">
              {untreatCount > 0
                ? `${untreatCount} demande${untreatCount > 1 ? 's' : ''} en attente`
                : 'Aucune demande en attente'}
            </p>
          </div>
        </div>
      </div>

      <div className="flex gap-2 mb-6">
        <button
          onClick={() => setShowTreated(false)}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${!showTreated ? 'bg-red-500 text-white shadow-md' : 'bg-gray-100 dark:bg-gray-800 text-gray-600'}`}
        >
          En attente {untreatCount > 0 && <span className="ml-1.5 px-1.5 py-0.5 rounded-full bg-white/20 text-xs">{untreatCount}</span>}
        </button>
        <button
          onClick={() => setShowTreated(true)}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${showTreated ? 'bg-primary-500 text-white shadow-md' : 'bg-gray-100 dark:bg-gray-800 text-gray-600'}`}
        >
          Traitées
        </button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : !data || data.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <Search className="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <p className="text-gray-500">Aucune demande d'aide</p>
        </div>
      ) : (
        <div className="space-y-3">
          {data.map((req) => (
            <div key={req.reportId} className={`glass-card p-5 ${!req.traite ? 'border-l-[3px] border-l-red-500' : ''}`}>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    {!req.traite ? (
                      <span className="badge-danger text-xs">Urgent</span>
                    ) : (
                      <span className="badge-success text-xs">Traité</span>
                    )}
                    {req.dateSoumission && (
                      <span className="flex items-center gap-1 text-xs text-gray-400">
                        <Clock className="w-3 h-3" />
                        {new Date(req.dateSoumission).toLocaleDateString('fr-FR')}
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-800 dark:text-gray-200 mt-2">{req.demande}</p>
                </div>
                {!req.traite && (
                  <button
                    onClick={() => markTreatedMutation.mutate(req.reportId)}
                    disabled={markTreatedMutation.isPending}
                    className="btn-primary btn-sm bg-green-600 hover:bg-green-700 ml-4"
                  >
                    {markTreatedMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                    Marquer traitée
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
