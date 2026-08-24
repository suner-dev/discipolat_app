import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { Loader2, Smartphone, CheckCircle2, XCircle, Clock, TrendingUp } from 'lucide-react';
import toast from 'react-hot-toast';

const OPERATORS = [
  { value: 'M_PESA', label: 'M-Pesa' },
  { value: 'MTN_MOMO', label: 'MTN MoMo' },
  { value: 'ORANGE_MONEY', label: 'Orange Money' },
  { value: 'AIRTEL_MONEY', label: 'Airtel Money' },
  { value: 'WAVE', label: 'Wave' },
  { value: 'CARD', label: 'Carte bancaire' },
  { value: 'CASH', label: 'Espèces' },
];

const PURPOSES = [
  { value: 'DIME', label: 'Dîme' },
  { value: 'OFFRANDE', label: 'Offrande' },
  { value: 'PROMESSE', label: 'Promesse' },
  { value: 'PROJET_SPECIAL', label: 'Projet spécial' },
  { value: 'DON_DIASPORA', label: 'Don diaspora' },
];

interface PaymentIntent {
  id: string;
  operator: string;
  amount: number;
  currency: string;
  purpose: string;
  status: string;
  providerReference: string | null;
  failureReason?: string | null;
}

/** Tithe & Offering 2.0 — dons par Mobile Money avec suivi temps réel. */
export default function GivingPage() {
  const { activeRole } = useAuth();
  // Gestionnaires : vue globale (tous les paiements + statistiques agrégées).
  // Autres rôles : uniquement « mes dons » (endpoint /payments/mine).
  const canManage = activeRole === 'ADMIN' || activeRole === 'PASTEUR' || activeRole === 'RESPONSABLE';
  const [operator, setOperator] = useState('ORANGE_MONEY');
  const [amount, setAmount] = useState('');
  const [phone, setPhone] = useState('');
  const [purpose, setPurpose] = useState('OFFRANDE');
  const [pendingRef, setPendingRef] = useState<string | null>(null);

  const recentQuery = useQuery({
    queryKey: ['payments-recent', canManage],
    queryFn: async () => (await api.get<PaymentIntent[]>(canManage ? '/payments' : '/payments/mine')).data,
    refetchInterval: pendingRef ? 3000 : false,
  });

  const statsQuery = useQuery({
    queryKey: ['payments-stats'],
    enabled: canManage,
    queryFn: async () =>
      (
        await api.get<{
          byOperator: { operator: string; label: string; total: number; count: number }[];
          confirmed: number;
          monthlyTrend: { month: string; total: number }[];
        }>('/payments/stats')
      ).data,
  });

  const initiateMutation = useMutation({
    mutationFn: async () =>
      (
        await api.post<PaymentIntent>('/payments/initiate', {
          operator,
          amount: Number(amount),
          phoneNumber: phone || undefined,
          purpose,
          currency: 'XOF',
        })
      ).data,
    onSuccess: (intent) => {
      toast.success(`Paiement initié — référence ${intent.providerReference}`);
      setPendingRef(intent.id);
      setAmount('');
      // Simulation de confirmation opérateur (sandbox, gestionnaires uniquement)
      if (import.meta.env.DEV && canManage) {
        setTimeout(() => {
          api.post(`/payments/${intent.id}/simulate-confirmation?success=true`)
            .then(() => toast.success('Paiement confirmé — reçu généré !'))
            .catch(() => undefined)
            .finally(() => {
              setPendingRef(null);
              statsQuery.refetch();
              recentQuery.refetch();
            });
        }, 4000);
      }
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-600 text-white shadow-lg">
          <Smartphone className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Dîmes & Offrandes</h1>
          <p className="page-subtitle">Mobile Money instantané — M-Pesa, MTN, Orange, Wave…</p>
        </div>
      </div>

      {/* Formulaire de don */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          initiateMutation.mutate();
        }}
        className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2 animate-slide-up"
      >
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Type de don</label>
          <select className="input w-full" value={purpose} onChange={(e) => setPurpose(e.target.value)}>
            {PURPOSES.map((p) => (
              <option key={p.value} value={p.value}>{p.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Opérateur</label>
          <select className="input w-full" value={operator} onChange={(e) => setOperator(e.target.value)}>
            {OPERATORS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">Montant (XOF)</label>
          <input required type="number" min="100" step="100" placeholder="5000"
            className="input w-full" value={amount} onChange={(e) => setAmount(e.target.value)} />
        </div>
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
            Numéro Mobile Money (optionnel)
          </label>
          <input type="tel" placeholder="+225 07 xx xx xx xx"
            className="input w-full" value={phone} onChange={(e) => setPhone(e.target.value)} />
        </div>
        <button type="submit" disabled={initiateMutation.isPending}
          className="btn-primary md:col-span-2 flex items-center justify-center gap-2">
          {initiateMutation.isPending ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : (
            <Smartphone className="w-5 h-5" />
          )}
          Donner maintenant
        </button>
        {pendingRef && (
          <p className="md:col-span-2 text-sm text-center text-amber-600 dark:text-amber-400 flex items-center justify-center gap-2">
            <Clock className="w-4 h-4 animate-pulse" /> Paiement en cours de confirmation par l'opérateur…
          </p>
        )}
      </form>

      {/* Répartition par opérateur */}
      {(statsQuery.data?.byOperator ?? []).length > 0 && (
        <div className="glass-card p-6 mb-6">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-primary-500" /> Répartition par mode de don
          </h2>
          <div className="space-y-3">
            {(statsQuery.data?.byOperator ?? []).map((o) => {
              const max = Math.max(...(statsQuery.data?.byOperator ?? []).map((x) => Number(x.total)));
              return (
                <div key={o.operator}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-700 dark:text-gray-300">{o.label}</span>
                    <span className="font-medium text-gray-900 dark:text-gray-100">
                      {Number(o.total).toLocaleString('fr-FR')} ({o.count})
                    </span>
                  </div>
                  <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-blue-500 to-cyan-500 rounded-full"
                      style={{ width: `${(Number(o.total) / max) * 100}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Derniers paiements */}
      <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
        <div className="px-5 py-4">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100">Derniers paiements</h2>
        </div>
        {(recentQuery.data ?? []).map((p) => (
          <div key={p.id} className="flex items-center justify-between px-5 py-3">
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                {OPERATORS.find((o) => o.value === p.operator)?.label ?? p.operator} ·{' '}
                {Number(p.amount).toLocaleString('fr-FR')} {p.currency}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                {PURPOSES.find((x) => x.value === p.purpose)?.label ?? p.purpose} · {p.providerReference}
              </p>
            </div>
            {p.status === 'CONFIRMED' && (
              <span className="badge badge-success flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3" /> Confirmé
              </span>
            )}
            {p.status === 'CONFIRMED' && (
              <RecuFiscalButton id={p.id} />
            )}
            {p.status === 'PENDING' && (
              <span className="badge badge-warning flex items-center gap-1">
                <Clock className="w-3 h-3" /> En attente
              </span>
            )}
            {p.status === 'FAILED' && (
              <span className="badge badge-danger flex items-center gap-1">
                <XCircle className="w-3 h-3" /> Échoué
              </span>
            )}
          </div>
        ))}
        {!recentQuery.isLoading && (recentQuery.data ?? []).length === 0 && (
          <p className="text-center text-sm text-gray-500 py-8">Aucun paiement pour le moment.</p>
        )}
      </div>
    </div>
  );
}
/** P12 — Téléchargement du reçu fiscal PDF pour un paiement confirmé. */
function RecuFiscalButton({ id }: { id: string }) {
  const [loading, setLoading] = useState(false);
  const download = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/payments/${id}/tax-receipt`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `recu-fiscal-${id}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      toast.error('Reçu indisponible.');
    } finally {
      setLoading(false);
    }
  };
  return (
    <button onClick={download} disabled={loading} className="btn-secondary btn-sm flex items-center gap-1.5 ml-2">
      {loading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <span>🧾</span>} Reçu fiscal
    </button>
  );
}
