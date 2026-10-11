import { useCallback, useEffect, useState } from 'react';
import { Plus, Pencil, RefreshCw, Trash2, AlertTriangle, CheckCircle2, ExternalLink } from 'lucide-react';
import api from '@/lib/api';
import { getErrorMessage } from '@/lib/api';
import { majorToInput, parseMajorToMinor } from '@/lib/money';
import type {
  AdminCurrencyCatalogEntry,
  AdminPlanPrice,
  BillingCycleName,
  PaymentChannelName,
  PricingDiagnostics,
} from '@/features/pricing/types';

/**
 * Administration plateforme — catalogue de prix (T5.2).
 *
 * <p><b>Correction de la faille F5</b> : l'ancien écran affichait
 * « FCFA/mois » en dur, quelle que soit la devise du plan. Un plan en euros
 * était donc présenté comme étant en francs CFA. Ici la devise vient du prix
 * lui-même, et son symbole vient du catalogue ISO-4217 du serveur.</p>
 *
 * <p><b>Un montant n'est saisi qu'en MAJEUR et converti par
 * {@link parseMajorToMinor}</b>, qui applique les décimales de la devise. C'est le
 * seul chemin d'écriture du montant : aucun « ×100 » n'existe dans ce fichier,
 * et un montant à décimales sur une devise sans centimes est refusé par le
 * serveur.</p>
 */

const GLOBAL_REGION = 'GLOBAL';

const CHANNELS: PaymentChannelName[] = ['CARD', 'MOMO', 'STRONG', 'BANK', 'CONTACT'];

const STATUS_STYLE: Record<string, string> = {
  SYNCED: 'bg-emerald-100 text-emerald-700',
  MANUAL: 'bg-sky-100 text-sky-700',
  PENDING: 'bg-amber-100 text-amber-700',
  FAILED: 'bg-red-100 text-red-700',
  NOT_REQUIRED: 'bg-slate-100 text-slate-600',
};

const STATUS_LABEL: Record<string, string> = {
  SYNCED: 'Synchronisé',
  MANUAL: 'Saisi manuellement',
  PENDING: 'En attente',
  FAILED: 'Échec',
  NOT_REQUIRED: 'Non requis',
};


const emptyForm = {
  currency: 'EUR',
  billingCycle: 'MONTHLY' as BillingCycleName,
  amountInput: '',
  regionCode: GLOBAL_REGION,
  checkoutCurrency: '',
  isEnabled: true,
  isVisible: true,
  isSelectable: true,
  paymentChannels: ['CARD'] as PaymentChannelName[],
  stripePriceId: '',
};

export default function PlanPricesPanel({ planKey }: { planKey: string }) {
  const [prices, setPrices] = useState<AdminPlanPrice[]>([]);
  const [diagnostics, setDiagnostics] = useState<PricingDiagnostics | null>(null);
  const [currencies, setCurrencies] = useState<AdminCurrencyCatalogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [pricesRes, diagRes, currenciesRes] = await Promise.all([
        api.get<AdminPlanPrice[]>(`/platform/admin/pricing/plans/${encodeURIComponent(planKey)}/prices`),
        api.get<PricingDiagnostics>(`/platform/admin/pricing/plans/${encodeURIComponent(planKey)}/diagnostics`),
        api.get<{ currencies: AdminCurrencyCatalogEntry[] }>('/platform/admin/pricing/currencies'),
      ]);
      setPrices(pricesRes.data ?? []);
      setDiagnostics(diagRes.data ?? null);
      setCurrencies(currenciesRes.data?.currencies ?? []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [planKey]);

  useEffect(() => {
    void load();
  }, [load]);

  const selectedCurrency =
    currencies.find((entry) => entry.code === form.currency) ??
    currencies.find((entry) => entry.code === 'EUR');

  const resetForm = () => {
    setForm(emptyForm);
    setEditingId(null);
  };

  const startEdit = (price: AdminPlanPrice) => {
    setEditingId(price.id);
    setForm({
      currency: price.currency,
      billingCycle: price.billingCycle,
      amountInput: majorToInput(price.amountMinor, price.decimals),
      regionCode: price.regionCode,
      checkoutCurrency: price.checkoutCurrency ?? '',
      isEnabled: price.isEnabled,
      isVisible: price.isVisible,
      isSelectable: price.isSelectable,
      paymentChannels: price.paymentChannels,
      stripePriceId: price.stripePriceId ?? '',
    });
  };

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    const decimals = selectedCurrency?.decimals ?? 2;
    const amountMinor = parseMajorToMinor(form.amountInput, decimals);

    // Contrôle avant envoi : le serveur refusera de la même façon, mais une
    // erreur ici coûte une seconde au lieu d'un aller-retour complet.
    if (amountMinor === null) {
      setError(
        decimals === 0
          ? `Montant invalide : ${form.currency} n'admet pas de décimales.`
          : `Montant invalide : ${decimals} décimale(s) maximum pour ${form.currency}.`,
      );
      return;
    }
    if (form.paymentChannels.length === 0) {
      setError('Sélectionnez au moins un canal de paiement.');
      return;
    }

    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const payload = {
        currency: form.currency,
        billingCycle: form.billingCycle,
        amountMinor,
        regionCode: form.regionCode,
        checkoutCurrency: form.checkoutCurrency || null,
        isEnabled: form.isEnabled,
        isVisible: form.isVisible,
        isSelectable: form.isSelectable,
        paymentChannels: form.paymentChannels.join(','),
        stripePriceId: form.stripePriceId || null,
      };
      if (editingId) {
        await api.put(`/platform/admin/pricing/plans/${encodeURIComponent(planKey)}/prices/${editingId}`, payload);
      } else {
        await api.post(`/platform/admin/pricing/plans/${encodeURIComponent(planKey)}/prices`, payload);
      }
      resetForm();
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (price: AdminPlanPrice) => {
    if (!window.confirm(`Retirer ${price.currency} ${price.billingCycle === 'YEARLY' ? 'annuel' : 'mensuel'} de ce plan ?`)) {
      return;
    }
    setError(null);
    try {
      await api.delete(`/platform/admin/pricing/plans/${encodeURIComponent(planKey)}/prices/${price.id}`);
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    }
  };

  const syncAll = async () => {
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const { data } = await api.post<{ synced: number; failed: number; stripeEnabled: boolean }>(
        `/platform/admin/pricing/plans/${encodeURIComponent(planKey)}/prices/stripe-sync`,
      );
      if (!data.stripeEnabled) {
        setNotice('Stripe n’est pas configuré sur cette installation : aucun Price n’a pu être créé.');
      } else {
        setNotice(`${data.synced} prix synchronisé(s), ${data.failed} en échec.`);
      }
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">Prix</h3>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={syncAll}
            disabled={saving}
            className="inline-flex items-center rounded-md border border-slate-300 bg-white px-3 py-1.5 text-xs hover:bg-slate-50 disabled:opacity-50"
          >
            <RefreshCw className="mr-1.5 h-3.5 w-3.5" /> Synchroniser Stripe
          </button>
          <button
            type="button"
            onClick={() => setForm({ ...emptyForm })}
            className="inline-flex items-center rounded-md border border-slate-300 bg-white px-3 py-1.5 text-xs hover:bg-slate-50"
          >
            <Plus className="mr-1.5 h-3.5 w-3.5" /> Ajouter un prix
          </button>
        </div>
      </div>

      {error && <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>}
      {notice && <div role="status" className="rounded-lg border border-sky-200 bg-sky-50 p-3 text-sm text-sky-800">{notice}</div>}

      {diagnostics && !diagnostics.healthy && (
        <div className="rounded-lg border border-amber-200 bg-amber-50 p-3">
          <p className="flex items-center gap-2 text-sm font-medium text-amber-900">
            <AlertTriangle className="h-4 w-4" /> Points à corriger
          </p>
          <ul className="mt-2 list-disc space-y-1 pl-5 text-xs text-amber-800">
            {diagnostics.issues
              .filter((issue) => issue.severity === 'ERROR')
              .map((issue) => (
                <li key={issue.code}>{issue.message}</li>
              ))}
          </ul>
        </div>
      )}

      {form.currency !== '' || editingId ? (
        <form onSubmit={save} className="space-y-3 rounded-lg border border-slate-200 bg-slate-50 p-4">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <label className="block text-xs">
              Devise
              <select
                value={form.currency}
                disabled={editingId !== null}
                onChange={(event) => setForm({ ...form, currency: event.target.value })}
                className="mt-1 w-full rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm"
              >
                {currencies
                  .filter((entry) => entry.enabled || entry.code === form.currency)
                  .map((entry) => (
                    <option key={entry.code} value={entry.code}>
                      {entry.symbol} {entry.code}
                      {entry.zeroDecimal ? ' (sans centimes)' : ''}
                    </option>
                  ))}
              </select>
            </label>

            <label className="block text-xs">
              Cycle
              <select
                value={form.billingCycle}
                disabled={editingId !== null}
                onChange={(event) => setForm({ ...form, billingCycle: event.target.value as BillingCycleName })}
                className="mt-1 w-full rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm"
              >
                <option value="MONTHLY">Mensuel</option>
                <option value="YEARLY">Annuel</option>
              </select>
            </label>

            <label className="block text-xs">
              Montant ({form.currency})
              <input
                required
                inputMode="decimal"
                value={form.amountInput}
                onChange={(event) => setForm({ ...form, amountInput: event.target.value })}
                className="mt-1 w-full rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm"
                placeholder={selectedCurrency?.decimals === 0 ? '15000' : '29.00'}
              />
              <span className="mt-1 block text-[11px] text-slate-500">
                {selectedCurrency?.decimals === 0
                  ? 'Devise sans centimes : saisissez un nombre entier.'
                  : `${selectedCurrency?.decimals ?? 2} décimale(s).`}
              </span>
            </label>

            <label className="block text-xs">
              Région
              <input
                value={form.regionCode}
                onChange={(event) => setForm({ ...form, regionCode: event.target.value.toUpperCase() })}
                className="mt-1 w-full rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm"
                placeholder={GLOBAL_REGION}
              />
              <span className="mt-1 block text-[11px] text-slate-500">{GLOBAL_REGION} = tarif mondial.</span>
            </label>
          </div>

          <fieldset className="text-xs">
            <legend className="mb-1">Canaux de paiement</legend>
            <div className="flex flex-wrap gap-3">
              {CHANNELS.map((channel) => (
                <label key={channel} className="inline-flex items-center gap-1.5">
                  <input
                    type="checkbox"
                    checked={form.paymentChannels.includes(channel)}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        paymentChannels: event.target.checked
                          ? [...form.paymentChannels, channel]
                          : form.paymentChannels.filter((item) => item !== channel),
                      })
                    }
                  />
                  {channel}
                </label>
              ))}
            </div>
          </fieldset>

          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-xs">
              Devise de débit (canal STRONG)
              <select
                value={form.checkoutCurrency}
                onChange={(event) => setForm({ ...form, checkoutCurrency: event.target.value })}
                className="mt-1 w-full rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm"
              >
                <option value="">Identique à la devise d’affichage</option>
                {currencies
                  .filter((entry) => entry.enabled)
                  .map((entry) => (
                    <option key={entry.code} value={entry.code}>
                      {entry.code}
                    </option>
                  ))}
              </select>
            </label>

            <label className="block text-xs">
              Stripe Price ID (saisie manuelle, facultative)
              <input
                value={form.stripePriceId}
                onChange={(event) => setForm({ ...form, stripePriceId: event.target.value })}
                placeholder="price_…"
                className="mt-1 w-full rounded-md border border-slate-300 bg-white px-2 py-1.5 font-mono text-xs"
              />
            </label>
          </div>

          <div className="flex flex-wrap gap-4 text-xs">
            <label className="inline-flex items-center gap-1.5">
              <input
                type="checkbox"
                checked={form.isEnabled}
                onChange={(event) => setForm({ ...form, isEnabled: event.target.checked })}
              />
              Actif
            </label>
            <label className="inline-flex items-center gap-1.5">
              <input
                type="checkbox"
                checked={form.isVisible}
                onChange={(event) => setForm({ ...form, isVisible: event.target.checked })}
              />
              Affiché
            </label>
            <label className="inline-flex items-center gap-1.5">
              <input
                type="checkbox"
                checked={form.isSelectable}
                onChange={(event) => setForm({ ...form, isSelectable: event.target.checked })}
              />
              Sélectionnable
            </label>
          </div>

          <div className="flex justify-end gap-2">
            <button type="button" onClick={resetForm} className="rounded-md border px-3 py-1.5 text-xs">
              Annuler
            </button>
            <button
              type="submit"
              disabled={saving}
              className="rounded-md bg-indigo-600 px-4 py-1.5 text-xs font-medium text-white disabled:opacity-50"
            >
              {saving ? 'Enregistrement…' : editingId ? 'Mettre à jour' : 'Ajouter'}
            </button>
          </div>
        </form>
      ) : null}

      {loading ? (
        <p className="text-sm text-slate-500">Chargement des prix…</p>
      ) : prices.length === 0 ? (
        <p className="text-sm text-slate-500">
          Aucun prix configuré : ce plan s’affichera sans montant tant qu’un prix n’est pas ajouté.
        </p>
      ) : (
        <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
          {prices.map((price) => (
            <li key={price.id} className="flex flex-wrap items-center gap-3 px-3 py-2.5 text-sm">
              <span className="font-mono text-xs text-slate-500">{price.currency}</span>
              <span className="w-16 text-xs text-slate-500">
                {price.billingCycle === 'YEARLY' ? 'Annuel' : 'Mensuel'}
              </span>
              <span className="font-semibold">
                {new Intl.NumberFormat('fr-FR', {
                  style: 'currency',
                  currency: price.currency,
                  minimumFractionDigits: price.decimals,
                  maximumFractionDigits: price.decimals,
                }).format(price.amountMinor / 10 ** price.decimals)}
              </span>
              {price.regionCode !== GLOBAL_REGION && (
                <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-600">
                  {price.regionCode}
                </span>
              )}
              {price.checkoutCurrency && price.checkoutCurrency !== price.currency && (
                <span className="rounded bg-violet-100 px-1.5 py-0.5 text-[11px] text-violet-700">
                  débit {price.checkoutCurrency}
                </span>
              )}
              <span
                className={`rounded-full px-2 py-0.5 text-[11px] ${STATUS_STYLE[price.stripePriceStatus ?? 'NOT_REQUIRED'] ?? STATUS_STYLE.NOT_REQUIRED}`}
                title={price.stripeSyncError ?? undefined}
              >
                {STATUS_LABEL[price.stripePriceStatus ?? 'NOT_REQUIRED'] ?? price.stripePriceStatus}
              </span>
              {!price.isVisible && (
                <span className="text-[11px] text-slate-400">masqué</span>
              )}
              {price.isVisible && !price.isSelectable && (
                <span className="text-[11px] text-slate-400">affichage seul</span>
              )}
              <span className="ml-auto flex items-center gap-1">
                {price.stripePriceStatus === 'SYNCED' && (
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" aria-label="Synchronisé" />
                )}
                {price.stripePriceId && (
                  <a
                    href={`https://dashboard.stripe.com/prices/${price.stripePriceId}`}
                    target="_blank"
                    rel="noreferrer"
                    className="text-slate-400 hover:text-slate-600"
                    aria-label="Ouvrir dans Stripe"
                  >
                    <ExternalLink className="h-4 w-4" />
                  </a>
                )}
                <button
                  type="button"
                  onClick={() => startEdit(price)}
                  className="rounded p-1.5 text-slate-600 hover:bg-slate-100"
                  aria-label="Modifier"
                >
                  <Pencil className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={() => void remove(price)}
                  className="rounded p-1.5 text-slate-600 hover:bg-red-50 hover:text-red-700"
                  aria-label="Retirer"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}