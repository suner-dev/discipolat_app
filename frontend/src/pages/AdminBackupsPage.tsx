import { formatEnum } from '@/lib/labels';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { HardDriveDownload, Play, ShieldCheck, Trash2, History, Loader2 } from 'lucide-react';

interface BackupRecord { id: string; nom?: string; type?: string; statut: string; tailleOctets?: number; scheduledAt?: string; executedAt?: string; integriteOk?: boolean; }

const STATUS_STYLE: Record<string, string> = {
  SUCCESS: 'text-green-400 bg-green-500/20', COMPLETED: 'text-green-400 bg-green-500/20',
  FAILED: 'text-red-400 bg-red-500/20', PENDING: 'text-yellow-400 bg-yellow-500/20', RUNNING: 'text-blue-400 bg-blue-500/20',
};

/** P3 #110 — Gestion des sauvegardes PostgreSQL : planification, déclenchement, intégrité, restauration. */
export default function AdminBackupsPage() {
  const qc = useQueryClient();
  const listQ = useQuery({ queryKey: ['backups'], queryFn: async () => (await api.get('/backups')).data as BackupRecord[] });
  const restoreQ = useQuery({ queryKey: ['backups-restore-points'], queryFn: async () => (await api.get('/backups/restore-points')).data as Array<Record<string, unknown>> });

  const invalidate = () => { qc.invalidateQueries({ queryKey: ['backups'] }); qc.invalidateQueries({ queryKey: ['backups-restore-points'] }); };
  const schedule = useMutation({ mutationFn: async () => api.post('/backups', { type: 'FULL', statut: 'PENDING' }), onSuccess: invalidate });
  const trigger = useMutation({ mutationFn: async (id: string) => api.post(`/backups/${id}/trigger`), onSuccess: invalidate });
  const verify = useMutation({ mutationFn: async (id: string) => api.post(`/backups/${id}/verify`), onSuccess: invalidate });
  const remove = useMutation({ mutationFn: async (id: string) => api.delete(`/backups/${id}`), onSuccess: invalidate });

  const fmtSize = (b?: number) => !b ? '—' : b > 1e9 ? `${(b / 1e9).toFixed(1)} Go` : `${Math.round(b / 1e6)} Mo`;

  return (
    <div className="space-y-6 p-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2"><HardDriveDownload className="text-amber-400" /> Sauvegardes PostgreSQL</h1>
        <button onClick={() => schedule.mutate()} disabled={schedule.isPending} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-amber-600 to-orange-600 text-white text-sm font-medium hover:opacity-90 disabled:opacity-50">
          {schedule.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />} Planifier une sauvegarde complète
        </button>
      </div>

      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <h2 className="text-white font-semibold mb-3">Sauvegardes</h2>
        {(listQ.data ?? []).length === 0 ? <p className="text-sm text-gray-500">Aucune sauvegarde planifiée. Cliquez sur « Planifier » pour démarrer.</p> : (
          <div className="space-y-2">
            {(listQ.data ?? []).map((b) => (
              <div key={b.id} className="flex flex-wrap items-center justify-between gap-3 bg-black/20 rounded-xl px-4 py-3 text-sm">
                <div>
                  <span className="text-white font-medium">{b.nom ?? b.type ?? 'Backup'}</span>
                  <span className="text-gray-500 ml-3">{fmtSize(b.tailleOctets)}{b.executedAt ? ` • exécutée le ${new Date(b.executedAt).toLocaleString('fr-FR')}` : ''}</span>
                </div>
                <div className="flex items-center gap-3">
                  {b.integriteOk === true && <span title="Intégrité vérifiée"><ShieldCheck className="w-4 h-4 text-green-400" /></span>}
                  <span className={`px-2 py-0.5 rounded-full text-xs ${STATUS_STYLE[b.statut] ?? 'text-gray-400 bg-gray-500/20'}`}>{formatEnum(b.statut)}</span>
                  <button onClick={() => trigger.mutate(b.id)} disabled={trigger.isPending} aria-label="Déclencher la sauvegarde" className="text-sky-400 hover:text-sky-300"><Play className="w-4 h-4" /></button>
                  <button onClick={() => verify.mutate(b.id)} disabled={verify.isPending} className="text-green-400 hover:text-green-300" aria-label="Vérifier intégrité" title="Vérifier intégrité"><ShieldCheck className="w-4 h-4" /></button>
                  <button onClick={() => remove.mutate(b.id)} disabled={remove.isPending} aria-label="Supprimer la sauvegarde" className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <h2 className="text-white font-semibold mb-3 flex items-center gap-2"><History className="w-4 h-4 text-amber-400" /> Points de restauration (point-in-time)</h2>
        {(restoreQ.data ?? []).length === 0 ? <p className="text-sm text-gray-500">Aucun point de restauration disponible.</p> : (
          <div className="space-y-1">
            {(restoreQ.data ?? []).map((r, i) => (
              <div key={i} className="flex items-center justify-between text-sm bg-black/20 rounded-lg px-3 py-2">
                <span className="text-gray-300">{String(r.horodatage ?? r.timestamp ?? r.date ?? `Point #${i + 1}`)}</span>
                <span className="text-gray-500">{String(r.type ?? '')}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
