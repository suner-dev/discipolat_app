import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Target,
  Plus,
  Trash2,
  Trophy,
  Phone,
  UserPlus,
  Activity,
  Church,
  HandHeart,
  CalendarCheck,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useDictionaries } from '@/hooks/useDictionaries';
import type {
  Objective,
  ObjectiveProgress,
  ObjectiveType,
  ObjectivePeriode,
  CreateObjectiveRequest,
  UserRole,
} from '@/types';

const TYPE_META: Record<ObjectiveType, { label: string; icon: React.ReactNode; color: string }> = {
  VISITES: { label: 'Visites', icon: <Phone className="w-4 h-4" />, color: 'text-sky-500 bg-sky-500/10' },
  NOUVELLES_AMES: { label: 'Nouvelles âmes', icon: <UserPlus className="w-4 h-4" />, color: 'text-emerald-500 bg-emerald-500/10' },
  DISCIPLES_ACTIFS: { label: 'Disciples actifs', icon: <Activity className="w-4 h-4" />, color: 'text-primary-500 bg-primary-500/10' },
  EVANGELISATION: { label: 'Évangélisation', icon: <Church className="w-4 h-4" />, color: 'text-violet-500 bg-violet-500/10' },
  SUIVIS: { label: 'Suivis', icon: <HandHeart className="w-4 h-4" />, color: 'text-amber-500 bg-amber-500/10' },
  PRESENCE: { label: 'Présence', icon: <CalendarCheck className="w-4 h-4" />, color: 'text-rose-500 bg-rose-500/10' },
};

const PERIODE_LABEL: Record<ObjectivePeriode, string> = {
  MENSUEL: 'Mensuel',
  TRIMESTRIEL: 'Trimestriel',
  ANNUEL: 'Annuel',
};

const ROLES: UserRole[] = ['FAISEUR', 'CHEF_DE_FAMILLE', 'RESPONSABLE', 'PASTEUR'];

export default function ObjectivesPage() {
  const { user } = useAuth();
  const dictionaries = useDictionaries();
  const isAdmin = !!user && (user.roles.includes('ADMIN') || user.roles.includes('PASTEUR'));
  const queryClient = useQueryClient();

  /** Libellé d'un rôle (dictionnaire USER_ROLE, repli sur le code). */
  const roleLabel = (r: string) => dictionaries.label('USER_ROLE', r) || r.replace(/_/g, ' ');

  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<CreateObjectiveRequest>({
    role: 'FAISEUR',
    type: 'VISITES',
    cible: 10,
    periode: 'MENSUEL',
  });

  const progressQuery = useQuery({
    queryKey: ['objectives', 'my-progress'],
    queryFn: async () => {
      const res = await api.get('/objectives/my-progress');
      return res.data as ObjectiveProgress[];
    },
  });

  const allQuery = useQuery({
    queryKey: ['objectives', 'all'],
    queryFn: async () => {
      const res = await api.get('/objectives');
      return res.data as Objective[];
    },
    enabled: isAdmin,
  });

  const createMutation = useMutation({
    mutationFn: async (payload: CreateObjectiveRequest) => {
      const res = await api.post('/objectives', payload);
      return res.data as Objective;
    },
    onSuccess: () => {
      toast.success('Objectif créé');
      setShowCreate(false);
      queryClient.invalidateQueries({ queryKey: ['objectives'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/objectives/${id}`);
    },
    onSuccess: () => {
      toast.success('Objectif supprimé');
      queryClient.invalidateQueries({ queryKey: ['objectives'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const progress = progressQuery.data ?? [];

  return (
    <div className="animate-fade-in space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Système d'objectifs</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Objectifs chiffrés par rôle, mesurés automatiquement sur votre périmètre réel
          </p>
        </div>
        {isAdmin && (
          <button onClick={() => setShowCreate(v => !v)} className="btn btn-primary flex items-center gap-2">
            <Plus className="w-4 h-4" /> Nouvel objectif
          </button>
        )}
      </div>

      {showCreate && isAdmin && (
        <div className="glass-card p-4 space-y-4">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Créer un objectif</h2>
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Rôle</label>
              <select
                value={form.role}
                onChange={e => setForm({ ...form, role: e.target.value as UserRole })}
                className="input w-full"
              >
                {ROLES.map(r => <option key={r} value={r}>{roleLabel(r)}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Type</label>
              <select
                value={form.type}
                onChange={e => setForm({ ...form, type: e.target.value as ObjectiveType })}
                className="input w-full"
              >
                {Object.entries(TYPE_META).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Cible</label>
              <input
                type="number"
                min={1}
                value={form.cible}
                onChange={e => setForm({ ...form, cible: Number(e.target.value) })}
                className="input w-full"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Période</label>
              <select
                value={form.periode}
                onChange={e => setForm({ ...form, periode: e.target.value as ObjectivePeriode })}
                className="input w-full"
              >
                {Object.entries(PERIODE_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => createMutation.mutate(form)}
              disabled={form.cible < 1}
              className="btn btn-primary text-sm"
            >
              Créer
            </button>
            <button onClick={() => setShowCreate(false)} className="btn text-sm">Annuler</button>
          </div>
        </div>
      )}

      {/* Progression */}
      {progressQuery.isLoading ? (
        <div className="glass-card p-8 text-center text-sm text-gray-400">Chargement…</div>
      ) : progress.length === 0 ? (
        <div className="glass-card p-12 text-center">
          <Trophy className="w-10 h-10 text-gray-300 mx-auto mb-3" />
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Aucun objectif défini pour votre rôle actif.
          </p>
          {isAdmin && (
            <p className="text-xs text-gray-400 mt-1">Créez un objectif avec le bouton « Nouvel objectif ».</p>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {progress.map(obj => {
            const meta = TYPE_META[obj.type];
            return (
              <div key={obj.id} className="glass-card p-5 space-y-3">
                <div className="flex items-start justify-between">
                  <div className={`p-2 rounded-lg ${meta.color}`}>{meta.icon}</div>
                  {obj.atteint ? (
                    <span className="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-600 bg-emerald-500/10 px-2 py-1 rounded-full">
                      <Trophy className="w-3 h-3" /> ATTEINT
                    </span>
                  ) : (
                    <span className="text-[10px] font-medium text-gray-400 bg-gray-100 dark:bg-gray-800 px-2 py-1 rounded-full">
                      {PERIODE_LABEL[obj.periode]}
                    </span>
                  )}
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{meta.label}</h3>
                  <p className="text-xs text-gray-400 mt-0.5">
                    {obj.realise} / {obj.cible} réalisé
                  </p>
                </div>
                <div>
                  <div className="h-2 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all ${obj.atteint ? 'bg-emerald-500' : 'bg-primary-500'}`}
                      style={{ width: `${Math.min(100, obj.taux)}%` }}
                    />
                  </div>
                  <p className="text-right text-xs font-bold text-gray-500 dark:text-gray-400 mt-1">{obj.taux}%</p>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Admin: all objectives */}
      {isAdmin && allQuery.data && allQuery.data.length > 0 && (
        <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
          <div className="px-4 py-3 flex items-center gap-2">
            <Target className="w-4 h-4 text-primary-500" />
            <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Tous les objectifs</h2>
          </div>
          {allQuery.data.map(o => (
            <div key={o.id} className="px-4 py-3 flex items-center justify-between gap-3">
              <div className="flex items-center gap-3 min-w-0">
                <span className={`p-1.5 rounded-lg ${TYPE_META[o.type].color}`}>{TYPE_META[o.type].icon}</span>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                    {TYPE_META[o.type].label}
                  </p>
                  <p className="text-xs text-gray-400">
                    {roleLabel(o.role)} · {PERIODE_LABEL[o.periode]} · cible {o.cible}
                  </p>
                </div>
              </div>
              <button
                onClick={() => deleteMutation.mutate(o.id)}
                className="p-2 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
                title="Supprimer"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
