import { formatEnum } from '@/lib/labels';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { HardDriveDownload, ShieldCheck, Trash2, Loader2, Plus } from 'lucide-react';
import toast from 'react-hot-toast';

import { getI18nLocale } from '@/i18n';
import { tText } from '@/i18n';

// audit-routes: /backups
// Contrat réel — BackupResponse (com.discipolat.modules.backup.api.BackupResponse)
interface BackupRecord {
  id: string;
  tenantId: string;
  fileName: string;
  sizeBytes: number;
  sha256?: string;
  createdAt: string;
  createdBy?: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'VERIFIED';
  tablesExported?: number | null;
  rowsExported?: number | null;
  durationMs?: number | null;
}

const STATUS_STYLE: Record<string, string> = {
  COMPLETED: 'text-green-400 bg-green-500/20',
  VERIFIED: 'text-emerald-400 bg-emerald-500/20',
  FAILED: 'text-red-400 bg-red-500/20',
  PENDING: 'text-yellow-400 bg-yellow-500/20',
  RUNNING: 'text-blue-400 bg-blue-500/20',
};

/** P3 #110 — Sauvegardes PostgreSQL : création, vérification d'intégrité, téléchargement, suppression. */
export default function AdminBackupsPage() {
  const qc = useQueryClient();
  const listQ = useQuery({ queryKey: ['backups'], queryFn: async () => (await api.get('/backups')).data as BackupRecord[] });

  const invalidate = () => qc.invalidateQueries({ queryKey: ['backups'] });

  // POST /backups — aucun corps, lance une sauvegarde complète du tenant courant.
  const create = useMutation({
    mutationFn: async () => (await api.post<BackupRecord>('/backups')).data,
    onSuccess: (b) => { toast.success(`Sauvegarde créée : ${b.fileName}`); invalidate(); },
    onError: (e) => toast.error((e as Error).message),
  });

  // POST /backups/{id}/verify — relit l'archive et recalcule le SHA-256.
  const verify = useMutation({
    mutationFn: async (id: string) => (await api.post<{ valid: boolean }>(`/backups/${id}/verify`)).data,
    onSuccess: (r) => { toast.success(r.valid ? tText('Intégrité vérifiée') : tText('Intégrité non conforme')); invalidate(); },
    onError: (e) => toast.error((e as Error).message),
  });

  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/backups/${id}`),
    onSuccess: () => { toast.success(tText('Sauvegarde supprimée')); invalidate(); },
    onError: (e) => toast.error((e as Error).message),
  });

  // GET /backups/{id}/download — archive gzip.
  const download = useMutation({
    mutationFn: async (id: string) => {
      const res = await api.get(`/backups/${id}/download`, { responseType: 'blob' });
      return res.data as Blob;
    },
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'backup.tar.gz'; a.click();
      URL.revokeObjectURL(url);
    },
    onError: (e) => toast.error((e as Error).message),
  });

  const fmtSize = (b?: number) => !b ? '—' : b > 1e9 ? `${(b / 1e9).toFixed(1)} Go` : `${Math.round(b / 1e6)} Mo`;

  return (
    <div className="space-y-6 p-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2"><HardDriveDownload className="text-amber-400" /> Sauvegardes PostgreSQL</h1>
        <button onClick={() => create.mutate()} disabled={create.isPending} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-amber-600 to-orange-600 text-white text-sm font-medium hover:opacity-90 disabled:opacity-50">
          {create.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />} Nouvelle sauvegarde complète
        </button>
      </div>

      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <h2 className="text-white font-semibold mb-3">Sauvegardes</h2>
        {(listQ.data ?? []).length === 0 ? <p className="text-sm text-gray-500">Aucune sauvegarde. Cliquez sur « Nouvelle sauvegarde » pour démarrer.</p> : (
          <div className="space-y-2">
            {(listQ.data ?? []).map((b) => (
              <div key={b.id} className="flex flex-wrap items-center justify-between gap-3 bg-black/20 rounded-xl px-4 py-3 text-sm">
                <div className="min-w-0">
                  <span className="text-white font-medium">{b.fileName}</span>
                  <span className="text-gray-500 ml-3">{fmtSize(b.sizeBytes)} • {new Date(b.createdAt).toLocaleString(getI18nLocale())}</span>
                  {b.rowsExported != null && <span className="text-gray-600 ml-3">{b.rowsExported} lignes / {b.tablesExported} tables</span>}
                </div>
                <div className="flex items-center gap-3">
                  {(b.status === 'VERIFIED' || b.status === 'COMPLETED') && <ShieldCheck className="w-4 h-4 text-green-400" aria-label={tText('Intégrité')} />}
                  <span className={`px-2 py-0.5 rounded-full text-xs ${STATUS_STYLE[b.status] ?? 'text-gray-400 bg-gray-500/20'}`}>{formatEnum(b.status)}</span>
                  <button onClick={() => download.mutate(b.id)} disabled={download.isPending} aria-label={tText('Télécharger')} title={tText('Télécharger')} className="text-sky-400 hover:text-sky-300"><HardDriveDownload className="w-4 h-4" /></button>
                  <button onClick={() => verify.mutate(b.id)} disabled={verify.isPending} className="text-green-400 hover:text-green-300" aria-label={tText('Vérifier intégrité')} title={tText('Vérifier intégrité')}><ShieldCheck className="w-4 h-4" /></button>
                  <button onClick={() => remove.mutate(b.id)} disabled={remove.isPending} aria-label={tText('Supprimer la sauvegarde')} className="text-red-400 hover:text-red-300"><Trash2 className="w-4 h-4" /></button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
