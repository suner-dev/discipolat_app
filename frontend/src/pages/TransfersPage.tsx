import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import type { TransferRequest, TransferStatus, TransferType } from '@/types';
import { TRANSFER_TYPE_LABELS, TRANSFER_STATUS_LABELS, PRIORITE_LABELS } from '@/types';
import type { ReactNode } from 'react';
import {
  ArrowLeftRight, Plus, Loader2, Send, XCircle, Search, Clock, AlertTriangle, ShieldCheck, FileText,
} from 'lucide-react';

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const STATUS_BADGE: Record<TransferStatus, string> = {
  BROUILLON: 'badge-gray',
  SOUMIS: 'badge-info',
  EN_ATTENTE_VALIDATION: 'badge-warning',
  VALIDATION_PARTIELLE: 'badge-info',
  VALIDE: 'badge-primary',
  REFUSE: 'badge-danger',
  ANNULE: 'badge-gray',
  EXECUTE: 'badge-success',
  ARCHIVE: 'badge-gray',
};

const STATUS_ICON: Record<TransferStatus, ReactNode> = {
  BROUILLON: <FileText className="w-3 h-3" />,
  SOUMIS: <Clock className="w-3 h-3" />,
  EN_ATTENTE_VALIDATION: <Clock className="w-3 h-3" />,
  VALIDATION_PARTIELLE: <ShieldCheck className="w-3 h-3" />,
  VALIDE: <ShieldCheck className="w-3 h-3" />,
  REFUSE: <XCircle className="w-3 h-3" />,
  ANNULE: <XCircle className="w-3 h-3" />,
  EXECUTE: <ArrowLeftRight className="w-3 h-3" />,
  ARCHIVE: <FileText className="w-3 h-3" />,
};

const PRIORITY_STYLE: Record<string, string> = {
  BASSE: 'badge-gray',
  MOYENNE: 'badge-info',
  HAUTE: 'badge-warning',
  URGENTE: 'badge-danger',
};

export default function TransfersPage() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [statut, setStatut] = useState('');
  const [type, setType] = useState('');
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['transfers', statut, type, page],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), size: '20' });
      if (statut) params.set('statut', statut);
      if (type) params.set('type', type);
      const res = await api.get(`/transfers?${params}`);
      return res.data as PageResponse<TransferRequest>;
    },
  });

  const submitMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/transfers/${id}/submit`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transfers'] });
      toast.success('Demande soumise au circuit de validation');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const cancelMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/transfers/${id}/cancel`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transfers'] });
      toast.success('Demande annulée');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const isMine = (t: TransferRequest) => t.demandeurId === user?.id;

  const canSubmit = (t: TransferRequest) =>
    (t.statut === 'BROUILLON' || t.statut === 'SOUMIS') && isMine(t);
  const canCancel = (t: TransferRequest) =>
    ['BROUILLON', 'SOUMIS', 'EN_ATTENTE_VALIDATION', 'VALIDATION_PARTIELLE'].includes(t.statut) && isMine(t);

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg">
            <ArrowLeftRight className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Demandes de transfert</h1>
            <p className="page-subtitle">
              Workflow intelligent et configurable — validations, historiques et exécution automatique
            </p>
          </div>
        </div>
        <Link to="/transfers/new" className="btn-primary btn-sm">
          <Plus className="w-4 h-4" />
          Nouvelle demande
        </Link>
      </div>

      {/* Filtres */}
      <div className="flex flex-wrap gap-2 mb-6">
        <select value={statut} onChange={(e) => { setStatut(e.target.value); setPage(0); }} className="input w-auto">
          <option value="">Tous les statuts</option>
          {Object.entries(TRANSFER_STATUS_LABELS).map(([key, label]) => (
            <option key={key} value={key}>{label}</option>
          ))}
        </select>
        <select value={type} onChange={(e) => { setType(e.target.value); setPage(0); }} className="input w-auto">
          <option value="">Tous les types</option>
          {Object.entries(TRANSFER_TYPE_LABELS).map(([key, label]) => (
            <option key={key} value={key}>{label}</option>
          ))}
        </select>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : !data || data.content.length === 0 ? (
        <div className="glass-card p-14 text-center">
          <Search className="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Aucune demande</h3>
          <p className="text-sm text-gray-500">Créez une demande de transfert pour démarrer un circuit de validation</p>
        </div>
      ) : (
        <div className="space-y-3">
          {data.content.map((t) => (
            <div key={t.id} className="glass-card p-5 hover:shadow-md transition-shadow hover-lift">
              <div className="flex items-start justify-between gap-4">
                <Link to={`/transfers/${t.id}`} className="flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2 mb-1.5">
                    <span className={`badge text-[10px] ${STATUS_BADGE[t.statut]}`}>
                      {STATUS_ICON[t.statut]} {TRANSFER_STATUS_LABELS[t.statut]}
                    </span>
                    <span className={`badge text-[10px] ${PRIORITY_STYLE[t.priorite]}`}>
                      <AlertTriangle className="w-3 h-3" /> {PRIORITE_LABELS[t.priorite]}
                    </span>
                    <span className="text-xs text-gray-400">
                      {new Date(t.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}
                    </span>
                  </div>
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                    {TRANSFER_TYPE_LABELS[t.type]}
                  </p>
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1 mt-1.5 text-xs text-gray-500 dark:text-gray-400">
                    <span><span className="text-gray-400">Personne :</span> <strong className="text-gray-700 dark:text-gray-300">{t.personneNom}</strong></span>
                    {t.ancienneAffectation?.nom && (
                      <>
                        <span className="text-gray-300 dark:text-gray-600">→</span>
                        <span className="line-through decoration-red-400/60">{t.ancienneAffectation.nom}</span>
                      </>
                    )}
                    <span className="text-gray-300 dark:text-gray-600">→</span>
                    <span className="font-medium text-emerald-600 dark:text-emerald-400">{t.nouvelleAffectation.nom}</span>
                  </div>
                  {t.totalEtapes > 0 && (
                    <div className="flex items-center gap-2 mt-2">
                      <div className="flex-1 h-1.5 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden max-w-xs">
                        <div
                          className="h-full bg-gradient-to-r from-emerald-500 to-teal-500 rounded-full transition-all"
                          style={{ width: `${(t.approbationsObtenues / Math.max(t.totalEtapes, 1)) * 100}%` }}
                        />
                      </div>
                      <span className="text-[10px] text-gray-400">{t.approbationsObtenues}/{t.totalEtapes} validations</span>
                    </div>
                  )}
                </Link>
                <div className="flex flex-col items-end gap-2 flex-shrink-0">
                  <Link to={`/transfers/${t.id}`} className="btn-secondary btn-sm">
                    Détail
                  </Link>
                  {canSubmit(t) && (
                    <button
                      onClick={() => submitMutation.mutate(t.id)}
                      disabled={submitMutation.isPending}
                      className="btn-primary btn-sm"
                    >
                      {submitMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                      Soumettre
                    </button>
                  )}
                  {canCancel(t) && (
                    <button
                      onClick={() => { if (confirm('Annuler cette demande ?')) cancelMutation.mutate(t.id); }}
                      className="btn-secondary btn-sm text-red-600 border-red-200 hover:bg-red-50"
                    >
                      <XCircle className="w-4 h-4" /> Annuler
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">Page {data.page + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.page === 0} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.page >= data.totalPages - 1} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}
    </div>
  );
}
