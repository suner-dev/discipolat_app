import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { ParallelFollowup, Soul, PageResponse } from '@/types';
import type { ColumnDef } from '@/types/table';
import { Activity, Plus, Loader2, X, UserPlus, Calendar } from 'lucide-react';
import toast from 'react-hot-toast';

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const RAISON_FALLBACK: Record<string, string> = {
  TRANSFERT_EN_COURS: 'Transfert en cours',
  RENFORT: 'Renfort',
  VISITE: 'Visite',
  REPRISE_CONTACT: 'Reprise de contact',
  AUTRE: 'Autre',
};

export default function ParallelFollowupsPage() {
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();
  const [page, setPage] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    ameId: '',
    initiateurId: '',
    raison: 'AUTRE' as const,
    raisonDetail: '',
  });

  const { data, isLoading } = useQuery({
    queryKey: ['parallel-followups', page],
    queryFn: async () => {
      const res = await api.get(`/parallel-followups?size=20&page=${page}`);
      return res.data as PageResponse<ParallelFollowup>;
    },
  });

  const { data: souls } = useQuery({
    queryKey: ['souls', 'all'],
    queryFn: async () => {
      const res = await api.get('/souls?size=100');
      return res.data.content as Soul[];
    },
  });

  const raisonEntries = useMemo(() => {
    const configured = dictionaries.options('FOLLOWUP_RAISON');
    return configured.length > 0
      ? configured.map((e) => ({ code: e.code, label: e.label }))
      : Object.entries(RAISON_FALLBACK).map(([code, label]) => ({ code, label }));
  }, [dictionaries]);

  const raisonLabel = (code: string) =>
    dictionaries.label('FOLLOWUP_RAISON', code) || RAISON_FALLBACK[code] || code;

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post('/parallel-followups', formData);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['parallel-followups'] });
      toast.success('Suivi parallèle créé avec succès');
      setShowModal(false);
      setFormData({ ameId: '', initiateurId: '', raison: 'AUTRE', raisonDetail: '' });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const columns: ColumnDef<ParallelFollowup>[] = [
    {
      header: 'Âme',
      cell: (f) => (
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center">
            <span className="text-xs font-bold text-primary-600 dark:text-primary-400">{f.ameId.slice(0, 2).toUpperCase()}</span>
          </div>
          <span className="text-sm font-medium text-gray-900 dark:text-gray-100">{f.ameId.slice(0, 8)}...</span>
        </div>
      ),
    },
    {
      header: 'Raison',
      cell: (f) => (
        <span className="badge-info">{raisonLabel(f.raison)}</span>
      ),
    },
    {
      header: 'Début',
      cell: (f) => (
        <span className="text-sm text-gray-500 dark:text-gray-400 flex items-center gap-1">
          <Calendar className="w-3 h-3" />
          {new Date(f.dateDebut).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}
        </span>
      ),
    },
    {
      header: 'Statut',
      cell: (f) => (
        <span className={`inline-flex items-center gap-1.5 ${f.statut === 'EN_COURS' ? 'badge-warning' : 'badge-gray'}`}>
          <span className={`w-1.5 h-1.5 rounded-full ${f.statut === 'EN_COURS' ? 'bg-amber-500 animate-pulse-soft' : 'bg-gray-400'}`} />
          {f.statut === 'EN_COURS' ? 'En cours' : 'Clôturé'}
        </span>
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Activity className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Suivis parallèles</h1>
          </div>
          <p className="page-subtitle">Accompagnements hors périmètre formel</p>
        </div>
        <button onClick={() => setShowModal(true)} className="btn-primary btn-sm animate-scale-in">
          <Plus className="w-4 h-4" /> Nouveau suivi
        </button>
      </div>

      <DataTable<ParallelFollowup>
        columns={columns}
        data={data?.content || []}
        isLoading={isLoading}
        emptyMessage="Aucun suivi parallèle"
        emptyIcon={<Activity className="w-16 h-16 text-gray-300 dark:text-gray-600" />}
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">{data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {/* Create modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white">
                  <UserPlus className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Nouveau suivi parallèle</h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Accompagnement hors périmètre formel</p>
                </div>
              </div>
              <button onClick={() => setShowModal(false)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>

            <div className="modal-body space-y-4">
              <div>
                <label className="label">Âme concernée *</label>
                <select className="input" value={formData.ameId} onChange={(e) => setFormData({ ...formData, ameId: e.target.value })}>
                  <option value="">Sélectionner une âme...</option>
                  {souls?.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.prenom ? `${s.prenom} ${s.nom}` : s.nom} ({s.typeDisciple === 'NOUVEAU_CONVERTI' ? 'Converti' : 'Arrivant'})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Raison</label>
                <select className="input" value={formData.raison} onChange={(e) => setFormData({ ...formData, raison: e.target.value as any })}>
                  {raisonEntries.map((o) => (
                    <option key={o.code} value={o.code}>{o.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Détail (optionnel)</label>
                <textarea className="input" rows={3} value={formData.raisonDetail} onChange={(e) => setFormData({ ...formData, raisonDetail: e.target.value })} placeholder="Informations complémentaires..." />
              </div>
            </div>

            <div className="modal-footer">
              <button onClick={() => setShowModal(false)} className="btn-secondary btn-sm">Annuler</button>
              <button onClick={() => createMutation.mutate()} disabled={!formData.ameId || createMutation.isPending} className="btn-primary btn-sm">
                {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                Créer le suivi
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
