import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import { getI18nLocale } from '@/i18n';
import { tText } from '@/i18n';
import { AlertTriangle, CheckCircle2, Loader2, RefreshCw } from 'lucide-react';

/**
 * §G5.7 — Conflits hors-ligne (LWW) en attente de réconciliation.
 *
 * Le mobile ne perd RIEN : les écritures terrain sont rejouées par lot
 * (POST /sync/batch, idempotence par client_uuid). Quand la version serveur a
 * changé APRÈS la saisie terrain (LWW), le serveur enregistre un SyncConflict
 * et notifie les responsables — jamais d'écrasement silencieux. Cette page
 * documente la réconciliation manuelle décidée par le responsable.
 */
interface SyncConflict {
  id: string;
  clientUuid?: string;
  entityType: string;
  entityId: string;
  fieldName?: string;
  clientValue?: string;
  serverValue?: string;
  clientOpAt?: string;
  serverUpdatedAt?: string;
  createdAt: string;
}

function pretty(value?: string) {
  if (!value) return '—';
  try {
    return JSON.stringify(JSON.parse(value), null, 1);
  } catch {
    return value;
  }
}

export default function SyncConflictsPage() {
  const qc = useQueryClient();
  const [noteByConflict, setNoteByConflict] = useState<Record<string, string>>({});

  const { data: conflicts = [], isLoading } = useQuery({
    queryKey: ['sync-conflicts'],
    queryFn: async () => (await api.get('/sync/conflicts')).data as SyncConflict[],
  });

  const resolveMutation = useMutation({
    mutationFn: async ({ id, note }: { id: string; note: string }) => {
      const res = await api.post(`/sync/conflicts/${id}/resolve`, { note });
      return res.data as SyncConflict;
    },
    onSuccess: () => {
      toast.success(tText('Conflit réconcilié'));
      qc.invalidateQueries({ queryKey: ['sync-conflicts'] });
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-orange-500 to-red-700 text-white shadow-lg">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">{tText('Conflits hors-ligne')}</h1>
          <p className="page-subtitle">
            {tText('Écritures terrain en conflit avec la version serveur — réconciliation manuelle, aucune perte silencieuse')}
          </p>
        </div>
        <div className="ml-auto">
          <button
            onClick={() => qc.invalidateQueries({ queryKey: ['sync-conflicts'] })}
            className="btn-sm btn-secondary flex items-center gap-1.5"
          >
            <RefreshCw className="w-4 h-4" /> {tText('Actualiser')}
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : conflicts.length === 0 ? (
        <div className="glass-card p-10 text-center text-gray-500">
          <CheckCircle2 className="w-10 h-10 mx-auto mb-3 text-green-500 opacity-70" />
          {tText('Aucun conflit en attente — la file hors-ligne est propre')}
        </div>
      ) : (
        <div className="space-y-3">
          {conflicts.map((c) => (
            <div key={c.id} className="glass-card p-5">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2 mb-1">
                    <span className="px-2 py-0.5 rounded-full text-xs font-medium text-orange-400 bg-orange-500/20">
                      LWW
                    </span>
                    <span className="text-xs text-gray-400">
                      {c.entityType} · {c.fieldName ?? '—'}
                    </span>
                    <span className="text-[11px] text-gray-500">
                      {new Date(c.createdAt).toLocaleString(getI18nLocale())}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 break-all mb-2">
                    {tText('Entité')} <span className="font-mono">{c.entityId}</span>
                    {c.clientUuid && (
                      <> · {tText('Opération')} <span className="font-mono">{c.clientUuid}</span></>
                    )}
                  </p>
                  <div className="grid md:grid-cols-2 gap-3">
                    <div className="rounded-lg bg-white/5 border border-white/10 p-3">
                      <p className="text-[11px] uppercase tracking-wide text-gray-500 mb-1">
                        {tText('Version terrain (mobile)')}
                        {c.clientOpAt && <> · {new Date(c.clientOpAt).toLocaleString(getI18nLocale())}</>}
                      </p>
                      <pre className="text-xs text-gray-300 whitespace-pre-wrap break-words max-h-32 overflow-auto">{pretty(c.clientValue)}</pre>
                    </div>
                    <div className="rounded-lg bg-white/5 border border-white/10 p-3">
                      <p className="text-[11px] uppercase tracking-wide text-gray-500 mb-1">
                        {tText('Version serveur (conservée)')}
                        {c.serverUpdatedAt && <> · {new Date(c.serverUpdatedAt).toLocaleString(getI18nLocale())}</>}
                      </p>
                      <pre className="text-xs text-gray-300 whitespace-pre-wrap break-words max-h-32 overflow-auto">{pretty(c.serverValue)}</pre>
                    </div>
                  </div>
                  <p className="text-[11px] text-gray-500 mt-2">
                    {tText('La résolution LWW a conservé la dernière écriture en ligne ; le mobile a gardé sa trace ici pour arbitrage humain.')}
                  </p>
                </div>
                <div className="w-full md:w-64 shrink-0">
                  <textarea
                    value={noteByConflict[c.id] ?? ''}
                    onChange={(e) => setNoteByConflict((p) => ({ ...p, [c.id]: e.target.value }))}
                    placeholder={tText('Décision du responsable (documentée)…')}
                    className="input w-full text-xs h-20 resize-none mb-2"
                  />
                  <button
                    onClick={() => resolveMutation.mutate({
                      id: c.id,
                      note: noteByConflict[c.id]?.trim() || tText('Réconcilié sans modification'),
                    })}
                    disabled={resolveMutation.isPending}
                    className="btn-sm w-full px-3 py-1.5 rounded-lg bg-green-600 text-white text-xs hover:bg-green-700 flex items-center justify-center gap-1"
                  >
                    {resolveMutation.isPending
                      ? <Loader2 className="w-3 h-3 animate-spin" />
                      : <CheckCircle2 className="w-3 h-3" />}
                    {tText('Valider la réconciliation')}
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
