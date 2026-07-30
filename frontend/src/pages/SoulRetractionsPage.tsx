import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { Loader2, CheckCircle2, XCircle, MessageSquare, AlertTriangle, Send, Search, ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';

interface RetractionRequest {
  id: string;
  ameId: string;
  demandeurId: string;
  justification: string;
  statut: string;
  traitePar?: string;
  dateTraitement?: string;
  commentaireReponse?: string;
  createdAt: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export default function SoulRetractionsPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState('EN_ATTENTE');
  const [page, setPage] = useState(0);
  const [commentaire, setCommentaire] = useState('');
  const [selectedRequest, setSelectedRequest] = useState<RetractionRequest | null>(null);
  const [action, setAction] = useState<'approve' | 'reject' | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['soul-retractions', filter, page],
    queryFn: async () => {
      const res = await api.get(`/souls/retraction-requests?statut=${filter}&page=${page}&size=20`);
      return res.data as PageResponse<RetractionRequest>;
    },
  });

  const processMutation = useMutation({
    mutationFn: async ({ id, action, commentaire }: { id: string; action: string; commentaire: string }) => {
      await api.patch(`/souls/retraction-request/${id}/${action}`, { commentaire });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['soul-retractions'] });
      toast.success('Demande traitée avec succès');
      setAction(null);
      setSelectedRequest(null);
      setCommentaire('');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const statutLabels: Record<string, string> = {
    EN_ATTENTE: 'En attente',
    APPROUVEE: 'Approuvée',
    REJETEE: 'Rejetée',
  };

  const statutColors: Record<string, string> = {
    EN_ATTENTE: 'badge-warning',
    APPROUVEE: 'badge-success',
    REJETEE: 'badge-danger',
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <Link to="/souls" className="btn-ghost btn-sm mb-2">
          <ArrowLeft className="w-4 h-4" />
          Retour aux âmes
        </Link>
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500 to-red-500 text-white shadow-lg">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Demandes de retrait</h1>
            <p className="page-subtitle">Approuver ou rejeter les demandes de retrait d'âmes</p>
          </div>
        </div>
      </div>

      <div className="flex gap-2 mb-6">
        {['EN_ATTENTE', 'APPROUVEE', 'REJETEE'].map((s) => (
          <button
            key={s}
            onClick={() => { setFilter(s); setPage(0); }}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
              filter === s
                ? 'bg-primary-500 text-white shadow-md'
                : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-200'
            }`}
          >
            {statutLabels[s]}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : !data || data.content.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <Search className="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <p className="text-gray-500">Aucune demande de retrait</p>
        </div>
      ) : (
        <div className="space-y-3">
          {data.content.map((req) => (
            <div key={req.id} className="glass-card p-5 hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={statutColors[req.statut]}>{statutLabels[req.statut]}</span>
                    <span className="text-xs text-gray-400">{new Date(req.createdAt).toLocaleDateString('fr-FR')}</span>
                  </div>
                  <p className="text-sm text-gray-800 dark:text-gray-200 mt-2">{req.justification}</p>
                  {req.commentaireReponse && (
                    <div className="mt-2 p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                      <p className="text-xs text-gray-500 mb-1">Réponse :</p>
                      <p className="text-sm text-gray-700 dark:text-gray-300">{req.commentaireReponse}</p>
                    </div>
                  )}
                </div>
                {req.statut === 'EN_ATTENTE' && (
                  <div className="flex gap-2 ml-4">
                    <button
                      onClick={() => { setSelectedRequest(req); setAction('approve'); }}
                      className="btn-primary btn-sm bg-green-600 hover:bg-green-700"
                    >
                      <CheckCircle2 className="w-4 h-4" /> Approuver
                    </button>
                    <button
                      onClick={() => { setSelectedRequest(req); setAction('reject'); }}
                      className="btn-secondary btn-sm text-red-600 border-red-200 hover:bg-red-50"
                    >
                      <XCircle className="w-4 h-4" /> Rejeter
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">{data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {action && selectedRequest && (
        <div className="modal-overlay" onClick={() => { setAction(null); setSelectedRequest(null); setCommentaire(''); }}>
          <div className="modal-content max-w-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                {action === 'approve' ? 'Approuver' : 'Rejeter'} la demande
              </h3>
              <button onClick={() => { setAction(null); setSelectedRequest(null); setCommentaire(''); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <XCircle className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">{selectedRequest.justification}</p>
              <div>
                <label className="label">Commentaire (optionnel)</label>
                <div className="relative">
                  <MessageSquare className="absolute left-3 top-3 w-4 h-4 text-gray-400" />
                  <textarea className="input pl-10" rows={3} value={commentaire}
                    onChange={(e) => setCommentaire(e.target.value)}
                    placeholder="Ajouter un commentaire..." />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button onClick={() => { setAction(null); setSelectedRequest(null); setCommentaire(''); }} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => processMutation.mutate({ id: selectedRequest.id, action, commentaire })}
                disabled={processMutation.isPending}
                className={`btn-sm ${action === 'approve' ? 'btn-primary bg-green-600 hover:bg-green-700' : 'btn-secondary text-red-600 border-red-200'}`}
              >
                {processMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                {action === 'approve' ? 'Approuver' : 'Rejeter'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
