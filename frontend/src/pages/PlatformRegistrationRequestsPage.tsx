import { useCallback, useEffect, useState } from 'react';
import api from '@/lib/api';
import { Check, Loader2, RefreshCw, X } from 'lucide-react';

type RegistrationRequest = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  organizationName: string;
  slug: string;
  status: string;
  createdAt: string;
};

type RegistrationPage = {
  content: RegistrationRequest[];
  page: number;
  totalPages: number;
  totalElements: number;
};

export default function PlatformRegistrationRequestsPage() {
  const [data, setData] = useState<RegistrationPage | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get(`/platform/admin/registration-requests?page=${page}&size=50`);
      setData(response.data);
    } catch {
      setError('Impossible de charger les demandes.');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    void load();
  }, [load]);

  /**
   * F3 (console plateforme) — la décision ne passe plus par `window.prompt` :
   * une modale in-app à champ contrôlé. Le refus EXIGE un motif (il est
   * communiqué à la demandeuse et journalisé) ; l'approbation l'accepte.
   */
  const [deciding, setDeciding] = useState<{ id: string; action: 'approve' | 'reject' } | null>(null);
  const [decideReason, setDecideReason] = useState('');

  const decide = async (id: string, action: 'approve' | 'reject', reason: string) => {
    setLoading(true);
    try {
      await api.post(`/platform/admin/registration-requests/${id}/${action}`, { reason });
      setDeciding(null);
      setDecideReason('');
      await load();
    } catch {
      setError('La décision n\'a pas pu être enregistrée.');
    } finally {
      setLoading(false);
    }
  };

  const confirmDecision = () => {
    if (!deciding) return;
    const reason = decideReason.trim();
    // Refus sans motif = refus impossible : le demandeur doit savoir pourquoi.
    if (deciding.action === 'reject' && !reason) return;
    void decide(deciding.id, deciding.action, reason);
  };

  return (
    <main className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold">Demandes d'églises</h1>
            <p className="text-sm text-slate-500">Approuver les nouvelles organisations avant création.</p>
          </div>
          <button type="button" className="inline-flex items-center rounded-md border border-slate-300 bg-white px-4 py-2 text-sm hover:bg-slate-50 disabled:opacity-50" onClick={() => void load()} disabled={loading}>
            <RefreshCw className="mr-2 h-4 w-4" /> Actualiser
          </button>
        </div>
        {error && <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
        {loading && !data ? <div className="flex justify-center py-12"><Loader2 className="h-6 w-6 animate-spin" /></div> : data?.content.length ? (
          <div className="space-y-3">
            {data.content.map((request) => (
              <div key={request.id} className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                <div>
                  <h2 className="font-semibold">{request.organizationName}</h2>
                  <p className="text-sm text-slate-600">{request.firstName} {request.lastName} · {request.email}</p>
                  <p className="mt-1 font-mono text-xs text-slate-400">{request.slug} · {new Date(request.createdAt).toLocaleString()}</p>
                </div>
                <div className="flex gap-2">
                  <button type="button" className="inline-flex items-center rounded-md bg-emerald-600 px-3 py-2 text-sm text-white disabled:opacity-50" onClick={() => { setDecideReason(''); setDeciding({ id: request.id, action: 'approve' }); }} disabled={loading}><Check className="mr-1 h-4 w-4" />Approuver</button>
                  <button type="button" className="inline-flex items-center rounded-md border border-red-200 px-3 py-2 text-sm text-red-700 disabled:opacity-50" onClick={() => { setDecideReason(''); setDeciding({ id: request.id, action: 'reject' }); }} disabled={loading}><X className="mr-1 h-4 w-4" />Refuser</button>
                </div>
              </div>
            ))}
            <div className="flex justify-between border-t pt-4 text-sm text-slate-500">
              <span>{data.totalElements} demande(s)</span>
              <div className="flex gap-2">
                <button type="button" className="rounded-md border px-3 py-1 disabled:opacity-50" onClick={() => setPage((value) => value - 1)} disabled={page === 0}>Précédent</button>
                <button type="button" className="rounded-md border px-3 py-1 disabled:opacity-50" onClick={() => setPage((value) => value + 1)} disabled={page + 1 >= data.totalPages}>Suivant</button>
              </div>
            </div>
          </div>
        ) : <div className="rounded-xl border border-slate-200 bg-white p-12 text-center text-sm text-slate-500">Aucune demande en attente.</div>}
      </div>

      {/* ---- Modale de décision (F3 : zéro window.prompt) ---- */}
      {deciding && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-5 shadow-xl space-y-4">
            <h2 className="text-lg font-semibold text-slate-900">
              {deciding.action === 'approve' ? 'Approuver la demande' : 'Refuser la demande'}
            </h2>
            <label className="block text-sm font-medium text-slate-700">
              {deciding.action === 'approve' ? 'Motif (facultatif)' : 'Motif du refus (obligatoire)'}
            </label>
            <textarea
              value={decideReason}
              onChange={(e) => setDecideReason(e.target.value)}
              rows={3}
              autoFocus
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-slate-400"
            />
            {deciding.action === 'reject' && (
              <p className="text-xs text-slate-500">Le motif est obligatoire : il est communiqué à la demandeuse.</p>
            )}
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => { setDeciding(null); setDecideReason(''); }}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm hover:bg-slate-50"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={confirmDecision}
                disabled={loading || (deciding.action === 'reject' && !decideReason.trim())}
                className={`rounded-md px-3 py-2 text-sm font-medium text-white disabled:opacity-50 ${
                  deciding.action === 'approve' ? 'bg-emerald-600 hover:bg-emerald-500' : 'bg-red-600 hover:bg-red-500'
                }`}
              >
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : deciding.action === 'approve' ? 'Approuver' : 'Refuser'}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
