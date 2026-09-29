// B1 + G1 — Étape 0 : identité de l'église.
// Action serveur : renommage de l'église racine + mise à jour des réglages
// du tenant (champs optionnels uniquement, cf. OnboardingStepActions côté
// backend).
//
// Deux corrections par rapport à la version initiale de cette étape :
// 1. `Devise` et `Fuseau horaire` étaient des champs texte libre. Le backend
//    expose désormais GET /platform/currencies (ISO-4217) : la liste vient de
//    l'API, reste saisie libre (D12), et l'utilisateur est prévenu si le
//    catalogue est indisponible. Le fuseau utilise `Intl.supportedValuesOf`,
//    donc aucune dépendance ajoutée.
// 2. Les libellés passaient par `t(texteFrançais)`, c'est-à-dire une recherche
//    par CLÉ : les clés n'existant pas, le libellé retombait en français dans
//    les 5 autres locales. Ils passent maintenant par `tText`, qui traduit par
//    VALEUR — le mécanisme réellement supporté par le dépôt.

import { useMemo, useState } from 'react';
import { useI18n, tText } from '@/i18n';
import { useCurrencies, listTimeZones } from '@/hooks/useCurrencies';
import type { ChurchIdentityData } from '@/types/onboarding';
import type { StepProps } from './StepProps';

const FIELD_CLASS =
  'w-full rounded-lg border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 min-h-[44px]';

export default function ChurchIdentityStep({ step, disabled, onSubmit }: StepProps) {
  const { t } = useI18n();
  const prior = (step.completedData ?? {}) as Partial<ChurchIdentityData>;
  const [form, setForm] = useState<ChurchIdentityData>({
    churchName: prior.churchName ?? '',
    businessName: prior.businessName ?? '',
    city: prior.city ?? '',
    phone: prior.phone ?? '',
    email: prior.email ?? '',
    timezone: prior.timezone ?? '',
    currency: prior.currency ?? '',
  });
  const [error, setError] = useState<string | null>(null);
  const { currencies, isCatalog, isLoading: currenciesLoading } = useCurrencies();
  const timeZones = useMemo(() => listTimeZones(), []);

  const set = (k: keyof ChurchIdentityData) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const name = form.churchName.trim();
    if (name.length < 2 || name.length > 120) {
      setError(tText('Le nom doit contenir entre 2 et 120 caractères.'));
      return;
    }
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      setError(tText('Adresse email invalide.'));
      return;
    }
    setError(null);
    onSubmit(form as unknown as Record<string, unknown>);
  };

  const label = (k: string) => `${tText(k)}:`;

  return (
    <form onSubmit={submit} className="glass-card p-6 space-y-4" noValidate>
      <div>
        <label htmlFor="churchName" className="block text-sm font-medium mb-1">
          {label('Nom de l’église')} <span className="text-red-400">*</span>
        </label>
        <input
          id="churchName"
          name="churchName"
          value={form.churchName}
          onChange={set('churchName')}
          disabled={disabled}
          required
          minLength={2}
          maxLength={120}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'churchName-error' : undefined}
          className={FIELD_CLASS}
        />
      </div>

      {[
        { id: 'businessName', k: 'Nom commercial' },
        { id: 'city', k: 'Ville' },
        { id: 'phone', k: 'Téléphone' },
        { id: 'email', k: 'Email', type: 'email' },
      ].map((f) => (
        <div key={f.id}>
          <label htmlFor={f.id} className="block text-sm font-medium mb-1">{label(f.k)}</label>
          <input
            id={f.id}
            name={f.id}
            type={f.type ?? 'text'}
            value={form[f.id as keyof ChurchIdentityData] ?? ''}
            onChange={set(f.id as keyof ChurchIdentityData)}
            disabled={disabled}
            className={FIELD_CLASS}
          />
        </div>
      ))}

      {/* Fuseau horaire : liste de la plateforme, saisie toujours possible. */}
      <div>
        <label htmlFor="timezone" className="block text-sm font-medium mb-1">
          {label('Fuseau horaire (IANA)')}
        </label>
        <input
          id="timezone"
          name="timezone"
          list="timezone-suggestions"
          value={form.timezone}
          onChange={set('timezone')}
          disabled={disabled}
          aria-describedby="timezone-help"
          className={FIELD_CLASS}
        />
        <datalist id="timezone-suggestions">
          {timeZones.map((tz) => (
            <option key={tz} value={tz} />
          ))}
        </datalist>
        <p id="timezone-help" className="text-xs text-gray-500 dark:text-gray-400 mt-1">
          {t('onboarding.step.identity.timezoneHint')}
        </p>
      </div>

      {/* Devise : catalogue ISO-4217 du backend, saisie libre en repli. */}
      <div>
        <label htmlFor="currency" className="block text-sm font-medium mb-1">
          {label('Devise (ISO-4217)')}
        </label>
        <input
          id="currency"
          name="currency"
          list="currency-suggestions"
          value={form.currency}
          onChange={set('currency')}
          disabled={disabled}
          aria-describedby="currency-help"
          aria-busy={currenciesLoading}
          className={FIELD_CLASS}
        />
        <datalist id="currency-suggestions">
          {currencies.map((c) => (
            <option key={c.code} value={c.code}>
              {`${c.symbol} — ${c.name}`}
            </option>
          ))}
        </datalist>
        <p id="currency-help" className="text-xs text-gray-500 dark:text-gray-400 mt-1">
          {isCatalog
            ? t('onboarding.step.identity.currencyFromCatalog')
            : currenciesLoading
              ? t('onboarding.step.identity.currencyLoading')
              : t('onboarding.step.identity.currencyFallback')}
        </p>
      </div>

      {error && (
        <p id="churchName-error" role="alert" className="text-sm text-red-500">{error}</p>
      )}
      <button type="submit" disabled={disabled} className="btn-primary btn-sm min-h-[44px]">
        {tText('Enregistrer')}
      </button>
    </form>
  );
}
