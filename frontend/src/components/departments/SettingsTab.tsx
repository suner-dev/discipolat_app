import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  Settings, AlertTriangle, Activity, CalendarDays, Save, Loader2,
} from 'lucide-react';

export function SettingsTab({ deptId }: { deptId: string }) {
  const queryClient = useQueryClient();
  const [absenceSeuil, setAbsenceSeuil] = useState(2);
  const [absencePeriode, setAbsencePeriode] = useState(3);
  const [inactiviteMois, setInactiviteMois] = useState(3);
  const [tacheRetardAlerte, setTacheRetardAlerte] = useState(true);
  const [eventRappelJours, setEventRappelJours] = useState(1);
  const [loaded, setLoaded] = useState(false);

  const { isLoading } = useQuery({
    queryKey: ['department', deptId, 'settings'],
    queryFn: async () => {
      const res = await api.get(`/departments/${deptId}/settings`);
      const s = res.data ?? {};
      setAbsenceSeuil(s.absenceSeuil ?? 2);
      setAbsencePeriode(s.absencePeriode ?? 3);
      setInactiviteMois(s.inactiviteMois ?? 3);
      setTacheRetardAlerte(s.tacheRetardAlerte ?? true);
      setEventRappelJours(s.eventRappelJours ?? 1);
      setLoaded(true);
      return s;
    },
    enabled: !!deptId,
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      await api.put(`/departments/${deptId}/settings`, {
        absenceSeuil, absencePeriode, inactiviteMois, tacheRetardAlerte, eventRappelJours,
      });
    },
    onSuccess: () => {
      toast.success('Seuils d\'alertes enregistrés ✅');
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'settings'] });
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'alerts'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const num = (v: string) => {
    const n = Number(v);
    return Number.isNaN(n) ? 0 : n;
  };

  return (
    <div className="glass-card p-5">
      <div className="flex items-center gap-2 mb-1">
        <Settings className="w-4 h-4 text-primary-500" />
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Paramètres du département</h3>
      </div>
      <p className="text-xs text-gray-400 mb-4">
        Seuils des alertes intelligentes — aucun paramètre n'est codé en dur : les règles
        lisent ces valeurs. Modifiez-les puis enregistrez.
      </p>

      {isLoading && !loaded ? (
        <div className="flex justify-center py-8"><Loader2 className="w-5 h-5 animate-spin text-primary-500" /></div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40">
            <div className="flex items-center gap-2 mb-1">
              <AlertTriangle className="w-4 h-4 text-amber-500" />
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Absences répétées</p>
            </div>
            <p className="text-[11px] text-gray-400 mb-3">
              Alerte HAUTE quand un membre est absent au moins N fois sur la période (en semaines).
            </p>
            <div className="flex gap-3">
              <div className="flex-1">
                <label className="label" htmlFor="absence-seuil">Absences requises (1–10)</label>
                <input id="absence-seuil" type="number" min={1} max={10} className="input" value={absenceSeuil}
                  onChange={(e) => setAbsenceSeuil(num(e.target.value))} />
              </div>
              <div className="flex-1">
                <label className="label" htmlFor="absence-periode">Période en semaines (1–12)</label>
                <input id="absence-periode" type="number" min={1} max={12} className="input" value={absencePeriode}
                  onChange={(e) => setAbsencePeriode(num(e.target.value))} />
              </div>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40">
            <div className="flex items-center gap-2 mb-1">
              <Activity className="w-4 h-4 text-emerald-500" />
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Inactivité</p>
            </div>
            <p className="text-[11px] text-gray-400 mb-3">
              Alerte MOYENNE quand un membre n'a aucune fiche de présence depuis N mois (0 = désactivé).
            </p>
            <div className="flex-1">
              <label className="label" htmlFor="inactivite-mois">Mois sans présence (0–24)</label>
              <input id="inactivite-mois" type="number" min={0} max={24} className="input" value={inactiviteMois}
                onChange={(e) => setInactiviteMois(num(e.target.value))} />
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 sm:col-span-2">
            <label className="flex items-center gap-3 cursor-pointer">
              <input type="checkbox" checked={tacheRetardAlerte}
                onChange={(e) => setTacheRetardAlerte(e.target.checked)}
                className="w-4 h-4 accent-amber-500 cursor-pointer" />
              <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                Activer l'alerte « tâche en retard »
              </span>
            </label>
            <p className="text-[11px] text-gray-400 mt-1">
              Alerte MOYENNE quand une tâche affectée dépasse son échéance.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/40 sm:col-span-2">
            <div className="flex items-center gap-2 mb-1">
              <CalendarDays className="w-4 h-4 text-indigo-500" />
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">Rappel automatique des événements</p>
            </div>
            <p className="text-[11px] text-gray-400 mb-3">
              Notifie le responsable N jours avant chaque événement rattaché au département
              (0 = rappel désactivé). En plus du rappel J-1 envoyé aux inscrits.
            </p>
            <div className="flex-1">
              <label className="label" htmlFor="event-rappel-jours">Jours avant l'événement (0–30)</label>
              <input id="event-rappel-jours" type="number" min={0} max={30} className="input" value={eventRappelJours}
                onChange={(e) => setEventRappelJours(num(e.target.value))} />
            </div>
          </div>
        </div>
      )}

      <button onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}
        className="btn-primary btn-sm mt-4 cursor-pointer">
        {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
        Enregistrer les paramètres
      </button>
    </div>
  );
}
