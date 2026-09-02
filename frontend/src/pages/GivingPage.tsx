import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import api, { getErrorMessage } from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { useI18n } from '@/i18n/index';
import {
  Loader2,
  Smartphone,
  CheckCircle2,
  XCircle,
  Clock,
  TrendingUp,
  Repeat,
  Calendar,
  Pause,
} from 'lucide-react';
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
  { value: 'DIME', key: 'giving.purpose.dime' },
  { value: 'OFFRANDE', key: 'giving.purpose.offrande' },
  { value: 'PROMESSE', key: 'giving.purpose.promesse' },
  { value: 'PROJET_SPECIAL', key: 'giving.purpose.projet' },
  { value: 'DON_DIASPORA', key: 'giving.purpose.diaspora' },
];

const FREQUENCIES = [
  { value: 'WEEKLY', key: 'giving.recurring.frequency.weekly' },
  { value: 'BIWEEKLY', key: 'giving.recurring.frequency.bimonthly' },
  { value: 'MONTHLY', key: 'giving.recurring.frequency.monthly' },
  { value: 'QUARTERLY', key: 'giving.recurring.frequency.quarterly' },
  { value: 'YEARLY', key: 'giving.recurring.frequency.yearly' },
];

interface PaymentIntent {
  id: string;
  operator: string;
  amount: number;
  currency: string;
  purpose: string;
  status: string;
  providerReference: string | null;
  checkoutUrl?: string | null;
  instructions?: string | null;
  failureReason?: string | null;
  createdAt?: string;
  confirmedAt?: string;
}

interface RecurringDonation {
  id: string;
  operator: string;
  phoneNumber: string | null;
  amount: number;
  currency: string;
  purpose: string;
  frequency: string;
  active: boolean;
  nextDonationDate: string | null;
  totalDonated: number;
  donationCount: number;
  createdAt: string;
}

/** Tithe & Offering 2.0 — dons par Mobile Money avec suivi temps réel + dons récurrents. */
export default function GivingPage() {
  const { activeRole } = useAuth();
  const { t } = useI18n();
  const canManage =
    activeRole === 'ADMIN' || activeRole === 'PASTEUR' || activeRole === 'RESPONSABLE';

  // --- Don unique ---
  const [operator, setOperator] = useState('ORANGE_MONEY');
  const [amount, setAmount] = useState('');
  const [phone, setPhone] = useState('');
  const [purpose, setPurpose] = useState('OFFRANDE');
  const [pendingRef, setPendingRef] = useState<string | null>(null);

  // --- Don récurrent ---
  const [showRecurring, setShowRecurring] = useState(false);
  const [rcOperator, setRcOperator] = useState('ORANGE_MONEY');
  const [rcAmount, setRcAmount] = useState('');
  const [rcPhone, setRcPhone] = useState('');
  const [rcPurpose, setRcPurpose] = useState('DIME');
  const [rcFrequency, setRcFrequency] = useState('MONTHLY');

  const recentQuery = useQuery({
    queryKey: ['payments', 'recent', canManage],
    queryFn: async () =>
      (await api.get<PaymentIntent[]>(canManage ? '/payments' : '/payments/mine')).data,
    refetchInterval: pendingRef ? 3000 : false,
  });

  const statsQuery = useQuery({
    queryKey: ['payments', 'stats'],
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

  // Dons récurrents
  const recurringQuery = useQuery({
    queryKey: ['payments', 'recurring', 'mine'],
    queryFn: async () =>
      (await api.get<RecurringDonation[]>('/payments/recurring/mine')).data,
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
      toast.success(t('giving.initiated', { ref: intent.providerReference ?? '' }));
      setAmount('');

      // Orange Money retourne une URL de paiement externe
      if (intent.checkoutUrl) {
        toast.success(intent.instructions ?? 'Redirection vers Orange Money...', { duration: 4000 });
        window.open(intent.checkoutUrl, '_blank');
      }

      // Polling du statut en arrière-plan
      setPendingRef(intent.id);
      let attempts = 0;
      const pollInterval = setInterval(() => {
        attempts++;
        api
          .get<PaymentIntent>(`/payments/${intent.id}`)
          .then((res) => {
            const status = res.data.status;
            if (status === 'CONFIRMED' || status === 'FAILED' || status === 'CANCELLED' || attempts > 20) {
              clearInterval(pollInterval);
              if (status === 'CONFIRMED') {
                toast.success(t('giving.confirmed'));
              }
              setPendingRef(null);
              recentQuery.refetch();
              statsQuery.refetch();
            }
          })
          .catch(() => {
            if (attempts > 20) {
              clearInterval(pollInterval);
              setPendingRef(null);
            }
          });
      }, 3000);
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const createRecurringMutation = useMutation({
    mutationFn: async () =>
      (
        await api.post<RecurringDonation>('/payments/recurring', {
          operator: rcOperator,
          amount: Number(rcAmount),
          phoneNumber: rcPhone || undefined,
          purpose: rcPurpose,
          frequency: rcFrequency,
          currency: 'XOF',
        })
      ).data,
    onSuccess: () => {
      toast.success(t('giving.recurring.created'));
      setRcAmount('');
      setRcPhone('');
      setShowRecurring(false);
      recurringQuery.refetch();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const cancelRecurringMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/payments/recurring/${id}/cancel`);
    },
    onSuccess: () => {
      toast.success(t('giving.recurring.cancelled'));
      recurringQuery.refetch();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const cancelPaymentMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/payments/${id}/cancel`);
    },
    onSuccess: () => {
      toast.success(t('giving.cancelled'));
      recentQuery.refetch();
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
          <h1 className="page-title">{t('giving.title')}</h1>
          <p className="page-subtitle">{t('giving.subtitle')}</p>
        </div>
      </div>

      {/* Formulaire de don unique */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          initiateMutation.mutate();
        }}
        className="glass-card p-6 mb-6 grid gap-4 md:grid-cols-2 animate-slide-up"
      >
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
            {t('giving.purpose')}
          </label>
          <select
            className="input w-full"
            value={purpose}
            onChange={(e) => setPurpose(e.target.value)}
          >
            {PURPOSES.map((p) => (
              <option key={p.value} value={p.value}>
                {t(p.key)}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
            {t('giving.operator')}
          </label>
          <select
            className="input w-full"
            value={operator}
            onChange={(e) => setOperator(e.target.value)}
          >
            {OPERATORS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
            {t('giving.amount')}
          </label>
          <input
            required
            type="number"
            min="100"
            step="100"
            placeholder="5000"
            className="input w-full"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
        </div>
        <div>
          <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
            {t('giving.phone')}
          </label>
          <input
            type="tel"
            placeholder={t('giving.phonePlaceholder')}
            className="input w-full"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />
        </div>
        <button
          type="submit"
          disabled={initiateMutation.isPending}
          className="btn-primary md:col-span-2 flex items-center justify-center gap-2"
        >
          {initiateMutation.isPending ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : (
            <Smartphone className="w-5 h-5" />
          )}
          {t('giving.donate')}
        </button>
        {pendingRef && (
          <p className="md:col-span-2 text-sm text-center text-amber-600 dark:text-amber-400 flex items-center justify-center gap-2">
            <Clock className="w-4 h-4 animate-pulse" /> {t('giving.pending')}
          </p>
        )}
      </form>

      {/* === Dons récurrents === */}
      <div className="glass-card p-6 mb-6 animate-slide-up">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <Repeat className="w-5 h-5 text-purple-500" /> {t('giving.recurring.title')}
          </h2>
          <button
            type="button"
            onClick={() => setShowRecurring(!showRecurring)}
            className="btn-secondary btn-sm"
          >
            {showRecurring ? t('giving.recurring.hide') : t('giving.recurring.new')}
          </button>
        </div>

        {showRecurring && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              createRecurringMutation.mutate();
            }}
            className="grid gap-4 md:grid-cols-2 mb-4 p-4 rounded-xl bg-gray-50 dark:bg-gray-800/50"
          >
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
                {t('giving.purpose')}
              </label>
              <select
                className="input w-full"
                value={rcPurpose}
                onChange={(e) => setRcPurpose(e.target.value)}
              >
                {PURPOSES.map((p) => (
                  <option key={p.value} value={p.value}>
                    {t(p.key)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
                {t('giving.operator')}
              </label>
              <select
                className="input w-full"
                value={rcOperator}
                onChange={(e) => setRcOperator(e.target.value)}
              >
                {OPERATORS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
                {t('giving.amount')}
              </label>
              <input
                required
                type="number"
                min="100"
                step="100"
                placeholder="5000"
                className="input w-full"
                value={rcAmount}
                onChange={(e) => setRcAmount(e.target.value)}
              />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
                {t('giving.recurring.frequency')}
              </label>
              <select
                className="input w-full"
                value={rcFrequency}
                onChange={(e) => setRcFrequency(e.target.value)}
              >
                {FREQUENCIES.map((f) => (
                  <option key={f.value} value={f.value}>
                    {t(f.key)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 block">
                {t('giving.phone')}
              </label>
              <input
                type="tel"
                placeholder={t('giving.phonePlaceholder')}
                className="input w-full"
                value={rcPhone}
                onChange={(e) => setRcPhone(e.target.value)}
              />
            </div>
            <div className="flex items-end">
              <button
                type="submit"
                disabled={createRecurringMutation.isPending}
                className="btn-primary w-full flex items-center justify-center gap-2"
              >
                {createRecurringMutation.isPending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Repeat className="w-4 h-4" />
                )}
                {t('giving.recurring.create')}
              </button>
            </div>
          </form>
        )}

        {/* Liste des dons récurrents */}
        {(recurringQuery.data ?? []).length === 0 ? (
          <p className="text-sm text-gray-500 dark:text-gray-400 text-center py-4">
            {t('giving.recurring.empty')}
          </p>
        ) : (
          <div className="space-y-3">
            {(recurringQuery.data ?? []).map((rd) => (
              <div
                key={rd.id}
                className="flex items-center justify-between p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50"
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`p-2 rounded-lg ${rd.active ? 'bg-purple-100 dark:bg-purple-900/30' : 'bg-gray-200 dark:bg-gray-700'}`}
                  >
                    <Repeat
                      className={`w-4 h-4 ${rd.active ? 'text-purple-600 dark:text-purple-400' : 'text-gray-400'}`}
                    />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                      {Number(rd.amount).toLocaleString('fr-FR')} {rd.currency} ·{' '}
                      {OPERATORS.find((o) => o.value === rd.operator)?.label ?? rd.operator}
                    </p>
                    <p className="text-xs text-gray-500 dark:text-gray-400 flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      {t(
                        FREQUENCIES.find((f) => f.value === rd.frequency)?.key ??
                          'giving.recurring.frequency.monthly',
                      )}
                      {rd.nextDonationDate && (
                        <>
                          {' · '}
                          {t('giving.recurring.nextDate')}: {rd.nextDonationDate}
                        </>
                      )}
                    </p>
                    <p className="text-xs text-gray-400 dark:text-gray-500">
                      {t('giving.recurring.totalDonated')}: {Number(rd.totalDonated).toLocaleString('fr-FR')}{' '}
                      {rd.currency} ({rd.donationCount} {t('giving.recurring.donations')})
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {rd.active ? (
                    <span className="badge badge-success text-xs">
                      <CheckCircle2 className="w-3 h-3 mr-1" /> {t('giving.recurring.active')}
                    </span>
                  ) : (
                    <span className="badge text-xs bg-gray-200 dark:bg-gray-700 text-gray-500">
                      {t('giving.recurring.inactive')}
                    </span>
                  )}
                  {rd.active && (
                    <button
                      onClick={() => cancelRecurringMutation.mutate(rd.id)}
                      disabled={cancelRecurringMutation.isPending}
                      className="btn-secondary btn-sm flex items-center gap-1"
                      title={t('giving.recurring.cancel')}
                    >
                      <Pause className="w-3 h-3" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Répartition par opérateur */}
      {(statsQuery.data?.byOperator ?? []).length > 0 && (
        <div className="glass-card p-6 mb-6">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-primary-500" /> {t('giving.byOperator')}
          </h2>
          <div className="space-y-3">
            {(statsQuery.data?.byOperator ?? []).map((o) => {
              const max = Math.max(
                ...(statsQuery.data?.byOperator ?? []).map((x) => Number(x.total)),
              );
              return (
                <div key={o.operator}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="text-gray-700 dark:text-gray-300">{o.label}</span>
                    <span className="font-medium text-gray-900 dark:text-gray-100">
                      {Number(o.total).toLocaleString('fr-FR')} ({o.count})
                    </span>
                  </div>
                  <div className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-blue-500 to-cyan-500 rounded-full"
                      style={{ width: `${(Number(o.total) / max) * 100}%` }}
                    />
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
          <h2 className="font-semibold text-gray-900 dark:text-gray-100">
            {t('giving.lastPayments')}
          </h2>
        </div>
        {(recentQuery.data ?? []).map((p) => (
          <div key={p.id} className="flex items-center justify-between px-5 py-3">
            <div>
              <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                {OPERATORS.find((o) => o.value === p.operator)?.label ?? p.operator} ·{' '}
                {Number(p.amount).toLocaleString('fr-FR')} {p.currency}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                {t(PURPOSES.find((x) => x.value === p.purpose)?.key ?? 'giving.purpose.offrande')}{' '}
                · {p.providerReference}
              </p>
            </div>
            <div className="flex items-center gap-2">
              {p.status === 'CONFIRMED' && (
                <span className="badge badge-success flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> {t('giving.status.confirmed')}
                </span>
              )}
              {p.status === 'CONFIRMED' && <RecuFiscalButton id={p.id} t={t} />}
              {p.status === 'PENDING' && (
                <div className="flex items-center gap-2">
                  <span className="badge badge-warning flex items-center gap-1">
                    <Clock className="w-3 h-3" /> {t('giving.status.pending')}
                  </span>
                  <button
                    onClick={() => cancelPaymentMutation.mutate(p.id)}
                    disabled={cancelPaymentMutation.isPending}
                    className="text-xs text-red-500 hover:text-red-700 underline"
                    title={t('giving.cancel')}
                  >
                    {t('giving.cancel')}
                  </button>
                </div>
              )}
              {p.status === 'FAILED' && (
                <span className="badge badge-danger flex items-center gap-1">
                  <XCircle className="w-3 h-3" /> {t('giving.status.failed')}
                </span>
              )}
            </div>
          </div>
        ))}
        {!recentQuery.isLoading && (recentQuery.data ?? []).length === 0 && (
          <p className="text-center text-sm text-gray-500 py-8">{t('giving.noPayments')}</p>
        )}
      </div>
    </div>
  );
}

/** P12 — Téléchargement du reçu fiscal PDF pour un paiement confirmé. */
function RecuFiscalButton({ id, t }: { id: string; t: (k: string, opts?: Record<string, string>) => string }) {
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
      toast.error(t('giving.receiptUnavailable'));
    } finally {
      setLoading(false);
    }
  };
  return (
    <button
      onClick={download}
      disabled={loading}
      className="btn-secondary btn-sm flex items-center gap-1.5 ml-2"
    >
      {loading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <span>🧾</span>}{' '}
      {t('giving.receipt')}
    </button>
  );
}
