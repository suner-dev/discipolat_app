import { FormEvent, useState } from 'react';
import { AlertTriangle, LogIn, ShieldAlert } from 'lucide-react';
import { useImpersonation } from '@/contexts/ImpersonationContext';

export default function PlatformImpersonationPage() {
  const { startImpersonation } = useImpersonation();
  const [tenantId, setTenantId] = useState('');
  const [targetEmail, setTargetEmail] = useState('');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await startImpersonation(targetEmail, reason, tenantId);
    } catch {
      setError('Impersonation refusée ou indisponible.');
      setSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <div className="mx-auto max-w-2xl space-y-6">
        <div>
          <h1 className="text-2xl font-semibold">Impersonation</h1>
          <p className="text-sm text-slate-500">Observer l'espace d'un utilisateur sans lui accorder de privilèges supplémentaires.</p>
        </div>

        <div className="flex gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          <ShieldAlert className="mt-0.5 h-5 w-5 shrink-0" />
          <p>Toute session est journalisée avec l'acteur Super Admin, l'IP, le motif et la durée. Le jeton expire automatiquement.</p>
        </div>

        <form onSubmit={submit} className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
          <label className="block text-sm font-medium">Tenant ID<input required value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="UUID du tenant" className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 font-mono text-sm" /></label>
          <label className="block text-sm font-medium">Email de la cible<input required type="email" value={targetEmail} onChange={(event) => setTargetEmail(event.target.value)} placeholder="utilisateur@eglise.com" className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2" /></label>
          <label className="block text-sm font-medium">Motif obligatoire<textarea required minLength={5} value={reason} onChange={(event) => setReason(event.target.value)} className="mt-1 min-h-24 w-full rounded-md border border-slate-300 px-3 py-2" /></label>
          {error && <div className="flex items-center gap-2 rounded-md bg-red-50 p-3 text-sm text-red-700"><AlertTriangle className="h-4 w-4" />{error}</div>}
          <button type="submit" disabled={submitting} className="inline-flex items-center rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50">
            <LogIn className="mr-2 h-4 w-4" />{submitting ? 'Démarrage...' : 'Démarrer impersonation'}
          </button>
        </form>
      </div>
    </main>
  );
}
