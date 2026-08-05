import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import { Plus, CalendarCheck, DoorOpen, CheckCircle2, XCircle, Clock, Camera, FileText } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import type { Visit, CreateVisitRequest, UpdateVisitRequest, Soul } from '@/types';

const STATUT_STYLE: Record<string, { label: string; cls: string }> = {
  PLANIFIEE: { label: 'Planifiée', cls: 'bg-sky-500/10 text-sky-600 dark:text-sky-400' },
  REALISEE: { label: 'Réalisée', cls: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' },
  ANNULEE: { label: 'Annulée', cls: 'bg-red-500/10 text-red-600 dark:text-red-400' },
  REPORTEE: { label: 'Reportée', cls: 'bg-amber-500/10 text-amber-600 dark:text-amber-400' },
};

export default function VisitsPage() {
  const { user } = useAuth();
  const isLeader = !!user && (user.roles.includes('ADMIN') || user.roles.includes('PASTEUR')
    || user.roles.includes('RESPONSABLE') || user.roles.includes('CHEF_DE_FAMILLE'));
  const queryClient = useQueryClient();

  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<CreateVisitRequest>({
    soulId: '',
    datePrevue: new Date().toISOString().slice(0, 10),
    motif: 'SUIVI',
    objectif: '',
  });
  const [editForm, setEditForm] = useState<{ id: string; statut: Visit['statut']; compteRendu: string; present: boolean } | null>(null);

  const myVisitsQuery = useQuery({
    queryKey: ['visits', 'my'],
    queryFn: async () => {
      const res = await api.get('/visits/my');
      return res.data as Visit[];
    },
  });

  const upcomingQuery = useQuery({
    queryKey: ['visits', 'upcoming'],
    queryFn: async () => {
      const res = await api.get('/visits/upcoming');
      return res.data as Visit[];
    },
    enabled: isLeader,
  });

  const soulsQuery = useQuery({
    queryKey: ['souls', 'light'],
    queryFn: async () => {
      const res = await api.get('/souls', { params: { size: 100 } });
      return (res.data as { content: Soul[] }).content;
    },
    enabled: showCreate,
  });

  const createMutation = useMutation({
    mutationFn: async (payload: CreateVisitRequest) => {
      const res = await api.post('/visits', payload);
      return res.data as Visit;
    },
    onSuccess: () => {
      toast.success('Visite planifiée');
      setShowCreate(false);
      queryClient.invalidateQueries({ queryKey: ['visits'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, payload }: { id: string; payload: UpdateVisitRequest }) => {
      const res = await api.patch(`/visits/${id}`, payload);
      return res.data as Visit;
    },
    onSuccess: () => {
      toast.success('Visite mise à jour');
      setEditForm(null);
      queryClient.invalidateQueries({ queryKey: ['visits'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const myVisits = myVisitsQuery.data ?? [];
  const upcoming = upcomingQuery.data ?? [];

  return (
    <div className="animate-fade-in space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Visites pastorales</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Planification, compte rendu et suivi de chaque visite
          </p>
        </div>
        <button onClick={() => setShowCreate(v => !v)} className="btn btn-primary flex items-center gap-2">
          <Plus className="w-4 h-4" /> Planifier une visite
        </button>
      </div>

      {showCreate && (
        <div className="glass-card p-4 space-y-4">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Nouvelle visite</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Âme à visiter</label>
              <select
                value={form.soulId}
                onChange={e => setForm({ ...form, soulId: e.target.value })}
                className="input w-full"
              >
                <option value="">— Choisir —</option>
                {(soulsQuery.data ?? []).map(s => (
                  <option key={s.id} value={s.id}>
                    {[s.prenom, s.nom].filter(Boolean).join(' ')}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Date prévue</label>
              <input
                type="date"
                value={form.datePrevue}
                onChange={e => setForm({ ...form, datePrevue: e.target.value })}
                className="input w-full"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Motif</label>
              <select
                value={form.motif}
                onChange={e => setForm({ ...form, motif: e.target.value })}
                className="input w-full"
              >
                <option value="SUIVI">Suivi</option>
                <option value="CONSOLIDATION">Consolidation</option>
                <option value="CONSEIL">Conseil</option>
                <option value="PRIERE">Prière</option>
                <option value="ACCUEIL">Accueil de nouvelle âme</option>
                <option value="AUTRE">Autre</option>
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Objectif</label>
              <input
                type="text"
                placeholder="Objectif de la visite…"
                value={form.objectif}
                onChange={e => setForm({ ...form, objectif: e.target.value })}
                className="input w-full"
              />
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => form.soulId && createMutation.mutate(form)}
              disabled={!form.soulId}
              className="btn btn-primary text-sm"
            >
              Créer
            </button>
            <button onClick={() => setShowCreate(false)} className="btn text-sm">Annuler</button>
          </div>
        </div>
      )}

      {/* Upcoming (vue leader) */}
      {isLeader && upcoming.length > 0 && (
        <div className="glass-card p-4">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2 mb-3">
            <CalendarCheck className="w-4 h-4 text-primary-500" /> Prochaines visites (toutes équipes)
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {upcoming.filter(v => v.statut === 'PLANIFIEE').slice(0, 6).map(v => (
              <div key={v.id} className="flex items-center gap-3 border border-gray-100 dark:border-gray-800 rounded-xl p-3">
                <span className="p-2 rounded-lg bg-sky-500/10 text-sky-500"><Clock className="w-4 h-4" /></span>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{v.soulNom}</p>
                  <p className="text-xs text-gray-400">
                    {new Date(v.datePrevue).toLocaleDateString('fr-FR')} · {v.visiteurNom}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Mes visites */}
      <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
        <div className="px-4 py-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <DoorOpen className="w-4 h-4 text-primary-500" /> Mes visites
          </h2>
          <span className="text-xs text-gray-400">{myVisits.length} visite(s)</span>
        </div>

        {myVisitsQuery.isLoading ? (
          <div className="p-8 text-center text-sm text-gray-400">Chargement…</div>
        ) : myVisits.length === 0 ? (
          <div className="p-12 text-center">
            <DoorOpen className="w-10 h-10 text-gray-300 mx-auto mb-3" />
            <p className="text-sm text-gray-500 dark:text-gray-400">Aucune visite pour le moment.</p>
            <p className="text-xs text-gray-400 mt-1">Planifiez votre première visite de suivi.</p>
          </div>
        ) : (
          myVisits.map(v => {
            const st = STATUT_STYLE[v.statut];
            const isEditing = editForm?.id === v.id;
            return (
              <div key={v.id} className="px-4 py-3 hover:bg-gray-50/60 dark:hover:bg-gray-800/30 transition-colors">
                <div className="flex flex-wrap items-center gap-3">
                  <span className={`inline-flex items-center gap-1 text-[10px] font-bold px-2 py-1 rounded-full ${st.cls}`}>
                    {v.statut === 'REALISEE' ? <CheckCircle2 className="w-3 h-3" /> : v.statut === 'ANNULEE' ? <XCircle className="w-3 h-3" /> : <Clock className="w-3 h-3" />}
                    {st.label}
                  </span>
                  <div className="flex-1 min-w-0">
                    <Link to={`/souls/${v.soulId}`} className="text-sm font-semibold text-gray-900 dark:text-gray-100 hover:text-primary-600 truncate">
                      {v.soulNom ?? 'Âme'}
                    </Link>
                    <p className="text-xs text-gray-400">
                      Prévue le {new Date(v.datePrevue).toLocaleDateString('fr-FR')}
                      {v.dateRealisee && <> · réalisée le {new Date(v.dateRealisee).toLocaleDateString('fr-FR')}</>}
                      {v.motif && <> · {v.motif}</>}
                    </p>
                  </div>
                  {v.statut === 'PLANIFIEE' && (
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setEditForm({ id: v.id, statut: 'REALISEE', compteRendu: v.compteRendu ?? '', present: true })}
                        className="p-2 rounded-lg text-emerald-500 hover:bg-emerald-50 dark:hover:bg-emerald-500/10 transition-colors"
                        title="Remplir le compte rendu et clôturer"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                      </button>
                    </div>
                  )}
                </div>

                {v.objectif && (
                  <p className="mt-2 text-xs text-gray-500 dark:text-gray-400 flex items-start gap-1.5">
                    <FileText className="w-3.5 h-3.5 mt-0.5 text-gray-300 shrink-0" />
                    <span><span className="font-medium">Objectif :</span> {v.objectif}</span>
                  </p>
                )}
                {v.compteRendu && (
                  <p className="mt-1.5 text-xs text-gray-500 dark:text-gray-400 flex items-start gap-1.5">
                    <Camera className="w-3.5 h-3.5 mt-0.5 text-gray-300 shrink-0" />
                    <span>{v.compteRendu}</span>
                  </p>
                )}

                {isEditing && (
                  <div className="mt-3 space-y-2 border-t border-gray-100 dark:border-gray-800 pt-3">
                    <textarea
                      rows={2}
                      placeholder="Compte rendu de la visite…"
                      value={editForm.compteRendu}
                      onChange={e => setEditForm({ ...editForm, compteRendu: e.target.value })}
                      className="input w-full text-sm"
                    />
                    <div className="flex items-center gap-3">
                      <label className="flex items-center gap-2 text-xs text-gray-600 dark:text-gray-400">
                        <input
                          type="checkbox"
                          checked={editForm.present}
                          onChange={e => setEditForm({ ...editForm, present: e.target.checked })}
                          className="rounded"
                        />
                        Présent(e)
                      </label>
                      <div className="flex-1" />
                      <button
                        onClick={() => updateMutation.mutate({ id: editForm.id, payload: { statut: editForm.statut, compteRendu: editForm.compteRendu, present: editForm.present } })}
                        className="btn btn-primary text-sm"
                      >
                        Enregistrer
                      </button>
                      <button onClick={() => setEditForm(null)} className="btn text-sm">Annuler</button>
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
