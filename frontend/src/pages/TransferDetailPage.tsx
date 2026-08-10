import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import AttachmentPicker from '@/components/shared/AttachmentPicker';
import AttachmentLinks from '@/components/shared/AttachmentLinks';
import type { TransferDetail, TransferHistoryEntry, DecisionType, TransferStatus } from '@/types';
import { TRANSFER_TYPE_LABELS, TRANSFER_STATUS_LABELS, DECISION_LABELS, PRIORITE_LABELS } from '@/types';
import {
  ArrowLeft, ArrowLeftRight, Loader2, Send, XCircle, CheckCircle2, MessageSquare,
  RefreshCcw, AlertTriangle, Archive, Clock, ShieldCheck, History, Paperclip, User,
} from 'lucide-react';

const STATUS_BADGE: Record<TransferStatus, string> = {
  BROUILLON: 'badge-gray', SOUMIS: 'badge-info', EN_ATTENTE_VALIDATION: 'badge-warning',
  VALIDATION_PARTIELLE: 'badge-info', VALIDE: 'badge-primary', REFUSE: 'badge-danger',
  ANNULE: 'badge-gray', EXECUTE: 'badge-success', ARCHIVE: 'badge-gray',
};

const HISTORY_ACTION_LABELS: Record<string, { label: string; color: string }> = {
  CREATION: { label: 'Demande créée', color: 'bg-emerald-500' },
  MODIFICATION: { label: 'Demande modifiée', color: 'bg-blue-500' },
  SOUMISSION: { label: 'Demande soumise', color: 'bg-blue-500' },
  VALIDATION: { label: 'Validation', color: 'bg-emerald-500' },
  REFUS: { label: 'Refus', color: 'bg-red-500' },
  DEMANDE_INFORMATIONS: { label: 'Informations demandées', color: 'bg-amber-500' },
  RENVOI_CORRECTION: { label: 'Renvoi pour correction', color: 'bg-amber-500' },
  ANNULATION: { label: 'Demande annulée', color: 'bg-gray-500' },
  EXECUTION: { label: 'Transfert exécuté', color: 'bg-teal-500' },
  ECHEC_EXECUTION: { label: 'Échec d\u2019exécution', color: 'bg-red-500' },
  ARCHIVAGE: { label: 'Demande archivée', color: 'bg-gray-500' },
};

export default function TransferDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [modal, setModal] = useState<DecisionType | null>(null);
  const [motivation, setMotivation] = useState('');
  const [editingPieces, setEditingPieces] = useState(false);
  const [pieceIds, setPieceIds] = useState<string[]>([]);

  const { data, isLoading } = useQuery({
    queryKey: ['transfer', id],
    queryFn: async () => {
      const res = await api.get(`/transfers/${id}`);
      return res.data as TransferDetail;
    },
  });

  const { data: history } = useQuery({
    queryKey: ['transfer-history', id],
    queryFn: async () => {
      const res = await api.get(`/transfers/${id}/history`);
      return res.data as TransferHistoryEntry[];
    },
    enabled: !!id,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['transfer', id] });
    queryClient.invalidateQueries({ queryKey: ['transfer-history', id] });
    queryClient.invalidateQueries({ queryKey: ['transfers'] });
  };

  const actionMutation = useMutation({
    mutationFn: async ({ action, body }: { action: string; body?: unknown }) => {
      await api.post(`/transfers/${id}/${action}`, body ?? {});
    },
    onSuccess: () => {
      invalidate();
      toast.success('Opération effectuée avec succès');
      setModal(null);
      setMotivation('');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const piecesMutation = useMutation({
    mutationFn: async (fichierIds: string[]) => {
      await api.put(`/transfers/${id}`, { fichierIds });
    },
    onSuccess: () => {
      invalidate();
      toast.success('Pièces jointes mises à jour');
      setEditingPieces(false);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return <div className="page-container flex justify-center py-16"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>;
  }
  if (!data) {
    return <div className="page-container">Demande introuvable</div>;
  }

  const t = data.transfert;
  const isMine = t.demandeurId === user?.id;
  const estTerminal = ['EXECUTE', 'REFUSE', 'ANNULE', 'ARCHIVE'].includes(t.statut);
  const peutSoumettre = (t.statut === 'BROUILLON' || t.statut === 'SOUMIS') && isMine;
  const peutAnnuler = ['BROUILLON', 'SOUMIS', 'EN_ATTENTE_VALIDATION', 'VALIDATION_PARTIELLE'].includes(t.statut) && isMine;
  const peutModifierPieces = t.statut === 'BROUILLON' && isMine;

  const startEditPieces = () => {
    setPieceIds(data.piecesJointes.map(a => a.fileId));
    setEditingPieces(true);
  };
  const peutArchiver = !estTerminal && t.statut !== 'BROUILLON' && t.statut !== 'SOUMIS'
    && ['ADMIN', 'PASTEUR'].includes(user?.activeRole ?? '');

  const openModal = (d: DecisionType) => {
    setMotivation('');
    setModal(d);
  };

  return (
    <div className="page-container max-w-5xl">
      <div className="page-header">
        <Link to="/transfers" className="btn-ghost btn-sm mb-2">
          <ArrowLeft className="w-4 h-4" />
          Retour aux transferts
        </Link>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg">
              <ArrowLeftRight className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="page-title mb-0">{TRANSFER_TYPE_LABELS[t.type]}</h1>
                <span className={`badge ${STATUS_BADGE[t.statut]}`}>{TRANSFER_STATUS_LABELS[t.statut]}</span>
              </div>
              <p className="page-subtitle">Demande de {t.demandeurNom || '—'} · créée le {new Date(t.createdAt).toLocaleDateString('fr-FR')}</p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            {peutSoumettre && (
              <button onClick={() => actionMutation.mutate({ action: 'submit' })} disabled={actionMutation.isPending} className="btn-primary btn-sm">
                <Send className="w-4 h-4" /> Soumettre
              </button>
            )}
            {peutAnnuler && (
              <button onClick={() => { if (confirm('Annuler cette demande ?')) actionMutation.mutate({ action: 'cancel' }); }} className="btn-secondary btn-sm text-red-600 border-red-200 hover:bg-red-50">
                <XCircle className="w-4 h-4" /> Annuler
              </button>
            )}
            {peutArchiver && (
              <button onClick={() => { if (confirm('Archiver cette demande ?')) actionMutation.mutate({ action: 'archive' }); }} className="btn-secondary btn-sm">
                <Archive className="w-4 h-4" /> Archiver
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Colonne principale */}
        <div className="lg:col-span-2 space-y-6">
          {/* Détails */}
          <div className="glass-card p-6 space-y-4">
            <h3 className="font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <ArrowLeftRight className="w-4 h-4 text-emerald-500" /> Détails du transfert
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-gray-400 mb-1">Personne concernée</p>
                <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{t.personneNom}</p>
              </div>
              <div>
                <p className="text-xs text-gray-400 mb-1">Priorité</p>
                <span className={`badge ${t.priorite === 'URGENTE' ? 'badge-danger' : t.priorite === 'HAUTE' ? 'badge-warning' : 'badge-gray'}`}>
                  {PRIORITE_LABELS[t.priorite]}
                </span>
              </div>
              <div>
                <p className="text-xs text-gray-400 mb-1">Affectation actuelle</p>
                <p className="text-sm font-medium text-gray-800 dark:text-gray-200 line-through decoration-red-400/60">
                  {t.ancienneAffectation?.nom ?? '—'}
                </p>
              </div>
              <div>
                <p className="text-xs text-gray-400 mb-1">Nouvelle affectation</p>
                <p className="text-sm font-semibold text-emerald-600 dark:text-emerald-400">{t.nouvelleAffectation.nom}</p>
              </div>
            </div>
            <div>
              <p className="text-xs text-gray-400 mb-1">Justification</p>
              <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-line">{t.justification}</p>
            </div>
            {t.commentaires && (
              <div>
                <p className="text-xs text-gray-400 mb-1">Commentaires</p>
                <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-line">{t.commentaires}</p>
              </div>
            )}
            {t.delaiLimite && (
              <p className="text-xs text-amber-600 dark:text-amber-400 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5" />
                Traitement attendu avant le {new Date(t.delaiLimite).toLocaleString('fr-FR')}
              </p>
            )}
          </div>

          {/* Pièces jointes */}
          {(data.piecesJointes.length > 0 || editingPieces || peutModifierPieces) && (
            <div className="glass-card p-6">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
                  <Paperclip className="w-4 h-4 text-gray-500" /> Pièces jointes ({data.piecesJointes.length})
                </h3>
                {peutModifierPieces && !editingPieces && (
                  <button onClick={startEditPieces} className="btn-secondary btn-sm">
                    Modifier
                  </button>
                )}
              </div>
              {editingPieces ? (
                <div className="space-y-3">
                  <AttachmentPicker value={pieceIds} onChange={setPieceIds} />
                  <div className="flex justify-end gap-2">
                    <button onClick={() => setEditingPieces(false)} className="btn-secondary btn-sm">Annuler</button>
                    <button
                      onClick={() => piecesMutation.mutate(pieceIds)}
                      disabled={piecesMutation.isPending}
                      className="btn-primary btn-sm"
                    >
                      {piecesMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                      Enregistrer les pièces jointes
                    </button>
                  </div>
                </div>
              ) : data.piecesJointes.length === 0 ? (
                <p className="text-sm text-gray-500">Aucune pièce jointe.</p>
              ) : (
                <AttachmentLinks pieces={data.piecesJointes} />
              )}
            </div>
          )}

          {/* Décisions */}
          {data.decisions.length > 0 && (
            <div className="glass-card p-6">
              <h3 className="font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2 mb-3">
                <ShieldCheck className="w-4 h-4 text-emerald-500" /> Décisions des validateurs
              </h3>
              <ul className="space-y-3">
                {data.decisions.map(d => (
                  <li key={d.id} className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-medium text-gray-800 dark:text-gray-200">
                        {d.validateurNom || '—'}
                        {d.roleValidateur && <span className="ml-2 text-xs badge badge-gray">{d.roleValidateur}</span>}
                      </p>
                      <span className={`badge text-[10px] ${
                        d.decision === 'APPROBATION' ? 'badge-success'
                        : d.decision === 'REFUS' ? 'badge-danger' : 'badge-warning'
                      }`}>{DECISION_LABELS[d.decision]}</span>
                    </div>
                    {d.motivation && <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">« {d.motivation} »</p>}
                    <p className="text-xs text-gray-400 mt-1">{new Date(d.createdAt).toLocaleString('fr-FR')}</p>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        {/* Colonne latérale */}
        <div className="space-y-6">
          {/* Circuit de validation */}
          <div className="glass-card p-6">
            <h3 className="font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2 mb-4">
              <ShieldCheck className="w-4 h-4 text-emerald-500" /> Circuit de validation
            </h3>
            {data.modeValidation && (
              <p className="text-xs text-gray-400 mb-3">
                Mode : {data.modeValidation === 'SEQUENTIEL' ? 'Séquentiel' : data.modeValidation === 'PARALLELE' ? 'Parallèle' : 'N validations requises'}
              </p>
            )}
            {data.etapes.length === 0 ? (
              <p className="text-sm text-gray-500">Aucune validation requise — exécution automatique dès la soumission.</p>
            ) : (
              <ol className="space-y-3">
                {data.etapes.map((s, i) => (
                  <li key={s.id} className="flex gap-3">
                    <div className="flex flex-col items-center">
                      <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold ${
                        s.validee ? 'bg-emerald-500 text-white' : 'bg-gray-200 dark:bg-gray-700 text-gray-500'
                      }`}>
                        {s.validee ? '✓' : i + 1}
                      </div>
                      {i < data.etapes.length - 1 && <div className="w-px flex-1 bg-gray-200 dark:bg-gray-700 my-1" />}
                    </div>
                    <div className="pb-2">
                      <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{s.label}</p>
                      <p className="text-xs text-gray-400">{s.rolesValidateurs.join(' / ')}</p>
                      {s.description && <p className="text-xs text-gray-500 mt-0.5">{s.description}</p>}
                    </div>
                  </li>
                ))}
              </ol>
            )}

            {/* Actions de validation */}
            {(data.peutValider && !estTerminal && t.statut !== 'BROUILLON' && t.statut !== 'SOUMIS') && (
              <div className="mt-4 pt-4 border-t border-gray-100 dark:border-gray-800 grid grid-cols-2 gap-2">
                <button onClick={() => openModal('APPROBATION')} className="btn-primary btn-sm bg-emerald-600 hover:bg-emerald-700">
                  <CheckCircle2 className="w-4 h-4" /> Approuver
                </button>
                <button onClick={() => openModal('REFUS')} className="btn-secondary btn-sm text-red-600 border-red-200 hover:bg-red-50">
                  <XCircle className="w-4 h-4" /> Refuser
                </button>
                <button onClick={() => openModal('DEMANDE_INFORMATIONS')} className="btn-secondary btn-sm">
                  <MessageSquare className="w-4 h-4" /> Infos
                </button>
                <button onClick={() => openModal('RENVOI_CORRECTION')} className="btn-secondary btn-sm">
                  <RefreshCcw className="w-4 h-4" /> Correction
                </button>
              </div>
            )}
          </div>

          {/* Historique */}
          <div className="glass-card p-6">
            <h3 className="font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2 mb-4">
              <History className="w-4 h-4 text-blue-500" /> Historique
            </h3>
            {!history || history.length === 0 ? (
              <p className="text-sm text-gray-500">Aucun événement</p>
            ) : (
              <ol className="space-y-3">
                {[...history].reverse().map(h => {
                  const meta = HISTORY_ACTION_LABELS[h.action] ?? { label: h.action, color: 'bg-gray-500' };
                  return (
                    <li key={h.id} className="flex gap-3">
                      <div className="flex flex-col items-center">
                        <div className={`w-2.5 h-2.5 rounded-full mt-1 ${meta.color}`} />
                      </div>
                      <div className="pb-2">
                        <p className="text-sm font-medium text-gray-800 dark:text-gray-200">{meta.label}</p>
                        <p className="text-xs text-gray-400">
                          {h.utilisateurNom || 'Système'}
                          {h.roleActif ? ` · ${h.roleActif}` : ''} · {new Date(h.createdAt).toLocaleString('fr-FR')}
                        </p>
                        {h.commentaire && <p className="text-xs text-gray-500 mt-0.5">« {h.commentaire} »</p>}
                        {h.nouveauStatut && (
                          <p className="text-[10px] text-gray-400 mt-0.5">
                            {h.ancienStatut ? `${TRANSFER_STATUS_LABELS[h.ancienStatut as TransferStatus]} → ` : ''}
                            {TRANSFER_STATUS_LABELS[h.nouveauStatut as TransferStatus]}
                          </p>
                        )}
                      </div>
                    </li>
                  );
                })}
              </ol>
            )}
          </div>
        </div>
      </div>

      {/* Modal de décision */}
      {modal && (
        <div className="modal-overlay" onClick={() => { setModal(null); setMotivation(''); }}>
          <div className="modal-content max-w-sm" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                {DECISION_LABELS[modal]}
              </h3>
              <button onClick={() => { setModal(null); setMotivation(''); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <XCircle className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {modal === 'APPROBATION' ? 'Validez cette demande pour faire avancer le circuit de validation.'
                  : modal === 'REFUS' ? 'La demande sera définitivement refusée et notifiée au demandeur.'
                  : 'La demande sera renvoyée au demandeur. Expliquez ce qui manque :'}
              </p>
              <div>
                <label className="label">{modal === 'APPROBATION' ? 'Motivation (optionnelle)' : 'Motivation *'}</label>
                <textarea
                  className="input"
                  rows={3}
                  value={motivation}
                  onChange={(e) => setMotivation(e.target.value)}
                  placeholder={modal === 'REFUS' ? 'Raisons du refus...' : modal === 'APPROBATION' ? 'Observations...' : 'Éléments attendus...'}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button onClick={() => { setModal(null); setMotivation(''); }} className="btn-secondary btn-sm">Annuler</button>
              <button
                onClick={() => actionMutation.mutate({ action: 'decide', body: { decision: modal, motivation: motivation || undefined } })}
                disabled={actionMutation.isPending || (modal !== 'APPROBATION' && !motivation.trim())}
                className={`btn-sm ${modal === 'APPROBATION' ? 'btn-primary bg-emerald-600 hover:bg-emerald-700'
                  : modal === 'REFUS' ? 'btn-secondary text-red-600 border-red-200'
                  : 'btn-secondary text-amber-600 border-amber-200'}`}
              >
                {actionMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                {DECISION_LABELS[modal]}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
