import { useCallback, useEffect, useState } from 'react';
import api from '@/lib/api';
import { AlertCircle, ChevronLeft, ChevronRight, Loader2, RefreshCw } from 'lucide-react';

type AuditLog = {
  id: string;
  tenantId: string;
  userId?: string;
  action: string;
  entityType: string;
  entityId?: string;
  ipAddress?: string;
  createdAt: string;
};

type AuditPage = {
  content: AuditLog[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export default function PlatformAuditPage() {
  const [data, setData] = useState<AuditPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get(`/platform/admin/audit-logs?page=${page}&size=50`);
      setData(response.data);
    } catch {
      setError('Impossible de charger les journaux plateforme.');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <main className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold">Audit plateforme</h1>
            <p className="text-sm text-slate-500">Traçabilité globale des actions Super Admin.</p>
          </div>
          <button type="button" className="inline-flex items-center rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50" onClick={() => void load()} disabled={loading}>
            <RefreshCw className={loading ? 'mr-2 h-4 w-4 animate-spin' : 'mr-2 h-4 w-4'} />
            Actualiser
          </button>
        </div>

        {error && (
          <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
            <AlertCircle className="h-4 w-4" />
            {error}
          </div>
        )}

        <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b p-6">
            <h2 className="text-lg font-semibold">Événements récents</h2>
            <span className="text-sm text-slate-500">{data?.totalElements ?? 0} entrées</span>
          </div>
          <div className="p-6">
            {loading && !data ? (
              <div className="flex items-center justify-center py-12 text-slate-500">
                <Loader2 className="mr-2 h-5 w-5 animate-spin" /> Chargement...
              </div>
            ) : data?.content.length ? (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="border-b text-xs uppercase text-slate-500">
                    <tr>
                      <th className="px-3 py-3">Date</th>
                      <th className="px-3 py-3">Action</th>
                      <th className="px-3 py-3">Entité</th>
                      <th className="px-3 py-3">Tenant</th>
                      <th className="px-3 py-3">Utilisateur</th>
                      <th className="px-3 py-3">Adresse IP</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {data.content.map((log) => (
                      <tr key={log.id} className="hover:bg-slate-50">
                        <td className="whitespace-nowrap px-3 py-3 text-slate-500">{new Date(log.createdAt).toLocaleString()}</td>
                        <td className="px-3 py-3 font-medium">{log.action}</td>
                        <td className="px-3 py-3">{log.entityType}</td>
                        <td className="px-3 py-3 font-mono text-xs">{log.tenantId.slice(0, 8)}</td>
                        <td className="px-3 py-3 font-mono text-xs">{log.userId?.slice(0, 8) ?? '—'}</td>
                        <td className="px-3 py-3 text-slate-500">{log.ipAddress ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="py-12 text-center text-sm text-slate-500">Aucun journal disponible.</div>
            )}

            <div className="mt-4 flex items-center justify-between border-t pt-4">
              <span className="text-sm text-slate-500">Page {data ? data.page + 1 : page + 1} / {data?.totalPages || 1}</span>
              <div className="flex gap-2">
                <button type="button" className="inline-flex items-center rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm hover:bg-slate-50 disabled:opacity-50" disabled={page === 0 || loading} onClick={() => setPage((value) => value - 1)}>
                  <ChevronLeft className="h-4 w-4" /> Précédent
                </button>
                <button type="button" className="inline-flex items-center rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm hover:bg-slate-50 disabled:opacity-50" disabled={!data || page + 1 >= data.totalPages || loading} onClick={() => setPage((value) => value + 1)}>
                  Suivant <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
