import { useCallback, useEffect, useState } from 'react';
import { RefreshCw, Save } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import type { AdminCurrencyCatalogEntry, PricingSettingsView } from './types';

/**
 * Réglages plateforme de la tarification (T5.3).
 *
 * <p>Tous les interrupteurs de la politique tarifaire sont ici : devises
 * autorisées, mode d'affichage, choix de devise et de cycle accordé au
 * visiteur, tarification régionale, canaux de paiement ouverts, synchronisation
 * Stripe, essai et remise, et comportement de la section tarifs de la landing.</p>
 *
 * <p>Le principe du produit tient en une phrase : <b>le super-admin décide de
 * tout</b>. Aucun de ces bascules n'est imposée par le code — elles sont
 * décrites dans {@code saas_pricing_settings} et appliquées par le serveur.</p>
 */
export default function PricingSettingsPanel() {
  const [settings, setSettings] = useState<PricingSettingsView | null>(null);
  const [currencies, setCurrencies] = useState<AdminCurrencyCatalogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [settingsRes, currenciesRes] = await Promise.all([
        api.get<PricingSettingsView>('/platform/admin/pricing/settings'),
        api.get<{ currencies: AdminCurrencyCatalogEntry[] }>('/platform/admin/pricing/currencies'),
      ]);
      setSettings(settingsRes.data);
      setCurrencies(currenciesRes.data?.currencies ?? []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const save = async () => {
    if (!settings) return;
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      await api.put('/platform/admin/pricing/settings', settings);
      setNotice('Réglages enregistrés. Le catalogue public sera servi à jour immédiatement.');
      await load();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <p className="text-sm text-slate-500">Chargement des réglages…</p>;
  if (!settings) return null;

  const enabled = new Set(settings.enabledCurrencies);

  return (
    <div className="space-y-6">
      {error && <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
      {notice && <div role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">{notice}</div>}

      <Section title="Devises" hint="Les devises listées ici sont celles que le catalogue sait servir. Une devise activée mais sans prix reste « non communiquée » sur la page publique.">
        <div className="flex flex-wrap gap-2">
          {currencies.map((entry) => (
            <label
              key={entry.code}
              className={`inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1 text-xs ${
                enabled.has(entry.code)
                  ? 'border-indigo-300 bg-indigo-50 text-indigo-800'
                  : 'border-slate-200 bg-white text-slate-500'
              }`}
            >
              <input
                type="checkbox"
                checked={enabled.has(entry.code)}
                onChange={(event) => {
                  const next = new Set(enabled);
                  if (event.target.checked) next.add(entry.code);
                  else next.delete(entry.code);
                  setSettings({ ...settings, enabledCurrencies: [...next].sort() });
                }}
              />
              {entry.symbol} {entry.code}
              {entry.zeroDecimal && <span className="text-[10px] opacity-70">0 déc.</span>}
            </label>
          ))}
        </div>

        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          <Select
            label="Devise de base"
            hint="Devise du catalogue par défaut."
            value={settings.baseCurrency}
            options={settings.enabledCurrencies}
            onChange={(value) => setSettings({ ...settings, baseCurrency: value })}
          />
          <Select
            label="Devise affichée par défaut"
            hint="Ce que voit un visiteur qui n'a pas choisi."
            value={settings.defaultDisplayCurrency}
            options={settings.enabledCurrencies}
            onChange={(value) => setSettings({ ...settings, defaultDisplayCurrency: value })}
          />
          <Toggle
            label="Repli sur la devise de base"
            hint="Si un plan n'a pas de prix dans la devise demandée, afficher celui de la devise de base. Désactivé, le tarif reste « non communiqué » — jamais un montant estimé."
            checked={settings.showFallbackCurrency}
            onChange={(value) => setSettings({ ...settings, showFallbackCurrency: value })}
          />
        </div>
      </Section>

      <Section title="Affichage" hint="Ce que le visiteur voit, et ce qu'il a le droit de choisir.">
        <div className="grid gap-3 sm:grid-cols-2">
          <Select
            label="Mode d'affichage"
            value={settings.displayMode}
            options={['SINGLE', 'SWITCHER', 'LIST_ALL', 'AUTO_REGION']}
            onChange={(value) => setSettings({ ...settings, displayMode: value as PricingSettingsView['displayMode'] })}
          />
          <Select
            label="Cycle par défaut"
            value={settings.defaultCycle}
            options={['MONTHLY', 'YEARLY']}
            onChange={(value) => setSettings({ ...settings, defaultCycle: value as PricingSettingsView['defaultCycle'] })}
          />
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <Toggle
            label="Le visiteur peut changer de devise"
            checked={settings.allowUserCurrencyChoice}
            onChange={(value) => setSettings({ ...settings, allowUserCurrencyChoice: value })}
          />
          <Toggle
            label="Le visiteur peut changer de cycle"
            checked={settings.allowCycleChoice}
            onChange={(value) => setSettings({ ...settings, allowCycleChoice: value })}
          />
          <Toggle
            label="Cycle mensuel autorisé"
            checked={settings.allowMonthly}
            onChange={(value) => setSettings({ ...settings, allowMonthly: value })}
          />
          <Toggle
            label="Cycle annuel autorisé"
            checked={settings.allowYearly}
            onChange={(value) => setSettings({ ...settings, allowYearly: value })}
          />
        </div>
      </Section>

      <Section title="Régions" hint="Une grille tarifaire par région, si vous en avez besoin. Par défaut, un tarif mondial unique.">
        <Toggle
          label="Tarification régionale activée"
          checked={settings.regionPricingEnabled}
          onChange={(value) => setSettings({ ...settings, regionPricingEnabled: value })}
        />
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <Select
            label="Source de la région"
            value={settings.regionResolution}
            options={['TENANT', 'HEADER', 'QUERY', 'IP']}
            onChange={(value) => setSettings({ ...settings, regionResolution: value as PricingSettingsView['regionResolution'] })}
          />
          <label className="block text-sm">
            Région par défaut
            <input
              value={settings.defaultRegion}
              onChange={(event) => setSettings({ ...settings, defaultRegion: event.target.value.toUpperCase() })}
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            />
          </label>
        </div>
        <div className="mt-4 space-y-3">
          <Toggle
            label="Faire confiance à l'en-tête de région"
            hint="Accepté par un reverse-proxy. Déconseillé : un visiteur pourrait alors choisir sa région, donc son tarif."
            checked={settings.trustRegionHeader}
            onChange={(value) => setSettings({ ...settings, trustRegionHeader: value })}
          />
          <Toggle
            label="Autoriser ?region= dans l'URL"
            hint="Usage : comparer deux devis. Sans effet sur le prix facturé."
            checked={settings.allowRegionQueryOverride}
            onChange={(value) => setSettings({ ...settings, allowRegionQueryOverride: value })}
          />
          <Toggle
            label="Le visiteur peut choisir sa région"
            checked={settings.allowRegionChoice}
            onChange={(value) => setSettings({ ...settings, allowRegionChoice: value })}
          />
        </div>
      </Section>

      <Section title="Canaux de paiement" hint="Un canal déclaré sur un prix mais indisponible n'est jamais proposé : mieux vaut « nous contacter » qu'un bouton vers un échec.">
        <div className="grid gap-3 sm:grid-cols-2">
          <Toggle label="Carte bancaire (Stripe)" checked={settings.allowCardChannel} onChange={(v) => setSettings({ ...settings, allowCardChannel: v })} />
          <Toggle label="Mobile Money" checked={settings.allowMomoChannel} onChange={(v) => setSettings({ ...settings, allowMomoChannel: v })} />
          <Toggle
            label="Débit en devise forte"
            hint="Afficher en XAF/XOF, débiter en devise internationale."
            checked={settings.allowStrongChannel}
            onChange={(v) => setSettings({ ...settings, allowStrongChannel: v })}
          />
          <Toggle label="Virement bancaire" checked={settings.allowBankChannel} onChange={(v) => setSettings({ ...settings, allowBankChannel: v })} />
          <Toggle label="Contact commercial" checked={settings.allowContactChannel} onChange={(v) => setSettings({ ...settings, allowContactChannel: v })} />
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <Select
            label="Devise forte"
            hint="Dans laquelle débite le canal STRONG."
            value={settings.strongCurrency}
            options={settings.enabledCurrencies}
            onChange={(value) => setSettings({ ...settings, strongCurrency: value })}
          />
          <label className="block text-sm">
            Opérateurs Mobile Money
            <select
              multiple
              value={settings.momoOperators}
              onChange={(event) =>
                setSettings({
                  ...settings,
                  momoOperators: [...event.target.selectedOptions].map((option) => option.value),
                })
              }
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            >
              {['MTN_MOMO', 'ORANGE_MONEY', 'M_PESA', 'AIRTEL_MONEY', 'WAVE'].map((operator) => (
                <option key={operator} value={operator}>
                  {operator}
                </option>
              ))}
            </select>
          </label>
        </div>
      </Section>

      <Section title="Stripe" hint="Les devises sans centimes (XAF, XOF, JPY) ne peuvent pas être facturées en prix inline : Stripe exige un Price créé au préalable.">
        <Toggle
          label="Créer les Prices automatiquement"
          hint="Création par l'API Stripe, avec clé d'idempotence. Désactivée, vous pouvez saisir les Price IDs à la main."
          checked={settings.stripePriceAutoSync}
          onChange={(value) => setSettings({ ...settings, stripePriceAutoSync: value })}
        />
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <label className="block text-sm">
            Taille du lot de synchronisation
            <input
              type="number"
              min={1}
              max={200}
              value={settings.stripeSyncBatchSize}
              onChange={(event) => setSettings({ ...settings, stripeSyncBatchSize: Number(event.target.value) })}
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            />
          </label>
          <label className="block text-sm">
            Intervalle de rattrapage (ms)
            <input
              type="number"
              min={10000}
              step={1000}
              value={settings.stripeSyncIntervalMs}
              onChange={(event) => setSettings({ ...settings, stripeSyncIntervalMs: Number(event.target.value) })}
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            />
          </label>
        </div>
        <p className="mt-3 text-xs text-slate-500">
          Le rattrapage des prix en attente ou en échec reste actif même si la création automatique est désactivée :
          « ne pas créer tout seul » ne veut pas dire « ne jamais réessayer ».
        </p>
      </Section>

      <Section title="Essai et remise">
        <div className="grid gap-3 sm:grid-cols-2">
          <Toggle label="Essai gratuit activé" checked={settings.trialEnabled} onChange={(v) => setSettings({ ...settings, trialEnabled: v })} />
          <label className="block text-sm">
            Remise annuelle par défaut (%)
            <input
              type="number"
              min={0}
              max={90}
              value={settings.annualDiscountDefault}
              onChange={(event) => setSettings({ ...settings, annualDiscountDefault: Number(event.target.value) })}
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            />
          </label>
        </div>
        <p className="mt-3 text-xs text-slate-500">
          Le prix annuel saisi reste prioritaire. La remise sert uniquement à dériver un annuel quand il n'existe pas.
        </p>
      </Section>

      <Section title="Landing" hint="La section tarifs de la page d'accueil lit le même catalogue que /pricing, sauf si vous la mettez en mode statique.">
        <Toggle label="Afficher la section tarifs" checked={settings.landingPricingEnabled} onChange={(v) => setSettings({ ...settings, landingPricingEnabled: v })} />
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <Select
            label="Source"
            value={settings.landingPricingSource}
            options={['API', 'STATIC']}
            onChange={(value) => setSettings({ ...settings, landingPricingSource: value as PricingSettingsView['landingPricingSource'] })}
          />
          <label className="block text-sm">
            Nombre maximum de plans
            <input
              type="number"
              min={1}
              max={12}
              value={settings.landingPricingMaxPlans}
              onChange={(event) => setSettings({ ...settings, landingPricingMaxPlans: Number(event.target.value) })}
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            />
          </label>
        </div>
        <div className="mt-4 space-y-3">
          <Toggle label="Afficher les prix" checked={settings.landingPricingShowPrices} onChange={(v) => setSettings({ ...settings, landingPricingShowPrices: v })} />
          <Toggle label="Afficher le lien « voir tous les tarifs »" checked={settings.landingPricingShowToggle} onChange={(v) => setSettings({ ...settings, landingPricingShowToggle: v })} />
        </div>
        <label className="mt-4 block text-sm">
          Libellé du bouton (laisser vide pour la valeur par défaut)
          <input
            value={settings.landingPricingCtaLabel ?? ''}
            onChange={(event) => setSettings({ ...settings, landingPricingCtaLabel: event.target.value || null })}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </label>
      </Section>

      <div className="sticky bottom-0 flex items-center justify-end gap-2 border-t border-slate-200 bg-slate-50 py-3">
        <span className="mr-auto text-xs text-slate-500">
          Version du catalogue : {settings.catalogVersion}
          {settings.updatedAt ? ` · modifié le ${new Date(settings.updatedAt).toLocaleString('fr-FR')}` : ''}
        </span>
        <button
          type="button"
          onClick={() => void load()}
          className="inline-flex items-center rounded-md border border-slate-300 bg-white px-4 py-2 text-sm hover:bg-slate-50"
        >
          <RefreshCw className="mr-2 h-4 w-4" /> Recharger
        </button>
        <button
          type="button"
          onClick={() => void save()}
          disabled={saving}
          className="inline-flex items-center rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          <Save className="mr-2 h-4 w-4" /> {saving ? 'Enregistrement…' : 'Enregistrer'}
        </button>
      </div>
    </div>
  );
}

function Section({
  title,
  hint,
  children,
}: {
  title: string;
  hint?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-5">
      <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500">{title}</h2>
      {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
      <div className="mt-4">{children}</div>
    </section>
  );
}

function Toggle({
  label,
  hint,
  checked,
  onChange,
}: {
  label: string;
  hint?: string;
  checked: boolean;
  onChange: (value: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-start gap-2 rounded-lg border border-slate-200 p-3 text-sm">
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} className="mt-0.5" />
      <span>
        <span className="font-medium">{label}</span>
        {hint && <span className="mt-0.5 block text-xs text-slate-500">{hint}</span>}
      </span>
    </label>
  );
}

function Select({
  label,
  hint,
  value,
  options,
  onChange,
}: {
  label: string;
  hint?: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
}) {
  return (
    <label className="block text-sm">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
      >
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
      {hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
    </label>
  );
}